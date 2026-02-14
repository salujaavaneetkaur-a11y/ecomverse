package com.ecommerce.project.service;

import com.ecommerce.project.exceptions.APIException;
import com.ecommerce.project.exceptions.ResourceNotFoundException;
import com.ecommerce.project.model.Category;
import com.ecommerce.project.payload.CategoryDTO;
import com.ecommerce.project.payload.CategoryResponse;
import com.ecommerce.project.repositories.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    private Category category;
    private CategoryDTO categoryDTO;

    @BeforeEach
    void setUp() {
        category = new Category();
        category.setCategoryId(1L);
        category.setCategoryName("Electronics");
        category.setProducts(Collections.emptyList());

        categoryDTO = new CategoryDTO();
        categoryDTO.setCategoryId(1L);
        categoryDTO.setCategoryName("Electronics");
    }

    @Nested
    @DisplayName("getAllCategories() Tests")
    class GetAllCategoriesTests {

        @Test
        @DisplayName("Should return paginated categories successfully")
        void getAllCategories_Success() {
            List<Category> categories = List.of(category);
            Page<Category> categoryPage = new PageImpl<>(categories, PageRequest.of(0, 10), 1);

            when(categoryRepository.findAll(any(Pageable.class))).thenReturn(categoryPage);
            when(modelMapper.map(any(Category.class), eq(CategoryDTO.class))).thenReturn(categoryDTO);

            CategoryResponse result = categoryService.getAllCategories(0, 10, "categoryName", "asc");

            assertNotNull(result);
            assertEquals(1, result.getContent().size());
            assertEquals(0, result.getPageNumber());
            assertEquals(10, result.getPageSize());
            assertEquals(1, result.getTotalElements());
            assertTrue(result.isLastPage());
        }

        @Test
        @DisplayName("Should throw APIException when no categories exist")
        void getAllCategories_EmptyDatabase_ThrowsException() {
            Page<Category> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 10), 0);
            when(categoryRepository.findAll(any(Pageable.class))).thenReturn(emptyPage);

            APIException exception = assertThrows(APIException.class,
                () -> categoryService.getAllCategories(0, 10, "categoryName", "asc")
            );

            assertEquals("No category created till now.", exception.getMessage());
        }

        @Test
        @DisplayName("Should handle descending sort order")
        void getAllCategories_DescendingSort() {
            List<Category> categories = List.of(category);
            Page<Category> categoryPage = new PageImpl<>(categories, PageRequest.of(0, 10), 1);

            when(categoryRepository.findAll(any(Pageable.class))).thenReturn(categoryPage);
            when(modelMapper.map(any(Category.class), eq(CategoryDTO.class))).thenReturn(categoryDTO);

            CategoryResponse result = categoryService.getAllCategories(0, 10, "categoryName", "desc");

            assertNotNull(result);
            verify(categoryRepository).findAll(any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("createCategory() Tests")
    class CreateCategoryTests {

        @Test
        @DisplayName("Should create category successfully when name is unique")
        void createCategory_Success() {
            when(modelMapper.map(categoryDTO, Category.class)).thenReturn(category);
            when(categoryRepository.findByCategoryName("Electronics")).thenReturn(null);
            when(categoryRepository.save(any(Category.class))).thenReturn(category);
            when(modelMapper.map(category, CategoryDTO.class)).thenReturn(categoryDTO);

            CategoryDTO result = categoryService.createCategory(categoryDTO);

            assertNotNull(result);
            assertEquals("Electronics", result.getCategoryName());
            verify(categoryRepository).save(any(Category.class));
        }

        @Test
        @DisplayName("Should throw APIException when category name already exists")
        void createCategory_DuplicateName_ThrowsException() {
            when(modelMapper.map(categoryDTO, Category.class)).thenReturn(category);
            when(categoryRepository.findByCategoryName("Electronics")).thenReturn(category);

            APIException exception = assertThrows(APIException.class,
                () -> categoryService.createCategory(categoryDTO)
            );

            assertTrue(exception.getMessage().contains("already exists"));
            verify(categoryRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("updateCategory() Tests")
    class UpdateCategoryTests {

        @Test
        @DisplayName("Should update category successfully")
        void updateCategory_Success() {
            CategoryDTO updatedDTO = new CategoryDTO();
            updatedDTO.setCategoryName("Updated Electronics");

            Category updatedCategory = new Category();
            updatedCategory.setCategoryId(1L);
            updatedCategory.setCategoryName("Updated Electronics");

            when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
            when(modelMapper.map(updatedDTO, Category.class)).thenReturn(updatedCategory);
            when(categoryRepository.save(any(Category.class))).thenReturn(updatedCategory);
            when(modelMapper.map(any(Category.class), eq(CategoryDTO.class))).thenReturn(updatedDTO);

            CategoryDTO result = categoryService.updateCategory(updatedDTO, 1L);

            assertNotNull(result);
            assertEquals("Updated Electronics", result.getCategoryName());
            verify(categoryRepository).save(any(Category.class));
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException for non-existent category")
        void updateCategory_NotFound_ThrowsException() {
            when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                () -> categoryService.updateCategory(categoryDTO, 999L)
            );
        }
    }

    @Nested
    @DisplayName("deleteCategory() Tests")
    class DeleteCategoryTests {

        @Test
        @DisplayName("Should delete category successfully")
        void deleteCategory_Success() {
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
            when(modelMapper.map(category, CategoryDTO.class)).thenReturn(categoryDTO);

            CategoryDTO result = categoryService.deleteCategory(1L);

            assertNotNull(result);
            assertEquals("Electronics", result.getCategoryName());
            verify(categoryRepository).delete(category);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException for non-existent category")
        void deleteCategory_NotFound_ThrowsException() {
            when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                () -> categoryService.deleteCategory(999L)
            );

            verify(categoryRepository, never()).delete(any());
        }
    }
}
