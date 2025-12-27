package main

import (
	"crypto/sha256"
	"encoding/hex"
	"encoding/json"
	"io"
	"log"
	"net/http"
	"os"
	"path/filepath"
	"sync"
	"time"

	"github.com/gorilla/mux"
)

type Block struct {
	Index     int       `json:"index"`
	Timestamp time.Time `json:"timestamp"`
	FileName  string    `json:"file_name"`
	NFSPath   string    `json:"nfs_path"`
	FileHash  string    `json:"file_hash"`
	ISCSI     string    `json:"iscsi,omitempty"` // optional: iqn or LUN id
	PrevHash  string    `json:"prev_hash"`
	Hash      string    `json:"hash"`
}

type Chain struct {
	Blocks []Block `json:"blocks"`
	mu     sync.Mutex
}

func (c *Chain) load(path string) {
	f, err := os.Open(path)
	if err != nil {
		return
	}
	defer f.Close()
	json.NewDecoder(f).Decode(&c.Blocks)
}

func (c *Chain) save(path string) {
	c.mu.Lock()
	defer c.mu.Unlock()
	tmp := path + ".tmp"
	f, err := os.Create(tmp)
	if err != nil {
		log.Printf("save error: %v", err)
		return
	}
	enc := json.NewEncoder(f)
	enc.SetIndent("", "  ")
	if err := enc.Encode(c.Blocks); err != nil {
		log.Printf("encode error: %v", err)
	}
	f.Close()
	os.Rename(tmp, path)
}

func (c *Chain) lastHash() string {
	if len(c.Blocks) == 0 {
		return ""
	}
	return c.Blocks[len(c.Blocks)-1].Hash
}

func hashBlock(b Block) string {
	h := sha256.New()
	_ = json.NewEncoder(h).Encode(b.Index)
	_ = json.NewEncoder(h).Encode(b.Timestamp.UnixNano())
	_ = json.NewEncoder(h).Encode(b.FileName)
	_ = json.NewEncoder(h).Encode(b.NFSPath)
	_ = json.NewEncoder(h).Encode(b.FileHash)
	_ = json.NewEncoder(h).Encode(b.ISCSI)
	_ = json.NewEncoder(h).Encode(b.PrevHash)
	return hex.EncodeToString(h.Sum(nil))
}

func main() {
	storageDir := os.Getenv("STORAGE_DIR")
	if storageDir == "" {
		storageDir = "/data"
	}
	chainFile := os.Getenv("CHAIN_FILE")
	if chainFile == "" {
		chainFile = filepath.Join(storageDir, "chain.json")
	}

	if err := os.MkdirAll(storageDir, 0755); err != nil {
		log.Fatalf("cannot create storage dir: %v", err)
	}

	chain := &Chain{}
	chain.load(chainFile)

	r := mux.NewRouter()
	r.HandleFunc("/upload", func(w http.ResponseWriter, r *http.Request) {
		// multipart form upload
		if err := r.ParseMultipartForm(1 << 30); err != nil {
			http.Error(w, "could not parse form", http.StatusBadRequest)
			return
		}
		file, header, err := r.FormFile("file")
		if err != nil {
			http.Error(w, "file required", http.StatusBadRequest)
			return
		}
		defer file.Close()

		// optional iscsi identifier provided by operator who created a LUN already
		iscsiID := r.FormValue("iscsi") // e.g., "iqn.2025-12.example:target1/lun0" or similar

		// Save file to storageDir
		outPath := filepath.Join(storageDir, header.Filename)
		outFile, err := os.Create(outPath)
		if err != nil {
			http.Error(w, "cannot write file", http.StatusInternalServerError)
			return
		}
		h := sha256.New()
		mw := io.MultiWriter(outFile, h)
		if _, err := io.Copy(mw, file); err != nil {
			outFile.Close()
			http.Error(w, "error saving file", http.StatusInternalServerError)
			return
		}
		outFile.Close()
		sum := hex.EncodeToString(h.Sum(nil))

		// Create block
		chain.mu.Lock()
		idx := len(chain.Blocks)
		prev := chain.lastHash()
		block := Block{
			Index:     idx,
			Timestamp: time.Now().UTC(),
			FileName:  header.Filename,
			NFSPath:   outPath,
			FileHash:  sum,
			ISCSI:     iscsiID,
			PrevHash:  prev,
		}
		block.Hash = hashBlock(block)
		chain.Blocks = append(chain.Blocks, block)
		chain.mu.Unlock()

		chain.save(chainFile)

		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(block)
	}).Methods("POST")

	r.HandleFunc("/chain", func(w http.ResponseWriter, r *http.Request) {
		chain.mu.Lock()
		defer chain.mu.Unlock()
		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(chain.Blocks)
	}).Methods("GET")

	// simple health
	r.HandleFunc("/health", func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusOK)
		w.Write([]byte("ok"))
	}).Methods("GET")

	log.Println("starting node on :8080, storage:", storageDir)
	log.Fatal(http.ListenAndServe(":8080", r))
}