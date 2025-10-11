package com.Abhinav.backend.features.authentication.filter;

import com.Abhinav.backend.features.authentication.utils.JwtService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final RedisTemplate<String, Object> redisTemplate;

    public JwtAuthenticationFilter(JwtService jwtService, UserDetailsService userDetailsService, RedisTemplate<String, Object> redisTemplate) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.redisTemplate = redisTemplate;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // If no token is present, continue the filter chain for public endpoints.
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(7);

        // 1. Check if the token is on the blocklist (logged out)
        final String redisKey = "blocklist:" + jwt;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(redisKey))) {
            logger.warn("Authentication attempt with a blocklisted token for URI: {}", request.getRequestURI());
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Token is invalidated. Please log in again.");
            return;
        }

        try {
            final String userEmail = jwtService.extractUsername(jwt);

            // 2. If token is valid and user is not yet authenticated, set the security context
            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

                if (jwtService.isTokenValid(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    logger.debug("Successfully authenticated user '{}' for request URI: {}", userEmail, request.getRequestURI());
                }
            }
            // 3. Continue the filter chain for the authenticated request
            filterChain.doFilter(request, response);

        } catch (ExpiredJwtException e) {
            // THIS IS THE FIX for your original problem.
            logger.warn("JWT token has expired: {}", e.getMessage());
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Token has expired.");
        } catch (JwtException e) {
            // This catches other JWT-related issues like malformed tokens, signature errors, etc.
            logger.error("Invalid JWT token error: {}", e.getMessage());
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid token.");
        }
    }

    /**
     * Helper method to create a standardized error response.
     */
    private void sendErrorResponse(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        // Simple JSON response
        response.getWriter().write("{\"error\": \"" + message + "\"}");
        response.getWriter().flush();
    }
}