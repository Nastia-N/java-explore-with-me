package ru.practicum.explorewithme.service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.explorewithme.service.dto.CompilationDto;
import ru.practicum.explorewithme.service.dto.NewCompilationDto;
import ru.practicum.explorewithme.service.dto.UpdateCompilationRequest;
import ru.practicum.explorewithme.service.exception.NotFoundException;
import ru.practicum.explorewithme.service.mapper.CompilationMapper;
import ru.practicum.explorewithme.service.model.Compilation;
import ru.practicum.explorewithme.service.model.Event;
import ru.practicum.explorewithme.service.repository.CompilationRepository;
import ru.practicum.explorewithme.service.repository.EventRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CompilationServiceImpl implements CompilationService {

    private final CompilationRepository compilationRepository;
    private final EventRepository eventRepository;
    private final CompilationMapper compilationMapper;

    @Override
    @Transactional
    public CompilationDto createCompilation(NewCompilationDto newCompilationDto) {
        log.info("Создание новой подборки с заголовком: {}", newCompilationDto.getTitle());

        Set<Event> events = new HashSet<>();
        if (newCompilationDto.getEvents() != null && !newCompilationDto.getEvents().isEmpty()) {
            events = new HashSet<>(eventRepository.findAllByIdIn(newCompilationDto.getEvents()));

            if (events.size() != newCompilationDto.getEvents().size()) {
                log.warn("Некоторые события не найдены при создании подборки");
            }
        }

        Compilation compilation = compilationMapper.toEntity(newCompilationDto, events);
        Compilation savedCompilation = compilationRepository.save(compilation);

        log.info("Создана подборка с ID: {}", savedCompilation.getId());
        return compilationMapper.toDto(savedCompilation);
    }

    @Override
    @Transactional
    public CompilationDto updateCompilation(Long compId, UpdateCompilationRequest updateRequest) {
        log.info("Обновление подборки с ID: {}", compId);

        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Compilation", compId));

        Set<Event> events = null;
        if (updateRequest.getEvents() != null) {
            events = new HashSet<>(eventRepository.findAllByIdIn(updateRequest.getEvents()));

            if (events.size() != updateRequest.getEvents().size()) {
                log.warn("Некоторые события не найдены при обновлении подборки");
            }
        }

        compilationMapper.updateFromRequest(updateRequest, compilation, events);
        Compilation updatedCompilation = compilationRepository.save(compilation);

        log.info("Подборка с ID: {} обновлена", compId);
        return compilationMapper.toDto(updatedCompilation);
    }

    @Override
    @Transactional
    public void deleteCompilation(Long compId) {
        log.info("Удаление подборки с ID: {}", compId);

        if (!compilationRepository.existsById(compId)) {
            throw new NotFoundException("Compilation", compId);
        }

        compilationRepository.deleteById(compId);
        log.info("Подборка с ID: {} удалена", compId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompilationDto> getCompilations(Boolean pinned, Integer from, Integer size) {
        log.info("Получение подборок. Pinned: {}, from: {}, size: {}", pinned, from, size);

        validatePaginationParams(from, size);
        PageRequest pageRequest = PageRequest.of(from / size, size);

        List<Compilation> compilations;
        if (pinned != null) {
            compilations = compilationRepository.findAllByPinned(pinned, pageRequest).getContent();
        } else {
            compilations = compilationRepository.findAll(pageRequest).getContent();
        }

        return compilations.stream()
                .map(compilationMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CompilationDto getCompilation(Long compId) {
        log.info("Получение подборки с ID: {}", compId);

        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Compilation", compId));

        return compilationMapper.toDto(compilation);
    }

    private void validatePaginationParams(Integer from, Integer size) {
        if (size <= 0) {
            throw new IllegalArgumentException("Параметр size должен быть больше 0");
        }
        if (from < 0) {
            throw new IllegalArgumentException("Параметр from должен быть неотрицательным");
        }
    }
}
