# Solips

## Authentication & Security

The core service implements stateless JWT-based authentication using Spring Security OAuth2 Resource Server with JOSE Nimbus HS256 algorithm pinning.

### Configuration

- **`JWT_SIGNING_KEY`**: Base64-encoded secret key used for HMAC-SHA256 signing and verification.
  - Required at startup (application fails closed if missing, invalid Base64, or decoded length < 32 bytes / 256 bits).
  - Can be supplied via the `JWT_SIGNING_KEY` environment variable or `jwt.signing-key` property.
  - **Root `.env` Loading**: The application natively imports root `.env` configuration using Spring Boot's optional config data imports (`optional:file:.env[.properties]` and `optional:file:../.env[.properties]`), supporting execution from either the repository root or the `core-service` working directory.
  - **Environment Precedence**: Real OS environment variables (`JWT_SIGNING_KEY`) have higher precedence and automatically override properties defined in `.env`.
  - **Secret Hygiene**: Local `.env` files are ignored by git across root and nested directories and must never be committed to source control. Developers should maintain their own local `.env` with a securely generated 256-bit (>= 32 bytes decoded) Base64 string.

### Endpoints

#### Authentication: Login
- **Method & Path**: `POST /api/auth/login` (publicly accessible; all other HTTP methods and paths require Bearer authentication).
- **Request Headers**: `Content-Type: application/json`
- **Request Body**:
  ```json
  {
    "username": "alice",
    "password": "your_password"
  }
  ```
- **Success Response (`200 OK`)**:
  ```json
  {
    "code": 1000,
    "result": {
      "accessToken": "<signed-jwt-token>",
      "tokenType": "Bearer",
      "expiresIn": 3600
    }
  }
  ```
- **Error Responses**:
  - `400 Bad Request`: Malformed JSON or missing required fields (`username`, `password`).
    ```json
    {
      "code": 1004,
      "message": "Malformed request"
    }
    ```
  - `401 Unauthorized`: Generic response for unknown username, invalid password, or disabled user.
    ```json
    {
      "code": 1001,
      "message": "Invalid credentials"
    }
    ```

### API Response Specification

All REST API responses conform to a unified `ApiResponse<T>` envelope with JSON null omission:
- **Success Responses**: Return application `code: 1000` with payload in `result`. The `message` field is `null` and omitted from JSON.
- **Error Responses**: Return an application error `code` and `message`. The `result` field is `null` and omitted from JSON.

#### Static Factory Usage

`ApiResponse<T>` provides convenient static factory methods:
- `ApiResponse.success(result)`: Produces a success envelope (`code: 1000`, `result`, `message: null`). When `result` is `null`, both `result` and `message` are omitted in JSON serialization (`{"code":1000}`).
- `ApiResponse.error(errorCode)`: Produces an error envelope from an `ErrorCode` (`code` and `message` from `ErrorCode`, `result: null`). Infers generic type parameter `<T>` for flexible controller and exception handler returns.

```java
// Controller success response
return ApiResponse.success(response);

// Exception handler error response
return ResponseEntity.status(errorCode.getHttpStatus())
        .body(ApiResponse.error(errorCode));
```

#### Error Code Reference

| Code | HTTP Status | Description / Semantics |
| :--- | :--- | :--- |
| `1000` | `200 OK` | Request succeeded |
| `1001` | `401 Unauthorized` | Invalid credentials (unknown user, bad password, or disabled user) |
| `1002` | `401 Unauthorized` | Unauthenticated (missing, expired, or invalid Bearer token) |
| `1003` | `403 Forbidden` | Access denied (insufficient permissions) |
| `1004` | `400 Bad Request` | Malformed request (invalid JSON or validation failure) |
| `9999` | `500 Internal Server Error` | Unexpected server error (safe generic error without leakage) |

### Token Specification
- **Algorithm**: HS256 (HMAC with SHA-256), pinned.
- **Claims**:
  - `sub`: User ID (UUID string format).
  - `iat`: Issued at timestamp.
  - `exp`: Expiration timestamp (1 hour / 3600 seconds from issuance).

### Accessing Protected Endpoints
Protected endpoints require the standard `Authorization` header:
```http
Authorization: Bearer <accessToken>
```
Unauthenticated, expired, or tampered token requests receive `401 Unauthorized` with a standard `WWW-Authenticate: Bearer` challenge and standard error envelope (`code: 1002`, `message: "Unauthenticated"`). Sessions are strictly stateless (`sessionCreationPolicy: STATELESS`) and CSRF protection is disabled for stateless API operations.
