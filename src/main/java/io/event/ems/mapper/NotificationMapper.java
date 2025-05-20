package io.event.ems.mapper;

import org.mapstruct.Mapper;

import io.event.ems.dto.NotificationDTO;
import io.event.ems.model.Notification;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    NotificationDTO toDTO(Notification notification);

}
