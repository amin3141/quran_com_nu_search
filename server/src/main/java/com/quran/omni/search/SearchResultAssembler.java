package com.quran.omni.search;

import com.fasterxml.jackson.databind.JsonNode;
import com.quran.omni.AppConfig;
import com.quran.omni.SpaceType;
import com.quran.omni.goodmem.GoodMemClient;
import com.quran.omni.goodmem.GoodMemClient.MemoryHit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SearchResultAssembler {
    private static final Logger logger = LoggerFactory.getLogger(SearchResultAssembler.class);

    private final GoodMemClient client;
    private final AppConfig config;
    private final QuranTextRepository quranTextRepo;
    private final TranslationRepository translationRepo;

    public SearchResultAssembler(GoodMemClient client, AppConfig config) {
        this.client = client;
        this.config = config;
        this.quranTextRepo = new QuranTextRepository();
        this.translationRepo = new TranslationRepository();
    }

    public Models.SearchResponse assemble(
        String traceId,
        String query,
        String language,
        Set<SpaceType> requestedSpaces,
        Map<SpaceType, String> spaceIds,
        List<MemoryHit> hits,
        Models.AiOverview aiOverview,
        List<Models.AgentToolCall> toolCalls,
        Models.AgentMetadata agentMetadata
    ) {
        String resolvedLanguage = language == null || language.isBlank()
            ? config.defaultLanguage()
            : language.toLowerCase(Locale.ROOT);
        logger.info(
            "[{}] assemble.start query={} language={} requestedSpaces={} rawHits={}",
            traceId,
            quoted(query),
            resolvedLanguage,
            requestedSpaces,
            hits.size()
        );

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
            AyahAggregate aggregate = ayahMap.computeIfAbsent(
                result.ayah_key(),
                key -> new AyahAggregate(result.ayah_key(), result.surah(), result.ayah())
            );
            aggregate.setQuran(result);
        }

        for (Models.TranslationResult result : translationResults) {
            if (!isValidAyahKey(result.ayah_key())) {
                continue;
            }
            AyahAggregate aggregate = ayahMap.computeIfAbsent(
                result.ayah_key(),
                key -> new AyahAggregate(result.ayah_key(), result.surah(), result.ayah())
            );
            aggregate.addTranslation(result);
        }

        for (Models.TafsirResult result : tafsirResults) {
            if (!isValidAyahKey(result.ayah_key())) {
                continue;
            }
            AyahAggregate aggregate = ayahMap.computeIfAbsent(
                result.ayah_key(),
                key -> new AyahAggregate(result.ayah_key(), result.surah(), result.ayah())
            );
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
                AyahAggregate aggregate = ayahMap.computeIfAbsent(ayahKey, AyahAggregate::fromKey);
                aggregate.addPost(result);
            }
            if (hasValidAyah) {
                attachedPostIds.add(result.post_id());
            }
        }

        if (requestedSpaces.contains(SpaceType.QURAN)) {
            ensureQuranText(query, spaceIds, ayahMap);
        }
        if (requestedSpaces.contains(SpaceType.TRANSLATION)) {
            ensureTranslations(ayahMap);
        }

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
        logger.info(
            "[{}] assemble.done ayahResults={} directHits={} totalResults={} topAyahs={} topDirectHits={}",
            traceId,
            ayahResults.size(),
            directHits.size(),
            totalResults,
            ayahResults.stream()
                .limit(12)
                .map(result -> "{ayahKey=" + result.ayah_key()
                    + ",topScore=" + String.format(Locale.ROOT, "%.6f", result.topScore())
                    + ",hasQuran=" + (result.quran() != null)
                    + ",translations=" + result.translations().size()
                    + ",tafsirs=" + result.tafsirs().size()
                    + ",posts=" + result.posts().size()
                    + "}")
                .collect(Collectors.joining(", ", "[", "]")),
            directHits.stream()
                .limit(8)
                .map(hit -> abbreviated(String.valueOf(hit), 220))
                .collect(Collectors.joining(", ", "[", "]"))
        );
        return new Models.SearchResponse(
            query,
            aiOverview,
            directHits,
            ayahResults,
            totalResults,
            toolCalls,
            agentMetadata
        );
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
                AyahAggregate aggregate = ayahMap.computeIfAbsent(
                    ayahKey,
                    key -> new AyahAggregate(ayahKey, verse.surah(), verse.ayah())
                );
                aggregate.setQuran(quran);
            } else {
                stillMissing.add(ayahKey);
            }
        }

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
                    AyahAggregate aggregate = ayahMap.computeIfAbsent(
                        quran.ayah_key(),
                        key -> new AyahAggregate(quran.ayah_key(), quran.surah(), quran.ayah())
                    );
                    aggregate.setQuran(quran);
                }
            } catch (Exception ex) {
                logger.warn("Failed to fetch Quran text", ex);
            }
        }
    }

    private void ensureTranslations(Map<String, AyahAggregate> ayahMap) {
        for (AyahAggregate aggregate : ayahMap.values()) {
            if (aggregate.translations.isEmpty() && aggregate.ayahKey != null) {
                translationRepo.toTranslationResult(aggregate.ayahKey)
                    .ifPresent(aggregate::addTranslation);
            }
        }
    }

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

    private static String quoted(String value) {
        if (value == null) {
            return null;
        }
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"").replaceAll("\\s+", " ").trim() + "\"";
    }

    private static String abbreviated(String value, int limit) {
        if (value == null) {
            return null;
        }
        String compact = value.replaceAll("\\s+", " ").trim();
        if (compact.length() <= limit) {
            return compact;
        }
        return compact.substring(0, Math.max(0, limit - 1)) + "…";
    }

    private Models.QuranResult toQuranResult(MemoryHit hit) {
        JsonNode meta = hit.metadata();
        String ayahKey = getString(meta, "ayah_key");
        AyahParts parts = AyahParts.from(meta, ayahKey);
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
        String text = TextCleaner.cleanSnippet(hit.text(), 0);
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
            List<Models.TranslationResult> sortedTranslations = sortByLanguage(
                translations,
                language,
                Models.TranslationResult::lang,
                Models.TranslationResult::score
            );
            List<Models.TafsirResult> sortedTafsirs = sortByLanguage(
                tafsirs,
                language,
                Models.TafsirResult::lang,
                Models.TafsirResult::score
            );
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
