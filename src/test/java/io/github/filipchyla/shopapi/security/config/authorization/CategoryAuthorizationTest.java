package io.github.filipchyla.shopapi.security.config.authorization;

import io.github.filipchyla.shopapi.product.category.CategoryController;
import io.github.filipchyla.shopapi.product.category.CategoryMapper;
import io.github.filipchyla.shopapi.product.category.CategoryService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.UUID;

import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CategoryController.class)
@AutoConfigureMockMvc
class CategoryAuthorizationTest extends AuthorizationTest{
    @MockitoBean
    private CategoryService categoryService;
    @MockitoBean
    private CategoryMapper categoryMapper;

    @Test
    void getCategories_IsAccessible_WithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk());
    }

    @Test
    void postCategories_IsNotAccessible_WithoutAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/categories"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void postCategories_ReturnsForbidden_WhenUser() throws Exception {
        mockMvc.perform(post("/api/v1/categories")
                        .with(user("testuser").roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void postCategories_IsAccessible_WhenAdmin() throws Exception {
        mockMvc.perform(post("/api/v1/categories")
                        .with(user("testadmin").roles("ADMIN")))
                .andExpect(status().is(not(403)))
                .andExpect(status().is(not(401)));
    }

    @Test
    void deleteCategory_IsNotAccessible_WithoutAuthentication() throws Exception {
        mockMvc.perform(delete("/api/v1/categories/{id}", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteCategory_ReturnsForbidden_WhenUser() throws Exception {
        mockMvc.perform(delete("/api/v1/categories/{id}", UUID.randomUUID())
                        .with(user("testuser").roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteCategory_IsAccessible_WhenAdmin() throws Exception {
        mockMvc.perform(delete("/api/v1/categories/{id}", UUID.randomUUID())
                        .with(user("testadmin").roles("ADMIN")))
                .andExpect(status().is(not(403)))
                .andExpect(status().is(not(401)));
    }
}