# EComVerse — API Debug & Learning Guide

> **How to use this:** Start your app, open Postman, and hit each endpoint in order.
> For every endpoint — read what it does, what to observe in the response, and what Spring Boot concept it teaches.
> Base URL: `http://localhost:8080/api`

---

## Setup Before You Start

1. Start the app: `mvn spring-boot:run`
2. Open Postman and import `EComVerse_Postman_Collection.json`
3. The collection has variables: `baseUrl`, `jwtToken`, `refreshToken` — these auto-fill after login
4. Follow the sections **in order** — Auth must come first

---

## Section 1 — Authentication

> **Spring Boot concepts:** `@RestController`, `@PostMapping`, `@RequestBody`, `ResponseEntity`, Spring Security, BCrypt, JWT, Cookies

---

### 1.1 Register User

```
POST /api/auth/signup
Auth: None
```

**Request Body:**
```json
{
  "username": "testuser",
  "email": "test@example.com",
  "password": "password123",
  "role": ["user"]
}
```

**What to observe:**
- Returns `200 OK` with message `"User registered successfully"`
- Check your DB — `users` table has a new row
- Check `user_roles` table — role is linked
- Password in DB is a BCrypt hash like `$2a$10$...` — never plain text

**What breaks it:**
- Send same username again → should return `400` (username already taken)
- Remove `email` field → should return `400` validation error
- Send `"role": ["superadmin"]` → should be rejected (only user/seller/admin allowed)

**Spring Boot concept:**
> `@Valid` on the request body triggers Bean Validation. `@ExceptionHandler` in `MyGlobalExceptionHandler` catches `MethodArgumentNotValidException` and returns 400 with field errors.

---

### 1.2 Register Admin

```
POST /api/auth/signup
Auth: None
```

**Request Body:**
```json
{
  "username": "adminuser",
  "email": "admin@example.com",
  "password": "admin123",
  "role": ["admin"]
}
```

**What to observe:**
- Same flow as Register User
- Check `user_roles` table — this user has `ROLE_ADMIN`
- You will use this token later for admin-only endpoints

---

### 1.3 Login

```
POST /api/auth/signin
Auth: None
```

**Request Body:**
```json
{
  "username": "testuser",
  "password": "password123"
}
```

