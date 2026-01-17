package com.quran.omni;

import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public record AppConfig(
    int port,
    String goodMemBaseUrl,
    String goodMemApiKey,
    boolean goodMemInsecureSsl,
    Map<SpaceType, String> spaceIdOverrides,
    Map<SpaceType, Integer> spaceLimits,
    String defaultLanguage,
    Duration spaceCacheTtl
) {
    public static AppConfig fromEnv() {
        int port = readIntEnv("PORT", 7070);
        String baseUrl = readEnv("GOODMEM_BASE_URL", "https://omni-dev.quran.ai:8080");
        String apiKey = readEnv("GOODMEM_API_KEY", null);
        if (apiKey == null || apiKey.isBlank()) {
            apiKey = readEnv("GM_API_KEY", "");
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("GOODMEM_API_KEY is required");
        }

        boolean insecureSsl = readBoolEnv("GOODMEM_INSECURE_SSL", true);
        Map<SpaceType, String> spaceIdOverrides = new EnumMap<>(SpaceType.class);
        String spaceIds = readEnv("GOODMEM_SPACE_IDS", "");
        if (!spaceIds.isBlank()) {
            for (String entry : splitCsv(spaceIds)) {
                String[] parts = entry.split("=", 2);
                if (parts.length == 2) {
                    SpaceType.fromString(parts[0]).ifPresent(type -> {
                        String value = parts[1].trim();
                        if (!value.isBlank()) {
                            spaceIdOverrides.put(type, value);
                        }
                    });
                }
            }
        }
        for (SpaceType type : SpaceType.values()) {
            String envKey = "GOODMEM_SPACE_" + type.envKey();
            String value = readEnv(envKey, "").trim();
            if (!value.isBlank()) {
                spaceIdOverrides.put(type, value);
            }
        }

        Map<SpaceType, Integer> spaceLimits = new EnumMap<>(SpaceType.class);
        spaceLimits.put(SpaceType.QURAN, readIntEnv("SEARCH_LIMIT_QURAN", 6));
        spaceLimits.put(SpaceType.TRANSLATION, readIntEnv("SEARCH_LIMIT_TRANSLATION", 12));
        spaceLimits.put(SpaceType.TAFSIR, readIntEnv("SEARCH_LIMIT_TAFSIR", 10));
        spaceLimits.put(SpaceType.POST, readIntEnv("SEARCH_LIMIT_POST", 8));
        spaceLimits.put(SpaceType.COURSE, readIntEnv("SEARCH_LIMIT_COURSE", 6));
        spaceLimits.put(SpaceType.ARTICLE, readIntEnv("SEARCH_LIMIT_ARTICLE", 6));

        String defaultLanguage = readEnv("SEARCH_DEFAULT_LANGUAGE", "en").toLowerCase(Locale.ROOT);
        Duration spaceCacheTtl = Duration.ofSeconds(readIntEnv("SPACE_CACHE_TTL_SECONDS", 600));

        return new AppConfig(
            port,
            baseUrl,
            apiKey,
            insecureSsl,
            spaceIdOverrides,
            spaceLimits,
            defaultLanguage,
            spaceCacheTtl
        );
    }

    private static String readEnv(String key, String fallback) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    private static int readIntEnv(String key, int fallback) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static boolean readBoolEnv(String key, boolean fallback) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim().equalsIgnoreCase("true") || value.trim().equalsIgnoreCase("1");
    }

    private static List<String> splitCsv(String value) {
        List<String> result = new ArrayList<>();
        if (value == null || value.isBlank()) {
            return result;
        }
        for (String entry : value.split(",")) {
            String trimmed = entry.trim();
            if (!trimmed.isBlank()) {
                result.add(trimmed);
            }
        }
        return result;
    }
}
