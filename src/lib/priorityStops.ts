import type { ChainState, LegalAction } from '../types';

export type PriorityStopSettings = {
  autoPassEmptyWindows: boolean;
  holdOpponentTurn: boolean;
  holdShowdowns: boolean;
  holdBeforeMySpells: boolean;
  holdBeforeOpponentSpells: boolean;
};

export const DEFAULT_PRIORITY_STOP_SETTINGS: PriorityStopSettings = {
  autoPassEmptyWindows: false,
  holdOpponentTurn: true,
  holdShowdowns: true,
  holdBeforeMySpells: true,
  holdBeforeOpponentSpells: true,
};

export const PRIORITY_STOP_STORAGE_KEY = 'riftforge.priorityStops.v1';

export function normalizePriorityStopSettings(value: Partial<PriorityStopSettings> | null | undefined): PriorityStopSettings {
  return {
    ...DEFAULT_PRIORITY_STOP_SETTINGS,
    ...(value ?? {}),
  };
}

export function loadPriorityStopSettings(storage: Pick<Storage, 'getItem'> | undefined = globalThis.localStorage): PriorityStopSettings {
  if (!storage) return DEFAULT_PRIORITY_STOP_SETTINGS;
  try {
    const raw = storage.getItem(PRIORITY_STOP_STORAGE_KEY);
    return raw ? normalizePriorityStopSettings(JSON.parse(raw) as Partial<PriorityStopSettings>) : DEFAULT_PRIORITY_STOP_SETTINGS;
  } catch {
    return DEFAULT_PRIORITY_STOP_SETTINGS;
  }
}

export function savePriorityStopSettings(settings: PriorityStopSettings, storage: Pick<Storage, 'setItem'> | undefined = globalThis.localStorage) {
  if (!storage) return;
  storage.setItem(PRIORITY_STOP_STORAGE_KEY, JSON.stringify(settings));
}

export function hasOnlyPassChainFocus(legalActions: LegalAction[] | undefined) {
  return (legalActions?.length ?? 0) === 1 && legalActions?.[0] === 'PASS_CHAIN_FOCUS';
}

export function shouldLocalAutoPassPriority(options: {
  settings: PriorityStopSettings;
  legalActions: LegalAction[] | undefined;
  chainState: ChainState | null | undefined;
  playerId: string;
  activePlayerId?: string | null;
}) {
  const { settings, legalActions, chainState, playerId, activePlayerId } = options;
  if (!settings.autoPassEmptyWindows) return false;
  if (!chainState || chainState.readyToResolveTop) return false;
  if (chainState.focusedPlayerId !== playerId) return false;
  if (!hasOnlyPassChainFocus(legalActions)) return false;
  if (settings.holdShowdowns && chainState.sourceContext === 'SHOWDOWN_ACTION') return false;
  if (settings.holdOpponentTurn && activePlayerId && activePlayerId !== playerId) return false;
  const topItem = chainState.chainItems?.[chainState.chainItems.length - 1];
  if (topItem?.controllerPlayerId === playerId && settings.holdBeforeMySpells) return false;
  if (topItem?.controllerPlayerId && topItem.controllerPlayerId !== playerId && settings.holdBeforeOpponentSpells) return false;
  return true;
}
