import { useEffect, useRef, useState } from 'react';
import type { LogEntry } from '../types';

export function GameLog({ entries, embedded = false }: { entries: LogEntry[]; embedded?: boolean }) {
  const [open, setOpen] = useState(true);
  const scrollRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    scrollRef.current?.scrollTo({ top: scrollRef.current.scrollHeight });
  }, [entries.length]);

  return (
    <section className={`pointer-events-auto bg-panel/95 ${embedded ? 'flex h-full flex-col' : 'border border-line shadow-glow'}`}>
      <button className={`flex w-full items-center justify-between px-3 py-2 text-left text-sm font-semibold text-white ${embedded ? 'hidden' : ''}`} onClick={() => setOpen((value) => !value)}>
        Game log <span className="text-slate-500">{open ? '-' : '+'}</span>
      </button>
      {open ? (
        <div className={`${embedded ? 'min-h-0 flex-1' : 'max-h-56 border-t border-line'} overflow-auto px-3 py-2 text-sm`} ref={scrollRef}>
          {entries.map((entry) => (
            <p className="mb-2 text-slate-300" key={entry.id}>
              <span className="text-slate-500">{new Date(entry.timestamp).toLocaleTimeString()}</span> {entry.text}
            </p>
          ))}
          {entries.length === 0 ? <p className="text-slate-500">No actions yet.</p> : null}
        </div>
      ) : null}
    </section>
  );
}
