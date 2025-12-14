# EComVerse

A comprehensive, production-ready e-commerce platform built with Spring Boot 3.5.3 and Java 17, featuring secure authentication, payment integration, and complete order management capabilities.

## 📋 Project Overview

EComVerse is a modern e-commerce application designed with clean architecture principles and industry best practices. It provides a complete backend solution for online retail operations with REST APIs, robust security, and scalable infrastructure.

## ✨ Key Features

### 1. **User Management**
- Secure user registration and authentication
- JWT-based token authentication
- Role-based access control (ADMIN, USER)
- User profile management
- Address management for shipping

### 2. **Product Catalog**
- Product listing and search
- Category-based organization
- Inventory management
- Product details and specifications

### 3. **Shopping Cart**
- Add/remove products from cart
- Cart item management
- Real-time cart updates
- Cart persistence

### 4. **Order Processing**
- Create and manage orders
- Order status tracking
- Order history and details
- Integration with cart management

### 5. **Payment Integration**
- Secure payment processing with Stripe
- Multiple payment method support
- Payment status tracking
- Transaction logging

### 6. **Security Features**
- JWT token-based authentication
- Password encryption with bcrypt
- Input validation and sanitization
- Role-based authorization
- CORS configuration
- SQL injection prevention

## 🛠 Technology Stack

### Backend Framework
- **Spring Boot 3.5.3** - Modern, efficient Spring Boot framework
- **Java 17** - Latest LTS Java version with latest features
- **Spring Security** - Robust security framework
- **Spring Data JPA** - Simplified data access layer
- **Hibernate** - ORM for database operations

### Database
- **MySQL** (Primary)
- **PostgreSQL** (Alternative support)

### Libraries & Tools
- **Lombok 1.18.30** - Reduces boilerplate code
- **ModelMapper 3.0.0** - Entity to DTO mapping
- **JJWT 0.12.5** - JWT token creation and validation
- **Stripe Java SDK 29.3.0** - Payment processing
- **SpringDoc OpenAPI 2.8.9** - Swagger/OpenAPI documentation
- **Spring Validation** - Bean validation and constraint violations

### Build & Testing
- **Maven** - Build automation and dependency management
- **JUnit 5** - Unit testing framework
- **Mockito** - Mocking framework for tests
- **Spring Security Test** - Security testing utilities

## 🏗 Architecture

### Layered Architecture

```
┌─────────────────────────────────────────┐
│   REST API Layer (Controllers)          │
├─────────────────────────────────────────┤
│   Business Logic Layer (Services)       │
├─────────────────────────────────────────┤
│   Data Access Layer (Repositories)      │
├─────────────────────────────────────────┤
│   Domain Model Layer (Entities/DTOs)    │
├─────────────────────────────────────────┤
│   Database (MySQL/PostgreSQL)           │
└─────────────────────────────────────────┘
```

### Core Components

1. **Controllers** (6 REST Controllers)
   - AuthController - Authentication endpoints
   - ProductController - Product management
   - CategoryController - Category management
   - CartController - Shopping cart operations
   - OrderController - Order processing
   - AddressController - User address management

2. **Services** (12 Service Classes)
   - Interface-based service design
   - Business logic implementation
   - Transaction management
   - Data validation

3. **Repositories** (10 JPA Repositories)
   - Database abstraction
   - Custom query methods
   - CRUD operations

4. **Models** (11 Entities)
   - User, Role, AppRole
   - Product, Category
   - Cart, CartItem
   - Order, OrderItem
   - Address, Payment

5. **Exception Handling**
   - Global exception handler
   - Custom exception classes
   - Proper HTTP status mapping

## 🔐 Security Implementation

### Authentication Flow
```
User Credentials → AuthController → AuthenticationManager
                                   ↓
                        JWT Token Generation
                                   ↓
                        JwtTokenProvider
                                   ↓
                        Return JWT Token
```

### Authorization
- Role-based access control (RBAC)
- Method-level security annotations
- Path-based security configuration

## 🚀 Getting Started

### Prerequisites
- Java 17 or higher
- Maven 3.6+
- MySQL 5.7+ or PostgreSQL 12+

### Installation

