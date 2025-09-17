package com.encom.mapgen.generator;

import com.encom.mapgen.model.*;
import com.encom.mapgen.validator.MapValidator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Core map generation engine using frontier-based growth algorithm
 */
public class MapGenerator {
    private static final Logger logger = LogManager.getLogger(MapGenerator.class);
    
    private final SeedManager seedManager;
    private final GenerationOptions options;
    private final CorridorGenerator corridorGenerator;
    private final RoomGenerator roomGenerator;
    
    // Map state
    private final Map<String, Hexagon> hexagonMap;
    private final Set<HexCoordinate> frontier;
    private int currentHexagonCount;
    
    public MapGenerator(GenerationRequest request) {
        this.seedManager = new SeedManager(request.getSeed());
        this.options = request.getOptions();
        this.corridorGenerator = new CorridorGenerator(seedManager, options);
        this.roomGenerator = new RoomGenerator(seedManager, options);
        
        this.hexagonMap = new HashMap<>();
        this.frontier = new HashSet<>();
        this.currentHexagonCount = 0;
        
        logger.info("MapGenerator initialized with seed: {}, target count: {}", 
                   seedManager.getSeed(), request.getHexagonCount());
    }
    
    /**
     * Generate a complete map manifest
     */
    public MapManifest generateMap(int targetHexagonCount) {
        long startTime = System.currentTimeMillis();
        
        try {
            logger.info("Starting map generation for {} hexagons", targetHexagonCount);
            
            // Initialize with starting hexagon at origin
            initializeMap();
            
            // Growth phase - add hexagons until target reached
            growMap(targetHexagonCount);
            
            // Post-processing phase - optimize corridor paths
            postProcessCorridors();
            
            // Validate connectivity
            validateMap();
            
            // Calculate statistics
            MapManifest.Statistics statistics = calculateStatistics();
            
            // Build response
            MapManifest.Metadata metadata = buildMetadata(targetHexagonCount, 
                                                        System.currentTimeMillis() - startTime, 
                                                        statistics);
            
            List<Hexagon> hexagonList = new ArrayList<>(hexagonMap.values());
            MapManifest manifest = new MapManifest(metadata, hexagonList);
            
            logger.info("Map generation completed successfully in {}ms", 
                       System.currentTimeMillis() - startTime);
            
            return manifest;
            
        } catch (Exception e) {
            logger.error("Map generation failed", e);
            throw new RuntimeException("Failed to generate map: " + e.getMessage(), e);
        }
    }
    
    /**
     * Initialize map with starting hexagon at origin
     */
    private void initializeMap() {
        HexCoordinate origin = new HexCoordinate(0, 0);
        Hexagon startHex = new Hexagon(origin, Hexagon.HexType.CORRIDOR);
        
        hexagonMap.put(startHex.getId(), startHex);
        currentHexagonCount = 1;
        
        // Add neighbors to frontier
        origin.getNeighbors().forEach(frontier::add);
        
        logger.debug("Map initialized with starting hexagon at origin");
    }
    
    /**
     * Main growth algorithm - continue until target count reached
     */
    private void growMap(int targetHexagonCount) {
        while (currentHexagonCount < targetHexagonCount && !frontier.isEmpty()) {
            // Decide whether to generate corridor or room
            boolean shouldGenerateCorridor = seedManager.shouldGenerateCorridor(options.getCorridorRatio());
            
            int remainingHexagons = targetHexagonCount - currentHexagonCount;
            
            if (shouldGenerateCorridor) {
                generateCorridorSegment(remainingHexagons);
            } else {
                generateRoomCluster(remainingHexagons);
            }
            
            // Safety check to prevent infinite loops
            if (frontier.isEmpty()) {
                logger.warn("Frontier exhausted with {} hexagons generated", currentHexagonCount);
                break;
            }
        }
        
        logger.info("Growth phase completed: {} hexagons generated", currentHexagonCount);
    }
    
