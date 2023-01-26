package manager

import (
	"errors"
	"fmt"
	"log"
	"qkdc-service/constants"
	"qkdc-service/logging"
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
	all         *deque.Deque[Key]
	reservable  *deque.Deque[Key]
	delayed     map[string]bool // keys outside reservable waiting to enter
	delay       uint32          // key lifetime outside reservable ( milliseconds )
	notifier    chan int        // alerts threads waiting for reservable keys
	dictionary  map[string]Key  // key id, val dictionary
	sizeLimit   uint64          // maximum all size
	servesLeft  bool            // true <-> returns left of key and serves even ( otherwise returns right and serves odd)
	mutex       sync.Mutex      // mutex for all, reservable, delayed, dictionary
	running     bool            // running state ( keys can be reserved by the users)
	keysAdded   uint64
	keysServerd uint64
	logger      *log.Logger
}

func newKeyManager(maxKeyCount uint64, aija bool, logger *log.Logger) *KeyManager {
	return &KeyManager{
		all:        deque.New[Key](),
		reservable: deque.New[Key](),
		delayed:    make(map[string]bool),
		notifier:   make(chan int, maxKeyCount*100),
		delay:      500,
		dictionary: make(map[string]Key),
		sizeLimit:  maxKeyCount,
		servesLeft: aija,
		running:    true,
		logger:     logger,
	}
}

type KeyManagerState struct {
	AllSize        uint64
	ReservableSize uint64
	NotifierSize   uint64
	DictionarySize uint64
	KeysAdded      uint64
	KeysServed     uint64
	Running        bool
}

func (k *KeyManager) getManagerState() KeyManagerState {
	return KeyManagerState{
		AllSize:        uint64(k.all.Len()),
		ReservableSize: uint64(k.reservable.Len()),
		NotifierSize:   uint64(len(k.notifier)),
		DictionarySize: uint64(len(k.dictionary)),
		KeysAdded:      k.keysAdded,
		KeysServed:     k.keysServerd,
		Running:        k.running,
	}
}

func (k *KeyManager) getKey(id []byte) (Key, *logging.KDCError) {
	if !k.running {
		return Key{}, logging.NewKDCError(constants.ErrorNotRunning, errors.New("key manager is not running"))
	}

	k.mutex.Lock()
	val, exists := k.dictionary[string(id)]
	if !exists {
		k.mutex.Unlock()
		return Key{}, logging.NewKDCError(constants.ErrorKeyNotFound,
			errors.New(fmt.Sprintf("key %v not found in manager", utils.BytesToHexOctets(id))))
	}
	k.mutex.Unlock()
	return val, nil
}

func (k *KeyManager) addKey(id []byte, val []byte) error {
	key := Key{KeyId: id, KeyVal: val, Created: time.Now()}
	keyByteSum := utils.ByteSum(key.KeyVal)
	keyReservable := k.servesLeft == ((keyByteSum % 2) == 0)

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
			time.Sleep(time.Duration(k.delay) * time.Millisecond)
			k.mutex.Lock()
			if k.all.Front().Order > key.Order {
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
func (k *KeyManager) extractKey() (Key, *logging.KDCError) {
	if !k.running {
		return Key{}, logging.NewKDCError(constants.ErrorNotRunning, errors.New("key manager is not running"))
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
	return result, nil
}

// getFirstKey returns the first key in the queue that is either even or odd
func (k *KeyManager) getFirstKey(even bool) ([]byte, error) {
	k.mutex.Lock()

	k.mutex.Unlock()
	return []byte{}, nil
}