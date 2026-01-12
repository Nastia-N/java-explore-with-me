package ru.practicum.explorewithme.service.mapper;

import org.springframework.stereotype.Component;
import ru.practicum.explorewithme.service.dto.UserDto;
import ru.practicum.explorewithme.service.dto.NewUserRequest;
import ru.practicum.explorewithme.service.dto.UserShortDto;
import ru.practicum.explorewithme.service.model.User;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class UserMapper {

    public UserDto toDto(User user) {
        if (user == null) {
            return null;
        }

        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        return dto;
    }

    public UserShortDto toShortDto(User user) {
        if (user == null) {
            return null;
        }

        UserShortDto dto = new UserShortDto();
        dto.setId(user.getId());
        dto.setName(user.getName());
        return dto;
    }

    public List<UserDto> toDtoList(List<User> users) {
        if (users == null) {
            return null;
        }

        return users.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public User toEntity(NewUserRequest newUserRequest) {
        if (newUserRequest == null) {
            return null;
        }

        return User.builder()
                .name(newUserRequest.getName())
                .email(newUserRequest.getEmail())
                .build();
    }
}