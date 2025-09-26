# ENCOM Lambda - Hexagonal Map Generator

AWS Lambda function for generating connected hexagonal maps using organic growth algorithms. Part of the ENCOM dungeon generation suite for creating immersive, connected hexagonal environments.

## Infrastructure Architecture

### AWS Architecture Overview
```
┌─────────────────────────────────┐  ┌─────────────────────────────────┐
│             DEV                 │  │            PROD                 │
├─────────────────────────────────┤  ├─────────────────────────────────┤
│                                 │  │                                 │
│  ┌─────────────────────┐       │  │  ┌─────────────────────┐       │
│  │  Custom Domain      │       │  │  │  Custom Domain      │       │
│  │encom-api-dev        │       │  │  │ encom-api           │       │
│  │.riperoni.com        │       │  │  │ .riperoni.com       │       │
│  │(Route53 + ACM)      │       │  │  │ (Route53 + ACM)     │       │
│  └─────────────────────┘       │  │  └─────────────────────┘       │
│            │                   │  │            │                   │
│  ┌─────────────────────┐       │  │  ┌─────────────────────┐       │
│  │   API Gateway       │       │  │  │   API Gateway       │       │
│  │ kxt2knsej3 (REST)   │       │  │  │ 3901ff1oz1 (REST)   │◀──────┤
│  │ /api/v1/map/generate│       │  │  │ /api/v1/map/generate│   API │
│  │ No Auth Required    │       │  │  │ API Key Required    │  Key  │
│  └─────────────────────┘       │  │  └─────────────────────┘       │
│            │                   │  │            │                   │
│  ┌─────────────────────┐       │  │  ┌─────────────────────┐       │
│  │   Lambda Function   │       │  │  │   Lambda Function   │       │
│  │encom-map-generator- │       │  │encom-map-generator-   │       │
│  │dev (Java 17)        │       │  │prod (Java 17)         │       │
│  │Alias: live          │       │  │Alias: live            │       │
│  └─────────────────────┘       │  │  └─────────────────────┘       │
│            │                   │  │            │                   │
│  ┌─────────────────────┐       │  │  ┌─────────────────────┐       │
│  │   CloudWatch Logs   │       │  │  │   CloudWatch Logs   │       │
│  │  /aws/lambda/...    │       │  │  │  /aws/lambda/...    │       │
│  │  /aws/apigateway/.. │       │  │  │  /aws/apigateway/.. │       │
│  └─────────────────────┘       │  │  └─────────────────────┘       │
│                                 │  │                                 │
└─────────────────────────────────┘  └─────────────────────────────────┘

         ┌─────────────────────┐              ┌─────────────────────┐
         │   Terraform State   │              │   Terraform State   │
         │dev-encom-lambda-    │              │prod-encom-lambda-   │
         │terraform-state      │              │terraform-state      │
         │    (S3 Bucket)      │              │    (S3 Bucket)      │
         └─────────────────────┘              └─────────────────────┘
```

### Application Architecture
```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│  API Gateway    │───▶│  Lambda Handler  │───▶│  Map Generator  │
│  (REST API)     │    │  (Request/Resp)  │    │  (Core Logic)   │
└─────────────────┘    └──────────────────┘    └─────────────────┘
                                                        │
                       ┌─────────────────────────────────┼──────────────────────┐
                       ▼                                 ▼                      ▼
              ┌─────────────────┐              ┌─────────────────┐    ┌─────────────────┐
              │ Corridor Gen    │              │   Room Gen      │    │ Post-Processing │
              │ (Linear paths)  │              │ (Blob clusters) │    │ (Linear Enforce)│
              └─────────────────┘              └─────────────────┘    └─────────────────┘
                                                                               │
                                                                               ▼
                                                                    ┌─────────────────┐
                                                                    │   Validator     │
                                                                    │ (Connectivity)  │
                                                                    └─────────────────┘
```

## API Endpoints

### Development Environment
- **Custom Domain**: `https://encom-api-dev.riperoni.com/api/v1/map/generate`
- **Direct Endpoint**: `https://kxt2knsej3.execute-api.us-west-1.amazonaws.com/dev/api/v1/map/generate`
- **Authentication**: None required
- **CORS**: Enabled for all origins
- **Rate Limiting**: Basic throttling
- **SSL Certificate**: Auto-managed ACM certificate with DNS validation

### Production Environment  
- **Custom Domain**: `https://encom-api.riperoni.com/api/v1/map/generate`
- **Direct Endpoint**: `https://3901ff1oz1.execute-api.us-west-1.amazonaws.com/prod/api/v1/map/generate`
- **Authentication**: API Key required (`x-api-key` header)
- **CORS**: Enabled for all origins
- **Rate Limiting**: 100 requests/second, 10,000 requests/month per key
- **SSL Certificate**: Auto-managed ACM certificate with DNS validation

## Features

