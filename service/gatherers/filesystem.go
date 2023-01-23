package gatherers

type FileSystemKeyGatherer struct {
	keyGathererBase
	path string
}

func NewFileSystemKeyGatherer(path string) *FileSystemKeyGatherer {
	return &FileSystemKeyGatherer{keyGathererBase: keyGathererBase{make([]KeyGathererListener, 0), 0}, path: path}
}

func (fkg *FileSystemKeyGatherer) Start() error {
	panic("Filesystem key gatherer implementation is not yet implemented.")
}
