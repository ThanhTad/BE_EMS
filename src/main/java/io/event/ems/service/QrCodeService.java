package io.event.ems.service;

import io.event.ems.model.EventSeatStatus;
import io.event.ems.model.PurchasedGATicket;

import java.util.List;

public interface QrCodeService {

    /**
     * Tạo QR code cho một vé có chỗ ngồi cụ thể (Reserved Seating)
     *
     * @param soldSeat Đối tượng EventSeatStatus đã được bán
     * @return Dữ liệu byte của ảnh QR code
     */
    byte[] generateQrCodeForReservedSeat(EventSeatStatus soldSeat);

    /**
     * Tạo nhiều QR code cho một nhóm vé tự do (General Admission)
     *
     * @param gaTicketGroup Đối tượng PurchasedGaTicket chứa thông tin nhóm vé
     * @return Dữ liệu byte của ảnh QR code (có thể tạo 1 QR cho cả nhóm hoặc nhiều QR)
     * Ở đây ta tạo 1 QR cho cả nhóm.
     */
    List<byte[]> generateQrCodeForGaTicket(PurchasedGATicket gaTicketGroup);

}
