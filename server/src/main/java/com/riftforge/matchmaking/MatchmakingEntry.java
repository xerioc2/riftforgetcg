package com.riftforge.matchmaking;

import java.util.List;

public record MatchmakingEntry(String playerId, String playerName, List<String> deckCardIds) {}
