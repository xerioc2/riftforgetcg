export const KEYWORD_DESCRIPTIONS: Record<string, string> = {
  ACTION: 'Timing/action word. Active-player Main Phase and participant showdown Action play are partially supported; full timing is incomplete.',
  REACTION: 'Timing/action word for responding to events. Rules support incomplete until reaction windows are fully modeled.',
  ASSAULT: 'Gets +X Might while attacking. Combat bonus support is implemented; card-specific text may still be partial.',
  SHIELD: 'Gets +X Might while defending. Combat bonus support is implemented; card-specific text may still be partial.',
  TANK: 'Must be assigned lethal damage before units without Tank during combat.',
  BACKLINE: 'Is assigned combat damage after units without Backline.',
  TEMPORARY: "Destroyed at the start of its controller's next Beginning Phase.",
  DEATHKNELL: 'Applies when the unit dies. Basic trigger support exists; XP/reveal/card-specific effects may still be incomplete.',
  VISION: 'Look at the top cards of your deck. Basic choice prompt exists; card-specific effects may still be partial.',
  ACCELERATE: 'You may pay 1 additional energy for its Accelerate effect.',
  LEGION: 'Applies if another Main Deck card was played earlier this turn. Simple bonuses/token effects are partial.',
  MIGHTY: 'A descriptor for a unit with 5 or more effective Might. Threshold checks are supported; triggers that care about becoming Mighty may still be partial.',
  AMBUSH: 'Partial: may be played directly to a battlefield during supported windows if you already have a friendly unit there. Reaction timing and additional costs are incomplete.',
  'QUICK-DRAW': 'Rules support incomplete. Quick-Draw timing is deferred.',
  HIDDEN: 'Can be hidden from hand into a private hidden area by paying a ready rune. Later hidden play/reaction timing is not implemented yet.',
  GANKING: 'Gets +X Might for combat when entering against an opponent with higher Might. Partial combat support.',
  REPEAT: 'Allows you to play one additional card this turn.',
  EQUIP: 'Attachment/action word for Gear. Gear plays to Base first, then can equip from Base to a friendly Unit or Champion by paying its printed Equip cost; full official timing edge cases remain incomplete.',
  WEAPONMASTER: 'Gear-related Might keyword. Rules support is deferred until full equipment modifier cleanup.',
  STUN: 'A stunned card cannot participate normally until the effect clears. Rules support incomplete.',
  STUNNED: 'A stunned card cannot participate normally until the effect clears. Rules support incomplete.',
  DEFLECT: "Redirects an opponent's spell to another random battlefield unit when possible. Rules support incomplete.",
  HUNT: 'Progression keyword from newer rules/cards. Rules support incomplete.',
  LEVEL: 'Progression keyword from newer rules/cards. Rules support incomplete.',
  BUFF: 'Modifier/effect word from newer rules/cards. Rules support incomplete.',
  PREDICT: 'Look/selection effect related to future draws. Rules support incomplete.',
};

export function keywordDescription(keyword: string) {
  const normalized = keyword.toUpperCase().replace(/\s+\d+$/, '');
  return KEYWORD_DESCRIPTIONS[normalized] ?? KEYWORD_DESCRIPTIONS[keyword.toUpperCase()] ?? 'Card keyword. Rules support may be incomplete.';
}
