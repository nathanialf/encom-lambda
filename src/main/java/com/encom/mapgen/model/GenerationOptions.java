package com.encom.mapgen.model;

/**
 * Options for customizing map generation
 */
public class GenerationOptions {
    private double corridorRatio = 0.7;
    private int roomSizeMin = 4;
    private int roomSizeMax = 8;
    private int[] corridorWidth = {1, 2};
    
    public GenerationOptions() {}
    
    public GenerationOptions(double corridorRatio, int roomSizeMin, int roomSizeMax, int[] corridorWidth) {
        this.corridorRatio = corridorRatio;
        this.roomSizeMin = roomSizeMin;
        this.roomSizeMax = roomSizeMax;
        this.corridorWidth = corridorWidth != null ? corridorWidth.clone() : new int[]{1, 2};
    }
    
    public double getCorridorRatio() {
        return corridorRatio;
    }
    
    public void setCorridorRatio(double corridorRatio) {
        this.corridorRatio = corridorRatio;
    }
    
    public int getRoomSizeMin() {
        return roomSizeMin;
    }
    
    public void setRoomSizeMin(int roomSizeMin) {
        this.roomSizeMin = roomSizeMin;
    }
    
    public int getRoomSizeMax() {
        return roomSizeMax;
    }
    
    public void setRoomSizeMax(int roomSizeMax) {
        this.roomSizeMax = roomSizeMax;
    }
    
    public int[] getCorridorWidth() {
        return corridorWidth != null ? corridorWidth.clone() : new int[]{1, 2};
    }
    
    public void setCorridorWidth(int[] corridorWidth) {
        this.corridorWidth = corridorWidth != null ? corridorWidth.clone() : new int[]{1, 2};
    }
    
    /**
     * Validate the options
     */
    public void validate() {
        if (corridorRatio < 0.0 || corridorRatio > 1.0) {
            throw new IllegalArgumentException("Corridor ratio must be between 0.0 and 1.0");
        }
        
        if (roomSizeMin < 1 || roomSizeMin > roomSizeMax) {
            throw new IllegalArgumentException("Room size min must be positive and less than or equal to max");
        }
        
        if (roomSizeMax < 1 || roomSizeMax > 20) {
            throw new IllegalArgumentException("Room size max must be between 1 and 20");
        }
        
        if (corridorWidth == null || corridorWidth.length == 0) {
            throw new IllegalArgumentException("Corridor width array cannot be null or empty");
        }
        
        for (int width : corridorWidth) {
            if (width < 1 || width > 3) {
                throw new IllegalArgumentException("Corridor width must be between 1 and 3");
            }
        }
    }
    
    @Override
    public String toString() {
        return "GenerationOptions{" +
                "corridorRatio=" + corridorRatio +
                ", roomSizeMin=" + roomSizeMin +
                ", roomSizeMax=" + roomSizeMax +
                ", corridorWidth=" + java.util.Arrays.toString(corridorWidth) +
                '}';
    }
}