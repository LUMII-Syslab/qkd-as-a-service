package data

import (
	"bytes"
	"errors"
	"fmt"
	"log"
	"qkdc-service/utils"
	"time"
)

type Key struct {
	KeyId   []byte
	KeyVal  []byte
	Created time.Time
}

type KeyManager struct {
	A *SyncDeque[Key]       // all keys
	B *SyncDeque[Key]       // reservable keys
	C *SyncDeque[Key]       // queue into B
	Z int                   // key lifetime in C ( milliseconds )
	D *SyncMap[string, Key] // key id, val dictionary
	W int                   // maximum A size
	L bool                  // true <-> returns left of key and serves even ( otherwise returns right and serves odd)
}

func (k *KeyManager) getKey(id []byte) (Key, error) {
	val, exists := k.D.Get(string(id))
	if !exists {
		return Key{}, errors.New(fmt.Sprintf("key %v not found in data", id))
	}
	return val, nil
}

// refreshC ensures that no key in C is older than Z milliseconds
func (k *KeyManager) refreshC() {
	now := time.Now()
	front := k.C.Front()
	for now.Sub(front.Created).Milliseconds() >= int64(k.Z) {
		k.B.PushBack(front)
		k.C.PopFront()
		if k.C.Size() > 0 {
			front = k.C.Front()
		}
	}
}

func (k *KeyManager) addKey(key Key) error {
	if k.D.Exists(string(key.KeyId)) {
		return errors.New(fmt.Sprintf("key %v already exists", utils.BytesToHexOctets(id)))
	}
	k.refreshC() // moves keys into B
	rem, remKey := k.A.AddWithW(key, k.W)
	if rem {
		k.D.Erase(string(remKey.KeyId))
		if k.B.Size() > 0 && bytes.Compare(k.B.Front().KeyId, remKey.KeyId) == 0 {
			_, _ = k.B.PopFront()
		}
	}
	k.D.Add(string(key.KeyId), key)
	if k.L == (utils.ByteSum(key.KeyVal)%2 == 0) {
		k.C.PushBack(key)
	}
	return nil
}

// ReserveKey extracts key and removes it from queue
func (k *KeyManager) ReserveKey() (key Key, err error) {
	key
	key := <-k.queue
	log.Println(len(k.queue), utils.BytesToHexOctets(key))
	return key
}
