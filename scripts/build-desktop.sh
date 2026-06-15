#!/usr/bin/env bash
set -euo pipefail

if [[ -z "${VITE_BUILD_DATE:-}" ]]; then
  export VITE_BUILD_DATE="$(date -u +%F)"
fi
echo "Using frontend build date $VITE_BUILD_DATE"
echo "Building RiftForge desktop package. Tauri will rebuild and verify the bundled server before packaging..."
npm run build:desktop
echo "Done. Installer is in src-tauri/target/release/bundle/"
