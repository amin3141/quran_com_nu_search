package com.quran.omni.search;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Repository for fallback translations loaded from SQLite database.
 * Provides A.J. Arberry translation when GoodMem doesn't return translations.
 */
public final class TranslationRepository {
    private static final Logger logger = LoggerFactory.getLogger(TranslationRepository.class);
    private static final String RESOURCE_PATH = "/en-arberry-simple.db";
    private static final String AUTHOR = "A.J. Arberry";
    private static final String EDITION_ID = "en-arberry";
    private static final String LANG = "en";
    private static final String NAME = "The Koran Interpreted";

    private final Map<String, TranslationInfo> translations = new HashMap<>();

    public TranslationRepository() {
        load();
    }

    private void load() {
        Path tempDb = null;
        try {
            // Extract DB from resources to temp file (SQLite needs file path)
            tempDb = Files.createTempFile("arberry-", ".db");
            try (InputStream is = getClass().getResourceAsStream(RESOURCE_PATH)) {
                if (is == null) {
                    logger.warn("en-arberry-simple.db not found in resources");
                    return;
                }
                Files.copy(is, tempDb, StandardCopyOption.REPLACE_EXISTING);
            }

            // Load all translations into memory
            String url = "jdbc:sqlite:" + tempDb.toAbsolutePath();
            try (Connection conn = DriverManager.getConnection(url);
                 PreparedStatement stmt = conn.prepareStatement("SELECT sura, ayah, ayah_key, text FROM translation");
                 ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {
                    int surah = rs.getInt("sura");
                    int ayah = rs.getInt("ayah");
                    String ayahKey = rs.getString("ayah_key");
                    String text = rs.getString("text");

                    translations.put(ayahKey, new TranslationInfo(
                        surah,
                        ayah,
                        ayahKey,
                        text
                    ));
                }
            }
            logger.info("Loaded {} translations from SQLite", translations.size());

        } catch (IOException | SQLException e) {
            logger.error("Failed to load translations from SQLite", e);
        } finally {
            // Clean up temp file
            if (tempDb != null) {
                try {
                    Files.deleteIfExists(tempDb);
                } catch (IOException ignored) {
                }
            }
        }
    }

    /**
     * Get translation for the given ayah key (e.g., "18:17").
     */
    public Optional<TranslationInfo> getTranslation(String ayahKey) {
        return Optional.ofNullable(translations.get(ayahKey));
    }

    /**
     * Convert to a Models.TranslationResult for the API response.
     */
    public Optional<Models.TranslationResult> toTranslationResult(String ayahKey) {
        return getTranslation(ayahKey).map(info -> new Models.TranslationResult(
            "translation",
            info.ayah(),
            info.ayahKey(),
            info.surah(),
            info.text(),
            AUTHOR,
            EDITION_ID,
            LANG,
            NAME,
            "https://quran.com/" + info.surah() + "/" + info.ayah() + "?translations=en-arberry",
            0.0  // fallback score
        ));
    }

    public record TranslationInfo(
        int surah,
        int ayah,
        String ayahKey,
        String text
    ) {}
}
