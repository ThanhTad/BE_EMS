package io.event.ems.service;

import java.util.UUID;

import io.event.ems.dto.QrCodeVerificationResultDTO;
import io.event.ems.exception.ResourceNotFoundException;

public interface QrCodeService {

    byte[] genarateQrCodeForPurchase(UUID purchaseId) throws ResourceNotFoundException;

    QrCodeVerificationResultDTO verifyQrCode(String qrCodeData);

}
