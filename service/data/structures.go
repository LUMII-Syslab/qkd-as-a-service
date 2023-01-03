package data

import (
	"github.com/gammazero/deque"
	"sync"
)

// SyncDeque - thread safe deque structure
type SyncDeque[T any] struct {
	data deque.Deque[T]
	lock sync.Mutex
}

type SyncMap[T any, T2 any] struct {
}
