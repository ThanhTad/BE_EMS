package io.event.ems.mapper;

import io.event.ems.dto.*;
import io.event.ems.model.Category;
import io.event.ems.model.Event;
import io.event.ems.model.User;
import io.event.ems.model.Venue;
import org.mapstruct.*;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        uses = {UserMapper.class, CategoryMapper.class, VenueMapper.class, StatusCodeMapper.class, SeatMapMapper.class}
)
public interface EventMapper {


    EventResponseDTO toResponseDTO(Event event);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "slug", ignore = true)
    @Mapping(target = "venue", ignore = true)
    @Mapping(target = "creator", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "categories", ignore = true)
    @Mapping(target = "seatMap", ignore = true)
    @Mapping(target = "latitude", ignore = true)
    @Mapping(target = "longitude", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Event toEntity(EventCreationDTO eventCreationDTO);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "slug", ignore = true)
    @Mapping(target = "venue", ignore = true)
    @Mapping(target = "creator", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "categories", ignore = true)
    @Mapping(target = "seatMap", ignore = true)
    @Mapping(target = "latitude", ignore = true)
    @Mapping(target = "longitude", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromDTO(EventCreationDTO dto, @MappingTarget Event entity);

    @Mapping(target = "venueName", ignore = true)
    EventSummaryDTO toEventSummaryDTO(Event event);

    @Named("categoriesToIds")
    default Set<UUID> categoriesToIds(Set<Category> categories) {
        if (categories == null || categories.isEmpty()) {
            return null;
        }
        return categories.stream()
                .map(Category::getId)
                .collect(Collectors.toSet());
    }

    @Named("categoriesToNames")
    default Set<String> categoriesToNames(Set<Category> categories) {
        if (categories == null || categories.isEmpty()) {
            return null;
        }
        return categories.stream()
                .map(Category::getName)
                .collect(Collectors.toSet());
    }

    @Named("categoriesCount")
    default Integer categoriesCount(Set<Category> categories) {
        return categories != null ? categories.size() : 0;
    }


    EventResponseDTO toSearchResultDTO(Event event);

    @Mapping(source = "id", target = "eventId")
    @Mapping(source = "title", target = "eventTitle")
    @Mapping(source = "description", target = "eventDescription")
    @Mapping(source = "startDate", target = "eventStartDate")
    @Mapping(source = "endDate", target = "eventEndDate")
    @Mapping(source = "coverImageUrl", target = "coverImageUrl")
    @Mapping(source = "creator", target = "creator")
    @Mapping(source = "venue", target = "venue")
    @Mapping(source = "isPublic", target = "isPublic")
    @Mapping(source = "ticketSelectionMode", target = "ticketSelectionMode")
    @Mapping(target = "ticketingData", ignore = true)
    EventTicketingResponseDTO eventToEventTicketingResponseDto(Event event);

    EventCreatorDTO userToEventCreatorDto(User user);

    @Mapping(source = "venue.id", target = "venueId")
    EventVenueDTO venueToEventVenueDto(Venue venue);
}
