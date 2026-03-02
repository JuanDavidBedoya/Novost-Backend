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

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Maneja los errores de @NotBlank, @Email, @Pattern, etc.
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

    // Maneja excepciones de recursos no encontrados
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFoundException(ResourceNotFoundException ex) {
        Map<String, String> errors = new HashMap<>();
        errors.put(ex.getCampo(), ex.getMessage());
        
        Map<String, Object> response = createErrorResponse(ex.getCodigoError(), ex.getMessage(), errors, HttpStatus.NOT_FOUND);
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    // Maneja excepciones de recursos duplicados
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateResourceException(DuplicateResourceException ex) {
        Map<String, String> errors = new HashMap<>();
        errors.put(ex.getCampo(), ex.getMessage());
        
        Map<String, Object> response = createErrorResponse(ex.getCodigoError(), ex.getMessage(), errors, HttpStatus.CONFLICT);
        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }

    // Maneja excepciones de credenciales inválidas
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidCredentialsException(InvalidCredentialsException ex) {
        Map<String, String> errors = new HashMap<>();
        errors.put(ex.getCampo(), ex.getMessage());
        
        Map<String, Object> response = createErrorResponse(ex.getCodigoError(), "Credenciales inválidas", errors, HttpStatus.UNAUTHORIZED);
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }

    // Maneja excepciones de token expirado
    @ExceptionHandler(TokenExpiredException.class)
    public ResponseEntity<Map<String, Object>> handleTokenExpiredException(TokenExpiredException ex) {
        Map<String, String> errors = new HashMap<>();
        errors.put(ex.getCampo(), ex.getMessage());
        
        Map<String, Object> response = createErrorResponse(ex.getCodigoError(), ex.getMessage(), errors, HttpStatus.UNAUTHORIZED);
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }

    // Maneja excepciones de validación de negocio
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(ValidationException ex) {
        Map<String, String> errors = new HashMap<>();
        errors.put(ex.getCampo(), ex.getMessage());
        
        Map<String, Object> response = createErrorResponse(ex.getCodigoError(), ex.getMessage(), errors, HttpStatus.BAD_REQUEST);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    // Maneja excepciones de pago
    @ExceptionHandler(PaymentException.class)
    public ResponseEntity<Map<String, Object>> handlePaymentException(PaymentException ex) {
        Map<String, String> errors = new HashMap<>();
        errors.put(ex.getCampo(), ex.getMessage());
        
        Map<String, Object> response = createErrorResponse(ex.getCodigoError(), ex.getMessage(), errors, HttpStatus.PAYMENT_REQUIRED);
        return new ResponseEntity<>(response, HttpStatus.PAYMENT_REQUIRED);
    }

    // Maneja excepciones de negocio genéricas
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, Object>> handleBusinessException(BusinessException ex) {
        Map<String, String> errors = new HashMap<>();
        errors.put(ex.getCampo(), ex.getMessage());
        
        Map<String, Object> response = createErrorResponse(ex.getCodigoError(), "Error de negocio", errors, HttpStatus.BAD_REQUEST);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    // Maneja los errores de RuntimeException genéricos
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeExceptions(RuntimeException ex) {
        Map<String, String> errors = new HashMap<>();
        String message = ex.getMessage();
        
        // Si el mensaje tiene el formato "campo:mensaje", lo separamos
        if(message != null && message.contains(":")) {
            String[] parts = message.split(":", 2);
            errors.put(parts[0], parts[1]);
        } else {
            errors.put("general", message);
        }
        
        Map<String, Object> response = createErrorResponse("RUNTIME_ERROR", "Ha ocurrido un error", errors, HttpStatus.BAD_REQUEST);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    // Maneja excepciones no esperadas
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        Map<String, String> errors = new HashMap<>();
        errors.put("general", "Error interno del servidor: " + ex.getMessage());
        
        Map<String, Object> response = createErrorResponse("INTERNAL_ERROR", "Error interno del servidor", errors, HttpStatus.INTERNAL_SERVER_ERROR);
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // Método auxiliar para crear la estructura de respuesta de error
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
