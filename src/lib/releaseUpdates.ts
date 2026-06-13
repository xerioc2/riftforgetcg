import { APP_VERSION } from './appMetadata';

const LATEST_RELEASE_API = 'https://api.github.com/repos/xerioc2/riftforgetcg/releases/latest';
export const LATEST_RELEASE_FALLBACK_URL = 'https://github.com/xerioc2/riftforgetcg/releases/latest';
const DISMISSED_RELEASE_KEY = 'riftforge.dismissedReleaseTag';

export type ReleaseUpdate = {
  currentVersion: string;
  latestTag: string;
  latestName: string;
  releaseUrl: string;
};

type GitHubRelease = {
  tag_name?: string;
  name?: string;
  html_url?: string;
};

type ParsedVersion = {
  major: number;
  minor: number;
  patch: number;
};

export function normalizeReleaseVersion(version: string): string {
  return version.trim().replace(/^v/i, '');
}

function parseVersion(version: string): ParsedVersion | null {
  const normalized = normalizeReleaseVersion(version);
  const match = normalized.match(/^(\d+)\.(\d+)\.(\d+)/);
  if (!match) return null;
  return {
    major: Number(match[1]),
    minor: Number(match[2]),
    patch: Number(match[3]),
  };
}

export function isReleaseNewer(latestVersion: string, currentVersion: string): boolean {
  const latest = parseVersion(latestVersion);
  const current = parseVersion(currentVersion);
  if (!latest || !current) return false;
  if (latest.major !== current.major) return latest.major > current.major;
  if (latest.minor !== current.minor) return latest.minor > current.minor;
  return latest.patch > current.patch;
}

export function wasReleaseDismissed(tag: string): boolean {
  try {
    return localStorage.getItem(DISMISSED_RELEASE_KEY) === tag;
  } catch {
    return false;
  }
}

export function dismissRelease(tag: string): void {
  try {
    localStorage.setItem(DISMISSED_RELEASE_KEY, tag);
  } catch {
    // Storage can be unavailable in privacy modes; dismissal is best-effort.
  }
}

export async function checkLatestRelease(currentVersion = APP_VERSION): Promise<ReleaseUpdate | null> {
  const response = await fetch(LATEST_RELEASE_API, {
    headers: {
      Accept: 'application/vnd.github+json',
    },
  });
  if (!response.ok) return null;
  const release = (await response.json()) as GitHubRelease;
  const latestTag = release.tag_name?.trim();
  if (!latestTag || wasReleaseDismissed(latestTag)) return null;
  if (!isReleaseNewer(latestTag, currentVersion)) return null;
  return {
    currentVersion,
    latestTag,
    latestName: release.name?.trim() || latestTag,
    releaseUrl: release.html_url?.trim() || LATEST_RELEASE_FALLBACK_URL,
  };
}
