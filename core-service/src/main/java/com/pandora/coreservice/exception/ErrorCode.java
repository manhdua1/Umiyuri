package com.pandora.coreservice.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    UNCATEGORIZED_EXCEPTION(9999, "Internal server error", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_CREDENTIALS(1001, "Invalid credentials", HttpStatus.UNAUTHORIZED),
    UNAUTHENTICATED(1002, "Unauthenticated", HttpStatus.UNAUTHORIZED),
    ACCESS_DENIED(1003, "Access denied", HttpStatus.FORBIDDEN),
    INVALID_REQUEST(1004, "Malformed request", HttpStatus.BAD_REQUEST),
    RESOURCE_NOT_FOUND(1005, "Resource not found", HttpStatus.NOT_FOUND),
    DUPLICATE_RESOURCE(1006, "Resource already exists", HttpStatus.CONFLICT),
    LOCK_CONFLICT(1007, "Lock acquisition conflict; please retry", HttpStatus.CONFLICT);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;
}
