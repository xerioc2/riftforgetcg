import { spawnSync } from 'node:child_process';
import fs from 'node:fs';
import { prepareDesktop } from './prepare-desktop.mjs';

function commandName(name) {
  return process.platform === 'win32' ? `${name}.cmd` : name;
}

function runGit(args, fallback) {
  const result = spawnSync('git', args, { encoding: 'utf8', stdio: ['ignore', 'pipe', 'ignore'] });
  if (result.status !== 0) return fallback;
  return result.stdout.trim() || fallback;
}

function readJson(file) {
  return JSON.parse(fs.readFileSync(new URL(`../${file}`, import.meta.url), 'utf8'));
}

const packageJson = readJson('package.json');
const buildTimestamp = process.env.RIFTFORGE_BUILD_TIMESTAMP || new Date().toISOString();
const buildSha = process.env.RIFTFORGE_BUILD_GIT_SHA || runGit(['rev-parse', '--short', 'HEAD'], 'local-dev');
const fullBuildSha = process.env.RIFTFORGE_BUILD_FULL_GIT_SHA || runGit(['rev-parse', 'HEAD'], buildSha);
const appVersion = process.env.VITE_APP_VERSION || `v${packageJson.version}-alpha`;

const env = {
  ...process.env,
  RIFTFORGE_BUILD_GIT_SHA: buildSha,
  RIFTFORGE_BUILD_FULL_GIT_SHA: fullBuildSha,
  RIFTFORGE_BUILD_TIMESTAMP: buildTimestamp,
  RIFTFORGE_RELEASE_TAG: process.env.RIFTFORGE_RELEASE_TAG || appVersion,
  VITE_APP_VERSION: appVersion,
  VITE_BUILD_TAG: process.env.VITE_BUILD_TAG || buildSha,
  VITE_BUILD_DATE: process.env.VITE_BUILD_DATE || buildTimestamp.slice(0, 10),
};

process.env.RIFTFORGE_BUILD_GIT_SHA = env.RIFTFORGE_BUILD_GIT_SHA;
process.env.RIFTFORGE_BUILD_FULL_GIT_SHA = env.RIFTFORGE_BUILD_FULL_GIT_SHA;
process.env.RIFTFORGE_BUILD_TIMESTAMP = env.RIFTFORGE_BUILD_TIMESTAMP;
process.env.RIFTFORGE_RELEASE_TAG = env.RIFTFORGE_RELEASE_TAG;

prepareDesktop();

console.log(`[desktop] Using frontend build date ${env.VITE_BUILD_DATE}`);
const npmCommand = commandName('npm');
const useCmdShim = process.platform === 'win32';
const result = spawnSync(useCmdShim ? 'cmd.exe' : npmCommand, useCmdShim ? ['/d', '/s', '/c', npmCommand, 'run', 'build'] : ['run', 'build'], {
  env,
  stdio: 'inherit',
  shell: false,
});

if (result.error) throw result.error;
if (result.status !== 0) {
  throw new Error(`npm run build failed with exit code ${result.status}.`);
}
