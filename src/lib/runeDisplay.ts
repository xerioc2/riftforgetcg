import type { RiftCard, RuneInstance } from '../types';

export type RuneDisplayState = {
  usesCardArt: boolean;
  label: string;
  domainLabel: string;
  title: string;
  opacity: number;
  borderColor: string;
  fillColor: string;
};

export function primaryRuneDomain(card: RiftCard | undefined) {
  return card?.domains.find((domain) => domain.toUpperCase() !== 'COLORLESS') ?? card?.domains[0] ?? 'Rune';
}

export function runeDisplayState(
  rune: Pick<RuneInstance, 'cardId' | 'normalEnergy' | 'premiumEnergy' | 'tapped'>,
  card: RiftCard | undefined,
  pending = false,
): RuneDisplayState {
  const domain = primaryRuneDomain(card);
  const label = card?.name ?? 'Unknown Rune';
  return {
    usesCardArt: Boolean(card?.imageUrl),
    label,
    domainLabel: domain,
    title: `${label}: ${rune.normalEnergy} energy, ${rune.premiumEnergy} premium${domain === 'Rune' ? '' : ` (${domain})`}`,
    opacity: rune.tapped && !pending ? 0.45 : 1,
    borderColor: pending ? '#9eebd8' : rune.tapped ? '#475569' : '#d8b05d',
    fillColor: pending ? '#0d2b28' : rune.tapped ? '#151b24' : '#21160a',
  };
}
