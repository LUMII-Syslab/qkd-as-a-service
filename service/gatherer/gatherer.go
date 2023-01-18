package gatherer

type KeyGathererListener interface {
	AddKey(id []byte, val []byte) error
}

type KeyGatherer interface {
	PublishTo(KeyGathererListener)
	Start() error
}

func NewClavisKeyGatherer(clavisURL string) KeyGatherer {
	return &ClavisKeyGatherer{keyGathererBase: keyGathererBase{make([]KeyGathererListener, 0), 0}, clavisURL: clavisURL}
}

func NewRandomKeyGatherer(keyIdLength, keyValLength int) KeyGatherer {
	return &RandomKeyGatherer{keyGathererBase{make([]KeyGathererListener, 0), 0}, keyIdLength, keyValLength}
}
