Below is the comprehensive README documentation for all the endpoints in the provided `AuthController` class. The README covers the `/register`, `/login`, `/magic-link`, `/validate-magic-link`, and `/oauth2/success` endpoints, following the style and structure of the provided example. It includes detailed request and response structures, validation rules, error handling, and testing instructions.



# StreamRepo API Documentation

This document provides detailed information about the authentication-related endpoints for the StreamRepo API. These endpoints handle user registration, login, magic link requests, magic link validation, and OAuth2 authentication. All endpoints are part of the authentication module and are accessible under the base URL.

## Base URL
```
https://stream-repo-l30u.onrender.com
```

## Endpoints Overview
The following endpoints are available in the `/api/auth` module:
1. **POST /api/auth/register** - Register a new user and send a verification email with a magic link.
2. **POST /api/auth/login** - Authenticate a user and return a JWT token.
3. **POST /api/auth/magic-link** - Request a magic link for passwordless login or verification.
4. **GET /api/auth/validate-magic-link** - Validate a magic link to verify a user’s account.
5. **GET /api/auth/oauth2/success** - Handle successful OAuth2 authentication and redirect with a JWT token.

---

## 1. Register Endpoint
### Overview
The User Registration API is a POST endpoint designed for creating user accounts. It validates input, checks email uniqueness, enforces password strength, and sends a verification email with a magic link.

### Endpoint
```
POST /api/auth/register
```

### Purpose
This endpoint allows clients to register a new user by submitting a JSON payload with user details. On success, the API saves the user to a MongoDB database, encrypts the password, and sends a magic link to the user’s email for verification.

### Request Structure
#### Request Headers
- **Content-Type**: `application/json`

#### Request Body
The request body must include the following fields, as defined in the `UserDTO` class:

| Field      | Type   | Description                                   | Constraints                                      | Required |
|------------|--------|-----------------------------------------------|--------------------------------------------------|----------|
| `username` | String | The user’s chosen username                    | 3–20 characters, cannot be blank                 | Yes      |
| `name`     | String | The user’s full name                          | Max 50 characters, cannot be blank               | Yes      |
| `email`    | String | The user’s email address                      | Must be a valid email format, cannot be blank    | Yes      |
| `bio`      | String | A short description of the user               | Max 200 characters, optional                     | No       |
| `picture`  | String | A Base64 string or URL for the user’s profile picture | Optional                                         | No       |
| `password` | String | The user’s password                           | Min 8 characters, must include uppercase, lowercase, and number | Yes      |

#### Example Request
```json
{
  "username": "zipDemon",
  "name": "Kelechi Divine",
  "email": "okoroaforkelechi123@streamrepo.com",
  "bio": "Javascript is shit",
  "picture": "",
  "password": "SecurePass123!"
}
```

#### Validation Rules
- **Username**: Must be 3–20 characters. Cannot be empty.
- **Name**: Cannot exceed 50 characters. Cannot be empty.
- **Email**: Must follow a valid email format (e.g., `user@domain.com`). Must contain `@` and `.`.
- **Bio**: Optional, but if provided, cannot exceed 200 characters.
- **Picture**: Optional, can be a Base64 string or a URL.
- **Password**: Must be at least 8 characters, including at least one uppercase letter, one lowercase letter, and one number.

### Response Structure
#### Success Response
**Status Code**: `201 Created`

**Response Body**:
```json
{
  "timestamp": "2025-07-29T11:52:00.123+01:00",
  "message": "Your account has been created successfully",
  "status": "CREATED"
}
```

#### Error Responses
##### 1. Empty or Missing Fields
**Status Code**: `400 Bad Request`

**Response Body**:
```json
{
  "timestamp": "2025-07-29T11:52:00.123+01:00",
  "message": "Username cannot be empty",
  "status": "BAD_REQUEST"
}
```

**Possible Messages**:
- "Username cannot be empty"
- "Email cannot be empty"
- "Password cannot be empty"

##### 2. Invalid Email
**Status Code**: `400 Bad Request`

**Response Body**:
```json
{
  "timestamp": "2025-07-29T11:52:00.123+01:00",
  "message": "User email is invalid.",
  "status": "BAD_REQUEST"
}
```

##### 3. Weak or Short Password
**Status Code**: `400 Bad Request`

**Response Body**:
```json
{
  "timestamp": "2025-07-29T11:52:00.123+01:00",
  "message": "Password should be at least 5 characters.",
  "status": "BAD_REQUEST"
}
```

**Alternative Message**:
- "Password is too weak."

