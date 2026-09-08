package com.deepanshu.taskmanager.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory rate limiter using Bucket4j (token-bucket algorithm).
 * Limits /api/v1/auth/** endpoints to 10 requests/minute per IP.
 * Prevents brute-force password attacks on login/register endpoints.
 *
 * In production, replace the ConcurrentHashMap store with Redis
 * using bucket4j-redis for distributed rate limiting across instances.
 */
@Component
@Slf4j
public class RateLimitingFilter extends OncePerRequestFilter {

    // 10 requests per minute per IP on auth endpoints
    private static final int MAX_REQUESTS_PER_MINUTE = 10;
    private static final String AUTH_PATH = "/api/v1/auth/";

    // In-memory bucket store — swap for Redis in production
    private final Map<String, Bucket> bucketCache = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        if (!request.getRequestURI().startsWith(AUTH_PATH)) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = extractClientIp(request);
        Bucket bucket = bucketCache.computeIfAbsent(clientIp, this::newBucket);

        if (bucket.tryConsume(1)) {
            response.addHeader("X-Rate-Limit-Remaining",
                String.valueOf(bucket.getAvailableTokens()));
            filterChain.doFilter(request, response);
        } else {
            log.warn("Rate limit exceeded for IP: {} on {}", clientIp, request.getRequestURI());
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.addHeader("X-Rate-Limit-Retry-After-Seconds", "60");
            response.getWriter().write("""
                {
                  "status": 429,
                  "error": "Too Many Requests",
                  "message": "Rate limit exceeded. Max %d requests/minute. Try again in 60 seconds.",
                  "path": "%s"
                }
                """.formatted(MAX_REQUESTS_PER_MINUTE, request.getRequestURI()));
        }
    }

    private Bucket newBucket(String ip) {
        return Bucket.builder()
            .addLimit(Bandwidth.builder()
                .capacity(MAX_REQUESTS_PER_MINUTE)
                .refillIntervally(MAX_REQUESTS_PER_MINUTE, Duration.ofMinutes(1))
                .build())
            .build();
    }

    private String extractClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
