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
    Duration spaceCacheTtl,
    String rerankerId,
    int rerankCandidateSize,
    boolean rerankChronologicalResort,
    String overviewLlmId,
    String overviewSysPrompt,
    String overviewPrompt,
    int overviewTokenBudget,
    double overviewTemperature,
    int overviewMaxResults,
    int overviewCandidateSize,
    Double overviewRelevanceThreshold
) {
    private static final String DEFAULT_OVERVIEW_SYS_PROMPT = String.join("\n",
        "You are an AI assistant generating a concise AI Overview for Quran.com search.",
        "Use only the retrieved memory data. Do not add facts that are not present.",
        "If the data is insufficient, say so plainly.",
        "Keep the overview under 4 sentences. Avoid recommendations.",
        "Do not use bracketed citations like [1] or [2].",
        "Exclude reflection content; focus on Quran, translations, and tafsir."
    );

    private static final String DEFAULT_OVERVIEW_PROMPT = String.join("\n",
        "User query: \"{{ userQuery }}\"",
        "",
        "Retrieved data:",
        "{{ dataSection }}",
        "",
        "Write an AI overview that synthesizes the retrieved data and answers the query.",
        "Use only Quran, translation, and tafsir content; ignore reflections or community posts.",
        "After each factual assertion, add a brief verbatim snippet in quotes inside parentheses.",
        "Use short quotes from Quran, translations, and tafsir (verbatim).",
        "Do not use bracketed references like [1] or [2].",
        "If sources disagree, mention that briefly with a quoted snippet."
    );

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
        String rerankerId = readEnv("SEARCH_RERANKER_ID", "019bd887-2953-7562-92b8-964abb5bffa4");
        int rerankCandidateSize = readIntEnv("SEARCH_RERANK_CANDIDATES", 100);
        boolean rerankChronologicalResort = readBoolEnv("SEARCH_RERANK_CHRONOLOGICAL_RESORT", false);

        String overviewLlmId = readEnv("SEARCH_OVERVIEW_LLM_ID", "019bc775-3b20-767f-a15f-42cda8039b2c");
        String overviewSysPrompt = readEnv("SEARCH_OVERVIEW_SYS_PROMPT", DEFAULT_OVERVIEW_SYS_PROMPT);
        String overviewPrompt = readEnv("SEARCH_OVERVIEW_PROMPT", DEFAULT_OVERVIEW_PROMPT);
        int overviewTokenBudget = readIntEnv("SEARCH_OVERVIEW_TOKEN_BUDGET", 256);
        double overviewTemperature = readDoubleEnv("SEARCH_OVERVIEW_TEMP", 0.3);
        int overviewMaxResults = readIntEnv("SEARCH_OVERVIEW_MAX_RESULTS", 8);
        int overviewCandidateSize = readIntEnv("SEARCH_OVERVIEW_CANDIDATES", 24);
        Double overviewRelevanceThreshold = readNullableDoubleEnv("SEARCH_OVERVIEW_RELEVANCE_THRESHOLD");

        return new AppConfig(
            port,
            baseUrl,
            apiKey,
            insecureSsl,
            spaceIdOverrides,
            spaceLimits,
            defaultLanguage,
            spaceCacheTtl,
            rerankerId,
            rerankCandidateSize,
            rerankChronologicalResort,
            overviewLlmId,
            overviewSysPrompt,
            overviewPrompt,
            overviewTokenBudget,
            overviewTemperature,
            overviewMaxResults,
            overviewCandidateSize,
            overviewRelevanceThreshold
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

    private static double readDoubleEnv(String key, double fallback) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static Double readNullableDoubleEnv(String key) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException ignored) {
            return null;
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
