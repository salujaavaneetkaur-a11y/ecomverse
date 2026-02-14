package com.ecommerce.project.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
@Component
public class RateLimitConfig {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public enum RateLimitTier {
        ANONYMOUS(20, Duration.ofMinutes(1)),
        AUTHENTICATED(100, Duration.ofMinutes(1)),
        PREMIUM(500, Duration.ofMinutes(1));

        private final int requests;
        private final Duration duration;

        RateLimitTier(int requests, Duration duration) {
            this.requests = requests;
            this.duration = duration;
        }

        public int getRequests() {
            return requests;
        }

        public Duration getDuration() {
            return duration;
        }
    }

    public Bucket resolveBucket(String clientId, RateLimitTier tier) {
        return buckets.computeIfAbsent(clientId + "_" + tier.name(),
            key -> createBucket(tier));
    }

    private Bucket createBucket(RateLimitTier tier) {
        Bandwidth limit = Bandwidth.classic(
            tier.getRequests(),
            Refill.greedy(tier.getRequests(), tier.getDuration())
        );

        return Bucket.builder()
            .addLimit(limit)
            .build();
    }

    public boolean tryConsume(String clientId, RateLimitTier tier) {
        Bucket bucket = resolveBucket(clientId, tier);
        return bucket.tryConsume(1);
    }

    public long getRemainingTokens(String clientId, RateLimitTier tier) {
        Bucket bucket = resolveBucket(clientId, tier);
        return bucket.getAvailableTokens();
    }
}
