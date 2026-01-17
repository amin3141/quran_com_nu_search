package com.quran.omni;

import com.quran.omni.goodmem.GoodMemClient;
import com.quran.omni.goodmem.SpaceRegistry;
import com.quran.omni.search.Models;
import com.quran.omni.search.SearchService;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.json.JavalinJackson;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        AppConfig config = AppConfig.fromEnv();
        GoodMemClient goodMemClient = new GoodMemClient(config);
        SpaceRegistry spaceRegistry = new SpaceRegistry(goodMemClient, config);
        SearchService searchService = new SearchService(goodMemClient, spaceRegistry, config);

        Javalin app = Javalin.create(javalinConfig -> {
            javalinConfig.jsonMapper(new JavalinJackson());
            javalinConfig.routes.before(ctx -> {
                ctx.header("Access-Control-Allow-Origin", "*");
                ctx.header("Access-Control-Allow-Methods", "GET,POST,OPTIONS");
                ctx.header("Access-Control-Allow-Headers", "Content-Type");
            });
            javalinConfig.routes.options("/*", ctx -> ctx.status(HttpStatus.NO_CONTENT));

            javalinConfig.routes.get("/api/health", ctx -> ctx.json(Map.of("status", "ok")));
            javalinConfig.routes.get("/api/search", ctx -> handleSearchQuery(ctx, searchService));
            javalinConfig.routes.post("/api/search", ctx -> handleSearchBody(ctx, searchService));

            javalinConfig.routes.exception(IllegalArgumentException.class, (ex, ctx) -> {
                ctx.status(HttpStatus.BAD_REQUEST).json(Map.of("error", "bad_request", "message", ex.getMessage()));
            });

            javalinConfig.routes.exception(Exception.class, (ex, ctx) -> {
                logger.error("Unexpected error", ex);
                if (!ctx.res().isCommitted()) {
                    ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .json(Map.of("error", "server_error", "message", "Unexpected server error"));
                }
            });

            javalinConfig.events.serverStopping(searchService::shutdown);
        });

        app.start(config.port());
        logger.info("Search server started on port {}", config.port());
    }

    private static void handleSearchQuery(Context ctx, SearchService searchService) {
        String query = firstNonBlank(ctx.queryParam("query"), ctx.queryParam("q"));
        String spaces = ctx.queryParam("spaces");
        String language = firstNonBlank(ctx.queryParam("language"), ctx.queryParam("lang"));
        Integer limit = parseInt(ctx.queryParam("limit"));
        List<String> spaceList = spaces == null || spaces.isBlank() ? null : List.of(spaces);
        Models.SearchRequest request = new Models.SearchRequest(query, spaceList, language, limit);
        Models.SearchResponse response = searchService.search(request);
        ctx.json(response);
    }

    private static void handleSearchBody(Context ctx, SearchService searchService) {
        Models.SearchRequest request = ctx.bodyAsClass(Models.SearchRequest.class);
        Models.SearchResponse response = searchService.search(request);
        ctx.json(response);
    }

    private static String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        if (second != null && !second.isBlank()) {
            return second;
        }
        return null;
    }

    private static Integer parseInt(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
