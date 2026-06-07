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
  championCardId?: string;
  cards: DeckCard[];
  updatedAt: string;
};

export type DeckValidation = {
  valid: boolean;
  messages: string[];
  champion?: RiftCard;
  domains: string[];
  totalCards: number;
  runeCards: number;
  battlefieldCards: number;
};

export type RoomStatus = 'waiting' | 'playing' | 'finished';

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
};

export type RoomState = {
  code: string;
  hostId: string;
  players: DevLobbyPlayer[];
  status: 'waiting' | 'playing';
  botEnabled: boolean;
};

export type ZoneName = 'hand' | 'battlefield' | 'base' | 'rune' | 'rune-deck' | 'champion' | 'legend' | 'discard' | 'deck' | 'limbo';

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
  temporaryPowerModifier?: number;
  attachedToInstanceId?: string | null;
  hasSummoningSickness?: boolean;
};

export type PlayerGameState = {
  userId: string;
  name: string;
  score: number;
  availableEnergy?: number;
};

export type MatchRecord = {
  id: string;
  completedAt: string;
  turnCount: number;
  winnerId: string;
  players: { userId: string; name: string; score: number }[];
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
  activePlayerId?: string;
  turnNumber?: number;
  cards: CardInstance[];
  players: PlayerGameState[];
  runes?: RuneInstance[];
  log: LogEntry[];
  updatedAt: string;
  winnerId?: string | null;
  declaredAttackers?: string[];
  blockerToAttacker?: Record<string, string>;
  mulligansDone?: string[];
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