**What to observe:**
- Returns JWT token and refresh token
- Postman auto-saves `jwtToken` and `refreshToken` to collection variables
- Check response headers — `Set-Cookie` header contains `JWT_TOKEN` as HttpOnly cookie
- JWT is a three-part dot-separated string: `header.payload.signature`
- Decode the payload at [jwt.io](https://jwt.io) — you'll see `sub` (username) and `exp` (expiry)

**What breaks it:**
- Wrong password → `401 Unauthorized`
- Non-existent username → `401 Unauthorized`
- Note: never says "wrong password" vs "user not found" — security best practice

**Spring Boot concept:**
> `AuthenticationManager.authenticate()` calls `UserDetailsServiceImpl.loadUserByUsername()`, fetches from DB, BCrypt compares passwords. On success, `JwtUtils.generateTokenFromUsername()` creates the JWT. `OncePerRequestFilter` pattern for `AuthTokenFilter`.

---

### 1.4 Refresh Token

```
POST /api/auth/refresh
Auth: None
```

**Request Body:**
```json
{
  "refreshToken": "{{refreshToken}}"
}
```

**What to observe:**
- Returns a new `accessToken` and new `refreshToken`
- Old refresh token is deleted from DB (token rotation)
- Check `refresh_tokens` table before and after — old token gone, new token present

**What breaks it:**
- Use the old refresh token again after rotating → `403 Forbidden` (token not found)
- Manually expire the token in DB → `403` with "Refresh token expired"

**Spring Boot concept:**
> Refresh token rotation — each use invalidates the old token and issues a new one. Prevents replay attacks. `RefreshTokenService.verifyExpiration()` checks `Instant` comparison.

---

### 1.5 Get Current User

```
GET /api/auth/user
Auth: Bearer {{jwtToken}}
```

**What to observe:**
- Returns logged-in user's id, username, email, roles
- Remove the `Authorization` header → `401 Unauthorized`
- Use an expired JWT → `401 Unauthorized`

**Spring Boot concept:**
> `AuthTokenFilter` intercepts this request, validates JWT, sets `SecurityContextHolder`. `AuthUtil.loggedInUser()` reads from `SecurityContextHolder` to get the current user.

---

### 1.6 Logout

```
POST /api/auth/signout
Auth: Bearer {{jwtToken}}
```

**What to observe:**
- Returns `200` with "Logged out successfully"
- Check `refresh_tokens` table — your token is deleted
- Response has `Set-Cookie` that clears the JWT cookie (maxAge=0)
- Try using the old refresh token after logout → `403` (token deleted)

**Spring Boot concept:**
> Stateless JWT — you can't "invalidate" a JWT itself. But deleting the refresh token means the user can't get new JWTs. The short-lived JWT will expire on its own.

---

## Section 2 — Password Reset

> **Spring Boot concepts:** `@Service`, Email sending, Token generation, `@Transactional`

---

### 2.1 Request Password Reset

```
POST /api/password/forgot
Auth: None
```

**Request Body:**
```json
{
  "email": "test@example.com"
}
```

**What to observe:**
- Returns `200` with success message
- Check `password_reset_tokens` table — a UUID token is created with expiry
- If email service is configured, an email is sent with the reset link
- If email not configured, check logs for the token

**What breaks it:**
- Non-existent email → check if it returns 404 or generic message (security: should be generic)

---

### 2.2 Reset Password

```
POST /api/password/reset
Auth: None
```

**Request Body:**
```json
{
  "token": "uuid-token-from-email",
  "newPassword": "newpassword123"
}
```

**What to observe:**
- Returns `200` on success
- Check DB — password is now a new BCrypt hash
- Token in `password_reset_tokens` is marked `used = true` or deleted
- Try using same token again → should fail (already used)
- Try logging in with old password → `401`
- Try logging in with new password → `200` ✓

---

## Section 3 — Categories

> **Spring Boot concepts:** `@GetMapping`, `@PathVariable`, Pagination, `@PreAuthorize`, `@Auditable`

---

### 3.1 Get All Categories

```
GET /api/public/categories?pageNumber=0&pageSize=10&sortBy=categoryName&sortOrder=asc
Auth: None
```

**What to observe:**
- No auth required — `/public/**` is open in `WebSecurityConfig`
- Returns paginated response: `content`, `pageNumber`, `pageSize`, `totalElements`, `totalPages`, `isLastPage`
- Try `pageSize=2` — see how pagination works
- Try `sortBy=categoryId&sortOrder=desc` — results change order

**What breaks it:**
- Invalid `sortBy` field → observe behavior
- `pageNumber=999` → empty content array but no error

**Spring Boot concept:**
> `Pageable` object is built from `@RequestParam` values in the controller. `PageRequest.of(pageNumber, pageSize, Sort.by(...))` passed to repository. Spring Data JPA handles the SQL `LIMIT` and `OFFSET` automatically.

---

### 3.2 Create Category (Admin)

```
POST /api/admin/categories
Auth: Bearer {{jwtToken}} (must be ADMIN)
```

**Request Body:**
```json
{
  "categoryName": "Electronics"
}
```

**What to observe:**
- Login as admin first, use that JWT
- Returns `201 Created` with the new category
- Check DB — category row created
- Try same category name again → `409` or `400` (duplicate)

**What breaks it:**
- Use a USER token (not admin) → `403 Forbidden`
- No auth header → `401 Unauthorized`
- Empty `categoryName` → `400` validation error

**Spring Boot concept:**
> `/admin/**` endpoints require `ROLE_ADMIN` in `WebSecurityConfig`. Spring Security checks `getAuthorities()` from `UserDetailsImpl`. `@Auditable` annotation triggers `AuditAspect` — check `audit_logs` table after this call.

---

### 3.3 Update Category (Admin)

```
PUT /api/admin/categories/{categoryId}
Auth: Bearer {{jwtToken}} (must be ADMIN)
```

**Request Body:**
```json
{
  "categoryName": "Electronics & Gadgets"
}
```

**What to observe:**
- Replace `{categoryId}` with a real ID from your DB
- Returns updated category
- Check DB — name changed
- Check `audit_logs` table — new audit entry created

**What breaks it:**
- Non-existent `categoryId` → `404 ResourceNotFoundException`
- Empty body → `400` validation error

---

### 3.4 Delete Category (Admin)

```
DELETE /api/admin/categories/{categoryId}
Auth: Bearer {{jwtToken}} (must be ADMIN)
```

**What to observe:**
- Returns success message
- Check DB — category deleted
- If category has products, observe cascade behavior

**What breaks it:**
- Delete category with products linked → check if it throws error or cascades
- Non-existent ID → `404`

---

## Section 4 — Products

> **Spring Boot concepts:** `@RequestParam`, `@Auditable`, `ModelMapper`, `MultipartFile`, Redis caching

---

### 4.1 Get All Products

```
GET /api/public/products?pageNumber=0&pageSize=10&sortBy=price&sortOrder=asc
Auth: None
```

**What to observe:**
- No auth required
- Returns paginated `ProductResponse` with list of `ProductDTO`
- Notice: response has `specialPrice` — calculated as `price × (1 - discount/100)`
- Entity is never returned directly — always converted to DTO via ModelMapper

**Spring Boot concept:**
> `ModelMapper` bean from `AppConfig` maps `Product` entity to `ProductDTO`. Redis cache kicks in on repeat calls — second call should be faster. Check Redis with `redis-cli KEYS "*"` after hitting this endpoint.

---

### 4.2 Get Products by Category

```
GET /api/public/categories/{categoryId}/products
Auth: None
```

**What to observe:**
- Returns only products in that category
- Replace `{categoryId}` with a real ID
- Non-existent categoryId → `404`

**Spring Boot concept:**
> `ProductRepository.findByCategoryOrderByPriceAsc(category)` — Spring Data JPA generates SQL from method name. No `@Query` needed.

---

### 4.3 Search Products by Keyword

```
GET /api/public/products/keyword/{keyword}
Auth: None
```

**Example:** `/api/public/products/keyword/phone`

**What to observe:**
- Returns products where name contains "phone" (case-insensitive)
- Try partial matches — "phon", "PHONE"
- Empty result for no match — not a 404, just empty list

**Spring Boot concept:**
> `findByProductNameLikeIgnoreCase("%phone%")` — the `%` wildcards are added in service layer. This is a LIKE query in SQL. At scale this is slow — Elasticsearch would replace this.

---

### 4.4 Add Product (Admin)

```
POST /api/admin/categories/{categoryId}/product
Auth: Bearer {{jwtToken}} (must be ADMIN)
```

**Request Body:**
```json
{
  "productName": "iPhone 15 Pro",
  "description": "Latest Apple smartphone with A17 Pro chip",
  "price": 999.99,
  "discount": 10.0,
  "quantity": 100
}
```

**What to observe:**
- Returns `ProductDTO` with `specialPrice = 999.99 × (1 - 10/100) = 899.991`
- Check `audit_logs` table — `CREATE_PRODUCT` action logged with user info
- Check `products` table in DB

**What breaks it:**
- `price` as negative → `400` validation
- Missing required fields → `400`
- Non-existent `categoryId` → `404`
- USER role token → `403`

**Spring Boot concept:**
> `@Auditable(action = "CREATE_PRODUCT", entityType = "Product", logRequestBody = true)` — `AuditAspect` intercepts, captures IP from `HttpServletRequest`, saves `AuditLog` entity. Special price calculation in `ProductServiceImpl`.

---

### 4.5 Update Product (Admin)

```
PUT /api/admin/products/{productId}
Auth: Bearer {{jwtToken}} (must be ADMIN)
```

**Request Body:**
```json
{
  "productName": "iPhone 15 Pro Max",
  "description": "Updated description",
  "price": 1199.99,
  "discount": 15.0,
  "quantity": 50
}
```

**What to observe:**
- Returns updated `ProductDTO`
- `specialPrice` recalculated: `1199.99 × 0.85 = 1019.99`
- Check `audit_logs` — `UPDATE_PRODUCT` entry
- Cart items with this product — check if price updates there too (`updateProductInCarts()`)

---

### 4.6 Delete Product (Admin)

```
DELETE /api/admin/products/{productId}
Auth: Bearer {{jwtToken}} (must be ADMIN)
```

**What to observe:**
- Returns success message
- Product removed from DB
- Check `audit_logs` — `DELETE_PRODUCT` entry

**What breaks it:**
- Product in an active order → check behavior (may throw error due to FK constraint)

---

### 4.7 Upload Product Image

```
PUT /api/products/{productId}/image
Auth: Bearer {{jwtToken}}
Body: form-data, key = "image", value = any image file
```

**What to observe:**
- Returns updated `ProductDTO` with image filename
- Image saved to `images/` folder in project root
- `product.image` column updated in DB

**Spring Boot concept:**
> `MultipartFile` in controller. `FileServiceImpl` handles saving to disk. In production this would be S3 — local disk doesn't work with multiple servers.

---

## Section 5 — Cart

> **Spring Boot concepts:** `@Transactional`, `AuthUtil`, SecurityContext, JPQL queries

---

### 5.1 Get Cart

```
GET /api/carts/users/cart
Auth: Bearer {{jwtToken}}
```

**What to observe:**
- Returns your cart with all items, quantities, prices, total
- Empty cart → `cartItems: []`, `totalPrice: 0`
- Each item has `productPrice` (price at time of adding) and `discount`

**Spring Boot concept:**
> `AuthUtil.loggedInEmail()` reads from `SecurityContextHolder` — no need to pass user ID in URL. The API knows who you are from the JWT.

---

### 5.2 Add Product to Cart

```
POST /api/carts/products/{productId}/quantity/{quantity}
Auth: Bearer {{jwtToken}}
```

**Example:** `POST /api/carts/products/1/quantity/2`

**What to observe:**
- Returns updated cart with new item
- Add same product again → quantity increases, doesn't create duplicate
- Check `cart_items` table in DB
- `totalPrice` recalculated

**What breaks it:**
- `quantity` more than `product.quantity` (stock) → `400` (not enough stock)
- Non-existent `productId` → `404`
- Adding 0 quantity → check behavior

**Spring Boot concept:**
> `CartServiceImpl.addProductToCart()` is `@Transactional`. Checks if cart item exists first. If yes, updates quantity. If no, creates new. Uses `CartRepository` with custom JPQL to find cart by user email.

---

### 5.3 Update Cart Item Quantity

```
PUT /api/cart/products/{productId}/quantity/{operation}
Auth: Bearer {{jwtToken}}
```

**operation** = `increase` or `decrease`

**Example:** `PUT /api/cart/products/1/quantity/increase`

**What to observe:**
- Quantity goes up by 1 (increase) or down by 1 (decrease)
- Decrease to 0 → item removed from cart automatically
- `totalPrice` recalculated

---

### 5.4 Remove Product from Cart

```
DELETE /api/carts/{cartId}/product/{productId}
Auth: Bearer {{jwtToken}}
```

**What to observe:**
- Item removed from cart
- `totalPrice` recalculated
- `cart_items` row deleted in DB

---

## Section 6 — Address

> **Spring Boot concepts:** `@Valid`, Bean Validation, `@ManyToOne` relationship

---

### 6.1 Add Address

```
POST /api/addresses
Auth: Bearer {{jwtToken}}
```

**Request Body:**
```json
{
  "street": "123 Main Street",
  "buildingName": "Tower A",
  "city": "Mumbai",
  "state": "Maharashtra",
  "country": "India",
  "pincode": "400001"
}
```

**What to observe:**
- Returns created `AddressDTO` with `addressId`
- Check `addresses` table — `user_id` links to your user
- One user can have multiple addresses

**What breaks it:**
- Missing `city` → `400` validation
- Missing `pincode` → `400` validation

---

### 6.2 Get All Addresses (Admin)

```
GET /api/addresses
Auth: Bearer {{jwtToken}}
```

**What to observe:**
- Returns ALL addresses in system (admin view)
- Note the difference from "Get User Addresses" below

---

### 6.3 Get Address by ID

```
GET /api/addresses/{addressId}
Auth: Bearer {{jwtToken}}
```

**What to observe:**
- Returns single address
- Non-existent ID → `404 ResourceNotFoundException`
- The exception message format: `"Address not found with addressId: 1"`

---

### 6.4 Get User's Addresses

```
GET /api/users/addresses
Auth: Bearer {{jwtToken}}
```

**What to observe:**
- Returns ONLY your addresses (uses `AuthUtil.loggedInUser()`)
- Different users see different results — same endpoint, auth-driven filtering

---

### 6.5 Update Address

```
PUT /api/addresses/{addressId}
Auth: Bearer {{jwtToken}}
```

**Request Body:**
```json
{
  "street": "456 Oak Avenue",
  "buildingName": "Building B",
  "city": "Delhi",
  "state": "Delhi",
  "country": "India",
  "pincode": "110001"
}
```

**What to observe:**
- Returns updated address
- Check DB — all fields updated

---

### 6.6 Delete Address

```
DELETE /api/addresses/{addressId}
Auth: Bearer {{jwtToken}}
```

**What to observe:**
- Returns success message
- Address removed from DB
- Check if user still linked — `user.addresses` list should update

---

## Section 7 — Orders

> **Spring Boot concepts:** `@Transactional`, State machine (OrderStatus), Cascade operations, Stripe integration

---

### 7.1 Place Order

```
POST /api/order/users/payments/{paymentMethod}
Auth: Bearer {{jwtToken}}
```

**Example:** `POST /api/order/users/payments/Card`

**Request Body:**
```json
{
  "addressId": 1,
  "pgName": "Stripe",
  "pgPaymentId": "pay_123456789",
  "pgStatus": "Success",
  "pgResponseMessage": "Payment successful"
}
```

**What to observe:**
- Cart must not be empty (add products first)
- Returns `OrderDTO` with `orderId`, `orderItems`, `payment`, `totalAmount`, `orderStatus: PENDING`
- Check DB — `orders` table has new row, `order_items` populated, `cart_items` CLEARED
- Payment details in `payments` table
- This is `@Transactional` — ALL of this or NONE of it

**What breaks it:**
- Empty cart → `400` error
- Non-existent `addressId` → `404`
- Try throwing an exception mid-flow — observe rollback

**Spring Boot concept:**
> This is the most important transaction in the app. `OrderServiceImpl.placeOrder()` is `@Transactional`. Cart → Order → OrderItems → Payment → Clear Cart, all in one transaction. If payment fails, nothing is saved.

---

### 7.2 Get User Orders

```
GET /api/order/users/orders
Auth: Bearer {{jwtToken}}
```

**What to observe:**
- Returns list of your orders with all details
- Different user → different orders (auth-driven)

---

### 7.3 Track Order

```
GET /api/orders/{orderId}/tracking
Auth: Bearer {{jwtToken}}
```

**What to observe:**
- Returns tracking info: status, trackingNumber, carrier, location, timestamp
- Initially minimal info (just PENDING status)
- More info available after admin updates status

---

### 7.4 Update Order Status (Admin)

```
PUT /api/admin/orders/{orderId}/status
Auth: Bearer {{jwtToken}} (must be ADMIN)
```

**Request Body:**
```json
{
  "status": "CONFIRMED",
  "notes": "Order confirmed and being processed"
}
```

**Valid status transitions:**
```
PENDING → CONFIRMED → PROCESSING → SHIPPED → OUT_FOR_DELIVERY → DELIVERED
                                                                → RETURNED → REFUNDED
```

**What to observe:**
- Order status changes in DB
- `order_status_history` table gets a new row (audit trail)
- Try invalid transition: PENDING → DELIVERED → should fail

**What breaks it:**
- Invalid transition (e.g., DELIVERED → PENDING) → `400`
- `OrderStatus.canTransitionTo()` method validates this

**Spring Boot concept:**
> State machine pattern. `OrderStatus` enum has a `canTransitionTo()` method that defines valid transitions. This is the Strategy/State pattern.

---

### 7.5 Cancel Order

```
PUT /api/orders/{orderId}/cancel
Auth: Bearer {{jwtToken}}
```

**Request Body:**
```json
{
  "reason": "Changed my mind"
}
```

**What to observe:**
- Can only cancel if status is PENDING or CONFIRMED
- Status changes to CANCELLED

---

## Section 8 — Reviews

> **Spring Boot concepts:** Unique constraint, `@PreAuthorize`, Pagination, Aggregation queries

---

### 8.1 Add Review

```
POST /api/products/{productId}/reviews
Auth: Bearer {{jwtToken}} (must be USER role)
```

**Request Body:**
```json
{
  "rating": 5,
  "title": "Great Product!",
  "comment": "This product exceeded my expectations."
}
```

**What to observe:**
- Returns created review with `reviewId`, `createdAt`
- Check `reviews` table — `user_id` and `product_id` linked
- Try adding second review for same product with same user → should fail (unique constraint on user_id + product_id)

**What breaks it:**
- `rating` outside 1-5 → `400` validation
- Same user reviewing same product twice → `400` or `409`
- No auth → `401`

**Spring Boot concept:**
> `@UniqueConstraint(columnNames = {"user_id", "product_id"})` on the `Review` entity. One review per user per product enforced at DB level.

---

### 8.2 Get Product Reviews

```
GET /api/public/products/{productId}/reviews?page=0&size=10&sortBy=newest
Auth: None
```

**What to observe:**
- Public endpoint — no auth needed
- Paginated results
- Try `sortBy=rating` — sorted by rating

---

### 8.3 Get Review Stats

```
GET /api/public/products/{productId}/reviews/stats
Auth: None
```

**What to observe:**
- Returns average rating, total count, rating distribution (how many 5-star, 4-star, etc.)
- This is an aggregation query — check `ReviewRepository` for the JPQL

---

### 8.4 Update Review

```
PUT /api/reviews/{reviewId}
Auth: Bearer {{jwtToken}}
```

**Request Body:**
```json
{
  "rating": 4,
  "title": "Good Product",
  "comment": "Updated my review after using it for a month."
}
```

**What to observe:**
- Can only update YOUR own review
- Try updating someone else's review → should return `403` or `404`
- `updatedAt` timestamp changes

---

### 8.5 Delete Review

```
DELETE /api/reviews/{reviewId}
Auth: Bearer {{jwtToken}}
```

**What to observe:**
- Deletes review
- Can only delete your own

---

### 8.6 Mark Review Helpful

```
POST /api/reviews/{reviewId}/helpful
Auth: Bearer {{jwtToken}}
```

**What to observe:**
- `helpfulVotes` counter increments by 1
- Check DB — `helpful_votes` column updated

---

## Section 9 — Wishlist

> **Spring Boot concepts:** `@OneToOne`, `@OneToMany`, service orchestration (move-to-cart)

---

### 9.1 Get Wishlist

```
GET /api/wishlist
Auth: Bearer {{jwtToken}}
```

**What to observe:**
- Returns your wishlist with items
- `priceWhenAdded` — price captured at time of adding (price may change later)

---

### 9.2 Add to Wishlist

```
POST /api/wishlist/products/{productId}
Auth: Bearer {{jwtToken}}
```

**What to observe:**
- Product added to wishlist
- `priceWhenAdded` = current product price
- Add same product twice → should handle gracefully (no duplicate)
- Check `wishlist_items` table

---

### 9.3 Remove from Wishlist

```
DELETE /api/wishlist/products/{productId}
Auth: Bearer {{jwtToken}}
```

**What to observe:**
- Item removed from wishlist
- Wishlist still exists (not deleted), just item removed

---

### 9.4 Move to Cart

```
POST /api/wishlist/products/{productId}/move-to-cart?quantity=1
Auth: Bearer {{jwtToken}}
```

**What to observe:**
- Product added to cart
- Product removed from wishlist
- This calls both `CartService` and `WishlistService` — service orchestration
- `@Transactional` ensures both happen or neither does

**Spring Boot concept:**
> One endpoint triggering two service operations. `WishlistServiceImpl.moveToCart()` calls `CartServiceImpl.addProductToCart()` internally — services calling other services.

---

### 9.5 Clear Wishlist

```
DELETE /api/wishlist
Auth: Bearer {{jwtToken}}
```

**What to observe:**
- ALL items removed from wishlist
- Wishlist entity still exists, just empty

---

## Section 10 — Health & Monitoring

> **Spring Boot concepts:** Spring Actuator, Prometheus metrics

---

### 10.1 Health Check

```
GET http://localhost:8080/actuator/health
Auth: None
```

**What to observe:**
- Returns `{"status": "UP"}`
- Shows DB connection status, disk space, Redis status
- If DB is down → `{"status": "DOWN"}`
- Used by load balancers to know if this instance is healthy

---

### 10.2 Application Info

```
GET http://localhost:8080/actuator/info
Auth: None
```

**What to observe:**
- Returns app metadata

---

### 10.3 All Metrics (Dev only)

```
GET http://localhost:8080/actuator/metrics
GET http://localhost:8080/actuator/prometheus
```

**What to observe:**
- `metrics` → lists all available metric names
- `prometheus` → Prometheus scrape format (used by Grafana dashboards)
- Try `GET /actuator/metrics/http.server.requests` — request count, latency

---

## Debugging Checklist

Use this after hitting each endpoint:

```
[ ] Response status code correct? (200, 201, 400, 401, 403, 404)
[ ] Response body is a DTO (not raw entity)?
[ ] DB updated correctly? (check via MySQL client)
[ ] Audit log created? (check audit_logs table for @Auditable endpoints)
[ ] Cache updated? (redis-cli KEYS "*" before and after)
[ ] Error responses follow APIResponse format? {message, status}
[ ] Validation errors return field-level messages?
[ ] Unauthorized access properly rejected?
```

---

## Common Errors and Why They Happen

| Error | Reason | Fix |
|---|---|---|
| `401 Unauthorized` | No JWT or expired JWT | Login again, use fresh token |
| `403 Forbidden` | Wrong role (USER doing ADMIN action) | Login as admin |
| `404 Not Found` | Wrong ID in path | Check DB for valid IDs |
| `400 Bad Request` | Validation failed or business rule violated | Check error message for which field |
| `429 Too Many Requests` | Rate limit hit | Wait a minute, then retry |
| `500 Internal Server Error` | Bug or DB connection issue | Check application logs |

---

## Spring Boot Concept Map

| Endpoint | Key Concept to Understand |
|---|---|
| POST /auth/signup | `@Valid`, Bean Validation, BCrypt |
| POST /auth/signin | `AuthenticationManager`, JWT generation, HttpOnly cookie |
| POST /auth/refresh | Refresh token rotation, `Instant` comparison |
| GET /public/categories | `Pageable`, no auth required, public endpoint |
| POST /admin/categories | `@Auditable`, AOP interception, role check |
| POST /admin/.../product | `@Transactional`, ModelMapper, special price calc |
| POST /carts/products/... | `@Transactional`, SecurityContext, cart logic |
| POST /order/users/payments/... | Full `@Transactional` flow, cascade operations |
| PUT /admin/orders/.../status | State machine, `canTransitionTo()` validation |
| POST /wishlist/.../move-to-cart | Service orchestration, cross-service calls |
| GET /actuator/health | Spring Actuator, production monitoring |
