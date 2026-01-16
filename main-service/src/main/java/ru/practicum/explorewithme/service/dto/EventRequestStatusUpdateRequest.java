package ru.practicum.explorewithme.service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventRequestStatusUpdateRequest {

    private List<Long> requestIds;
    private StatusAction status;

    public enum StatusAction {
        CONFIRMED,
        REJECTED
    }
}
