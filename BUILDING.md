# Building RiftForge Desktop

RiftForge Desktop packages the React client in a Tauri 2 window and starts the
Spring Boot game server with a stripped Java 21 runtime bundled inside the
application. Players do not need to install Java separately.

## Prerequisites

- Node.js and npm
- Java 21 with `jlink` on `PATH` (Oracle JDK, Temurin, or another full JDK)
- Maven
- Rust toolchain installed with `rustup`
- Tauri CLI v2, available through the project's npm dependencies
- Windows only: Visual Studio 2022 Build Tools with the **Desktop development
  with C++** workload

Verify the desktop-specific tools:

```bash
java -version
jlink --version
mvn -version
cargo --version
rustc -vV
npx tauri --version
```

Set `JAVA_HOME` to the Java 21 JDK before building. On Windows PowerShell:

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
```

Install project dependencies before the first build:

```bash
npm install
```

## Pre-built Downloads

Packaged installers are distributed through
[GitHub Releases](https://github.com/xerioc2/riftforgetcg/releases). The
repository contains source code and build scripts; generated installers are not
tracked in normal Git history.

Download the latest Windows installer from the latest release and run it - no
prerequisites required. The installer bundles a stripped Java 21 runtime; players
do not need Java installed.

## Build An Installer

The desktop scripts run the required frontend and backend builds for packaging.
For a quick local validation before packaging, run each build directly:

```bash
npm run build
```

```bash
cd server
mvn -q -DskipTests package
```

Windows PowerShell:

```powershell
./scripts/build-desktop.ps1
```

macOS or Linux:

```bash
./scripts/build-desktop.sh
```

The script builds the Spring Boot fat JAR, creates a stripped Java runtime with
`jlink`, stages both under `src-tauri/binaries/`, and builds the platform
installer. Output is written under:

```text
src-tauri/target/release/bundle/
```

The Windows script produces:

```text
src-tauri/target/release/bundle/nsis/RiftForge_<version>_x64-setup.exe
```

Typical outputs on other platforms are `.dmg` files on macOS and
`.deb`/`.AppImage` packages on Linux.

## Publishing A Release

Generated installer binaries should be uploaded as GitHub Release assets:

1. Run validation:
   ```bash
   npm run build
   cd server
   mvn -q test
   mvn -q -DskipTests compile
   ```
2. Build the desktop app with the commands above.
3. Generate checksums for release assets. On Windows:
   ```powershell
   Get-FileHash .\src-tauri\target\release\bundle\nsis\RiftForge_<version>_x64-setup.exe -Algorithm SHA256
   ```
4. Create a release tag, for example `v0.1.0-alpha`.
5. Create a GitHub Release for that tag.
6. Use [docs/RELEASE_TEMPLATE.md](docs/RELEASE_TEMPLATE.md) for release notes.
7. Upload the generated installer from `src-tauri/target/release/bundle/` as a
   release asset and include checksums in the release notes.

Do not commit generated installers to the repository. Git LFS is not needed
unless future large source assets must remain versioned with the repo. If the
repository size becomes a problem later, consider a separate history-cleanup
task; do not rewrite history as part of routine release publishing.

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
bundled production server. The usual browser workflow with `npm run dev` also
remains unchanged.

## Notes

- Do not commit files under `src-tauri/binaries/`; the JRE and server JAR are
  platform-specific build output.
- Do not commit generated installers such as `RiftForgeInstaller.exe`; attach
  them to GitHub Releases instead.
- The packaged frontend connects to the bundled server at
  `http://localhost:8080`.
- Closing the desktop window stops the bundled Java server process.
