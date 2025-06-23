package io.event.ems.mapper;

import io.event.ems.dto.NotificationDTO;
import io.event.ems.model.Event;
import io.event.ems.model.Notification;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    @Mapping(source = "relatedEvent", target = "relatedEvent", qualifiedByName = "toRelatedEventInfo")
    NotificationDTO toDTO(Notification notification);

    @Named("toRelatedEventInfo")
    default NotificationDTO.RelatedEventInfo toRelatedEventInfo(Event event) {
        if (event == null) return null;
        return NotificationDTO.RelatedEventInfo.builder()
                .id(event.getId())
                .title(event.getTitle())
                .slug(event.getSlug())
                .coverImageUrl(event.getCoverImageUrl())
                .build();
    }

}
