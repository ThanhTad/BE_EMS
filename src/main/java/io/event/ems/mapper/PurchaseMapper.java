package io.event.ems.mapper;

import io.event.ems.dto.PurchaseDetailDTO;
import io.event.ems.dto.PurchaseListItemDTO;
import io.event.ems.model.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PurchaseMapper {

    /**
     * Chuyển đổi từ TicketPurchase Entity sang PurchaseListItemDTO.
     *
     * @param purchase Entity nguồn.
     * @return DTO tóm tắt.
     */
    @Mapping(source = "event.title", target = "eventTitle")
    @Mapping(source = "event.id", target = "eventId")
    @Mapping(source = "user", target = "customerName", qualifiedByName = "userToFullName")
    @Mapping(source = "status.status", target = "status")
    @Mapping(source = "event.coverImageUrl", target = "eventImageUrl")
    PurchaseListItemDTO toListItemDTO(TicketPurchase purchase);


    // =================================================================
    // MAPPING CHO TRANG CHI TIẾT (DETAIL)
    // =================================================================

    /**
     * Chuyển đổi từ TicketPurchase Entity sang PurchaseDetailDTO.
     * Các trường collection (vé GA, vé seated) sẽ được xử lý riêng.
     *
     * @param purchase Entity nguồn.
     * @return DTO chi tiết.
     */
    @Mapping(source = "status.status", target = "status")
    @Mapping(source = "user", target = "customer")
    @Mapping(source = "event", target = "event")
    @Mapping(target = "generalAdmissionTickets", ignore = true)
    @Mapping(target = "seatedTickets", ignore = true)
    // Sẽ được điền thủ công trong service
    PurchaseDetailDTO toDetailDTO(TicketPurchase purchase);

    // --- Helper Mappers cho Detail DTO ---

    // Map User -> CustomerInfoDTO
    @Mapping(source = "id", target = "id")
    @Mapping(source = "fullName", target = "fullName")
    @Mapping(source = "email", target = "email")
    @Mapping(source = "phone", target = "phoneNumber")
    PurchaseDetailDTO.CustomerInfoDTO userToCustomerInfoDTO(User user);

    // Map Event -> EventInfoDTO
    @Mapping(source = "id", target = "id")
    @Mapping(source = "title", target = "title")
    @Mapping(source = "slug", target = "slug")
    @Mapping(source = "startDate", target = "startDate")
    PurchaseDetailDTO.EventInfoDTO eventToEventInfoDTO(Event event);

    /**
     * Chuyển đổi một danh sách các PurchasedGATicket Entity.
     * MapStruct sẽ tự động áp dụng mapper `toPurchasedGATicketDTO` cho mỗi phần tử.
     */
    List<PurchaseDetailDTO.PurchasedGATicketDTO> purchasedGaTicketsToDTOs(List<PurchasedGATicket> gaTickets);

    /**
     * Chuyển đổi một PurchasedGATicket Entity sang DTO.
     */
    @Mapping(source = "ticket.name", target = "ticketName")
    PurchaseDetailDTO.PurchasedGATicketDTO purchasedGaTicketToDTO(PurchasedGATicket gaTicket);

    /**
     * Chuyển đổi một danh sách các EventSeatStatus Entity (vé ngồi).
     * MapStruct sẽ tự động áp dụng mapper `toPurchasedSeatedTicketDTO` cho mỗi phần tử.
     */
    List<PurchaseDetailDTO.PurchasedSeatedTicketDTO> toPurchasedSeatedTicketDTOs(List<EventSeatStatus> seatedTickets);

    /**
     * Chuyển đổi một EventSeatStatus Entity sang DTO vé ngồi.
     */
    @Mapping(source = "seat.section.name", target = "sectionName")
    @Mapping(source = "seat.rowLabel", target = "rowLabel")
    @Mapping(source = "seat.seatNumber", target = "seatNumber")
    @Mapping(source = "ticket.name", target = "ticketName")
    @Mapping(source = "priceAtPurchase", target = "priceAtPurchase")
    PurchaseDetailDTO.PurchasedSeatedTicketDTO eventSeatStatusToDTO(EventSeatStatus seatStatus);


    // =================================================================
    // CÁC HÀM MAPPING TÙY CHỈNH (QUALIFIED BY NAME)
    // =================================================================

    /**
     * Hàm tùy chỉnh để lấy fullName từ User, nếu không có thì lấy username.
     *
     * @param user Entity User.
     * @return Tên hiển thị của người dùng.
     */
    @Named("userToFullName")
    default String userToFullName(User user) {
        if (user == null) {
            return "N/A";
        }
        return (user.getFullName() != null && !user.getFullName().isEmpty())
                ? user.getFullName()
                : user.getUsername();
    }
}
