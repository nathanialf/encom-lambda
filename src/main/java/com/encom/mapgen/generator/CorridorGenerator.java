package com.encom.mapgen.generator;

import com.encom.mapgen.model.GenerationOptions;
import com.encom.mapgen.model.HexCoordinate;
import com.encom.mapgen.model.Hexagon;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

/**
 * Generates corridor structures - linear paths that can branch
 */
public class CorridorGenerator {
    private static final Logger logger = LogManager.getLogger(CorridorGenerator.class);
    
    private final SeedManager seedManager;
    private final GenerationOptions options;
    
    public CorridorGenerator(SeedManager seedManager, GenerationOptions options) {
        this.seedManager = seedManager;
        this.options = options;
    }
    
    /**
     * Generate a corridor segment from available frontier positions
     */
    public List<HexCoordinate> generateCorridor(Set<HexCoordinate> frontier, 
                                               Map<String, Hexagon> existingHexagons,
                                               int maxLength) {
        if (frontier.isEmpty()) {
            return new ArrayList<>();
        }
        
        // Choose a random starting point from frontier that connects to existing map
        HexCoordinate startPoint = selectCorridorStartPoint(frontier, existingHexagons);
        if (startPoint == null) {
            return new ArrayList<>();
        }
        
        // Choose corridor width
        int width = seedManager.randomChoice(options.getCorridorWidth());
        
        // Generate corridor path
        List<HexCoordinate> corridorPath = generateCorridorPath(startPoint, existingHexagons, 
                                                               maxLength, width);
        
        logger.debug("Generated corridor with {} hexagons (width: {})", corridorPath.size(), width);
        return corridorPath;
    }
    
