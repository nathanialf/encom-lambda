package com.encom.mapgen.model;

import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

/**
 * Unit tests for HexCoordinate
 */
public class HexCoordinateTest {
    
    @Test
    public void testCoordinateCreation() {
        HexCoordinate coord = new HexCoordinate(3, -2);
        
        assertEquals("Q coordinate should match", 3, coord.getQ());
        assertEquals("R coordinate should match", -2, coord.getR());
        assertEquals("S coordinate should be calculated correctly", -1, coord.getS());
    }
    
    @Test
    public void testCubeCoordinateConstraint() {
        // In cube coordinates, q + r + s should always equal 0
        for (int q = -5; q <= 5; q++) {
            for (int r = -5; r <= 5; r++) {
                HexCoordinate coord = new HexCoordinate(q, r);
                assertEquals("Cube coordinate constraint: q + r + s = 0", 
                           0, coord.getQ() + coord.getR() + coord.getS());
            }
        }
    }
    
    @Test
    public void testNeighbors() {
        HexCoordinate origin = new HexCoordinate(0, 0);
        List<HexCoordinate> neighbors = origin.getNeighbors();
        
        assertEquals("Should have exactly 6 neighbors", 6, neighbors.size());
        
        // Expected neighbors for flat-top hexagon at origin
        HexCoordinate[] expectedNeighbors = {
            new HexCoordinate(1, 0),   // East
            new HexCoordinate(1, -1),  // Northeast
            new HexCoordinate(0, -1),  // Northwest
            new HexCoordinate(-1, 0),  // West
            new HexCoordinate(-1, 1),  // Southwest
            new HexCoordinate(0, 1)    // Southeast
        };
        
        for (HexCoordinate expected : expectedNeighbors) {
            assertTrue("Should contain expected neighbor " + expected, 
                      neighbors.contains(expected));
        }
    }
    
    @Test
    public void testSpecificNeighbor() {
        HexCoordinate origin = new HexCoordinate(0, 0);
        
        assertEquals("Direction 0 should be (1,0)", new HexCoordinate(1, 0), 
                    origin.getNeighbor(0));
        assertEquals("Direction 1 should be (1,-1)", new HexCoordinate(1, -1), 
                    origin.getNeighbor(1));
        assertEquals("Direction 2 should be (0,-1)", new HexCoordinate(0, -1), 
                    origin.getNeighbor(2));
        assertEquals("Direction 3 should be (-1,0)", new HexCoordinate(-1, 0), 
                    origin.getNeighbor(3));
        assertEquals("Direction 4 should be (-1,1)", new HexCoordinate(-1, 1), 
                    origin.getNeighbor(4));
        assertEquals("Direction 5 should be (0,1)", new HexCoordinate(0, 1), 
                    origin.getNeighbor(5));
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidNeighborDirection() {
        HexCoordinate coord = new HexCoordinate(0, 0);
        coord.getNeighbor(6); // Should throw exception
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testNegativeNeighborDirection() {
        HexCoordinate coord = new HexCoordinate(0, 0);
        coord.getNeighbor(-1); // Should throw exception
    }
    
    @Test
    public void testDistance() {
        HexCoordinate origin = new HexCoordinate(0, 0);
        
        // Distance to self should be 0
        assertEquals("Distance to self should be 0", 0, origin.distanceTo(origin));
        
        // Distance to immediate neighbors should be 1
        for (HexCoordinate neighbor : origin.getNeighbors()) {
            assertEquals("Distance to immediate neighbor should be 1", 
                        1, origin.distanceTo(neighbor));
        }
        
        // Test some known distances
        assertEquals("Distance from (0,0) to (2,0) should be 2", 
                    2, origin.distanceTo(new HexCoordinate(2, 0)));
        assertEquals("Distance from (0,0) to (-1,-1) should be 2", 
                    2, origin.distanceTo(new HexCoordinate(-1, -1)));
        assertEquals("Distance from (0,0) to (1,2) should be 3", 
                    3, origin.distanceTo(new HexCoordinate(1, 2)));
    }
    
    @Test
    public void testDistanceSymmetry() {
        HexCoordinate coord1 = new HexCoordinate(2, -1);
        HexCoordinate coord2 = new HexCoordinate(-1, 3);
        
        assertEquals("Distance should be symmetric", 
                    coord1.distanceTo(coord2), coord2.distanceTo(coord1));
    }
    
    @Test
    public void testIdGeneration() {
        HexCoordinate coord = new HexCoordinate(3, -2);
        assertEquals("ID should follow expected format", "hex_3_-2", coord.toId());
        
        HexCoordinate negCoord = new HexCoordinate(-5, 8);
        assertEquals("ID should handle negative coordinates", "hex_-5_8", negCoord.toId());
    }
    
    @Test
    public void testEquality() {
        HexCoordinate coord1 = new HexCoordinate(2, -3);
        HexCoordinate coord2 = new HexCoordinate(2, -3);
        HexCoordinate coord3 = new HexCoordinate(2, -2);
        
        assertEquals("Equal coordinates should be equal", coord1, coord2);
        assertNotEquals("Different coordinates should not be equal", coord1, coord3);
        
        // Test reflexivity
        assertEquals("Coordinate should equal itself", coord1, coord1);
        
        // Test with null
        assertNotEquals("Coordinate should not equal null", coord1, null);
        
        // Test with different type
        assertNotEquals("Coordinate should not equal different type", coord1, "string");
    }
    
    @Test
    public void testHashCode() {
        HexCoordinate coord1 = new HexCoordinate(2, -3);
        HexCoordinate coord2 = new HexCoordinate(2, -3);
        HexCoordinate coord3 = new HexCoordinate(2, -2);
        
        assertEquals("Equal coordinates should have same hash code", 
                    coord1.hashCode(), coord2.hashCode());
        
        // Different coordinates should preferably have different hash codes
        // (not guaranteed, but likely)
        assertNotEquals("Different coordinates should preferably have different hash codes", 
                       coord1.hashCode(), coord3.hashCode());
    }
    
    @Test
    public void testToString() {
        HexCoordinate coord = new HexCoordinate(5, -2);
        String str = coord.toString();
        
        assertNotNull("ToString should not return null", str);
        assertTrue("ToString should contain q value", str.contains("5"));
        assertTrue("ToString should contain r value", str.contains("-2"));
    }
    
    @Test
    public void testNeighborSymmetry() {
        // If B is a neighbor of A, then A should be a neighbor of B
        HexCoordinate center = new HexCoordinate(0, 0);
        
        for (HexCoordinate neighbor : center.getNeighbors()) {
            List<HexCoordinate> neighborsOfNeighbor = neighbor.getNeighbors();
            assertTrue("Neighbor relationship should be symmetric", 
                      neighborsOfNeighbor.contains(center));
        }
    }
}