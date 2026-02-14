# Filesystem & Storage: Comiqueta
[Voltar ao Índice](./INDEX.md)

Comiqueta is a local-first comic reader. Efficient file handling and Scoped Storage compliance are critical.

## 📂 Storage Strategy

- **Scoped Storage**: Full compliance using `DocumentFile` and SAF (Storage Access Framework).
- **Core Wrapper**: `core:data` contains utilities for interacting with URIs safely.
- **Root Directory**: User-selected directory via `ACTION_OPEN_DOCUMENT_TREE`.

## 📚 Comic Formats

| Format | Handling Strategy |
| :--- | :--- |
| **CBZ** | Standard ZIP extraction (streamed). |
| **CBR** | RAR extraction (requires native or pure Kotlin library). |
| **PDF** | Android `PdfRenderer` (Future/Optional). |

## 🛠️ IO Operations

- **Concurrency**: All IO must run on `Dispatchers.IO`.
- **Gaurds**: Use `runCatching` for all file operations to prevent crashes on missing permissions or corrupted files.
- **Metadata**: Stored in Room (cached) to avoid re-scanning the entire storage on every launch.

## 📝 Rules
1. Never use `java.io.File` for external storage.
2. Use `DocumentFile.fromTreeUri` for directory traversal.
3. Always check for `Uri` persistence (Take Persistable URI Permissions).

---
Status: **Active**
Last Updated: 2026-02-08
