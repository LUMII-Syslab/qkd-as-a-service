package manager

import (
	"errors"
	"fmt"
	"log"
	"qkdc-service/constants"
	"qkdc-service/utils"
	"sync"
	"time"

	"github.com/gammazero/deque"
)

type Key struct {
	KeyId   []byte
	KeyVal  []byte
	Created time.Time
	Order   uint64
}

type KeyManager struct {
	all        *deque.Deque[Key]
	reservable *deque.Deque[Key]
	dictionary map[string]Key // key id, val map
	delay      uint32         // key lifetime outside reservable ( milliseconds )
	notifier   chan int       // alerts threads waiting for reservable keys
	sizeLimit  uint64         // maximum all size
	aija       bool           // true <-> returns left of key and serves even ( otherwise returns right and serves odd)
	mutex      sync.Mutex     // mutex for all, reservable, delayed, dictionary
	serving    bool           // serving state ( keys can be reserved by the users)

	keysAdded   uint64
	keysServerd uint64
	keysDelayed uint64

	logger *log.Logger

	HashAlgId []byte
}

func newKeyManager(maxKeyCount uint64, aija bool, logger *log.Logger) *KeyManager {
	return &KeyManager{
		all:        deque.New[Key](),
		reservable: deque.New[Key](),
		notifier:   make(chan int, maxKeyCount*10),
		delay:      500,
		dictionary: make(map[string]Key),
		sizeLimit:  maxKeyCount,
		aija:       aija,
		serving:    false,
		logger:     logger,
		HashAlgId:  []byte{0x60, 0x86, 0x48, 0x01, 0x65, 0x03, 0x04, 0x02, 0x11},
	}
}

type KeyManagerState struct {
	AllSize        uint64
	ReservableSize uint64
	NotifierSize   uint64
	DictionarySize uint64
	KeysAdded      uint64
	KeysServed     uint64
	KeysDelayed    uint64
	Serving        bool
	OldestOddKey   *Key
	OldestEvenKey  *Key
}

func (k *KeyManager) getManagerState() KeyManagerState {
	return KeyManagerState{
		AllSize:        uint64(k.all.Len()),
		ReservableSize: uint64(k.reservable.Len()),
		NotifierSize:   uint64(len(k.notifier)),
		DictionarySize: uint64(len(k.dictionary)),
		KeysAdded:      k.keysAdded,
		KeysServed:     k.keysServerd,
		Serving:        k.serving,
		OldestOddKey:   k.getOldestKey(false),
		OldestEvenKey:  k.getOldestKey(true),
	}
}

func (k *KeyManager) getKey(id []byte) (key *Key, errId int) {
	if !k.serving {
		errId = constants.ErrorNotServing
		return
	}

	k.mutex.Lock()
	val, exists := k.dictionary[string(id)]
	if !exists {
		k.mutex.Unlock()
		return nil, constants.ErrorKeyNotFound
	}
	k.mutex.Unlock()
	k.keysServerd += 1
	return &val, constants.NoError
}

func (k *KeyManager) addKey(id []byte, val []byte) error {
	key := Key{KeyId: id, KeyVal: val, Created: time.Now()}
	keyByteSum := utils.ByteSum(key.KeyVal)
	keyReservable := k.aija == ((keyByteSum % 2) == 0)

	k.mutex.Lock()
	_, exists := k.dictionary[string(key.KeyId)]
	if exists {
		k.mutex.Unlock()
		return errors.New(fmt.Sprintf("key %v already exists", utils.BytesToHexOctets(key.KeyId)))
	}

	k.keysAdded += 1
	key.Order = k.keysAdded

	k.dictionary[string(key.KeyId)] = key
	k.all.PushBack(key)
	if uint64(k.all.Len()) > k.sizeLimit {
		rem := k.all.PopFront()
		if keyReservable {
			for k.reservable.Len() > 0 && k.reservable.Front().Order <= rem.Order {
				<-k.notifier
				k.reservable.PopFront()
			}
		}
		delete(k.dictionary, string(rem.KeyId))
	}
	k.mutex.Unlock()

	if keyReservable {
		go func() {
			k.keysDelayed += 1
			time.Sleep(time.Duration(k.delay) * time.Millisecond)
			k.keysDelayed -= 1
			k.mutex.Lock()
			if k.all.Len() == 0 || k.all.Front().Order > key.Order {
				k.mutex.Unlock()
				return
			}
			k.reservable.PushBack(key)
			k.notifier <- 1
			k.mutex.Unlock()
		}()
	}

	return nil
}

// extractKey extracts key and removes it from queue
func (k *KeyManager) extractKey() (key *Key, errId int) {
	if !k.serving {
		errId = constants.ErrorNotServing
		return
	}
	retrieved := false
	result := Key{}
	for !retrieved {
		<-k.notifier
		k.mutex.Lock()
		if k.reservable.Len() > 0 {
			result = k.reservable.PopBack()
			retrieved = true
		}
		k.mutex.Unlock()
	}
	return &result, constants.NoError
}

// getOldestKey returns the first key in the queue that is either even or odd
func (k *KeyManager) getOldestKey(even bool) *Key {
	k.mutex.Lock()
	defer k.mutex.Unlock()
	if k.all.Len() == 0 {
		return nil
	}
	for i := 0; i < k.all.Len(); i++ {
		if (utils.ByteSum(k.all.At(i).KeyVal)%2 == 0) == even {
			val := k.all.At(i)
			return &val
		}
	}
	return nil
}

func (k *KeyManager) stopServingAndClear() {
	k.mutex.Lock()
	k.serving = false
	k.all.Clear()
	k.reservable.Clear()
	k.dictionary = make(map[string]Key)
	// clear notifier
	for len(k.notifier) > 0 {
		<-k.notifier
	}
	k.mutex.Unlock()
}

func (k *KeyManager) startServing() {
	k.mutex.Lock()
	k.serving = true
	k.mutex.Unlock()
}
