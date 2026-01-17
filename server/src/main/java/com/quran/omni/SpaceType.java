package com.quran.omni;

import java.util.Locale;
import java.util.Optional;

public enum SpaceType {
    QURAN("quran"),
    TRANSLATION("translation"),
    TAFSIR("tafsir"),
    POST("post"),
    COURSE("course"),
    ARTICLE("article");

    private final String apiName;

    SpaceType(String apiName) {
        this.apiName = apiName;
    }

    public String apiName() {
        return apiName;
    }

    public String envKey() {
        return name();
    }

    public static Optional<SpaceType> fromString(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        switch (normalized) {
            case "quran":
                return Optional.of(QURAN);
            case "translation":
            case "translations":
                return Optional.of(TRANSLATION);
            case "tafsir":
            case "tafsirs":
                return Optional.of(TAFSIR);
            case "post":
            case "posts":
                return Optional.of(POST);
            case "course":
            case "courses":
                return Optional.of(COURSE);
            case "article":
            case "articles":
                return Optional.of(ARTICLE);
            default:
                return Optional.empty();
        }
    }
}
