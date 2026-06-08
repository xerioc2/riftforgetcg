type ServerLoaderProps = {
  serverUrl: string;
  showHelp: boolean;
  onRetry: () => void;
};

export function ServerLoader({ serverUrl, showHelp, onRetry }: ServerLoaderProps) {
  return (
    <main className="grid min-h-screen place-items-center bg-panel text-slate-100">
      <div className="flex max-w-lg flex-col items-center px-6 text-center">
        <div className="h-10 w-10 animate-spin rounded-full border-2 border-line border-t-forge" />
        <h1 className="mt-5 text-3xl font-semibold text-forge">RiftForge</h1>
        <p className="mt-2 text-sm text-slate-400">Starting server...</p>
        <p className="mt-1 text-xs text-slate-500">First launch fetches card data and can take 1–2 minutes.</p>
        {showHelp ? (
          <div className="mt-6 border border-line bg-ink/60 p-4 text-left text-sm text-slate-300">
            <p className="font-semibold text-white">The game server is not responding.</p>
            <p className="mt-2">
              RiftForge is trying to connect to <span className="text-forge">{serverUrl}</span>. Close any older
              RiftForge server, then retry. Browser development also requires the Spring Boot server to be running.
            </p>
            <button className="btn-secondary mt-4 w-full" onClick={onRetry}>
              Retry connection
            </button>
          </div>
        ) : null}
      </div>
    </main>
  );
}
