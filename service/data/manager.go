package data

import (
	"errors"
	"fmt"
	"qkdc-service/constants"
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
}

type KeyManager struct {
	A *deque.Deque[Key] // all keys
	B *deque.Deque[Key] // reservable keys
	C map[string]bool   // keys outside B
	S chan int          // notifies threads waiting for B elements
	Z int               // key lifetime outside B ( milliseconds )
	D map[string]Key    // key id, val dictionary
	W int               // maximum A size
	L bool              // true <-> returns left of key and serves even ( otherwise returns right and serves odd)
	M sync.Mutex        // mutex for A, B, C, D
	R bool              // running ( keys can be reserved by the users)
}

func (k *KeyManager) getKey(id []byte) (Key, error) {
	if !k.R {
		return Key{}, errors.New("key manager is not running")
	}

	k.M.Lock()
	val, exists := k.D[string(id)]
	if !exists {
		k.M.Unlock()
		return Key{}, errors.New(fmt.Sprintf("key %v not found in data", id))
	}
	k.M.Unlock()
	return val, nil
}

func (k *KeyManager) addKey(id []byte, val []byte) error {
	key := Key{KeyId: id, KeyVal: val, Created: time.Now()}

	k.M.Lock()
	_, exists := k.D[string(key.KeyId)]
	if exists {
		k.M.Unlock()
		return errors.New(fmt.Sprintf("key %v already exists", utils.BytesToHexOctets(key.KeyId)))
	}
	k.A.PushBack(key)
	k.D[string(key.KeyId)] = key
	if k.A.Len() > k.W {
		rem := k.A.PopFront()
		delete(k.D, string(rem.KeyId))
		_, exists = k.C[string(rem.KeyId)]
		if exists {
			k.C[string(rem.KeyId)] = false
		} else if k.B.Len() > 0 && reflect.DeepEqual(rem, k.B.Front()) { // the key will be in front of B if in B
			<-k.S
			k.B.PopFront()
		}
	}
	// add key to B after a delay of Z milliseconds
	if k.L == (utils.ByteSum(key.KeyVal)%2 == 0) {
		k.C[string(key.KeyId)] = true
		go func() {
			time.Sleep(time.Duration(k.Z) * time.Millisecond)
			k.M.Lock()
			in, e := k.C[string(key.KeyId)]
			if e && in {
				k.B.PushBack(key)
				k.S <- 1
			}
			k.M.Unlock()
		}()
	}
	k.M.Unlock()

	return nil
}

// extractKey extracts key and removes it from queue
func (k *KeyManager) extractKey() (Key, error) {
	if !k.R {
		return Key{}, errors.New("key manager is not running")
	}
	retrieved := false
	result := Key{}
	for !retrieved {
		<-k.S
		k.M.Lock()
		if k.B.Len() > 0 {
			result = k.B.PopBack()
			retrieved = true
		}
		k.M.Unlock()
	}
	return result, nil
}

func (k *KeyManager) getManagerState() int {
	keysAvailable := 0
	k.M.Lock()
	keysAvailable = k.B.Len()
	k.M.Unlock()
	if keysAvailable == 0 {
		return constants.EMPTY
	} else if k.R == false {
		return constants.RECEIVING
	} else {
		return constants.RUNNING
	}
}

// getFirstKey returns the first key in the queue that is either even or odd
func (k *KeyManager) getFirstKey(even bool) ([]byte, error) {
	k.M.Lock()

	k.M.Unlock()
	return []byte{}, nil
}