    /**
     * Generate a corridor segment from frontier
     */
    private void generateCorridorSegment(int maxHexagons) {
        List<HexCoordinate> corridorHexagons = corridorGenerator.generateCorridor(
                frontier, hexagonMap, Math.min(maxHexagons, 8));
        
        addHexagonsToMap(corridorHexagons, Hexagon.HexType.CORRIDOR);
    }
    
    /**
     * Generate a room cluster from frontier
     */
    private void generateRoomCluster(int maxHexagons) {
        // Ensure we don't try to generate more hexagons than available
        int maxPossibleRoomSize = Math.min(options.getRoomSizeMax(), maxHexagons);
        int minRoomSize = Math.min(options.getRoomSizeMin(), maxPossibleRoomSize);
        
        // If maxPossibleRoomSize is less than minRoomSize, just use what we have
        if (maxPossibleRoomSize < minRoomSize) {
            minRoomSize = maxPossibleRoomSize;
        }
        
        int roomSize = minRoomSize == maxPossibleRoomSize ? minRoomSize : 
                      seedManager.nextInt(minRoomSize, maxPossibleRoomSize + 1);
        
        List<HexCoordinate> roomHexagons = roomGenerator.generateRoom(
                frontier, hexagonMap, roomSize);
        
        addHexagonsToMap(roomHexagons, Hexagon.HexType.ROOM);
    }
    
    /**
     * Add generated hexagons to the map and update frontier
     */
    private void addHexagonsToMap(List<HexCoordinate> coordinates, Hexagon.HexType type) {
        if (coordinates.isEmpty()) {
            return;
        }
        
        // Create hexagons
        for (HexCoordinate coord : coordinates) {
            if (!hexagonMap.containsKey(coord.toId())) {
                Hexagon hex = new Hexagon(coord, type);
                hexagonMap.put(hex.getId(), hex);
                currentHexagonCount++;
                
                // Remove from frontier if present
                frontier.remove(coord);
            }
        }
        
        // Update connections
        updateConnections(coordinates);
        
        // Update frontier with new neighbors
        updateFrontier(coordinates);
        
        logger.debug("Added {} {} hexagons to map", coordinates.size(), type);
    }
    
    /**
     * Update connections between hexagons
     */
    private void updateConnections(List<HexCoordinate> newCoordinates) {
        for (HexCoordinate coord : newCoordinates) {
            Hexagon hex = hexagonMap.get(coord.toId());
            if (hex == null) continue;
            
            // Check each neighbor
            for (HexCoordinate neighbor : coord.getNeighbors()) {
                Hexagon neighborHex = hexagonMap.get(neighbor.toId());
                if (neighborHex != null) {
                    // Create bidirectional connection
                    hex.addConnection(neighborHex.getId());
                    neighborHex.addConnection(hex.getId());
                }
            }
        }
    }
    
    /**
     * Update frontier with new expansion possibilities
     */
    private void updateFrontier(List<HexCoordinate> newCoordinates) {
        for (HexCoordinate coord : newCoordinates) {
            for (HexCoordinate neighbor : coord.getNeighbors()) {
                // Add to frontier if not already occupied
                if (!hexagonMap.containsKey(neighbor.toId())) {
                    frontier.add(neighbor);
                }
            }
        }
    }
    
    /**
     * Validate the generated map for connectivity
     */
    private void validateMap() {
        MapValidator validator = new MapValidator();
        boolean isValid = validator.validateConnectivity(new ArrayList<>(hexagonMap.values()));
        
        if (!isValid) {
            throw new RuntimeException("Generated map failed connectivity validation");
        }
        
        logger.debug("Map validation passed");
    }
    
