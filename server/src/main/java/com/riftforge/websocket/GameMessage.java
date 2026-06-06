package com.riftforge.websocket;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.riftforge.model.LiveGameState;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(value = GameMessage.StateUpdate.class, name = "STATE_UPDATE"),
  @JsonSubTypes.Type(value = GameMessage.GameError.class, name = "ERROR")
})
public sealed interface GameMessage {
  record StateUpdate(LiveGameState state) implements GameMessage {}
  record GameError(String message, String playerId) implements GameMessage {}
}
