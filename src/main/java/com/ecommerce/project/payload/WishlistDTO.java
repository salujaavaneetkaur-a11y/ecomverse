package com.ecommerce.project.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Wishlist DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WishlistDTO {

    private Long wishlistId;
    private Long userId;
    private List<WishlistItemDTO> items = new ArrayList<>();
    private int totalItems;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WishlistItemDTO {
        private Long id;
        private Long productId;
        private String productName;
        private String productImage;
        private Double currentPrice;
        private Double priceWhenAdded;
        private Double priceDifference; // Positive = price increased, Negative = price dropped
        private Boolean inStock;
        private LocalDateTime addedAt;
    }
}
