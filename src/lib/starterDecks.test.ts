import { describe, expect, it } from 'vitest';
import { STARTER_DECKS } from './starterDecks';

const count = (entries: Array<{ quantity: number }>) => entries.reduce((sum, entry) => sum + entry.quantity, 0);

describe('deck presets', () => {
  it('uses the uploaded Irelia tournament list as the first playtest preset', () => {
    const [irelia] = STARTER_DECKS;

    expect(irelia.name).toBe('Irelia Uploaded Meta - Playtest');
    expect(irelia.legend).toBe('Irelia - Blade Dancer');
    expect(irelia.champion).toBe('Irelia - Fervent');
    expect(count(irelia.main)).toBe(39);
    expect(count(irelia.runes)).toBe(12);
    expect(irelia.battlefields).toHaveLength(3);
    expect(irelia.main.some((entry) => entry.name === 'Irelia - Fervent')).toBe(false);
    expect(irelia.main.find((entry) => entry.name === 'Vex - Apathetic')?.quantity).toBe(2);
  });

  it('exposes uploaded pro deck presets without the old 40-card Irelia starter list', () => {
    const names = STARTER_DECKS.map((deck) => deck.name);

    expect(names).toContain('Diana Uploaded Meta - Playtest');
    expect(names).toContain('LeBlanc Uploaded Meta - Zhongshan');
    expect(names).toContain('Azir Uploaded Meta - Lille');
    expect(names).not.toContain('Irelia Tempo');
  });

  it('exposes uploaded Diana as an enforced-playable playtest preset without replacing Irelia default', () => {
    const [irelia, diana] = STARTER_DECKS;

    expect(irelia.name).toBe('Irelia Uploaded Meta - Playtest');
    expect(diana.name).toBe('Diana Uploaded Meta - Playtest');
    expect(diana.status).toBe('Mostly supported');
    expect(diana.description).toContain('Enforced-playable');
    expect(diana.warnings.join(' ')).toContain('many Partial card behaviors remain');
    expect(diana.legend).toBe('Diana - Scorn of the Moon');
    expect(diana.champion).toBe('Diana - Lunari');
    expect(count(diana.main)).toBe(39);
    expect(count(diana.runes)).toBe(12);
    expect(diana.battlefields).toHaveLength(3);
    expect(diana.main.some((entry) => entry.name === 'Diana - Lunari')).toBe(false);
    expect(diana.main.find((entry) => entry.name === 'Moonfall')?.quantity).toBe(3);
  });
});
