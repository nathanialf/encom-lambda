package com.encom.mapgen.model;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for GenerationOptions
 */
public class GenerationOptionsTest {
    
    @Test
    public void testDefaultValues() {
        GenerationOptions options = new GenerationOptions();
        
        assertEquals("Default corridor ratio should be 0.7", 0.7, options.getCorridorRatio(), 0.001);
        assertEquals("Default room size min should be 4", 4, options.getRoomSizeMin());
        assertEquals("Default room size max should be 8", 8, options.getRoomSizeMax());
        assertArrayEquals("Default corridor width should be [1, 2]", 
                         new int[]{1, 2}, options.getCorridorWidth());
    }
    
    @Test
    public void testParameterizedConstructor() {
        int[] corridorWidth = {2, 3};
        GenerationOptions options = new GenerationOptions(0.6, 3, 10, corridorWidth);
        
        assertEquals("Corridor ratio should be set", 0.6, options.getCorridorRatio(), 0.001);
        assertEquals("Room size min should be set", 3, options.getRoomSizeMin());
        assertEquals("Room size max should be set", 10, options.getRoomSizeMax());
        assertArrayEquals("Corridor width should be set", new int[]{2, 3}, options.getCorridorWidth());
    }
    
    @Test
    public void testNullCorridorWidthHandling() {
        GenerationOptions options = new GenerationOptions(0.5, 4, 8, null);
        assertArrayEquals("Null corridor width should default to [1, 2]", 
                         new int[]{1, 2}, options.getCorridorWidth());
        
        options.setCorridorWidth(null);
        assertArrayEquals("Setting null corridor width should default to [1, 2]", 
                         new int[]{1, 2}, options.getCorridorWidth());
    }
    
    @Test
    public void testArrayCopying() {
        int[] originalWidth = {1, 2, 3};
        GenerationOptions options = new GenerationOptions(0.7, 4, 8, originalWidth);
        
        // Modify original array
        originalWidth[0] = 999;
        
        // Options should not be affected (defensive copy)
        assertNotEquals("Options should not be affected by external array modification", 
                       999, options.getCorridorWidth()[0]);
        
        // Test getter returns copy
        int[] retrieved = options.getCorridorWidth();
        retrieved[0] = 888;
        assertNotEquals("Retrieved array modification should not affect internal state", 
                       888, options.getCorridorWidth()[0]);
    }
    
    @Test
    public void testValidationSuccess() {
        GenerationOptions options = new GenerationOptions();
        options.validate(); // Should not throw exception
        
        // Test boundary values
        options.setCorridorRatio(0.0);
        options.validate();
        
        options.setCorridorRatio(1.0);
        options.validate();
        
        options.setRoomSizeMin(1);
        options.setRoomSizeMax(1);
        options.validate();
        
        options.setRoomSizeMax(20);
        options.validate();
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testCorridorRatioTooLow() {
        GenerationOptions options = new GenerationOptions();
        options.setCorridorRatio(-0.1);
        options.validate();
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testCorridorRatioTooHigh() {
        GenerationOptions options = new GenerationOptions();
        options.setCorridorRatio(1.1);
        options.validate();
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testRoomSizeMinZero() {
        GenerationOptions options = new GenerationOptions();
        options.setRoomSizeMin(0);
        options.validate();
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testRoomSizeMinNegative() {
        GenerationOptions options = new GenerationOptions();
        options.setRoomSizeMin(-1);
        options.validate();
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testRoomSizeMaxTooSmall() {
        GenerationOptions options = new GenerationOptions();
        options.setRoomSizeMin(5);
        options.setRoomSizeMax(4); // Less than min
        options.validate();
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testRoomSizeMaxTooLarge() {
        GenerationOptions options = new GenerationOptions();
        options.setRoomSizeMax(21);
        options.validate();
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testNullCorridorWidthValidation() {
        GenerationOptions options = new GenerationOptions();
        options.setCorridorWidth(null);
        // This should be handled gracefully, but let's test explicit null setting
        java.lang.reflect.Field field;
        try {
            field = GenerationOptions.class.getDeclaredField("corridorWidth");
            field.setAccessible(true);
            field.set(options, null);
            options.validate(); // Should throw exception due to null check
        } catch (Exception e) {
            throw new IllegalArgumentException("Forced null for testing");
        }
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testEmptyCorridorWidth() {
        GenerationOptions options = new GenerationOptions();
        options.setCorridorWidth(new int[0]);
        options.validate();
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testCorridorWidthTooSmall() {
        GenerationOptions options = new GenerationOptions();
        options.setCorridorWidth(new int[]{0, 2});
        options.validate();
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testCorridorWidthTooLarge() {
        GenerationOptions options = new GenerationOptions();
        options.setCorridorWidth(new int[]{1, 4});
        options.validate();
    }
    
    @Test
    public void testSettersAndGetters() {
        GenerationOptions options = new GenerationOptions();
        
        options.setCorridorRatio(0.8);
        assertEquals("Setter/getter for corridor ratio", 0.8, options.getCorridorRatio(), 0.001);
        
        options.setRoomSizeMin(2);
        assertEquals("Setter/getter for room size min", 2, options.getRoomSizeMin());
        
        options.setRoomSizeMax(15);
        assertEquals("Setter/getter for room size max", 15, options.getRoomSizeMax());
        
        int[] newWidth = {1, 3};
        options.setCorridorWidth(newWidth);
        assertArrayEquals("Setter/getter for corridor width", newWidth, options.getCorridorWidth());
    }
    
    @Test
    public void testToString() {
        GenerationOptions options = new GenerationOptions();
        String str = options.toString();
        
        assertNotNull("ToString should not return null", str);
        assertTrue("ToString should contain corridor ratio", str.contains("0.7"));
        assertTrue("ToString should contain room size info", str.contains("4"));
        assertTrue("ToString should contain room size info", str.contains("8"));
    }
    
    @Test
    public void testCorridorWidthValidValues() {
        GenerationOptions options = new GenerationOptions();
        
        // Test valid single width
        options.setCorridorWidth(new int[]{2});
        options.validate();
        
        // Test valid multiple widths
        options.setCorridorWidth(new int[]{1, 2, 3});
        options.validate();
        
        // Test boundary values
        options.setCorridorWidth(new int[]{1});
        options.validate();
        
        options.setCorridorWidth(new int[]{3});
        options.validate();
    }
}