**Note**: The service enforces a minimum of 8 characters with a mix of uppercase, lowercase, and numbers, but some error messages reference a 5-character minimum due to legacy checks.

##### 4. Username or Email Already Exists
**Status Code**: `400 Bad Request`

**Response Body**:
```json
{
  "timestamp": "2025-07-29T11:52:00.123+01:00",
  "message": "Email already exists",
  "status": "BAD_REQUEST"
}
```

**Alternative Message**:
- "Username already exists"
- "User is already verified. Please log in"

##### 5. Internal Server Error
**Status Code**: `500 Internal Server Error`

**Response Body**:
```json
{
  "timestamp": "2025-07-29T11:52:00.123+01:00",
  "message": "Failed to save user",
  "status": "INTERNAL_SERVER_ERROR"
}
```

**Possible Causes**:
- Failure to save the user to MongoDB.
- Failure to send the magic link email.

**Note**: The timeout has been increased to 20 seconds to reduce the likelihood of this error.

### Testing the API
Use tools like Postman or cURL to test the endpoint. Example cURL command:

```bash
curl -X POST https://stream-repo-l30u.onrender.com/api/auth/register \
-H "Content-Type: application/json" \
-d '{
   "username": "zipDemon",
   "name": "Kelechi Divine",
   "email": "okoroaforkelechi123@streamrepo.com",
   "bio": "Javascript is shit",
   "picture": "",
   "password": "SecurePass123!"
}'
```

**Expected Response**:
```json
{
  "timestamp": "2025-07-29T11:52:00.123+01:00",
  "message": "Your account has been created successfully",
  "status": "CREATED"
}
```

---

## 2. Login Endpoint
### Overview
The Login API is a POST endpoint that authenticates a user based on their username and password, returning a JWT token upon successful authentication.

### Endpoint
```
POST /api/auth/login
```

### Purpose
This endpoint allows users to log in by submitting their username and password. If credentials are valid, the API generates and returns a JWT token for subsequent authenticated requests.

### Request Structure
#### Request Headers
- **Content-Type**: `application/json`

#### Request Body
The request body must include the following fields, as defined in the `UserDTO` class:

| Field      | Type   | Description                                   | Constraints                                      | Required |
|------------|--------|-----------------------------------------------|--------------------------------------------------|----------|
| `username` | String | The user’s username                           | Cannot be blank                                  | Yes      |
| `password` | String | The user’s password                           | Cannot be blank                                  | Yes      |

#### Example Request
```json
{
  "username": "zipDemon",
  "password": "SecurePass123!"
}
```

#### Validation Rules
- **Username**: Cannot be empty.
- **Password**: Cannot be empty.

### Response Structure
#### Success Response
**Status Code**: `200 OK`

**Response Body**:
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "responseDetails": {
    "timestamp": "2025-07-29T11:52:00.123+01:00",
    "message": "Login successful",
    "status": "OK"
  }
}
```

#### Error Responses
##### 1. Invalid Credentials
**Status Code**: `401 Unauthorized`

**Response Body**:
```json
{
  "timestamp": "2025-07-29T11:52:00.123+01:00",
  "message": "Invalid credentials",
  "status": "UNAUTHORIZED"
}
```

### Testing the API
Example cURL command:

```bash
curl -X POST https://stream-repo-l30u.onrender.com/api/auth/login \
-H "Content-Type: application/json" \
-d '{
   "username": "zipDemon",
   "password": "SecurePass123!"
}'
```

**Expected Response**:
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "responseDetails": {
    "timestamp": "2025-07-29T11:52:00.123+01:00",
    "message": "Login successful",
    "status": "OK"
  }
}
```

---

## 3. Request Magic Link Endpoint
### Overview
The Magic Link Request API is a POST endpoint that generates and sends a magic link to a user’s email for passwordless login or account verification.

### Endpoint
```
POST /api/auth/magic-link
```

### Purpose
This endpoint allows users to request a magic link by submitting their email address. The API generates a secure, time-limited link and sends it to the user’s email.

### Request Structure
#### Request Headers
- **Content-Type**: `application/json`

#### Request Body
The request body must include the following field, as defined in the `MagicLinkRequest` class:

| Field   | Type   | Description                                   | Constraints                                      | Required |
|---------|--------|-----------------------------------------------|--------------------------------------------------|----------|
| `email` | String | The user’s email address                      | Must be a valid email format, cannot be blank    | Yes      |

#### Example Request
```json
{
  "email": "okoroaforkelechi123@streamrepo.com"
}
```

#### Validation Rules
- **Email**: Must be a valid email format and correspond to an existing user.

