package com.encom.mapgen.validator;

import com.encom.mapgen.model.Hexagon;
import com.encom.mapgen.model.HexCoordinate;
import org.junit.Before;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

/**
 * Unit tests for MapValidator
 */
public class MapValidatorTest {
    
    private MapValidator validator;
    
    @Before
    public void setUp() {
        validator = new MapValidator();
    }
    
    @Test
    public void testEmptyMapConnectivity() {
        List<Hexagon> emptyMap = new ArrayList<>();
        assertTrue("Empty map should be considered connected", 
                  validator.validateConnectivity(emptyMap));
    }
    
    @Test
    public void testNullMapConnectivity() {
        assertTrue("Null map should be considered connected", 
                  validator.validateConnectivity(null));
    }
    
    @Test
    public void testSingleHexagonConnectivity() {
        List<Hexagon> map = Arrays.asList(
            createHexagon(0, 0, Hexagon.HexType.CORRIDOR)
        );
        
        assertTrue("Single hexagon should be connected", 
                  validator.validateConnectivity(map));
    }
    
    @Test
    public void testConnectedMapValidation() {
        // Create a connected map: (0,0) -> (1,0) -> (1,-1)
        Hexagon hex1 = createHexagon(0, 0, Hexagon.HexType.CORRIDOR);
        Hexagon hex2 = createHexagon(1, 0, Hexagon.HexType.CORRIDOR);
        Hexagon hex3 = createHexagon(1, -1, Hexagon.HexType.ROOM);
        
        // Connect them
        hex1.addConnection(hex2.getId());
        hex2.addConnection(hex1.getId());
        hex2.addConnection(hex3.getId());
        hex3.addConnection(hex2.getId());
        
        List<Hexagon> map = Arrays.asList(hex1, hex2, hex3);
        
        assertTrue("Connected map should pass validation", 
                  validator.validateConnectivity(map));
    }
    
    @Test
    public void testDisconnectedMapValidation() {
        // Create disconnected map: (0,0) and isolated (2,2)
        Hexagon hex1 = createHexagon(0, 0, Hexagon.HexType.CORRIDOR);
        Hexagon hex2 = createHexagon(2, 2, Hexagon.HexType.ROOM); // No connections
        
        List<Hexagon> map = Arrays.asList(hex1, hex2);
        
        assertFalse("Disconnected map should fail validation", 
                   validator.validateConnectivity(map));
    }
    
    @Test
    public void testBidirectionalConnections() {
        Hexagon hex1 = createHexagon(0, 0, Hexagon.HexType.CORRIDOR);
        Hexagon hex2 = createHexagon(1, 0, Hexagon.HexType.CORRIDOR);
        
        // Create bidirectional connection
        hex1.addConnection(hex2.getId());
        hex2.addConnection(hex1.getId());
        
        List<Hexagon> map = Arrays.asList(hex1, hex2);
        
        assertTrue("Bidirectional connections should pass validation", 
                  validator.validateBidirectionalConnections(map));
    }
    
    @Test
    public void testUnidirectionalConnectionsFail() {
        Hexagon hex1 = createHexagon(0, 0, Hexagon.HexType.CORRIDOR);
        Hexagon hex2 = createHexagon(1, 0, Hexagon.HexType.CORRIDOR);
        
        // Create unidirectional connection (missing reverse)
        hex1.addConnection(hex2.getId());
        // hex2.addConnection(hex1.getId()); // Missing this line
        
        List<Hexagon> map = Arrays.asList(hex1, hex2);
        
        assertFalse("Unidirectional connections should fail validation", 
                   validator.validateBidirectionalConnections(map));
    }
    
    @Test
    public void testConnectionToNonexistentHexagon() {
        Hexagon hex1 = createHexagon(0, 0, Hexagon.HexType.CORRIDOR);
        hex1.addConnection("hex_999_999"); // Nonexistent hexagon
        
        List<Hexagon> map = Arrays.asList(hex1);
        
        assertFalse("Connection to nonexistent hexagon should fail validation", 
                   validator.validateBidirectionalConnections(map));
    }
    
    @Test
    public void testAdjacentConnections() {
        Hexagon hex1 = createHexagon(0, 0, Hexagon.HexType.CORRIDOR);
        Hexagon hex2 = createHexagon(1, 0, Hexagon.HexType.CORRIDOR); // Adjacent
        
        hex1.addConnection(hex2.getId());
        hex2.addConnection(hex1.getId());
        
        List<Hexagon> map = Arrays.asList(hex1, hex2);
        
        assertTrue("Adjacent connections should pass validation", 
                  validator.validateAdjacentConnections(map));
    }
    
    @Test
    public void testNonAdjacentConnectionsFail() {
        Hexagon hex1 = createHexagon(0, 0, Hexagon.HexType.CORRIDOR);
        Hexagon hex2 = createHexagon(2, 2, Hexagon.HexType.CORRIDOR); // Not adjacent
        
        hex1.addConnection(hex2.getId());
        hex2.addConnection(hex1.getId());
        
        List<Hexagon> map = Arrays.asList(hex1, hex2);
        
        assertFalse("Non-adjacent connections should fail validation", 
                   validator.validateAdjacentConnections(map));
    }
    
