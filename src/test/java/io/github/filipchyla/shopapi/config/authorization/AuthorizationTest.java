package io.github.filipchyla.shopapi.config.authorization;

import io.github.filipchyla.shopapi.auth.service.JwtService;
import io.github.filipchyla.shopapi.config.SecurityConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@Import(SecurityConfiguration.class)
abstract class AuthorizationTest {
    @Autowired
    protected MockMvc mockMvc;
    @MockitoBean
    protected JwtService jwtService;
    @MockitoBean
    protected UserDetailsService userDetailsService;
}
