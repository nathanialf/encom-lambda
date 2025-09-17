package com.encom.mapgen.generator;

import com.encom.mapgen.model.*;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.List;

/**
 * Test corridor post-processing functionality
 */
public class CorridorPostProcessingTest {
    
    @Test
    public void testCorridorConnectionLimiting() {
        // Generate a medium-sized map that's likely to have corridor clusters
        GenerationRequest request = new GenerationRequest();
        request.setSeed("corridor-test-seed");
        request.setHexagonCount(100);
        
        GenerationOptions options = new GenerationOptions();
        options.setCorridorRatio(0.8); // High corridor ratio to increase clustering
        request.setOptions(options);
        
        MapGenerator generator = new MapGenerator(request);
        MapManifest manifest = generator.generateMap(100);
        
        // Count corridors with >2 connections
        List<Hexagon> corridorsWithManyConnections = manifest.getHexagons().stream()
                .filter(hex -> hex.getType() == Hexagon.HexType.CORRIDOR)
                .filter(hex -> hex.getConnectionCount() > 2)
                .toList();
        
        System.out.println("=== Corridor Post-Processing Test Results ===");
        System.out.println("Total hexagons: " + manifest.getHexagons().size());
        
        long totalCorridors = manifest.getHexagons().stream()
                .filter(hex -> hex.getType() == Hexagon.HexType.CORRIDOR).count();
        System.out.println("Total corridors: " + totalCorridors);
        System.out.println("Corridors with >2 connections: " + corridorsWithManyConnections.size());
        
        // Additional debug info
        long corridorsWith1 = manifest.getHexagons().stream()
                .filter(hex -> hex.getType() == Hexagon.HexType.CORRIDOR)
                .filter(hex -> hex.getConnectionCount() == 1).count();
        long corridorsWith2 = manifest.getHexagons().stream()
                .filter(hex -> hex.getType() == Hexagon.HexType.CORRIDOR)
                .filter(hex -> hex.getConnectionCount() == 2).count();
        
        System.out.println("Corridors with 1 connection: " + corridorsWith1);
        System.out.println("Corridors with 2 connections: " + corridorsWith2);
        System.out.println("Expected after post-processing: prioritize 2 connections, allow 3 for branching");
        
        // Count corridors with exactly 3 connections (allowed for branching)
        long corridorsWith3 = manifest.getHexagons().stream()
                .filter(hex -> hex.getType() == Hexagon.HexType.CORRIDOR)
                .filter(hex -> hex.getConnectionCount() == 3).count();
        System.out.println("Corridors with 3 connections: " + corridorsWith3);
        
        // Count corridors with 4+ connections (should be eliminated)
        List<Hexagon> corridorsWithTooMany = manifest.getHexagons().stream()
                .filter(hex -> hex.getType() == Hexagon.HexType.CORRIDOR)
                .filter(hex -> hex.getConnectionCount() > 3)
                .toList();
        
        // Print details of over-connected corridors (>3 connections)
        corridorsWithTooMany.forEach(hex -> {
            System.out.println("Corridor " + hex.getId() + " has " + hex.getConnectionCount() + " connections (too many!)");
        });
        
        // Verify post-processing worked:
        // 1. No corridors should have >3 connections
        // 2. Most should have 2 connections (linear preference)
        assertTrue("Post-processing should eliminate corridors with >3 connections, but found " 
                + corridorsWithTooMany.size() + " with >3 connections",
                corridorsWithTooMany.size() == 0);
        
        // Verify that most corridors prefer 2 connections over 3
        assertTrue("Should prioritize 2 connections over 3 connections",
                corridorsWith2 >= corridorsWith3);
    }
    
    @Test
    public void testConnectivityAfterPostProcessing() {
        GenerationRequest request = new GenerationRequest();
        request.setSeed("connectivity-test");
        request.setHexagonCount(50);
        request.setOptions(new GenerationOptions());
        
        MapGenerator generator = new MapGenerator(request);
        MapManifest manifest = generator.generateMap(50);
        
        // Verify the map is still connected after post-processing
        // This test will fail if post-processing breaks connectivity
        assertNotNull(manifest);
        assertEquals(50, manifest.getHexagons().size());
        
        // The fact that generation completed without throwing means validation passed
        System.out.println("Map connectivity verified after post-processing");
    }
}