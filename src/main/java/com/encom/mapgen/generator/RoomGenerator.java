package com.encom.mapgen.generator;

import com.encom.mapgen.model.GenerationOptions;
import com.encom.mapgen.model.HexCoordinate;
import com.encom.mapgen.model.Hexagon;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

/**
 * Generates room structures - connected clusters of hexagons
 */
public class RoomGenerator {
    private static final Logger logger = LogManager.getLogger(RoomGenerator.class);
    
    private final SeedManager seedManager;
    private final GenerationOptions options;
    
    public RoomGenerator(SeedManager seedManager, GenerationOptions options) {
        this.seedManager = seedManager;
        this.options = options;
    }
    
    /**
     * Generate a room cluster from available frontier positions
     */
    public List<HexCoordinate> generateRoom(Set<HexCoordinate> frontier, 
                                           Map<String, Hexagon> existingHexagons,
                                           int targetSize) {
        if (frontier.isEmpty() || targetSize <= 0) {
            return new ArrayList<>();
        }
        
        // Choose a random starting point from frontier that connects to existing map
        HexCoordinate startPoint = selectRoomStartPoint(frontier, existingHexagons);
        if (startPoint == null) {
            return new ArrayList<>();
        }
        
        // Generate room using organic growth algorithm
        List<HexCoordinate> roomHexagons = generateRoomCluster(startPoint, existingHexagons, targetSize);
        
        logger.debug("Generated room with {} hexagons", roomHexagons.size());
        return roomHexagons;
    }
    
    /**
     * Select a starting point for room generation
     */
    private HexCoordinate selectRoomStartPoint(Set<HexCoordinate> frontier, 
                                              Map<String, Hexagon> existingHexagons) {
        // Filter frontier points that are adjacent to existing hexagons
        List<HexCoordinate> validStarts = new ArrayList<>();
        
        for (HexCoordinate coord : frontier) {
            if (hasAdjacentExistingHexagon(coord, existingHexagons)) {
                // Prefer positions that don't have too many existing neighbors (avoid overcrowding)
                int adjacentCount = countAdjacentExisting(coord, existingHexagons);
                if (adjacentCount <= 3) { // Allow up to 3 adjacent existing hexagons
                    validStarts.add(coord);
                }
            }
        }
        
        if (validStarts.isEmpty()) {
            // If no ideal positions, relax the constraint
            for (HexCoordinate coord : frontier) {
                if (hasAdjacentExistingHexagon(coord, existingHexagons)) {
                    validStarts.add(coord);
                }
            }
        }
        
        if (validStarts.isEmpty()) {
            logger.warn("No valid starting points found for room generation");
            return null;
        }
        
        // Select random starting point
        return validStarts.get(seedManager.nextInt(validStarts.size()));
    }
    
    /**
     * Check if a coordinate has at least one adjacent existing hexagon
     */
    private boolean hasAdjacentExistingHexagon(HexCoordinate coord, Map<String, Hexagon> existingHexagons) {
        return coord.getNeighbors().stream()
                   .anyMatch(neighbor -> existingHexagons.containsKey(neighbor.toId()));
    }
    
    /**
     * Count adjacent existing hexagons
     */
    private int countAdjacentExisting(HexCoordinate coord, Map<String, Hexagon> existingHexagons) {
        return (int) coord.getNeighbors().stream()
                         .filter(neighbor -> existingHexagons.containsKey(neighbor.toId()))
                         .count();
    }
    
    /**
     * Generate room cluster using organic growth algorithm
     */
    private List<HexCoordinate> generateRoomCluster(HexCoordinate start, 
                                                   Map<String, Hexagon> existingHexagons,
                                                   int targetSize) {
        List<HexCoordinate> room = new ArrayList<>();
        Set<HexCoordinate> visited = new HashSet<>();
        Queue<HexCoordinate> growthQueue = new LinkedList<>();
        
        // Start the room
        room.add(start);
        visited.add(start);
        growthQueue.offer(start);
        
        // Organic growth algorithm
        while (!growthQueue.isEmpty() && room.size() < targetSize) {
            HexCoordinate current = growthQueue.poll();
            
            // Get potential growth positions
            List<HexCoordinate> growthCandidates = getGrowthCandidates(current, existingHexagons, visited);
            
            if (growthCandidates.isEmpty()) {
                continue;
            }
            
            // Determine how many positions to add from this hexagon
            int remainingSpace = targetSize - room.size();
            int positionsToAdd = determineGrowthAmount(growthCandidates.size(), remainingSpace);
            
            // Select positions using organic growth preferences
            List<HexCoordinate> selectedPositions = selectGrowthPositions(current, growthCandidates, 
                                                                        positionsToAdd, room, visited);
            
            // Add selected positions to room
            for (HexCoordinate pos : selectedPositions) {
                room.add(pos);
                visited.add(pos);
                
                // Add to growth queue with probability based on room connectivity
                if (shouldContinueGrowthFrom(pos, room, targetSize)) {
                    growthQueue.offer(pos);
                }
            }
        }
        
        return room;
    }
    
