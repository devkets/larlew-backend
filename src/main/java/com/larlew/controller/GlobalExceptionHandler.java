package com.larlew.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static Logger logger = Logger.getLogger(GlobalExceptionHandler.class.getName());

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, String>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "Invalid parameter type");
        error.put("parameter", ex.getName());
        Class<?> requiredType = ex.getRequiredType();
        error.put("message", String.format("Parameter '%s' must be of type %s", 
            ex.getName(), 
            requiredType != null ? requiredType.getSimpleName() : "unknown"));
        logger.warning("Type mismatch exception: " + error.get("message"));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
}
