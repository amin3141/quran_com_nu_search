package com.quran.omni.search;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.quran.omni.AppConfig;
import com.quran.omni.SpaceType;
import com.quran.omni.goodmem.GoodMemClient;
import com.quran.omni.goodmem.GoodMemClient.MemoryHit;
import com.quran.omni.goodmem.SpaceRegistry;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SearchService {
    private static final Logger logger = LoggerFactory.getLogger(SearchService.class);
    private static final Pattern AYAH_REF = Pattern.compile("\\b(\\d{1,3})\\s*[:/.-]\\s*(\\d{1,3})\\b");
    private static final Pattern SURAH_AYAH_REF = Pattern.compile(
        "(?i)\\bsurah\\s+(\\d{1,3})\\D+ayah\\s+(\\d{1,3})\\b"
    );
    private static final List<TafsirSourceDefinition> TAFSIR_SOURCE_DEFINITIONS = List.of(
        TafsirSourceDefinition.of(
            "ibn-kathir",
            "Ibn Kathir",
            "(?iu)\\bibn\\s*[- ]?katheer\\b",
            "(?iu)\\bibn\\s*[- ]?kathir\\b",
            "(?u)ابن\\s*كثير"
        ),
        TafsirSourceDefinition.of(
            "tabari",
            "al-Tabari",
            "(?iu)\\bal\\s*[- ]?tabari\\b",
            "(?iu)\\btabari\\b",
            "(?u)الطبري"
        ),
        TafsirSourceDefinition.of(
            "qurtubi",
            "al-Qurtubi",
            "(?iu)\\bal\\s*[- ]?qurtubi\\b",
            "(?iu)\\bqurtubi\\b",
            "(?u)القرطبي"
        ),
        TafsirSourceDefinition.of(
            "jalalayn",
            "al-Jalalayn",
            "(?iu)\\bjalalayn\\b",
            "(?iu)\\bal\\s*[- ]?jalalayn\\b",
            "(?u)الجلالين"
        ),
        TafsirSourceDefinition.of(
            "saadi",
            "al-Saadi",
            "(?iu)\\bsa[' ]?di\\b",
            "(?iu)\\bal\\s*[- ]?sa[' ]?di\\b",
            "(?u)السعدي"
        ),
        TafsirSourceDefinition.of(
            "baghawi",
            "al-Baghawi",
            "(?iu)\\bbaghawi\\b",
            "(?iu)\\bal\\s*[- ]?baghawi\\b",
            "(?u)البغوي"
        ),
        TafsirSourceDefinition.of(
            "al-wasit",
            "al-Wasit",
            "(?iu)\\bal\\s*[- ]?wasit\\b",
            "(?iu)\\bwasit\\b",
            "(?u)الوسيط"
        ),
        TafsirSourceDefinition.of(
            "maarif-ul-quran",
            "Ma'arif al-Qur'an",
            "(?iu)\\bma['’]?arif\\s+(?:al|ul)\\s+qur['’]?an\\b",
            "(?iu)\\bmaarif\\b"
        ),
        TafsirSourceDefinition.of(
            "tazkirul-quran",
            "Tazkirul Quran",
            "(?iu)\\btazkir\\s*ul\\s*qur['’]?an\\b",
            "(?iu)\\btazkirul\\s*qur['’]?an\\b"
        ),
        TafsirSourceDefinition.of(
            "muyassar",
            "al-Muyassar",
            "(?iu)\\bmuyassar\\b",
            "(?u)الميسر"
        ),
        TafsirSourceDefinition.of(
            "tahrir-wa-tanwir",
            "al-Tahrir wa al-Tanwir",
            "(?iu)\\btahrir\\s+wa\\s+(?:al\\s+)?tanwir\\b",
            "(?u)التحرير\\s+والتنوير"
        ),
        TafsirSourceDefinition.of(
            "nathm-aldurar",
            "Nathm al-Durar",
            "(?iu)\\bnathm\\s+al\\s*durar\\b",
            "(?u)نظم\\s+الدرر"
        )
    );

    private final ObjectMapper mapper = new ObjectMapper();
    private final GoodMemClient client;
    private final SpaceRegistry spaceRegistry;
    private final AppConfig config;
    private final ExecutorService executor;
    private final OpenAiChatClient openAiClient;
    private final SearchResultAssembler assembler;

    public SearchService(GoodMemClient client, SpaceRegistry spaceRegistry, AppConfig config) {
        this.client = client;
        this.spaceRegistry = spaceRegistry;
        this.config = config;
        this.executor = Executors.newFixedThreadPool(6);
        this.openAiClient = new OpenAiChatClient(config);
        this.assembler = new SearchResultAssembler(client, config);
    }

    public Models.SearchResponse search(Models.SearchRequest request) {
        return search(request, SearchEventListener.noop());
    }

    public Models.SearchResponse search(Models.SearchRequest request, SearchEventListener listener) {
        String query = request.query() == null ? "" : request.query().trim();
        if (query.isBlank()) {
            throw new IllegalArgumentException("query is required");
        }

        String language = request.language();
        if (language == null || language.isBlank()) {
            language = config.defaultLanguage();
        }
        EnumSet<SpaceType> requestedSpaces = parseSpaces(request.spaces());
        requestedSpaces.add(SpaceType.QURAN);
        Map<SpaceType, String> spaceIds = spaceRegistry.resolve();
        int maxSteps = resolveMaxSteps(request.maxSteps());
        int requestedLimit = request.limit() != null && request.limit() > 0 ? request.limit() : 8;
        QueryIntent queryIntent = inferIntent(query);
        String ayahReference = extractAyahReference(query);
        TafsirSourceConstraint tafsirSource = detectTafsirSource(query);

        listener.onStatus("Agent started");

        Map<String, MemoryHit> bestHits = new LinkedHashMap<>();
        List<Models.AgentToolCall> toolCalls = new ArrayList<>();
        EnumSet<SpaceType> searchedSpaces = EnumSet.noneOf(SpaceType.class);
        boolean usedLlmPlanner = false;
        boolean usedHeuristicFallback = false;
        int noNewResultsStreak = 0;
        String plannerSummary = null;
        int nextStep = 1;

        if (ayahReference != null && maxSteps > 0) {
            ExactAyahLookup exactLookup = prefetchExactAyah(
                ayahReference,
                requestedSpaces,
                spaceIds,
                requestedLimit,
                tafsirSource
            );
            if (!exactLookup.spaces().isEmpty()) {
                listener.onStatus("Step 1: exact ayah lookup for " + ayahReference);
                int newResultCount = mergeHits(bestHits, exactLookup.hits());
                if (newResultCount == 0) {
                    noNewResultsStreak = 1;
                }
                searchedSpaces.addAll(exactLookup.spaces());

                Models.AgentToolCall exactToolCall = new Models.AgentToolCall(
                    1,
                    "Use deterministic ayah-key lookup before semantic search.",
                    "goodmem_exact_ayah",
                    ayahReference,
                    exactLookup.spaces().stream().map(SpaceType::apiName).collect(Collectors.toList()),
                    exactLookup.limit(),
                    exactLookup.hits().size(),
                    newResultCount,
                    true,
                    exactAyahLookupReason(ayahReference, tafsirSource, exactLookup.spaces()),
                    previewHits(exactLookup.hits(), 5)
                );
                toolCalls.add(exactToolCall);
                listener.onToolCall(exactToolCall);
                nextStep = 2;
            }
        }

        for (int step = nextStep; step <= maxSteps; step++) {
            List<MemoryHit> aggregatedHits = sortedHits(bestHits.values());
            PlanningOutcome planning = planStep(
                query,
                requestedSpaces,
                searchedSpaces,
                toolCalls,
                aggregatedHits,
                step,
                maxSteps,
                requestedLimit,
                noNewResultsStreak
            );
            usedLlmPlanner = usedLlmPlanner || planning.usedLlmPlanner();
            usedHeuristicFallback = usedHeuristicFallback || planning.usedHeuristicFallback();

            PlannerDecision decision = planning.decision();
            String action = decision.action();
            ToolInput toolInput = decision.toolInput();
            boolean forced = false;
            String forcedReason = null;

            if ("finish".equals(action) && step == 1 && aggregatedHits.isEmpty()) {
                forced = true;
                forcedReason = "Need at least one retrieval step before finishing";
                action = "goodmem_search";
                toolInput = chooseToolInput(
                    query,
                    requestedSpaces,
                    searchedSpaces,
                    requestedLimit,
                    0,
                    queryIntent
                );
            }

            if ("finish".equals(action)) {
                plannerSummary = blankToNull(decision.summary());
                listener.onStatus("Agent finished evidence collection");
                break;
            }

            if (toolInput.spaces().isEmpty()) {
                forced = true;
                forcedReason = "Planner returned no spaces; using heuristic space selection";
                toolInput = chooseToolInput(
                    query,
                    requestedSpaces,
                    searchedSpaces,
                    requestedLimit,
                    noNewResultsStreak,
                    queryIntent
                );
            }

            ToolInputTightening tightening = tightenPlannerToolInput(
                toolInput,
                query,
                queryIntent,
                ayahReference,
                requestedSpaces,
                step
            );
            if (tightening.reason() != null) {
                forced = true;
                forcedReason = combineReasons(forcedReason, tightening.reason());
                toolInput = tightening.toolInput();
            }

            if (noNewResultsStreak > 0 && searchedSpaces.containsAll(toolInput.spaces())) {
                List<SpaceType> unexplored = requestedSpaces.stream()
                    .filter(space -> !searchedSpaces.contains(space))
                    .collect(Collectors.toList());
                if (!unexplored.isEmpty()) {
                    forced = true;
                    forcedReason = combineReasons(forcedReason, "Exploring spaces not yet searched");
                    toolInput = new ToolInput(unexplored, toolInput.query(), toolInput.limit());
                }
            }

            if (tafsirSource != null && toolInput.spaces().contains(SpaceType.TAFSIR)) {
                forced = true;
                forcedReason = combineReasons(
                    forcedReason,
                    "Applied tafsir source filter for " + tafsirSource.label()
                );
            }

            String toolSpaces = toolInput.spaces().stream()
                .map(SpaceType::apiName)
                .collect(Collectors.joining(", "));
            listener.onStatus("Step " + step + ": searching " + toolSpaces);
            List<MemoryHit> hits = executeTool(toolInput, spaceIds, tafsirSource);
            int newResultCount = mergeHits(bestHits, hits);
            if (newResultCount == 0) {
                noNewResultsStreak += 1;
            } else {
                noNewResultsStreak = 0;
            }
            searchedSpaces.addAll(toolInput.spaces());

            Models.AgentToolCall toolCall = new Models.AgentToolCall(
                step,
                blankToNull(decision.thought()),
                action,
                toolInput.query(),
                toolInput.spaces().stream().map(SpaceType::apiName).collect(Collectors.toList()),
                toolInput.limit(),
                hits.size(),
                newResultCount,
                forced,
                forcedReason,
                previewHits(hits, 5)
            );
            toolCalls.add(toolCall);
            listener.onToolCall(toolCall);

            if (step == maxSteps) {
                break;
            }
        }

        listener.onStatus("Assembling verse-centric results");
        List<MemoryHit> aggregatedHits = sortedHits(bestHits.values());
        Models.AiOverview aiOverview = buildOverview(
            query,
            requestedSpaces,
            spaceIds,
            aggregatedHits,
            plannerSummary
        );
        Models.AgentMetadata agentMetadata = new Models.AgentMetadata(
            openAiClient.isConfigured() ? "llm" : "heuristic",
            openAiClient.isConfigured() ? config.plannerModel() : null,
            toolCalls.size(),
            usedLlmPlanner,
            usedHeuristicFallback
        );
        return assembler.assemble(
            query,
            language,
            requestedSpaces,
            spaceIds,
            aggregatedHits,
            aiOverview,
            toolCalls,
            agentMetadata
        );
    }

    public void shutdown() {
        executor.shutdownNow();
    }

    private PlanningOutcome planStep(
        String query,
        EnumSet<SpaceType> requestedSpaces,
        EnumSet<SpaceType> searchedSpaces,
        List<Models.AgentToolCall> toolCalls,
        List<MemoryHit> aggregatedHits,
        int step,
        int maxSteps,
        int requestedLimit,
        int noNewResultsStreak
    ) {
        if (openAiClient.isConfigured()) {
            try {
                PlannerDecision llmDecision = decideWithLlm(
                    query,
                    requestedSpaces,
                    toolCalls,
                    aggregatedHits,
                    step,
                    maxSteps,
                    requestedLimit,
                    noNewResultsStreak
                );
                return new PlanningOutcome(llmDecision, true, false);
            } catch (Exception ex) {
                logger.warn("Planner model failed; falling back to heuristics", ex);
            }
        }
        PlannerDecision heuristicDecision = decideHeuristically(
            query,
            requestedSpaces,
            searchedSpaces,
            aggregatedHits,
            step,
            maxSteps,
            requestedLimit,
            noNewResultsStreak
        );
        return new PlanningOutcome(
            heuristicDecision,
            false,
            openAiClient.isConfigured()
        );
    }

    private PlannerDecision decideWithLlm(
        String query,
        EnumSet<SpaceType> requestedSpaces,
        List<Models.AgentToolCall> toolCalls,
        List<MemoryHit> aggregatedHits,
        int step,
        int maxSteps,
        int requestedLimit,
        int noNewResultsStreak
    ) throws IOException, InterruptedException {
        QueryIntent queryIntent = inferIntent(query);
        String ayahReference = extractAyahReference(query);
        ObjectNode payload = mapper.createObjectNode();
        payload.put("query", query);
        payload.put("queryIntent", queryIntent.name());
        if (ayahReference != null) {
            payload.put("ayahReference", ayahReference);
        }
        payload.put("step", step);
        payload.put("maxSteps", maxSteps);
        payload.put("requestedLimit", requestedLimit);
        payload.put("noNewResultsStreak", noNewResultsStreak);

        ArrayNode spaces = payload.putArray("requestedSpaces");
        requestedSpaces.stream().map(SpaceType::apiName).forEach(spaces::add);

        ArrayNode toolLog = payload.putArray("toolLog");
        for (Models.AgentToolCall toolCall : toolCalls.stream().skip(Math.max(0, toolCalls.size() - 4)).toList()) {
            toolLog.addPOJO(toolCall);
        }

        ArrayNode hits = payload.putArray("aggregatedHitsPreview");
        for (Models.ResultPreview preview : previewHits(aggregatedHits, 8)) {
            hits.addPOJO(preview);
        }

        JsonNode response = openAiClient.chatJson(
            """
            You are the search controller for a Quran.com omni-search agent.
            Available spaces:
            - quran: Arabic ayah text and verse references
            - translation: translated ayah text
            - tafsir: explanatory commentary
            - post: community reflections/posts
            - course: course lessons
            - article: editorial/article content

            Use one action per step.
            Basic routing rules:
            - If ayahReference is present, the first search must stay in quran/translation/tafsir.
            - If queryIntent is VERSE_REFERENCE, start with quran and translation; add tafsir if explanation is implied.
            - If queryIntent is EXPLANATION, the first search must include tafsir and should not start with post/course/article.
            - If queryIntent is COMMUNITY, the first search should include post.
            - If queryIntent is LEARNING, the first search should include course or article.
            - If queryIntent is TOPICAL, start with quran/translation/tafsir unless the wording clearly asks for reflections, courses, or articles.

            Basic constraints:
            - Do not finish on step 1.
            - For goodmem_search, choose 1 to 3 spaces only.
            - For goodmem_search, tool_input.query must be non-empty and should usually be a simplified rewrite of the user query.
            - If noNewResultsStreak is greater than 0, prefer unsearched spaces or a shorter query rewrite before finishing.
            - Finish only when evidence is already good enough or the useful spaces are exhausted.

            Return STRICT JSON:
            {"thought":string,"action":"goodmem_search"|"finish","tool_input":{"spaces":[string],"query":string,"limit":number},"summary":string}
            For finish, summary should be a concise high-level synthesis and tool_input may be ignored.
            """,
            payload,
            config.plannerModel(),
            config.plannerFallbackModels(),
            0.1
        );

        String thought = text(response, "thought");
        String action = text(response, "action");
        String summary = text(response, "summary");

        JsonNode toolInput = response.path("tool_input");
        List<SpaceType> spacesForTool = parseSpaceList(toolInput.path("spaces"), requestedSpaces);
        String toolQuery = text(toolInput, "query");
        int limit = toolInput.path("limit").asInt(requestedLimit);

        if (toolQuery == null || toolQuery.isBlank()) {
            toolQuery = rewriteQueryForSearch(query, step, 0);
        }
        if (action == null || action.isBlank()) {
            action = "goodmem_search";
        }

        return new PlannerDecision(
            thought,
            action.trim().toLowerCase(Locale.ROOT),
            new ToolInput(spacesForTool, toolQuery, clampLimit(limit)),
            summary
        );
    }

    private PlannerDecision decideHeuristically(
        String query,
        EnumSet<SpaceType> requestedSpaces,
        EnumSet<SpaceType> searchedSpaces,
        List<MemoryHit> aggregatedHits,
        int step,
        int maxSteps,
        int requestedLimit,
        int noNewResultsStreak
    ) {
        QueryIntent intent = inferIntent(query);
        if (step >= maxSteps) {
            return new PlannerDecision(
                "Reached max agent steps.",
                "finish",
                ToolInput.empty(query, requestedLimit),
                null
            );
        }

        boolean hasTextualEvidence = aggregatedHits.stream().anyMatch(hit ->
            hit.spaceType() == SpaceType.QURAN
                || hit.spaceType() == SpaceType.TRANSLATION
                || hit.spaceType() == SpaceType.TAFSIR
        );
        boolean hasDirectEvidence = aggregatedHits.stream().anyMatch(hit ->
            hit.spaceType() == SpaceType.POST
                || hit.spaceType() == SpaceType.COURSE
                || hit.spaceType() == SpaceType.ARTICLE
        );

        if (step > 1) {
            if ((intent == QueryIntent.VERSE_REFERENCE || intent == QueryIntent.EXPLANATION)
                && hasTextualEvidence) {
                return new PlannerDecision(
                    "Verse-oriented evidence is already strong.",
                    "finish",
                    ToolInput.empty(query, requestedLimit),
                    null
                );
            }
            if ((intent == QueryIntent.COMMUNITY || intent == QueryIntent.LEARNING)
                && hasDirectEvidence && noNewResultsStreak > 0) {
                return new PlannerDecision(
                    "Direct-content evidence is sufficient and the last step added nothing new.",
                    "finish",
                    ToolInput.empty(query, requestedLimit),
                    null
                );
            }
            if (searchedSpaces.containsAll(requestedSpaces) || noNewResultsStreak > 1) {
                return new PlannerDecision(
                    "Useful spaces are exhausted.",
                    "finish",
                    ToolInput.empty(query, requestedLimit),
                    null
                );
            }
        }

        ToolInput nextToolInput = chooseToolInput(
            query,
            requestedSpaces,
            searchedSpaces,
            requestedLimit,
            noNewResultsStreak,
            intent
        );
        return new PlannerDecision(
            "Use heuristic space selection for the next retrieval step.",
            "goodmem_search",
            nextToolInput,
            null
        );
    }

    private ToolInput chooseToolInput(
        String query,
        EnumSet<SpaceType> requestedSpaces,
        EnumSet<SpaceType> searchedSpaces,
        int requestedLimit,
        int noNewResultsStreak,
        QueryIntent intent
    ) {
        List<SpaceType> priority = prioritizeSpaces(intent, requestedSpaces);
        List<SpaceType> unexplored = priority.stream()
            .filter(space -> !searchedSpaces.contains(space))
            .collect(Collectors.toList());
        List<SpaceType> chosen;
        if (!unexplored.isEmpty()) {
            chosen = unexplored.stream().limit(intent == QueryIntent.TOPICAL ? 3 : 2).collect(Collectors.toList());
        } else {
            chosen = priority.stream().limit(Math.min(3, priority.size())).collect(Collectors.toList());
        }
        String toolQuery = rewriteQueryForSearch(query, searchedSpaces.size() + 1, noNewResultsStreak);
        int limit = clampLimit(intent == QueryIntent.TOPICAL ? requestedLimit + 2 : requestedLimit);
        return new ToolInput(chosen, toolQuery, limit);
    }

    private ToolInputTightening tightenPlannerToolInput(
        ToolInput toolInput,
        String query,
        QueryIntent intent,
        String ayahReference,
        EnumSet<SpaceType> requestedSpaces,
        int step
    ) {
        List<SpaceType> spaces = new ArrayList<>(toolInput.spaces());
        String toolQuery = toolInput.query();
        int limit = clampLimit(toolInput.limit());
        String reason = null;

        if (toolQuery == null || toolQuery.isBlank()) {
            toolQuery = rewriteQueryForSearch(query, step, 0);
            reason = combineReasons(reason, "Planner returned an empty query rewrite");
        }

        if (step == 1) {
            List<SpaceType> preferredSpaces = preferredFirstStepSpaces(intent, requestedSpaces);
            boolean shouldForcePreferredSpaces = switch (intent) {
                case VERSE_REFERENCE -> !spaces.contains(SpaceType.QURAN);
                case EXPLANATION -> requestedSpaces.contains(SpaceType.TAFSIR) && !spaces.contains(SpaceType.TAFSIR);
                case COMMUNITY -> requestedSpaces.contains(SpaceType.POST) && !spaces.contains(SpaceType.POST);
                case LEARNING -> requestedSpaces.contains(SpaceType.COURSE) && requestedSpaces.contains(SpaceType.ARTICLE)
                    ? !(spaces.contains(SpaceType.COURSE) || spaces.contains(SpaceType.ARTICLE))
                    : requestedSpaces.contains(SpaceType.COURSE)
                        ? !spaces.contains(SpaceType.COURSE)
                        : requestedSpaces.contains(SpaceType.ARTICLE) && !spaces.contains(SpaceType.ARTICLE);
                case TOPICAL -> spaces.isEmpty();
            };
            if (shouldForcePreferredSpaces && !preferredSpaces.isEmpty()) {
                spaces = preferredSpaces;
                reason = combineReasons(
                    reason,
                    "Tightened first-step routing for " + intent.name().toLowerCase(Locale.ROOT)
                );
            }
            if ((intent == QueryIntent.VERSE_REFERENCE || intent == QueryIntent.EXPLANATION)
                && ayahReference != null
                && !ayahReference.equals(toolQuery)) {
                toolQuery = ayahReference;
                reason = combineReasons(reason, "Normalized first-step query to explicit ayah reference");
            }
        }

        if (spaces.size() > 3) {
            spaces = new ArrayList<>(spaces.subList(0, 3));
            reason = combineReasons(reason, "Trimmed planner output to at most three spaces");
        }

        if (reason == null) {
            return new ToolInputTightening(toolInput, null);
        }
        return new ToolInputTightening(new ToolInput(spaces, toolQuery, limit), reason);
    }

    private List<SpaceType> preferredFirstStepSpaces(
        QueryIntent intent,
        EnumSet<SpaceType> requestedSpaces
    ) {
        List<SpaceType> preferred = switch (intent) {
            case VERSE_REFERENCE -> List.of(SpaceType.QURAN, SpaceType.TRANSLATION, SpaceType.TAFSIR);
            case EXPLANATION -> List.of(SpaceType.TAFSIR, SpaceType.QURAN, SpaceType.TRANSLATION);
            case COMMUNITY -> List.of(SpaceType.POST, SpaceType.QURAN, SpaceType.TRANSLATION);
            case LEARNING -> List.of(SpaceType.COURSE, SpaceType.ARTICLE, SpaceType.QURAN);
            case TOPICAL -> List.of(SpaceType.QURAN, SpaceType.TRANSLATION, SpaceType.TAFSIR);
        };
        return preferred.stream()
            .filter(requestedSpaces::contains)
            .limit(3)
            .collect(Collectors.toList());
    }

    private List<MemoryHit> executeTool(
        ToolInput toolInput,
        Map<SpaceType, String> spaceIds,
        TafsirSourceConstraint tafsirSource
    ) {
        List<CompletableFuture<List<MemoryHit>>> futures = new ArrayList<>();
        for (SpaceType spaceType : toolInput.spaces()) {
            String spaceId = spaceIds.get(spaceType);
            if (spaceId == null || spaceId.isBlank()) {
                logger.warn("Missing space ID for {}", spaceType);
                continue;
            }
            String filter = buildRetrievalFilter(spaceType, null, tafsirSource);
            futures.add(CompletableFuture.supplyAsync(() -> {
                try {
                    return client.retrieve(toolInput.query(), spaceType, spaceId, toolInput.limit(), filter);
                } catch (Exception ex) {
                    logger.warn("Search failed for {}", spaceType, ex);
                    return List.of();
                }
            }, executor));
        }
        return futures.stream()
            .map(CompletableFuture::join)
            .flatMap(List::stream)
            .sorted(Comparator.comparingDouble(MemoryHit::score).reversed())
            .collect(Collectors.toList());
    }

    private ExactAyahLookup prefetchExactAyah(
        String ayahReference,
        EnumSet<SpaceType> requestedSpaces,
        Map<SpaceType, String> spaceIds,
        int requestedLimit,
        TafsirSourceConstraint tafsirSource
    ) {
        List<SpaceType> exactSpaces = List.of(SpaceType.QURAN, SpaceType.TRANSLATION, SpaceType.TAFSIR).stream()
            .filter(requestedSpaces::contains)
            .collect(Collectors.toList());
        if (exactSpaces.isEmpty()) {
            return ExactAyahLookup.empty(requestedLimit);
        }

        List<SpaceType> attemptedSpaces = new ArrayList<>();
        List<CompletableFuture<List<MemoryHit>>> futures = new ArrayList<>();
        for (SpaceType spaceType : exactSpaces) {
            String spaceId = spaceIds.get(spaceType);
            if (spaceId == null || spaceId.isBlank()) {
                logger.warn("Missing space ID for {}", spaceType);
                continue;
            }

            String filter = buildRetrievalFilter(
                spaceType,
                buildExactAyahFilter(spaceType, ayahReference),
                tafsirSource
            );
            int limit = exactAyahLimit(spaceType, requestedLimit);
            attemptedSpaces.add(spaceType);
            futures.add(CompletableFuture.supplyAsync(() -> {
                try {
                    return client.retrieve(ayahReference, spaceType, spaceId, limit, filter);
                } catch (Exception ex) {
                    logger.warn("Exact ayah lookup failed for {}", spaceType, ex);
                    return List.of();
                }
            }, executor));
        }

        if (attemptedSpaces.isEmpty()) {
            return ExactAyahLookup.empty(requestedLimit);
        }

        List<MemoryHit> hits = futures.stream()
            .map(CompletableFuture::join)
            .flatMap(List::stream)
            .sorted(Comparator.comparingDouble(MemoryHit::score).reversed())
            .collect(Collectors.toList());
        int limit = attemptedSpaces.stream()
            .mapToInt(spaceType -> exactAyahLimit(spaceType, requestedLimit))
            .max()
            .orElse(clampLimit(requestedLimit));
        return new ExactAyahLookup(attemptedSpaces, hits, limit);
    }

    private int exactAyahLimit(SpaceType spaceType, int requestedLimit) {
        if (spaceType == SpaceType.QURAN) {
            return 1;
        }
        int configuredLimit = config.spaceLimits().getOrDefault(spaceType, clampLimit(requestedLimit));
        return Math.max(1, Math.min(clampLimit(requestedLimit), configuredLimit));
    }

    private String buildRetrievalFilter(
        SpaceType spaceType,
        String baseFilter,
        TafsirSourceConstraint tafsirSource
    ) {
        String sourceFilter = buildTafsirSourceFilter(spaceType, tafsirSource);
        return combineFilters(baseFilter, sourceFilter);
    }

    private String buildExactAyahFilter(SpaceType spaceType, String ayahReference) {
        String escaped = escapeFilterValue(ayahReference);
        return switch (spaceType) {
            case QURAN, TRANSLATION ->
                "CAST(val('$.ayah_key') AS TEXT) = '" + escaped + "'";
            case TAFSIR ->
                "CAST(val('$.ayah_key') AS TEXT) = '" + escaped + "'"
                    + " OR CAST(val('$.passage_ayah_key') AS TEXT) = '" + escaped + "'"
                    + " OR CAST(val('$.passage_ayah_range') AS TEXT) = '" + escaped + "'";
            default -> "";
        };
    }

    private String buildTafsirSourceFilter(SpaceType spaceType, TafsirSourceConstraint tafsirSource) {
        if (spaceType != SpaceType.TAFSIR || tafsirSource == null) {
            return null;
        }
        return "CAST(val('$.code') AS TEXT) = '" + escapeFilterValue(tafsirSource.code()) + "'";
    }

    private String combineFilters(String left, String right) {
        String normalizedLeft = blankToNull(left);
        String normalizedRight = blankToNull(right);
        if (normalizedLeft == null) {
            return normalizedRight;
        }
        if (normalizedRight == null) {
            return normalizedLeft;
        }
        return "(" + normalizedLeft + ") AND (" + normalizedRight + ")";
    }

    private String escapeFilterValue(String value) {
        return value.replace("'", "''");
    }

    private Models.AiOverview buildOverview(
        String query,
        EnumSet<SpaceType> requestedSpaces,
        Map<SpaceType, String> spaceIds,
        List<MemoryHit> hits,
        String plannerSummary
    ) {
        if (hits.isEmpty()) {
            return null;
        }

        String summary = null;
        if (openAiClient.isConfigured()) {
            try {
                summary = summarizeWithLlm(query, hits);
            } catch (Exception ex) {
                logger.warn("OpenAI summary generation failed", ex);
            }
        }
        if (summary == null || summary.isBlank()) {
            summary = plannerSummary;
        }
        if ((summary == null || summary.isBlank()) && client.isOverviewEnabled()) {
            try {
                List<String> overviewSpaceIds = requestedSpaces.stream()
                    .filter(space -> space == SpaceType.QURAN || space == SpaceType.TRANSLATION || space == SpaceType.TAFSIR)
                    .map(spaceIds::get)
                    .filter(id -> id != null && !id.isBlank())
                    .distinct()
                    .collect(Collectors.toList());
                summary = client.generateOverview(query, overviewSpaceIds);
            } catch (Exception ex) {
                logger.warn("GoodMem overview generation failed", ex);
            }
        }
        if (summary == null || summary.isBlank()) {
            summary = heuristicOverview(hits);
        }
        if (summary == null || summary.isBlank()) {
            return null;
        }
        return new Models.AiOverview(summary.trim());
    }

    private String summarizeWithLlm(String query, List<MemoryHit> hits) throws IOException, InterruptedException {
        ObjectNode payload = mapper.createObjectNode();
        payload.put("query", query);
        ArrayNode evidence = payload.putArray("evidence");
        for (MemoryHit hit : hits.stream().limit(10).toList()) {
            ObjectNode item = evidence.addObject();
            item.put("space", hit.spaceType().apiName());
            item.put("title", previewTitle(hit));
            item.put("ayahKey", previewAyahKey(hit));
            item.put("url", previewUrl(hit));
            item.put("text", TextCleaner.cleanSnippet(hit.text(), 500));
            item.put("score", hit.score());
        }

        JsonNode response = openAiClient.chatJson(
            """
            You are summarizing retrieved Quran.com search evidence.
            Use only the evidence supplied.
            Prefer verse references like 2:255 when applicable.
            Mention when evidence comes from tafsir, reflections, courses, or articles.
            Keep the response concise and factual.
            Return STRICT JSON: {"summary": string}
            """,
            payload,
            config.summaryModel(),
            config.summaryFallbackModels(),
            0.2
        );
        return text(response, "summary");
    }

    private int mergeHits(Map<String, MemoryHit> bestHits, List<MemoryHit> hits) {
        int newResults = 0;
        for (MemoryHit hit : hits) {
            String key = hit.spaceType().apiName() + ":" + hit.memoryId();
            MemoryHit existing = bestHits.get(key);
            if (existing == null) {
                bestHits.put(key, hit);
                newResults += 1;
                continue;
            }
            if (hit.score() > existing.score()) {
                bestHits.put(key, hit);
            }
        }
        return newResults;
    }

    private static List<MemoryHit> sortedHits(Iterable<MemoryHit> hits) {
        List<MemoryHit> sorted = new ArrayList<>();
        hits.forEach(sorted::add);
        sorted.sort(Comparator.comparingDouble(MemoryHit::score).reversed());
        return sorted;
    }

    private List<Models.ResultPreview> previewHits(List<MemoryHit> hits, int limit) {
        return hits.stream()
            .limit(limit)
            .map(hit -> new Models.ResultPreview(
                hit.spaceType().apiName(),
                previewTitle(hit),
                previewAyahKey(hit),
                previewUrl(hit),
                hit.score()
            ))
            .collect(Collectors.toList());
    }

    private QueryIntent inferIntent(String query) {
        String normalized = query.toLowerCase(Locale.ROOT);
        if (extractAyahReference(query) != null
            || normalized.contains("ayah")
            || normalized.contains("verse")
            || normalized.contains("surah")
            || normalized.contains("sura")
            || normalized.contains("آية")
            || normalized.contains("سورة")) {
            return QueryIntent.VERSE_REFERENCE;
        }
        if (detectTafsirSource(query) != null) {
            return QueryIntent.EXPLANATION;
        }
        if (normalized.contains("tafsir")
            || normalized.contains("interpret")
            || normalized.contains("explain")
            || normalized.contains("meaning")) {
            return QueryIntent.EXPLANATION;
        }
        if (normalized.contains("reflection")
            || normalized.contains("post")
            || normalized.contains("community")) {
            return QueryIntent.COMMUNITY;
        }
        if (normalized.contains("course")
            || normalized.contains("lesson")
            || normalized.contains("study")
            || normalized.contains("learn")) {
            return QueryIntent.LEARNING;
        }
        return QueryIntent.TOPICAL;
    }

    private List<SpaceType> prioritizeSpaces(QueryIntent intent, Set<SpaceType> requestedSpaces) {
        List<SpaceType> priority = switch (intent) {
            case VERSE_REFERENCE, EXPLANATION -> List.of(
                SpaceType.QURAN,
                SpaceType.TRANSLATION,
                SpaceType.TAFSIR,
                SpaceType.POST,
                SpaceType.ARTICLE,
                SpaceType.COURSE
            );
            case COMMUNITY -> List.of(
                SpaceType.POST,
                SpaceType.QURAN,
                SpaceType.TRANSLATION,
                SpaceType.ARTICLE,
                SpaceType.COURSE,
                SpaceType.TAFSIR
            );
            case LEARNING -> List.of(
                SpaceType.COURSE,
                SpaceType.ARTICLE,
                SpaceType.QURAN,
                SpaceType.TRANSLATION,
                SpaceType.TAFSIR,
                SpaceType.POST
            );
            case TOPICAL -> List.of(
                SpaceType.QURAN,
                SpaceType.TRANSLATION,
                SpaceType.TAFSIR,
                SpaceType.ARTICLE,
                SpaceType.COURSE,
                SpaceType.POST
            );
        };
        return priority.stream().filter(requestedSpaces::contains).collect(Collectors.toList());
    }

    private String rewriteQueryForSearch(String query, int step, int noNewResultsStreak) {
        String ayahRef = extractAyahReference(query);
        if (ayahRef != null) {
            return ayahRef;
        }
        String normalized = query.replaceAll("\\s+", " ").trim();
        TafsirSourceConstraint tafsirSource = detectTafsirSource(query);
        if (tafsirSource != null) {
            normalized = stripTafsirSourceMentions(normalized, tafsirSource)
                .replaceAll("(?i)\\bwhat does\\b", " ")
                .replaceAll("(?i)\\bwhat is\\b", " ")
                .replaceAll("(?i)\\b(?:say|says)\\s+about\\b", " ")
                .replaceAll("(?i)\\b(?:tafsir|commentary)\\b", " ")
                .replaceAll("\\s+", " ")
                .trim();
        }
        if (step > 1 || noNewResultsStreak > 0) {
            normalized = normalized
                .replaceAll("(?i)\\bwhat does\\b", "")
                .replaceAll("(?i)\\bshow me\\b", "")
                .replaceAll("(?i)\\btell me about\\b", "")
                .replaceAll("(?i)\\binterpretation of\\b", "")
                .replaceAll("(?i)\\breflections? about\\b", "")
                .replaceAll("\\s+", " ")
                .trim();
        }
        return normalized.isBlank() ? query : normalized;
    }

    private TafsirSourceConstraint detectTafsirSource(String query) {
        if (query == null || query.isBlank()) {
            return null;
        }
        for (TafsirSourceDefinition definition : TAFSIR_SOURCE_DEFINITIONS) {
            for (Pattern pattern : definition.queryPatterns()) {
                if (pattern.matcher(query).find()) {
                    return new TafsirSourceConstraint(definition.code(), definition.label(), definition.queryPatterns());
                }
            }
        }
        return null;
    }

    private String stripTafsirSourceMentions(String query, TafsirSourceConstraint tafsirSource) {
        String stripped = query;
        for (Pattern pattern : tafsirSource.queryPatterns()) {
            stripped = pattern.matcher(stripped).replaceAll(" ");
        }
        return stripped.replaceAll("\\s+", " ").trim();
    }

    private String extractAyahReference(String query) {
        Matcher direct = AYAH_REF.matcher(query);
        if (direct.find()) {
            return direct.group(1) + ":" + direct.group(2);
        }
        Matcher verbose = SURAH_AYAH_REF.matcher(query);
        if (verbose.find()) {
            return verbose.group(1) + ":" + verbose.group(2);
        }
        String canonical = canonicalAyahAlias(query);
        if (canonical != null) {
            return canonical;
        }
        return null;
    }

    private String canonicalAyahAlias(String query) {
        String normalized = query.toLowerCase(Locale.ROOT)
            .replace('-', ' ')
            .replace('_', ' ')
            .replaceAll("\\s+", " ")
            .trim();
        if (normalized.contains("ayat al kursi")
            || normalized.contains("ayah al kursi")
            || normalized.contains("ayat ul kursi")
            || normalized.contains("ayatul kursi")
            || normalized.contains("verse of the throne")) {
            return "2:255";
        }
        return null;
    }

    private String exactAyahLookupReason(String ayahReference, TafsirSourceConstraint tafsirSource, List<SpaceType> spaces) {
        String reason = "Exact ayah lookup for " + ayahReference;
        if (tafsirSource != null && spaces.contains(SpaceType.TAFSIR)) {
            reason += "; applied tafsir source filter for " + tafsirSource.label();
        }
        return reason;
    }

    private String heuristicOverview(List<MemoryHit> hits) {
        Set<String> refs = new LinkedHashSet<>();
        Set<String> sources = new LinkedHashSet<>();
        for (MemoryHit hit : hits) {
            if (refs.size() < 3) {
                String ayahKey = previewAyahKey(hit);
                if (ayahKey != null && !ayahKey.isBlank()) {
                    refs.add(ayahKey);
                }
            }
            if (sources.size() < 3) {
                sources.add(hit.spaceType().apiName());
            }
        }
        if (!refs.isEmpty()) {
            return "Top matches center on " + String.join(", ", refs)
                + " with evidence from " + String.join(", ", sources) + ".";
        }
        return "Relevant matches were found across " + String.join(", ", sources) + ".";
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

    private List<SpaceType> parseSpaceList(JsonNode node, Set<SpaceType> requestedSpaces) {
        if (node == null || !node.isArray()) {
            return List.of();
        }
        List<SpaceType> spaces = new ArrayList<>();
        for (JsonNode item : node) {
            SpaceType.fromString(item.asText()).ifPresent(space -> {
                if (requestedSpaces.contains(space) && !spaces.contains(space)) {
                    spaces.add(space);
                }
            });
        }
        return spaces;
    }

    private int resolveMaxSteps(Integer requested) {
        if (requested == null) {
            return 4;
        }
        return Math.max(1, Math.min(requested, 6));
    }

    private int clampLimit(int requestedLimit) {
        return Math.max(2, Math.min(requestedLimit, 16));
    }

    private String previewTitle(MemoryHit hit) {
        JsonNode meta = hit.metadata();
        return switch (hit.spaceType()) {
            case QURAN, TRANSLATION, TAFSIR -> {
                String ayahKey = previewAyahKey(hit);
                yield ayahKey == null || ayahKey.isBlank() ? hit.spaceType().apiName() : ayahKey;
            }
            case POST -> {
                String displayName = text(meta, "display_name");
                String username = text(meta, "username");
                if (displayName != null && !displayName.isBlank()) {
                    yield displayName;
                }
                yield username == null || username.isBlank() ? "Post" : "@" + username;
            }
            case COURSE -> {
                String courseTitle = text(meta, "course_title");
                String lessonTitle = text(meta, "lesson_title");
                yield joinTitle(courseTitle, lessonTitle);
            }
            case ARTICLE -> {
                String title = text(meta, "title");
                yield title == null || title.isBlank() ? "Article" : title;
            }
        };
    }

    private String previewAyahKey(MemoryHit hit) {
        return text(hit.metadata(), "ayah_key");
    }

    private String previewUrl(MemoryHit hit) {
        String url = text(hit.metadata(), "url");
        return url == null ? "" : url;
    }

    private static String text(JsonNode node, String field) {
        if (node == null) {
            return null;
        }
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        String text = value.asText();
        return text == null || text.isBlank() ? null : text.trim();
    }

    private static String joinTitle(String primary, String secondary) {
        if (primary != null && !primary.isBlank() && secondary != null && !secondary.isBlank()) {
            return primary + " / " + secondary;
        }
        if (primary != null && !primary.isBlank()) {
            return primary;
        }
        if (secondary != null && !secondary.isBlank()) {
            return secondary;
        }
        return "Untitled";
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static String combineReasons(String existing, String next) {
        if (next == null || next.isBlank()) {
            return existing;
        }
        if (existing == null || existing.isBlank()) {
            return next;
        }
        return existing + "; " + next;
    }

    public interface SearchEventListener {
        void onStatus(String message);

        void onToolCall(Models.AgentToolCall toolCall);

        static SearchEventListener noop() {
            return new SearchEventListener() {
                @Override
                public void onStatus(String message) {
                }

                @Override
                public void onToolCall(Models.AgentToolCall toolCall) {
                }
            };
        }
    }

    private enum QueryIntent {
        VERSE_REFERENCE,
        EXPLANATION,
        COMMUNITY,
        LEARNING,
        TOPICAL
    }

    private record ToolInput(List<SpaceType> spaces, String query, int limit) {
        static ToolInput empty(String query, int limit) {
            return new ToolInput(List.of(), query, limit);
        }
    }

    private record PlannerDecision(
        String thought,
        String action,
        ToolInput toolInput,
        String summary
    ) {
    }

    private record PlanningOutcome(
        PlannerDecision decision,
        boolean usedLlmPlanner,
        boolean usedHeuristicFallback
    ) {
    }

    private record ToolInputTightening(
        ToolInput toolInput,
        String reason
    ) {
    }

    private record TafsirSourceConstraint(
        String code,
        String label,
        List<Pattern> queryPatterns
    ) {
    }

    private record ExactAyahLookup(
        List<SpaceType> spaces,
        List<MemoryHit> hits,
        int limit
    ) {
        static ExactAyahLookup empty(int requestedLimit) {
            return new ExactAyahLookup(List.of(), List.of(), Math.max(1, requestedLimit));
        }
    }

    private record TafsirSourceDefinition(
        String code,
        String label,
        List<Pattern> queryPatterns
    ) {
        static TafsirSourceDefinition of(String code, String label, String... queryPatterns) {
            return new TafsirSourceDefinition(
                code,
                label,
                List.of(queryPatterns).stream().map(Pattern::compile).collect(Collectors.toList())
            );
        }
    }
}
