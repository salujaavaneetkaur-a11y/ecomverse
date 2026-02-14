package com.ecommerce.project.integration;

import com.ecommerce.project.model.Category;
import com.ecommerce.project.model.Role;
import com.ecommerce.project.model.AppRole;
import com.ecommerce.project.model.User;
import com.ecommerce.project.payload.ProductDTO;
import com.ecommerce.project.repositories.CategoryRepository;
import com.ecommerce.project.repositories.RoleRepository;
import com.ecommerce.project.repositories.UserRepository;
import com.ecommerce.project.security.jwt.JwtUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration Tests for Product API
 *
 * ============================================================
 * 🎓 INTEGRATION TEST vs UNIT TEST:
 * ============================================================
 *
 * UNIT TEST:
 * - Tests one class in isolation
 * - Mocks all dependencies
 * - Very fast (milliseconds)
 * - Tests business logic
 *
 * INTEGRATION TEST:
 * - Tests multiple components together
 * - Uses real database (TestContainers)
 * - Slower (seconds)
 * - Tests the full flow (Controller → Service → Repository → DB)
 *
 * ============================================================
 * 📋 INTERVIEW TIP:
 * "Integration tests verify that all layers work together.
 * For example, this test verifies:
 * 1. HTTP request is correctly parsed
 * 2. Controller validates input
 * 3. Service processes business logic
 * 4. Repository saves to real MySQL
 * 5. Response is correctly formatted"
 * ============================================================
 */
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ProductIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtils jwtUtils;

    private String adminToken;
    private Category testCategory;

    /**
     * Setup runs before each test
     * Creates necessary test data in the REAL database
     */
    @BeforeEach
    void setUp() {
        // Clean up first
        categoryRepository.deleteAll();

        // Create roles if not exist
        if (roleRepository.findByRoleName(AppRole.ROLE_ADMIN).isEmpty()) {
            roleRepository.save(new Role(AppRole.ROLE_ADMIN));
        }
        if (roleRepository.findByRoleName(AppRole.ROLE_USER).isEmpty()) {
            roleRepository.save(new Role(AppRole.ROLE_USER));
        }
        if (roleRepository.findByRoleName(AppRole.ROLE_SELLER).isEmpty()) {
            roleRepository.save(new Role(AppRole.ROLE_SELLER));
        }

        // Create admin user if not exists
        if (!userRepository.existsByUserName("integrationAdmin")) {
            User admin = new User();
            admin.setUserName("integrationAdmin");
            admin.setEmail("admin@integration.test");
            admin.setPassword(passwordEncoder.encode("password123"));

            Set<Role> roles = new HashSet<>();
            roles.add(roleRepository.findByRoleName(AppRole.ROLE_ADMIN).get());
            roles.add(roleRepository.findByRoleName(AppRole.ROLE_SELLER).get());
            roles.add(roleRepository.findByRoleName(AppRole.ROLE_USER).get());
            admin.setRoles(roles);

            userRepository.save(admin);
        }

        // Generate JWT token for admin
        var authorities = List.of(
            new SimpleGrantedAuthority("ROLE_ADMIN"),
            new SimpleGrantedAuthority("ROLE_SELLER"),
            new SimpleGrantedAuthority("ROLE_USER")
        );
        var authentication = new UsernamePasswordAuthenticationToken(
            "integrationAdmin", null, authorities
        );
        adminToken = jwtUtils.generateTokenFromUsername(authentication);

        // Create test category
        testCategory = new Category();
        testCategory.setCategoryName("Integration Test Electronics");
        testCategory = categoryRepository.save(testCategory);
    }

    // ==================== GET ALL PRODUCTS TESTS ====================

    @Test
    @Order(1)
    @DisplayName("GET /api/public/products - Should return empty list initially")
    void getAllProducts_EmptyDatabase_ReturnsEmptyList() throws Exception {
        mockMvc.perform(get("/api/public/products")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements", is(0)));
    }

    // ==================== ADD PRODUCT TESTS ====================

    @Test
    @Order(2)
    @DisplayName("POST /api/admin/categories/{id}/product - Should add product successfully")
    void addProduct_ValidInput_ReturnsCreated() throws Exception {
        ProductDTO productDTO = new ProductDTO();
        productDTO.setProductName("MacBook Pro 16");
        productDTO.setDescription("Apple laptop with M3 chip");
        productDTO.setPrice(2499.99);
        productDTO.setDiscount(5.0);
        productDTO.setQuantity(50);

        mockMvc.perform(post("/api/admin/categories/{categoryId}/product", testCategory.getCategoryId())
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(productDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.productName", is("MacBook Pro 16")))
                .andExpect(jsonPath("$.price", is(2499.99)))
                .andExpect(jsonPath("$.specialPrice", is(2374.99))); // 2499.99 - 5% = 2374.99
    }

    @Test
    @Order(3)
    @DisplayName("POST /api/admin/categories/{id}/product - Should fail without auth")
    void addProduct_NoAuth_ReturnsUnauthorized() throws Exception {
        ProductDTO productDTO = new ProductDTO();
        productDTO.setProductName("Unauthorized Product");
        productDTO.setDescription("Should not be created");
        productDTO.setPrice(100.0);
        productDTO.setQuantity(10);

        mockMvc.perform(post("/api/admin/categories/{categoryId}/product", testCategory.getCategoryId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(productDTO)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(4)
    @DisplayName("POST /api/admin/categories/{id}/product - Should fail for invalid category")
    void addProduct_InvalidCategory_ReturnsNotFound() throws Exception {
        ProductDTO productDTO = new ProductDTO();
        productDTO.setProductName("Test Product");
        productDTO.setDescription("Test description here");
        productDTO.setPrice(100.0);
        productDTO.setQuantity(10);

        mockMvc.perform(post("/api/admin/categories/{categoryId}/product", 99999L)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(productDTO)))
                .andExpect(status().isNotFound());
    }

    // ==================== SEARCH PRODUCTS TESTS ====================

    @Test
    @Order(5)
    @DisplayName("GET /api/public/products/keyword/{keyword} - Should search products")
    void searchProducts_ValidKeyword_ReturnsMatchingProducts() throws Exception {
        // First, add a product
        ProductDTO productDTO = new ProductDTO();
        productDTO.setProductName("iPhone 15 Pro Max");
        productDTO.setDescription("Latest Apple smartphone");
        productDTO.setPrice(1199.99);
        productDTO.setDiscount(0.0);
        productDTO.setQuantity(100);

        mockMvc.perform(post("/api/admin/categories/{categoryId}/product", testCategory.getCategoryId())
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(productDTO)))
                .andExpect(status().isCreated());

        // Now search for it
        mockMvc.perform(get("/api/public/products/keyword/{keyword}", "iPhone")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.content[0].productName", containsStringIgnoringCase("iPhone")));
    }

    // ==================== UPDATE PRODUCT TESTS ====================

    @Test
    @Order(6)
    @DisplayName("PUT /api/admin/products/{id} - Should update product successfully")
    void updateProduct_ValidInput_ReturnsUpdated() throws Exception {
        // First, add a product
        ProductDTO productDTO = new ProductDTO();
        productDTO.setProductName("Original Product Name");
        productDTO.setDescription("Original description");
        productDTO.setPrice(500.0);
        productDTO.setDiscount(10.0);
        productDTO.setQuantity(25);

        MvcResult createResult = mockMvc.perform(post("/api/admin/categories/{categoryId}/product", testCategory.getCategoryId())
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(productDTO)))
                .andExpect(status().isCreated())
                .andReturn();

        ProductDTO created = objectMapper.readValue(
            createResult.getResponse().getContentAsString(),
            ProductDTO.class
        );

        // Now update it
        ProductDTO updateDTO = new ProductDTO();
        updateDTO.setProductName("Updated Product Name");
        updateDTO.setDescription("Updated description");
        updateDTO.setPrice(600.0);
        updateDTO.setDiscount(15.0);
        updateDTO.setQuantity(30);
        updateDTO.setSpecialPrice(510.0); // 600 - 15% = 510

        mockMvc.perform(put("/api/admin/products/{productId}", created.getProductId())
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productName", is("Updated Product Name")))
                .andExpect(jsonPath("$.price", is(600.0)));
    }

    // ==================== PAGINATION TESTS ====================

    @Test
    @Order(7)
    @DisplayName("GET /api/public/products - Should support pagination")
    void getAllProducts_WithPagination_ReturnsPaginatedResults() throws Exception {
        // Add multiple products
        for (int i = 1; i <= 5; i++) {
            ProductDTO productDTO = new ProductDTO();
            productDTO.setProductName("Pagination Test Product " + i);
            productDTO.setDescription("Product for pagination test");
            productDTO.setPrice(100.0 * i);
            productDTO.setDiscount(0.0);
            productDTO.setQuantity(10);

            mockMvc.perform(post("/api/admin/categories/{categoryId}/product", testCategory.getCategoryId())
                    .header("Authorization", "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(productDTO)))
                    .andExpect(status().isCreated());
        }

        // Test pagination - page 0, size 2
        mockMvc.perform(get("/api/public/products")
                .param("pageNumber", "0")
                .param("pageSize", "2")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.pageNumber", is(0)))
                .andExpect(jsonPath("$.pageSize", is(2)))
                .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(5)));
    }

    // ==================== SORTING TESTS ====================

    @Test
    @Order(8)
    @DisplayName("GET /api/public/products - Should support sorting")
    void getAllProducts_WithSorting_ReturnsSortedResults() throws Exception {
        // Test sorting by price descending
        mockMvc.perform(get("/api/public/products")
                .param("sortBy", "price")
                .param("sortOrder", "desc")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    // ==================== DELETE PRODUCT TESTS ====================

    @Test
    @Order(9)
    @DisplayName("DELETE /api/admin/products/{id} - Should delete product successfully")
    void deleteProduct_ValidId_ReturnsDeleted() throws Exception {
        // First, add a product
        ProductDTO productDTO = new ProductDTO();
        productDTO.setProductName("Product To Delete");
        productDTO.setDescription("This will be deleted");
        productDTO.setPrice(100.0);
        productDTO.setDiscount(0.0);
        productDTO.setQuantity(5);

        MvcResult createResult = mockMvc.perform(post("/api/admin/categories/{categoryId}/product", testCategory.getCategoryId())
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(productDTO)))
                .andExpect(status().isCreated())
                .andReturn();

        ProductDTO created = objectMapper.readValue(
            createResult.getResponse().getContentAsString(),
            ProductDTO.class
        );

        // Now delete it
        mockMvc.perform(delete("/api/admin/products/{productId}", created.getProductId())
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productName", is("Product To Delete")));
    }
}
