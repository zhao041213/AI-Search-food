package com.example.food.common;

import com.example.food.admin.error.AdminErrorLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.beans.factory.ObjectProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String INVALID_REQUEST_PARAMETERS = "Invalid request parameters";

    private final ObjectProvider<AdminErrorLogService> errorLogServiceProvider;

    public GlobalExceptionHandler(ObjectProvider<AdminErrorLogService> errorLogServiceProvider) {
        this.errorLogServiceProvider = errorLogServiceProvider;
    }

    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            BindException.class,
            ConstraintViolationException.class,
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleInvalidRequest(Exception exception) {
        return ResponseEntity.badRequest().body(ApiResponse.error(400, INVALID_REQUEST_PARAMETERS));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(ApiResponse.error(400, exception.getMessage()));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Void>> handleResponseStatus(
            ResponseStatusException exception,
            HttpServletRequest request
    ) {
        HttpStatusCode statusCode = exception.getStatusCode();
        return statusEnvelope(statusCode, exception.getReason(), exception, request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResource(NoResourceFoundException exception) {
        return statusEnvelope(exception.getStatusCode(), null, exception, null);
    }

    @ExceptionHandler({
            HttpRequestMethodNotSupportedException.class,
            HttpMediaTypeNotSupportedException.class,
            HttpMediaTypeNotAcceptableException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleSpringMvcStatusException(
            Exception exception,
            HttpServletRequest request
    ) {
        return errorResponseEnvelope((ErrorResponse) exception, exception, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(
            Exception exception,
            HttpServletRequest request
    ) {
        if (exception instanceof ErrorResponse errorResponse) {
            return errorResponseEnvelope(errorResponse, exception, request);
        }
        log.error("Unexpected exception", exception);
        recordFailure(exception, HttpStatus.INTERNAL_SERVER_ERROR.value(), request);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(500, "Internal server error"));
    }

    private ResponseEntity<ApiResponse<Void>> errorResponseEnvelope(
            ErrorResponse errorResponse,
            Throwable exception,
            HttpServletRequest request
    ) {
        return statusEnvelope(errorResponse.getStatusCode(), errorResponseMessage(errorResponse), exception, request);
    }

    private String errorResponseMessage(ErrorResponse errorResponse) {
        String title = errorResponse.getBody().getTitle();
        if (title != null && !title.isBlank()) {
            return title;
        }
        String detail = errorResponse.getBody().getDetail();
        if (detail != null && !detail.isBlank()) {
            return detail;
        }
        return null;
    }

    private ResponseEntity<ApiResponse<Void>> statusEnvelope(
            HttpStatusCode statusCode,
            String reason,
            Throwable exception,
            HttpServletRequest request
    ) {
        recordFailure(exception, statusCode.value(), request);
        return ResponseEntity
                .status(statusCode)
                .body(ApiResponse.error(statusCode.value(), statusMessage(statusCode, reason)));
    }

    private void recordFailure(Throwable exception, int statusCode, HttpServletRequest request) {
        if (exception == null || statusCode < 500) {
            return;
        }
        AdminErrorLogService service = errorLogServiceProvider.getIfAvailable();
        if (service == null) {
            return;
        }
        service.recordException(exception, request, statusCode);
    }

    private String statusMessage(HttpStatusCode statusCode, String reason) {
        if (reason != null && !reason.isBlank()) {
            return reason;
        }
        if (statusCode instanceof HttpStatus httpStatus) {
            return httpStatus.getReasonPhrase();
        }
        return "HTTP " + statusCode.value();
    }
}
