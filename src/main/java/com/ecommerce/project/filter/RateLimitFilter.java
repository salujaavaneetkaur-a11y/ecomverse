package com.ecommerce.project.filter;

import com.ecommerce.project.config.RateLimitConfig;
import com.ecommerce.project.config.RateLimitConfig.RateLimitTier;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(1)
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitConfig rateLimitConfig;

    @Autowired
    public RateLimitFilter(RateLimitConfig rateLimitConfig) {
        this.rateLimitConfig = rateLimitConfig;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        if (shouldSkipRateLimit(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientId = getClientId(request);
        RateLimitTier tier = determineRateLimitTier();

        if (rateLimitConfig.tryConsume(clientId, tier)) {
            long remaining = rateLimitConfig.getRemainingTokens(clientId, tier);
            response.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));
            response.setHeader("X-RateLimit-Limit", String.valueOf(tier.getRequests()));

            filterChain.doFilter(request, response);
        } else {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.setHeader("Retry-After", "60");
            response.setHeader("X-RateLimit-Remaining", "0");
            response.setHeader("X-RateLimit-Limit", String.valueOf(tier.getRequests()));

            response.getWriter().write(
                "{\"error\": \"Too many requests\", " +
                "\"message\": \"Rate limit exceeded. Please try again later.\", " +
                "\"retryAfter\": 60}"
            );
        }
    }

    private String getClientId(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private RateLimitTier determineRateLimitTier() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return RateLimitTier.ANONYMOUS;
        }

        boolean isPremium = auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") ||
                          a.getAuthority().equals("ROLE_PREMIUM"));

        return isPremium ? RateLimitTier.PREMIUM : RateLimitTier.AUTHENTICATED;
    }

    private boolean shouldSkipRateLimit(String path) {
        return path.startsWith("/actuator") ||
               path.startsWith("/swagger") ||
               path.startsWith("/v3/api-docs") ||
               path.equals("/favicon.ico") ||
               path.startsWith("/images/");
    }
}
