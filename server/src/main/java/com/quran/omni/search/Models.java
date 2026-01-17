package com.quran.omni.search;

import java.util.List;

public final class Models {
    private Models() {
    }

    public record SearchRequest(
        String query,
        List<String> spaces,
        String language,
        Integer limit
    ) {}

    public record SearchResponse(
        String query,
        List<Object> directHits,
        List<ConsolidatedAyahResult> ayahResults,
        int totalResults
    ) {}

    public record QuranResult(
        String type,
        int ayah,
        String ayah_key,
        int surah,
        String text,
        String edition_id,
        String edition_type,
        String lang,
        String name,
        String url,
        double score
    ) {}

    public record TranslationResult(
        String type,
        int ayah,
        String ayah_key,
        int surah,
        String text,
        String author,
        String edition_id,
        String lang,
        String name,
        String url,
        double score
    ) {}

    public record TafsirResult(
        String type,
        int ayah,
        String ayah_key,
        int surah,
        String text,
        String author,
        String edition_id,
        String lang,
        String name,
        String url,
        double score
    ) {}

    public record PostResult(
        String type,
        String post_id,
        String reflection_id,
        String text,
        String username,
        String display_name,
        List<String> ayah_keys,
        List<Integer> surahs,
        String category,
        int likes_count,
        String created_at,
        String url,
        double score
    ) {}

    public record CourseResult(
        String type,
        String course_id,
        String course_title,
        String course_slug,
        String lesson_id,
        String lesson_title,
        String lesson_slug,
        String text,
        String lang,
        List<String> tags,
        String url,
        double score
    ) {}

    public record ArticleResult(
        String type,
        String title,
        String slug,
        String text,
        String url,
        double score
    ) {}

    public record ConsolidatedAyahResult(
        String ayah_key,
        int surah,
        int ayah,
        QuranResult quran,
        List<TranslationResult> translations,
        List<TafsirResult> tafsirs,
        List<PostResult> posts,
        List<CourseResult> courses,
        List<ArticleResult> articles,
        double topScore
    ) {}
}
