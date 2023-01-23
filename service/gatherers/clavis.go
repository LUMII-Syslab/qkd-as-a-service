package gatherers

type ClavisKeyGatherer struct {
	keyGathererBase
	clavisURL string
}

func NewClavisKeyGatherer(clavisURL string) *ClavisKeyGatherer {
	return &ClavisKeyGatherer{keyGathererBase: keyGathererBase{make([]KeyGathererListener, 0), 0}, clavisURL: clavisURL}
}

func (kg *ClavisKeyGatherer) Start() error {
	panic("Clavis key gatherers implementation can't be public. It's a secret.")
}
