package com.payment.payment.agent.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationContextServiceTest {

    private AuthenticationContextService authenticationContextService;

    @BeforeEach
    void setUp() {
        authenticationContextService = new AuthenticationContextService();
    }

    @Test
    void testGetCurrentUserId_WithUsernameAuthentication_ShouldReturnUsername() {
        // Given
        Authentication authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("testuser");

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);

        try (MockedStatic<SecurityContextHolder> mockedSecurityContextHolder =
             Mockito.mockStatic(SecurityContextHolder.class)) {
            mockedSecurityContextHolder.when(SecurityContextHolder::getContext)
                .thenReturn(securityContext);

            // When
            String userId = authenticationContextService.getCurrentUserId();

            // Then
            assertEquals("testuser", userId);
        }
    }

    @Test
    void testGetCurrentUserId_WithJwtAuthentication_ShouldReturnSubject() {
        // Given
        Jwt jwt = mock(Jwt.class);
        when(jwt.getSubject()).thenReturn("jwt-subject");
        when(jwt.getClaimAsString("sub")).thenReturn("jwt-subject");

        Authentication authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(jwt);

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);

        try (MockedStatic<SecurityContextHolder> mockedSecurityContextHolder =
             Mockito.mockStatic(SecurityContextHolder.class)) {
            mockedSecurityContextHolder.when(SecurityContextHolder::getContext)
                .thenReturn(securityContext);

            // When
            String userId = authenticationContextService.getCurrentUserId();

            // Then
            assertEquals("jwt-subject", userId);
        }
    }

    @Test
    void testGetCurrentUserId_WithJwtUserIdClaim_ShouldReturnUserId() {
        // Given
        Jwt jwt = mock(Jwt.class);
        when(jwt.getSubject()).thenReturn("jwt-subject");
        when(jwt.getClaimAsString("sub")).thenReturn(null);
        when(jwt.getClaimAsString("user_id")).thenReturn("custom-user-id");

        Authentication authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(jwt);

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);

        try (MockedStatic<SecurityContextHolder> mockedSecurityContextHolder =
             Mockito.mockStatic(SecurityContextHolder.class)) {
            mockedSecurityContextHolder.when(SecurityContextHolder::getContext)
                .thenReturn(securityContext);

            // When
            String userId = authenticationContextService.getCurrentUserId();

            // Then
            assertEquals("custom-user-id", userId);
        }
    }

    @Test
    void testGetCurrentUserId_WithNoAuthentication_ShouldThrowException() {
        // Given
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(null);

        try (MockedStatic<SecurityContextHolder> mockedSecurityContextHolder =
             Mockito.mockStatic(SecurityContextHolder.class)) {
            mockedSecurityContextHolder.when(SecurityContextHolder::getContext)
                .thenReturn(securityContext);

            // When & Then
            SecurityException exception = assertThrows(SecurityException.class,
                () -> authenticationContextService.getCurrentUserId());

            assertTrue(exception.getMessage().contains("No authenticated user found"));
        }
    }

    @Test
    void testGetCurrentUserId_WithUnauthenticatedUser_ShouldThrowException() {
        // Given
        Authentication authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(false);

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);

        try (MockedStatic<SecurityContextHolder> mockedSecurityContextHolder =
             Mockito.mockStatic(SecurityContextHolder.class)) {
            mockedSecurityContextHolder.when(SecurityContextHolder::getContext)
                .thenReturn(securityContext);

            // When & Then
            SecurityException exception = assertThrows(SecurityException.class,
                () -> authenticationContextService.getCurrentUserId());

            assertTrue(exception.getMessage().contains("No authenticated user found"));
        }
    }

    @Test
    void testHasRole_WithMatchingRole_ShouldReturnTrue() {
        // Given
        Authentication authentication = mock(Authentication.class);
        when(authentication.getAuthorities()).thenReturn((Collection)List.of(
            new SimpleGrantedAuthority("ROLE_USER"),
            new SimpleGrantedAuthority("ROLE_ADMIN")
        ));

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);

        try (MockedStatic<SecurityContextHolder> mockedSecurityContextHolder =
             Mockito.mockStatic(SecurityContextHolder.class)) {
            mockedSecurityContextHolder.when(SecurityContextHolder::getContext)
                .thenReturn(securityContext);

            // When
            boolean result = authenticationContextService.hasRole("ADMIN");

            // Then
            assertTrue(result);
        }
    }

    @Test
    void testHasRole_WithNonMatchingRole_ShouldReturnFalse() {
        // Given
        Authentication authentication = mock(Authentication.class);
        when(authentication.getAuthorities()).thenReturn((Collection)List.of(
            new SimpleGrantedAuthority("ROLE_USER")
        ));

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);

        try (MockedStatic<SecurityContextHolder> mockedSecurityContextHolder =
             Mockito.mockStatic(SecurityContextHolder.class)) {
            mockedSecurityContextHolder.when(SecurityContextHolder::getContext)
                .thenReturn(securityContext);

            // When
            boolean result = authenticationContextService.hasRole("ADMIN");

            // Then
            assertFalse(result);
        }
    }

    @Test
    void testCanOverrideTransactions_WithAdminRole_ShouldReturnTrue() {
        // Given
        Authentication authentication = mock(Authentication.class);
        when(authentication.getAuthorities()).thenReturn((Collection)List.of(
            new SimpleGrantedAuthority("ROLE_ADMIN")
        ));

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);

        try (MockedStatic<SecurityContextHolder> mockedSecurityContextHolder =
             Mockito.mockStatic(SecurityContextHolder.class)) {
            mockedSecurityContextHolder.when(SecurityContextHolder::getContext)
                .thenReturn(securityContext);

            // When
            boolean result = authenticationContextService.canOverrideTransactions();

            // Then
            assertTrue(result);
        }
    }

    @Test
    void testCanOverrideTransactions_WithSupervisorRole_ShouldReturnTrue() {
        // Given
        Authentication authentication = mock(Authentication.class);
        when(authentication.getAuthorities()).thenReturn((Collection)List.of(
            new SimpleGrantedAuthority("ROLE_SUPERVISOR")
        ));

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);

        try (MockedStatic<SecurityContextHolder> mockedSecurityContextHolder =
             Mockito.mockStatic(SecurityContextHolder.class)) {
            mockedSecurityContextHolder.when(SecurityContextHolder::getContext)
                .thenReturn(securityContext);

            // When
            boolean result = authenticationContextService.canOverrideTransactions();

            // Then
            assertTrue(result);
        }
    }

    @Test
    void testCanOverrideTransactions_WithUserRole_ShouldReturnFalse() {
        // Given
        Authentication authentication = mock(Authentication.class);
        when(authentication.getAuthorities()).thenReturn((Collection)List.of(
            new SimpleGrantedAuthority("ROLE_USER")
        ));

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);

        try (MockedStatic<SecurityContextHolder> mockedSecurityContextHolder =
             Mockito.mockStatic(SecurityContextHolder.class)) {
            mockedSecurityContextHolder.when(SecurityContextHolder::getContext)
                .thenReturn(securityContext);

            // When
            boolean result = authenticationContextService.canOverrideTransactions();

            // Then
            assertFalse(result);
        }
    }

    @Test
    void testValidateUserAccess_WithMatchingUserId_ShouldNotThrowException() {
        // Given
        Authentication authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("testuser");

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);

        try (MockedStatic<SecurityContextHolder> mockedSecurityContextHolder =
             Mockito.mockStatic(SecurityContextHolder.class)) {
            mockedSecurityContextHolder.when(SecurityContextHolder::getContext)
                .thenReturn(securityContext);

            // When & Then
            assertDoesNotThrow(() ->
                authenticationContextService.validateUserAccess("testuser"));
        }
    }

    @Test
    void testValidateUserAccess_WithNonMatchingUserIdAndNoAdminRole_ShouldThrowException() {
        // Given
        Authentication authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("currentuser");
        when(authentication.getAuthorities()).thenReturn((Collection)List.of(
            new SimpleGrantedAuthority("ROLE_USER")
        ));

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);

        try (MockedStatic<SecurityContextHolder> mockedSecurityContextHolder =
             Mockito.mockStatic(SecurityContextHolder.class)) {
            mockedSecurityContextHolder.when(SecurityContextHolder::getContext)
                .thenReturn(securityContext);

            // When & Then
            SecurityException exception = assertThrows(SecurityException.class,
                () -> authenticationContextService.validateUserAccess("differentuser"));

            assertTrue(exception.getMessage().contains("does not have permission"));
        }
    }

    @Test
    void testValidateUserAccess_WithNonMatchingUserIdAndAdminRole_ShouldNotThrowException() {
        // Given
        Authentication authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("currentuser");
        when(authentication.getAuthorities()).thenReturn((Collection)List.of(
            new SimpleGrantedAuthority("ROLE_ADMIN")
        ));

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);

        try (MockedStatic<SecurityContextHolder> mockedSecurityContextHolder =
             Mockito.mockStatic(SecurityContextHolder.class)) {
            mockedSecurityContextHolder.when(SecurityContextHolder::getContext)
                .thenReturn(securityContext);

            // When & Then
            assertDoesNotThrow(() ->
                authenticationContextService.validateUserAccess("differentuser"));
        }
    }
}