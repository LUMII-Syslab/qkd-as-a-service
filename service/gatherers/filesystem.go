package gatherers

type FilesystemKeyGatherer struct {
	keyGathererBase
	path string
}

func NewFilesystemKeyGatherer(path string) KeyGatherer {
	return &FilesystemKeyGatherer{keyGathererBase: keyGathererBase{make([]KeyGathererListener, 0), 0}, path: path}
}

func (fkg *FilesystemKeyGatherer) Start() error {
	panic("Filesystem key gatherer implementation is not yet implemented.")
}
