import { NavLink } from 'react-router-dom';
import { useLocalPlayer } from '../lib/playerContext';

export function Nav() {
  const player = useLocalPlayer();

  return (
    <nav className="sticky top-0 z-30 border-b border-line bg-panel/95 backdrop-blur">
      <div className="mx-auto flex max-w-[1500px] flex-wrap items-center justify-between gap-4 px-5 py-4">
        <NavLink to="/" className="text-sm font-semibold uppercase tracking-[0.18em] text-forge">
          RiftForge
        </NavLink>
        <div className="flex items-center gap-2">
          <NavLink className={({ isActive }) => (isActive ? 'nav-link border-forge text-forge' : 'nav-link')} to="/">
            Home
          </NavLink>
          <NavLink className={({ isActive }) => (isActive ? 'nav-link border-forge text-forge' : 'nav-link')} to="/build">
            Deck Builder
          </NavLink>
        </div>
        <span className="max-w-[220px] truncate text-sm text-slate-300">{player.name}</span>
      </div>
    </nav>
  );
}
