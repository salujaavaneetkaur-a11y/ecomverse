package com.ecommerce.project.controller;

import com.ecommerce.project.exceptions.ResourceNotFoundException;
import com.ecommerce.project.payload.ProductDTO;
import com.ecommerce.project.payload.ProductResponse;
import com.ecommerce.project.service.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller Tests for ProductController
 *
 * ============================================================
 * 🎓 @WebMvcTest vs @SpringBootTest:
 * ============================================================
 *
 * @WebMvcTest (What we use here):
 * - Loads ONLY web layer (Controller + Security)
 * - Mocks service layer
 * - Very fast (< 1 second)
 * - Tests: Request mapping, validation, serialization
 *
 * @SpringBootTest:
 * - Loads ENTIRE application
 * - Can use real or mocked dependencies
 * - Slower (several seconds)
 * - Tests: Full integration
 *
 * ============================================================
 * 📋 INTERVIEW TIP:
 * "I use @WebMvcTest for controller tests because:
 * 1. Fast execution (no DB, no services)
 * 2. Tests HTTP layer in isolation
 * 3. Verifies request/response mapping
 * 4. Validates input validation annotations"
 * ============================================================
 */
@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * @MockBean creates a Mockito mock AND registers it in Spring context
     * Different from @Mock which doesn't register in Spring
     */
    @MockBean
    private ProductService productService;

    private ProductDTO productDTO;
    private ProductResponse productResponse;

    @BeforeEach
    void setUp() {
        productDTO = new ProductDTO();
        productDTO.setProductId(1L);
        productDTO.setProductName("Test Product");
        productDTO.setDescription("Test Description");
        productDTO.setPrice(99.99);
        productDTO.setDiscount(10.0);
        productDTO.setQuantity(50);
        productDTO.setSpecialPrice(89.99);
        productDTO.setImage("test.png");

        productResponse = new ProductResponse();
        productResponse.setContent(List.of(productDTO));
        productResponse.setPageNumber(0);
        productResponse.setPageSize(10);
        productResponse.setTotalElements(1L);
        productResponse.setTotalPages(1);
        productResponse.setLastPage(true);
    }

    // ==================== GET ALL PRODUCTS TESTS ====================
    @Nested
    @DisplayName("GET /api/public/products")
    class GetAllProductsTests {

        @Test
        @DisplayName("Should return products with default pagination")
        void getAllProducts_DefaultPagination_ReturnsOk() throws Exception {
            when(productService.getAllProducts(0, 10, "productId", "asc"))
                .thenReturn(productResponse);

            mockMvc.perform(get("/api/public/products")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.pageNumber", is(0)))
                    .andExpect(jsonPath("$.pageSize", is(10)));
        }

        @Test
        @DisplayName("Should return products with custom pagination")
        void getAllProducts_CustomPagination_ReturnsOk() throws Exception {
            when(productService.getAllProducts(1, 5, "price", "desc"))
                .thenReturn(productResponse);

            mockMvc.perform(get("/api/public/products")
                    .param("pageNumber", "1")
                    .param("pageSize", "5")
                    .param("sortBy", "price")
                    .param("sortOrder", "desc")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());

            verify(productService).getAllProducts(1, 5, "price", "desc");
        }

        @Test
        @DisplayName("Should return empty list when no products")
        void getAllProducts_EmptyDatabase_ReturnsEmptyList() throws Exception {
            ProductResponse emptyResponse = new ProductResponse();
            emptyResponse.setContent(Collections.emptyList());
            emptyResponse.setPageNumber(0);
            emptyResponse.setPageSize(10);
            emptyResponse.setTotalElements(0L);
            emptyResponse.setTotalPages(0);
            emptyResponse.setLastPage(true);

            when(productService.getAllProducts(anyInt(), anyInt(), anyString(), anyString()))
                .thenReturn(emptyResponse);

            mockMvc.perform(get("/api/public/products"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(0)))
                    .andExpect(jsonPath("$.totalElements", is(0)));
        }
    }

    // ==================== ADD PRODUCT TESTS ====================
    @Nested
    @DisplayName("POST /api/admin/categories/{categoryId}/product")
    class AddProductTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should add product when admin is authenticated")
        void addProduct_AdminAuth_ReturnsCreated() throws Exception {
            when(productService.addProduct(eq(1L), any(ProductDTO.class)))
                .thenReturn(productDTO);

            mockMvc.perform(post("/api/admin/categories/{categoryId}/product", 1L)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(productDTO)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.productName", is("Test Product")))
                    .andExpect(jsonPath("$.price", is(99.99)));
        }

        @Test
        @DisplayName("Should return 401 when not authenticated")
        void addProduct_NoAuth_ReturnsUnauthorized() throws Exception {
            mockMvc.perform(post("/api/admin/categories/{categoryId}/product", 1L)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(productDTO)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Should return 403 when user is not admin")
        void addProduct_UserRole_ReturnsForbidden() throws Exception {
            mockMvc.perform(post("/api/admin/categories/{categoryId}/product", 1L)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(productDTO)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should return 404 when category not found")
        void addProduct_CategoryNotFound_ReturnsNotFound() throws Exception {
            when(productService.addProduct(eq(999L), any(ProductDTO.class)))
                .thenThrow(new ResourceNotFoundException("Category", "categoryId", 999L));

            mockMvc.perform(post("/api/admin/categories/{categoryId}/product", 999L)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(productDTO)))
                    .andExpect(status().isNotFound());
        }
    }

    // ==================== SEARCH BY CATEGORY TESTS ====================
    @Nested
    @DisplayName("GET /api/public/categories/{categoryId}/products")
    class SearchByCategoryTests {

        @Test
        @DisplayName("Should return products for valid category")
        void getProductsByCategory_ValidCategory_ReturnsOk() throws Exception {
            when(productService.searchByCategory(eq(1L), anyInt(), anyInt(), anyString(), anyString()))
                .thenReturn(productResponse);

            mockMvc.perform(get("/api/public/categories/{categoryId}/products", 1L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)));
        }

        @Test
        @DisplayName("Should return 404 for invalid category")
        void getProductsByCategory_InvalidCategory_ReturnsNotFound() throws Exception {
            when(productService.searchByCategory(eq(999L), anyInt(), anyInt(), anyString(), anyString()))
                .thenThrow(new ResourceNotFoundException("Category", "categoryId", 999L));

            mockMvc.perform(get("/api/public/categories/{categoryId}/products", 999L))
                    .andExpect(status().isNotFound());
        }
    }

    // ==================== SEARCH BY KEYWORD TESTS ====================
    @Nested
    @DisplayName("GET /api/public/products/keyword/{keyword}")
    class SearchByKeywordTests {

        @Test
        @DisplayName("Should return matching products for keyword")
        void getProductsByKeyword_ValidKeyword_ReturnsFound() throws Exception {
            when(productService.searchProductByKeyword(eq("test"), anyInt(), anyInt(), anyString(), anyString()))
                .thenReturn(productResponse);

            mockMvc.perform(get("/api/public/products/keyword/{keyword}", "test"))
                    .andExpect(status().isFound())
                    .andExpect(jsonPath("$.content", hasSize(1)));
        }
    }

    // ==================== UPDATE PRODUCT TESTS ====================
    @Nested
    @DisplayName("PUT /api/admin/products/{productId}")
    class UpdateProductTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should update product when admin is authenticated")
        void updateProduct_AdminAuth_ReturnsOk() throws Exception {
            ProductDTO updatedDTO = new ProductDTO();
            updatedDTO.setProductId(1L);
            updatedDTO.setProductName("Updated Product");
            updatedDTO.setDescription("Updated Description");
            updatedDTO.setPrice(149.99);
            updatedDTO.setDiscount(15.0);
            updatedDTO.setQuantity(75);

            when(productService.updateProduct(eq(1L), any(ProductDTO.class)))
                .thenReturn(updatedDTO);

            mockMvc.perform(put("/api/admin/products/{productId}", 1L)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updatedDTO)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.productName", is("Updated Product")))
                    .andExpect(jsonPath("$.price", is(149.99)));
        }

        @Test
        @DisplayName("Should return 401 when not authenticated")
        void updateProduct_NoAuth_ReturnsUnauthorized() throws Exception {
            mockMvc.perform(put("/api/admin/products/{productId}", 1L)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(productDTO)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should return 404 when product not found")
        void updateProduct_NotFound_ReturnsNotFound() throws Exception {
            when(productService.updateProduct(eq(999L), any(ProductDTO.class)))
                .thenThrow(new ResourceNotFoundException("Product", "productId", 999L));

            mockMvc.perform(put("/api/admin/products/{productId}", 999L)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(productDTO)))
                    .andExpect(status().isNotFound());
        }
    }

    // ==================== DELETE PRODUCT TESTS ====================
    @Nested
    @DisplayName("DELETE /api/admin/products/{productId}")
    class DeleteProductTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should delete product when admin is authenticated")
        void deleteProduct_AdminAuth_ReturnsOk() throws Exception {
            when(productService.deleteProduct(1L)).thenReturn(productDTO);

            mockMvc.perform(delete("/api/admin/products/{productId}", 1L)
                    .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.productName", is("Test Product")));
        }

        @Test
        @DisplayName("Should return 401 when not authenticated")
        void deleteProduct_NoAuth_ReturnsUnauthorized() throws Exception {
            mockMvc.perform(delete("/api/admin/products/{productId}", 1L))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Should return 403 when user is not admin")
        void deleteProduct_UserRole_ReturnsForbidden() throws Exception {
            mockMvc.perform(delete("/api/admin/products/{productId}", 1L)
                    .with(csrf()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should return 404 when product not found")
        void deleteProduct_NotFound_ReturnsNotFound() throws Exception {
            when(productService.deleteProduct(999L))
                .thenThrow(new ResourceNotFoundException("Product", "productId", 999L));

            mockMvc.perform(delete("/api/admin/products/{productId}", 999L)
                    .with(csrf()))
                    .andExpect(status().isNotFound());
        }
    }
}
