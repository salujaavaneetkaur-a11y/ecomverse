package com.ecommerce.project.service;

import com.ecommerce.project.exceptions.APIException;
import com.ecommerce.project.exceptions.ResourceNotFoundException;
import com.ecommerce.project.model.PasswordResetToken;
import com.ecommerce.project.model.User;
import com.ecommerce.project.repositories.PasswordResetTokenRepository;
import com.ecommerce.project.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@Transactional
public class PasswordResetService {

    @Value("${app.password-reset.expiration-ms:3600000}")
    private Long tokenExpirationMs;

    @Autowired
    private PasswordResetTokenRepository tokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public String createPasswordResetToken(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        tokenRepository.findByUser(user)
                .ifPresent(existingToken -> tokenRepository.delete(existingToken));

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUser(user);
        resetToken.setToken(UUID.randomUUID().toString());
        resetToken.setExpiryDate(Instant.now().plusMillis(tokenExpirationMs));
        resetToken.setUsed(false);

        tokenRepository.save(resetToken);

        return resetToken.getToken();
    }

    public boolean validateToken(String token) {
        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new APIException("Invalid password reset token"));

        if (resetToken.isExpired()) {
            tokenRepository.delete(resetToken);
            throw new APIException("Password reset token has expired. Please request a new one.");
        }

        if (resetToken.isUsed()) {
            throw new APIException("Password reset token has already been used.");
        }

        return true;
    }

    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new APIException("Invalid password reset token"));

        if (!resetToken.isValid()) {
            throw new APIException("Password reset token is invalid or expired");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        resetToken.setUsed(true);
        tokenRepository.save(resetToken);
    }

    public User getUserByToken(String token) {
        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new APIException("Invalid password reset token"));
        return resetToken.getUser();
    }

    public void deleteExpiredTokens() {
        tokenRepository.deleteAllExpiredTokens(Instant.now());
    }
}
