import fs from 'node:fs';
import path from 'node:path';
import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import { verifyBundledServer } from './prepare-desktop.mjs';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const ROOT = path.resolve(__dirname, '..');
const RELEASE_ASSETS = path.join(ROOT, 'release-assets');

function run(command, args, options = {}) {
  console.log(`[release] ${command} ${args.join(' ')}`);
  const useCmdShim = process.platform === 'win32' && command.endsWith('.cmd');
  const result = spawnSync(useCmdShim ? 'cmd.exe' : command, useCmdShim ? ['/d', '/s', '/c', command, ...args] : args, {
    cwd: options.cwd ?? ROOT,
    env: options.env ?? process.env,
    stdio: options.stdio ?? 'inherit',
    encoding: options.encoding,
    shell: false,
  });
  if (result.error) throw result.error;
  if (result.status !== 0) {
    throw new Error(`${command} ${args.join(' ')} failed with exit code ${result.status}.`);
  }
  return result;
}

function commandName(name) {
  return process.platform === 'win32' ? `${name}.cmd` : name;
}

function output(command, args) {
  const result = spawnSync(command, args, { cwd: ROOT, encoding: 'utf8', stdio: ['ignore', 'pipe', 'pipe'] });
  if (result.status !== 0) throw new Error(result.stderr.trim() || `${command} ${args.join(' ')} failed.`);
  return result.stdout.trim();
}

function assertCleanGitTree() {
  const status = output('git', ['status', '--porcelain=v1', '--untracked-files=all']);
  if (!status) return;
  console.error('[release] Refusing to build a playtester release from a dirty tree.');
  console.error('[release] Commit or stash these files first:');
  console.error(status);
  throw new Error('Release builds require a clean committed tree.');
}

function readJson(file) {
  return JSON.parse(fs.readFileSync(path.join(ROOT, file), 'utf8'));
}

function readServerVersion() {
  const pom = fs.readFileSync(path.join(ROOT, 'server', 'pom.xml'), 'utf8');
  const match = pom.match(/<artifactId>riftforge-server<\/artifactId>\s*<version>([^<]+)<\/version>/);
  if (!match) throw new Error('Could not determine server version from server/pom.xml.');
  return match[1];
}

function stripPrerelease(version) {
  return version.replace(/^v/i, '').split('-')[0];
}

function assertVersionConsistency(packageVersion, tauriVersion, serverVersion) {
  const basePackage = stripPrerelease(packageVersion);
  if (basePackage !== tauriVersion || basePackage !== serverVersion) {
    throw new Error(
      `Version mismatch: package.json=${packageVersion}, tauri.conf.json=${tauriVersion}, server/pom.xml=${serverVersion}. ` +
        'Bump them together before cutting a playtester release.',
    );
  }
}

function copyReleaseAssets(releaseVersion) {
  const nsis = path.join(ROOT, 'src-tauri', 'target', 'release', 'bundle', 'nsis', `RiftForge_${stripPrerelease(releaseVersion)}_x64-setup.exe`);
  const msi = path.join(ROOT, 'src-tauri', 'target', 'release', 'bundle', 'msi', `RiftForge_${stripPrerelease(releaseVersion)}_x64_en-US.msi`);
  const jar = path.join(ROOT, 'src-tauri', 'binaries', 'riftforge-server.jar');
  fs.mkdirSync(RELEASE_ASSETS, { recursive: true });

  const copied = [];
  for (const [source, targetName] of [
    [nsis, `RiftForge_${releaseVersion}_x64-setup.exe`],
    [msi, `RiftForge_${releaseVersion}_x64_en-US.msi`],
  ]) {
    if (!fs.existsSync(source)) throw new Error(`Expected installer was not produced: ${source}`);
    if (fs.statSync(source).mtimeMs < fs.statSync(jar).mtimeMs) {
      throw new Error(`Installer ${source} is older than the bundled server JAR. Packaging did not include the freshly built server.`);
    }
    const destination = path.join(RELEASE_ASSETS, targetName);
    fs.copyFileSync(source, destination);
    copied.push(destination);
    console.log(`[release] Copied versioned asset: ${destination}`);
  }
  return copied;
}

assertCleanGitTree();

const packageJson = readJson('package.json');
const tauriConfig = readJson('src-tauri/tauri.conf.json');
const serverVersion = readServerVersion();
assertVersionConsistency(packageJson.version, tauriConfig.version, serverVersion);

const fullSha = output('git', ['rev-parse', 'HEAD']);
const shortSha = output('git', ['rev-parse', '--short', 'HEAD']);
const buildTimestamp = new Date().toISOString();
const releaseTag = process.env.RIFTFORGE_RELEASE_TAG || `v${packageJson.version}-alpha`;
const releaseVersion = releaseTag.replace(/^v/i, '');

console.log(`[release] Release tag: ${releaseTag}`);
console.log(`[release] Git HEAD: ${fullSha}`);
console.log(`[release] Build timestamp: ${buildTimestamp}`);

const env = {
  ...process.env,
  RIFTFORGE_RELEASE_MODE: '1',
  RIFTFORGE_RELEASE_TAG: releaseTag,
  RIFTFORGE_BUILD_GIT_SHA: shortSha,
  RIFTFORGE_BUILD_FULL_GIT_SHA: fullSha,
  RIFTFORGE_BUILD_TIMESTAMP: buildTimestamp,
  VITE_APP_VERSION: releaseTag,
  VITE_BUILD_TAG: shortSha,
  VITE_BUILD_DATE: buildTimestamp.slice(0, 10),
};

run(commandName('npm'), ['run', 'build:desktop'], { env });
verifyBundledServer();
const assets = copyReleaseAssets(releaseVersion);

const provenance = {
  releaseTag,
  fullSha,
  shortSha,
  buildTimestamp,
  packageVersion: packageJson.version,
  tauriVersion: tauriConfig.version,
  serverVersion,
  assets: assets.map((asset) => path.relative(ROOT, asset)),
};
fs.writeFileSync(path.join(RELEASE_ASSETS, `RiftForge_${releaseVersion}_provenance.json`), `${JSON.stringify(provenance, null, 2)}\n`);
console.log(`[release] Wrote provenance: ${path.join(RELEASE_ASSETS, `RiftForge_${releaseVersion}_provenance.json`)}`);
