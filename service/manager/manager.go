package manager

import (
	"errors"
	"fmt"
	"qkdc-service/constants"
	"qkdc-service/logging"
	"qkdc-service/utils"
	"reflect"
	"sync"
	"time"

	"github.com/gammazero/deque"
)

type Key struct {
	KeyId   []byte
	KeyVal  []byte
	Created time.Time
	Order   int
}

type KeyManager struct {
	all        *deque.Deque[Key]
	reservable *deque.Deque[Key]
	delayed    map[string]bool // keys outside reservable waiting to enter
	delay      int             // key lifetime outside reservable ( milliseconds )
	notifier   chan int        // alerts threads waiting for reservable keys
	dictionary map[string]Key  // key id, val dictionary
	sizeLimit  int             // maximum all size
	servesLeft bool            // true <-> returns left of key and serves even ( otherwise returns right and serves odd)
	mutex      sync.Mutex      // mutex for all, reservable, delayed, dictionary
	running    bool            // running state ( keys can be reserved by the users)
	keysAdded  int
}

func newKeyManager(maxKeyCount int, aija bool) *KeyManager {
	return &KeyManager{
		all:        deque.New[Key](),
		reservable: deque.New[Key](),
		delayed:    make(map[string]bool),
		notifier:   make(chan int, maxKeyCount),
		delay:      500,
		dictionary: make(map[string]Key),
		sizeLimit:  maxKeyCount,
		servesLeft: aija,
		running:    true,
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
	k.mutex.Lock()
	k.keysAdded += 1
	key.Order = k.keysAdded

	_, exists := k.dictionary[string(key.KeyId)]
	if exists {
		k.mutex.Unlock()
		return errors.New(fmt.Sprintf("key %v already exists", utils.BytesToHexOctets(key.KeyId)))
	}
	k.all.PushBack(key)
	k.dictionary[string(key.KeyId)] = key
	if k.all.Len() > k.sizeLimit {
		rem := k.all.PopFront()
		delete(k.dictionary, string(rem.KeyId))
		_, exists = k.delayed[string(rem.KeyId)]
		if exists {
			k.delayed[string(rem.KeyId)] = false
		} else if k.reservable.Len() > 0 && reflect.DeepEqual(rem, k.reservable.Front()) { // the key will be in front of reservable if in reservable
			<-k.notifier
			k.reservable.PopFront()
		}
	}
	// add key to reservable after a delay
	if k.servesLeft == (utils.ByteSum(key.KeyVal)%2 == 0) {
		k.delayed[string(key.KeyId)] = true
		go func() {
			time.Sleep(time.Duration(k.delay) * time.Millisecond)
			k.mutex.Lock()
			in, e := k.delayed[string(key.KeyId)]
			if e && in {
				k.reservable.PushBack(key)
				k.notifier <- 1
			}
			if e {
				delete(k.delayed, string(key.KeyId))
			}
			k.mutex.Unlock()
		}()
	}
	k.mutex.Unlock()

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

func (k *KeyManager) getManagerState() int {
	keysAvailable := 0
	k.mutex.Lock()
	keysAvailable = k.reservable.Len()
	k.mutex.Unlock()
	if keysAvailable == 0 {
		return constants.Empty
	} else if k.running == false {
		return constants.Receiving
	} else {
		return constants.Running
	}
}

// getFirstKey returns the first key in the queue that is either even or odd
func (k *KeyManager) getFirstKey(even bool) ([]byte, error) {
	k.mutex.Lock()

	k.mutex.Unlock()
	return []byte{}, nil
}
