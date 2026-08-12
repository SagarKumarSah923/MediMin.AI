package com.medimin.gateway;

import com.medimin.common.ApiModels;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpStatusCodeException;

@RestControllerAdvice
public class GatewayExceptionHandler {
    @ExceptionHandler(HttpStatusCodeException.class)
    ResponseEntity<ApiModels.ErrorResponse> downstream(HttpStatusCodeException exception) {
        return ResponseEntity.status(exception.getStatusCode())
                .body(new ApiModels.ErrorResponse(exception.getResponseBodyAsString()));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiModels.ErrorResponse> unexpected(Exception exception) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ApiModels.ErrorResponse("MediMin services are temporarily unavailable."));
    }
}