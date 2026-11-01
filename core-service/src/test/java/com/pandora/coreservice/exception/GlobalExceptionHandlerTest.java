package com.pandora.coreservice.exception;

import com.pandora.coreservice.dto.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("PessimisticLockingFailureException returns 409 CONFLICT with LOCK_CONFLICT envelope and no raw details")
    void handleLockConflictWithPessimisticLockingFailureException() {
        String sensitiveDbDetails = "Deadlock found when trying to get lock; try restarting transaction in table 'employee'";
        PessimisticLockingFailureException exception = new PessimisticLockingFailureException(sensitiveDbDetails);

        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleLockConflict(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getStatusCode().value()).isEqualTo(409);

        ApiResponse<Void> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getCode()).isEqualTo(ErrorCode.LOCK_CONFLICT.getCode());
        assertThat(body.getCode()).isEqualTo(1007);
        assertThat(body.getMessage()).isEqualTo(ErrorCode.LOCK_CONFLICT.getMessage());
        assertThat(body.getMessage()).isEqualTo("Lock acquisition conflict; please retry");
        assertThat(body.getResult()).isNull();

        assertThat(body.getMessage()).doesNotContain("Deadlock", "table", "employee", sensitiveDbDetails);
    }

    @Test
    @DisplayName("CannotAcquireLockException returns 409 CONFLICT with LOCK_CONFLICT envelope and no raw details")
    void handleLockConflictWithCannotAcquireLockException() {
        String sensitiveDbDetails = "Lock wait timeout exceeded; try restarting transaction on index 'idx_dep_manager'";
        CannotAcquireLockException exception = new CannotAcquireLockException(sensitiveDbDetails);

        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleLockConflict(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getStatusCode().value()).isEqualTo(409);

        ApiResponse<Void> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getCode()).isEqualTo(ErrorCode.LOCK_CONFLICT.getCode());
        assertThat(body.getCode()).isEqualTo(1007);
        assertThat(body.getMessage()).isEqualTo(ErrorCode.LOCK_CONFLICT.getMessage());
        assertThat(body.getMessage()).isEqualTo("Lock acquisition conflict; please retry");
        assertThat(body.getResult()).isNull();

        assertThat(body.getMessage()).doesNotContain("Lock wait timeout", "idx_dep_manager", sensitiveDbDetails);
    }

    @Test
    @DisplayName("Generic ConcurrencyFailureException returns 409 CONFLICT with LOCK_CONFLICT envelope and no raw details")
    void handleLockConflictWithConcurrencyFailureException() {
        String rawMessage = "Row was updated or deleted by another transaction (or unsaved-value mapping was incorrect)";
        ConcurrencyFailureException exception = new ConcurrencyFailureException(rawMessage);

        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleLockConflict(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getStatusCode().value()).isEqualTo(409);

        ApiResponse<Void> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getCode()).isEqualTo(ErrorCode.LOCK_CONFLICT.getCode());
        assertThat(body.getCode()).isEqualTo(1007);
        assertThat(body.getMessage()).isEqualTo(ErrorCode.LOCK_CONFLICT.getMessage());
        assertThat(body.getMessage()).isEqualTo("Lock acquisition conflict; please retry");
        assertThat(body.getResult()).isNull();

        assertThat(body.getMessage()).doesNotContain("unsaved-value", "updated or deleted", rawMessage);
    }

    @Test
    @DisplayName("handleLockConflict is mapped to PessimisticLockingFailureException, CannotAcquireLockException, and ConcurrencyFailureException")
    void handleLockConflictAnnotationContract() throws NoSuchMethodException {
        Method method = GlobalExceptionHandler.class.getMethod("handleLockConflict", ConcurrencyFailureException.class);
        ExceptionHandler annotation = method.getAnnotation(ExceptionHandler.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.value()).containsExactlyInAnyOrder(
                PessimisticLockingFailureException.class,
                CannotAcquireLockException.class,
                ConcurrencyFailureException.class
        );
    }
}
