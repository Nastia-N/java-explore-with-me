package ru.practicum.explorewithme.service.service;

import ru.practicum.explorewithme.service.dto.NewUserRequest;
import ru.practicum.explorewithme.service.dto.UserDto;

import java.util.List;

public interface UserService {

    UserDto createUser(NewUserRequest newUserRequest);

    List<UserDto> getUsers(List<Long> ids, int from, int size);

    void deleteUser(Long userId);

}
