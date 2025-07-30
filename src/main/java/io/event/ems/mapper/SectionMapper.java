package io.event.ems.mapper;

import io.event.ems.dto.SectionDetailDTO;
import io.event.ems.dto.SectionRequestDTO;
import io.event.ems.model.SeatSection;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = {SeatMapper.class})
public interface SectionMapper {

    @Mapping(source = "seatMap.id", target = "seatMapId")
    @Mapping(source = "seats", target = "seats")
    SectionDetailDTO toDetailDTO(SeatSection section);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "seatMap", ignore = true)
    @Mapping(target = "seats", ignore = true)
    @Mapping(target = "capacity", ignore = true)
    SeatSection toEntity(SectionRequestDTO dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "seatMap", ignore = true)
    @Mapping(target = "seats", ignore = true)
    @Mapping(target = "capacity", ignore = true)
    void updateEntityFromDTO(SectionRequestDTO dto, @MappingTarget SeatSection entity);
}
