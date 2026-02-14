package com.ecommerce.project.controller;

import com.ecommerce.project.payload.APIResponse;
import com.ecommerce.project.payload.WishlistDTO;
import com.ecommerce.project.service.WishlistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Wishlist Controller
 *
 * ============================================================
 * 🎓 WISHLIST API DESIGN:
 * ============================================================
 *
 * User-centric resource (implied user from auth):
 * GET /api/wishlist - Get my wishlist
 * POST /api/wishlist/products/{id} - Add product
 * DELETE /api/wishlist/products/{id} - Remove product
 *
 * ============================================================
 * 📋 INTERVIEW TIP:
 * "The wishlist API is user-centric - we infer the user from
 * the JWT token. This keeps URLs clean and prevents users
 * from accessing each other's wishlists."
 * ============================================================
 */
@RestController
@RequestMapping("/api/wishlist")
@PreAuthorize("hasRole('USER')")
@Tag(name = "Wishlist", description = "User wishlist management")
public class WishlistController {

    @Autowired
    private WishlistService wishlistService;

    /**
     * Get current user's wishlist
     */
    @GetMapping
    @Operation(summary = "Get wishlist", description = "Get current user's wishlist with all items")
    public ResponseEntity<WishlistDTO> getWishlist() {
        WishlistDTO wishlist = wishlistService.getWishlist();
        return ResponseEntity.ok(wishlist);
    }

    /**
     * Add product to wishlist
     */
    @PostMapping("/products/{productId}")
    @Operation(summary = "Add to wishlist", description = "Add a product to your wishlist")
    public ResponseEntity<WishlistDTO> addToWishlist(@PathVariable Long productId) {
        WishlistDTO wishlist = wishlistService.addToWishlist(productId);
        return new ResponseEntity<>(wishlist, HttpStatus.CREATED);
    }

    /**
     * Remove product from wishlist
     */
    @DeleteMapping("/products/{productId}")
    @Operation(summary = "Remove from wishlist", description = "Remove a product from your wishlist")
    public ResponseEntity<WishlistDTO> removeFromWishlist(@PathVariable Long productId) {
        WishlistDTO wishlist = wishlistService.removeFromWishlist(productId);
        return ResponseEntity.ok(wishlist);
    }

    /**
     * Check if product is in wishlist
     */
    @GetMapping("/products/{productId}/check")
    @Operation(summary = "Check if in wishlist", description = "Check if a product is in your wishlist")
    public ResponseEntity<Map<String, Boolean>> isInWishlist(@PathVariable Long productId) {
        boolean inWishlist = wishlistService.isProductInWishlist(productId);
        return ResponseEntity.ok(Map.of("inWishlist", inWishlist));
    }

    /**
     * Move product from wishlist to cart
     */
    @PostMapping("/products/{productId}/move-to-cart")
    @Operation(summary = "Move to cart", description = "Move a product from wishlist to cart")
    public ResponseEntity<APIResponse> moveToCart(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "1") Integer quantity) {

        wishlistService.moveToCart(productId, quantity);
        return ResponseEntity.ok(new APIResponse("Product moved to cart successfully", true));
    }

    /**
     * Clear entire wishlist
     */
    @DeleteMapping
    @Operation(summary = "Clear wishlist", description = "Remove all items from your wishlist")
    public ResponseEntity<APIResponse> clearWishlist() {
        wishlistService.clearWishlist();
        return ResponseEntity.ok(new APIResponse("Wishlist cleared successfully", true));
    }
}
