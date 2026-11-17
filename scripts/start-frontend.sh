#!/usr/bin/env bash
set -e
cd "$(dirname "${BASH_SOURCE[0]}")/../frontend"
[ -d node_modules ] || pnpm install
exec pnpm dev
