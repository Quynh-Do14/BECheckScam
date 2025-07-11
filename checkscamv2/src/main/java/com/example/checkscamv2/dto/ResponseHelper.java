package com.example.checkscamv2.dto;

import com.example.checkscamv2.dto.response.ResponseObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

/**
 * Helper component for creating standardized HTTP responses
 * Centralizes response creation logic and error handling
 * 
 * @author CheckScam Team
 * @since 2.0
 */
@Slf4j
@Component
public class ResponseHelper {
    
    /**
     * Handle service call with standardized error handling
     * 
     * @param serviceCall The service method to call
     * @param successMessage Message for successful response
     * @param errorMessage Message for error response  
     * @param <T> Return type of service call
     * @return Standardized ResponseEntity
     */
    public <T> ResponseEntity<ResponseObject> handleServiceCall(
            Supplier<T> serviceCall, 
            String successMessage, 
            String errorMessage) {
        
        try {
            T result = serviceCall.get();
            log.debug("Service call successful");
            return createSuccessResponse(result, successMessage);
            
        } catch (IllegalArgumentException e) {
            log.warn("Invalid argument in service call: {}", e.getMessage());
            return createBadRequestResponse("Invalid request: " + e.getMessage());
            
        } catch (Exception e) {
            log.error("Service call failed: {}", e.getMessage(), e);
            return createErrorResponse(errorMessage);
        }
    }
    
    /**
     * Create a successful response (200 OK)
     * 
     * @param data Response data
     * @param message Success message
     * @return ResponseEntity with 200 status
     */
    public ResponseEntity<ResponseObject> createSuccessResponse(Object data, String message) {
        return ResponseEntity.ok(
            ResponseObject.builder()
                .status(HttpStatus.OK)
                .message(message)
                .data(data)
                .build()
        );
    }
    
    /**
     * Create a not found response (404 NOT FOUND)
     * 
     * @param message Error message
     * @return ResponseEntity with 404 status
     */
    public ResponseEntity<ResponseObject> createNotFoundResponse(String message) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ResponseObject.builder()
                .status(HttpStatus.NOT_FOUND)
                .message(message)
                .data(null)
                .build());
    }
    
    /**
     * Create a bad request response (400 BAD REQUEST)
     * 
     * @param message Error message
     * @return ResponseEntity with 400 status
     */
    public ResponseEntity<ResponseObject> createBadRequestResponse(String message) {
        return ResponseEntity.badRequest()
            .body(ResponseObject.builder()
                .status(HttpStatus.BAD_REQUEST)
                .message(message)
                .data(null)
                .build());
    }
    
    /**
     * Create an internal server error response (500 INTERNAL SERVER ERROR)
     * 
     * @param message Error message
     * @return ResponseEntity with 500 status
     */
    public ResponseEntity<ResponseObject> createErrorResponse(String message) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ResponseObject.builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .message(message)
                .data(null)
                .build());
    }
    
    /**
     * Create a custom status response
     * 
     * @param status HTTP status
     * @param message Response message
     * @param data Response data
     * @return ResponseEntity with custom status
     */
    public ResponseEntity<ResponseObject> createCustomResponse(
            HttpStatus status, String message, Object data) {
        
        return ResponseEntity.status(status)
            .body(ResponseObject.builder()
                .status(status)
                .message(message)
                .data(data)
                .build());
    }
    
    /**
     * Create a validation error response (422 UNPROCESSABLE ENTITY)
     * 
     * @param message Validation error message
     * @param errors Validation error details
     * @return ResponseEntity with 422 status
     */
    public ResponseEntity<ResponseObject> createValidationErrorResponse(
            String message, Object errors) {
        
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
            .body(ResponseObject.builder()
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .message(message)
                .data(errors)
                .build());
    }
}
