# VERSION.sh — единый источник версии (читается release.sh, build.sh и документацией).
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
VERSION="$(tr -d '[:space:]' < "$ROOT/VERSION.txt" 2>/dev/null || echo v1.0)"
export LOOM_VERSION="$VERSION"
