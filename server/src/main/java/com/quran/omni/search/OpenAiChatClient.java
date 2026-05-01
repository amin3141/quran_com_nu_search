package com.quran.omni.search;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.quran.omni.AppConfig;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class OpenAiChatClient {
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(15))
        .build();
    private final String baseUrl;
    private final String apiKey;

    public OpenAiChatClient(AppConfig config) {
        this.baseUrl = trimTrailingSlash(config.openAiBaseUrl());
        this.apiKey = config.openAiApiKey() == null ? "" : config.openAiApiKey().trim();
    }

    public boolean isConfigured() {
        return !apiKey.isBlank();
    }

    public JsonNode chatJson(
        String systemPrompt,
        JsonNode userPayload,
        String primaryModel,
        List<String> fallbackModels,
        double temperature
    ) throws IOException, InterruptedException {
        if (!isConfigured()) {
            throw new IOException("OPENAI_API_KEY is not configured");
        }
        List<String> models = orderedModels(primaryModel, fallbackModels);
        IOException lastError = null;
        for (String model : models) {
            try {
                return sendJsonChat(systemPrompt, userPayload, model);
            } catch (IOException ex) {
                lastError = ex;
            }
        }
        if (lastError != null) {
            throw lastError;
        }
        throw new IOException("No OpenAI model configured");
    }

    private JsonNode sendJsonChat(
        String systemPrompt,
        JsonNode userPayload,
        String model
    ) throws IOException, InterruptedException {
        ObjectNode payload = mapper.createObjectNode();
        payload.put("model", model);

        ArrayNode messages = payload.putArray("messages");
        ObjectNode systemMessage = messages.addObject();
        systemMessage.put("role", "system");
        systemMessage.put("content", systemPrompt);

        ObjectNode userMessage = messages.addObject();
        userMessage.put("role", "user");
        userMessage.put("content", mapper.writeValueAsString(userPayload));

        ObjectNode responseFormat = payload.putObject("response_format");
        responseFormat.put("type", "json_object");

        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/chat/completions"))
            .timeout(Duration.ofSeconds(60))
            .header("Authorization", "Bearer " + apiKey)
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() / 100 != 2) {
            throw new IOException("OpenAI chat failed: HTTP " + response.statusCode() + " " + abbreviate(response.body()));
        }

        JsonNode root = mapper.readTree(response.body());
        JsonNode content = root.path("choices").path(0).path("message").path("content");
        if (!content.isTextual() || content.asText().isBlank()) {
            throw new IOException("OpenAI chat returned empty content");
        }
        return mapper.readTree(content.asText());
    }

    private static List<String> orderedModels(String primaryModel, List<String> fallbackModels) {
        Set<String> seen = new LinkedHashSet<>();
        if (primaryModel != null && !primaryModel.isBlank()) {
            seen.add(primaryModel.trim());
        }
        if (fallbackModels != null) {
            for (String model : fallbackModels) {
                if (model != null && !model.isBlank()) {
                    seen.add(model.trim());
                }
            }
        }
        return new ArrayList<>(seen);
    }

    private static String trimTrailingSlash(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private static String abbreviate(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String compact = value.replaceAll("\\s+", " ").trim();
        if (compact.length() <= 240) {
            return compact;
        }
        return compact.substring(0, 239) + "…";
    }
}
