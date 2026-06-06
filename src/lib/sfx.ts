type SfxName = 'cardPlay' | 'tap' | 'attack' | 'score' | 'victory' | 'defeat';

const cache = new Map<SfxName, AudioBuffer>();
let ctx: AudioContext | null = null;

const FILES: Record<SfxName, string> = {
  cardPlay: '/sounds/card-play.mp3',
  tap: '/sounds/tap.mp3',
  attack: '/sounds/attack.mp3',
  score: '/sounds/score.mp3',
  victory: '/sounds/victory.mp3',
  defeat: '/sounds/defeat.mp3',
};

async function load(name: SfxName) {
  if (!ctx) ctx = new AudioContext();
  if (cache.has(name)) return;
  try {
    const res = await fetch(FILES[name]);
    if (!res.ok) return;
    const buf = await ctx.decodeAudioData(await res.arrayBuffer());
    cache.set(name, buf);
  } catch {
    // Missing audio assets should never interrupt gameplay.
  }
}

export async function play(name: SfxName) {
  await load(name);
  if (!ctx) return;
  const buf = cache.get(name);
  if (!buf) return;
  const src = ctx.createBufferSource();
  src.buffer = buf;
  src.connect(ctx.destination);
  src.start();
}

export function preload() {
  (Object.keys(FILES) as SfxName[]).forEach((name) => load(name).catch(() => {}));
}
