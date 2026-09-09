package com.example.food.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class JwtAuthenticationFilterTest {

    @Test
    void filtersAsyncDispatchesForSseRequests() {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(
                mock(JwtService.class),
                mock(org.springframework.security.web.AuthenticationEntryPoint.class),
                null
        );

        assertThat(filter.shouldNotFilterAsyncDispatch()).isFalse();
    }
}
