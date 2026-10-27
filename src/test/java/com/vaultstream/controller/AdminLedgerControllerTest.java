package com.vaultstream.controller;

import com.vaultstream.dto.ReconciliationReportDto;
import com.vaultstream.service.LedgerReconciliationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminLedgerControllerTest {

    @Mock
    private LedgerReconciliationService reconciliationService;

    @InjectMocks
    private AdminLedgerController adminLedgerController;

    private ReconciliationReportDto sampleReport;

    @BeforeEach
    void setUp() {
        sampleReport = ReconciliationReportDto.builder()
                .reconciliationTimestamp(LocalDateTime.now())
                .totalDebits(new BigDecimal("1000.00"))
                .totalCredits(new BigDecimal("1000.00"))
                .systemImbalance(BigDecimal.ZERO)
                .systemBalanced(true)
                .accountsAudited(5)
                .discrepanciesCount(0)
                .discrepancies(Collections.emptyList())
                .build();
    }

    @Test
    void triggerReconciliation_ReturnsReportWithStatus200() {
        when(reconciliationService.reconcile()).thenReturn(sampleReport);

        ResponseEntity<ReconciliationReportDto> response = adminLedgerController.triggerReconciliation();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(sampleReport, response.getBody());
        verify(reconciliationService).reconcile();
    }

    @Test
    void getLatestReport_WhenReportAlreadyCached_ReturnsCachedReport() {
        when(reconciliationService.getLastReport()).thenReturn(sampleReport);

        ResponseEntity<ReconciliationReportDto> response = adminLedgerController.getLatestReport();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(sampleReport, response.getBody());
        verify(reconciliationService).getLastReport();
        verify(reconciliationService, never()).reconcile();
    }

    @Test
    void getLatestReport_WhenNoReportCached_TriggersReconciliation() {
        when(reconciliationService.getLastReport()).thenReturn(null);
        when(reconciliationService.reconcile()).thenReturn(sampleReport);

        ResponseEntity<ReconciliationReportDto> response = adminLedgerController.getLatestReport();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(sampleReport, response.getBody());
        verify(reconciliationService).getLastReport();
        verify(reconciliationService).reconcile();
    }
}
