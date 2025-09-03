package com.payment.payment.agent.audit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuditLogRepository
 * Tests basic CRUD operations and custom queries
 */
@ExtendWith(MockitoExtension.class)
class AuditLogRepositoryTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    private AuditLog testAuditLog;
    private String testTransactionId;

    @BeforeEach
    void setUp() {
        testTransactionId = "test-tx-123";
        testAuditLog = createTestAuditLog();
    }

    @Test
    void save_ValidAuditLog_ReturnsSavedLog() {
        // Given
        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(testAuditLog);

        // When
        AuditLog savedLog = auditLogRepository.save(testAuditLog);

        // Then
        assertNotNull(savedLog);
        assertEquals(testTransactionId, savedLog.getTransactionId());
        assertEquals("TEST_EVENT", savedLog.getEventType());
        assertEquals("Test details", savedLog.getDetails());
        assertNotNull(savedLog.getTimestamp());
        verify(auditLogRepository).save(testAuditLog);
    }

    @Test
    void findById_ValidId_ReturnsAuditLog() {
        // Given
        Long testId = 1L;
        when(auditLogRepository.findById(testId)).thenReturn(Optional.of(testAuditLog));

        // When
        Optional<AuditLog> foundLog = auditLogRepository.findById(testId);

        // Then
        assertTrue(foundLog.isPresent());
        assertEquals(testTransactionId, foundLog.get().getTransactionId());
        verify(auditLogRepository).findById(testId);
    }

    @Test
    void findById_InvalidId_ReturnsEmpty() {
        // Given
        Long invalidId = 999L;
        when(auditLogRepository.findById(invalidId)).thenReturn(Optional.empty());

        // When
        Optional<AuditLog> foundLog = auditLogRepository.findById(invalidId);

        // Then
        assertFalse(foundLog.isPresent());
        verify(auditLogRepository).findById(invalidId);
    }

    @Test
    void findAll_ReturnsAllAuditLogs() {
        // Given
        List<AuditLog> expectedLogs = List.of(
            createTestAuditLog("tx-1", "EVENT_1"),
            createTestAuditLog("tx-2", "EVENT_2"),
            createTestAuditLog("tx-3", "EVENT_3")
        );
        when(auditLogRepository.findAll()).thenReturn(expectedLogs);

        // When
        List<AuditLog> allLogs = auditLogRepository.findAll();

        // Then
        assertEquals(3, allLogs.size());
        assertEquals("tx-1", allLogs.get(0).getTransactionId());
        assertEquals("tx-2", allLogs.get(1).getTransactionId());
        assertEquals("tx-3", allLogs.get(2).getTransactionId());
        verify(auditLogRepository).findAll();
    }

    @Test
    void findAll_EmptyList_ReturnsEmptyList() {
        // Given
        when(auditLogRepository.findAll()).thenReturn(Collections.emptyList());

        // When
        List<AuditLog> allLogs = auditLogRepository.findAll();

        // Then
        assertTrue(allLogs.isEmpty());
        verify(auditLogRepository).findAll();
    }

    @Test
    void findAll_Pageable_ReturnsPage() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        List<AuditLog> auditLogs = List.of(testAuditLog);
        Page<AuditLog> expectedPage = new PageImpl<>(auditLogs, pageable, 1);
        
        when(auditLogRepository.findAll(pageable)).thenReturn(expectedPage);

        // When
        Page<AuditLog> resultPage = auditLogRepository.findAll(pageable);

        // Then
        assertNotNull(resultPage);
        assertEquals(1, resultPage.getContent().size());
        assertEquals(testTransactionId, resultPage.getContent().get(0).getTransactionId());
        assertEquals(1, resultPage.getTotalElements());
        assertEquals(1, resultPage.getTotalPages());
        verify(auditLogRepository).findAll(pageable);
    }

    @Test
    void count_ReturnsCorrectCount() {
        // Given
        long expectedCount = 5L;
        when(auditLogRepository.count()).thenReturn(expectedCount);

        // When
        long actualCount = auditLogRepository.count();

        // Then
        assertEquals(expectedCount, actualCount);
        verify(auditLogRepository).count();
    }

    @Test
    void deleteById_ValidId_DeletesAuditLog() {
        // Given
        Long testId = 1L;
        doNothing().when(auditLogRepository).deleteById(testId);

        // When
        auditLogRepository.deleteById(testId);

        // Then
        verify(auditLogRepository).deleteById(testId);
    }

    @Test
    void delete_ValidAuditLog_DeletesAuditLog() {
        // Given
        doNothing().when(auditLogRepository).delete(testAuditLog);

        // When
        auditLogRepository.delete(testAuditLog);

        // Then
        verify(auditLogRepository).delete(testAuditLog);
    }

    @Test
    void deleteAll_DeletesAllAuditLogs() {
        // Given
        doNothing().when(auditLogRepository).deleteAll();

        // When
        auditLogRepository.deleteAll();

        // Then
        verify(auditLogRepository).deleteAll();
    }

    @Test
    void existsById_ValidId_ReturnsTrue() {
        // Given
        Long testId = 1L;
        when(auditLogRepository.existsById(testId)).thenReturn(true);

        // When
        boolean exists = auditLogRepository.existsById(testId);

        // Then
        assertTrue(exists);
        verify(auditLogRepository).existsById(testId);
    }

    @Test
    void existsById_InvalidId_ReturnsFalse() {
        // Given
        Long invalidId = 999L;
        when(auditLogRepository.existsById(invalidId)).thenReturn(false);

        // When
        boolean exists = auditLogRepository.existsById(invalidId);

        // Then
        assertFalse(exists);
        verify(auditLogRepository).existsById(invalidId);
    }

    @Test
    void saveAll_MultipleAuditLogs_ReturnsSavedLogs() {
        // Given
        List<AuditLog> logsToSave = List.of(
            createTestAuditLog("tx-1", "EVENT_1"),
            createTestAuditLog("tx-2", "EVENT_2")
        );
        when(auditLogRepository.saveAll(logsToSave)).thenReturn(logsToSave);

        // When
        List<AuditLog> savedLogs = auditLogRepository.saveAll(logsToSave);

        // Then
        assertEquals(2, savedLogs.size());
        assertEquals("tx-1", savedLogs.get(0).getTransactionId());
        assertEquals("tx-2", savedLogs.get(1).getTransactionId());
        verify(auditLogRepository).saveAll(logsToSave);
    }

    @Test
    void saveAll_EmptyList_ReturnsEmptyList() {
        // Given
        List<AuditLog> emptyList = Collections.emptyList();
        when(auditLogRepository.saveAll(emptyList)).thenReturn(emptyList);

        // When
        List<AuditLog> savedLogs = auditLogRepository.saveAll(emptyList);

        // Then
        assertTrue(savedLogs.isEmpty());
        verify(auditLogRepository).saveAll(emptyList);
    }

    private AuditLog createTestAuditLog() {
        return createTestAuditLog(testTransactionId, "TEST_EVENT");
    }

    private AuditLog createTestAuditLog(String transactionId, String eventType) {
        AuditLog log = new AuditLog();
        log.setId(1L);
        log.setTransactionId(transactionId);
        log.setEventType(eventType);
        log.setDetails("Test details");
        log.setTimestamp(LocalDateTime.now());
        return log;
    }
}