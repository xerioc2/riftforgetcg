import { create } from 'zustand';

export type ToastTone = 'info' | 'success' | 'warning' | 'error';

export type ToastMessage = {
  id: string;
  tone: ToastTone;
  title: string;
  message?: string;
};

type ToastStore = {
  toasts: ToastMessage[];
  lastError?: string;
  pushToast: (toast: Omit<ToastMessage, 'id'>) => void;
  dismissToast: (id: string) => void;
};

export const useToastStore = create<ToastStore>((set) => ({
  toasts: [],
  pushToast: (toast) =>
    set((state) => {
      const isDuplicate = state.toasts.some((existing) =>
        existing.tone === toast.tone && existing.title === toast.title && existing.message === toast.message,
      );
      if (isDuplicate) {
        return {
          lastError: toast.tone === 'error' || toast.tone === 'warning' ? [toast.title, toast.message].filter(Boolean).join(': ') : state.lastError,
        };
      }
      const id = crypto.randomUUID();
      const next = [...state.toasts, { ...toast, id }].slice(-4);
      window.setTimeout(() => useToastStore.getState().dismissToast(id), toast.tone === 'error' ? 8000 : 5000);
      return {
        toasts: next,
        lastError: toast.tone === 'error' || toast.tone === 'warning' ? [toast.title, toast.message].filter(Boolean).join(': ') : state.lastError,
      };
    }),
  dismissToast: (id) => set((state) => ({ toasts: state.toasts.filter((toast) => toast.id !== id) })),
}));

export function notifyError(title: string, message?: string) {
  useToastStore.getState().pushToast({ tone: 'error', title, message });
}

export function notifyWarning(title: string, message?: string) {
  useToastStore.getState().pushToast({ tone: 'warning', title, message });
}

export function notifySuccess(title: string, message?: string) {
  useToastStore.getState().pushToast({ tone: 'success', title, message });
}
