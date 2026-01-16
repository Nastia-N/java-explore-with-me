package ru.practicum.explorewithme.service.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.explorewithme.service.dto.NewUserRequest;
import ru.practicum.explorewithme.service.dto.UserDto;
import ru.practicum.explorewithme.service.service.UserService;

import java.util.List;

@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
@Slf4j
@Validated
public class AdminUserController {

    private final UserService userService;

    private static final int DEFAULT_PAGE_OFFSET = 0;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final String PAGE_OFFSET_DESCRIPTION = "Начальное смещение (количество элементов для пропуска)";
    private static final String PAGE_SIZE_DESCRIPTION = "Количество элементов на странице";

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto createUser(@Valid @RequestBody NewUserRequest newUserRequest) {
        log.info("Admin: Создание пользователя с email: {}", newUserRequest.getEmail());
        return userService.createUser(newUserRequest);
    }

    @GetMapping
    public List<UserDto> getUsers(
            @RequestParam(required = false) List<Long> ids,
            @RequestParam(defaultValue = DEFAULT_PAGE_OFFSET + "")
            @PositiveOrZero Integer from,
            @RequestParam(defaultValue = DEFAULT_PAGE_SIZE + "")
            @Positive Integer size) {

        log.info("Admin: Получение пользователей по ids: {}, from: {}, size: {}", ids, from, size);
        return userService.getUsers(ids, from, size);
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable Long userId) {
        log.info("Admin: Удаление пользователя с id: {}", userId);
        userService.deleteUser(userId);
    }
}