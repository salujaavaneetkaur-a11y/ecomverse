package com.ecommerce.project.integration;

import com.ecommerce.project.model.AppRole;
import com.ecommerce.project.model.Role;
import com.ecommerce.project.repositories.RoleRepository;
import com.ecommerce.project.repositories.UserRepository;
import com.ecommerce.project.security.request.LoginRequest;
import com.ecommerce.project.security.request.SignupRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration Tests for Authentication API
 *
 * ============================================================
 * 🎓 SECURITY TESTING BEST PRACTICES:
 * ============================================================
 *
 * 1. TEST HAPPY PATHS
 *    - Valid registration
 *    - Valid login
 *    - Token generation
 *
 * 2. TEST SECURITY EDGE CASES
 *    - Duplicate username
 *    - Wrong password
 *    - Invalid token
 *
 * 3. TEST AUTHORIZATION
 *    - Access without token
 *    - Access with expired token
 *    - Role-based access
 *
 * ============================================================
 * 📋 INTERVIEW TIP:
 * "Security tests are critical. I test:
 * 1. Password is never returned in responses
 * 2. Failed login doesn't reveal if user exists
 * 3. Tokens expire correctly
 * 4. Role-based access is enforced"
 * ============================================================
 */
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @BeforeEach
    void setUp() {
        // Clean up users (but keep roles)
        userRepository.deleteAll();

        // Create roles if not exist
        if (roleRepository.findByRoleName(AppRole.ROLE_ADMIN).isEmpty()) {
            roleRepository.save(new Role(AppRole.ROLE_ADMIN));
        }
        if (roleRepository.findByRoleName(AppRole.ROLE_USER).isEmpty()) {
            roleRepository.save(new Role(AppRole.ROLE_USER));
        }
        if (roleRepository.findByRoleName(AppRole.ROLE_SELLER).isEmpty()) {
            roleRepository.save(new Role(AppRole.ROLE_SELLER));
        }
    }

    // ==================== SIGNUP TESTS ====================

    @Test
    @Order(1)
    @DisplayName("POST /api/auth/signup - Should register user successfully")
    void signup_ValidInput_ReturnsSuccess() throws Exception {
        SignupRequest signupRequest = new SignupRequest();
        signupRequest.setUsername("testuser");
        signupRequest.setEmail("testuser@example.com");
        signupRequest.setPassword("password123");
        signupRequest.setRole(Set.of("user"));

        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("User registered successfully!")));
    }

    @Test
    @Order(2)
    @DisplayName("POST /api/auth/signup - Should fail for duplicate username")
    void signup_DuplicateUsername_ReturnsBadRequest() throws Exception {
        // First registration
        SignupRequest firstUser = new SignupRequest();
        firstUser.setUsername("duplicateuser");
        firstUser.setEmail("first@example.com");
        firstUser.setPassword("password123");
        firstUser.setRole(Set.of("user"));

        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(firstUser)))
                .andExpect(status().isOk());

        // Second registration with same username
        SignupRequest secondUser = new SignupRequest();
        secondUser.setUsername("duplicateuser");
        secondUser.setEmail("second@example.com");
        secondUser.setPassword("password123");
        secondUser.setRole(Set.of("user"));

        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(secondUser)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Username is already taken")));
    }

    @Test
    @Order(3)
    @DisplayName("POST /api/auth/signup - Should fail for duplicate email")
    void signup_DuplicateEmail_ReturnsBadRequest() throws Exception {
        // First registration
        SignupRequest firstUser = new SignupRequest();
        firstUser.setUsername("firstuser");
        firstUser.setEmail("duplicate@example.com");
        firstUser.setPassword("password123");
        firstUser.setRole(Set.of("user"));

        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(firstUser)))
                .andExpect(status().isOk());

        // Second registration with same email
        SignupRequest secondUser = new SignupRequest();
        secondUser.setUsername("seconduser");
        secondUser.setEmail("duplicate@example.com");
        secondUser.setPassword("password123");
        secondUser.setRole(Set.of("user"));

        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(secondUser)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Email is already in use")));
    }

    @Test
    @Order(4)
    @DisplayName("POST /api/auth/signup - Should register admin with all roles")
    void signup_AdminRole_HasAllRoles() throws Exception {
        SignupRequest adminRequest = new SignupRequest();
        adminRequest.setUsername("adminuser");
        adminRequest.setEmail("admin@example.com");
        adminRequest.setPassword("password123");
        adminRequest.setRole(Set.of("admin"));

        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(adminRequest)))
                .andExpect(status().isOk());

        // Login and verify roles
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("adminuser");
        loginRequest.setPassword("password123");

        mockMvc.perform(post("/api/auth/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles", hasItem("ROLE_ADMIN")))
                .andExpect(jsonPath("$.roles", hasItem("ROLE_USER")))
                .andExpect(jsonPath("$.roles", hasItem("ROLE_SELLER")));
    }

    // ==================== SIGNIN TESTS ====================

    @Test
    @Order(5)
    @DisplayName("POST /api/auth/signin - Should login successfully with valid credentials")
    void signin_ValidCredentials_ReturnsTokenAndUserInfo() throws Exception {
        // First register a user
        SignupRequest signupRequest = new SignupRequest();
        signupRequest.setUsername("logintest");
        signupRequest.setEmail("logintest@example.com");
        signupRequest.setPassword("password123");
        signupRequest.setRole(Set.of("user"));

        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isOk());

        // Now login
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("logintest");
        loginRequest.setPassword("password123");

        mockMvc.perform(post("/api/auth/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is("logintest")))
                .andExpect(jsonPath("$.jwtToken", notNullValue()))
                .andExpect(jsonPath("$.roles", hasItem("ROLE_USER")));
    }

    @Test
    @Order(6)
    @DisplayName("POST /api/auth/signin - Should fail with wrong password")
    void signin_WrongPassword_ReturnsUnauthorized() throws Exception {
        // First register a user
        SignupRequest signupRequest = new SignupRequest();
        signupRequest.setUsername("wrongpasstest");
        signupRequest.setEmail("wrongpass@example.com");
        signupRequest.setPassword("correctpassword");
        signupRequest.setRole(Set.of("user"));

        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isOk());

        // Try login with wrong password
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("wrongpasstest");
        loginRequest.setPassword("wrongpassword");

        mockMvc.perform(post("/api/auth/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(7)
    @DisplayName("POST /api/auth/signin - Should fail for non-existent user")
    void signin_NonExistentUser_ReturnsUnauthorized() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("nonexistentuser");
        loginRequest.setPassword("password123");

        mockMvc.perform(post("/api/auth/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }

    // ==================== JWT TOKEN TESTS ====================

    @Test
    @Order(8)
    @DisplayName("GET /api/auth/user - Should return user info with valid token")
    void getUser_ValidToken_ReturnsUserInfo() throws Exception {
        // Register and login
        SignupRequest signupRequest = new SignupRequest();
        signupRequest.setUsername("tokentest");
        signupRequest.setEmail("tokentest@example.com");
        signupRequest.setPassword("password123");
        signupRequest.setRole(Set.of("user"));

        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupRequest)));

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("tokentest");
        loginRequest.setPassword("password123");

        MvcResult loginResult = mockMvc.perform(post("/api/auth/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        // Extract token from response
        String response = loginResult.getResponse().getContentAsString();
        String token = objectMapper.readTree(response).get("jwtToken").asText();

        // Use token to access protected endpoint
        mockMvc.perform(get("/api/auth/user")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is("tokentest")));
    }

    @Test
    @Order(9)
    @DisplayName("GET /api/auth/user - Should fail without token")
    void getUser_NoToken_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/auth/user"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(10)
    @DisplayName("GET /api/auth/user - Should fail with invalid token")
    void getUser_InvalidToken_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/auth/user")
                .header("Authorization", "Bearer invalid.token.here"))
                .andExpect(status().isUnauthorized());
    }

    // ==================== SIGNOUT TESTS ====================

    @Test
    @Order(11)
    @DisplayName("POST /api/auth/signout - Should logout successfully")
    void signout_ValidSession_ReturnsSuccess() throws Exception {
        // Register and login first
        SignupRequest signupRequest = new SignupRequest();
        signupRequest.setUsername("logouttest");
        signupRequest.setEmail("logout@example.com");
        signupRequest.setPassword("password123");
        signupRequest.setRole(Set.of("user"));

        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupRequest)));

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("logouttest");
        loginRequest.setPassword("password123");

        MvcResult loginResult = mockMvc.perform(post("/api/auth/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andReturn();

        String response = loginResult.getResponse().getContentAsString();
        String token = objectMapper.readTree(response).get("jwtToken").asText();

        // Logout
        mockMvc.perform(post("/api/auth/signout")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("signed out")));
    }
}