1. **Clone the repository**
```bash
git clone <repository-url>
cd ecomverse
```

2. **Configure Database**
Edit `src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/ecommerce
spring.datasource.username=root
spring.datasource.password=your_password
```

3. **Configure JWT Secret**
Update JWT secret in application.properties:
```properties
spring.app.jwtSecret=your-secret-key-here
spring.app.jwtExpirationMs=3000000
```

4. **Build the Project**
```bash
mvn clean install
```

5. **Run the Application**
```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

## 📚 API Documentation

### Swagger/OpenAPI Documentation
Once the application is running, access the API documentation at:
```
http://localhost:8080/swagger-ui.html
```

### API Endpoints Overview

#### Authentication
- `POST /api/auth/signin` - User login
- `POST /api/auth/signup` - User registration
- `POST /api/auth/signout` - User logout

#### Products
- `GET /api/public/products` - Get all products
- `GET /api/public/products/{id}` - Get product by ID
- `POST /api/admin/products` - Create product (Admin only)

#### Categories
- `GET /api/public/categories` - Get all categories
- `POST /api/public/categories` - Create category
- `DELETE /api/admin/categories/{id}` - Delete category

#### Cart
- `POST /api/carts` - Add to cart
- `GET /api/carts` - Get cart items
- `DELETE /api/carts/{id}` - Remove from cart

#### Orders
- `POST /api/orders` - Create order
- `GET /api/orders` - Get user orders
- `GET /api/orders/{id}` - Get order details

#### Addresses
- `POST /api/addresses` - Add address
- `GET /api/addresses` - Get user addresses
- `PUT /api/addresses/{id}` - Update address
- `DELETE /api/addresses/{id}` - Delete address

## 💡 Design Patterns & Best Practices

### Design Patterns Implemented
- **Layered Architecture** - Clear separation of concerns
- **Repository Pattern** - Data access abstraction
- **Service Pattern** - Business logic encapsulation
- **DTO Pattern** - Data transfer between layers
- **Singleton Pattern** - Spring beans
- **Factory Pattern** - Object creation

### Best Practices
- Clean Code Principles
- SOLID Principles (Single Responsibility, Open/Closed, Liskov Substitution, Interface Segregation, Dependency Inversion)
- RESTful API Design
- Input Validation
- Exception Handling
- Logging
- Documentation

## 🔄 Database Schema Highlights

### Key Relationships
- **One-to-Many**: User → Orders, Order → OrderItems
- **Many-to-Many**: Product ↔ Categories
- **One-to-One**: User → Cart, Order → Payment

### Transactional Consistency
- Database transactions for critical operations
- Optimistic locking for concurrent updates
- Foreign key constraints for data integrity

## 📊 Project Statistics

- **Total Controllers**: 6
- **Total Services**: 12
- **Total Repositories**: 10
- **Database Entities**: 11
- **API Endpoints**: 30+
- **Lines of Code**: 5000+

## 🎯 Future Enhancements

1. **Performance Optimization**
   - Redis caching for frequently accessed data
   - Database query optimization
   - Pagination for list endpoints

2. **Advanced Features**
   - Email notifications for order updates
   - Product reviews and ratings
   - Wishlist functionality
   - Inventory management

3. **Deployment**
   - Docker containerization
   - Kubernetes orchestration
   - CI/CD pipeline (GitHub Actions)

4. **Microservices Architecture**
   - Service decomposition
   - Message queue integration (Kafka/RabbitMQ)
   - API Gateway implementation

5. **Testing**
   - Comprehensive unit tests
   - Integration tests
   - End-to-end tests

## 📝 Project Configuration

### Application Profiles
- **dev** - Development environment
- **prod** - Production environment
- **test** - Testing environment

### Logging Configuration
- SLF4J with Logback
- Configurable log levels
- Debug mode available

## 🤝 Contributing

This is a personal portfolio project. Suggestions and improvements are welcome.

## 📄 License

This project is open source and available for educational purposes.

## 👤 Author

**Avaneet**
- E-Commerce Platform Developer
- Spring Boot Specialist
- Full-Stack Java Developer

## 📞 Contact

For questions or suggestions, feel free to reach out.

---

**Built with ❤️ using Spring Boot and Java**
