package gatherers

import (
	"fmt"
	"log"
	"os"
	"path/filepath"
	"sort"
	"time"

	"github.com/fsnotify/fsnotify"
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
	if err != nil {
		panic(err)
	}

	err = watcher.Add(fkg.dirPath)
	if err != nil {
		_ = watcher.Close()
		return err
	}

	ticker := time.NewTicker(5 * time.Second)
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
		case <-ticker.C:
			log.Println("keys gathered from filesystem: ", fkg.keysGathered)
			fkg.keysGathered = 0
		}

	}
}

type keyFile struct {
	fileName string
	keyId    []byte
	keyValue []byte
	modTime  time.Time
}

func (fkg *FileSystemKeyGatherer) readAndRemoveKeys() error {
	entries, err := os.ReadDir(fkg.dirPath)
	if err != nil {
		return err
	}

	fmt.Println("entry count:", len(entries))
	fmt.Println("reading entries")
	var keyFiles []keyFile
	for _, entry := range entries {
		if entry.IsDir() {
			return fmt.Errorf("directory %s contains a subdirectory %s. This is not supported", fkg.dirPath, entry.Name())
		}
		fileName := entry.Name()
		filePath := filepath.Join(fkg.dirPath, fileName)

		keyId := []byte(fileName)
		if len(keyId) > 128 { // ASN.1 SEQUENCE can't be longer than 128 bytes
			keyId = keyId[:128]
		}

		keyVal, err := os.ReadFile(filePath)
		if err != nil {
			return err
		}
		if len(keyVal) > 128 { // ASN.1 SEQUENCE can't be longer than 128 bytes
			keyVal = keyVal[:128]
		}

		entryStat, err := os.Stat(filePath)
		if err != nil {
			return err
		}

		modTime := entryStat.ModTime()

		keyFiles = append(keyFiles, keyFile{keyId: keyId, keyValue: keyVal, modTime: modTime, fileName: fileName})
	}

	fmt.Println("sorting entries")
	sort.Slice(keyFiles, func(i, j int) bool {
		return keyFiles[i].modTime.Before(keyFiles[j].modTime)
	})

	for _, keys := range keyFiles {
		err = fkg.distributeKey(keys.keyId, keys.keyValue)
		if err != nil {
			return err
		}

		filePath := filepath.Join(fkg.dirPath, keys.fileName)
		if err != nil {
			return err
		}

		err = os.Remove(filePath)
		if err != nil {
			return err
		}
	}

	return nil
}