    /**
     * Calculate detailed statistics about the generated map
     */
    private MapManifest.Statistics calculateStatistics() {
        MapManifest.Statistics stats = new MapManifest.Statistics();
        
        List<Hexagon> hexagons = new ArrayList<>(hexagonMap.values());
        
        // Basic counts
        stats.setActualHexagons(hexagons.size());
        stats.setCorridorHexagons((int) hexagons.stream()
                .filter(h -> h.getType() == Hexagon.HexType.CORRIDOR).count());
        stats.setRoomHexagons((int) hexagons.stream()
                .filter(h -> h.getType() == Hexagon.HexType.ROOM).count());
        
        // Connection statistics
        double avgConnections = hexagons.stream()
                .mapToInt(Hexagon::getConnectionCount)
                .average()
                .orElse(0.0);
        stats.setAverageConnections(Math.round(avgConnections * 100.0) / 100.0);
        
        int maxConnections = hexagons.stream()
                .mapToInt(Hexagon::getConnectionCount)
                .max()
                .orElse(0);
        stats.setMaxConnections(maxConnections);
        
        // Longest path (simplified BFS from random starting point)
        stats.setLongestPath(calculateLongestPath(hexagons));
        
        // Bounding box
        stats.setBoundingBox(calculateBoundingBox(hexagons));
        
        return stats;
    }
    
    /**
     * Calculate the longest path in the map using BFS
     */
    private int calculateLongestPath(List<Hexagon> hexagons) {
        if (hexagons.isEmpty()) return 0;
        
        // Use BFS to find longest path from a random starting hexagon
        Hexagon start = hexagons.get(seedManager.nextInt(hexagons.size()));
        return bfsLongestPath(start, hexagons);
    }
    
    /**
     * BFS to find longest path from a starting hexagon
     */
    private int bfsLongestPath(Hexagon start, List<Hexagon> hexagons) {
        Map<String, Hexagon> hexMap = hexagons.stream()
                .collect(Collectors.toMap(Hexagon::getId, h -> h));
        
        Queue<String> queue = new LinkedList<>();
        Map<String, Integer> distances = new HashMap<>();
        
        queue.offer(start.getId());
        distances.put(start.getId(), 0);
        
        int maxDistance = 0;
        
        while (!queue.isEmpty()) {
            String currentId = queue.poll();
            Hexagon current = hexMap.get(currentId);
            int currentDistance = distances.get(currentId);
            
            for (String connId : current.getConnections()) {
                if (!distances.containsKey(connId)) {
                    distances.put(connId, currentDistance + 1);
                    queue.offer(connId);
                    maxDistance = Math.max(maxDistance, currentDistance + 1);
                }
            }
        }
        
        return maxDistance;
    }
    
    /**
     * Calculate bounding box of all hexagons
     */
    private MapManifest.BoundingBox calculateBoundingBox(List<Hexagon> hexagons) {
        if (hexagons.isEmpty()) {
            return new MapManifest.BoundingBox(0, 0, 0, 0);
        }
        
        int minQ = hexagons.stream().mapToInt(Hexagon::getQ).min().orElse(0);
        int maxQ = hexagons.stream().mapToInt(Hexagon::getQ).max().orElse(0);
        int minR = hexagons.stream().mapToInt(Hexagon::getR).min().orElse(0);
        int maxR = hexagons.stream().mapToInt(Hexagon::getR).max().orElse(0);
        
        return new MapManifest.BoundingBox(minQ, maxQ, minR, maxR);
    }
    
    /**
     * Build metadata for the response
     */
    private MapManifest.Metadata buildMetadata(int targetCount, long generationTime, 
                                             MapManifest.Statistics statistics) {
        MapManifest.Metadata metadata = new MapManifest.Metadata();
        metadata.setSeed(seedManager.getSeed());
        metadata.setHexagonCount(targetCount);
        metadata.setGenerationTime(generationTime);
        metadata.setStatistics(statistics);
        
        return metadata;
    }
    
