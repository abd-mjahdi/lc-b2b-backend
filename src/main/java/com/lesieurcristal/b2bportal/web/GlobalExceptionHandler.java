package com.lesieurcristal.b2bportal.web;

import com.lesieurcristal.b2bportal.auth.AuthException;
import com.lesieurcristal.b2bportal.order.OrderException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<Map<String, Object>> handleAuthException(AuthException ex) {
        return ResponseEntity.status(ex.getStatus()).body(errorBody(
                ex.getStatus().value(),
                ex.getCode(),
                ex.getMessage()
        ));
    }

    @ExceptionHandler(OrderException.class)
    public ResponseEntity<Map<String, Object>> handleOrderException(OrderException ex) {
        return ResponseEntity.status(ex.getStatus()).body(errorBody(
                ex.getStatus().value(),
                ex.getCode(),
                ex.getMessage()
        ));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(ConstraintViolationException ex) {
        return ResponseEntity.badRequest().body(errorBody(
                HttpStatus.BAD_REQUEST.value(),
                "VALIDATION_ERROR",
                ex.getMessage() != null ? ex.getMessage() : "Requête invalide"
        ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }

        Map<String, Object> body = errorBody(
                HttpStatus.BAD_REQUEST.value(),
                "VALIDATION_ERROR",
                "Request validation failed"
        );
        body.put("fields", fieldErrors);
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatus(ResponseStatusException ex) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        String message = ex.getReason() != null ? ex.getReason() : status.getReasonPhrase();
        return ResponseEntity.status(status).body(errorBody(status.value(), status.name(), message));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleUploadTooLarge(MaxUploadSizeExceededException ex) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(errorBody(
                HttpStatus.PAYLOAD_TOO_LARGE.value(),
                "PAYLOAD_TOO_LARGE",
                "Fichier trop volumineux (maximum 5 Mo)"
        ));
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<Map<String, Object>> handleMultipart(MultipartException ex) {
        return ResponseEntity.badRequest().body(errorBody(
                HttpStatus.BAD_REQUEST.value(),
                "BAD_REQUEST",
                "Requête multipart invalide"
        ));
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleEntityNotFound(EntityNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody(
                HttpStatus.NOT_FOUND.value(),
                "NOT_FOUND",
                ex.getMessage() != null ? ex.getMessage() : "Ressource introuvable"
        ));
    }

    /**
     * Role / ownership failures from services. Cross-tenant reads prefer 404
     * so existence is not leaked; role-gate messages stay 403.
     */
    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Map<String, Object>> handleSecurityException(SecurityException ex) {
        if (looksLikeCrossTenantOrMissing(ex.getMessage())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody(
                    HttpStatus.NOT_FOUND.value(),
                    "NOT_FOUND",
                    "Ressource introuvable"
            ));
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorBody(
                HttpStatus.FORBIDDEN.value(),
                "ACCESS_DENIED",
                ex.getMessage() != null ? ex.getMessage() : "Accès refusé"
        ));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorBody(
                HttpStatus.FORBIDDEN.value(),
                "ACCESS_DENIED",
                "Accès refusé"
        ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(errorBody(
                HttpStatus.BAD_REQUEST.value(),
                "BAD_REQUEST",
                ex.getMessage() != null ? ex.getMessage() : "Requête invalide"
        ));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException ex) {
        String message = ex.getMessage();
        if (message != null && message.toLowerCase(Locale.ROOT).contains("non authentifié")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorBody(
                    HttpStatus.UNAUTHORIZED.value(),
                    "UNAUTHORIZED",
                    message
            ));
        }
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorBody(
                HttpStatus.CONFLICT.value(),
                "CONFLICT",
                message != null ? message : "État invalide"
        ));
    }

    private static boolean looksLikeCrossTenantOrMissing(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }
        String m = message.toLowerCase(Locale.ROOT);
        return m.contains("n'appartient")
                || m.contains("appartient pas")
                || m.contains("accès refusé")
                || m.contains("acces refuse")
                || m.contains("introuvable");
    }

    private static Map<String, Object> errorBody(int status, String code, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", status);
        body.put("code", code);
        body.put("message", message);
        return body;
    }
}
