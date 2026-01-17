package com.quran.omni.search;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Repository for Quran verses with full tashkeel (diacritical marks).
 * Loads from quran.json resource file at startup.
 */
public final class QuranTextRepository {
    private static final Logger logger = LoggerFactory.getLogger(QuranTextRepository.class);
    private static final String RESOURCE_PATH = "/quran.json";

    private final Map<String, VerseInfo> verses = new HashMap<>();
    private final Map<Integer, SurahInfo> surahs = new HashMap<>();

    public QuranTextRepository() {
        load();
    }

    private void load() {
        try (InputStream is = getClass().getResourceAsStream(RESOURCE_PATH)) {
            if (is == null) {
                logger.warn("quran.json not found in resources");
                return;
            }
            ObjectMapper mapper = new ObjectMapper();
            List<SurahData> surahList = mapper.readValue(is, new TypeReference<>() {});

            for (SurahData surah : surahList) {
                surahs.put(surah.id(), new SurahInfo(
                    surah.id(),
                    surah.name(),
                    surah.transliteration(),
                    surah.type(),
                    surah.total_verses()
                ));

                for (VerseData verse : surah.verses()) {
                    String key = surah.id() + ":" + verse.id();
                    verses.put(key, new VerseInfo(
                        surah.id(),
                        verse.id(),
                        key,
                        verse.text(),
                        surah.name(),
                        surah.transliteration()
                    ));
                }
            }
            logger.info("Loaded {} verses from quran.json", verses.size());
        } catch (IOException e) {
            logger.error("Failed to load quran.json", e);
        }
    }

    /**
     * Get the verse text with full tashkeel for the given ayah key (e.g., "2:255").
     */
    public Optional<String> getVerseText(String ayahKey) {
        VerseInfo info = verses.get(ayahKey);
        return info == null ? Optional.empty() : Optional.of(info.text());
    }

    /**
     * Get full verse information for the given ayah key.
     */
    public Optional<VerseInfo> getVerse(String ayahKey) {
        return Optional.ofNullable(verses.get(ayahKey));
    }

    /**
     * Get surah information by number.
     */
    public Optional<SurahInfo> getSurah(int surahNumber) {
        return Optional.ofNullable(surahs.get(surahNumber));
    }

    public record VerseInfo(
        int surah,
        int ayah,
        String ayahKey,
        String text,
        String surahNameArabic,
        String surahNameTransliteration
    ) {}

    public record SurahInfo(
        int id,
        String nameArabic,
        String transliteration,
        String type,
        int totalVerses
    ) {}

    // JSON mapping records
    private record SurahData(
        int id,
        String name,
        String transliteration,
        String type,
        int total_verses,
        List<VerseData> verses
    ) {}

    private record VerseData(int id, String text) {}
}
