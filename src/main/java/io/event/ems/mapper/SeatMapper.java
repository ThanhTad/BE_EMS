package io.event.ems.mapper;

import io.event.ems.dto.SeatDetailDTO;
import io.event.ems.model.Seat;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface SeatMapper {

    SeatDetailDTO toDetailDTO(Seat seat);

    List<SeatDetailDTO> toDetailDTOs(List<Seat> seats);
}
