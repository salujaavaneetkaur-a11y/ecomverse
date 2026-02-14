package com.ecommerce.project.repositories;

import com.ecommerce.project.model.RefreshToken;
import com.ecommerce.project.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

/**
 * Refresh Token Repository
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * Find refresh token by token string
     */
    Optional<RefreshToken> findByToken(String token);

    /**
     * Find refresh token by user
     */
    Optional<RefreshToken> findByUser(User user);

    /**
     * Delete refresh token by user (for logout)
     */
    @Modifying
    void deleteByUser(User user);

    /**
     * Delete all expired tokens (cleanup job)
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiryDate < :now")
    void deleteAllExpiredTokens(Instant now);
}
