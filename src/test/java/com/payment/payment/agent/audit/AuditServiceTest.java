package com.payment.payment.agent.audit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuditService
 * Covers normal operation and edge cases
 */
@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditService auditService;

    private String testTransactionId;
    private String testEventType;
    private String testDetails;

    @BeforeEach
    void setUp() {
        testTransactionId = "test-tx-123";
        testEventType = "TEST_EVENT";
        testDetails = "Test event details";
    }

    @Test
    void logEvent_ValidInput_CreatesAuditLog() {
        // When
        auditService.logEvent(testTransactionId, testEventType, testDetails);

        // Then
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog savedLog = captor.getValue();
        assertEquals(testTransactionId, savedLog.getTransactionId());
        assertEquals(testEventType, savedLog.getEventType());
        assertEquals(testDetails, savedLog.getDetails());
        assertNotNull(savedLog.getTimestamp());
        assertTrue(savedLog.getTimestamp().isBefore(LocalDateTime.now().plusSeconds(1)));
        assertTrue(savedLog.getTimestamp().isAfter(LocalDateTime.now().minusSeconds(1)));
    }

    @Test
    void logEvent_EmptyTransactionId_StillCreatesLog() {
        // When
        auditService.logEvent("", testEventType, testDetails);

        // Then
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog savedLog = captor.getValue();
        assertEquals("", savedLog.getTransactionId());
        assertEquals(testEventType, savedLog.getEventType());
        assertEquals(testDetails, savedLog.getDetails());
    }

    @Test
    void logEvent_NullTransactionId_StillCreatesLog() {
        // When
        auditService.logEvent(null, testEventType, testDetails);

        // Then
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog savedLog = captor.getValue();
        assertNull(savedLog.getTransactionId());
        assertEquals(testEventType, savedLog.getEventType());
        assertEquals(testDetails, savedLog.getDetails());
    }

    @Test
    void logEvent_EmptyEventType_StillCreatesLog() {
        // When
        auditService.logEvent(testTransactionId, "", testDetails);

        // Then
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog savedLog = captor.getValue();
        assertEquals(testTransactionId, savedLog.getTransactionId());
        assertEquals("", savedLog.getEventType());
        assertEquals(testDetails, savedLog.getDetails());
    }

    @Test
    void logEvent_NullEventType_StillCreatesLog() {
        // When
        auditService.logEvent(testTransactionId, null, testDetails);

        // Then
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog savedLog = captor.getValue();
        assertEquals(testTransactionId, savedLog.getTransactionId());
        assertNull(savedLog.getEventType());
        assertEquals(testDetails, savedLog.getDetails());
    }

    @Test
    void logEvent_EmptyDetails_StillCreatesLog() {
        // When
        auditService.logEvent(testTransactionId, testEventType, "");

        // Then
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog savedLog = captor.getValue();
        assertEquals(testTransactionId, savedLog.getTransactionId());
        assertEquals(testEventType, savedLog.getEventType());
        assertEquals("", savedLog.getDetails());
    }

    @Test
    void logEvent_NullDetails_StillCreatesLog() {
        // When
        auditService.logEvent(testTransactionId, testEventType, null);

        // Then
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog savedLog = captor.getValue();
        assertEquals(testTransactionId, savedLog.getTransactionId());
        assertEquals(testEventType, savedLog.getEventType());
        assertNull(savedLog.getDetails());
    }

    @Test
    void logEvent_LongDetails_HandlesCorrectly() {
        // Given
        String longDetails = "a".repeat(10000); // 10k characters

        // When
        auditService.logEvent(testTransactionId, testEventType, longDetails);

        // Then
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog savedLog = captor.getValue();
        assertEquals(testTransactionId, savedLog.getTransactionId());
        assertEquals(testEventType, savedLog.getEventType());
        assertEquals(longDetails, savedLog.getDetails());
    }

    @Test
    void logEvent_SpecialCharactersInDetails_HandlesCorrectly() {
        // Given
        String specialDetails = "Special chars: áéíóú 中文 😊 🚀 \n\t\r\"'{}[]";

        // When
        auditService.logEvent(testTransactionId, testEventType, specialDetails);

        // Then
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog savedLog = captor.getValue();
        assertEquals(testTransactionId, savedLog.getTransactionId());
        assertEquals(testEventType, savedLog.getEventType());
        assertEquals(specialDetails, savedLog.getDetails());
    }

    @Test
    void logEvent_RepositoryThrowsException_PropagatesException() {
        // Given
        doThrow(new RuntimeException("Database error")).when(auditLogRepository).save(any(AuditLog.class));

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            auditService.logEvent(testTransactionId, testEventType, testDetails);
        });

        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    void logEvent_MultipleEvents_CreatesMultipleLogs() {
        // When
        auditService.logEvent(testTransactionId, "EVENT_1", "Details 1");
        auditService.logEvent(testTransactionId, "EVENT_2", "Details 2");
        auditService.logEvent(testTransactionId, "EVENT_3", "Details 3");

        // Then
        verify(auditLogRepository, times(3)).save(any(AuditLog.class));

        // Verify each call
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository, times(3)).save(captor.capture());

        List<AuditLog> savedLogs = captor.getAllValues();
        assertEquals(testTransactionId, savedLogs.get(0).getTransactionId());
        assertEquals("EVENT_1", savedLogs.get(0).getEventType());
        assertEquals("Details 1", savedLogs.get(0).getDetails());

        assertEquals(testTransactionId, savedLogs.get(1).getTransactionId());
        assertEquals("EVENT_2", savedLogs.get(1).getEventType());
        assertEquals("Details 2", savedLogs.get(1).getDetails());

        assertEquals(testTransactionId, savedLogs.get(2).getTransactionId());
        assertEquals("EVENT_3", savedLogs.get(2).getEventType());
        assertEquals("Details 3", savedLogs.get(2).getDetails());
    }

    @Test
    void logEvent_TimestampAccuracy_WithinExpectedRange() {
        // Given
        LocalDateTime beforeCall = LocalDateTime.now();

        // When
        auditService.logEvent(testTransactionId, testEventType, testDetails);

        // Then
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        LocalDateTime afterCall = LocalDateTime.now();
        AuditLog savedLog = captor.getValue();

        assertNotNull(savedLog.getTimestamp());
        assertTrue(savedLog.getTimestamp().isBefore(afterCall.plusSeconds(1)));
        assertTrue(savedLog.getTimestamp().isAfter(beforeCall.minusSeconds(1)));
    }
}