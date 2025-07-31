# StreamRepo API Authentication Endpoints

This README documents the authentication endpoints for the StreamRepo API, accessible under the base URL `https://stream-repo-l30u.onrender.com/api/auth`. These endpoints handle user registration, login, magic link operations, OAuth2 authentication, and password resets.

## Base URL
```
https://stream-repo-l30u.onrender.com
```

---

## 1. Register User
### Overview
Creates a new user account, validates input, encrypts the password, and sends a magic link for email verification.

### Endpoint
```
POST /api/auth/register
```

### Request
- **Headers**: `Content-Type: application/json`
- **Body** (`UserDTO`):
  | Field      | Type   | Description                     | Required |
  |------------|--------|---------------------------------|----------|
  | `username` | String | Unique username (3–20 chars)   | Yes      |
  | `email`    | String | Valid email address            | Yes      |
  | `password` | String | Password (min 8 chars, mixed)  | Yes      |
  | `name`     | String | Full name (max 50 chars)       | Yes      |
  | `bio`      | String | User bio (max 200 chars)       | No       |
  | `picture`  | String | Profile picture URL/Base64     | No       |

- **Example**:
  ```json
  {
    "username": "zipDemon",
    "email": "okoroaforkelechi123@streamrepo.com",
    "password": "SecurePass123!",
    "name": "Kelechi Divine",
    "bio": "Javascript is fun",
    "picture": ""
  }
  ```

### Response
- **Success**: `201 Created`
  ```json
  {
    "timestamp": "2025-07-31T15:09:00.123+01:00",
    "message": "Your account has been created successfully",
    "status": "CREATED"
  }
  ```
- **Errors**:
  - `400 Bad Request`: Invalid email, weak password, or duplicate username/email.
  - `500 Internal Server Error`: Database or email service failure.

### Example
```bash
curl -X POST https://stream-repo-l30u.onrender.com/api/auth/register \
-H "Content-Type: application/json" \
-d '{"username":"zipDemon","email":"okoroaforkelechi123@streamrepo.com","password":"SecurePass123!","name":"Kelechi Divine","bio":"Javascript is fun","picture":""}'
```

---

## 2. Login
### Overview
Authenticates a user with username and password, returning a JWT token.

### Endpoint
```
POST /api/auth/login
```

### Request
- **Headers**: `Content-Type: application/json`
- **Body** (`UserDTO`):
  | Field      | Type   | Description            | Required |
  |------------|--------|------------------------|----------|
  | `username` | String | User’s username        | Yes      |
  | `password` | String | User’s password        | Yes      |

- **Example**:
  ```json
  {
    "username": "zipDemon",
    "password": "SecurePass123!"
  }
  ```

### Response
- **Success**: `200 OK`
  ```json
  {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "responseDetails": {
      "timestamp": "2025-07-31T15:09:00.123+01:00",
      "message": "Login successful",
      "status": "OK"
    }
  }
  ```
- **Error**: `401 Unauthorized`
  ```json
  {
    "timestamp": "2025-07-31T15:09:00.123+01:00",
    "message": "Invalid credentials",
    "status": "UNAUTHORIZED"
  }
  ```

### Example
```bash
curl -X POST https://stream-repo-l30u.onrender.com/api/auth/login \
-H "Content-Type: application/json" \
-d '{"username":"zipDemon","password":"SecurePass123!"}'
```

---

## 3. Request Magic Link
### Overview
Sends a magic link to the user’s email for account verification or passwordless login.

### Endpoint
```
POST /api/auth/magic-link
```

### Request
- **Headers**: `Content-Type: application/json`
- **Body** (`MagicLinkRequest`):
  | Field   | Type   | Description            | Required |
  |---------|--------|------------------------|----------|
  | `email` | String | User’s email address   | Yes      |

- **Example**:
  ```json
  {
    "email": "okoroaforkelechi123@streamrepo.com"
  }
  ```

### Response
- **Success**: `200 OK`
  ```json
  {
    "timestamp": "2025-07-31T15:09:00.123+01:00",
    "message": "Magic link sent to email",
    "status": "OK"
  }
  ```
- **Error**: `400 Bad Request`
  ```json
  {
    "timestamp": "2025-07-31T15:09:00.123+01:00",
    "message": "User not found",
    "status": "BAD_REQUEST"
  }
  ```

### Example
```bash
curl -X POST https://stream-repo-l30u.onrender.com/api/auth/magic-link \
-H "Content-Type: application/json" \
-d '{"email":"okoroaforkelechi123@streamrepo.com"}'
```

---

