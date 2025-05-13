package com.kt.pnwanalyzer.data;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kt.pnwanalyzer.model.AllianceData;
import com.kt.pnwanalyzer.model.GameConstants;
import com.kt.pnwanalyzer.model.NationData;
import com.kt.pnwanalyzer.model.ResourcePrices;
import com.kt.pnwanalyzer.util.ConfigManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class PnWDataFetcher {
    private static final Logger logger = LoggerFactory.getLogger(PnWDataFetcher.class);
    private static final String API_URL = "https://api.politicsandwar.com/graphql";
    private final String apiKey;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public PnWDataFetcher(String apiKey) {
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(30))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public PnWDataFetcher() {
        this(ConfigManager.getInstance().getApiKey());
    }

    public JsonNode fetchGraphql(String query, Map<String, Object> variables) {
        if (apiKey == null || apiKey.isEmpty()) {
            logger.error("API key is missing");
            throw new IllegalStateException("API key is missing");
        }

        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("query", query);
            payload.put("variables", variables != null ? variables : new HashMap<>());

            String fullUrl = API_URL + "?api_key=" + apiKey;
            String jsonPayload = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(fullUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() >= 300) {
                logger.error("HTTP Error: {}", response.statusCode());
                throw new RuntimeException("HTTP Error: " + response.statusCode());
            }

            JsonNode result = objectMapper.readTree(response.body());
            
            if (result.has("errors")) {
                logger.error("GraphQL API Error: {}", result.get("errors"));
            }
            
            return result;
        } catch (IOException | InterruptedException e) {
            logger.error("Request failed: {}", e.getMessage());
            throw new RuntimeException("API request failed", e);
        }
    }

    public Map<String, Object> getAllianceData(int allianceId) {
        logger.info("Fetching core data for alliance ID {}", allianceId);

        String allianceQuery = "query($id:[Int!]){alliances(id:$id,first:1){data{id name acronym color score}}}";
        
        Map<String, Object> variables = Map.of("id", List.of(allianceId));
        JsonNode apiResult = fetchGraphql(allianceQuery, variables);
        
        if (!apiResult.has("data") || !apiResult.get("data").has("alliances") || 
            !apiResult.get("data").get("alliances").has("data") || 
            apiResult.get("data").get("alliances").get("data").size() == 0) {
            logger.info("No alliance {}", allianceId);
            return Map.of("nations", List.of(), "alliance", null);
        }
        
        JsonNode allianceCoreData = apiResult.get("data").get("alliances").get("data").get(0);
        logger.info("Found: {}({})", 
                   allianceCoreData.get("name").asText(), 
                   allianceCoreData.get("acronym").asText());
        
        StringBuilder projectFields = new StringBuilder();
        for (String field : GameConstants.PROJECT_FIELDS_GRAPHQL) {
            projectFields.append(field).append(" ");
        }
        
        String nationsQuery = String.format(
            "query($alliance_id_var:[Int!],$page_var:Int){" +
            "nations(alliance_id:$alliance_id_var,page:$page_var,first:50){" +
            "data{id nation_name leader_name alliance_position color score num_cities " +
            "soldiers tanks aircraft ships missiles nukes last_active continent war_policy domestic_policy %s " +
            "cities{id name infrastructure land powered date coalpower oilpower nuclearpower windpower " + 
            "coalmine oilwell ironmine bauxitemine leadmine uramine farm gasrefinery steelmill " +
            "aluminumrefinery munitionsfactory policestation hospital recyclingcenter subway " +
            "supermarket bank mall stadium barracks factory hangar drydock} " +
            "alliance{id name acronym}}" +
            "paginatorInfo{hasMorePages currentPage}}}",
            projectFields.toString());

        List<JsonNode> allNationsData = new ArrayList<>();
        int currentPage = 1;
        boolean hasMorePages = true;
        
        while (hasMorePages) {
            Map<String, Object> pageVars = new HashMap<>();
            pageVars.put("alliance_id_var", List.of(allianceId));
            pageVars.put("page_var", currentPage);
            
            JsonNode nationsApiResult = fetchGraphql(nationsQuery, pageVars);
            
            if (!nationsApiResult.has("data") || !nationsApiResult.get("data").has("nations")) {
                logger.error("Error fetching nations page {}", currentPage);
                break;
            }
            
            JsonNode nationsPageData = nationsApiResult.get("data").get("nations");
            if (!nationsPageData.has("data") || nationsPageData.get("data").size() == 0) {
                break;
            }
            
            nationsPageData.get("data").forEach(allNationsData::add);
            
            hasMorePages = nationsPageData.get("paginatorInfo").get("hasMorePages").asBoolean(false);
            currentPage++;
            logger.info("Fetched nation page {}, more pages: {}", currentPage-1, hasMorePages);
            
            // Avoid rate limiting
            if (hasMorePages) {
                try {
                    TimeUnit.MILLISECONDS.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        
        if (allNationsData.isEmpty()) {
            logger.info("No nations found in alliance.");
            return Map.of("nations", List.of(), "alliance", allianceCoreData);
        }
        
        return Map.of("nations", allNationsData, "alliance", allianceCoreData);
    }

    public JsonNode getTradePriceHistory(int count) {
        String tradePricesQuery = "query($fetch_count:Int!){tradeprices(first:$fetch_count){data{date coal oil uranium lead iron bauxite gasoline munitions steel aluminum food}}}";
        
        Map<String, Object> variables = Map.of("fetch_count", count);
        JsonNode apiResult = fetchGraphql(tradePricesQuery, variables);
        
        if (!apiResult.has("data") || !apiResult.get("data").has("tradeprices") || 
            !apiResult.get("data").get("tradeprices").has("data")) {
            return null;
        }
        
        return apiResult.get("data").get("tradeprices").get("data");
    }


    public JsonNode getNationDetails(int nationId) {
        StringBuilder projectFields = new StringBuilder();
        for (String field : GameConstants.PROJECT_FIELDS_GRAPHQL) {
            projectFields.append(field).append(" ");
        }
        
        String nationDetailsQuery = String.format(
            "query($nation_id_var:[Int!]){" +
            "nations(id:$nation_id_var,first:1){" +
            "data{id nation_name leader_name domestic_policy war_policy %s " +
            "cities{id name infrastructure land powered date}}}}",
            projectFields.toString());
        
        Map<String, Object> variables = Map.of("nation_id_var", List.of(nationId));
        JsonNode apiResult = fetchGraphql(nationDetailsQuery, variables);
        
        if (!apiResult.has("data") || !apiResult.get("data").has("nations") || 
            !apiResult.get("data").get("nations").has("data") || 
            apiResult.get("data").get("nations").get("data").size() == 0) {
            return null;
        }
        
        return apiResult.get("data").get("nations").get("data").get(0);
    }

    public Map<String, Object> getAllDataForAnalysis(int allianceId) {
        Map<String, Object> allianceData = getAllianceData(allianceId);
        List<JsonNode> nationsList = (List<JsonNode>) allianceData.get("nations");
        JsonNode allianceApiData = (JsonNode) allianceData.get("alliance");
        
        if (nationsList == null || allianceApiData == null) {
            return Map.of();
        }
        
        Map<String, Double> resourcePricesMap = new HashMap<>();
        JsonNode tradePricesApiList = getTradePriceHistory(1);
        
        if (tradePricesApiList != null && tradePricesApiList.size() > 0) {
            JsonNode rawPricesFromApi = tradePricesApiList.get(0);
            
            Iterator<Map.Entry<String, JsonNode>> fields = rawPricesFromApi.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                String resourceKey = entry.getKey();
                JsonNode priceValue = entry.getValue();
                
                if (!resourceKey.equals("date") && !priceValue.isNull()) {
                    resourcePricesMap.put(resourceKey, priceValue.asDouble());
                }
            }
        }
        
        return Map.of(
            "nations", nationsList,
            "alliance", allianceApiData,
            "resourcePrices", resourcePricesMap
        );
    }
}