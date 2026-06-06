package com.riftforge.model;

import java.util.List;

public record CardDefinition(
    String id,
    String name,
    String type,
    String champion,
    List<String> domains,
    int cost,
    int premiumCost,
    String rarity,
    String set,
    String imageUrl,
    String rulesText,
    int power,
    int health,
    List<String> keywords) {}
