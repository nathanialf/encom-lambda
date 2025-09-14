package com.encom.mapgen.generator;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for SeedManager
 */
public class SeedManagerTest {
    
    @Test
    public void testSeedGeneration() {
        SeedManager manager1 = new SeedManager(null);
        SeedManager manager2 = new SeedManager("");
        SeedManager manager3 = new SeedManager("  ");
        
        assertNotNull("Should generate seed when null", manager1.getSeed());
        assertNotNull("Should generate seed when empty", manager2.getSeed());
        assertNotNull("Should generate seed when whitespace", manager3.getSeed());
        
        assertFalse("Generated seeds should not be empty", manager1.getSeed().isEmpty());
        assertFalse("Generated seeds should not be empty", manager2.getSeed().isEmpty());
        assertFalse("Generated seeds should not be empty", manager3.getSeed().isEmpty());
        
        // Generated seeds should be different
        assertNotEquals("Different managers should generate different seeds", 
                       manager1.getSeed(), manager2.getSeed());
    }
    
    @Test
    public void testProvidedSeed() {
        String testSeed = "test123";
        SeedManager manager = new SeedManager(testSeed);
        
        assertEquals("Should use provided seed", testSeed, manager.getSeed());
    }
    
    @Test
    public void testDeterministicRandomness() {
        String seed = "deterministic";
        SeedManager manager1 = new SeedManager(seed);
        SeedManager manager2 = new SeedManager(seed);
        
        // Same seed should produce same random sequence
        for (int i = 0; i < 10; i++) {
            assertEquals("Same seed should produce same random sequence", 
                        manager1.nextInt(100), manager2.nextInt(100));
        }
    }
    
    @Test
    public void testRandomIntRange() {
        SeedManager manager = new SeedManager("range_test");
        
        // Test range validation
        for (int i = 0; i < 100; i++) {
            int value = manager.nextInt(5, 15);
            assertTrue("Random value should be >= min", value >= 5);
            assertTrue("Random value should be < max", value < 15);
        }
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidRange() {
        SeedManager manager = new SeedManager("invalid");
        manager.nextInt(10, 5); // min > max should throw exception
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testEqualRange() {
        SeedManager manager = new SeedManager("equal");
        manager.nextInt(5, 5); // min == max should throw exception
    }
    
    @Test
    public void testRandomChoiceArray() {
        SeedManager manager = new SeedManager("choice");
        String[] options = {"a", "b", "c", "d", "e"};
        
        for (int i = 0; i < 50; i++) {
            String choice = manager.randomChoice(options);
            boolean found = false;
            for (String option : options) {
                if (option.equals(choice)) {
                    found = true;
                    break;
                }
            }
            assertTrue("Choice should be from the provided array", found);
        }
    }
    
    @Test
    public void testRandomChoiceIntArray() {
        SeedManager manager = new SeedManager("int_choice");
        int[] options = {1, 3, 5, 7, 9};
        
        for (int i = 0; i < 50; i++) {
            int choice = manager.randomChoice(options);
            boolean found = false;
            for (int option : options) {
                if (option == choice) {
                    found = true;
                    break;
                }
            }
            assertTrue("Choice should be from the provided array", found);
        }
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testRandomChoiceNullArray() {
        SeedManager manager = new SeedManager("null");
        manager.randomChoice((String[]) null);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testRandomChoiceEmptyArray() {
        SeedManager manager = new SeedManager("empty");
        manager.randomChoice(new String[0]);
    }
    
    @Test
    public void testCorridorRatioProbability() {
        SeedManager manager = new SeedManager("corridor_test");
        double ratio = 0.7;
        int trials = 1000;
        int corridorCount = 0;
        
        for (int i = 0; i < trials; i++) {
            if (manager.shouldGenerateCorridor(ratio)) {
                corridorCount++;
            }
        }
        
        double actualRatio = (double) corridorCount / trials;
        double tolerance = 0.1; // 10% tolerance
        
        assertTrue("Corridor generation should approximate the ratio", 
                  Math.abs(actualRatio - ratio) <= tolerance);
    }
    
    @Test
    public void testBooleanDistribution() {
        SeedManager manager = new SeedManager("boolean_test");
        int trials = 1000;
        int trueCount = 0;
        
        for (int i = 0; i < trials; i++) {
            if (manager.nextBoolean()) {
                trueCount++;
            }
        }
        
        double actualRatio = (double) trueCount / trials;
        // Should be approximately 50%
        assertTrue("Boolean distribution should be roughly 50/50", 
                  Math.abs(actualRatio - 0.5) <= 0.1);
    }
    
    @Test
    public void testDoubleRange() {
        SeedManager manager = new SeedManager("double_test");
        
        for (int i = 0; i < 100; i++) {
            double value = manager.nextDouble();
            assertTrue("Random double should be >= 0.0", value >= 0.0);
            assertTrue("Random double should be < 1.0", value < 1.0);
        }
    }
}