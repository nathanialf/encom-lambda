package com.encom.mapgen.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents a single hexagon in the generated map
 */
public class Hexagon {
    private final String id;
    private final int q;
    private final int r;
    private final List<String> connections;
    private final HexType type;
    
    public enum HexType {
        CORRIDOR("corridor"),
        ROOM("room");
        
        private final String value;
        
        HexType(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
        
        @Override
        public String toString() {
            return value;
        }
    }
    
    public Hexagon(HexCoordinate coordinate, HexType type) {
        this.id = coordinate.toId();
        this.q = coordinate.getQ();
        this.r = coordinate.getR();
        this.type = type;
        this.connections = new ArrayList<>();
    }
    
    public Hexagon(String id, int q, int r, List<String> connections, HexType type) {
        this.id = id;
        this.q = q;
        this.r = r;
        this.connections = new ArrayList<>(connections);
        this.type = type;
    }
    
    public String getId() {
        return id;
    }
    
    public int getQ() {
        return q;
    }
    
    public int getR() {
        return r;
    }
    
    public HexCoordinate getCoordinate() {
        return new HexCoordinate(q, r);
    }
    
    public List<String> getConnections() {
        return new ArrayList<>(connections);
    }
    
    public HexType getType() {
        return type;
    }
    
    public void addConnection(String hexagonId) {
        if (!connections.contains(hexagonId)) {
            connections.add(hexagonId);
        }
    }
    
    public void removeConnection(String hexagonId) {
        connections.remove(hexagonId);
    }
    
    public boolean isConnectedTo(String hexagonId) {
        return connections.contains(hexagonId);
    }
    
    public int getConnectionCount() {
        return connections.size();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Hexagon hexagon = (Hexagon) o;
        return Objects.equals(id, hexagon.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "Hexagon{" +
                "id='" + id + '\'' +
                ", q=" + q +
                ", r=" + r +
                ", type=" + type +
                ", connections=" + connections.size() +
                '}';
    }
}