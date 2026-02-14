package com.ecommerce.project.repositories;

import com.ecommerce.project.model.WishlistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WishlistItemRepository extends JpaRepository<WishlistItem, Long> {

    @Query("SELECT wi FROM WishlistItem wi WHERE wi.wishlist.wishlistId = :wishlistId AND wi.product.productId = :productId")
    Optional<WishlistItem> findByWishlistIdAndProductId(@Param("wishlistId") Long wishlistId, @Param("productId") Long productId);

    boolean existsByWishlistWishlistIdAndProductProductId(Long wishlistId, Long productId);

    @Modifying
    @Query("DELETE FROM WishlistItem wi WHERE wi.wishlist.wishlistId = :wishlistId AND wi.product.productId = :productId")
    void deleteByWishlistIdAndProductId(@Param("wishlistId") Long wishlistId, @Param("productId") Long productId);

    long countByWishlistWishlistId(Long wishlistId);
}
