package com.pandora.coreservice.dto;

import com.pandora.coreservice.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("ApiResponse.success(result) produces envelope with code 1000, payload, and null message")
    void successFactoryCreatesSuccessEnvelope() {
        Map<String, String> payload = Map.of("key", "value");
        ApiResponse<Map<String, String>> response = ApiResponse.success(payload);

        assertThat(response.getCode()).isEqualTo(1000);
        assertThat(response.getMessage()).isNull();
        assertThat(response.getResult()).isEqualTo(payload);

        String json = objectMapper.writeValueAsString(response);
        assertThat(json).contains("\"code\":1000");
        assertThat(json).contains("\"result\":{\"key\":\"value\"}");
        assertThat(json).doesNotContain("\"message\"");
    }

    @Test
    @DisplayName("ApiResponse.success(null) produces envelope with code 1000 and omits result and message in JSON")
    void successFactoryWithNullResultOmitsResultAndMessage() {
        ApiResponse<Void> response = ApiResponse.success(null);

        assertThat(response.getCode()).isEqualTo(1000);
        assertThat(response.getMessage()).isNull();
        assertThat(response.getResult()).isNull();

        String json = objectMapper.writeValueAsString(response);
        assertThat(json).contains("\"code\":1000");
        assertThat(json).doesNotContain("\"result\"");
        assertThat(json).doesNotContain("\"message\"");
        assertThat(json).isEqualTo("{\"code\":1000}");
    }

    @Test
    @DisplayName("ApiResponse.error(ErrorCode) populates code and message from enum and omits result")
    void errorFactoryCreatesErrorEnvelope() {
        ApiResponse<Void> response = ApiResponse.error(ErrorCode.INVALID_CREDENTIALS);

        assertThat(response.getCode()).isEqualTo(ErrorCode.INVALID_CREDENTIALS.getCode());
        assertThat(response.getMessage()).isEqualTo(ErrorCode.INVALID_CREDENTIALS.getMessage());
        assertThat(response.getResult()).isNull();

        String json = objectMapper.writeValueAsString(response);
        assertThat(json).contains("\"code\":1001");
        assertThat(json).contains("\"message\":\"Invalid credentials\"");
        assertThat(json).doesNotContain("\"result\"");
    }

    @Test
    @DisplayName("ApiResponse.error(ErrorCode) provides generic type inference for arbitrary response types")
    void errorFactoryInfersGenericType() {
        ApiResponse<String> stringResponse = ApiResponse.error(ErrorCode.UNAUTHENTICATED);

        assertThat(stringResponse.getCode()).isEqualTo(1002);
        assertThat(stringResponse.getMessage()).isEqualTo("Unauthenticated");
        assertThat(stringResponse.getResult()).isNull();
    }

    @Test
    @DisplayName("Success ApiResponse serializes code and result while omitting null message")
    void successResponseOmitsNullMessage() {
        ApiResponse<Map<String, String>> response = ApiResponse.<Map<String, String>>builder()
                .result(Map.of("accessToken", "xyz", "tokenType", "Bearer"))
                .build();

        String json = objectMapper.writeValueAsString(response);

        assertThat(response.getCode()).isEqualTo(1000);
        assertThat(json).contains("\"code\":1000");
        assertThat(json).contains("\"result\":{");
        assertThat(json).contains("\"accessToken\":\"xyz\"");
        assertThat(json).doesNotContain("\"message\"");
    }

    @Test
    @DisplayName("Error ApiResponse serializes code and message while omitting null result")
    void errorResponseOmitsNullResult() {
        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .code(1001)
                .message("Invalid credentials")
                .build();

        String json = objectMapper.writeValueAsString(response);

        assertThat(json).contains("\"code\":1001");
        assertThat(json).contains("\"message\":\"Invalid credentials\"");
        assertThat(json).doesNotContain("\"result\"");
    }

    @Test
    @DisplayName("ApiResponse builder defaults code to 1000 when not specified")
    void defaultCodeIs1000() {
        ApiResponse<String> response = ApiResponse.<String>builder()
                .result("test-payload")
                .build();

        assertThat(response.getCode()).isEqualTo(1000);
        assertThat(response.getMessage()).isNull();
        assertThat(response.getResult()).isEqualTo("test-payload");
    }

    @Test
    @DisplayName("ApiResponse builder allows overriding code")
    void builderOverridesCode() {
        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .code(1004)
                .message("Malformed request")
                .build();

        assertThat(response.getCode()).isEqualTo(1004);
        assertThat(response.getMessage()).isEqualTo("Malformed request");
        assertThat(response.getResult()).isNull();
    }

    @Test
    @DisplayName("ApiResponse deserializes properly from JSON")
    void deserializesFromJson() {
        String json = "{\"code\":1000,\"result\":{\"key\":\"value\"}}";
        ApiResponse<?> deserialized = objectMapper.readValue(json, ApiResponse.class);

        assertThat(deserialized.getCode()).isEqualTo(1000);
        assertThat(deserialized.getMessage()).isNull();
        assertThat(deserialized.getResult()).isNotNull();
    }
}
