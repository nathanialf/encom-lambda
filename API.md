# ENCOM Lambda API Documentation

Complete API reference for the ENCOM Hexagonal Map Generator service.

## Overview

The ENCOM Lambda API generates connected hexagonal dungeon maps using organic growth algorithms. Maps are deterministic (same seed produces identical results) and guarantee full connectivity between all hexagons.

## Base URLs

### Development Environment
- **Base URL**: `https://kxt2knsej3.execute-api.us-west-1.amazonaws.com/dev`
- **Authentication**: None required
- **Rate Limiting**: Basic throttling

### Production Environment
- **Base URL**: `https://3901ff1oz1.execute-api.us-west-1.amazonaws.com/prod`
- **Authentication**: API Key required
- **Rate Limiting**: 100 req/sec, 10,000 req/month per key

## Authentication

### Development
No authentication required for development environment.

### Production
Production requires an API key passed in the `x-api-key` header:

```http
x-api-key: YOUR_API_KEY_HERE
```

## Endpoints

### Generate Hexagonal Map

Generates a connected hexagonal dungeon map with configurable parameters.

**Endpoint**: `POST /api/v1/map/generate`

#### Request Headers
```http
Content-Type: application/json
x-api-key: YOUR_API_KEY_HERE  # Production only
```

#### Request Body

```json
{
  "seed": "string",           // Optional: Deterministic seed
  "hexagonCount": "integer",  // Required: Number of hexagons (1-1000)
  "options": {                // Optional: Generation parameters
    "corridorRatio": "float",      // Optional: Corridor vs room ratio (0.0-1.0)
    "roomSizeMin": "integer",      // Optional: Minimum room size (1-10)
    "roomSizeMax": "integer",      // Optional: Maximum room size (roomSizeMin-20)
    "corridorWidth": ["integer"]   // Optional: Corridor width options [1,2,3]
  }
}
```

#### Request Parameters

| Parameter | Type | Required | Default | Range | Description |
|-----------|------|----------|---------|-------|-------------|
| `seed` | string | No | Random | Any | Deterministic seed for map generation |
| `hexagonCount` | integer | Yes | - | 1-1000 | Number of hexagons to generate |
| `options.corridorRatio` | float | No | 0.7 | 0.0-1.0 | Ratio of corridors to rooms (0.7 = 70% corridors) |
| `options.roomSizeMin` | integer | No | 4 | 1-10 | Minimum hexagons per room |
| `options.roomSizeMax` | integer | No | 8 | roomSizeMin-20 | Maximum hexagons per room |
| `options.corridorWidth` | array | No | [1,2] | [1], [2], [3], [1,2], [1,3], [2,3], [1,2,3] | Available corridor widths |

#### Response Format

```json
{
  "metadata": {
    "seed": "string",
    "hexagonCount": "integer",
    "generatedAt": "string",     // ISO 8601 timestamp
    "generationTime": "integer", // Generation time in milliseconds
    "statistics": {
      "actualHexagons": "integer",      // Final hexagon count
      "corridorHexagons": "integer",    // Number of corridor hexagons
      "roomHexagons": "integer",        // Number of room hexagons
      "averageConnections": "float"     // Average connections per hexagon
    }
  },
  "hexagons": [
    {
      "id": "string",           // Unique hexagon identifier
      "q": "integer",           // Axial coordinate Q
      "r": "integer",           // Axial coordinate R
      "connections": ["string"], // Array of connected hexagon IDs
      "type": "string"          // "corridor" or "room"
    }
  ]
}
```

#### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `metadata.seed` | string | Seed used for generation (provided or generated) |
| `metadata.hexagonCount` | integer | Requested hexagon count |
| `metadata.generatedAt` | string | Generation timestamp in ISO 8601 format |
| `metadata.generationTime` | integer | Time taken to generate map in milliseconds |
| `metadata.statistics.actualHexagons` | integer | Final number of hexagons created |
| `metadata.statistics.corridorHexagons` | integer | Number of corridor type hexagons |
| `metadata.statistics.roomHexagons` | integer | Number of room type hexagons |
| `metadata.statistics.averageConnections` | float | Average connections per hexagon |
| `hexagons[].id` | string | Unique identifier (format: "hex_{q}_{r}") |
| `hexagons[].q` | integer | Axial coordinate Q (column offset) |
| `hexagons[].r` | integer | Axial coordinate R (row offset) |
| `hexagons[].connections` | array | IDs of connected neighboring hexagons |
| `hexagons[].type` | string | Hexagon type: "corridor" or "room" |

