package data

import (
	"github.com/gammazero/deque"
	"sync"
)

// SyncDeque - thread safe deque structure
type SyncDeque[T any] struct {
	data *deque.Deque[T]
	lock sync.Mutex
}

func NewSyncDeque[T any]() *SyncDeque[T] {
	return &SyncDeque[T]{
		data: deque.New[T](),
		// lock doesn't have to be instantiated
	}
}

type SyncMap[Key string, Val any] struct {
	data map[Key]Val
	lock sync.Mutex
}

func NewSyncMap[Key string, Val any]() *SyncMap[Key, Val] {
	return &SyncMap[Key, Val]{
		data: make(map[Key]Val),
	}
}
