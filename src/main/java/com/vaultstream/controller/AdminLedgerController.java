package com.vaultstream.controller;

import com.vaultstream.dto.ReconciliationReportDto;
import com.vaultstream.service.LedgerReconciliationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/ledger")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminLedgerController {

    private final LedgerReconciliationService reconciliationService;

    @PostMapping("/reconcile")
    public ResponseEntity<ReconciliationReportDto> triggerReconciliation() {
        return ResponseEntity.ok(reconciliationService.reconcile());
    }

    @GetMapping("/reconcile/latest")
    public ResponseEntity<ReconciliationReportDto> getLatestReport() {
        ReconciliationReportDto report = reconciliationService.getLastReport();
        if (report == null) {
            report = reconciliationService.reconcile();
        }
        return ResponseEntity.ok(report);
    }
}
