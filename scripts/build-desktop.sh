#!/usr/bin/env bash
set -euo pipefail

echo "Building server JAR..."
(cd server && mvn -DskipTests package)

echo "Creating stripped JRE with jlink..."
MODULES="java.base,java.logging,java.xml,java.naming,java.management,java.instrument,jdk.unsupported,java.sql,jdk.crypto.ec,java.security.jgss,java.net.http,jdk.localedata,java.desktop,jdk.zipfs"
JRE_OUT="server/target/server-jre"
rm -rf "$JRE_OUT"
"$JAVA_HOME/bin/jlink" --add-modules "$MODULES" --output "$JRE_OUT" \
  --strip-debug --no-man-pages --no-header-files --compress=2

mkdir -p src-tauri/binaries
rm -rf src-tauri/binaries/jre
cp -r "$JRE_OUT" src-tauri/binaries/jre
cp server/target/riftforge-server-0.1.0.jar src-tauri/binaries/riftforge-server.jar
echo "JRE and JAR placed in src-tauri/binaries/"

npm run tauri:build
echo "Done. Installer is in src-tauri/target/release/bundle/"
