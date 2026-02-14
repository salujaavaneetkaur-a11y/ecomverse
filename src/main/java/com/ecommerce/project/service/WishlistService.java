package com.ecommerce.project.service;

import com.ecommerce.project.payload.WishlistDTO;

public interface WishlistService {

    WishlistDTO getWishlist();

    WishlistDTO addToWishlist(Long productId);

    WishlistDTO removeFromWishlist(Long productId);

    boolean isProductInWishlist(Long productId);

    void moveToCart(Long productId, Integer quantity);

    void clearWishlist();
}
