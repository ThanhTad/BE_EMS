package io.event.ems.model;

public enum TicketSelectionModeEnum {

    /**
     * Chế độ bán vé tự do, không có sơ đồ ghế.
     * Người dùng chỉ chọn loại vé và số lượng.
     * Tương ứng với vé xem ca nhạc đứng, hội chợ...
     */
    GENERAL_ADMISSION,

    /**
     * Chế độ bán vé theo khu vực, có sơ đồ nhưng không chọn ghế cụ thể.
     * Người dùng chọn khu vực (Zone) và số lượng.
     * Tương ứng với các sự kiện âm nhạc lớn có chia khu đứng VIP, khu GA...
     */
    ZONED_ADMISSION,

    /**
     * Chế độ bán vé theo ghế ngồi cụ thể, có sơ đồ chi tiết.
     * Người dùng chọn chính xác ghế mình muốn.
     * Tương ứng với nhà hát, rạp chiếu phim, sân khấu hòa nhạc có ghế ngồi.
     */
    RESERVED_SEATING
}
