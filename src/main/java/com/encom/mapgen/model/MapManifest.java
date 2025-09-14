package com.encom.mapgen.model;

import java.time.Instant;
import java.util.List;

/**
 * Complete response structure for generated maps
 */
public class MapManifest {
    private Metadata metadata;
    private List<Hexagon> hexagons;
    
    public MapManifest() {}
    
    public MapManifest(Metadata metadata, List<Hexagon> hexagons) {
        this.metadata = metadata;
        this.hexagons = hexagons;
    }
    
    public Metadata getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Metadata metadata) {
        this.metadata = metadata;
    }
    
    public List<Hexagon> getHexagons() {
        return hexagons;
    }
    
    public void setHexagons(List<Hexagon> hexagons) {
        this.hexagons = hexagons;
    }
    
    /**
     * Metadata about the generated map
     */
    public static class Metadata {
        private String seed;
        private int hexagonCount;
        private String generatedAt;
        private String version;
        private boolean cached;
        private long generationTime;
        private Statistics statistics;
        
        public Metadata() {
            this.version = "1.0.0";
            this.cached = false;
            this.generatedAt = Instant.now().toString();
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
        
        public String getGeneratedAt() {
            return generatedAt;
        }
        
        public void setGeneratedAt(String generatedAt) {
            this.generatedAt = generatedAt;
        }
        
        public String getVersion() {
            return version;
        }
        
        public void setVersion(String version) {
            this.version = version;
        }
        
        public boolean isCached() {
            return cached;
        }
        
        public void setCached(boolean cached) {
            this.cached = cached;
        }
        
        public long getGenerationTime() {
            return generationTime;
        }
        
        public void setGenerationTime(long generationTime) {
            this.generationTime = generationTime;
        }
        
        public Statistics getStatistics() {
            return statistics;
        }
        
        public void setStatistics(Statistics statistics) {
            this.statistics = statistics;
        }
    }
    
    /**
     * Statistics about the generated map
     */
    public static class Statistics {
        private int actualHexagons;
        private int corridorHexagons;
        private int roomHexagons;
        private double averageConnections;
        private int maxConnections;
        private int longestPath;
        private BoundingBox boundingBox;
        
        public Statistics() {}
        
        public int getActualHexagons() {
            return actualHexagons;
        }
        
        public void setActualHexagons(int actualHexagons) {
            this.actualHexagons = actualHexagons;
        }
        
        public int getCorridorHexagons() {
            return corridorHexagons;
        }
        
        public void setCorridorHexagons(int corridorHexagons) {
            this.corridorHexagons = corridorHexagons;
        }
        
        public int getRoomHexagons() {
            return roomHexagons;
        }
        
        public void setRoomHexagons(int roomHexagons) {
            this.roomHexagons = roomHexagons;
        }
        
        public double getAverageConnections() {
            return averageConnections;
        }
        
        public void setAverageConnections(double averageConnections) {
            this.averageConnections = averageConnections;
        }
        
        public int getMaxConnections() {
            return maxConnections;
        }
        
        public void setMaxConnections(int maxConnections) {
            this.maxConnections = maxConnections;
        }
        
        public int getLongestPath() {
            return longestPath;
        }
        
        public void setLongestPath(int longestPath) {
            this.longestPath = longestPath;
        }
        
        public BoundingBox getBoundingBox() {
            return boundingBox;
        }
        
        public void setBoundingBox(BoundingBox boundingBox) {
            this.boundingBox = boundingBox;
        }
    }
    
    /**
     * Bounding box of the generated map
     */
    public static class BoundingBox {
        private int minQ;
        private int maxQ;
        private int minR;
        private int maxR;
        
        public BoundingBox() {}
        
        public BoundingBox(int minQ, int maxQ, int minR, int maxR) {
            this.minQ = minQ;
            this.maxQ = maxQ;
            this.minR = minR;
            this.maxR = maxR;
        }
        
        public int getMinQ() {
            return minQ;
        }
        
        public void setMinQ(int minQ) {
            this.minQ = minQ;
        }
        
        public int getMaxQ() {
            return maxQ;
        }
        
        public void setMaxQ(int maxQ) {
            this.maxQ = maxQ;
        }
        
        public int getMinR() {
            return minR;
        }
        
        public void setMinR(int minR) {
            this.minR = minR;
        }
        
        public int getMaxR() {
            return maxR;
        }
        
        public void setMaxR(int maxR) {
            this.maxR = maxR;
        }
    }
}