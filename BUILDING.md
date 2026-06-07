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

## Build An Installer

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
- The packaged frontend connects to the bundled server at
  `http://localhost:8080`.
- Closing the desktop window stops the bundled Java server process.