    /**
     * Post-process corridors to reduce clustering while preserving connectivity
     * Allows up to 3 connections occasionally for branching
     */
    private void postProcessCorridors() {
        logger.info("Starting corridor post-processing");
        
        List<Hexagon> corridors = hexagonMap.values().stream()
                .filter(hex -> hex.getType() == Hexagon.HexType.CORRIDOR)
                .collect(Collectors.toList());
        
        int connectionsRemoved = 0;
        int corridorsProcessed = 0;
        
        // Sort corridors by connection count (highest first) to process worst cases first
        corridors.sort((a, b) -> Integer.compare(b.getConnectionCount(), a.getConnectionCount()));
        
        for (Hexagon corridor : corridors) {
            int connectionCount = corridor.getConnectionCount();
            
            // Process corridors with 3+ connections (prioritize 2, allow 3 if needed)
            if (connectionCount >= 3) {
                int beforeCount = corridor.getConnectionCount();
                int removed = reduceCorridorConnections(corridor);
                int afterCount = corridor.getConnectionCount();
                
                if (removed > 0) {
                    logger.debug("Processed corridor {}: {} -> {} connections ({} removed)", 
                               corridor.getId(), beforeCount, afterCount, removed);
                }
                
                connectionsRemoved += removed;
                corridorsProcessed++;
            }
        }
        
        logger.info("Corridor post-processing completed: {} connections removed from {} corridors out of {} total", 
                   connectionsRemoved, corridorsProcessed, corridors.size());
    }
    
    /**
     * Reduce corridor connections while preserving connectivity
     * Priority: reduce to 2 connections, allow 3 if needed for connectivity
     */
    private int reduceCorridorConnections(Hexagon corridor) {
        List<String> connections = new ArrayList<>(corridor.getConnections());
        
        if (connections.size() <= 2) {
            return 0; // Already at ideal target
        }
        
        // Get coordinates of connected hexagons
        List<HexCoordinate> connectedCoords = connections.stream()
                .map(connId -> hexagonMap.get(connId))
                .filter(Objects::nonNull)
                .map(hex -> new HexCoordinate(hex.getQ(), hex.getR()))
                .collect(Collectors.toList());
        
        if (connectedCoords.size() <= 3) {
            return 0;
        }
        
        // Find connections to remove one by one, checking connectivity after each removal
        int connectionsRemoved = 0;
        HexCoordinate corridorCoord = new HexCoordinate(corridor.getQ(), corridor.getR());
        
        // Calculate linearity scores for all connections
        List<ConnectionScore> connectionScores = new ArrayList<>();
        for (int i = 0; i < connections.size(); i++) {
            String connId = connections.get(i);
            HexCoordinate connCoord = connectedCoords.get(i);
            double score = calculateConnectionImportance(corridorCoord, connCoord, connections, connectedCoords);
            connectionScores.add(new ConnectionScore(connId, score));
        }
        
        // Sort by importance (lowest first - these are candidates for removal)
        connectionScores.sort((a, b) -> Double.compare(a.score, b.score));
        
        // Try to remove connections starting with least important, checking connectivity
        // Priority: get down to 2 connections, but allow 3 if needed for connectivity
        for (ConnectionScore connScore : connectionScores) {
            // First pass: try to get to 2 connections
            if (corridor.getConnectionCount() <= 2) {
                break; // Reached ideal target
            }
            
            // Second priority: allow up to 3 connections but no more
            if (corridor.getConnectionCount() == 3) {
                // Only remove if we can get to 2 without breaking connectivity
                // This is more aggressive for the 3->2 transition
            }
            
            String connId = connScore.connectionId;
            
            // Test removal - temporarily remove and check connectivity
            corridor.removeConnection(connId);
            Hexagon connectedHex = hexagonMap.get(connId);
            if (connectedHex != null) {
                connectedHex.removeConnection(corridor.getId());
            }
            
            // Check if map is still connected
            if (isMapConnected()) {
                // Good removal - keep it removed
                connectionsRemoved++;
                logger.debug("Safely removed connection {} from corridor {}", connId, corridor.getId());
            } else {
                // Bad removal - restore the connection
                corridor.addConnection(connId);
                if (connectedHex != null) {
                    connectedHex.addConnection(corridor.getId());
                }
                logger.debug("Restored connection {} to corridor {} (needed for connectivity)", connId, corridor.getId());
            }
        }
        
        return connectionsRemoved;
    }
    
