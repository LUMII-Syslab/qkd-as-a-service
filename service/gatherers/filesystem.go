package gatherers

import (
	"fmt"
	"github.com/fsnotify/fsnotify"
	"log"
	"os"
	"path/filepath"
	"sort"
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

	sort.Slice(entries, func(i, j int) bool {
		// compare modified date
		iStat, err := os.Stat(filepath.Join(fkg.dirPath, entries[i].Name()))
		if err != nil {
			panic(err)
		}
		jStat, err := os.Stat(filepath.Join(fkg.dirPath, entries[j].Name()))
		if err != nil {
			panic(err)
		}
		return iStat.ModTime().Before(jStat.ModTime())
	})

	for _, entry := range entries {
		if entry.IsDir() {
			return fmt.Errorf("directory %s contains a subdirectory %s. This is not supported", fkg.dirPath, entry.Name())
		}

		log.Println(entry.Name())
		entryFilePath := filepath.Join(fkg.dirPath, entry.Name())

		keyVal, err := os.ReadFile(entryFilePath)
		if len(keyVal) > 128 { // ASN.1 SEQUENCE can't be longer than 128 bytes
			keyVal = keyVal[:128]
		}

		if err != nil {
			return err
		}

		keyId := []byte(entry.Name())
		if len(keyId) > 128 { // ASN.1 SEQUENCE can't be longer than 128 bytes
			keyId = keyId[:128]
		}

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
