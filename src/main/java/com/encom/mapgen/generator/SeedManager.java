package com.encom.mapgen.generator;

import java.security.SecureRandom;
import java.util.Random;

/**
 * Manages seed generation and provides seeded random number generators
 */
public class SeedManager {
    private final String seed;
    private final Random random;
    
    public SeedManager(String providedSeed) {
        this.seed = providedSeed != null && !providedSeed.trim().isEmpty() 
                   ? providedSeed.trim() 
                   : generateRandomSeed();
        this.random = new Random(this.seed.hashCode());
    }
    
    /**
     * Generate a random seed string
     */
    private static String generateRandomSeed() {
        SecureRandom secureRandom = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        
        // Generate a 10-character alphanumeric seed
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        for (int i = 0; i < 10; i++) {
            sb.append(chars.charAt(secureRandom.nextInt(chars.length())));
        }
        
        return sb.toString();
    }
    
    public String getSeed() {
        return seed;
    }
    
    public Random getRandom() {
        return random;
    }
    
    /**
     * Get a random integer between min (inclusive) and max (exclusive)
     */
    public int nextInt(int min, int max) {
        if (min >= max) {
            throw new IllegalArgumentException("Min must be less than max");
        }
        return random.nextInt(max - min) + min;
    }
    
    /**
     * Get a random integer between 0 (inclusive) and bound (exclusive)
     */
    public int nextInt(int bound) {
        return random.nextInt(bound);
    }
    
    /**
     * Get a random boolean
     */
    public boolean nextBoolean() {
        return random.nextBoolean();
    }
    
    /**
     * Get a random double between 0.0 (inclusive) and 1.0 (exclusive)
     */
    public double nextDouble() {
        return random.nextDouble();
    }
    
    /**
     * Choose randomly between corridor and room generation based on ratio
     */
    public boolean shouldGenerateCorridor(double corridorRatio) {
        return nextDouble() < corridorRatio;
    }
    
    /**
     * Get random array element
     */
    public <T> T randomChoice(T[] array) {
        if (array == null || array.length == 0) {
            throw new IllegalArgumentException("Array cannot be null or empty");
        }
        return array[nextInt(array.length)];
    }
    
    /**
     * Get random int array element
     */
    public int randomChoice(int[] array) {
        if (array == null || array.length == 0) {
            throw new IllegalArgumentException("Array cannot be null or empty");
        }
        return array[nextInt(array.length)];
    }
}