    /**
     * Helper class to store connection with its importance score
     */
    private static class ConnectionScore {
        String connectionId;
        double score;
        
        ConnectionScore(String connectionId, double score) {
            this.connectionId = connectionId;
            this.score = score;
        }
    }
    
    /**
     * Calculate importance score for a connection (lower = less important = candidate for removal)
     * Prioritizes linear paths for corridor flow
     */
    private double calculateConnectionImportance(HexCoordinate center, HexCoordinate target,
                                               List<String> allConnections, List<HexCoordinate> allCoords) {
        // Base score starts at 0
        double importance = 0.0;
        
        // Factor 1: Linearity with other connections (heavily weighted for straight corridors)
        double maxLinearity = 0.0;
        for (int i = 0; i < allCoords.size(); i++) {
            HexCoordinate other = allCoords.get(i);
            if (!other.equals(target)) {
                double linearity = calculateLinearity(target, center, other);
                maxLinearity = Math.max(maxLinearity, linearity);
            }
        }
        importance += maxLinearity * 3.0; // Increased weight for linearity
        
        // Factor 2: Distance (hexagonal grid distance)
        double distance = center.distanceTo(target);
        importance += 1.0 / (distance + 1.0);
        
        // Factor 3: Small random factor to break ties deterministically
        importance += seedManager.nextDouble() * 0.05; // Reduced randomness
        
        return importance;
    }
    
    /**
     * Check if the map is still fully connected using BFS
     */
    private boolean isMapConnected() {
        List<Hexagon> allHexagons = new ArrayList<>(hexagonMap.values());
        if (allHexagons.isEmpty()) {
            return true;
        }
        
        // Start BFS from first hexagon
        Set<String> visited = new HashSet<>();
        Queue<String> queue = new LinkedList<>();
        
        String startId = allHexagons.get(0).getId();
        queue.offer(startId);
        visited.add(startId);
        
        while (!queue.isEmpty()) {
            String currentId = queue.poll();
            Hexagon current = hexagonMap.get(currentId);
            
            if (current != null) {
                for (String connId : current.getConnections()) {
                    if (!visited.contains(connId)) {
                        visited.add(connId);
                        queue.offer(connId);
                    }
                }
            }
        }
        
        // Check if all hexagons were visited
        return visited.size() == allHexagons.size();
    }
    
    /**
     * Calculate linearity score for three points (higher score = more linear)
     * Returns a value between 0 (90 degrees) and 1 (180 degrees)
     */
    private double calculateLinearity(HexCoordinate p1, HexCoordinate center, HexCoordinate p2) {
        // Vector from center to p1
        double v1x = p1.getQ() - center.getQ();
        double v1y = p1.getR() - center.getR();
        
        // Vector from center to p2
        double v2x = p2.getQ() - center.getQ();
        double v2y = p2.getR() - center.getR();
        
        // Calculate dot product and magnitudes
        double dotProduct = v1x * v2x + v1y * v2y;
        double mag1 = Math.sqrt(v1x * v1x + v1y * v1y);
        double mag2 = Math.sqrt(v2x * v2x + v2y * v2y);
        
        if (mag1 == 0 || mag2 == 0) {
            return 0;
        }
        
        // Calculate cosine of angle between vectors
        double cosAngle = dotProduct / (mag1 * mag2);
        
        // Clamp to valid range for acos
        cosAngle = Math.max(-1.0, Math.min(1.0, cosAngle));
        
        // Convert to linearity score: -1 (opposite directions, 180deg) = 1.0, 0 (perpendicular, 90deg) = 0.0
        // We want opposite directions to have the highest score (most linear)
        return -cosAngle; // -1 becomes 1.0 (best linearity), 0 stays 0.0, 1 becomes -1.0 (worst)
    }
    
    /**
     * Simple pair class for holding two values
     */
    private static class Pair<T> {
        final T first;
        final T second;
        
        Pair(T first, T second) {
            this.first = first;
            this.second = second;
        }
    }
}