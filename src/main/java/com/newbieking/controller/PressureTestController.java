package com.newbieking.controller;

import com.newbieking.service.InvoiceServiceDistributedFacade;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/test")
@Profile({"dev", "test", "pressure"})
@Slf4j
public class PressureTestController {

//    @Autowired
//    private InvoiceServiceJVMFacade facade;

    @Autowired
    private InvoiceServiceDistributedFacade facade;

    /**
     * 压测专用：作废发票（直接透传，无鉴权）
     */
    @PostMapping("/invoice/{invoiceId}/invalid")
    public ResponseEntity<?> invalidForTest(
            @PathVariable Long invoiceId,
            @RequestParam String reason) {

        long start = System.currentTimeMillis();
        try {
            facade.invalidInvoice(invoiceId, reason);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "invoiceId", invoiceId,
                    "costMs", System.currentTimeMillis() - start
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(409).body(Map.of(
                    "success", false,
                    "error", e.getMessage(),
                    "costMs", System.currentTimeMillis() - start
            ));
        }
    }

    /**
     * 重置发票状态（用于测试准备）
     */
    @PostMapping("/invoice/{invoiceId}/reset")
    public ResponseEntity<?> resetInvoice(@PathVariable Long invoiceId) {
        // 执行 SQL：UPDATE seller_invoice SET status=1, version=0 WHERE id=?
        return ResponseEntity.ok().build();
    }
}