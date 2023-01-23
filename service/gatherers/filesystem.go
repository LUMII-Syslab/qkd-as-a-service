package gatherers

import (
	"fmt"
	"github.com/fsnotify/fsnotify"
	"os"
	"path/filepath"
)

type FileSystemKeyGatherer struct {
	keyGathererBase
	dirPath string
}

func NewFileSystemKeyGatherer(dirPath string) *FileSystemKeyGatherer {
	return &FileSystemKeyGatherer{keyGathererBase: keyGathererBase{make([]KeyGathererListener, 0), 0}, dirPath: dirPath}
}

func (fkg *FileSystemKeyGatherer) Start() error {
	err := fkg.readAndRemoveKeys()
	if err != nil {
		return err
	}

	watcher, err := fsnotify.NewWatcher()

	err = watcher.Add(fkg.dirPath)
	if err != nil {
		_ = watcher.Close()
		return err
	}

	for {
		select {
		case event := <-watcher.Events:
			if event.Has(fsnotify.Write) {
				err = fkg.readAndRemoveKeys()
				if err != nil {
					_ = watcher.Close()
					return err
				}
			}
		case err, ok := <-watcher.Errors:
			if !ok {
				_ = watcher.Close()
				return err
			}
		}
	}
}

func (fkg *FileSystemKeyGatherer) readAndRemoveKeys() error {
	entries, err := os.ReadDir(fkg.dirPath)
	if err != nil {
		return err
	}

	for _, entry := range entries {
		if entry.IsDir() {
			return fmt.Errorf("directory %s contains a subdirectory %s. This is not supported", fkg.dirPath, entry.Name())
		}

		entryFilePath := filepath.Join(fkg.dirPath, entry.Name())

		keyVal, err := os.ReadFile(entryFilePath)
		if err != nil {
			return err
		}

		keyId := []byte(entry.Name())

		err = fkg.distributeKey(keyId, keyVal)
		if err != nil {
			return err
		}

		err = os.Remove(entryFilePath)
		if err != nil {
			return err
		}
	}

	return nil
}