    /**
     * Get candidate positions for room growth
     */
    private List<HexCoordinate> getGrowthCandidates(HexCoordinate current, 
                                                   Map<String, Hexagon> existingHexagons,
                                                   Set<HexCoordinate> visited) {
        List<HexCoordinate> candidates = new ArrayList<>();
        
        for (HexCoordinate neighbor : current.getNeighbors()) {
            // Skip if already visited or occupied by existing hexagons
            if (visited.contains(neighbor) || existingHexagons.containsKey(neighbor.toId())) {
                continue;
            }
            
            // Skip if it would create too dense connections with existing map
            int existingNeighborCount = countAdjacentExisting(neighbor, existingHexagons);
            if (existingNeighborCount > 2) {
                continue;
            }
            
            candidates.add(neighbor);
        }
        
        return candidates;
    }
    
    /**
     * Determine how many positions to add in this growth step
     */
    private int determineGrowthAmount(int availableCandidates, int remainingSpace) {
        if (availableCandidates == 0 || remainingSpace <= 0) {
            return 0;
        }
        
        // Organic growth - sometimes grow aggressively, sometimes slowly
        double growthFactor = seedManager.nextDouble();
        
        int maxGrowth = Math.min(availableCandidates, remainingSpace);
        
        if (growthFactor < 0.3) {
            // Slow growth - add 1 position
            return 1;
        } else if (growthFactor < 0.7) {
            // Medium growth - add 2-3 positions
            return Math.min(maxGrowth, 2 + seedManager.nextInt(2));
        } else {
            // Fast growth - add more positions
            return Math.min(maxGrowth, 3 + seedManager.nextInt(3));
        }
    }
    
    /**
     * Select specific positions for growth using organic preferences
     */
    private List<HexCoordinate> selectGrowthPositions(HexCoordinate center,
                                                     List<HexCoordinate> candidates,
                                                     int count,
                                                     List<HexCoordinate> existingRoom,
                                                     Set<HexCoordinate> visited) {
        if (candidates.isEmpty() || count <= 0) {
            return new ArrayList<>();
        }
        
        // Score candidates based on organic growth preferences
        List<ScoredPosition> scoredPositions = new ArrayList<>();
        
        for (HexCoordinate candidate : candidates) {
            double score = calculateGrowthScore(candidate, existingRoom, visited);
            scoredPositions.add(new ScoredPosition(candidate, score));
        }
        
        // Sort by score (higher is better)
        scoredPositions.sort((a, b) -> Double.compare(b.score, a.score));
        
        // Select top positions with some randomization
        List<HexCoordinate> selected = new ArrayList<>();
        int actualCount = Math.min(count, scoredPositions.size());
        
        for (int i = 0; i < actualCount; i++) {
            // Add some randomness - sometimes pick lower scored positions for variety
            int index = i;
            if (seedManager.nextDouble() < 0.2 && i < scoredPositions.size() - 1) {
                index = Math.min(i + 1 + seedManager.nextInt(2), scoredPositions.size() - 1);
            }
            selected.add(scoredPositions.get(index).coordinate);
        }
        
        return selected;
    }
    
    /**
     * Calculate growth score for organic room shape
     */
    private double calculateGrowthScore(HexCoordinate candidate, List<HexCoordinate> existingRoom,
                                       Set<HexCoordinate> visited) {
        double score = 0.0;
        
        // Prefer positions that connect to multiple existing room positions
        int roomNeighborCount = 0;
        for (HexCoordinate neighbor : candidate.getNeighbors()) {
            if (visited.contains(neighbor)) {
                roomNeighborCount++;
            }
        }
        
        // Score based on connectivity (prefer 2-3 connections for organic shape)
        if (roomNeighborCount == 2 || roomNeighborCount == 3) {
            score += 2.0;
        } else if (roomNeighborCount == 1) {
            score += 1.0;
        } else if (roomNeighborCount >= 4) {
            score += 0.5; // Discourage over-connectivity
        }
        
        // Prefer positions that don't create too regular shapes
        // Add slight randomness for organic feel
        score += seedManager.nextDouble() * 0.5;
        
        // Slightly prefer positions closer to room center (for compactness)
        if (!existingRoom.isEmpty()) {
            HexCoordinate roomCenter = calculateRoomCenter(existingRoom);
            int distance = candidate.distanceTo(roomCenter);
            score += Math.max(0, 3.0 - distance) * 0.1;
        }
        
        return score;
    }
    
    /**
     * Calculate the approximate center of the room
     */
    private HexCoordinate calculateRoomCenter(List<HexCoordinate> room) {
        if (room.isEmpty()) {
            return new HexCoordinate(0, 0);
        }
        
        int sumQ = room.stream().mapToInt(HexCoordinate::getQ).sum();
        int sumR = room.stream().mapToInt(HexCoordinate::getR).sum();
        
        int avgQ = sumQ / room.size();
        int avgR = sumR / room.size();
        
        return new HexCoordinate(avgQ, avgR);
    }
    
    /**
     * Determine if growth should continue from this position
     */
    private boolean shouldContinueGrowthFrom(HexCoordinate position, List<HexCoordinate> room, int targetSize) {
        // Continue growth if we haven't reached target size
        if (room.size() >= targetSize) {
            return false;
        }
        
        // Reduce probability as room gets larger (organic rooms tend to be more compact)
        double continueProbability = 0.8 - (room.size() / (double) targetSize) * 0.3;
        
        return seedManager.nextDouble() < continueProbability;
    }
    
    /**
     * Helper class for scoring growth positions
     */
    private static class ScoredPosition {
        final HexCoordinate coordinate;
        final double score;
        
        ScoredPosition(HexCoordinate coordinate, double score) {
            this.coordinate = coordinate;
            this.score = score;
        }
    }
}