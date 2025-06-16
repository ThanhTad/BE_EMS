package io.event.ems.mapper;

import io.event.ems.dto.EventCreationDTO;
import io.event.ems.dto.EventResponseDTO;
import io.event.ems.dto.EventSummaryDTO;
import io.event.ems.model.Category;
import io.event.ems.model.Event;
import org.mapstruct.*;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        uses = {UserMapper.class, CategoryMapper.class, VenueMapper.class, StatusCodeMapper.class}
)
public interface EventMapper {

    // Entity to Response DTO
    @Mapping(target = "venueId", source = "venue.id")
    @Mapping(target = "venueName", source = "venue.name")
    @Mapping(target = "venueAddress", source = "venue.address")
    @Mapping(target = "creatorId", source = "creator.id")
    @Mapping(target = "creatorName", source = "creator.name")
    @Mapping(target = "statusId", source = "status.id")
    @Mapping(target = "statusName", source = "status.name")
    @Mapping(target = "categoryIds", source = "categories", qualifiedByName = "categoriesToIds")
    @Mapping(target = "categoryNames", source = "categories", qualifiedByName = "categoriesToNames")
    @Mapping(target = "seatMapId", source = "seatMap.id")
    EventResponseDTO toResponseDTO(Event event);

    // Request DTO to Entity (for creation)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "slug", ignore = true)
    @Mapping(target = "venue", ignore = true)
    @Mapping(target = "creator", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "categories", ignore = true)
    @Mapping(target = "seatMap", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Event toEntity(EventCreationDTO eventCreationDTO);

    // Update entity from DTO
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "slug", ignore = true)
    @Mapping(target = "venue", ignore = true)
    @Mapping(target = "creator", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "categories", ignore = true)
    @Mapping(target = "seatMap", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    // Handled by @UpdateTimestamp
    void updateEntityFromDTO(EventCreationDTO dto, @MappingTarget Event entity);

    // Entity to Info DTO (for lightweight responses)
    @Mapping(target = "venueId", source = "venue.id")
    @Mapping(target = "venueName", source = "venue.name")
    @Mapping(target = "creatorId", source = "creator.id")
    @Mapping(target = "creatorName", source = "creator.name")
    @Mapping(target = "statusId", source = "status.id")
    @Mapping(target = "statusName", source = "status.name")
    @Mapping(target = "categoryCount", source = "categories", qualifiedByName = "categoriesCount")
    EventSummaryDTO toEventSummaryDTO(Event event);

    // Helper methods for category mapping
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

    // Mapping for search results (lighter version)
    @Mapping(target = "venueId", source = "venue.id")
    @Mapping(target = "venueName", source = "venue.name")
    @Mapping(target = "creatorId", source = "creator.id")
    @Mapping(target = "creatorName", source = "creator.name")
    @Mapping(target = "categoryNames", source = "categories", qualifiedByName = "categoriesToNames")
    @Mapping(target = "categoryIds", ignore = true) // Not needed for search results
    @Mapping(target = "statusId", ignore = true) // Not needed for search results
    @Mapping(target = "statusName", ignore = true) // Not needed for search results
    @Mapping(target = "seatMapId", ignore = true)
    // Not needed for search results
    EventResponseDTO toSearchResultDTO(Event event);
}
