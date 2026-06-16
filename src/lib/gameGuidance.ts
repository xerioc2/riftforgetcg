import type { ChainItem, LegalAction } from '../types';

const PHASE_GUIDANCE: Record<string, string> = {
  SELECT_BATTLEFIELD: 'Choose one Battlefield. After both players lock in, mulligans begin.',
  MULLIGAN: 'Choose cards to recycle or keep your opening hand.',
  AWAKEN: 'Start-of-turn ready step.',
  BEGINNING: 'Beginning effects and hold scoring.',
  CHANNEL: 'Channel runes.',
  DRAW: 'Draw step.',
  MAIN: 'Play cards, move units, use runes, or pass.',
  END: 'End your turn.',
};

const SHOWDOWN_GUIDANCE: Record<string, string> = {
  STAGED: 'Showdown in progress. Resolve showdown to continue.',
  ACTION_WINDOW: 'Showdown focus window. The focused player may play supported Action cards or pass focus.',
  ASSIGN_DAMAGE: 'Showdown damage assignment is resolving.',
  RESOLVE_DAMAGE: 'Showdown damage is resolving.',
  CLEANUP: 'Showdown cleanup is resolving.',
  COMPLETE: 'Showdown complete. Return to Main Phase actions.',
};

export function isBotPlayer(playerId?: string | null, playerName?: string | null) {
  return Boolean(playerId?.startsWith('bot-player-') || playerName?.toLowerCase().includes('riftbot'));
}

export function phaseGuidance(
  currentPhase?: string,
  activeShowdown?: boolean,
  showdownStep?: string,
  activeChain?: boolean,
  chainReady?: boolean,
  topChainItem?: ChainItem,
) {
  if (activeChain) {
    if (topChainItem?.status === 'COUNTERED') {
      return 'Countered items are skipped when they reach the top of the chain.';
    }
    if (topChainItem?.status === 'FIZZLED') {
      return 'Fizzled items are cleared when they reach the top of the chain.';
    }
    if (isStackedDeckChainItem(topChainItem)) {
      return chainReady
        ? 'Resolve Stacked Deck on the chain to choose from the top cards.'
        : 'Stacked Deck is waiting on priority/chain responses. Resolve the chain to choose from the top cards.';
    }
    return chainReady
      ? 'The top chain item is ready to resolve.'
      : 'Chain focus window. The focused player may pass chain focus.';
  }
  if (activeShowdown) return SHOWDOWN_GUIDANCE[showdownStep ?? 'STAGED'] ?? 'Showdown in progress. Resolve showdown to continue.';
  return PHASE_GUIDANCE[currentPhase ?? 'MAIN'] ?? 'Follow the available actions shown by the server.';
}

export function legalActionHint(
  legalActions: LegalAction[] | undefined,
  options: { isPlayer: boolean; isMyTurn: boolean; activePlayerName?: string; activePlayerIsBot?: boolean },
) {
  if (!options.isPlayer) return 'Spectating.';
  const actions = new Set(legalActions ?? []);
  if (actions.size === 0) {
    if (options.isMyTurn) return 'No server-approved actions are available right now.';
    return options.activePlayerIsBot ? 'RiftBot is thinking...' : `Waiting for ${options.activePlayerName ?? 'opponent'}.`;
  }
  if (actions.has('SELECT_BATTLEFIELD')) return 'Choose one Battlefield.';
  if (actions.has('KEEP_HAND') || actions.has('MULLIGAN')) return 'You can keep or mulligan.';
  if (actions.has('RESOLVE_CHOICE')) return 'Choose an option to continue.';
  if (actions.has('RESOLVE_CHAIN_TOP')) return 'You can resolve the top chain item.';
  if (actions.has('PASS_CHAIN_FOCUS')) return 'You can pass chain focus.';
  if (actions.has('ASSIGN_COMBAT_DAMAGE')) return 'Assign combat damage to continue the showdown.';
  if (actions.has('PASS_SHOWDOWN_FOCUS') && actions.has('PLAY_CARD')) return 'You may play a supported Action card or pass showdown focus.';
  if (actions.has('PASS_SHOWDOWN_FOCUS')) return 'You can pass showdown focus.';
  if (actions.has('RESOLVE_SHOWDOWN') && actions.has('PLAY_CARD')) return 'You may play supported Action cards or resolve the showdown.';
  if (actions.has('RESOLVE_SHOWDOWN')) return 'You can resolve this showdown.';
  if (actions.has('PLAY_CARD') && actions.size === 1) return 'You may play supported Action cards.';

  const parts: string[] = [];
  if (actions.has('PLAY_CARD')) parts.push('play cards');
  if (actions.has('HIDE_CARD')) parts.push('hide Hidden cards');
  if (actions.has('EQUIP_GEAR')) parts.push('equip Gear');
  if (actions.has('TAP_RUNE') || actions.has('DISCARD_RUNE') || actions.has('UNDO_RUNES')) parts.push('use runes');
  if (actions.has('MOVE_TO_BATTLEFIELD')) parts.push('move units');
  if (actions.has('VISION_CHOICE')) parts.push('choose Vision');
  if (actions.has('RESOLVE_CHOICE')) parts.push('choose an option');
  if (actions.has('PASS_PHASE') || actions.has('END_TURN')) parts.push('pass');
  if (parts.length > 0) return `You can ${joinActionParts(parts)}.`;

  if ([...actions].some((action) => action.startsWith('SANDBOX_'))) return 'Sandbox tools are available.';
  return 'Follow the highlighted legal actions.';
}

export function waitingStatusText(options: { isMyTurn: boolean; activePlayerName?: string; activePlayerIsBot?: boolean; waitingLong?: boolean }) {
  if (options.isMyTurn) return undefined;
  if (options.activePlayerIsBot) return options.waitingLong ? 'Still waiting for server update...' : 'RiftBot is thinking...';
  return `Waiting for ${options.activePlayerName ?? 'opponent'}.`;
}

function joinActionParts(parts: string[]) {
  if (parts.length <= 1) return parts[0] ?? '';
  if (parts.length === 2) return `${parts[0]} or ${parts[1]}`;
  return `${parts.slice(0, -1).join(', ')}, or ${parts[parts.length - 1]}`;
}

function isStackedDeckChainItem(item?: ChainItem) {
  if (!item) return false;
  return item.effectKey === 'STACKED_DECK_PICK_ONE'
    || item.sourceCardName?.toLowerCase() === 'stacked deck'
    || item.publicDescription?.toLowerCase() === 'stacked deck';
}
