package main

type KeyManager struct {
	data map[string]string
}

func (k *KeyManager) add(id, val string) {
}

func (k *KeyManager) getVal(id string) (string, error) {
	return "", nil
}

func (k *KeyManager) getAll() map[string]string {
	return k.data
}

func InitKeyManager(config Configuration) KeyManager {
	return KeyManager{}
}
