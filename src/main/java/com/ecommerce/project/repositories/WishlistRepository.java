package com.ecommerce.project.repositories;

import com.ecommerce.project.model.User;
import com.ecommerce.project.model.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, Long> {

    Optional<Wishlist> findByUser(User user);

    @Query("SELECT w FROM Wishlist w LEFT JOIN FETCH w.items i LEFT JOIN FETCH i.product WHERE w.user = :user")
    Optional<Wishlist> findByUserWithItems(@Param("user") User user);

    boolean existsByUser(User user);
}
