package com.riftforge.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class PendingChoice {
  public static final String TYPE_OPTIONAL_DRAW_ONE = "OPTIONAL_DRAW_ONE";
  public static final String TYPE_OPTIONAL_PAY_1_DRAW_ONE = "OPTIONAL_PAY_1_DRAW_ONE";
  public static final String TYPE_TOP_DECK_PICK_ONE = "TOP_DECK_PICK_ONE";
  public static final String TYPE_PREDICT_ORDER = "PREDICT_ORDER";
  public static final String OPTION_YES = "YES";
  public static final String OPTION_NO = "NO";
  public static final String OPTION_PAY_1 = "PAY_1";
  public static final String OPTION_DECLINE = "DECLINE";
  public static final String ACTION_HAND = "HAND";
  public static final String ACTION_TOP = "TOP";
  public static final String ACTION_BOTTOM = "BOTTOM";

  private String choiceId;
  private String playerId;
  private String type;
  private String prompt;
  private List<ChoiceOption> options = new ArrayList<>();
  private List<CardChoiceOption> cardOptions = new ArrayList<>();
  private List<CardChoiceAssignment> assignments = new ArrayList<>();
  private String sourceCardInstanceId;
  private String sourceCardId;
  private boolean publicChoice;
  private int requiredSelections;
  private boolean allowPartialResolve;
  private Map<String, String> context = new HashMap<>();

  public static PendingChoice optionalDrawOne(String choiceId, String playerId, String sourceCardId, String prompt) {
    PendingChoice choice = new PendingChoice();
    choice.setChoiceId(choiceId);
    choice.setPlayerId(playerId);
    choice.setType(TYPE_OPTIONAL_DRAW_ONE);
    choice.setSourceCardId(sourceCardId);
    choice.setPrompt(prompt);
    choice.setOptions(List.of(
        new ChoiceOption(OPTION_YES, "Yes"),
        new ChoiceOption(OPTION_NO, "No")));
    return choice;
  }

  public static PendingChoice optionalPayOneDrawOne(String choiceId, String playerId, String sourceCardId, String prompt) {
    PendingChoice choice = new PendingChoice();
    choice.setChoiceId(choiceId);
    choice.setPlayerId(playerId);
    choice.setType(TYPE_OPTIONAL_PAY_1_DRAW_ONE);
    choice.setSourceCardId(sourceCardId);
    choice.setPrompt(prompt);
    choice.setOptions(List.of(
        new ChoiceOption(OPTION_PAY_1, "Pay 1"),
        new ChoiceOption(OPTION_DECLINE, "Decline")));
    return choice;
  }

  public static PendingChoice topDeckPickOne(
      String choiceId,
      String playerId,
      String sourceCardId,
      String sourceCardInstanceId,
      List<CardDefinition> topCards) {
    PendingChoice choice = new PendingChoice();
    choice.setChoiceId(choiceId);
    choice.setPlayerId(playerId);
    choice.setType(TYPE_TOP_DECK_PICK_ONE);
    choice.setSourceCardId(sourceCardId);
    choice.setSourceCardInstanceId(sourceCardInstanceId);
    choice.setPrompt("Choose one of the top cards to put into your hand.");
    choice.setRequiredSelections(1);
    choice.setCardOptions(cardOptions(topCards));
    return choice;
  }

  public static PendingChoice predictOrder(
      String choiceId,
      String playerId,
      String sourceCardId,
      String sourceCardInstanceId,
      List<CardDefinition> topCards) {
    PendingChoice choice = new PendingChoice();
    choice.setChoiceId(choiceId);
    choice.setPlayerId(playerId);
    choice.setType(TYPE_PREDICT_ORDER);
    choice.setSourceCardId(sourceCardId);
    choice.setSourceCardInstanceId(sourceCardInstanceId);
    choice.setPrompt("Choose which cards stay on top and which go to the bottom.");
    choice.setRequiredSelections(topCards.size());
    choice.setCardOptions(cardOptions(topCards));
    return choice;
  }

  private static List<CardChoiceOption> cardOptions(List<CardDefinition> topCards) {
    List<CardChoiceOption> options = new ArrayList<>();
    for (int i = 0; i < topCards.size(); i++) {
      CardDefinition card = topCards.get(i);
      options.add(
          new CardChoiceOption(
              "card-" + i,
              card.id(),
              card.name(),
              card.imageUrl(),
              card.rulesText(),
              i));
    }
    return options;
  }

  public String getChoiceId() { return choiceId; }
  public void setChoiceId(String choiceId) { this.choiceId = choiceId; }
  public String getPlayerId() { return playerId; }
  public void setPlayerId(String playerId) { this.playerId = playerId; }
  public String getType() { return type; }
  public void setType(String type) { this.type = type; }
  public String getPrompt() { return prompt; }
  public void setPrompt(String prompt) { this.prompt = prompt; }
  public List<ChoiceOption> getOptions() { return options; }
  public void setOptions(List<ChoiceOption> options) { this.options = options == null ? new ArrayList<>() : options; }
  public List<CardChoiceOption> getCardOptions() { return cardOptions; }
  public void setCardOptions(List<CardChoiceOption> cardOptions) { this.cardOptions = cardOptions == null ? new ArrayList<>() : cardOptions; }
  public List<CardChoiceAssignment> getAssignments() { return assignments; }
  public void setAssignments(List<CardChoiceAssignment> assignments) { this.assignments = assignments == null ? new ArrayList<>() : assignments; }
  public String getSourceCardInstanceId() { return sourceCardInstanceId; }
  public void setSourceCardInstanceId(String sourceCardInstanceId) { this.sourceCardInstanceId = sourceCardInstanceId; }
  public String getSourceCardId() { return sourceCardId; }
  public void setSourceCardId(String sourceCardId) { this.sourceCardId = sourceCardId; }
  public boolean isPublicChoice() { return publicChoice; }
  public void setPublicChoice(boolean publicChoice) { this.publicChoice = publicChoice; }
  public int getRequiredSelections() { return requiredSelections; }
  public void setRequiredSelections(int requiredSelections) { this.requiredSelections = requiredSelections; }
  public boolean isAllowPartialResolve() { return allowPartialResolve; }
  public void setAllowPartialResolve(boolean allowPartialResolve) { this.allowPartialResolve = allowPartialResolve; }
  public Map<String, String> getContext() { return context; }
  public void setContext(Map<String, String> context) { this.context = context == null ? new HashMap<>() : context; }

  public record ChoiceOption(String id, String label) {}
  public record CardChoiceOption(
      String optionId,
      String cardId,
      String name,
      String imageUrl,
      String rulesText,
      int originalIndex) {}
  public record CardChoiceAssignment(String optionId, String action, int order) {}
}
