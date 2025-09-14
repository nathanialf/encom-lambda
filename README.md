# ENCOM Lambda - Hexagonal Map Generator

AWS Lambda function for generating connected hexagonal maps using organic growth algorithms.

## Features

- **Deterministic Generation**: Same seed produces identical maps
- **Organic Growth**: 70% corridors, 30% room clusters with natural-looking layouts
- **Connected Maps**: Guarantees all hexagons are reachable (no islands)
- **Configurable**: Customizable corridor ratios, room sizes, and generation options
- **Performance Optimized**: Generates 200 hexagons in <1 second
- **Comprehensive Testing**: 69 unit tests with 100% pass rate

## Architecture

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│  API Gateway    │───▶│  Lambda Handler  │───▶│  Map Generator  │
│  (REST API)     │    │  (Request/Resp)  │    │  (Core Logic)   │
└─────────────────┘    └──────────────────┘    └─────────────────┘
                                                         │
                       ┌─────────────────────────────────┼─────────────────┐
                       ▼                                 ▼                 ▼
              ┌─────────────────┐              ┌─────────────────┐ ┌─────────────────┐
              │ Corridor Gen    │              │   Room Gen      │ │   Validator     │
              │ (Linear paths)  │              │ (Blob clusters) │ │ (Connectivity)  │
              └─────────────────┘              └─────────────────┘ └─────────────────┘
```

## API Usage

**Endpoint**: `POST /api/v1/map/generate`

**Request**:
```json
{
  "seed": "my-map-seed",
  "hexagonCount": 50,
  "options": {
    "corridorRatio": 0.7,
    "roomSizeMin": 4,
    "roomSizeMax": 8,
    "corridorWidth": [1, 2]
  }
}
```

**Response**:
```json
{
  "metadata": {
    "seed": "my-map-seed",
    "hexagonCount": 50,
    "generationTime": 124,
    "statistics": {
      "actualHexagons": 50,
      "corridorHexagons": 35,
      "roomHexagons": 15,
      "averageConnections": 2.4
    }
  },
  "hexagons": [
    {
      "id": "hex_0_0",
      "q": 0,
      "r": 0,
      "connections": ["hex_1_0", "hex_0_1"],
      "type": "corridor"
    }
  ]
}
```

## Development

### Prerequisites
- Java 17
- Gradle

### Building
```bash
# Run tests
gradle test

# Build fat JAR
gradle fatJar

# Create deployment ZIP
gradle buildZip
```

### Testing
- **Unit Tests**: 69 comprehensive tests covering all components
- **Integration Tests**: End-to-end API validation
- **Performance Tests**: Large map generation (200+ hexagons)

### Deployment
The fat JAR (`build/libs/encom-lambda-1.0.0-all.jar`) contains all dependencies and can be deployed directly to AWS Lambda.

**Environment Variables**:
- `DEFAULT_HEXAGON_COUNT`: Default map size (default: 50)
- `MAX_HEXAGON_COUNT`: Maximum allowed size (default: 200)

## Algorithm Details

### Hexagon Coordinate System
Uses **axial coordinates** (q, r) with flat-top hexagon orientation:
- q: Column offset
- r: Row offset  
- s: Calculated as -q-r (cube coordinates)

### Generation Process
1. **Initialization**: Place starting hexagon at origin (0,0)
2. **Frontier Growth**: Maintain frontier set of expandable positions
3. **Structure Selection**: Choose corridor vs room based on ratio
4. **Organic Placement**: Generate connected structures with natural variation
5. **Validation**: Ensure connectivity and structural integrity

### Performance
- **Small Maps** (≤50 hexagons): <100ms
- **Medium Maps** (≤100 hexagons): <500ms  
- **Large Maps** (≤200 hexagons): <1000ms
- **Memory Usage**: <256MB for maximum size maps

## License

MIT License - See LICENSE file for details