#!/usr/bin/env bash
set -euo pipefail

echo "Building native server (this takes 3-8 minutes)..."
(cd server && mvn -Pnative -DskipTests package)

triple=$(rustc -vV | grep "^host:" | cut -d' ' -f2)
mkdir -p src-tauri/binaries
cp server/target/riftforge-server "src-tauri/binaries/riftforge-server-$triple"
chmod +x "src-tauri/binaries/riftforge-server-$triple"
echo "Sidecar placed at src-tauri/binaries/riftforge-server-$triple"

npm run tauri:build
echo "Done. Installer is in src-tauri/target/release/bundle/"
