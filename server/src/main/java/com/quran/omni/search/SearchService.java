package com.quran.omni.search;

import com.fasterxml.jackson.databind.JsonNode;
import com.quran.omni.AppConfig;
import com.quran.omni.SpaceType;
import com.quran.omni.goodmem.GoodMemClient;
import com.quran.omni.goodmem.GoodMemClient.MemoryHit;
import com.quran.omni.goodmem.SpaceRegistry;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SearchService {
    private static final Logger logger = LoggerFactory.getLogger(SearchService.class);

    private final GoodMemClient client;
    private final SpaceRegistry spaceRegistry;
    private final AppConfig config;
    private final ExecutorService executor;
    private final QuranTextRepository quranTextRepo;
    private final TranslationRepository translationRepo;

    public SearchService(GoodMemClient client, SpaceRegistry spaceRegistry, AppConfig config) {
        this.client = client;
        this.spaceRegistry = spaceRegistry;
        this.config = config;
        this.executor = Executors.newFixedThreadPool(6);
        this.quranTextRepo = new QuranTextRepository();
        this.translationRepo = new TranslationRepository();
    }

    public Models.SearchResponse search(Models.SearchRequest request) {
        String query = request.query() == null ? "" : request.query().trim();
        if (query.isBlank()) {
            throw new IllegalArgumentException("query is required");
        }

        String language = request.language();
        if (language == null || language.isBlank()) {
            language = config.defaultLanguage();
        }
        final String resolvedLanguage = language.toLowerCase(Locale.ROOT);

        EnumSet<SpaceType> requestedSpaces = parseSpaces(request.spaces());
        Map<SpaceType, String> spaceIds = spaceRegistry.resolve();

        List<CompletableFuture<List<MemoryHit>>> futures = new ArrayList<>();
        for (SpaceType spaceType : requestedSpaces) {
            String spaceId = spaceIds.get(spaceType);
            if (spaceId == null || spaceId.isBlank()) {
                logger.warn("Missing space ID for {}", spaceType);
                continue;
            }
            int limit = resolveLimit(spaceType, request.limit());
            futures.add(CompletableFuture.supplyAsync(() -> {
                try {
                    return client.retrieve(query, spaceType, spaceId, limit, null);
                } catch (Exception ex) {
                    logger.warn("Search failed for {}", spaceType, ex);
                    return List.of();
                }
            }, executor));
        }

        List<MemoryHit> hits = futures.stream()
            .map(CompletableFuture::join)
            .flatMap(List::stream)
            .collect(Collectors.toList());

        List<Models.QuranResult> quranResults = new ArrayList<>();
        List<Models.TranslationResult> translationResults = new ArrayList<>();
        List<Models.TafsirResult> tafsirResults = new ArrayList<>();
        List<Models.PostResult> postResults = new ArrayList<>();
        List<Models.CourseResult> courseResults = new ArrayList<>();
        List<Models.ArticleResult> articleResults = new ArrayList<>();

        for (MemoryHit hit : hits) {
            switch (hit.spaceType()) {
                case QURAN -> quranResults.add(toQuranResult(hit));
                case TRANSLATION -> translationResults.add(toTranslationResult(hit));
                case TAFSIR -> tafsirResults.add(toTafsirResult(hit));
                case POST -> postResults.add(toPostResult(hit));
                case COURSE -> courseResults.add(toCourseResult(hit));
                case ARTICLE -> articleResults.add(toArticleResult(hit));
            }
        }

        Map<String, AyahAggregate> ayahMap = new HashMap<>();

        for (Models.QuranResult result : quranResults) {
            if (!isValidAyahKey(result.ayah_key())) {
                continue;
            }
            AyahAggregate aggregate = ayahMap.computeIfAbsent(result.ayah_key(), key -> new AyahAggregate(result.ayah_key(), result.surah(), result.ayah()));
            aggregate.setQuran(result);
        }

        for (Models.TranslationResult result : translationResults) {
            if (!isValidAyahKey(result.ayah_key())) {
                continue;
            }
            AyahAggregate aggregate = ayahMap.computeIfAbsent(result.ayah_key(), key -> new AyahAggregate(result.ayah_key(), result.surah(), result.ayah()));
            aggregate.addTranslation(result);
        }

        for (Models.TafsirResult result : tafsirResults) {
            if (!isValidAyahKey(result.ayah_key())) {
                continue;
            }
            AyahAggregate aggregate = ayahMap.computeIfAbsent(result.ayah_key(), key -> new AyahAggregate(result.ayah_key(), result.surah(), result.ayah()));
            aggregate.addTafsir(result);
        }

        Set<String> attachedPostIds = new LinkedHashSet<>();
        for (Models.PostResult result : postResults) {
            if (result.ayah_keys() == null || result.ayah_keys().isEmpty()) {
                continue;
            }
            boolean hasValidAyah = false;
            for (String ayahKey : result.ayah_keys()) {
                if (!isValidAyahKey(ayahKey)) {
                    continue;
                }
                hasValidAyah = true;
                AyahAggregate aggregate = ayahMap.computeIfAbsent(ayahKey, key -> AyahAggregate.fromKey(ayahKey));
                aggregate.addPost(result);
            }
            if (hasValidAyah) {
                attachedPostIds.add(result.post_id());
            }
        }

        ensureQuranText(query, spaceIds, ayahMap);
        ensureTranslations(ayahMap);

        List<Models.ConsolidatedAyahResult> ayahResults = ayahMap.values().stream()
            .map(aggregate -> aggregate.toResult(resolvedLanguage))
            .sorted(Comparator.comparingDouble(Models.ConsolidatedAyahResult::topScore).reversed())
            .collect(Collectors.toList());

        List<ScoredDirectHit> directHitCandidates = new ArrayList<>();
        for (Models.PostResult post : postResults) {
            if (!attachedPostIds.contains(post.post_id())) {
                directHitCandidates.add(new ScoredDirectHit(post.score(), post));
            }
        }
        for (Models.CourseResult course : courseResults) {
            directHitCandidates.add(new ScoredDirectHit(course.score(), course));
        }
        for (Models.ArticleResult article : articleResults) {
            directHitCandidates.add(new ScoredDirectHit(article.score(), article));
        }

        List<Object> directHits = directHitCandidates.stream()
            .sorted(Comparator.comparingDouble(ScoredDirectHit::score).reversed())
            .map(ScoredDirectHit::hit)
            .collect(Collectors.toList());

        int totalResults = directHits.size() + ayahResults.size();
        return new Models.SearchResponse(query, directHits, ayahResults, totalResults);
    }

    public void shutdown() {
        executor.shutdownNow();
    }

    private void ensureQuranText(
        String query,
        Map<SpaceType, String> spaceIds,
        Map<String, AyahAggregate> ayahMap
    ) {
        if (ayahMap.isEmpty()) {
            return;
        }
        List<String> missingKeys = ayahMap.values().stream()
            .filter(aggregate -> aggregate.quran == null)
            .map(aggregate -> aggregate.ayahKey)
            .filter(Objects::nonNull)
            .distinct()
            .collect(Collectors.toList());

        if (missingKeys.isEmpty()) {
            return;
        }

        // First, try to get from local quran.json repository (has tashkeel)
        List<String> stillMissing = new ArrayList<>();
        for (String ayahKey : missingKeys) {
            var verseOpt = quranTextRepo.getVerse(ayahKey);
            if (verseOpt.isPresent()) {
                var verse = verseOpt.get();
                Models.QuranResult quran = new Models.QuranResult(
                    "quran",
                    verse.ayah(),
                    verse.ayahKey(),
                    verse.surah(),
                    verse.text(),
                    "quran-uthmani",
                    "quran",
                    "ar",
                    "Uthmani",
                    "https://quran.com/" + verse.surah() + "/" + verse.ayah(),
                    0.0
                );
                AyahAggregate aggregate = ayahMap.computeIfAbsent(ayahKey, key -> new AyahAggregate(ayahKey, verse.surah(), verse.ayah()));
                aggregate.setQuran(quran);
            } else {
                stillMissing.add(ayahKey);
            }
        }

        // Fall back to GoodMem for any remaining
        if (stillMissing.isEmpty()) {
            return;
        }

        String spaceId = spaceIds.get(SpaceType.QURAN);
        if (spaceId == null || spaceId.isBlank()) {
            return;
        }

        List<List<String>> batches = batch(stillMissing, 25);
        for (List<String> batch : batches) {
            String filter = buildAyahFilter(batch);
            try {
                List<MemoryHit> hits = client.retrieve(query, SpaceType.QURAN, spaceId, batch.size(), filter);
                for (MemoryHit hit : hits) {
                    Models.QuranResult quran = toQuranResult(hit);
                    AyahAggregate aggregate = ayahMap.computeIfAbsent(quran.ayah_key(), key -> new AyahAggregate(quran.ayah_key(), quran.surah(), quran.ayah()));
                    aggregate.setQuran(quran);
                }
            } catch (Exception ex) {
                logger.warn("Failed to fetch Quran text", ex);
            }
        }
    }

    /**
     * Ensure every ayah has at least one translation by falling back to local SQLite DB.
     */
    private void ensureTranslations(Map<String, AyahAggregate> ayahMap) {
        for (AyahAggregate aggregate : ayahMap.values()) {
            if (aggregate.translations.isEmpty() && aggregate.ayahKey != null) {
                translationRepo.toTranslationResult(aggregate.ayahKey)
                    .ifPresent(aggregate::addTranslation);
            }
        }
    }

    private static EnumSet<SpaceType> parseSpaces(List<String> spaces) {
        if (spaces == null || spaces.isEmpty()) {
            return EnumSet.allOf(SpaceType.class);
        }
        EnumSet<SpaceType> result = EnumSet.noneOf(SpaceType.class);
        for (String entry : spaces) {
            if (entry == null) {
                continue;
            }
            for (String value : entry.split(",")) {
                SpaceType.fromString(value).ifPresent(result::add);
            }
        }
        if (result.isEmpty()) {
            return EnumSet.allOf(SpaceType.class);
        }
        return result;
    }

    private int resolveLimit(SpaceType spaceType, Integer limitOverride) {
        if (limitOverride != null && limitOverride > 0) {
            return limitOverride;
        }
        return config.spaceLimits().getOrDefault(spaceType, 8);
    }

    /**
     * Check if an ayah key is valid (format "surah:ayah" with valid numbers).
     */
    private static boolean isValidAyahKey(String key) {
        if (key == null || key.isBlank()) {
            return false;
        }
        String[] parts = key.split(":", 2);
        if (parts.length != 2) {
            return false;
        }
        try {
            int surah = Integer.parseInt(parts[0].trim());
            int ayah = Integer.parseInt(parts[1].trim());
            // Valid surah range: 1-114, valid ayah: 1+
            return surah >= 1 && surah <= 114 && ayah >= 1;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private static String buildAyahFilter(List<String> ayahKeys) {
        return ayahKeys.stream()
            .map(key -> "CAST(val('$.ayah_key') AS TEXT) = '" + escapeFilterValue(key) + "'")
            .collect(Collectors.joining(" OR "));
    }

    private static String escapeFilterValue(String value) {
        return value.replace("'", "''");
    }

    private Models.QuranResult toQuranResult(MemoryHit hit) {
        JsonNode meta = hit.metadata();
        String ayahKey = getString(meta, "ayah_key");
        AyahParts parts = AyahParts.from(meta, ayahKey);

        // Use quran.json text with tashkeel if available, otherwise fall back to GoodMem text
        String text = quranTextRepo.getVerseText(ayahKey).orElse(hit.text());

        return new Models.QuranResult(
            "quran",
            parts.ayah,
            ayahKey,
            parts.surah,
            text,
            getString(meta, "edition_id"),
            getString(meta, "edition_type"),
            getString(meta, "lang"),
            getString(meta, "name"),
            getString(meta, "url"),
            hit.score()
        );
    }

    private Models.TranslationResult toTranslationResult(MemoryHit hit) {
        JsonNode meta = hit.metadata();
        String ayahKey = getString(meta, "ayah_key");
        AyahParts parts = AyahParts.from(meta, ayahKey);
        String author = getString(meta, "author");
        if (author == null || author.isBlank()) {
            author = getString(meta, "name");
        }
        return new Models.TranslationResult(
            "translation",
            parts.ayah,
            ayahKey,
            parts.surah,
            hit.text(),
            author,
            getString(meta, "edition_id"),
            getString(meta, "lang"),
            getString(meta, "name"),
            getString(meta, "url"),
            hit.score()
        );
    }

    private Models.TafsirResult toTafsirResult(MemoryHit hit) {
        JsonNode meta = hit.metadata();
        String ayahKey = getString(meta, "ayah_key");
        AyahParts parts = AyahParts.from(meta, ayahKey);
        String author = getString(meta, "author");
        if (author == null || author.isBlank()) {
            author = getString(meta, "name");
        }
        String text = TextCleaner.cleanSnippet(hit.text(), 400);
        return new Models.TafsirResult(
            "tafsir",
            parts.ayah,
            ayahKey,
            parts.surah,
            text,
            author,
            getString(meta, "edition_id"),
            getString(meta, "lang"),
            getString(meta, "name"),
            getString(meta, "url"),
            hit.score()
        );
    }

    private Models.PostResult toPostResult(MemoryHit hit) {
        JsonNode meta = hit.metadata();
        String text = TextCleaner.cleanSnippet(hit.text(), 400);
        return new Models.PostResult(
            "post",
            getString(meta, "post_id"),
            getString(meta, "reflection_id"),
            text,
            getString(meta, "username"),
            getString(meta, "display_name"),
            getStringList(meta, "ayah_keys"),
            getIntList(meta, "surahs"),
            getString(meta, "category"),
            getInt(meta, "likes_count"),
            getString(meta, "created_at"),
            getString(meta, "url"),
            hit.score()
        );
    }

    private Models.CourseResult toCourseResult(MemoryHit hit) {
        JsonNode meta = hit.metadata();
        String text = TextCleaner.cleanSnippet(hit.text(), 400);
        return new Models.CourseResult(
            "course",
            getString(meta, "course_id"),
            getString(meta, "course_title"),
            getString(meta, "course_slug"),
            getString(meta, "lesson_id"),
            getString(meta, "lesson_title"),
            getString(meta, "lesson_slug"),
            text,
            getString(meta, "lang"),
            getStringList(meta, "tags"),
            getString(meta, "url"),
            hit.score()
        );
    }

    private Models.ArticleResult toArticleResult(MemoryHit hit) {
        JsonNode meta = hit.metadata();
        String text = TextCleaner.cleanSnippet(hit.text(), 400);
        return new Models.ArticleResult(
            "article",
            getString(meta, "title"),
            getString(meta, "slug"),
            text,
            getString(meta, "url"),
            hit.score()
        );
    }

    private static String getString(JsonNode node, String field) {
        if (node == null || node.isMissingNode()) {
            return null;
        }
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        if (value.isNumber()) {
            double numeric = value.asDouble();
            if (Math.rint(numeric) == numeric) {
                return Long.toString((long) numeric);
            }
            return Double.toString(numeric);
        }
        return value.asText();
    }

    private static int getInt(JsonNode node, String field) {
        if (node == null || node.isMissingNode()) {
            return 0;
        }
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return 0;
        }
        if (value.isNumber()) {
            return value.asInt();
        }
        try {
            return Integer.parseInt(value.asText().trim());
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private static List<String> getStringList(JsonNode node, String field) {
        List<String> result = new ArrayList<>();
        if (node == null || node.isMissingNode()) {
            return result;
        }
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return result;
        }
        if (value.isArray()) {
            for (JsonNode item : value) {
                if (item == null || item.isNull()) {
                    continue;
                }
                result.add(item.asText());
            }
            return result;
        }
        result.add(value.asText());
        return result;
    }

    private static List<Integer> getIntList(JsonNode node, String field) {
        List<Integer> result = new ArrayList<>();
        if (node == null || node.isMissingNode()) {
            return result;
        }
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return result;
        }
        if (value.isArray()) {
            for (JsonNode item : value) {
                if (item == null || item.isNull()) {
                    continue;
                }
                if (item.isNumber()) {
                    result.add(item.asInt());
                } else {
                    try {
                        result.add(Integer.parseInt(item.asText().trim()));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
            return result;
        }
        try {
            result.add(Integer.parseInt(value.asText().trim()));
        } catch (NumberFormatException ignored) {
        }
        return result;
    }

    private static List<List<String>> batch(List<String> values, int batchSize) {
        List<List<String>> batches = new ArrayList<>();
        int index = 0;
        while (index < values.size()) {
            int end = Math.min(values.size(), index + batchSize);
            batches.add(values.subList(index, end));
            index = end;
        }
        return batches;
    }

    private record AyahParts(int surah, int ayah) {
        static AyahParts from(JsonNode meta, String ayahKey) {
            int surah = getInt(meta, "surah");
            int ayah = getInt(meta, "ayah");
            if ((surah == 0 || ayah == 0) && ayahKey != null) {
                AyahParts parsed = AyahParts.fromKey(ayahKey);
                surah = parsed.surah;
                ayah = parsed.ayah;
            }
            return new AyahParts(surah, ayah);
        }

        static AyahParts fromKey(String key) {
            if (key == null) {
                return new AyahParts(0, 0);
            }
            String[] parts = key.split(":", 2);
            if (parts.length != 2) {
                return new AyahParts(0, 0);
            }
            try {
                return new AyahParts(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
            } catch (NumberFormatException ex) {
                return new AyahParts(0, 0);
            }
        }
    }

    private static final class AyahAggregate {
        private final String ayahKey;
        private int surah;
        private int ayah;
        private Models.QuranResult quran;
        private final List<Models.TranslationResult> translations = new ArrayList<>();
        private final List<Models.TafsirResult> tafsirs = new ArrayList<>();
        private final List<Models.PostResult> posts = new ArrayList<>();
        private double topScore;

        private AyahAggregate(String ayahKey, int surah, int ayah) {
            this.ayahKey = ayahKey;
            this.surah = surah;
            this.ayah = ayah;
        }

        private static AyahAggregate fromKey(String ayahKey) {
            AyahParts parts = AyahParts.fromKey(ayahKey);
            return new AyahAggregate(ayahKey, parts.surah, parts.ayah);
        }

        private void setQuran(Models.QuranResult quran) {
            if (quran == null) {
                return;
            }
            if (this.quran == null || quran.score() > this.quran.score()) {
                this.quran = quran;
            }
            updateScore(quran.score());
            updateAyah(quran.surah(), quran.ayah());
        }

        private void addTranslation(Models.TranslationResult translation) {
            if (translation == null) {
                return;
            }
            translations.add(translation);
            updateScore(translation.score());
            updateAyah(translation.surah(), translation.ayah());
        }

        private void addTafsir(Models.TafsirResult tafsir) {
            if (tafsir == null) {
                return;
            }
            tafsirs.add(tafsir);
            updateScore(tafsir.score());
            updateAyah(tafsir.surah(), tafsir.ayah());
        }

        private void addPost(Models.PostResult post) {
            if (post == null) {
                return;
            }
            posts.add(post);
            updateScore(post.score());
        }

        private void updateScore(double score) {
            if (score > topScore) {
                topScore = score;
            }
        }

        private void updateAyah(int surah, int ayah) {
            if (this.surah == 0 && surah > 0) {
                this.surah = surah;
            }
            if (this.ayah == 0 && ayah > 0) {
                this.ayah = ayah;
            }
        }

        private Models.ConsolidatedAyahResult toResult(String language) {
            List<Models.TranslationResult> sortedTranslations = sortByLanguage(translations, language, Models.TranslationResult::lang, Models.TranslationResult::score);
            List<Models.TafsirResult> sortedTafsirs = sortByLanguage(tafsirs, language, Models.TafsirResult::lang, Models.TafsirResult::score);
            posts.sort(Comparator.comparingDouble(Models.PostResult::score).reversed());

            return new Models.ConsolidatedAyahResult(
                ayahKey,
                surah,
                ayah,
                quran,
                sortedTranslations,
                sortedTafsirs,
                posts,
                List.of(),
                List.of(),
                topScore
            );
        }

        private static <T> List<T> sortByLanguage(
            List<T> items,
            String language,
            java.util.function.Function<T, String> langGetter,
            java.util.function.ToDoubleFunction<T> scoreGetter
        ) {
            if (items.isEmpty()) {
                return List.of();
            }
            List<T> preferred = new ArrayList<>();
            List<T> other = new ArrayList<>();
            for (T item : items) {
                String lang = langGetter.apply(item);
                if (lang != null && lang.equalsIgnoreCase(language)) {
                    preferred.add(item);
                } else {
                    other.add(item);
                }
            }
            preferred.sort(Comparator.comparingDouble(scoreGetter).reversed());
            other.sort(Comparator.comparingDouble(scoreGetter).reversed());
            List<T> combined = new ArrayList<>(preferred.size() + other.size());
            combined.addAll(preferred);
            combined.addAll(other);
            return combined;
        }
    }

    private record ScoredDirectHit(double score, Object hit) {
    }
}