    @Test
    public void testCorridorRoomRatioValidation() {
        List<Hexagon> map = Arrays.asList(
            createHexagon(0, 0, Hexagon.HexType.CORRIDOR),
            createHexagon(1, 0, Hexagon.HexType.CORRIDOR),
            createHexagon(0, 1, Hexagon.HexType.CORRIDOR),
            createHexagon(-1, 0, Hexagon.HexType.ROOM),
            createHexagon(0, -1, Hexagon.HexType.ROOM)
        );
        
        // 3 corridors, 2 rooms = 60% corridor ratio
        double expectedRatio = 0.6;
        double tolerance = 0.1;
        
        assertTrue("Corridor ratio should be within tolerance", 
                  validator.validateCorridorRoomRatio(map, expectedRatio, tolerance));
        
        assertFalse("Corridor ratio should fail with tight tolerance", 
                   validator.validateCorridorRoomRatio(map, 0.8, 0.05)); // Test with different expected ratio
    }
    
    @Test
    public void testCorridorRoomRatioEmptyMap() {
        List<Hexagon> emptyMap = new ArrayList<>();
        
        assertTrue("Empty map should pass ratio validation", 
                  validator.validateCorridorRoomRatio(emptyMap, 0.5, 0.1));
    }
    
    @Test
    public void testComprehensiveValidation() {
        // Create a valid connected map
        Hexagon hex1 = createHexagon(0, 0, Hexagon.HexType.CORRIDOR);
        Hexagon hex2 = createHexagon(1, 0, Hexagon.HexType.CORRIDOR);
        Hexagon hex3 = createHexagon(0, 1, Hexagon.HexType.ROOM);
        
        // Connect properly
        hex1.addConnection(hex2.getId());
        hex1.addConnection(hex3.getId());
        hex2.addConnection(hex1.getId());
        hex3.addConnection(hex1.getId());
        
        List<Hexagon> map = Arrays.asList(hex1, hex2, hex3);
        
        MapValidator.ValidationResult result = validator.validateMap(map, 0.67); // ~67% corridors
        
        assertTrue("Comprehensive validation should pass", result.isValid);
        assertTrue("Should be connected", result.isConnected);
        assertTrue("Should have bidirectional connections", result.hasBidirectionalConnections);
        assertTrue("Should have valid adjacent connections", result.hasValidAdjacentConnections);
        assertTrue("Should have valid ratio", result.hasValidRatio);
    }
    
    @Test
    public void testComprehensiveValidationFailure() {
        // Create an invalid map (disconnected)
        Hexagon hex1 = createHexagon(0, 0, Hexagon.HexType.CORRIDOR);
        Hexagon hex2 = createHexagon(2, 2, Hexagon.HexType.ROOM); // Isolated
        
        List<Hexagon> map = Arrays.asList(hex1, hex2);
        
        MapValidator.ValidationResult result = validator.validateMap(map, 0.5);
        
        assertFalse("Comprehensive validation should fail", result.isValid);
        assertFalse("Should not be connected", result.isConnected);
    }
    
    @Test
    public void testValidationResultToString() {
        MapValidator.ValidationResult result = new MapValidator.ValidationResult();
        result.isValid = true;
        result.isConnected = true;
        result.hasBidirectionalConnections = false;
        result.hasValidAdjacentConnections = true;
        result.hasValidRatio = true;
        
        String str = result.toString();
        assertNotNull("ToString should not return null", str);
        assertTrue("ToString should contain validation info", str.contains("true"));
        assertTrue("ToString should contain validation info", str.contains("false"));
    }
    
    @Test
    public void testComplexConnectedMap() {
        // Create a more complex connected map (ring structure)
        Hexagon center = createHexagon(0, 0, Hexagon.HexType.ROOM);
        List<Hexagon> neighbors = new ArrayList<>();
        
        // Add all 6 neighbors
        int[][] directions = {{1, 0}, {1, -1}, {0, -1}, {-1, 0}, {-1, 1}, {0, 1}};
        for (int i = 0; i < directions.length; i++) {
            Hexagon neighbor = createHexagon(directions[i][0], directions[i][1], Hexagon.HexType.CORRIDOR);
            neighbors.add(neighbor);
            
            // Connect to center
            center.addConnection(neighbor.getId());
            neighbor.addConnection(center.getId());
        }
        
        // Connect neighbors to each other in a ring
        for (int i = 0; i < neighbors.size(); i++) {
            Hexagon current = neighbors.get(i);
            Hexagon next = neighbors.get((i + 1) % neighbors.size());
            current.addConnection(next.getId());
            next.addConnection(current.getId());
        }
        
        List<Hexagon> map = new ArrayList<>();
        map.add(center);
        map.addAll(neighbors);
        
        assertTrue("Complex connected map should pass validation", 
                  validator.validateConnectivity(map));
        assertTrue("Complex map should have bidirectional connections", 
                  validator.validateBidirectionalConnections(map));
        assertTrue("Complex map should have valid adjacent connections", 
                  validator.validateAdjacentConnections(map));
    }
    
    @Test
    public void testLargeConnectedMap() {
        // Test performance with larger map
        List<Hexagon> map = new ArrayList<>();
        
        // Create a chain of 100 connected hexagons
        Hexagon prev = createHexagon(0, 0, Hexagon.HexType.CORRIDOR);
        map.add(prev);
        
        for (int i = 1; i < 100; i++) {
            Hexagon current = createHexagon(i, 0, Hexagon.HexType.CORRIDOR);
            
            // Connect to previous
            prev.addConnection(current.getId());
            current.addConnection(prev.getId());
            
            map.add(current);
            prev = current;
        }
        
        long startTime = System.currentTimeMillis();
        boolean isConnected = validator.validateConnectivity(map);
        long endTime = System.currentTimeMillis();
        
        assertTrue("Large map should be connected", isConnected);
        assertTrue("Validation should complete quickly (< 1 second)", 
                  (endTime - startTime) < 1000);
    }
    
    private Hexagon createHexagon(int q, int r, Hexagon.HexType type) {
        HexCoordinate coord = new HexCoordinate(q, r);
        return new Hexagon(coord, type);
    }
}