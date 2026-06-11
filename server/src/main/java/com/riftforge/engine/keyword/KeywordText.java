package com.riftforge.engine.keyword;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class KeywordText {
  private static final Pattern COMPACT_VALUED_KEYWORD = Pattern.compile("^([A-Z][A-Z-]*?)\\d+$");
  private static final Pattern TRAILING_VALUE = Pattern.compile("(\\d+)$");

  private KeywordText() {}

  public static String name(String keyword) {
    String normalized = normalize(keyword);
    if (normalized.isEmpty()) return normalized;
    Matcher compact = COMPACT_VALUED_KEYWORD.matcher(normalized);
    if (compact.matches()) return compact.group(1);
    int index = normalized.indexOf(' ');
    return index < 0 ? normalized : normalized.substring(0, index);
  }

  public static int value(String keyword, String expectedName) {
    if (!name(keyword).equals(name(expectedName))) return 0;
    Matcher value = TRAILING_VALUE.matcher(normalize(keyword));
    return value.find() ? Integer.parseInt(value.group(1)) : 0;
  }

  private static String normalize(String keyword) {
    return keyword == null ? "" : keyword.trim().toUpperCase(Locale.ROOT).replaceAll("\\s+", " ");
  }
}
