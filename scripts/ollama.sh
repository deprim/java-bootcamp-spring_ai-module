#!/bin/sh
# Use system Ollama when available, otherwise the project-local official runtime.
set -eu
project_dir=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
if command -v ollama >/dev/null 2>&1; then
    exec ollama "$@"
fi
runtime="$project_dir/.local/ollama/bin/ollama"
if [ ! -x "$runtime" ]; then
    echo "Ollama is not installed. Follow the installation steps in README.md." >&2
    exit 1
fi
export OLLAMA_MODELS="${OLLAMA_MODELS:-$project_dir/.local/ollama/models}"
export OLLAMA_HOST="${OLLAMA_HOST:-127.0.0.1:11434}"
export OLLAMA_NO_CLOUD="${OLLAMA_NO_CLOUD:-1}"
exec "$runtime" "$@"
