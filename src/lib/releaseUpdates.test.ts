import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import {
  checkLatestRelease,
  dismissRelease,
  isReleaseNewer,
  LATEST_RELEASE_FALLBACK_URL,
  normalizeReleaseVersion,
  wasReleaseDismissed,
} from './releaseUpdates';

function stubLocalStorage() {
  const values = new Map<string, string>();
  vi.stubGlobal('localStorage', {
    getItem: vi.fn((key: string) => values.get(key) ?? null),
    setItem: vi.fn((key: string, value: string) => {
      values.set(key, value);
    }),
    removeItem: vi.fn((key: string) => {
      values.delete(key);
    }),
    clear: vi.fn(() => values.clear()),
  });
}

function stubLatestRelease(tagName: string, overrides: { name?: string; html_url?: string } = {}) {
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
    ok: true,
    json: vi.fn().mockResolvedValue({
      tag_name: tagName,
      ...overrides,
    }),
  }));
}

describe('releaseUpdates', () => {
  beforeEach(() => {
    stubLocalStorage();
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
  });

  it('strips leading v from release versions', () => {
    expect(normalizeReleaseVersion('v0.1.2-alpha')).toBe('0.1.2-alpha');
    expect(normalizeReleaseVersion(' V0.2.0-alpha ')).toBe('0.2.0-alpha');
  });

  it('detects v0.1.2-alpha as newer than v0.1.1-alpha', () => {
    expect(isReleaseNewer('v0.1.2-alpha', 'v0.1.1-alpha')).toBe(true);
  });

  it('does not report an update for the same version', () => {
    expect(isReleaseNewer('v0.1.2-alpha', '0.1.2-alpha')).toBe(false);
  });

  it('does not report an update when latest is older', () => {
    expect(isReleaseNewer('v0.1.1-alpha', 'v0.1.2-alpha')).toBe(false);
  });

  it('fails safe for malformed versions', () => {
    expect(isReleaseNewer('latest', 'v0.1.1-alpha')).toBe(false);
    expect(isReleaseNewer('v0.1.2-alpha', 'local-dev')).toBe(false);
  });

  it('documents prerelease suffix behavior by comparing only semver numbers', () => {
    expect(isReleaseNewer('v0.1.2-beta.1', 'v0.1.1-alpha')).toBe(true);
    expect(isReleaseNewer('v0.1.2-beta.1', 'v0.1.2-alpha')).toBe(false);
  });

  it('suppresses a dismissed latest tag', async () => {
    dismissRelease('v0.1.2-alpha');
    stubLatestRelease('v0.1.2-alpha');

    await expect(checkLatestRelease('0.1.1-alpha')).resolves.toBeNull();
    expect(wasReleaseDismissed('v0.1.2-alpha')).toBe(true);
  });

  it('returns update metadata for a newer undismissed release', async () => {
    stubLatestRelease('v0.1.2-alpha', { name: 'Alpha Two' });

    await expect(checkLatestRelease('0.1.1-alpha')).resolves.toEqual({
      currentVersion: '0.1.1-alpha',
      latestTag: 'v0.1.2-alpha',
      latestName: 'Alpha Two',
      releaseUrl: LATEST_RELEASE_FALLBACK_URL,
    });
  });
});
