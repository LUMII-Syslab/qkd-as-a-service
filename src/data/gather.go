package data

import (
	"crypto/rand"
	"encoding/base64"
	"fmt"
	"log"
	math_rand "math/rand"
	"time"

	zmq "github.com/pebbe/zmq4"
	"github.com/vmihailenco/msgpack/v5"
)

func GatherClavisKeys(keys KeyManager, url string) {
	zctx, err := zmq.NewContext()
	if err != nil {
		log.Panicln(err)
	}

	zs, err := zctx.NewSocket(zmq.SUB)
	if err != nil {
		log.Panicln(err)
	}
	err = zs.SetSubscribe("")
	if err != nil {
		log.Panicln(err)
	}
	defer zs.Close()
	log.Println("created a new zeromq socket")

	log.Println("attempting to connect to", url)
	err = zs.Connect(url)
	if err != nil {
		log.Panicln(err)
	}
	log.Println("connected zeromq to", url)

	for i := 0; i < 10; i++ {
		msg_b, err := zs.RecvBytes(0)
		if err != nil {
			log.Panic(err)
		}
		var msg []interface{}
		err = msgpack.Unmarshal(msg_b, &msg)
		if err != nil {
			log.Panic(err)
		}
		if len(msg) != 2 {
			continue
		}

		var keyId string
		switch t := msg[0].(type) {
		case string:
			keyId = t
		}

		var vals []uint8

		switch t := msg[1].(type) {
		case []interface{}:
			for _, v := range t {
				switch t2 := v.(type) {
				case int8:
					vals = append(vals, uint8(t2))
				case uint8:
					vals = append(vals, t2)
				}
			}
		}

		keyVal := base64.RawStdEncoding.EncodeToString(vals)
		fmt.Printf("\tk: %v \r", keyVal)

		keys.add([]byte(keyId), []byte(keyVal)) // TODO the conversion to base64 has to be removed
	}

}

func generateRandomBytes(n int) ([]byte, error) {
	b := make([]byte, n)
	rand.Read(b)
	return b, nil
}

func genRandomBase64Str(n int) string {
	b, _ := generateRandomBytes(n)
	return base64.RawStdEncoding.EncodeToString(b)
}

func GatherRandomKeys(keys KeyManager) {
	for {
		keyId, keyVal := genRandomBase64Str(5), genRandomBase64Str(10)
		fmt.Printf("\tk: %v \r", keyVal)
		time.Sleep(time.Duration(math_rand.Float32() * 3000000000))
		keys.add([]byte(keyId), []byte(keyVal)) // TODO the conversion to base64 has to be removed
	}
}
