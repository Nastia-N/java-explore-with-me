package ru.practicum.explorewithme.service.mapper;

import org.springframework.stereotype.Component;
import ru.practicum.explorewithme.service.dto.CompilationDto;
import ru.practicum.explorewithme.service.dto.NewCompilationDto;
import ru.practicum.explorewithme.service.dto.UpdateCompilationRequest;
import ru.practicum.explorewithme.service.model.Compilation;
import ru.practicum.explorewithme.service.model.Event;

import java.util.Set;
import java.util.stream.Collectors;

@Component
public class CompilationMapper {

    private final EventMapper eventMapper;

    public CompilationMapper(EventMapper eventMapper) {
        this.eventMapper = eventMapper;
    }

    public CompilationDto toDto(Compilation compilation) {
        if (compilation == null) {
            return null;
        }

        CompilationDto dto = new CompilationDto();
        dto.setId(compilation.getId());
        dto.setTitle(compilation.getTitle());
        dto.setPinned(compilation.getPinned());

        if (compilation.getEvents() != null) {
            dto.setEvents(compilation.getEvents().stream()
                    .map(eventMapper::toShortDto)
                    .collect(Collectors.toList()));
        }

        return dto;
    }

    public Compilation toEntity(NewCompilationDto newCompilationDto, Set<Event> events) {
        if (newCompilationDto == null) {
            return null;
        }

        return Compilation.builder()
                .title(newCompilationDto.getTitle())
                .pinned(newCompilationDto.getPinned() != null ? newCompilationDto.getPinned() : false)
                .events(events)
                .build();
    }

    public void updateFromRequest(UpdateCompilationRequest request, Compilation compilation, Set<Event> events) {
        if (request == null || compilation == null) {
            return;
        }

        if (request.getTitle() != null) {
            compilation.setTitle(request.getTitle());
        }
        if (request.getPinned() != null) {
            compilation.setPinned(request.getPinned());
        }
        if (events != null) {
            compilation.setEvents(events);
        }
    }
}
