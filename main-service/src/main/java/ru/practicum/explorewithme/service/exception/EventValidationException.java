package ru.practicum.explorewithme.service.exception;

public class EventValidationException extends RuntimeException {
    public EventValidationException(String message) {
        super(message);
    }
}
