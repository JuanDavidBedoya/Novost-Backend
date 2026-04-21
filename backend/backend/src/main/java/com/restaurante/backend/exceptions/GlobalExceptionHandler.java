package com.restaurante.backend.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

// Manejador global de excepciones: captura y formatea errores con respuestas HTTP consistentes

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Maneja excepciones de validación (@Valid): extrae errores de campos y retorna 400 BAD_REQUEST

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        Map<String, Object> response = createErrorResponse("VALIDATION_ERROR", "Error de validación", errors, HttpStatus.BAD_REQUEST);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    // Maneja ResourceNotFoundException: retorna 404 NOT_FOUND con código y campo del error

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFoundException(ResourceNotFoundException ex) {
        Map<String, String> errors = new HashMap<>();
        errors.put(ex.getCampo(), ex.getMessage());
        
        Map<String, Object> response = createErrorResponse(ex.getCodigoError(), ex.getMessage(), errors, HttpStatus.NOT_FOUND);
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    // Maneja DuplicateResourceException: retorna 409 CONFLICT para recursos duplicados

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateResourceException(DuplicateResourceException ex) {
        Map<String, String> errors = new HashMap<>();
        errors.put(ex.getCampo(), ex.getMessage());
        
        Map<String, Object> response = createErrorResponse(ex.getCodigoError(), ex.getMessage(), errors, HttpStatus.CONFLICT);
        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }

    // Maneja InvalidCredentialsException: retorna 401 UNAUTHORIZED para credenciales inválidas

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidCredentialsException(InvalidCredentialsException ex) {
        Map<String, String> errors = new HashMap<>();
        errors.put(ex.getCampo(), ex.getMessage());
        
        Map<String, Object> response = createErrorResponse(ex.getCodigoError(), "Credenciales inválidas", errors, HttpStatus.UNAUTHORIZED);
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }

    // Maneja TokenExpiredException: retorna 401 UNAUTHORIZED para tokens expirados

    @ExceptionHandler(TokenExpiredException.class)
    public ResponseEntity<Map<String, Object>> handleTokenExpiredException(TokenExpiredException ex) {
        Map<String, String> errors = new HashMap<>();
        errors.put(ex.getCampo(), ex.getMessage());
        
        Map<String, Object> response = createErrorResponse(ex.getCodigoError(), ex.getMessage(), errors, HttpStatus.UNAUTHORIZED);
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }

    // Maneja ValidationException: retorna 400 BAD_REQUEST para validaciones de negocio

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(ValidationException ex) {
        Map<String, String> errors = new HashMap<>();
        errors.put(ex.getCampo(), ex.getMessage());
        
        Map<String, Object> response = createErrorResponse(ex.getCodigoError(), ex.getMessage(), errors, HttpStatus.BAD_REQUEST);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    // Maneja PaymentException: retorna 402 PAYMENT_REQUIRED para errores de pago

    @ExceptionHandler(PaymentException.class)
    public ResponseEntity<Map<String, Object>> handlePaymentException(PaymentException ex) {
        Map<String, String> errors = new HashMap<>();
        errors.put(ex.getCampo(), ex.getMessage());
        
        Map<String, Object> response = createErrorResponse(ex.getCodigoError(), ex.getMessage(), errors, HttpStatus.PAYMENT_REQUIRED);
        return new ResponseEntity<>(response, HttpStatus.PAYMENT_REQUIRED);
    }

    // Maneja BusinessException: retorna 400 BAD_REQUEST para errores de lógica de negocio

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, Object>> handleBusinessException(BusinessException ex) {
        Map<String, String> errors = new HashMap<>();
        errors.put(ex.getCampo(), ex.getMessage());
        
        Map<String, Object> response = createErrorResponse(ex.getCodigoError(), "Error de negocio", errors, HttpStatus.BAD_REQUEST);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    // Maneja RuntimeException: retorna 400 BAD_REQUEST y parsea mensaje con formato "campo:error"

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeExceptions(RuntimeException ex) {
        Map<String, String> errors = new HashMap<>();
        String message = ex.getMessage();
        
        if(message != null && message.contains(":")) {
            String[] parts = message.split(":", 2);
            errors.put(parts[0], parts[1]);
        } else {
            errors.put("general", message);
        }
        
        Map<String, Object> response = createErrorResponse("RUNTIME_ERROR", "Ha ocurrido un error", errors, HttpStatus.BAD_REQUEST);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    // Maneja excepciones genéricas: retorna 500 INTERNAL_SERVER_ERROR como fallback

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        Map<String, String> errors = new HashMap<>();
        errors.put("general", "Error interno del servidor: " + ex.getMessage());
        
        Map<String, Object> response = createErrorResponse("INTERNAL_ERROR", "Error interno del servidor", errors, HttpStatus.INTERNAL_SERVER_ERROR);
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // Método auxiliar: construye respuesta de error con timestamp, código, mensaje, errores y estado HTTP

    private Map<String, Object> createErrorResponse(String codigo, String mensaje, Map<String, String> errores, HttpStatus estado) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("codigo", codigo);
        response.put("mensaje", mensaje);
        response.put("errores", errores);
        response.put("estado", estado.value());
        return response;
    }
}
