package com.quran.omni.goodmem;

import com.quran.omni.AppConfig;
import com.quran.omni.SpaceType;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SpaceRegistry {
    private static final Logger logger = LoggerFactory.getLogger(SpaceRegistry.class);

    private final GoodMemClient client;
    private final Map<SpaceType, String> overrides;
    private final long cacheTtlSeconds;

    private volatile Map<SpaceType, String> cached;
    private volatile Instant lastFetch;

    public SpaceRegistry(GoodMemClient client, AppConfig config) {
        this.client = client;
        this.overrides = new EnumMap<>(config.spaceIdOverrides());
        this.cacheTtlSeconds = config.spaceCacheTtl().getSeconds();
    }

    public Map<SpaceType, String> resolve() {
        if (overrides.size() == SpaceType.values().length) {
            return new EnumMap<>(overrides);
        }

        if (cached == null || isExpired()) {
            refresh();
        }

        Map<SpaceType, String> result = new EnumMap<>(SpaceType.class);
        if (cached != null) {
            result.putAll(cached);
        }
        result.putAll(overrides);
        return result;
    }

    private boolean isExpired() {
        if (lastFetch == null) {
            return true;
        }
        return lastFetch.plusSeconds(cacheTtlSeconds).isBefore(Instant.now());
    }

    private synchronized void refresh() {
        if (cached != null && !isExpired()) {
            return;
        }
        try {
            Map<String, String> spaces = client.listSpaces();
            Map<SpaceType, String> mapped = new EnumMap<>(SpaceType.class);
            for (Map.Entry<String, String> entry : spaces.entrySet()) {
                Optional<SpaceType> type = SpaceType.fromString(entry.getKey());
                type.ifPresent(spaceType -> mapped.put(spaceType, entry.getValue()));
            }
            cached = mapped;
            lastFetch = Instant.now();
        } catch (Exception ex) {
            logger.warn("Failed to refresh space registry", ex);
        }
    }
}
