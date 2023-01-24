package main

import (
	"fmt"
	"log"
	"net/http"
	"path/filepath"
)

const PORT = 3030

func serveStatic(w http.ResponseWriter, r *http.Request) {
	log.Println("serve: ", r.URL.Path)
	filename := r.URL.Path
	http.ServeFile(w, r, filepath.Join("static", filename))
	return
}

func main() {
	http.HandleFunc("/", serveStatic)
	log.Printf("starting server at port %v", PORT)
	log.Fatal(http.ListenAndServe(fmt.Sprintf(":%v", PORT), nil))
}
