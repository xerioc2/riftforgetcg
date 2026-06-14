export type CardType = 'Legend' | 'Champion' | 'Unit' | 'Spell' | 'Gear' | 'Rune' | 'Battlefield' | 'Token' | 'Unknown';

export type RiftCard = {
  id: string;
  name: string;
  type: CardType;
  champion?: string;
  domains: string[];
  cost?: number;
  premiumCost?: number;
  rarity?: string;
  set?: string;
  imageUrl?: string;
  rulesText?: string;
  flavorText?: string;
  power?: number;
  health?: number;
  keywords?: string[];
};

export type CardSupportStatus = 'SUPPORTED' | 'PARTIAL' | 'UNSUPPORTED' | 'BANNED' | 'NOT_AUDITED';

export type CardSupportSummary = {
  cardId: string;
  name: string;
  status: CardSupportStatus;
  reason: string;
};

export type CardFilters = {
  search: string;
  champion: string;
  domain: string;
  type: string;
  cost: string;
};

export type DeckCard = {
  cardId: string;
  quantity: number;
};

export type Deck = {
  id: string;
  userId?: string;
  name: string;
  legendCardId?: string;
  championCardId?: string;
  cards: DeckCard[];
  updatedAt: string;
};

export type DeckValidation = {
  valid: boolean;
  messages: string[];
  legend?: RiftCard;
  champion?: RiftCard;
  domains: string[];
  totalCards: number;
  mainDeckCards: number;
  championCards: number;
  runeCards: number;
  battlefieldCards: number;
  bannedCards: RiftCard[];
  unsupportedCards: Array<{ card: RiftCard; reason: string }>;
  partialCards: Array<{ card: RiftCard; reason: string }>;
  missingCardIds: string[];
};

export type RoomStatus = 'waiting' | 'playing' | 'finished';
export type GameMode = 'ENFORCED' | 'SANDBOX';
export type LegalAction =
  | 'SELECT_BATTLEFIELD'
  | 'KEEP_HAND'
  | 'MULLIGAN'
  | 'PASS_PHASE'
  | 'END_TURN'
  | 'PLAY_CARD'
  | 'MOVE_TO_BATTLEFIELD'
  | 'REPOSITION_CARD'
  | 'RESOLVE_SHOWDOWN'
  | 'TAP_RUNE'
  | 'DISCARD_RUNE'
  | 'UNDO_RUNES'
  | 'VISION_CHOICE'
  | 'RESOLVE_CHOICE'
  | 'HIDE_CARD'
  | 'EQUIP_GEAR'
  | 'SANDBOX_DEAL_CARD'
  | 'SANDBOX_ADJUST_SCORE'
  | 'SANDBOX_TAP_CARD'
  | 'SANDBOX_FLIP_CARD'
  | 'SANDBOX_MOVE_CARD';

export type PresencePlayer = {
  userId: string;
  email: string | null;
  deckId: string | null;
  ready: boolean;
  joinedAt: string;
};

export type GameRoom = {
  id: string;
  code: string;
  status: RoomStatus;
  hostId: string;
  playerIds: string[];
  settings: Record<string, unknown>;
  createdAt: string;
};

export type DevLobbyPlayer = {
  id: string;
  name: string;
  ready: boolean;
  deckCardIds: string[];
  deckWarnings?: string[];
  deckSupport?: CardSupportSummary[];
};

export type RoomState = {
  code: string;
  hostId: string;
  players: DevLobbyPlayer[];
  status: 'waiting' | 'playing';
  botEnabled: boolean;
  gameMode?: GameMode;
};

export type ZoneName = 'hand' | 'battlefield' | 'base' | 'rune' | 'rune-deck' | 'champion' | 'legend' | 'discard' | 'deck' | 'limbo' | 'hidden';

export type CardInstance = {
  instanceId: string;
  cardId: string;
  ownerId: string;
  zone: ZoneName;
  x: number;
  y: number;
  tapped: boolean;
  faceDown: boolean;
  zIndex: number;
  currentHealth?: number;
  mightBonus?: number;
  temporaryPowerModifier?: number;
  attachedToInstanceId?: string | null;
  hasSummoningSickness?: boolean;
};

export type PlayerGameState = {
  userId: string;
  name: string;
  score: number;
  availableEnergy?: number;
  runePoolRemaining?: number;
  deckCount?: number;
  battlefieldChoices?: string[];
  selectedBattlefieldId?: string | null;
};

export type MatchRecord = {
  id: string;
  completedAt: string;
  turnCount: number;
  winnerId: string;
  players: { userId: string; name: string; score: number }[];
};

export type PresenceSummary = {
  onlinePlayers: number;
  activeRooms: number;
  playersSearching?: number;
  queueSize?: number;
};

export type LogEntry = {
  id: string;
  timestamp: string;
  userId: string;
  text: string;
};

export type LiveGameState = {
  roomCode: string;
  currentPhase?: string;
  gameMode?: GameMode;
  activeShowdown?: ShowdownState | null;
  activePlayerId?: string;
  firstPlayerId?: string;
  turnNumber?: number;
  cards: CardInstance[];
  players: PlayerGameState[];
  runes?: RuneInstance[];
  log: LogEntry[];
  updatedAt: string;
  winnerId?: string | null;
  mulligansDone?: string[];
  cardPlayedThisTurn?: boolean;
  battlefieldController?: Record<string, string>;
  scoredBattlefieldsThisTurn?: string[];
  revealedHands?: RevealedHandSnapshot[];
  legalActions?: LegalAction[];
  pendingChoice?: PendingChoice | null;
};

export type PendingChoiceOption = {
  id: string;
  label: string;
};

export type PendingCardChoiceOption = {
  optionId: string;
  cardId: string;
  name: string;
  imageUrl?: string;
  rulesText?: string;
  originalIndex: number;
};

export type PendingCardChoiceAssignment = {
  optionId: string;
  action: 'TOP' | 'BOTTOM';
  order: number;
};

export type PendingChoice = {
  choiceId: string;
  playerId: string;
  type: string;
  prompt: string;
  options: PendingChoiceOption[];
  cardOptions?: PendingCardChoiceOption[];
  assignments?: PendingCardChoiceAssignment[];
  sourceCardInstanceId?: string;
  sourceCardId?: string;
  publicChoice?: boolean;
  requiredSelections?: number;
  paymentAmount?: number;
  effect?: string;
  allowPartialResolve?: boolean;
  context?: Record<string, string>;
};

export type ShowdownState = {
  attackingPlayerId: string;
  attackerInstanceIds: string[];
  gankingBonuses?: Record<string, number>;
  step?: 'STAGED' | 'ACTION_WINDOW' | 'ASSIGN_DAMAGE' | 'RESOLVE_DAMAGE' | 'CLEANUP' | 'COMPLETE';
};

export type RevealedHandSnapshot = {
  revealedToPlayerId: string;
  revealedOwnerId: string;
  instanceIds: string[];
  dismissedInstanceIds: string[];
};

export type ChatMessage = {
  id: string;
  userId: string;
  email: string | null;
  text: string;
  sentAt: string;
};

export type RuneInstance = {
  instanceId: string;
  cardId: string;
  ownerId: string;
  tapped: boolean;
  normalEnergy: number;
  premiumEnergy: number;
};
