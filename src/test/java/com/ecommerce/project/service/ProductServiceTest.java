package com.ecommerce.project.service;

import com.ecommerce.project.exceptions.APIException;
import com.ecommerce.project.exceptions.ResourceNotFoundException;
import com.ecommerce.project.model.Cart;
import com.ecommerce.project.model.Category;
import com.ecommerce.project.model.Product;
import com.ecommerce.project.payload.ProductDTO;
import com.ecommerce.project.payload.ProductResponse;
import com.ecommerce.project.repositories.CartRepository;
import com.ecommerce.project.repositories.CategoryRepository;
import com.ecommerce.project.repositories.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartService cartService;

    @Mock
    private FileService fileService;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private ProductServiceImpl productService;

    private Product product;
    private ProductDTO productDTO;
    private Category category;

    @BeforeEach
    void setUp() {
        category = new Category();
        category.setCategoryId(1L);
        category.setCategoryName("Electronics");
        category.setProducts(new ArrayList<>());

        product = new Product();
        product.setProductId(1L);
        product.setProductName("iPhone 15");
        product.setDescription("Latest Apple smartphone");
        product.setPrice(999.99);
        product.setDiscount(10.0);
        product.setQuantity(100);
        product.setCategory(category);
        product.setImage("default.png");
        product.setSpecialPrice(899.99);

        productDTO = new ProductDTO();
        productDTO.setProductId(1L);
        productDTO.setProductName("iPhone 15");
        productDTO.setDescription("Latest Apple smartphone");
        productDTO.setPrice(999.99);
        productDTO.setDiscount(10.0);
        productDTO.setQuantity(100);
        productDTO.setSpecialPrice(899.99);
    }

    @Nested
    @DisplayName("addProduct() Tests")
    class AddProductTests {

        @Test
        @DisplayName("Should add product successfully when category exists")
        void addProduct_Success() {
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
            when(modelMapper.map(any(ProductDTO.class), eq(Product.class))).thenReturn(product);
            when(productRepository.save(any(Product.class))).thenReturn(product);
            when(modelMapper.map(any(Product.class), eq(ProductDTO.class))).thenReturn(productDTO);

            ProductDTO result = productService.addProduct(1L, productDTO);

            assertNotNull(result);
            assertEquals("iPhone 15", result.getProductName());
            assertEquals(999.99, result.getPrice());

            verify(categoryRepository, times(1)).findById(1L);
            verify(productRepository, times(1)).save(any(Product.class));
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when category not found")
        void addProduct_CategoryNotFound_ThrowsException() {
            when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

            ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> productService.addProduct(999L, productDTO)
            );

            assertEquals("Category not found with categoryId: 999", exception.getMessage());
            verify(productRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw APIException when product already exists in category")
        void addProduct_ProductAlreadyExists_ThrowsException() {
            category.setProducts(List.of(product));
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

            APIException exception = assertThrows(
                APIException.class,
                () -> productService.addProduct(1L, productDTO)
            );

            assertEquals("Product already exist!!", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("getAllProducts() Tests")
    class GetAllProductsTests {

        @Test
        @DisplayName("Should return paginated products with ascending sort")
        void getAllProducts_Success_AscendingSort() {
            List<Product> products = List.of(product);
            Page<Product> productPage = new PageImpl<>(products, PageRequest.of(0, 10), 1);

            when(productRepository.findAll(any(Pageable.class))).thenReturn(productPage);
            when(modelMapper.map(any(Product.class), eq(ProductDTO.class))).thenReturn(productDTO);

            ProductResponse result = productService.getAllProducts(0, 10, "price", "asc");

            assertNotNull(result);
            assertEquals(1, result.getContent().size());
            assertEquals(0, result.getPageNumber());
            assertEquals(10, result.getPageSize());
            assertEquals(1, result.getTotalElements());
            assertTrue(result.isLastPage());
        }

        @Test
        @DisplayName("Should return paginated products with descending sort")
        void getAllProducts_Success_DescendingSort() {
            List<Product> products = List.of(product);
            Page<Product> productPage = new PageImpl<>(products, PageRequest.of(0, 10), 1);

            when(productRepository.findAll(any(Pageable.class))).thenReturn(productPage);
            when(modelMapper.map(any(Product.class), eq(ProductDTO.class))).thenReturn(productDTO);

            ProductResponse result = productService.getAllProducts(0, 10, "price", "desc");

            assertNotNull(result);
            assertEquals(1, result.getContent().size());
        }

        @Test
        @DisplayName("Should return empty content when no products exist")
        void getAllProducts_EmptyDatabase_ReturnsEmptyContent() {
            Page<Product> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 10), 0);
            when(productRepository.findAll(any(Pageable.class))).thenReturn(emptyPage);

            ProductResponse result = productService.getAllProducts(0, 10, "price", "asc");

            assertNotNull(result);
            assertTrue(result.getContent().isEmpty());
            assertEquals(0, result.getTotalElements());
        }
    }

    @Nested
    @DisplayName("searchByCategory() Tests")
    class SearchByCategoryTests {

        @Test
        @DisplayName("Should return products for valid category")
        void searchByCategory_Success() {
            List<Product> products = List.of(product);
            Page<Product> productPage = new PageImpl<>(products, PageRequest.of(0, 10), 1);

            when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
            when(productRepository.findByCategoryOrderByPriceAsc(any(Category.class), any(Pageable.class)))
                .thenReturn(productPage);
            when(modelMapper.map(any(Product.class), eq(ProductDTO.class))).thenReturn(productDTO);

            ProductResponse result = productService.searchByCategory(1L, 0, 10, "price", "asc");

            assertNotNull(result);
            assertEquals(1, result.getContent().size());
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException for invalid category")
        void searchByCategory_CategoryNotFound_ThrowsException() {
            when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                () -> productService.searchByCategory(999L, 0, 10, "price", "asc")
            );
        }

        @Test
        @DisplayName("Should throw APIException when category has no products")
        void searchByCategory_NoProducts_ThrowsException() {
            Page<Product> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 10), 0);

            when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
            when(productRepository.findByCategoryOrderByPriceAsc(any(Category.class), any(Pageable.class)))
                .thenReturn(emptyPage);

            APIException exception = assertThrows(APIException.class,
                () -> productService.searchByCategory(1L, 0, 10, "price", "asc")
            );

            assertTrue(exception.getMessage().contains("does not have any products"));
        }
    }

    @Nested
    @DisplayName("searchProductByKeyword() Tests")
    class SearchByKeywordTests {

        @Test
        @DisplayName("Should return products matching keyword")
        void searchProductByKeyword_Success() {
            List<Product> products = List.of(product);
            Page<Product> productPage = new PageImpl<>(products, PageRequest.of(0, 10), 1);

            when(productRepository.findByProductNameLikeIgnoreCase(anyString(), any(Pageable.class)))
                .thenReturn(productPage);
            when(modelMapper.map(any(Product.class), eq(ProductDTO.class))).thenReturn(productDTO);

            ProductResponse result = productService.searchProductByKeyword("iPhone", 0, 10, "price", "asc");

            assertNotNull(result);
            assertEquals(1, result.getContent().size());
        }

        @Test
        @DisplayName("Should throw APIException when no products match keyword")
        void searchProductByKeyword_NoMatch_ThrowsException() {
            Page<Product> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 10), 0);
            when(productRepository.findByProductNameLikeIgnoreCase(anyString(), any(Pageable.class)))
                .thenReturn(emptyPage);

            APIException exception = assertThrows(APIException.class,
                () -> productService.searchProductByKeyword("NonExistent", 0, 10, "price", "asc")
            );

            assertTrue(exception.getMessage().contains("Products not found with keyword"));
        }
    }

    @Nested
    @DisplayName("updateProduct() Tests")
    class UpdateProductTests {

        @Test
        @DisplayName("Should update product successfully")
        void updateProduct_Success() {
            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
            when(modelMapper.map(any(ProductDTO.class), eq(Product.class))).thenReturn(product);
            when(productRepository.save(any(Product.class))).thenReturn(product);
            when(cartRepository.findCartsByProductId(1L)).thenReturn(Collections.emptyList());
            when(modelMapper.map(any(Product.class), eq(ProductDTO.class))).thenReturn(productDTO);

            ProductDTO result = productService.updateProduct(1L, productDTO);

            assertNotNull(result);
            verify(productRepository).save(any(Product.class));
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException for non-existent product")
        void updateProduct_NotFound_ThrowsException() {
            when(productRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                () -> productService.updateProduct(999L, productDTO)
            );
        }

        @Test
        @DisplayName("Should update product in all carts when product is updated")
        void updateProduct_UpdatesAllCarts() {
            Cart cart = new Cart();
            cart.setCartId(1L);
            cart.setCartItems(new ArrayList<>());

            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
            when(modelMapper.map(any(ProductDTO.class), eq(Product.class))).thenReturn(product);
            when(productRepository.save(any(Product.class))).thenReturn(product);
            when(cartRepository.findCartsByProductId(1L)).thenReturn(List.of(cart));
            com.ecommerce.project.payload.CartDTO cartDTO = new com.ecommerce.project.payload.CartDTO();
            cartDTO.setCartId(1L);
            when(modelMapper.map(any(Cart.class), eq(com.ecommerce.project.payload.CartDTO.class)))
                .thenReturn(cartDTO);
            when(modelMapper.map(any(Product.class), eq(ProductDTO.class))).thenReturn(productDTO);

            productService.updateProduct(1L, productDTO);

            verify(cartService).updateProductInCarts(anyLong(), eq(1L));
        }
    }

    @Nested
    @DisplayName("deleteProduct() Tests")
    class DeleteProductTests {

        @Test
        @DisplayName("Should delete product successfully")
        void deleteProduct_Success() {
            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
            when(cartRepository.findCartsByProductId(1L)).thenReturn(Collections.emptyList());
            when(modelMapper.map(any(Product.class), eq(ProductDTO.class))).thenReturn(productDTO);

            ProductDTO result = productService.deleteProduct(1L);

            assertNotNull(result);
            verify(productRepository).delete(product);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException for non-existent product")
        void deleteProduct_NotFound_ThrowsException() {
            when(productRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                () -> productService.deleteProduct(999L)
            );

            verify(productRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Should remove product from all carts before deleting")
        void deleteProduct_RemovesFromAllCarts() {
            Cart cart = new Cart();
            cart.setCartId(1L);

            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
            when(cartRepository.findCartsByProductId(1L)).thenReturn(List.of(cart));
            when(modelMapper.map(any(Product.class), eq(ProductDTO.class))).thenReturn(productDTO);

            productService.deleteProduct(1L);

            verify(cartService).deleteProductFromCart(1L, 1L);
            verify(productRepository).delete(product);
        }
    }

    @Nested
    @DisplayName("Special Price Calculation Tests")
    class SpecialPriceTests {

        @Test
        @DisplayName("Should calculate special price correctly: price - (discount% * price)")
        void specialPrice_CalculatedCorrectly() {
            ProductDTO newProductDTO = new ProductDTO();
            newProductDTO.setProductName("Test Product");
            newProductDTO.setDescription("Test Description");
            newProductDTO.setPrice(100.0);
            newProductDTO.setDiscount(20.0);
            newProductDTO.setQuantity(10);

            Product newProduct = new Product();
            newProduct.setProductId(2L);
            newProduct.setProductName("Test Product");
            newProduct.setPrice(100.0);
            newProduct.setDiscount(20.0);

            category.setProducts(new ArrayList<>());

            when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
            when(modelMapper.map(any(ProductDTO.class), eq(Product.class))).thenReturn(newProduct);
            when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
                Product saved = invocation.getArgument(0);
                assertEquals(80.0, saved.getSpecialPrice(), 0.01);
                return saved;
            });
            when(modelMapper.map(any(Product.class), eq(ProductDTO.class))).thenReturn(newProductDTO);

            productService.addProduct(1L, newProductDTO);

            verify(productRepository).save(any(Product.class));
        }
    }
}
