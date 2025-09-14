package com.encom.mapgen.generator;

import com.encom.mapgen.model.*;
import com.encom.mapgen.validator.MapValidator;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for MapGenerator
 */
public class MapGeneratorTest {
    
    private GenerationRequest createTestRequest(String seed, int hexagonCount) {
        GenerationRequest request = new GenerationRequest();
        request.setSeed(seed);
        request.setHexagonCount(hexagonCount);
        request.setOptions(new GenerationOptions());
        return request;
    }
    
    @Test
    public void testDeterministicGeneration() {
        String testSeed = "test123";
        GenerationRequest request = createTestRequest(testSeed, 25);
        
        // Generate map twice with same seed
        MapGenerator generator1 = new MapGenerator(request);
        MapManifest manifest1 = generator1.generateMap(25);
        
        MapGenerator generator2 = new MapGenerator(request);
        MapManifest manifest2 = generator2.generateMap(25);
        
        // Both should have same seed and hexagon count
        assertEquals("Seeds should match", manifest1.getMetadata().getSeed(), 
                    manifest2.getMetadata().getSeed());
        assertEquals("Hexagon counts should match", 
                    manifest1.getMetadata().getStatistics().getActualHexagons(),
                    manifest2.getMetadata().getStatistics().getActualHexagons());
        
        // Both should generate the same map structure
        assertEquals("Maps should have same number of hexagons", 
                    manifest1.getHexagons().size(), manifest2.getHexagons().size());
    }
    
    @Test
    public void testCorrectHexagonCount() {
        GenerationRequest request = createTestRequest("test456", 50);
        MapGenerator generator = new MapGenerator(request);
        MapManifest manifest = generator.generateMap(50);
        
        assertEquals("Generated map should have correct hexagon count", 
                    50, manifest.getHexagons().size());
        assertEquals("Statistics should reflect correct count", 
                    50, manifest.getMetadata().getStatistics().getActualHexagons());
    }
    
    @Test
    public void testConnectivity() {
        GenerationRequest request = createTestRequest("connectivity_test", 30);
        MapGenerator generator = new MapGenerator(request);
        MapManifest manifest = generator.generateMap(30);
        
        MapValidator validator = new MapValidator();
        assertTrue("Generated map should be fully connected", 
                  validator.validateConnectivity(manifest.getHexagons()));
    }
    
    @Test
    public void testCorridorRoomRatio() {
        GenerationRequest request = createTestRequest("ratio_test", 100);
        request.getOptions().setCorridorRatio(0.7);
        
        MapGenerator generator = new MapGenerator(request);
        MapManifest manifest = generator.generateMap(100);
        
        MapManifest.Statistics stats = manifest.getMetadata().getStatistics();
        double actualRatio = (double) stats.getCorridorHexagons() / stats.getActualHexagons();
        
        // Allow 15% tolerance
        assertTrue("Corridor ratio should be approximately 0.7", 
                  Math.abs(actualRatio - 0.7) <= 0.15);
    }
    
    @Test
    public void testMinimumMapSize() {
        GenerationRequest request = createTestRequest("min_test", 1);
        MapGenerator generator = new MapGenerator(request);
        MapManifest manifest = generator.generateMap(1);
        
        assertEquals("Single hexagon map should work", 1, manifest.getHexagons().size());
        assertNotNull("Should have valid metadata", manifest.getMetadata());
        assertNotNull("Should have valid statistics", manifest.getMetadata().getStatistics());
    }
    
    @Test
    public void testLargeMapGeneration() {
        GenerationRequest request = createTestRequest("large_test", 150);
        MapGenerator generator = new MapGenerator(request);
        
        long startTime = System.currentTimeMillis();
        MapManifest manifest = generator.generateMap(150);
        long endTime = System.currentTimeMillis();
        
        assertTrue("Large map generation should complete in reasonable time (< 5 seconds)", 
                  (endTime - startTime) < 5000);
        assertEquals("Should generate requested number of hexagons", 
                    150, manifest.getHexagons().size());
        
        MapValidator validator = new MapValidator();
        assertTrue("Large map should be connected", 
                  validator.validateConnectivity(manifest.getHexagons()));
    }
    
    @Test
    public void testSeedHandling() {
        // Test with null seed - should generate random seed
        GenerationRequest request1 = createTestRequest(null, 20);
        MapGenerator generator1 = new MapGenerator(request1);
        MapManifest manifest1 = generator1.generateMap(20);
        
        assertNotNull("Should generate a seed when null provided", 
                     manifest1.getMetadata().getSeed());
        assertFalse("Generated seed should not be empty", 
                   manifest1.getMetadata().getSeed().isEmpty());
        
        // Test with empty seed - should generate random seed
        GenerationRequest request2 = createTestRequest("", 20);
        MapGenerator generator2 = new MapGenerator(request2);
        MapManifest manifest2 = generator2.generateMap(20);
        
        assertNotNull("Should generate a seed when empty provided", 
                     manifest2.getMetadata().getSeed());
        assertFalse("Generated seed should not be empty", 
                   manifest2.getMetadata().getSeed().isEmpty());
        
        // Generated seeds should be different
        assertNotEquals("Different generators should produce different seeds", 
                       manifest1.getMetadata().getSeed(), 
                       manifest2.getMetadata().getSeed());
    }
    
    @Test
    public void testStatisticsCalculation() {
        GenerationRequest request = createTestRequest("stats_test", 40);
        MapGenerator generator = new MapGenerator(request);
        MapManifest manifest = generator.generateMap(40);
        
        MapManifest.Statistics stats = manifest.getMetadata().getStatistics();
        
        // Basic validations
        assertEquals("Actual hexagons should equal total", 
                    40, stats.getActualHexagons());
        assertEquals("Corridor + room hexagons should equal total", 
                    stats.getActualHexagons(), 
                    stats.getCorridorHexagons() + stats.getRoomHexagons());
        assertTrue("Average connections should be positive", 
                  stats.getAverageConnections() > 0);
        assertTrue("Max connections should be at least 1", 
                  stats.getMaxConnections() >= 1);
        assertTrue("Longest path should be at least 0", 
                  stats.getLongestPath() >= 0);
        
        // Bounding box should be valid
        MapManifest.BoundingBox bbox = stats.getBoundingBox();
        assertNotNull("Bounding box should not be null", bbox);
        assertTrue("MinQ should be <= MaxQ", bbox.getMinQ() <= bbox.getMaxQ());
        assertTrue("MinR should be <= MaxR", bbox.getMinR() <= bbox.getMaxR());
    }
}