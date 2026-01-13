package ru.practicum.explorewithme.service.mapper;

import org.springframework.stereotype.Component;
import ru.practicum.explorewithme.service.dto.*;
import ru.practicum.explorewithme.service.model.Category;
import ru.practicum.explorewithme.service.model.Event;
import ru.practicum.explorewithme.service.model.User;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class EventMapper {

    private final CategoryMapper categoryMapper;
    private final UserMapper userMapper;

    public EventMapper(CategoryMapper categoryMapper, UserMapper userMapper) {
        this.categoryMapper = categoryMapper;
        this.userMapper = userMapper;
    }

    public EventFullDto toFullDto(Event event) {
        if (event == null) {
            return null;
        }

        EventFullDto dto = new EventFullDto();
        dto.setId(event.getId());
        dto.setTitle(event.getTitle());
        dto.setAnnotation(event.getAnnotation());
        dto.setDescription(event.getDescription());
        dto.setCategory(categoryMapper.toDto(event.getCategory()));
        dto.setInitiator(userMapper.toShortDto(event.getInitiator()));
        dto.setEventDate(event.getEventDate());
        dto.setCreatedOn(event.getCreatedOn());
        dto.setPublishedOn(event.getPublishedOn());
        dto.setLocation(LocationDto.fromEntity(event.getLocation()));
        dto.setPaid(event.getPaid());
        dto.setParticipantLimit(event.getParticipantLimit());
        dto.setRequestModeration(event.getRequestModeration());
        dto.setState(event.getState());
        dto.setConfirmedRequests(event.getConfirmedRequests());

        return dto;
    }

    public EventShortDto toShortDto(Event event) {
        if (event == null) {
            return null;
        }

        EventShortDto dto = new EventShortDto();
        dto.setId(event.getId());
        dto.setTitle(event.getTitle());
        dto.setAnnotation(event.getAnnotation());
        dto.setCategory(categoryMapper.toDto(event.getCategory()));
        dto.setInitiator(userMapper.toShortDto(event.getInitiator()));
        dto.setEventDate(event.getEventDate());
        dto.setPaid(event.getPaid());
        dto.setConfirmedRequests(event.getConfirmedRequests());

        return dto;
    }

    public List<EventFullDto> toFullDtoList(List<Event> events) {
        if (events == null) {
            return null;
        }

        return events.stream()
                .map(this::toFullDto)
                .collect(Collectors.toList());
    }

    public List<EventShortDto> toShortDtoList(List<Event> events) {
        if (events == null) {
            return null;
        }

        return events.stream()
                .map(this::toShortDto)
                .collect(Collectors.toList());
    }

    public Event toEntity(NewEventDto newEventDto, Category category, User initiator) {
        if (newEventDto == null) {
            return null;
        }

        return Event.builder()
                .title(newEventDto.getTitle())
                .annotation(newEventDto.getAnnotation())
                .description(newEventDto.getDescription())
                .category(category)
                .eventDate(newEventDto.getEventDate())
                // Конвертируем DTO LocationDto в Entity Location
                .location(newEventDto.getLocation() != null ?
                        newEventDto.getLocation().toEntity() : null)
                .paid(newEventDto.getPaid() != null ? newEventDto.getPaid() : false)
                .participantLimit(newEventDto.getParticipantLimit() != null ?
                        newEventDto.getParticipantLimit() : 0)
                .requestModeration(newEventDto.getRequestModeration() != null ?
                        newEventDto.getRequestModeration() : true)
                .initiator(initiator)
                .build();
    }

    public void updateEventFromAdminRequest(UpdateEventAdminRequest dto, Event event, Category category) {
        if (dto == null || event == null) {
            return;
        }

        if (dto.getAnnotation() != null) {
            event.setAnnotation(dto.getAnnotation());
        }
        if (category != null) {
            event.setCategory(category);
        }
        if (dto.getDescription() != null) {
            event.setDescription(dto.getDescription());
        }
        if (dto.getEventDate() != null) {
            event.setEventDate(dto.getEventDate());
        }
        if (dto.getLocation() != null) {
            event.setLocation(dto.getLocation().toEntity());
        }
        if (dto.getPaid() != null) {
            event.setPaid(dto.getPaid());
        }
        if (dto.getParticipantLimit() != null) {
            event.setParticipantLimit(dto.getParticipantLimit());
        }
        if (dto.getRequestModeration() != null) {
            event.setRequestModeration(dto.getRequestModeration());
        }
        if (dto.getTitle() != null) {
            event.setTitle(dto.getTitle());
        }
    }

    public void updateEventFromUserRequest(UpdateEventUserRequest dto, Event event, Category category) {
        if (dto == null || event == null) {
            return;
        }

        if (dto.getAnnotation() != null) {
            event.setAnnotation(dto.getAnnotation());
        }
        if (category != null) {
            event.setCategory(category);
        }
        if (dto.getDescription() != null) {
            event.setDescription(dto.getDescription());
        }
        if (dto.getEventDate() != null) {
            event.setEventDate(dto.getEventDate());
        }
        if (dto.getLocation() != null) {
            event.setLocation(dto.getLocation().toEntity());
        }
        if (dto.getPaid() != null) {
            event.setPaid(dto.getPaid());
        }
        if (dto.getParticipantLimit() != null) {
            event.setParticipantLimit(dto.getParticipantLimit());
        }
        if (dto.getRequestModeration() != null) {
            event.setRequestModeration(dto.getRequestModeration());
        }
        if (dto.getTitle() != null) {
            event.setTitle(dto.getTitle());
        }
    }
}