### Response Structure
#### Success Response
**Status Code**: `200 OK`

**Response Body**:
```json
{
  "timestamp": "2025-07-29T11:52:00.123+01:00",
  "message": "Magic link sent to email",
  "status": "OK"
}
```

#### Error Responses
##### 1. User Not Found
**Status Code**: `400 Bad Request`

**Response Body**:
```json
{
  "timestamp": "2025-07-29T11:52:00.123+01:00",
  "message": "User not found",
  "status": "BAD_REQUEST"
}
```

### Testing the API
Example cURL command:

```bash
curl -X POST https://stream-repo-l30u.onrender.com/api/auth/magic-link \
-H "Content-Type: application/json" \
-d '{
   "email": "okoroaforkelechi123@streamrepo.com"
}'
```

**Expected Response**:
```json
{
  "timestamp": "2025-07-29T11:52:00.123+01:00",
  "message": "Magic link sent to email",
  "status": "OK"
}
```

---

## 4. Validate Magic Link Endpoint
### Overview
The Magic Link Validation API is a GET endpoint that verifies a magic link to confirm a user’s account or facilitate passwordless login.

### Endpoint
```
GET /api/auth/validate-magic-link
```

### Purpose
This endpoint validates a magic link provided as a query parameter. If valid, it marks the user’s account as verified and deletes the magic link from the database.

### Request Structure
#### Query Parameters
| Parameter | Type   | Description                                   | Constraints                                      | Required |
|-----------|--------|-----------------------------------------------|--------------------------------------------------|----------|
| `link`    | String | The magic link token                          | Cannot be blank                                  | Yes      |

#### Example Request
```
GET https://stream-repo-l30u.onrender.com/api/auth/validate-magic-link?link=abc123xyz
```

### Response Structure
#### Success Response
**Status Code**: `201 Created`

**Response Body**:
```json
{
  "timestamp": "2025-07-29T11:52:00.123+01:00",
  "message": "Your account has been verified successfully",
  "status": "OK"
}
```

#### Error Responses
##### 1. Invalid or Expired Magic Link
**Status Code**: `400 Bad Request`

**Response Body**:
```json
{
  "timestamp": "2025-07-29T11:52:00.123+01:00",
  "message": "Your magic link is expired already",
  "status": "BAD_REQUEST"
}
```

### Testing the API
Example cURL command:

```bash
curl -X GET "https://stream-repo-l30u.onrender.com/api/auth/validate-magic-link?link=abc123xyz"
```

**Expected Response**:
```json
{
  "timestamp": "2025-07-29T11:52:00.123+01:00",
  "message": "Your account has been verified successfully",
  "status": "OK"
}
```

---

## 5. OAuth2 Success Endpoint
### Overview
The OAuth2 Success API is a GET endpoint that handles successful OAuth2 authentication, generating a JWT token and redirecting the user to the frontend with the token.

### Endpoint
```
GET /api/auth/oauth2/success
```

### Purpose
This endpoint processes OAuth2 authentication results, creates or updates a user account based on the OAuth2 provider’s data, generates a JWT token, and redirects to the frontend application.

### Request Structure
#### Query Parameters
This endpoint is called automatically by the OAuth2 provider and does not require manual query parameters. It uses the `OAuth2AuthenticationToken` provided by the OAuth2 flow, which includes:
- Provider (e.g., Google, GitHub)
- Provider ID
- Email
- Name
- Picture

### Response Structure
#### Success Response
**Status Code**: `302 Found` (Redirect)

**Redirect URL**:
```
https://stream-repo-frontend.vercel.app/auth/callback?token=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

#### Error Responses
##### 1. OAuth2 Login Failure
**Status Code**: `302 Found` (Redirect)

**Redirect URL**:
```
https://stream-repo-frontend.vercel.app/auth/callback?error=OAuth2 login failed
```

### Testing the API
This endpoint is typically accessed via an OAuth2 provider’s redirect after successful authentication. Manual testing requires initiating an OAuth2 flow (e.g., via Google or GitHub). Use an OAuth2 client library or test via a browser redirect from the provider.

---

## Notes
- **Timeout**: The API has a 20-second timeout for database and email operations to reduce `500 Internal Server Error` occurrences.
- **Security**: Passwords are encrypted using a utility (e.g., `PasswordUtil`). Magic links are secure, randomly generated, and time-limited.
- **OAuth2**: The endpoint supports multiple providers (e.g., Google, GitHub) and automatically generates unique usernames based on email if needed.
- **Testing Tools**: Use Postman, cURL, or a browser for testing. Ensure proper headers and JSON formatting.

