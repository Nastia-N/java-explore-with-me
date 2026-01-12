package ru.practicum.explorewithme.service.exception;

public class RequestValidationException extends RuntimeException {
  public RequestValidationException(String message) {
    super(message);
  }
}
