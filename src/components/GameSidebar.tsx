import { useState } from 'react';
import type { ChatMessage, LogEntry } from '../types';
import { GameChat } from './GameChat';
import { GameLog } from './GameLog';

type Tab = 'log' | 'chat';

interface GameSidebarProps {
  log: LogEntry[];
  chat: ChatMessage[];
  onSend: (msg: string) => void;
}

export function GameSidebar({ log, chat, onSend }: GameSidebarProps) {
  const [tab, setTab] = useState<Tab>('log');

  return (
    <aside className="pointer-events-auto absolute right-0 top-0 z-10 flex h-full w-[280px] flex-col border-l border-line bg-panel">
      <div className="flex border-b border-line">
        {(['log', 'chat'] as Tab[]).map((item) => (
          <button className={`flex-1 py-3 text-xs font-semibold uppercase ${tab === item ? 'border-b-2 border-forge text-forge' : 'text-slate-500 hover:text-slate-300'}`} key={item} onClick={() => setTab(item)}>
            {item === 'chat' ? <span title="Messages are not synced to opponents">Local Chat</span> : item}
          </button>
        ))}
      </div>
      <div className="min-h-0 flex-1 overflow-hidden">
        {tab === 'log' ? <GameLog entries={log} embedded /> : null}
        {tab === 'chat' ? <GameChat messages={chat} onSend={onSend} embedded /> : null}
      </div>
    </aside>
  );
}
