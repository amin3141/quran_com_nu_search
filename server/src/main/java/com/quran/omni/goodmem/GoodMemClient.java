package com.quran.omni.goodmem;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.quran.omni.AppConfig;
import com.quran.omni.SpaceType;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GoodMemClient {
    private static final Logger logger = LoggerFactory.getLogger(GoodMemClient.class);
    private static final String POST_PROCESSOR_FACTORY =
        "com.goodmem.retrieval.postprocess.ChatPostProcessorFactory";

    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient httpClient;
    private final String baseUrl;
    private final String apiKey;
    private final String rerankerId;
    private final int rerankCandidateSize;
    private final boolean rerankChronologicalResort;
    private final String overviewLlmId;
    private final String overviewSysPrompt;
    private final String overviewPrompt;
    private final int overviewTokenBudget;
    private final double overviewTemperature;
    private final int overviewMaxResults;
    private final int overviewCandidateSize;
    private final Double overviewRelevanceThreshold;

    public GoodMemClient(AppConfig config) {
        this.baseUrl = config.goodMemBaseUrl();
        this.apiKey = config.goodMemApiKey();
        this.rerankerId = config.rerankerId();
        this.rerankCandidateSize = config.rerankCandidateSize();
        this.rerankChronologicalResort = config.rerankChronologicalResort();
        this.overviewLlmId = config.overviewLlmId();
        this.overviewSysPrompt = config.overviewSysPrompt();
        this.overviewPrompt = config.overviewPrompt();
        this.overviewTokenBudget = config.overviewTokenBudget();
        this.overviewTemperature = config.overviewTemperature();
        this.overviewMaxResults = config.overviewMaxResults();
        this.overviewCandidateSize = config.overviewCandidateSize();
        this.overviewRelevanceThreshold = config.overviewRelevanceThreshold();
        this.httpClient = buildClient(config.goodMemInsecureSsl());
    }

    public Map<String, String> listSpaces() throws IOException, InterruptedException {
        URI uri = URI.create(baseUrl + "/v1/spaces?maxResults=200");
        HttpRequest request = HttpRequest.newBuilder(uri)
            .timeout(Duration.ofSeconds(15))
            .header("X-API-Key", apiKey)
            .header("Accept", "application/json")
            .GET()
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("GoodMem list spaces failed: " + response.statusCode());
        }

        JsonNode root = mapper.readTree(response.body());
        Map<String, String> result = new HashMap<>();
        for (JsonNode space : root.path("spaces")) {
            String name = space.path("name").asText(null);
            String id = space.path("spaceId").asText(null);
            if (name != null && id != null) {
                result.put(name, id);
            }
        }
        return result;
    }

    public List<MemoryHit> retrieve(
        String query,
        SpaceType spaceType,
        String spaceId,
        int limit,
        String filter
    ) throws IOException, InterruptedException {
        if (limit <= 0) {
            return List.of();
        }

        ObjectNode payload = mapper.createObjectNode();
        payload.put("message", query);
        int requestedSize = limit;
        if (shouldApplyReranker(filter)) {
            requestedSize = Math.max(limit, resolveRerankCandidateSize());
        }
        payload.put("requestedSize", requestedSize);
        payload.put("fetchMemory", true);
        payload.put("fetchMemoryContent", false);

        ArrayNode spaceKeys = payload.putArray("spaceKeys");
        ObjectNode spaceKey = spaceKeys.addObject();
        spaceKey.put("spaceId", spaceId);
        if (filter != null && !filter.isBlank()) {
            spaceKey.put("filter", filter);
        }
        if (shouldApplyReranker(filter)) {
            ObjectNode postProcessor = payload.putObject("postProcessor");
            postProcessor.put("name", POST_PROCESSOR_FACTORY);
            ObjectNode config = postProcessor.putObject("config");
            config.put("reranker_id", rerankerId);
            config.put("max_results", limit);
            config.put("chronological_resort", rerankChronologicalResort);
        }

        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/v1/memories:retrieve"))
            .timeout(Duration.ofSeconds(60))
            .header("X-API-Key", apiKey)
            .header("Content-Type", "application/json")
            .header("Accept", "application/x-ndjson")
            .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
            .build();

        HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() != 200) {
            String errorBody = readErrorBody(response.body());
            throw new IOException("GoodMem retrieve failed: " + response.statusCode() + " " + errorBody);
        }

        Map<String, JsonNode> metadataByMemoryId = new HashMap<>();
        Map<String, String> textByMemoryId = new HashMap<>();
        Map<String, Double> scoreByMemoryId = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                JsonNode root = mapper.readTree(line);

                JsonNode memoryDefinition = root.get("memoryDefinition");
                if (memoryDefinition != null && !memoryDefinition.isNull()) {
                    String memoryId = memoryDefinition.path("memoryId").asText(null);
                    JsonNode metadata = memoryDefinition.path("metadata");
                    if (memoryId != null && metadata != null && !metadata.isMissingNode()) {
                        metadataByMemoryId.put(memoryId, metadata);
                    }
                }

                JsonNode retrievedItem = root.get("retrievedItem");
                if (retrievedItem != null && !retrievedItem.isNull()) {
                    JsonNode chunkWrapper = retrievedItem.get("chunk");
                    if (chunkWrapper != null && !chunkWrapper.isNull()) {
                        JsonNode chunk = chunkWrapper.get("chunk");
                        if (chunk != null && !chunk.isNull()) {
                            String memoryId = chunk.path("memoryId").asText(null);
                            String text = chunk.path("chunkText").asText(null);
                            double relevanceScore = chunkWrapper.path("relevanceScore").asDouble(Double.NaN);
                            if (memoryId != null && text != null) {
                                double score = normalizeScore(relevanceScore);
                                Double existingScore = scoreByMemoryId.get(memoryId);
                                if (existingScore == null || score > existingScore) {
                                    scoreByMemoryId.put(memoryId, score);
                                    textByMemoryId.put(memoryId, text);
                                }
                            }
                        }
                    }
                }
            }
        }

        List<MemoryHit> hits = new ArrayList<>();
        for (Map.Entry<String, Double> entry : scoreByMemoryId.entrySet()) {
            String memoryId = entry.getKey();
            JsonNode metadata = metadataByMemoryId.get(memoryId);
            String text = textByMemoryId.get(memoryId);
            if (metadata == null || text == null) {
                continue;
            }
            hits.add(new MemoryHit(spaceType, memoryId, metadata, text, entry.getValue()));
        }

        return hits;
    }

    public boolean isOverviewEnabled() {
        return overviewLlmId != null && !overviewLlmId.isBlank();
    }

    public String generateOverview(String query, List<String> spaceIds) throws IOException, InterruptedException {
        if (!isOverviewEnabled() || spaceIds == null || spaceIds.isEmpty()) {
            return null;
        }

        ObjectNode payload = mapper.createObjectNode();
        payload.put("message", query);
        payload.put("requestedSize", resolveOverviewCandidateSize());
        payload.put("fetchMemory", true);
        payload.put("fetchMemoryContent", false);

        ArrayNode spaceKeys = payload.putArray("spaceKeys");
        for (String spaceId : spaceIds) {
            if (spaceId == null || spaceId.isBlank()) {
                continue;
            }
            ObjectNode spaceKey = spaceKeys.addObject();
            spaceKey.put("spaceId", spaceId);
        }
        if (spaceKeys.isEmpty()) {
            return null;
        }

        ObjectNode postProcessor = payload.putObject("postProcessor");
        postProcessor.put("name", POST_PROCESSOR_FACTORY);
        ObjectNode config = postProcessor.putObject("config");
        config.put("llm_id", overviewLlmId);
        if (rerankerId != null && !rerankerId.isBlank()) {
            config.put("reranker_id", rerankerId);
            if (overviewRelevanceThreshold != null) {
                config.put("relevance_threshold", overviewRelevanceThreshold);
            }
            config.put("chronological_resort", rerankChronologicalResort);
        }
        if (overviewMaxResults > 0) {
            config.put("max_results", overviewMaxResults);
        }
        if (overviewTemperature >= 0.0) {
            config.put("llm_temp", overviewTemperature);
        }
        if (overviewTokenBudget > 0) {
            config.put("gen_token_budget", overviewTokenBudget);
        }
        if (overviewSysPrompt != null && !overviewSysPrompt.isBlank()) {
            config.put("sys_prompt", overviewSysPrompt);
        }
        if (overviewPrompt != null && !overviewPrompt.isBlank()) {
            config.put("prompt", overviewPrompt);
        }

        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/v1/memories:retrieve"))
            .timeout(Duration.ofSeconds(60))
            .header("X-API-Key", apiKey)
            .header("Content-Type", "application/json")
            .header("Accept", "application/x-ndjson")
            .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
            .build();

        HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() != 200) {
            String errorBody = readErrorBody(response.body());
            throw new IOException("GoodMem overview failed: " + response.statusCode() + " " + errorBody);
        }

        String overview = null;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                JsonNode root = mapper.readTree(line);
                JsonNode abstractReply = root.get("abstractReply");
                if (abstractReply != null && !abstractReply.isNull()) {
                    String text = abstractReply.path("text").asText(null);
                    if (text != null && !text.isBlank()) {
                        overview = text.trim();
                    }
                }
                if (overview == null) {
                    JsonNode summaryNode = root.get("summary");
                    if (summaryNode != null && summaryNode.isTextual()) {
                        overview = summaryNode.asText().trim();
                        continue;
                    }
                    JsonNode resultsNode = root.get("results");
                    if (resultsNode != null && resultsNode.isArray()) {
                        for (JsonNode item : resultsNode) {
                            if (item == null || item.isNull()) {
                                continue;
                            }
                            JsonNode itemSummary = item.get("summary");
                            if (itemSummary != null && itemSummary.isTextual()) {
                                String text = itemSummary.asText();
                                if (text != null && !text.isBlank()) {
                                    overview = text.trim();
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }

        return overview;
    }

    private boolean shouldApplyReranker(String filter) {
        return rerankerId != null
            && !rerankerId.isBlank()
            && (filter == null || filter.isBlank());
    }

    private int resolveRerankCandidateSize() {
        if (rerankCandidateSize > 0) {
            return rerankCandidateSize;
        }
        return 100;
    }

    private int resolveOverviewCandidateSize() {
        int candidateSize = overviewCandidateSize > 0 ? overviewCandidateSize : 24;
        if (overviewMaxResults > candidateSize) {
            candidateSize = overviewMaxResults;
        }
        return candidateSize;
    }

    private static double normalizeScore(double relevanceScore) {
        if (Double.isNaN(relevanceScore)) {
            return 0.0;
        }
        if (relevanceScore < 0) {
            return -relevanceScore;
        }
        return relevanceScore;
    }

    private static String readErrorBody(InputStream body) {
        if (body == null) {
            return "";
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(body, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
                if (sb.length() > 1024) {
                    break;
                }
            }
            return sb.toString();
        } catch (IOException ex) {
            logger.warn("Failed reading error body", ex);
            return "";
        }
    }

    private static HttpClient buildClient(boolean insecureSsl) {
        HttpClient.Builder builder = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10));
        if (!insecureSsl) {
            return builder.build();
        }

        try {
            TrustManager[] trustAll = new TrustManager[] { new X509TrustManager() {
                @Override
                public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                }

                @Override
                public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                }

                @Override
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return new java.security.cert.X509Certificate[0];
                }
            } };

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAll, new SecureRandom());
            SSLParameters sslParameters = new SSLParameters();
            sslParameters.setEndpointIdentificationAlgorithm("");

            return builder
                .sslContext(sslContext)
                .sslParameters(sslParameters)
                .build();
        } catch (NoSuchAlgorithmException | KeyManagementException ex) {
            logger.warn("Failed to initialize insecure SSL context, falling back to default", ex);
            return builder.build();
        }
    }

    public static record MemoryHit(
        SpaceType spaceType,
        String memoryId,
        JsonNode metadata,
        String text,
        double score
    ) {}
}
