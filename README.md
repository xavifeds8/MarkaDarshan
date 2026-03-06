# Markdown Preview Server

A lightweight, self-contained Markdown preview and editor server built with Java. Browse your local filesystem, preview `.md` files with syntax highlighting, and edit them with a live split-pane editor — all from your browser.

## Features

- 📁 **File Browser** — Navigate directories, create files/folders, rename, delete, drag-and-drop move
- 📝 **Markdown Preview** — GitHub-flavored Markdown rendering with syntax-highlighted code blocks
- ✏️ **Live Editor** — Split-pane editor with real-time preview
- 🔒 **Path Traversal Protection** — Sandboxed to the configured root directory
- 🚀 **Zero Dependencies** — Uses only JDK built-in classes, no external libraries needed
- 🐳 **Docker Ready** — Multi-stage Dockerfile for easy containerized deployment

## Quick Start

### Prerequisites

- Java 17+
- Gradle 8+ (or use the wrapper)

### Run from Source

```bash
# Build and run
./gradlew run

# Run with a specific root directory
./gradlew run --args="/path/to/your/docs"

# Run with custom port
./gradlew run --args="/path/to/docs --port=3000"
```

Then open [http://localhost:8080](http://localhost:8080) in your browser.

### Build a Fat JAR

```bash
./gradlew fatJar
java -jar build/libs/MarkdownPreview-1.0.0-all.jar /path/to/docs
```

### Docker

```bash
# Build the image
docker build -t markdown-preview .

# Run with a mounted volume
docker run -p 8080:8080 -v /path/to/your/docs:/data markdown-preview
```

## Configuration

| Option | CLI Argument | Environment Variable | Default |
|---|---|---|---|
| Root directory | First positional arg | `MD_PREVIEW_ROOT` | User home directory |
| Port | `--port=8080` | `MD_PREVIEW_PORT` | `8080` |
| Max file size | `--max-file-size=10485760` | `MD_PREVIEW_MAX_FILE_SIZE` | 10 MB |
| Max request body | `--max-request-body=10485760` | `MD_PREVIEW_MAX_REQUEST_BODY` | 10 MB |

CLI arguments take precedence over environment variables.

## Project Structure

```
src/main/java/com/markdownpreview/
├── MarkdownPreviewApp.java          # Entry point
├── config/
│   └── AppConfig.java               # Configuration (CLI args + env vars)
├── server/
│   └── HttpServerFactory.java       # Server setup & route registration
├── handler/
│   ├── BaseHandler.java             # Centralized error handling
│   ├── StaticHandler.java           # Serves frontend HTML
│   ├── FileListHandler.java         # GET  /api/files
│   ├── ContentHandler.java          # GET  /api/content
│   ├── SaveHandler.java             # POST /api/save
│   ├── MkdirHandler.java            # POST /api/mkdir
│   ├── RenameHandler.java           # POST /api/rename
│   ├── DeleteHandler.java           # POST /api/delete
│   └── MoveHandler.java             # POST /api/move
├── service/
│   └── FileService.java             # Business logic for file operations
└── util/
    ├── HttpUtils.java               # HTTP response helpers
    ├── JsonUtils.java               # Lightweight JSON serialization/parsing
    └── PathValidator.java           # Path traversal prevention

src/main/resources/
└── frontend.html                    # Single-page frontend app

src/test/java/com/markdownpreview/
├── service/
│   └── FileServiceTest.java         # FileService unit tests
└── util/
    └── PathValidatorTest.java       # PathValidator unit tests
```

## API Endpoints

| Method | Path | Description |
|---|---|---|
| `GET` | `/` | Serves the frontend SPA |
| `GET` | `/api/files?path=...` | List directory contents |
| `GET` | `/api/content?path=...` | Read file content |
| `POST` | `/api/save` | Save file (`{ path, content }`) |
| `POST` | `/api/mkdir` | Create directory (`{ path }`) |
| `POST` | `/api/rename` | Rename file/dir (`{ oldPath, newName }`) |
| `POST` | `/api/delete` | Delete file/dir (`{ path }`) |
| `POST` | `/api/move` | Move file/dir (`{ sourcePath, targetPath }`) |

## Running Tests

```bash
./gradlew test
```

## License

MIT