- **Deterministic Generation**: Same seed produces identical maps
- **Organic Growth**: 70% corridors, 30% room clusters with natural-looking layouts
- **Connected Maps**: Guarantees all hexagons are reachable (no islands)
- **Configurable**: Customizable corridor ratios, room sizes, and generation options
- **Performance Optimized**: Generates 200 hexagons in <1 second
- **Corridor Post-Processing**: Connectivity-preserving algorithm prioritizes 2 connections, allows 3 for branching
- **Comprehensive Testing**: 71 unit tests with 100% pass rate
- **Independent Infrastructure**: Self-contained terraform modules for each environment

## API Usage

### Request Format
**Method**: `POST`  
**Path**: `/api/v1/map/generate`  
**Content-Type**: `application/json`

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

### Response Format
```json
{
  "metadata": {
    "seed": "my-map-seed",
    "hexagonCount": 50,
    "generatedAt": "2025-09-26T01:09:44.656413594Z",
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

### Example Usage

#### Development (No Authentication)
```bash
curl -X POST \
  "https://encom-api-dev.riperoni.com/api/v1/map/generate" \
  -H "Content-Type: application/json" \
  -d '{"seed": "test123", "hexagonCount": 10}'
```

#### Production (API Key Required)
```bash
curl -X POST \
  "https://encom-api.riperoni.com/api/v1/map/generate" \
  -H "Content-Type: application/json" \
  -H "x-api-key: YOUR_API_KEY_HERE" \
  -d '{"seed": "prod123", "hexagonCount": 10}'
```

## Infrastructure Management

### Terraform Structure
```
terraform/
├── bootstrap/              # State bucket creation
├── modules/
│   ├── lambda/             # Lambda function module
│   ├── api-gateway/        # API Gateway with custom domain support
│   └── route53/            # DNS hosted zone management
└── environments/
    ├── dev/                # Development environment
    └── prod/               # Production environment
```

### Deployment Pipeline
The project uses Jenkins for CI/CD with three main actions:

1. **Bootstrap**: Create S3 state bucket for terraform
2. **Plan**: Run terraform plan to preview changes
3. **Apply**: Apply terraform changes to deploy infrastructure

### Environment Configuration

#### Development
- **State Bucket**: `dev-encom-lambda-terraform-state`
- **Lambda Memory**: 512MB
- **Timeout**: 30 seconds
- **Log Retention**: 7 days
- **Authentication**: Disabled

#### Production  
- **State Bucket**: `prod-encom-lambda-terraform-state`
- **Lambda Memory**: 1024MB
- **Timeout**: 60 seconds
- **Log Retention**: 30 days
- **Authentication**: API Key required
- **Rate Limiting**: Enabled

## Development

### Prerequisites
- Java 17
- Gradle
- AWS CLI (configured with `encom-dev` and `encom-prod` profiles)
- Terraform 1.5+

### Local Development
```bash
# Clone the repository
git clone <repository-url>
cd encom-lambda

# Run tests
./gradlew test

# Build fat JAR
./gradlew fatJar

# The JAR will be in build/libs/encom-lambda-1.0.0-all.jar
```

### Testing
- **Unit Tests**: 71 comprehensive tests covering all components
- **Integration Tests**: End-to-end API validation
- **Performance Tests**: Large map generation (200+ hexagons)
- **Post-Processing Tests**: Connectivity-preserving corridor optimization

### Infrastructure Deployment
```bash
# Bootstrap state bucket (first time only)
cd terraform/environments/dev
terraform init
# Configure AWS_PROFILE=encom-dev
terraform apply -var="environment=dev"

# Deploy infrastructure changes
terraform plan    # Review changes
terraform apply   # Deploy changes
```

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
5. **Post-Processing**: Connectivity-preserving corridor optimization (prioritize 2 connections, allow 3 for branching)
6. **Validation**: Ensure connectivity and structural integrity

### Performance Characteristics
- **Small Maps** (≤50 hexagons): <100ms
- **Medium Maps** (≤100 hexagons): <500ms  
- **Large Maps** (≤200 hexagons): <1000ms
- **Memory Usage**: <256MB for maximum size maps

## Security

- **Production API Keys**: Required for all production API calls
- **CORS**: Properly configured for cross-origin requests
- **IAM Roles**: Minimal permissions following principle of least privilege
- **VPC**: Not required - Lambda function is stateless
- **Encryption**: All data encrypted in transit (HTTPS) and at rest (S3)

## Monitoring & Logging

### CloudWatch Logs
- **Lambda Logs**: `/aws/lambda/encom-map-generator-{env}`
- **API Gateway Logs**: `/aws/apigateway/encom-api-{env}`
- **Log Retention**: 7 days (dev), 30 days (prod)

### Metrics & Alarms
- Lambda duration, errors, throttles
- API Gateway request count, latency, errors
- Custom metrics for map generation statistics

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for new functionality
5. Ensure all tests pass
6. Submit a pull request

---

**Part of the ENCOM Project**: This service provides hexagonal map generation for the broader ENCOM dungeon creation suite, including frontend visualization and additional dungeon services.