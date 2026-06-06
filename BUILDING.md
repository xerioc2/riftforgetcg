# Building RiftForge Desktop

RiftForge Desktop packages the React client in a Tauri 2 window and starts the
Spring Boot game server as a GraalVM native sidecar. The packaged application
does not require a Java runtime.

## Prerequisites

- Node.js and npm
- Maven and Java 21
- GraalVM 21+ with `native-image` on `PATH`
- Rust toolchain installed with `rustup`
- Tauri CLI v2, available through the project's npm dependencies
- Windows only: Visual Studio 2022 Build Tools with the **Desktop development
  with C++** workload

Verify the desktop-specific tools:

```bash
native-image --version
cargo --version
rustc -vV
npx tauri --version
```

Install project dependencies before the first build:

```bash
npm install
```

## Build An Installer

Windows PowerShell:

```powershell
./scripts/build-desktop.ps1
```

macOS or Linux:

```bash
./scripts/build-desktop.sh
```

The script builds the GraalVM native server, copies it into Tauri's ignored
sidecar directory using the current Rust target triple, and builds the platform
installer. Output is written under:

```text
src-tauri/target/release/bundle/
```

Typical outputs are `.exe` installers on Windows, `.dmg` files on macOS, and
`.deb`/`.AppImage` packages on Linux.

## Development

Desktop development continues to use the normal JVM server:

```bash
cd server
mvn spring-boot:run
```

In a second terminal:

```bash
npm run tauri:dev
```

`tauri:dev` uses the Vite development server and does not build or launch the
native sidecar. The usual browser workflow with `npm run dev` also remains
unchanged.

## Notes

- Do not commit files under `src-tauri/binaries/`; sidecars are
  platform-specific build output.
- Native compilation can take several minutes.
- The packaged frontend connects to the local sidecar at
  `http://localhost:8080`.
