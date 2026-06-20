import type { ReleaseUpdate } from '../lib/releaseUpdates';
import { BUILD_DATE } from '../lib/appMetadata';

type UpdateAvailableBannerProps = {
  update: ReleaseUpdate;
  onDismiss: () => void;
};

export function UpdateAvailableBanner({ update, onDismiss }: UpdateAvailableBannerProps) {
  return (
    <section
      className="fixed right-4 top-[84px] z-50 w-[min(36rem,calc(100vw-2rem))] border border-forge/50 bg-panel/98 px-4 py-3 text-sm text-slate-200 shadow-[0_18px_60px_rgba(0,0,0,0.65)]"
      role="status"
      aria-live="polite"
    >
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="min-w-0">
          <p className="font-semibold text-forge">A newer RiftForge build is available.</p>
          <p className="mt-1 text-xs text-slate-400">
            Current {update.currentVersion} - Latest {update.latestTag}
            {update.latestName !== update.latestTag ? ` - ${update.latestName}` : ''}
          </p>
          <p className="mt-1 text-xs text-slate-500">Installed build date: {BUILD_DATE}</p>
        </div>
        <div className="flex flex-wrap items-center gap-2">
          <button
            className="btn-primary px-3 py-2 text-xs"
            onClick={() => window.open(update.releaseUrl, '_blank', 'noopener,noreferrer')}
          >
            Open Download Page
          </button>
          <button className="btn-secondary px-3 py-2 text-xs" onClick={onDismiss}>
            Remind me later
          </button>
        </div>
      </div>
    </section>
  );
}