## Example Requests

### Basic Request (Development)

```bash
curl -X POST \
  "https://kxt2knsej3.execute-api.us-west-1.amazonaws.com/dev/api/v1/map/generate" \
  -H "Content-Type: application/json" \
  -d '{
    "hexagonCount": 25,
    "seed": "example-map"
  }'
```

### Advanced Request (Production)

```bash
curl -X POST \
  "https://3901ff1oz1.execute-api.us-west-1.amazonaws.com/prod/api/v1/map/generate" \
  -H "Content-Type: application/json" \
  -H "x-api-key: YOUR_API_KEY_HERE" \
  -d '{
    "hexagonCount": 100,
    "seed": "dungeon-level-1",
    "options": {
      "corridorRatio": 0.8,
      "roomSizeMin": 3,
      "roomSizeMax": 12,
      "corridorWidth": [1, 2]
    }
  }'
```

### Minimal Request

```json
{
  "hexagonCount": 10
}
```

### Complex Configuration

```json
{
  "hexagonCount": 150,
  "seed": "complex-dungeon-42",
  "options": {
    "corridorRatio": 0.75,
    "roomSizeMin": 5,
    "roomSizeMax": 15,
    "corridorWidth": [1, 2, 3]
  }
}
```

## Example Response

```json
{
  "metadata": {
    "seed": "example-map",
    "hexagonCount": 25,
    "generatedAt": "2025-09-26T01:09:44.656413594Z",
    "generationTime": 87,
    "statistics": {
      "actualHexagons": 25,
      "corridorHexagons": 18,
      "roomHexagons": 7,
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
    },
    {
      "id": "hex_1_0",
      "q": 1,
      "r": 0,
      "connections": ["hex_0_0", "hex_2_0", "hex_1_-1"],
      "type": "corridor"
    },
    {
      "id": "hex_0_1",
      "q": 0,
      "r": 1,
      "connections": ["hex_0_0", "hex_-1_2"],
      "type": "room"
    }
  ]
}
```

## Error Responses

### Validation Errors

**Status Code**: `400 Bad Request`

```json
{
  "message": "Invalid hexagon count. Must be between 1 and 200"
}
```

Common validation errors:
- `"Missing required field: hexagonCount"`
- `"Invalid hexagon count. Must be between 1 and 200"`
- `"Invalid corridor ratio. Must be between 0.0 and 1.0"`
- `"Invalid room size. roomSizeMin must be <= roomSizeMax"`

### Authentication Errors

**Status Code**: `401 Unauthorized` (Production only)

```json
{
  "message": "Unauthorized"
}
```

### Rate Limiting

**Status Code**: `429 Too Many Requests`

```json
{
  "message": "Too Many Requests"
}
```

### Server Errors

**Status Code**: `500 Internal Server Error`

```json
{
  "message": "Internal server error"
}
```

## Hexagon Coordinate System

The API uses **axial coordinates** for hexagon positioning:

- **Q**: Column offset (horizontal axis)
- **R**: Row offset (diagonal axis)
- **S**: Calculated as `-q-r` (implied third coordinate)

### Coordinate Examples
```
     (0,-1)   (1,-1)
(-1, 0)   (0, 0)   (1, 0)
     (0,1)    (1,1)
```

### Neighboring Hexagons
Each hexagon has exactly 6 neighbors at coordinates:
- `(q+1, r)`, `(q-1, r)` - East/West neighbors
- `(q, r+1)`, `(q, r-1)` - Southeast/Northwest neighbors  
- `(q+1, r-1)`, `(q-1, r+1)` - Northeast/Southwest neighbors

## Algorithm Details

### Generation Process

1. **Initialization**: Start at origin `(0,0)`
2. **Frontier Expansion**: Grow outward maintaining connection frontier
3. **Structure Decision**: Choose corridor vs room based on `corridorRatio`
4. **Organic Growth**: Create connected structures with natural variation
5. **Post-Processing**: Optimize connections (prefer 2 connections, allow 3 for branching)
6. **Validation**: Ensure all hexagons are reachable

### Map Characteristics

