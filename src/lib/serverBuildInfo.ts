export type ServerBuildInfo = {
  serverVersion: string;
  serverBuildTime: string;
  serverGitSha: string;
  serverFullGitSha: string;
  serverBuildTimestamp: string;
  serverReleaseTag: string;
  serverJarSha256: string;
};

let latestServerBuildInfo: ServerBuildInfo = {
  serverVersion: 'unknown',
  serverBuildTime: 'unknown',
  serverGitSha: 'unknown',
  serverFullGitSha: 'unknown',
  serverBuildTimestamp: 'unknown',
  serverReleaseTag: 'unknown',
  serverJarSha256: 'unknown',
};

export function setServerBuildInfo(info: Partial<ServerBuildInfo>) {
  latestServerBuildInfo = {
    serverVersion: info.serverVersion || latestServerBuildInfo.serverVersion,
    serverBuildTime: info.serverBuildTime || latestServerBuildInfo.serverBuildTime,
    serverGitSha: info.serverGitSha || latestServerBuildInfo.serverGitSha,
    serverFullGitSha: info.serverFullGitSha || latestServerBuildInfo.serverFullGitSha,
    serverBuildTimestamp: info.serverBuildTimestamp || latestServerBuildInfo.serverBuildTimestamp,
    serverReleaseTag: info.serverReleaseTag || latestServerBuildInfo.serverReleaseTag,
    serverJarSha256: info.serverJarSha256 || latestServerBuildInfo.serverJarSha256,
  };
}

export function getServerBuildInfo(): ServerBuildInfo {
  return latestServerBuildInfo;
}
