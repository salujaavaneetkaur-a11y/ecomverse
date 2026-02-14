package com.ecommerce.project.controller;

import com.ecommerce.project.payload.ReviewDTO;
import com.ecommerce.project.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Review Controller - Product Reviews and Ratings API
 *
 * ============================================================
 * 🎓 REST API DESIGN BEST PRACTICES:
 * ============================================================
 *
 * 1. NESTED RESOURCES
 *    Reviews belong to products, so:
 *    POST /api/products/{productId}/reviews (create review for product)
 *    GET /api/products/{productId}/reviews (get product reviews)
 *
 * 2. PROPER HTTP METHODS
 *    POST = Create
 *    GET = Read
 *    PUT = Update
 *    DELETE = Delete
 *
 * 3. STATUS CODES
 *    201 = Created
 *    200 = OK
 *    204 = No Content (delete)
 *    400 = Bad Request
 *    404 = Not Found
 *
 * ============================================================
 * 📋 INTERVIEW TIP:
 * "I follow RESTful conventions with nested resources for
 * relationships. The review API uses proper HTTP methods
 * and status codes, with Swagger documentation for all endpoints."
 * ============================================================
 */
@RestController
@RequestMapping("/api")
@Tag(name = "Reviews", description = "Product Reviews and Ratings API")
public class ReviewController {

    @Autowired
    private ReviewService reviewService;

    /**
     * Create a new review for a product
     * Only authenticated users can create reviews
     */
    @PostMapping("/products/{productId}/reviews")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Create a review", description = "Create a new review for a product (one per user)")
    public ResponseEntity<ReviewDTO> createReview(
            @PathVariable Long productId,
            @Valid @RequestBody ReviewDTO reviewDTO) {

        ReviewDTO createdReview = reviewService.createReview(productId, reviewDTO);
        return new ResponseEntity<>(createdReview, HttpStatus.CREATED);
    }

    /**
     * Get all reviews for a product with pagination
     * Public endpoint - no authentication required
     */
    @GetMapping("/public/products/{productId}/reviews")
    @Operation(summary = "Get product reviews", description = "Get all reviews for a product with pagination")
    public ResponseEntity<Page<ReviewDTO>> getProductReviews(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "newest") String sortBy) {

        Page<ReviewDTO> reviews = reviewService.getProductReviews(productId, page, size, sortBy);
        return ResponseEntity.ok(reviews);
    }

    /**
     * Get review statistics for a product
     * Returns average rating, total reviews, and rating distribution
     */
    @GetMapping("/public/products/{productId}/reviews/stats")
    @Operation(summary = "Get review statistics", description = "Get average rating and rating distribution")
    public ResponseEntity<Map<String, Object>> getProductReviewStats(
            @PathVariable Long productId) {

        Map<String, Object> stats = reviewService.getProductReviewStats(productId);
        return ResponseEntity.ok(stats);
    }

    /**
     * Get a specific review by ID
     */
    @GetMapping("/public/reviews/{reviewId}")
    @Operation(summary = "Get a review", description = "Get a specific review by ID")
    public ResponseEntity<ReviewDTO> getReviewById(@PathVariable Long reviewId) {
        ReviewDTO review = reviewService.getReviewById(reviewId);
        return ResponseEntity.ok(review);
    }

    /**
     * Update an existing review
     * Only the author can update their review
     */
    @PutMapping("/reviews/{reviewId}")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Update a review", description = "Update your own review")
    public ResponseEntity<ReviewDTO> updateReview(
            @PathVariable Long reviewId,
            @Valid @RequestBody ReviewDTO reviewDTO) {

        ReviewDTO updatedReview = reviewService.updateReview(reviewId, reviewDTO);
        return ResponseEntity.ok(updatedReview);
    }

    /**
     * Delete a review
     * Author or admin can delete reviews
     */
    @DeleteMapping("/reviews/{reviewId}")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Delete a review", description = "Delete your own review (or any review if admin)")
    public ResponseEntity<Void> deleteReview(@PathVariable Long reviewId) {
        reviewService.deleteReview(reviewId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Mark a review as helpful
     * Authenticated users can vote
     */
    @PostMapping("/reviews/{reviewId}/helpful")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Mark review helpful", description = "Vote that a review was helpful")
    public ResponseEntity<Void> markReviewHelpful(@PathVariable Long reviewId) {
        reviewService.markReviewHelpful(reviewId);
        return ResponseEntity.ok().build();
    }

    /**
     * Get current user's review for a product
     */
    @GetMapping("/products/{productId}/reviews/me")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Get my review", description = "Get your review for a specific product")
    public ResponseEntity<ReviewDTO> getMyReview(@PathVariable Long productId) {
        ReviewDTO review = reviewService.getUserReviewForProduct(productId);
        if (review == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(review);
    }

    /**
     * Check if current user has reviewed a product
     */
    @GetMapping("/products/{productId}/reviews/check")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Check if reviewed", description = "Check if you have reviewed this product")
    public ResponseEntity<Map<String, Boolean>> hasUserReviewed(@PathVariable Long productId) {
        boolean hasReviewed = reviewService.hasUserReviewedProduct(productId);
        return ResponseEntity.ok(Map.of("hasReviewed", hasReviewed));
    }
}
