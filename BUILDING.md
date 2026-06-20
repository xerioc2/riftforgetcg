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

Use one desktop build entry point:

```bash
npm run build:desktop
```

Tauri is configured to run `npm run build:tauri-before` before every desktop
package build. That command:

1. runs `mvn -DskipTests package` for the Spring Boot server,
2. verifies `server/target/riftforge-server-0.1.0.jar` exists,
3. rebuilds the stripped Java runtime with `jlink`,
4. copies the freshly built server JAR to `src-tauri/binaries/riftforge-server.jar`,
5. verifies the bundled JAR is byte-identical to the fresh server JAR,
6. fails if the bundled JAR is older than anything under `server/src/main`, and
7. builds the frontend.

This guard is intentionally part of Tauri's `beforeBuildCommand`, so direct
commands such as `npm run tauri:build` or `npx tauri build` also refresh and
verify the bundled server before creating an installer. Do not build the
installer by manually invoking lower-level Tauri commands with the guard removed.

For a quick local validation before packaging, run each build directly:

```bash
npm run build
```

```bash
cd server
mvn -q -DskipTests package
```

Windows PowerShell wrapper:

```powershell
./scripts/build-desktop.ps1
```

macOS or Linux wrapper:

```bash
./scripts/build-desktop.sh
```

The wrappers call the same guarded `npm run build:desktop` path. Output is
written under:

```text
src-tauri/target/release/bundle/
```

The Windows script produces:

```text
src-tauri/target/release/bundle/nsis/RiftForge_<version>_x64-setup.exe
```

For the normal Windows playtest build loop, use:

```powershell
npm.cmd run build:release
```

That PowerShell wrapper cleans `server/target`, deletes any old staged server
JAR, optionally runs backend tests, invokes the guarded desktop build, verifies
the fresh and bundled JAR SHA-256 values match, verifies timestamps, and copies
the generated NSIS installer to the repository root. A faster local rebuild is
available when tests already ran:

```powershell
npm.cmd run build:release:fast
```

Typical outputs on other platforms are `.dmg` files on macOS and
`.deb`/`.AppImage` packages on Linux.

## Publishing A Release

Each playtester release must have a unique version and tag. Before building,
bump these together:

- `package.json` `version`
- `src-tauri/tauri.conf.json` `version`
- `server/pom.xml` `version`

Use a GitHub Release tag such as `v0.1.1-alpha`. The release command injects
that tag into the client as `VITE_APP_VERSION`; the latest-release banner uses
that value to tell older installed builds when a newer GitHub Release exists.
If the version is not bumped, old and new installers are indistinguishable to
testers.

Generated installer binaries should be uploaded as GitHub Release assets:

1. Run validation:
   ```bash
   npm run build
   npm run test:frontend
   cd server
   mvn -q test
   mvn -q -DskipTests compile
   ```
2. Confirm the Git tree is clean:
   ```bash
   git status --short
   ```
3. Build release assets:
   ```bash
   npm run release:desktop
   ```
   This command aborts if the tree has uncommitted or untracked files. It stamps
   the client and server with the current `git rev-parse HEAD` SHA and UTC build
   timestamp, runs the guarded desktop build, verifies the bundled server JAR,
   and copies uniquely named upload assets into `release-assets/`.
   For local playtest installer rebuilds that do not require the clean-tree
   publishing gate, use `npm.cmd run build:release`; it still rebuilds and
   verifies the backend JAR and places a copy of the NSIS installer in the repo
   root.
4. Generate checksums for release assets. On Windows:
   ```powershell
   Get-FileHash .\release-assets\RiftForge_<version>_x64-setup.exe -Algorithm SHA256
   ```
5. Create a release tag, for example `v0.1.1-alpha`.
6. Create a GitHub Release for that tag.
7. Use [docs/RELEASE_TEMPLATE.md](docs/RELEASE_TEMPLATE.md) for release notes.
8. Upload the versioned installer from `release-assets/` and include checksums
   in the release notes.

Pre-publish checklist:

1. Clean tree: `git status --short` prints nothing.
2. Version/tag bumped and committed.
3. `npm run release:desktop` passes.
4. `npm run verify:desktop-server` shows the bundled JAR timestamp is fresh and
   byte-identical to the compiled server JAR for the current HEAD.
5. Install the generated app and smoke test human-vs-RiftBot with Irelia Tempo:
   the "Choose your Battlefield (pick 1 of 3)" screen appears before mulligan.
6. Copy Debug Info shows the expected client build tag and server build SHA /
   timestamp matching `git rev-parse HEAD`, plus the running server JAR SHA-256.
7. Upload the versioned asset to GitHub Releases.
8. Verify an older installed build shows the update-available banner after the
   new release is published.

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
- `npm run verify:desktop-server` can be run manually to check that the staged
  server JAR is byte-identical to the latest `server/target` JAR when present
  and newer than the source under `server/src/main`.
- Release artifacts under `release-assets/` are generated outputs for upload to
  GitHub Releases and are ignored by Git.
- Do not commit generated installers such as `RiftForgeInstaller.exe`; attach
  them to GitHub Releases instead.
- The packaged frontend connects to the bundled server at
  `http://localhost:8080`.
- Closing the desktop window stops the bundled Java server process.
