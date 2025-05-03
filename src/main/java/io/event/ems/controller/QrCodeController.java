package io.event.ems.controller;

import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.event.ems.dto.ApiResponse;
import io.event.ems.dto.QrCodeVerificationResultDTO;
import io.event.ems.exception.ResourceNotFoundException;
import io.event.ems.service.QrCodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/qr-codes")
@RequiredArgsConstructor
@Tag(name = "QR Code", description = "QR Code generation and verification APIs")
public class QrCodeController {

    private final QrCodeService service;

    @GetMapping(value = "/purchase/{purchaseId}", produces = MediaType.IMAGE_PNG_VALUE)
    @Operation(summary = "Generate QR Code for a ticket purchase", description = "Generates a PNG image QR code containing the ticket purchase ID.")
    public ResponseEntity<byte[]> generateQrCode(
            @Parameter(description = "ID of the ticket purchase") @PathVariable UUID purchaseId)
            throws ResourceNotFoundException {
        byte[] qrCodeImage = service.genarateQrCodeForPurchase(purchaseId);
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(qrCodeImage);
    }

    @PostMapping("/verify")
    @Operation(summary = "Verify a scanned QR code", description = "Verifies the data from a scanned QR code (expected to be the ticket purchase ID) and attempts to check the user in.")
    public ResponseEntity<ApiResponse<QrCodeVerificationResultDTO>> verifyQrCode(
            @Parameter(description = "Data scanned from the QR code", required = true) @RequestBody String qrCodeData) {
        QrCodeVerificationResultDTO verificationResult = service.verifyQrCode(qrCodeData);
        return ResponseEntity.ok(ApiResponse.success("Verification processed", verificationResult));
    }

}
