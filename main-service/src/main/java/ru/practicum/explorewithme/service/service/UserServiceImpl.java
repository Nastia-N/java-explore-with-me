package ru.practicum.explorewithme.service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.explorewithme.service.dto.UserDto;
import ru.practicum.explorewithme.service.dto.NewUserRequest;
import ru.practicum.explorewithme.service.exception.ConflictException;
import ru.practicum.explorewithme.service.exception.NotFoundException;
import ru.practicum.explorewithme.service.mapper.UserMapper;
import ru.practicum.explorewithme.service.model.User;
import ru.practicum.explorewithme.service.repository.UserRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public UserDto createUser(NewUserRequest newUserRequest) {
        log.info("Создание нового пользователя с email: {}", newUserRequest.getEmail());

        if (userRepository.existsByEmail(newUserRequest.getEmail())) {
            throw new ConflictException("Пользователь с email=" + newUserRequest.getEmail() + " уже существует");
        }

        User user = userMapper.toEntity(newUserRequest);
        User savedUser = userRepository.save(user);

        log.info("Создан пользователь с id: {}", savedUser.getId());
        return userMapper.toDto(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDto> getUsers(List<Long> ids, int from, int size) {
        log.info("Получение пользователей с ids: {}, from: {}, size: {}", ids, from, size);

        if (size <= 0) {
            throw new IllegalArgumentException("Параметр size должен быть больше 0");
        }
        if (from < 0) {
            throw new IllegalArgumentException("Параметр from должен быть неотрицательным");
        }

        PageRequest pageRequest = PageRequest.of(from / size, size);
        List<User> users;

        if (ids == null || ids.isEmpty()) {
            users = userRepository.findAll(pageRequest).getContent();
        } else {
            users = userRepository.findAllByIdIn(ids, pageRequest).getContent();
        }

        return users.stream()
                .map(userMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteUser(Long userId) {
        log.info("Удаление пользователя с id: {}", userId);

        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("User", userId);
        }

        userRepository.deleteById(userId);
        log.info("Пользователь с id {} успешно удалён", userId);
    }
}