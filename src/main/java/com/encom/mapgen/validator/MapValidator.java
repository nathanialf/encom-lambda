package com.encom.mapgen.validator;

import com.encom.mapgen.model.Hexagon;
import com.encom.mapgen.model.HexCoordinate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

/**
 * Validates generated maps for connectivity and structural correctness
 */
public class MapValidator {
    private static final Logger logger = LogManager.getLogger(MapValidator.class);
    
    /**
     * Validate that all hexagons in the map are connected (no islands)
     */
    public boolean validateConnectivity(List<Hexagon> hexagons) {
        if (hexagons == null || hexagons.isEmpty()) {
            logger.warn("Empty hexagon list provided for validation");
            return true; // Empty map is technically connected
        }
        
        if (hexagons.size() == 1) {
            logger.debug("Single hexagon map is connected");
            return true; // Single hexagon is always connected
        }
        
        // Create hexagon lookup map
        Map<String, Hexagon> hexagonMap = new HashMap<>();
        for (Hexagon hex : hexagons) {
            hexagonMap.put(hex.getId(), hex);
        }
        
        // Perform BFS from first hexagon to check if all hexagons are reachable
        Set<String> visited = new HashSet<>();
        Queue<String> queue = new LinkedList<>();
        
        // Start BFS from first hexagon
        Hexagon start = hexagons.get(0);
        queue.offer(start.getId());
        visited.add(start.getId());
        
        while (!queue.isEmpty()) {
            String currentId = queue.poll();
            Hexagon current = hexagonMap.get(currentId);
            
            if (current == null) {
                logger.error("Hexagon {} not found in map during validation", currentId);
                return false;
            }
            
            // Visit all connected hexagons
            for (String connectionId : current.getConnections()) {
                if (!visited.contains(connectionId)) {
                    visited.add(connectionId);
                    queue.offer(connectionId);
                }
            }
        }
        
        // Check if all hexagons were visited
        boolean isConnected = visited.size() == hexagons.size();
        
        if (!isConnected) {
            logger.error("Map connectivity validation failed: {} of {} hexagons reachable", 
                        visited.size(), hexagons.size());
            logUnreachableHexagons(hexagons, visited);
        } else {
            logger.debug("Map connectivity validation passed: all {} hexagons reachable", 
                        hexagons.size());
        }
        
        return isConnected;
    }
    
    /**
     * Validate bidirectional connections between hexagons
     */
    public boolean validateBidirectionalConnections(List<Hexagon> hexagons) {
        Map<String, Hexagon> hexagonMap = new HashMap<>();
        for (Hexagon hex : hexagons) {
            hexagonMap.put(hex.getId(), hex);
        }
        
        for (Hexagon hex : hexagons) {
            for (String connectionId : hex.getConnections()) {
                Hexagon connectedHex = hexagonMap.get(connectionId);
                
                if (connectedHex == null) {
                    logger.error("Hexagon {} references non-existent connection {}", 
                               hex.getId(), connectionId);
                    return false;
                }
                
                if (!connectedHex.getConnections().contains(hex.getId())) {
                    logger.error("Connection from {} to {} is not bidirectional", 
                               hex.getId(), connectionId);
                    return false;
                }
            }
        }
        
        logger.debug("Bidirectional connection validation passed");
        return true;
    }
    
    /**
     * Validate that connections are only between adjacent hexagons
     */
    public boolean validateAdjacentConnections(List<Hexagon> hexagons) {
        Map<String, Hexagon> hexagonMap = new HashMap<>();
        for (Hexagon hex : hexagons) {
            hexagonMap.put(hex.getId(), hex);
        }
        
        for (Hexagon hex : hexagons) {
            Set<String> expectedNeighborIds = new HashSet<>();
            for (HexCoordinate neighbor : hex.getCoordinate().getNeighbors()) {
                expectedNeighborIds.add(neighbor.toId());
            }
            
            for (String connectionId : hex.getConnections()) {
                if (!expectedNeighborIds.contains(connectionId)) {
                    logger.error("Hexagon {} has invalid non-adjacent connection to {}", 
                               hex.getId(), connectionId);
                    return false;
                }
            }
        }
        
        logger.debug("Adjacent connection validation passed");
        return true;
    }
    
    /**
     * Validate corridor to room ratio is within acceptable bounds
     */
    public boolean validateCorridorRoomRatio(List<Hexagon> hexagons, double expectedCorridorRatio, 
                                           double tolerance) {
        if (hexagons.isEmpty()) {
            return true;
        }
        
        long corridorCount = hexagons.stream()
                                   .filter(h -> h.getType() == Hexagon.HexType.CORRIDOR)
                                   .count();
        
        double actualCorridorRatio = (double) corridorCount / hexagons.size();
        double deviation = Math.abs(actualCorridorRatio - expectedCorridorRatio);
        
        boolean withinTolerance = deviation <= tolerance;
        
        logger.debug("Corridor ratio validation: expected={}, actual={}, deviation={}, tolerance={}, passed={}", 
                    expectedCorridorRatio, actualCorridorRatio, deviation, tolerance, withinTolerance);
        
        return withinTolerance;
    }
    
    /**
     * Comprehensive validation of the entire map
     */
    public ValidationResult validateMap(List<Hexagon> hexagons, double expectedCorridorRatio) {
        ValidationResult result = new ValidationResult();
        
        // Test connectivity
        result.isConnected = validateConnectivity(hexagons);
        
        // Test bidirectional connections
        result.hasBidirectionalConnections = validateBidirectionalConnections(hexagons);
        
        // Test adjacent connections
        result.hasValidAdjacentConnections = validateAdjacentConnections(hexagons);
        
        // Test corridor/room ratio (with 15% tolerance)
        result.hasValidRatio = validateCorridorRoomRatio(hexagons, expectedCorridorRatio, 0.15);
        
        // Overall validity
        result.isValid = result.isConnected && result.hasBidirectionalConnections && 
                        result.hasValidAdjacentConnections && result.hasValidRatio;
        
        if (result.isValid) {
            logger.info("Map validation passed all tests");
        } else {
            logger.warn("Map validation failed: connected={}, bidirectional={}, adjacent={}, ratio={}", 
                       result.isConnected, result.hasBidirectionalConnections, 
                       result.hasValidAdjacentConnections, result.hasValidRatio);
        }
        
        return result;
    }
    
    /**
     * Log unreachable hexagons for debugging
     */
    private void logUnreachableHexagons(List<Hexagon> hexagons, Set<String> reachable) {
        List<String> unreachable = new ArrayList<>();
        for (Hexagon hex : hexagons) {
            if (!reachable.contains(hex.getId())) {
                unreachable.add(hex.getId());
            }
        }
        
        logger.error("Unreachable hexagons: {}", unreachable);
    }
    
    /**
     * Result of comprehensive map validation
     */
    public static class ValidationResult {
        public boolean isValid;
        public boolean isConnected;
        public boolean hasBidirectionalConnections;
        public boolean hasValidAdjacentConnections;
        public boolean hasValidRatio;
        
        public ValidationResult() {
            this.isValid = false;
            this.isConnected = false;
            this.hasBidirectionalConnections = false;
            this.hasValidAdjacentConnections = false;
            this.hasValidRatio = false;
        }
        
        @Override
        public String toString() {
            return "ValidationResult{" +
                    "isValid=" + isValid +
                    ", isConnected=" + isConnected +
                    ", hasBidirectionalConnections=" + hasBidirectionalConnections +
                    ", hasValidAdjacentConnections=" + hasValidAdjacentConnections +
                    ", hasValidRatio=" + hasValidRatio +
                    '}';
        }
    }
}