    /**
     * Select a starting point for corridor generation
     */
    private HexCoordinate selectCorridorStartPoint(Set<HexCoordinate> frontier, 
                                                  Map<String, Hexagon> existingHexagons) {
        // Filter frontier points that are adjacent to existing hexagons
        List<HexCoordinate> validStarts = new ArrayList<>();
        
        for (HexCoordinate coord : frontier) {
            if (hasAdjacentExistingHexagon(coord, existingHexagons)) {
                validStarts.add(coord);
            }
        }
        
        if (validStarts.isEmpty()) {
            logger.warn("No valid starting points found in frontier");
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
     * Generate the actual corridor path using random walk with some structure
     */
    private List<HexCoordinate> generateCorridorPath(HexCoordinate start, 
                                                    Map<String, Hexagon> existingHexagons,
                                                    int maxLength, int width) {
        List<HexCoordinate> path = new ArrayList<>();
        Set<HexCoordinate> visited = new HashSet<>();
        
        // Start the corridor
        Queue<HexCoordinate> toProcess = new LinkedList<>();
        toProcess.offer(start);
        path.add(start);
        visited.add(start);
        
        int currentLength = 1;
        HexCoordinate lastDirection = null;
        
        while (!toProcess.isEmpty() && currentLength < maxLength) {
            HexCoordinate current = toProcess.poll();
            
            // Get possible next positions
            List<HexCoordinate> nextPositions = getValidNextPositions(current, existingHexagons, visited, lastDirection);
            
            if (nextPositions.isEmpty()) {
                // Dead end, try to branch from another position in the path
                if (path.size() > 1 && seedManager.nextDouble() < 0.3) { // 30% chance to branch
                    HexCoordinate branchPoint = path.get(seedManager.nextInt(Math.max(1, path.size() - 2)));
                    List<HexCoordinate> branchPositions = getValidNextPositions(branchPoint, existingHexagons, visited, null);
                    
                    if (!branchPositions.isEmpty()) {
                        HexCoordinate branchNext = branchPositions.get(seedManager.nextInt(branchPositions.size()));
                        toProcess.offer(branchNext);
                        path.add(branchNext);
                        visited.add(branchNext);
                        currentLength++;
                        lastDirection = calculateDirection(branchPoint, branchNext);
                        continue;
                    }
                }
                break;
            }
            
            // Choose next position (prefer continuing in same direction)
            HexCoordinate next = chooseNextPosition(current, nextPositions, lastDirection);
            
            // Add width to corridor if specified and space allows
            List<HexCoordinate> widthPositions = addCorridorWidth(next, existingHexagons, visited, width);
            
            // Add all positions to path
            toProcess.offer(next);
            path.add(next);
            visited.add(next);
            currentLength++;
            
            for (HexCoordinate widthPos : widthPositions) {
                if (currentLength < maxLength) {
                    path.add(widthPos);
                    visited.add(widthPos);
                    currentLength++;
                }
            }
            
            // Update direction
            lastDirection = calculateDirection(current, next);
            
            // Occasionally change direction to create more interesting corridors
            if (seedManager.nextDouble() < 0.2) { // 20% chance to change direction
                lastDirection = null;
            }
        }
        
        return path;
    }
    
    /**
     * Get valid positions for corridor extension
     */
    private List<HexCoordinate> getValidNextPositions(HexCoordinate current, 
                                                     Map<String, Hexagon> existingHexagons,
                                                     Set<HexCoordinate> visited,
                                                     HexCoordinate preferredDirection) {
        List<HexCoordinate> validPositions = new ArrayList<>();
        
        for (HexCoordinate neighbor : current.getNeighbors()) {
            // Skip if already visited or occupied
            if (visited.contains(neighbor) || existingHexagons.containsKey(neighbor.toId())) {
                continue;
            }
            
            // Skip if it would create too dense a cluster
            if (countAdjacentOccupied(neighbor, existingHexagons, visited) > 2) {
                continue;
            }
            
            validPositions.add(neighbor);
        }
        
        return validPositions;
    }
    
    /**
     * Count adjacent occupied positions
     */
    private int countAdjacentOccupied(HexCoordinate coord, Map<String, Hexagon> existingHexagons, 
                                     Set<HexCoordinate> visited) {
        return (int) coord.getNeighbors().stream()
                         .filter(neighbor -> existingHexagons.containsKey(neighbor.toId()) || 
                                           visited.contains(neighbor))
                         .count();
    }
    
    /**
     * Choose next position, preferring to continue in the same direction
     */
    private HexCoordinate chooseNextPosition(HexCoordinate current, List<HexCoordinate> validPositions, 
                                           HexCoordinate lastDirection) {
        if (validPositions.size() == 1) {
            return validPositions.get(0);
        }
        
        // If we have a preferred direction, try to continue in that direction
        if (lastDirection != null) {
            for (HexCoordinate pos : validPositions) {
                HexCoordinate direction = calculateDirection(current, pos);
                if (isSimilarDirection(lastDirection, direction)) {
                    return pos;
                }
            }
        }
        
        // Otherwise, choose randomly
        return validPositions.get(seedManager.nextInt(validPositions.size()));
    }
    
    /**
     * Calculate direction vector between two coordinates
     */
    private HexCoordinate calculateDirection(HexCoordinate from, HexCoordinate to) {
        return new HexCoordinate(to.getQ() - from.getQ(), to.getR() - from.getR());
    }
    
    /**
     * Check if two directions are similar (for corridor straightness)
     */
    private boolean isSimilarDirection(HexCoordinate dir1, HexCoordinate dir2) {
        // Same direction
        if (dir1.equals(dir2)) {
            return true;
        }
        
        // Allow slight deviation (adjacent directions in hex grid)
        int deltaQ = Math.abs(dir1.getQ() - dir2.getQ());
        int deltaR = Math.abs(dir1.getR() - dir2.getR());
        
        return deltaQ <= 1 && deltaR <= 1;
    }
    
    /**
     * Add width to corridor by including parallel positions
     */
    private List<HexCoordinate> addCorridorWidth(HexCoordinate center, Map<String, Hexagon> existingHexagons,
                                                Set<HexCoordinate> visited, int width) {
        List<HexCoordinate> widthPositions = new ArrayList<>();
        
        if (width <= 1) {
            return widthPositions;
        }
        
        // For width > 1, add adjacent positions
        List<HexCoordinate> neighbors = center.getNeighbors();
        int additionalPositions = Math.min(width - 1, 2); // Limit to reasonable width
        
        List<HexCoordinate> availableNeighbors = new ArrayList<>();
        for (HexCoordinate neighbor : neighbors) {
            if (!visited.contains(neighbor) && !existingHexagons.containsKey(neighbor.toId())) {
                availableNeighbors.add(neighbor);
            }
        }
        
        // Select random adjacent positions for width
        Collections.shuffle(availableNeighbors, seedManager.getRandom());
        for (int i = 0; i < Math.min(additionalPositions, availableNeighbors.size()); i++) {
            widthPositions.add(availableNeighbors.get(i));
        }
        
        return widthPositions;
    }
}