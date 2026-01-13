package ru.practicum.explorewithme.service.exception;

public class DataIntegrityConflictException extends RuntimeException {
    public DataIntegrityConflictException(String message) {
        super(message);
    }
}
