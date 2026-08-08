package com.luna.skin.domain.analysis.enums;

public enum SkinStatusLabel {
  BAD,
  NORMAL,
  GOOD;

  public static SkinStatusLabel from(int score) {
    if (score >= 80) return GOOD;
    if (score >= 50) return NORMAL;
    return BAD;
  }

  public String toLabel() {
    return switch (this) {
      case GOOD -> "좋음";
      case NORMAL -> "보통";
      case BAD -> "나쁨";
    };
  }
}