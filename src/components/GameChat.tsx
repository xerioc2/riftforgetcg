import { useState } from 'react';
import type { ChatMessage } from '../types';

export function GameChat({ messages, onSend, readOnly = false, embedded = false }: { messages: ChatMessage[]; onSend: (text: string) => void; readOnly?: boolean; embedded?: boolean }) {
  const [open, setOpen] = useState(true);
  const [text, setText] = useState('');

  const send = () => {
    const trimmed = text.trim();
    if (!trimmed) return;
    onSend(trimmed);
    setText('');
  };

  return (
    <section className={`pointer-events-auto bg-panel/95 ${embedded ? 'flex h-full flex-col' : 'border border-line shadow-glow'}`}>
      <button className={`flex w-full items-center justify-between px-3 py-2 text-left text-sm font-semibold text-white ${embedded ? 'hidden' : ''}`} onClick={() => setOpen((value) => !value)}>
        Chat <span className="text-slate-500">{open ? '-' : '+'}</span>
      </button>
      {open ? (
        <div className={`${embedded ? 'flex min-h-0 flex-1 flex-col' : 'border-t border-line'}`}>
          <div className={`${embedded ? 'min-h-0 flex-1' : 'max-h-40'} overflow-auto px-3 py-2 text-sm`}>
            {messages.map((msg) => (
              <p className="mb-2 text-slate-300" key={msg.id}>
                <span className="font-semibold text-mint">{msg.email ?? msg.userId.slice(0, 6)}:</span> {msg.text}
              </p>
            ))}
            {messages.length === 0 ? <p className="text-slate-500">No messages yet.</p> : null}
          </div>
          {!readOnly ? (
            <div className="grid grid-cols-[1fr_auto] gap-2 border-t border-line p-2">
              <input className="input min-h-9" value={text} onChange={(event) => setText(event.target.value)} onKeyDown={(event) => event.key === 'Enter' && send()} />
              <button className="btn-secondary" onClick={send}>
                Send
              </button>
            </div>
          ) : null}
        </div>
      ) : null}
    </section>
  );
}
