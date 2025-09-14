package com.encom.mapgen.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents axial coordinates for flat-top hexagons.
 * Uses the q (column) and r (row) coordinate system.
 */
public class HexCoordinate {
    private final int q;
    private final int r;
    
    // Flat-top hexagon neighbor directions
    private static final int[][] DIRECTIONS = {
        {1, 0}, {1, -1}, {0, -1}, {-1, 0}, {-1, 1}, {0, 1}
    };
    
    public HexCoordinate(int q, int r) {
        this.q = q;
        this.r = r;
    }
    
    public int getQ() {
        return q;
    }
    
    public int getR() {
        return r;
    }
    
    public int getS() {
        return -q - r;
    }
    
    /**
     * Get all six neighboring coordinates
     */
    public List<HexCoordinate> getNeighbors() {
        List<HexCoordinate> neighbors = new ArrayList<>();
        for (int[] direction : DIRECTIONS) {
            neighbors.add(new HexCoordinate(q + direction[0], r + direction[1]));
        }
        return neighbors;
    }
    
    /**
     * Get neighbor in specific direction (0-5)
     */
    public HexCoordinate getNeighbor(int direction) {
        if (direction < 0 || direction >= 6) {
            throw new IllegalArgumentException("Direction must be between 0 and 5");
        }
        int[] dir = DIRECTIONS[direction];
        return new HexCoordinate(q + dir[0], r + dir[1]);
    }
    
    /**
     * Calculate distance between two hex coordinates
     */
    public int distanceTo(HexCoordinate other) {
        return (Math.abs(q - other.q) + Math.abs(q + r - other.q - other.r) + Math.abs(r - other.r)) / 2;
    }
    
    /**
     * Generate a unique string ID for this coordinate
     */
    public String toId() {
        return "hex_" + q + "_" + r;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HexCoordinate that = (HexCoordinate) o;
        return q == that.q && r == that.r;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(q, r);
    }
    
    @Override
    public String toString() {
        return "HexCoordinate{q=" + q + ", r=" + r + "}";
    }
}