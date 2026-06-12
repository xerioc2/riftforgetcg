export const KEYWORD_DESCRIPTIONS: Record<string, string> = {
  ACTION: 'Timing/action word for optional effects. Rules support incomplete until action windows are fully modeled.',
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
  AMBUSH: 'Can attack directly from Base and enters the battlefield ready. Rules support incomplete.',
  'QUICK-DRAW': 'Enters Base ready and can move to the battlefield the turn it is played.',
  HIDDEN: "Cannot be targeted by an opponent's spells or abilities while at Base. Rules support incomplete.",
  GANKING: 'Gets +X Might for combat when entering against an opponent with higher Might. Partial combat support.',
  REPEAT: 'Allows you to play one additional card this turn.',
  EQUIP: 'Attachment/action word for Gear. Basic friendly-unit attachment is supported; full timing and edge cases remain incomplete.',
  WEAPONMASTER: 'Gets +X Might permanently whenever Gear is attached to it. Rules support incomplete.',
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
