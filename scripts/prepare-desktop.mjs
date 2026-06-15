import { createHash } from 'node:crypto';
import fs from 'node:fs';
import path from 'node:path';
import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const ROOT = path.resolve(__dirname, '..');
const BUNDLED_JAR = path.join(ROOT, 'src-tauri', 'binaries', 'riftforge-server.jar');
const SERVER_SRC_MAIN = path.join(ROOT, 'server', 'src', 'main');
const SERVER_JRE = path.join(ROOT, 'server', 'target', 'server-jre');
const BUNDLED_JRE = path.join(ROOT, 'src-tauri', 'binaries', 'jre');
const SERVER_VERSION = readServerVersion();
const SERVER_JAR = path.join(ROOT, 'server', 'target', `riftforge-server-${SERVER_VERSION}.jar`);

const MODULES = [
  'java.base',
  'java.logging',
  'java.xml',
  'java.naming',
  'java.management',
  'java.instrument',
  'jdk.unsupported',
  'java.sql',
  'jdk.crypto.ec',
  'java.security.jgss',
  'java.net.http',
  'jdk.localedata',
  'java.desktop',
  'jdk.zipfs',
].join(',');

function isWindows() {
  return process.platform === 'win32';
}

function commandName(name) {
  return isWindows() ? `${name}.cmd` : name;
}

function jlinkPath() {
  const exe = isWindows() ? 'jlink.exe' : 'jlink';
  return process.env.JAVA_HOME ? path.join(process.env.JAVA_HOME, 'bin', exe) : exe;
}

function readServerVersion() {
  const pom = fs.readFileSync(path.join(ROOT, 'server', 'pom.xml'), 'utf8');
  const match = pom.match(/<artifactId>riftforge-server<\/artifactId>\s*<version>([^<]+)<\/version>/);
  if (!match) throw new Error('Could not determine server version from server/pom.xml.');
  return match[1];
}

function run(command, args, options = {}) {
  console.log(`[desktop] ${command} ${args.join(' ')}`);
  const useCmdShim = isWindows() && command.endsWith('.cmd');
  const result = spawnSync(useCmdShim ? 'cmd.exe' : command, useCmdShim ? ['/d', '/s', '/c', command, ...args] : args, {
    cwd: options.cwd ?? ROOT,
    env: options.env ?? process.env,
    stdio: 'inherit',
    shell: false,
  });
  if (result.error) throw result.error;
  if (result.status !== 0) {
    throw new Error(`${command} ${args.join(' ')} failed with exit code ${result.status}.`);
  }
}

function fileInfo(filePath) {
  const stat = fs.statSync(filePath);
  return {
    path: filePath,
    size: stat.size,
    mtimeMs: stat.mtimeMs,
    mtime: stat.mtime.toISOString(),
  };
}

function printFileInfo(label, filePath) {
  const info = fileInfo(filePath);
  console.log(`[desktop] ${label}: ${info.path}`);
  console.log(`[desktop] ${label} size: ${info.size} bytes`);
  console.log(`[desktop] ${label} modified: ${info.mtime}`);
}

function sha256(filePath) {
  return createHash('sha256').update(fs.readFileSync(filePath)).digest('hex');
}

function newestFileUnder(dir) {
  let newest = null;
  const stack = [dir];
  while (stack.length > 0) {
    const current = stack.pop();
    for (const entry of fs.readdirSync(current, { withFileTypes: true })) {
      const fullPath = path.join(current, entry.name);
      if (entry.isDirectory()) {
        stack.push(fullPath);
      } else if (entry.isFile()) {
        const info = fileInfo(fullPath);
        if (!newest || info.mtimeMs > newest.mtimeMs) newest = info;
      }
    }
  }
  if (!newest) throw new Error(`No source files found under ${dir}.`);
  return newest;
}

export function verifyBundledServer({ requireBuiltJarMatch = true } = {}) {
  if (!fs.existsSync(BUNDLED_JAR)) {
    throw new Error(`Bundled server JAR is missing: ${BUNDLED_JAR}`);
  }

  printFileInfo('Bundled server JAR', BUNDLED_JAR);

  if (fs.existsSync(SERVER_JAR)) {
    printFileInfo('Fresh server JAR', SERVER_JAR);
    const sourceHash = sha256(SERVER_JAR);
    const bundledHash = sha256(BUNDLED_JAR);
    if (sourceHash !== bundledHash) {
      throw new Error(`Bundled server JAR is not byte-identical to ${SERVER_JAR}. Rebuild desktop packaging before creating an installer.`);
    }
    console.log(`[desktop] Bundled server JAR SHA-256: ${bundledHash}`);
  } else if (requireBuiltJarMatch) {
    throw new Error(`Expected freshly built server JAR is missing: ${SERVER_JAR}`);
  } else {
    console.warn(`[desktop] Fresh server JAR is missing, so byte comparison was skipped: ${SERVER_JAR}`);
  }

  const newestSource = newestFileUnder(SERVER_SRC_MAIN);
  const bundledInfo = fileInfo(BUNDLED_JAR);
  console.log(`[desktop] Newest server source: ${newestSource.path}`);
  console.log(`[desktop] Newest server source modified: ${newestSource.mtime}`);
  if (bundledInfo.mtimeMs < newestSource.mtimeMs) {
    throw new Error(
      `Bundled server JAR is stale. ${BUNDLED_JAR} is older than server source ${newestSource.path}. ` +
        'Run npm run build:desktop so the server is rebuilt and refreshed before packaging.',
    );
  }

  console.log('[desktop] Bundled server JAR freshness check passed.');
}

export function prepareDesktop() {
  console.log('[desktop] Building server JAR...');
  run(commandName('mvn'), [
    '-DskipTests',
    `-Driftforge.build.gitSha=${process.env.RIFTFORGE_BUILD_GIT_SHA || 'local-dev'}`,
    `-Driftforge.build.fullGitSha=${process.env.RIFTFORGE_BUILD_FULL_GIT_SHA || 'local-dev'}`,
    `-Driftforge.build.timestamp=${process.env.RIFTFORGE_BUILD_TIMESTAMP || 'local-dev'}`,
    `-Driftforge.releaseTag=${process.env.RIFTFORGE_RELEASE_TAG || 'local-dev'}`,
    'package',
  ], { cwd: path.join(ROOT, 'server') });

  if (!fs.existsSync(SERVER_JAR)) {
    throw new Error(`Maven completed but expected server JAR was not created: ${SERVER_JAR}`);
  }
  printFileInfo('Fresh server JAR', SERVER_JAR);

  console.log('[desktop] Creating stripped JRE with jlink...');
  fs.rmSync(SERVER_JRE, { recursive: true, force: true });
  run(jlinkPath(), [
    '--add-modules',
    MODULES,
    '--output',
    SERVER_JRE,
    '--strip-debug',
    '--no-man-pages',
    '--no-header-files',
    '--compress=2',
  ]);

  fs.mkdirSync(path.dirname(BUNDLED_JAR), { recursive: true });
  fs.rmSync(BUNDLED_JRE, { recursive: true, force: true });
  fs.cpSync(SERVER_JRE, BUNDLED_JRE, { recursive: true });
  fs.copyFileSync(SERVER_JAR, BUNDLED_JAR);
  console.log('[desktop] JRE and JAR placed in src-tauri/binaries/.');

  verifyBundledServer();
}

if (process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  prepareDesktop();
}
