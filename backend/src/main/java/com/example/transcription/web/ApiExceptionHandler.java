package com.example.transcription.web;

import com.example.transcription.service.Errors;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(Errors.NotFound.class)
    ResponseEntity<Map<String, String>> notFound(Errors.NotFound e) { return body(HttpStatus.NOT_FOUND, e); }

    @ExceptionHandler(Errors.Forbidden.class)
    ResponseEntity<Map<String, String>> forbidden(Errors.Forbidden e) { return body(HttpStatus.FORBIDDEN, e); }

    @ExceptionHandler(Errors.Unauthorized.class)
    ResponseEntity<Map<String, String>> unauthorized(Errors.Unauthorized e) { return body(HttpStatus.UNAUTHORIZED, e); }

    @ExceptionHandler(Errors.BadRequest.class)
    ResponseEntity<Map<String, String>> badRequest(Errors.BadRequest e) { return body(HttpStatus.BAD_REQUEST, e); }

    private ResponseEntity<Map<String, String>> body(HttpStatus s, RuntimeException e) {
        return ResponseEntity.status(s).body(Map.of("error", e.getMessage()));
    }
}
