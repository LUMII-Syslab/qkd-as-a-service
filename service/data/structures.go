package data

import (
	"fmt"
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

func (d *SyncDeque[T]) PushBack(x T) {
	d.lock.Lock()
	d.data.PushBack(x)
	d.lock.Unlock()
}

// AddWithW adds new element maintaining deque max size W
func (d *SyncDeque[T]) AddWithW(x T, w int) (bool, T) {
	didRemove := false
	var removed T
	d.lock.Lock()
	if d.data.Len() == w {
		didRemove = true
		removed = d.data.PopFront()
	}
	d.data.PushBack(x)
	d.lock.Unlock()
	return didRemove, removed
}

func (d *SyncDeque[T]) Size() (res int) {
	d.lock.Lock()
	res = d.data.Len()
	d.lock.Unlock()
	return
}

func (d *SyncDeque[T]) Front() (res T) {
	d.lock.Lock()
	res = d.data.Front()
	d.lock.Unlock()
	return
}

func (d *SyncDeque[T]) PopFront() (res T, err error) {
	d.lock.Lock()
	if d.data.Len() == 0 {
		d.lock.Unlock()
		err = fmt.Errorf("queue is empty. can't pop front")
		return
	}
	res = d.data.PopFront()
	d.lock.Unlock()
	return
}

func (d *SyncDeque[T]) PopBack() (res T, err error) {
	d.lock.Lock()
	if d.data.Len() == 0 {
		d.lock.Unlock()
		err = fmt.Errorf("queue is empty. can't pop back")
		return
	}
	res = d.data.PopBack()
	d.lock.Unlock()
	return
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

func (s *SyncMap[Key, Val]) Exists(key Key) bool {
	res := false
	s.lock.Lock()
	_, res = s.data[key]
	s.lock.Unlock()
	return res
}

func (s *SyncMap[Key, Val]) Get(key Key) (val Val, ok bool) {
	s.lock.Lock()
	val, ok = s.data[key]
	s.lock.Unlock()
	return
}

func (s *SyncMap[Key, Val]) Erase(key Key) {
	s.lock.Lock()
	delete(s.data, key)
	s.lock.Unlock()
}

func (s *SyncMap[Key, Val]) Add(key Key, val Val) {
	s.lock.Lock()
	s.data[key] = val
	s.lock.Unlock()
	return
}
