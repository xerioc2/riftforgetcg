package com.riftforge.matchmaking;

import java.util.List;

public record MatchmakingRequest(String playerId, String playerName, List<String> deckCardIds) {}
