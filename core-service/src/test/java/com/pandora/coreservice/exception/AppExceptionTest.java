package com.pandora.coreservice.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class AppExceptionTest {

    @Test
    @DisplayName("AppException holds ErrorCode and exposes getErrorCode and message")
    void appExceptionHoldsErrorCode() {
        AppException exception = new AppException(ErrorCode.INVALID_CREDENTIALS);

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_CREDENTIALS);
        assertThat(exception.getMessage()).isEqualTo("Invalid credentials");
        assertThat(exception).isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("ErrorCode enums have correct codes, messages, and HTTP statuses")
    void errorCodeMapping() {
        assertThat(ErrorCode.UNCATEGORIZED_EXCEPTION.getCode()).isEqualTo(9999);
        assertThat(ErrorCode.UNCATEGORIZED_EXCEPTION.getMessage()).isEqualTo("Internal server error");
        assertThat(ErrorCode.UNCATEGORIZED_EXCEPTION.getHttpStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);

        assertThat(ErrorCode.INVALID_CREDENTIALS.getCode()).isEqualTo(1001);
        assertThat(ErrorCode.INVALID_CREDENTIALS.getMessage()).isEqualTo("Invalid credentials");
        assertThat(ErrorCode.INVALID_CREDENTIALS.getHttpStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);

        assertThat(ErrorCode.UNAUTHENTICATED.getCode()).isEqualTo(1002);
        assertThat(ErrorCode.UNAUTHENTICATED.getMessage()).isEqualTo("Unauthenticated");
        assertThat(ErrorCode.UNAUTHENTICATED.getHttpStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);

        assertThat(ErrorCode.ACCESS_DENIED.getCode()).isEqualTo(1003);
        assertThat(ErrorCode.ACCESS_DENIED.getMessage()).isEqualTo("Access denied");
        assertThat(ErrorCode.ACCESS_DENIED.getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN);

        assertThat(ErrorCode.INVALID_REQUEST.getCode()).isEqualTo(1004);
        assertThat(ErrorCode.INVALID_REQUEST.getMessage()).isEqualTo("Malformed request");
        assertThat(ErrorCode.INVALID_REQUEST.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);

        assertThat(ErrorCode.RESOURCE_NOT_FOUND.getCode()).isEqualTo(1005);
        assertThat(ErrorCode.RESOURCE_NOT_FOUND.getMessage()).isEqualTo("Resource not found");
        assertThat(ErrorCode.RESOURCE_NOT_FOUND.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);

        assertThat(ErrorCode.DUPLICATE_RESOURCE.getCode()).isEqualTo(1006);
        assertThat(ErrorCode.DUPLICATE_RESOURCE.getMessage()).isEqualTo("Resource already exists");
        assertThat(ErrorCode.DUPLICATE_RESOURCE.getHttpStatus()).isEqualTo(HttpStatus.CONFLICT);

        assertThat(ErrorCode.LOCK_CONFLICT.getCode()).isEqualTo(1007);
        assertThat(ErrorCode.LOCK_CONFLICT.getMessage()).isEqualTo("Lock acquisition conflict; please retry");
        assertThat(ErrorCode.LOCK_CONFLICT.getHttpStatus()).isEqualTo(HttpStatus.CONFLICT);
    }
}
