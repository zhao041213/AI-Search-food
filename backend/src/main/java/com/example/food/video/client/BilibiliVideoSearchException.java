package com.example.food.video.client;

public class BilibiliVideoSearchException extends RuntimeException {

    public enum FailureType {
        PRECONDITION_FAILED,
        FORBIDDEN,
        RATE_LIMITED,
        UPSTREAM,
        TIMEOUT,
        INVALID_RESPONSE
    }

    private final FailureType failureType;
    private final Integer statusCode;

    public BilibiliVideoSearchException(
            FailureType failureType,
            String message,
            Integer statusCode,
            Throwable cause
    ) {
        super(message, cause);
        this.failureType = failureType;
        this.statusCode = statusCode;
    }

    public FailureType failureType() {
        return failureType;
    }

    public Integer statusCode() {
        return statusCode;
    }
}
