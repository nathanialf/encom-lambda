package com.encom.mapgen.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.encom.mapgen.generator.MapGenerator;
import com.encom.mapgen.model.GenerationRequest;
import com.encom.mapgen.model.MapManifest;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * AWS Lambda handler for hexagonal map generation
 */
public class MapGeneratorHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private static final Logger logger = LogManager.getLogger(MapGeneratorHandler.class);
    
    private final Gson gson;
    
    // Environment configuration
    private final int defaultHexagonCount;
    private final int maxHexagonCount;
    
    public MapGeneratorHandler() {
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();
        
        // Read configuration from environment variables
        this.defaultHexagonCount = Integer.parseInt(
                System.getenv().getOrDefault("DEFAULT_HEXAGON_COUNT", "50"));
        this.maxHexagonCount = Integer.parseInt(
                System.getenv().getOrDefault("MAX_HEXAGON_COUNT", "200"));
        
        logger.info("MapGeneratorHandler initialized - default: {}, max: {}", 
                   defaultHexagonCount, maxHexagonCount);
    }
    
    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        logger.info("Received map generation request - RequestId: {}", context.getAwsRequestId());
        
        try {
            // Parse and validate request
            GenerationRequest request = parseRequest(input);
            validateRequest(request);
            
            logger.info("Processing generation request: seed={}, count={}", 
                       request.getSeed(), request.getHexagonCount());
            
            // Generate map
            MapGenerator generator = new MapGenerator(request);
            MapManifest manifest = generator.generateMap(request.getHexagonCount());
            
            // Log generation metrics
            logGenerationMetrics(manifest, context);
            
            // Return success response
            return createSuccessResponse(manifest);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request: {}", e.getMessage());
            return createErrorResponse(400, "Invalid request: " + e.getMessage());
            
        } catch (Exception e) {
            logger.error("Map generation failed", e);
            return createErrorResponse(500, "Internal server error: Map generation failed");
        }
    }
    
    /**
     * Parse the incoming request body
     */
    private GenerationRequest parseRequest(APIGatewayProxyRequestEvent input) {
        String body = input.getBody();
        
        if (body == null || body.trim().isEmpty()) {
            // Create default request
            GenerationRequest defaultRequest = new GenerationRequest();
            defaultRequest.setHexagonCount(defaultHexagonCount);
            return defaultRequest;
        }
        
        try {
            GenerationRequest request = gson.fromJson(body, GenerationRequest.class);
            
            // Set defaults if not provided
            if (request.getHexagonCount() <= 0) {
                request.setHexagonCount(defaultHexagonCount);
            }
            
            return request;
            
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JSON request body: " + e.getMessage());
        }
    }
    
    /**
     * Validate the generation request
     */
    private void validateRequest(GenerationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null");
        }
        
        if (request.getHexagonCount() < 1) {
            throw new IllegalArgumentException("Hexagon count must be at least 1");
        }
        
        if (request.getHexagonCount() > maxHexagonCount) {
            throw new IllegalArgumentException("Hexagon count cannot exceed " + maxHexagonCount);
        }
        
        // Validate request options
        if (request.getOptions() != null) {
            request.getOptions().validate();
        }
    }
    
    /**
     * Create successful response
     */
    private APIGatewayProxyResponseEvent createSuccessResponse(MapManifest manifest) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        response.setStatusCode(200);
        response.setHeaders(createResponseHeaders());
        response.setBody(gson.toJson(manifest));
        
        return response;
    }
    
    /**
     * Create error response
     */
    private APIGatewayProxyResponseEvent createErrorResponse(int statusCode, String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", message);
        error.put("statusCode", statusCode);
        error.put("timestamp", System.currentTimeMillis());
        
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        response.setStatusCode(statusCode);
        response.setHeaders(createResponseHeaders());
        response.setBody(gson.toJson(error));
        
        return response;
    }
    
    /**
     * Create standard response headers with CORS support
     */
    private Map<String, String> createResponseHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Access-Control-Allow-Origin", "*");
        headers.put("Access-Control-Allow-Methods", "POST, OPTIONS");
        headers.put("Access-Control-Allow-Headers", "Content-Type, x-api-key");
        headers.put("Access-Control-Max-Age", "86400");
        
        return headers;
    }
    
    /**
     * Log generation metrics for monitoring
     */
    private void logGenerationMetrics(MapManifest manifest, Context context) {
        MapManifest.Metadata metadata = manifest.getMetadata();
        MapManifest.Statistics stats = metadata.getStatistics();
        
        logger.info("Map generation completed - " +
                   "seed={}, hexagons={}, corridors={}, rooms={}, " +
                   "avgConnections={}, generationTime={}ms, requestId={}", 
                   metadata.getSeed(),
                   stats.getActualHexagons(),
                   stats.getCorridorHexagons(),
                   stats.getRoomHexagons(),
                   stats.getAverageConnections(),
                   metadata.getGenerationTime(),
                   context.getAwsRequestId());
        
        // Structured logging for CloudWatch metrics
        logger.info("METRIC generation_time_ms={}", metadata.getGenerationTime());
        logger.info("METRIC hexagon_count={}", stats.getActualHexagons());
        logger.info("METRIC corridor_count={}", stats.getCorridorHexagons());
        logger.info("METRIC room_count={}", stats.getRoomHexagons());
        logger.info("METRIC average_connections={}", stats.getAverageConnections());
        logger.info("METRIC max_connections={}", stats.getMaxConnections());
        logger.info("METRIC longest_path={}", stats.getLongestPath());
    }
    
    /**
     * Handle OPTIONS request for CORS preflight
     */
    private APIGatewayProxyResponseEvent handleOptionsRequest() {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        response.setStatusCode(200);
        response.setHeaders(createResponseHeaders());
        response.setBody("");
        
        return response;
    }
}