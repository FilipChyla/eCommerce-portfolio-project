package io.github.filipchyla.shopapi.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.web.csrf.CsrfToken;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CsrfCookieFilterTest {
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain filterChain;
    @Mock
    private CsrfToken csrfToken;

    private final TestableCsrfCookieFilter csrfCookieFilter = new TestableCsrfCookieFilter();

    @Test
    void doFilterInternal_ShouldCallGetToken_WhenCsrfTokenPresent() throws Exception {

        when(request.getAttribute("_csrf")).thenReturn(csrfToken);

        csrfCookieFilter.invoke(request, response, filterChain);

        verify(csrfToken).getToken();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_ShouldProceedFilterChain_WhenCsrfTokenAbsent() throws Exception {

        when(request.getAttribute("_csrf")).thenReturn(null);

        csrfCookieFilter.invoke(request, response, filterChain);

        verify(csrfToken, never()).getToken();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_ShouldThrowClassCastException_WhenCsrfAttributeHasWrongType() throws ServletException, IOException {

        when(request.getAttribute("_csrf"))
                .thenReturn("not-a-csrf-token");

        assertThrows(ClassCastException.class, () -> csrfCookieFilter.invoke(
                request,
                response,
                filterChain
                )
        );

        verify(filterChain, never()).doFilter(request, response);
    }

    private static class TestableCsrfCookieFilter extends CsrfCookieFilter {

        void invoke(
                HttpServletRequest request,
                HttpServletResponse response,
                FilterChain filterChain
        ) throws ServletException, IOException {

            super.doFilterInternal(request, response, filterChain);
        }
    }
}
