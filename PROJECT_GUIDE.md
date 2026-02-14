# EComVerse - Full-Stack E-Commerce Platform

A production-ready e-commerce backend built with Spring Boot 3.5.3 and Java 17.

---

## Table of Contents

1. [Features](#features)
2. [Tech Stack](#tech-stack)
3. [Project Structure](#project-structure)
4. [Prerequisites](#prerequisites)
5. [Running the Application](#running-the-application)
6. [API Documentation](#api-documentation)
7. [Authentication](#authentication)
8. [Database Schema](#database-schema)
9. [Testing](#testing)
10. [Docker Deployment](#docker-deployment)

---

## Features

### Core E-Commerce
- Product management with categories
- Shopping cart functionality
- Order placement and tracking
- Address management
- Payment integration

### Security
- JWT authentication with refresh token rotation
- Role-based access control (USER, SELLER, ADMIN)
- Password reset with email verification
- Rate limiting (20/100/500 requests per minute based on role)

### Advanced Features
- Product reviews and ratings
- Wishlist with price tracking
- Order status tracking with history
- Email notifications (async)
- Audit logging (AOP-based)

### Performance
- Redis caching with configurable TTL
- Async processing for non-blocking operations
- Optimized database queries with pagination

---

## Tech Stack

| Category | Technology |
|----------|------------|
| Framework | Spring Boot 3.5.3 |
| Language | Java 17 |
| Database | MySQL 8.0 |
| Caching | Redis |
| Security | Spring Security + JWT |
| Documentation | Swagger/OpenAPI 3 |
| Build Tool | Maven |
| Containerization | Docker |
| CI/CD | GitHub Actions |
| Testing | JUnit 5, Mockito, TestContainers |

---

## Project Structure

```
ecomverse/
├── src/main/java/com/ecommerce/project/
│   ├── annotation/          # Custom annotations (@Auditable)
│   ├── aspect/              # AOP aspects (AuditAspect)
│   ├── config/              # Configuration classes
│   │   ├── AppConfig.java
│   │   ├── AsyncConfig.java
│   │   ├── RateLimitConfig.java
│   │   ├── RedisConfig.java
│   │   └── SwaggerConfig.java
│   ├── controller/          # REST controllers
│   │   ├── AuthController.java
│   │   ├── ProductController.java
│   │   ├── CategoryController.java
│   │   ├── CartController.java
│   │   ├── OrderController.java
│   │   ├── AddressController.java
│   │   ├── ReviewController.java
│   │   ├── WishlistController.java
│   │   └── PasswordController.java
│   ├── exceptions/          # Custom exceptions
│   ├── filter/              # Servlet filters (RateLimitFilter)
│   ├── model/               # JPA entities
│   ├── payload/             # DTOs
│   ├── repositories/        # Spring Data JPA repositories
│   ├── security/            # Security configuration
│   ├── service/             # Business logic
│   └── util/                # Utility classes
├── src/main/resources/
│   ├── application.properties
│   ├── application-dev.properties
│   ├── application-prod.properties
│   └── application-docker.properties
├── src/test/                # Test classes
├── Dockerfile
├── docker-compose.yml
└── pom.xml
```

---

## Prerequisites

### For Local Development
- Java 17 or higher
- Maven 3.8+
- MySQL 8.0
- Redis (optional, for caching)

### For Docker
- Docker Desktop
- Docker Compose

### Check Installation

```bash
java -version    # Should be 17+
mvn -version     # Should be 3.8+
docker --version # For Docker deployment
```

---

## Running the Application

### Option 1: Docker (Recommended)

```bash
# Clone the repository
git clone https://github.com/salujaavaneetkaur-a11y/ecomverse.git
cd ecomverse

# Start all services
docker-compose up

# Stop services
docker-compose down
```

**Services Started:**
| Service | URL |
|---------|-----|
| Application | http://localhost:8080 |
| MySQL | localhost:3306 |
| Redis | localhost:6379 |
| Adminer (DB UI) | http://localhost:8081 |

### Option 2: Local Development

**Step 1: Setup MySQL**

```sql
-- Connect to MySQL
mysql -u root -p

-- Create database
CREATE DATABASE sb_ecom;
CREATE USER 'ecom_user'@'localhost' IDENTIFIED BY 'password';
GRANT ALL PRIVILEGES ON sb_ecom.* TO 'ecom_user'@'localhost';
FLUSH PRIVILEGES;
```

**Step 2: Configure Application**

Edit `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/sb_ecom
spring.datasource.username=ecom_user
spring.datasource.password=password
```

**Step 3: Build and Run**

```bash
# Build the project
mvn clean install -DskipTests

# Run the application
mvn spring-boot:run

# Or run with specific profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

**Step 4: Verify**

Open http://localhost:8080/swagger-ui.html

---

## API Documentation

### Swagger UI
Access interactive API docs at: `http://localhost:8080/swagger-ui.html`

### Base URL
```
http://localhost:8080/api
```

### Main Endpoints

#### Authentication
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | /api/auth/signup | Register new user |
| POST | /api/auth/signin | Login |
| POST | /api/auth/refresh | Refresh access token |
| POST | /api/auth/signout | Logout |

#### Products
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | /api/public/products | Get all products |
| GET | /api/public/products/{id} | Get product by ID |
| POST | /api/admin/categories/{categoryId}/product | Add product (Admin) |
| PUT | /api/admin/products/{id} | Update product (Admin) |
| DELETE | /api/admin/products/{id} | Delete product (Admin) |

#### Cart
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | /api/carts | Get user's cart |
| POST | /api/carts/products/{productId}/quantity/{qty} | Add to cart |
| DELETE | /api/carts/{cartId}/product/{productId} | Remove from cart |

#### Orders
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | /api/order/users/payments/{paymentMethod} | Place order |
| GET | /api/order/users/orders | Get user's orders |
| GET | /api/order/tracking/{orderId} | Track order |

#### Reviews
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | /api/products/{productId}/reviews | Add review |
| GET | /api/products/{productId}/reviews | Get product reviews |

#### Wishlist
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | /api/wishlist | Get wishlist |
| POST | /api/wishlist/products/{productId} | Add to wishlist |
| DELETE | /api/wishlist/products/{productId} | Remove from wishlist |

---

## Authentication

### Register a User

```bash
curl -X POST http://localhost:8080/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john",
    "email": "john@example.com",
    "password": "password123",
    "role": ["user"]
  }'
```

### Login

```bash
curl -X POST http://localhost:8080/api/auth/signin \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john",
    "password": "password123"
  }'
```

**Response:**
```json
{
  "id": 1,
  "username": "john",
  "email": "john@example.com",
  "roles": ["ROLE_USER"],
  "jwtToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000"
}
```

### Use Token in Requests

```bash
curl -X GET http://localhost:8080/api/carts \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

### Refresh Token

```bash
curl -X POST http://localhost:8080/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "550e8400-e29b-41d4-a716-446655440000"
  }'
```

---

## Database Schema

### Main Entities

```
users
├── user_id (PK)
├── username
├── email
├── password
└── roles (Many-to-Many)

products
├── product_id (PK)
├── product_name
├── description
├── price
├── discount
├── special_price
├── quantity
├── image
└── category_id (FK)

orders
├── order_id (PK)
├── email
├── order_date
├── total_amount
├── order_status
├── address_id (FK)
└── payment_id (FK)

reviews
├── review_id (PK)
├── rating
├── title
├── comment
├── user_id (FK)
└── product_id (FK)

wishlists
├── wishlist_id (PK)
├── user_id (FK)
└── items (One-to-Many)
```

---

## Testing

### Run All Tests

```bash
mvn test
```

### Run Unit Tests Only

```bash
mvn test -Dtest=*Test
```

### Run Integration Tests

```bash
mvn verify -Dspring.profiles.active=integration
```

### Generate Coverage Report

```bash
mvn jacoco:report
# Report at: target/site/jacoco/index.html
```

### Test Categories

| Type | Location | Purpose |
|------|----------|---------|
| Unit Tests | `src/test/.../service/` | Test business logic |
| Controller Tests | `src/test/.../controller/` | Test REST endpoints |
| Integration Tests | `src/test/.../integration/` | Test with real database |

---

## Docker Deployment

### Build Docker Image

```bash
docker build -t ecomverse:latest .
```

### Run with Docker Compose

```bash
# Start all services
docker-compose up -d

# View logs
docker-compose logs -f app

# Stop services
docker-compose down

# Stop and remove volumes
docker-compose down -v
```

### Docker Compose Services

```yaml
services:
  app:        # Spring Boot application (port 8080)
  mysql:      # MySQL database (port 3306)
  redis:      # Redis cache (port 6379)
  adminer:    # Database UI (port 8081)
```

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| SPRING_PROFILES_ACTIVE | Active profile | docker |
| MYSQL_HOST | MySQL hostname | mysql |
| MYSQL_PORT | MySQL port | 3306 |
| REDIS_HOST | Redis hostname | redis |
| JWT_SECRET | JWT signing key | (required) |

---

## Configuration

### Application Profiles

| Profile | File | Use Case |
|---------|------|----------|
| default | application.properties | Base configuration |
| dev | application-dev.properties | Local development |
| docker | application-docker.properties | Docker deployment |
| prod | application-prod.properties | Production |

### Key Configuration Properties

```properties
# JWT Configuration
app.jwt.secret=your-secret-key
app.jwt.expirationMs=900000          # 15 minutes
app.jwt.refreshExpirationMs=604800000 # 7 days

# Rate Limiting
app.ratelimit.anonymous=20
app.ratelimit.authenticated=100
app.ratelimit.premium=500

# Redis Cache TTL
spring.cache.redis.time-to-live=600000  # 10 minutes
```

---

## Monitoring

### Health Check

```bash
curl http://localhost:8080/actuator/health
```

### Available Actuator Endpoints

| Endpoint | Description |
|----------|-------------|
| /actuator/health | Application health |
| /actuator/info | Application info |
| /actuator/metrics | Application metrics |

---

## Troubleshooting

### Common Issues

**1. Database Connection Error**
```
Solution: Ensure MySQL is running and credentials are correct
```

**2. Port Already in Use**
```bash
# Find process using port 8080
lsof -i :8080
# Kill the process
kill -9 <PID>
```

**3. Docker Memory Issues**
```
Solution: Increase Docker memory allocation in Docker Desktop settings
```

**4. JWT Token Expired**
```
Solution: Use refresh token endpoint to get new access token
```

---

## Contact

- **Author**: Avaneet Kaur Saluja
- **Email**: salujaavaneet17@gmail.com
- **LinkedIn**: [linkedin.com/in/salujaavaneetkaur](https://linkedin.com/in/salujaavaneetkaur)
- **GitHub**: [github.com/salujaavaneetkaur-a11y](https://github.com/salujaavaneetkaur-a11y)

---

## License

This project is for educational and portfolio purposes.
