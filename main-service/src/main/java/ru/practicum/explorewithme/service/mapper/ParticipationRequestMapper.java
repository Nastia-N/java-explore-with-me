package ru.practicum.explorewithme.service.mapper;

import org.springframework.stereotype.Component;
import ru.practicum.explorewithme.service.dto.ParticipationRequestDto;
import ru.practicum.explorewithme.service.model.ParticipationRequest;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class ParticipationRequestMapper {

    public ParticipationRequestDto toDto(ParticipationRequest request) {
        if (request == null) {
            return null;
        }

        ParticipationRequestDto dto = new ParticipationRequestDto();
        dto.setId(request.getId());
        dto.setCreated(request.getCreated());
        dto.setEvent(request.getEvent().getId());
        dto.setRequester(request.getRequester().getId());
        dto.setStatus(request.getStatus());

        return dto;
    }

    public List<ParticipationRequestDto> toDtoList(List<ParticipationRequest> requests) {
        if (requests == null) {
            return null;
        }

        return requests.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
}
