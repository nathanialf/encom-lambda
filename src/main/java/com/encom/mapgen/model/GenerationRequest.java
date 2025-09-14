package com.encom.mapgen.model;

/**
 * Request model for map generation
 */
public class GenerationRequest {
    private String seed;
    private int hexagonCount;
    private GenerationOptions options;
    
    public GenerationRequest() {
        this.hexagonCount = 50; // Default
        this.options = new GenerationOptions();
    }
    
    public GenerationRequest(String seed, int hexagonCount, GenerationOptions options) {
        this.seed = seed;
        this.hexagonCount = hexagonCount;
        this.options = options != null ? options : new GenerationOptions();
    }
    
    public String getSeed() {
        return seed;
    }
    
    public void setSeed(String seed) {
        this.seed = seed;
    }
    
    public int getHexagonCount() {
        return hexagonCount;
    }
    
    public void setHexagonCount(int hexagonCount) {
        this.hexagonCount = hexagonCount;
    }
    
    public GenerationOptions getOptions() {
        return options;
    }
    
    public void setOptions(GenerationOptions options) {
        this.options = options;
    }
    
    /**
     * Validate the request parameters
     */
    public void validate() {
        if (hexagonCount < 1 || hexagonCount > 200) {
            throw new IllegalArgumentException("Hexagon count must be between 1 and 200");
        }
        
        if (options != null) {
            options.validate();
        }
    }
    
    @Override
    public String toString() {
        return "GenerationRequest{" +
                "seed='" + seed + '\'' +
                ", hexagonCount=" + hexagonCount +
                ", options=" + options +
                '}';
    }
}