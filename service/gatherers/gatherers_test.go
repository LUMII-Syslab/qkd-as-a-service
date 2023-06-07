package gatherers

import (
	"log"
	"qkdc-service/config"
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
	kg := NewPseudorandomKeyGatherer(32, 32, false)
	kgl := &keyGathererListener{make(chan [2]interface{})}
	kg.PublishTo(kgl)
	go kg.Start()
	b.Log("b.N:", b.N)
	<-kgl.queue // let the gatherer start
	b.ResetTimer()
	for i := 0; i < b.N; i++ {
		<-kgl.queue
	}
}

func BenchmarkClavisGatherer(b *testing.B) {
	conf := config.LoadConfig("../config.toml")
	log.Println("conf.ClavisURL:", conf.ClavisURL)
	kg := NewClavisKeyGatherer(conf.ClavisURL)
	kgl := &keyGathererListener{make(chan [2]interface{})}
	kg.PublishTo(kgl)
	go kg.Start()
	b.Log("b.N:", b.N)
	<-kgl.queue // let the gatherer start
	b.ResetTimer()
	for i := 0; i < b.N; i++ {
		<-kgl.queue
	}
}

func BenchmarkFilesystemGatherer(b *testing.B) {
	conf := config.LoadConfig("../config.toml")
	log.Println("conf.FSGathererDir:", conf.FSGathererDir)
	kg := NewFileSystemKeyGatherer(conf.FSGathererDir)
	kgl := &keyGathererListener{make(chan [2]interface{})}
	kg.PublishTo(kgl)
	go kg.Start()
	b.Log("b.N:", b.N)
	<-kgl.queue // let the gatherer start
	b.ResetTimer()
	for i := 0; i < b.N; i++ {
		<-kgl.queue
	}
}
