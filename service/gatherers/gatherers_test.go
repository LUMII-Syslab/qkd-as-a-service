package gatherers

import (
	"testing"
)

type keyGathererListener struct {
	queue chan [2]interface{}
}

func (kgl *keyGathererListener) AddKey(id []byte, val []byte) error {
	kgl.queue <- [2]interface{}{id, val}
	return nil
}

func BenchmarkPseudorandomGatherer(b *testing.B) {
	kg := NewPseudorandomKeyGatherer(16, 16, false)
	kgl := &keyGathererListener{make(chan [2]interface{})}
	kg.PublishTo(kgl)
	go kg.Start()
	b.ResetTimer()
	for i := 0; i < b.N; i++ {
		<-kgl.queue
	}
}

func BenchmarkClavisGatherer(b *testing.B) {

}

// 1072153 ns/op
// 2859 ns/op
// 3494 ns/op
// 3929 ns/op
// 2729 ns/op
// 2718 ns/op
// 1085647
