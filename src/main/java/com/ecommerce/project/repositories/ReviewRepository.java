package com.ecommerce.project.repositories;

import com.ecommerce.project.model.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Review Repository with Custom Queries
 *
 * ============================================================
 * 🎓 CUSTOM QUERIES EXPLAINED:
 * ============================================================
 *
 * Spring Data JPA generates queries from method names:
 * - findByProductProductId -> SELECT * FROM reviews WHERE product_id = ?
 *
 * For complex queries, use @Query annotation with JPQL or native SQL.
 *
 * ============================================================
 * 📋 INTERVIEW TIP:
 * "I use derived queries for simple cases and @Query for
 * complex aggregations. For the average rating calculation,
 * I use a native query for performance optimization."
 * ============================================================
 */
@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    /**
     * Find all reviews for a product with pagination
     */
    Page<Review> findByProductProductId(Long productId, Pageable pageable);

    /**
     * Find all reviews by a user
     */
    List<Review> findByUserUserId(Long userId);

    /**
     * Check if user already reviewed this product
     */
    boolean existsByUserUserIdAndProductProductId(Long userId, Long productId);

    /**
     * Find user's review for a specific product
     */
    Optional<Review> findByUserUserIdAndProductProductId(Long userId, Long productId);

    /**
     * Calculate average rating for a product
     */
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.product.productId = :productId")
    Double getAverageRatingByProductId(@Param("productId") Long productId);

    /**
     * Count reviews for a product
     */
    long countByProductProductId(Long productId);

    /**
     * Get rating distribution for a product (how many 1-star, 2-star, etc.)
     */
    @Query("SELECT r.rating, COUNT(r) FROM Review r WHERE r.product.productId = :productId GROUP BY r.rating ORDER BY r.rating DESC")
    List<Object[]> getRatingDistributionByProductId(@Param("productId") Long productId);

    /**
     * Find most helpful reviews for a product
     */
    @Query("SELECT r FROM Review r WHERE r.product.productId = :productId ORDER BY r.helpfulVotes DESC, r.createdAt DESC")
    Page<Review> findMostHelpfulByProductId(@Param("productId") Long productId, Pageable pageable);

    /**
     * Find verified purchase reviews only
     */
    Page<Review> findByProductProductIdAndVerifiedPurchaseTrue(Long productId, Pageable pageable);
}