## 4. Validate Magic Link
### Overview
Validates a magic link to verify a user’s account.

### Endpoint
```
GET /api/auth/validate-magic-link
```

### Request
- **Query Parameters**:
  | Parameter | Type   | Description            | Required |
  |-----------|--------|------------------------|----------|
  | `link`    | String | Magic link token       | Yes      |

- **Example**:
  ```
  GET https://stream-repo-l30u.onrender.com/api/auth/validate-magic-link?link=abc123xyz
  ```

### Response
- **Success**: `201 Created`
  ```json
  {
    "timestamp": "2025-07-31T15:09:00.123+01:00",
    "message": "Your account has been verified successfully",
    "status": "OK"
  }
  ```
- **Error**: `400 Bad Request`
  ```json
  {
    "timestamp": "2025-07-31T15:09:00.123+01:00",
    "message": "Your magic link is expired already",
    "status": "BAD_REQUEST"
  }
  ```

### Example
```bash
curl -X GET "https://stream-repo-l30u.onrender.com/api/auth/validate-magic-link?link=abc123xyz"
```

---

## 5. OAuth2 GitHub Authentication
### Overview
Handles GitHub OAuth2 authentication, returning user details after successful login.

### Endpoint
```
GET /api/auth/oauth2/github
```

### Request
- **Parameters**: Handled via OAuth2 flow (no manual input required).
- **OAuth2 Attributes**: Includes `id`, `email`, `name`, `avatar_url`, `login`.

### Response
- **Success**: Returns `UserDTO` with user details.
  ```json
  {
    "username": "zipDemon",
    "email": "okoroaforkelechi123@streamrepo.com",
    "name": "Kelechi Divine",
    "picture": "https://avatars.githubusercontent.com/u/123456?v=4"
  }
  ```
- **Error**: `500 Internal Server Error`
  ```json
  {
    "error": "OAuth2 login failed"
  }
  ```

### Example
Initiate via GitHub OAuth2 flow (e.g., browser redirect). Manual testing requires an OAuth2 client.

---

## 6. Request Password Reset
### Overview
Sends a password reset link to the user’s email.

### Endpoint
```
POST /api/auth/forgot-password
```

### Request
- **Headers**: `Content-Type: application/json`
- **Body** (`MagicLinkRequest`):
  | Field   | Type   | Description            | Required |
  |---------|--------|------------------------|----------|
  | `email` | String | User’s email address   | Yes      |

- **Example**:
  ```json
  {
    "email": "okoroaforkelechi123@streamrepo.com"
  }
  ```

### Response
- **Success**: `200 OK`
  ```json
  {
    "timestamp": "2025-07-31T15:09:00.123+01:00",
    "message": "Password reset link sent to email",
    "status": "OK"
  }
  ```
- **Errors**:
  - `400 Bad Request`: User not found or unverified.
  - `500 Internal Server Error`: Email service failure.

### Example
```bash
curl -X POST https://stream-repo-l30u.onrender.com/api/auth/forgot-password \
-H "Content-Type: application/json" \
-d '{"email":"okoroaforkelechi123@streamrepo.com"}'
```

---

## 7. Reset Password
### Overview
Resets a user’s password using a reset token.

### Endpoint
```
POST /api/auth/reset-password
```

### Request
- **Headers**: `Content-Type: application/json`
- **Body** (`PasswordResetRequest`):
  | Field         | Type   | Description                     | Required |
  |---------------|--------|---------------------------------|----------|
  | `link`        | String | Password reset token           | Yes      |
  | `newPassword` | String | New password (min 8 chars, mixed) | Yes      |

- **Example**:
  ```json
  {
    "link": "xyz789abc",
    "newPassword": "NewSecurePass123!"
  }
  ```

### Response
- **Success**: `200 OK`
  ```json
  {
    "timestamp": "2025-07-31T15:09:00.123+01:00",
    "message": "Password reset successful",
    "status": "OK"
  }
  ```
- **Errors**:
  - `400 Bad Request`: Invalid/expired token or weak password.
  - `500 Internal Server Error`: Database failure.

### Example
```bash
curl -X POST https://stream-repo-l30u.onrender.com/api/auth/reset-password \
-H "Content-Type: application/json" \
-d '{"link":"xyz789abc","newPassword":"NewSecurePass123!"}'
```

---

## Notes
- **Security**: Passwords are encrypted; magic links and reset tokens are time-limited.
- **Timeouts**: 20-second timeout for database/email operations.
- **Testing**: Use Postman or cURL with proper JSON formatting.
- **OAuth2**: Supports GitHub; other providers may be added.

