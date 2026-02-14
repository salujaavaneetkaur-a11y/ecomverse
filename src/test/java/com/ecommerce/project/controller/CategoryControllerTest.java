package com.ecommerce.project.controller;

import com.ecommerce.project.payload.CategoryResponse;
import com.ecommerce.project.service.CategoryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class CategoryControllerTest {

    @Mock
    private CategoryService categoryService;

    @InjectMocks
    private CategoryController categoryController;

    @Test
    void testGetAllCategories_ReturnsCategoryResponse() {
        CategoryResponse mockResponse = new CategoryResponse();
        Mockito.when(categoryService.getAllCategories(anyInt(), anyInt(), anyString(), anyString()))
                .thenReturn(mockResponse);

        ResponseEntity<CategoryResponse> response = categoryController.getAllCategories(0, 10, "name", "asc");

        assertEquals(200, response.getStatusCode().value());
        assertEquals(mockResponse, response.getBody());
    }

    @Test
    void testGetAllCategories_WithNullParams_UsesDefaults() {
        CategoryResponse mockResponse = new CategoryResponse();
        Mockito.when(categoryService.getAllCategories(any(), any(), any(), any()))
                .thenReturn(mockResponse);

        ResponseEntity<CategoryResponse> response = categoryController.getAllCategories(null, null, null, null);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(mockResponse, response.getBody());
    }
}
