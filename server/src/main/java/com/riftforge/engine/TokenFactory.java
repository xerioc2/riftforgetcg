package com.riftforge.engine;

import com.riftforge.model.CardInstance;
import com.riftforge.model.LiveGameState;
import com.riftforge.model.ZoneName;
import com.riftforge.service.CardDataService;
import java.util.ArrayList;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class TokenFactory {
  public static final String RECRUIT_TOKEN_CARD_ID = "token-recruit-1";

  private final CardDataService cardDataService;

  public TokenFactory(CardDataService cardDataService) {
    this.cardDataService = cardDataService;
  }

  public CardInstance createRecruit(LiveGameState state, String ownerId, ZoneName zone, int x, int y) {
    int maxZ = state.getCards().stream().mapToInt(CardInstance::getZIndex).max().orElse(0);
    CardInstance token = new CardInstance();
    token.setInstanceId("token-" + UUID.randomUUID());
    token.setCardId(RECRUIT_TOKEN_CARD_ID);
    token.setOwnerId(ownerId);
    token.setZone(zone);
    token.setX(x);
    token.setY(y);
    token.setZIndex(maxZ + 1);
    token.setCurrentHealth(cardDataService.getCard(RECRUIT_TOKEN_CARD_ID).health());
    token.setHasSummoningSickness(false);
    token.setTapped(zone == ZoneName.BATTLEFIELD);
    token.setTempKeywords(new ArrayList<>());
    state.getCards().add(token);
    return token;
  }
}