- **Connectivity**: All hexagons guaranteed reachable from any other hexagon
- **Organic Layout**: Natural-looking dungeon structures 
- **Deterministic**: Same seed always produces identical maps
- **Balanced**: Configurable corridor-to-room ratios
- **Optimized**: Post-processed for clean corridor layouts

## Performance Guidelines

### Response Times
- **Small maps** (≤50 hexagons): <100ms
- **Medium maps** (≤100 hexagons): <500ms
- **Large maps** (≤200 hexagons): <1000ms

### Best Practices

1. **Use appropriate hexagon counts** for your use case
2. **Cache results** when using the same seed repeatedly
3. **Implement retry logic** for transient errors
4. **Respect rate limits** in production
5. **Validate input parameters** before sending requests

## Rate Limits

### Development Environment
- Basic throttling applied
- No strict limits for development and testing

### Production Environment
- **Rate Limit**: 100 requests per second per API key
- **Quota Limit**: 10,000 requests per month per API key
- **Burst Allowance**: 200 requests

Rate limit headers included in responses:
```http
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 99
X-RateLimit-Reset: 1640995200
```

## SDK Examples

### JavaScript/Node.js

```javascript
const generateMap = async (hexagonCount, seed, options = {}) => {
  const response = await fetch('https://3901ff1oz1.execute-api.us-west-1.amazonaws.com/prod/api/v1/map/generate', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'x-api-key': 'YOUR_API_KEY_HERE'
    },
    body: JSON.stringify({
      hexagonCount,
      seed,
      options
    })
  });
  
  if (!response.ok) {
    throw new Error(`HTTP ${response.status}: ${await response.text()}`);
  }
  
  return await response.json();
};

// Usage
const map = await generateMap(50, 'my-dungeon', {
  corridorRatio: 0.7,
  roomSizeMin: 4,
  roomSizeMax: 10
});
```

### Python

```python
import requests

def generate_map(hexagon_count, seed=None, options=None, api_key=None):
    url = "https://3901ff1oz1.execute-api.us-west-1.amazonaws.com/prod/api/v1/map/generate"
    
    headers = {"Content-Type": "application/json"}
    if api_key:
        headers["x-api-key"] = api_key
    
    payload = {"hexagonCount": hexagon_count}
    if seed:
        payload["seed"] = seed
    if options:
        payload["options"] = options
    
    response = requests.post(url, json=payload, headers=headers)
    response.raise_for_status()
    
    return response.json()

# Usage
map_data = generate_map(
    hexagon_count=75,
    seed="python-generated",
    options={
        "corridorRatio": 0.8,
        "roomSizeMin": 3,
        "roomSizeMax": 12
    },
    api_key="YOUR_API_KEY_HERE"
)
```

### Java

```java
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import com.google.gson.Gson;

public class EncomMapGenerator {
    private final String baseUrl;
    private final String apiKey;
    private final HttpClient client;
    private final Gson gson;
    
    public EncomMapGenerator(String baseUrl, String apiKey) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.client = HttpClient.newHttpClient();
        this.gson = new Gson();
    }
    
    public MapResponse generateMap(int hexagonCount, String seed, MapOptions options) 
            throws Exception {
        
        MapRequest request = new MapRequest(hexagonCount, seed, options);
        String json = gson.toJson(request);
        
        HttpRequest httpRequest = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/api/v1/map/generate"))
            .header("Content-Type", "application/json")
            .header("x-api-key", apiKey)
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .build();
            
        HttpResponse<String> response = client.send(
            httpRequest, 
            HttpResponse.BodyHandlers.ofString()
        );
        
        if (response.statusCode() != 200) {
            throw new RuntimeException("HTTP " + response.statusCode() + ": " + response.body());
        }
        
        return gson.fromJson(response.body(), MapResponse.class);
    }
}
```

## Changelog

### Version 1.0.0
- Initial API release
- Hexagonal map generation with organic growth
- Development and production environments
- API key authentication for production
- Rate limiting and quota management
- Comprehensive error handling

## Support

For API support, issues, or feature requests:

1. Check this documentation for usage guidelines
2. Review error messages for specific validation issues
3. Ensure API key is valid and has remaining quota (production)
4. Verify request format matches the documented schema
5. Report persistent issues through the appropriate channels

---

**API Version**: 1.0.0  
**Last Updated**: September 2025  
**Service Status**: [Service Status Page]