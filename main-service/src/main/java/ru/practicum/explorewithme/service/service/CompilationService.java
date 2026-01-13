package ru.practicum.explorewithme.service.service;

import ru.practicum.explorewithme.service.dto.CompilationDto;
import ru.practicum.explorewithme.service.dto.NewCompilationDto;
import ru.practicum.explorewithme.service.dto.UpdateCompilationRequest;

import java.util.List;

public interface CompilationService {

    CompilationDto createCompilation(NewCompilationDto newCompilationDto);

    CompilationDto updateCompilation(Long compId, UpdateCompilationRequest updateRequest);

    void deleteCompilation(Long compId);

    List<CompilationDto> getCompilations(Boolean pinned, Integer from, Integer size);

    CompilationDto getCompilation(Long compId);

}
