import { useToastStore } from '../store/toasts';

const toneClass = {
  info: 'border-slate-500/60 bg-panel text-slate-100',
  success: 'border-mint/60 bg-panel text-mint',
  warning: 'border-forge/70 bg-panel text-forge',
  error: 'border-ember/70 bg-panel text-ember',
};

export function ToastHost() {
  const { toasts, dismissToast } = useToastStore();
  if (toasts.length === 0) return null;

  return (
    <div className="fixed right-4 top-24 z-[100] flex w-[min(420px,calc(100vw-2rem))] flex-col gap-3">
      {toasts.map((toast) => (
        <section className={`border px-4 py-3 shadow-glow ${toneClass[toast.tone]}`} key={toast.id} role="status" aria-live="polite">
          <div className="flex items-start justify-between gap-3">
            <div className="min-w-0">
              <p className="font-semibold text-white">{toast.title}</p>
              {toast.message ? <p className="mt-1 text-sm leading-5 text-slate-300">{toast.message}</p> : null}
            </div>
            <button className="icon-btn shrink-0" onClick={() => dismissToast(toast.id)} aria-label="Dismiss notification">
              x
            </button>
          </div>
        </section>
      ))}
    </div>
  );
}
