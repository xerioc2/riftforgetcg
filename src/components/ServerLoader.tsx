export function ServerLoader() {
  return (
    <main className="grid min-h-screen place-items-center bg-panel text-slate-100">
      <div className="flex flex-col items-center">
        <div className="h-10 w-10 animate-spin rounded-full border-2 border-line border-t-forge" />
        <h1 className="mt-5 text-3xl font-semibold text-forge">RiftForge</h1>
        <p className="mt-2 text-sm text-slate-400">Starting server...</p>
      </div>
    </main>
  );
}
