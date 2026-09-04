package com.example.food.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.food.user.User;
import com.example.food.user.UserMapper;
import java.io.IOException;
import java.util.List;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final AuthenticationEntryPoint authenticationEntryPoint;
    private final UserMapper userMapper;

    public JwtAuthenticationFilter(JwtService jwtService, AuthenticationEntryPoint authenticationEntryPoint) {
        this(jwtService, authenticationEntryPoint, null);
    }

    public JwtAuthenticationFilter(
            JwtService jwtService,
            AuthenticationEntryPoint authenticationEntryPoint,
            UserMapper userMapper
    ) {
        this.jwtService = jwtService;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.userMapper = userMapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (!StringUtils.hasText(header) || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);
        if (!StringUtils.hasText(token)) {
            reject(request, response, new BadCredentialsException("Invalid JWT token"));
            return;
        }

        try {
            AuthPrincipal principal = jwtService.parseToken(token);
            if (!isCurrentUserSession(principal)) {
                reject(request, response, new BadCredentialsException("User session is no longer valid"));
                return;
            }
            SecurityContextHolder.getContext().setAuthentication(authentication(principal));
        } catch (AuthenticationException exception) {
            reject(request, response, exception);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean isCurrentUserSession(AuthPrincipal principal) {
        if (principal.role() != AppRole.USER || principal.authVersion() == null || userMapper == null) {
            // Tokens issued before V20 omit authVersion and remain a legacy-compatible session.
            return true;
        }
        User user = userMapper.selectById(principal.id());
        int currentVersion = user == null || user.getAuthVersion() == null ? -1 : user.getAuthVersion();
        return user != null
                && Boolean.TRUE.equals(user.getEnabled())
                && user.getDeletedAt() == null
                && currentVersion == principal.authVersion();
    }

    private UsernamePasswordAuthenticationToken authentication(AuthPrincipal principal) {
        return new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + principal.role().name()))
        );
    }

    private void reject(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws ServletException, IOException {
        SecurityContextHolder.clearContext();
        authenticationEntryPoint.commence(request, response, exception);
    }
}
