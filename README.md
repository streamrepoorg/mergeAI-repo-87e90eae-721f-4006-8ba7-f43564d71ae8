# StreamRepo API documentation

## 1: Register endpoint Overview
The User Registration API is a POST endpoint designed for creating user accounts. It validates input, checks email uniqueness, enforces password strength, and sends a verification email with a magic link. The endpoint is part of the authentication module, accessible at `/api/auth/register`.

### Base URL
```
https://stream-repo-l30u.onrender.com
```

### Endpoint
```
POST /api/auth/register
```

### Purpose
This endpoint allows clients to register a new user by submitting a JSON payload with user details. On success, the API saves the user to a MongoDB database, encrypts the password, and sends a magic link to the user’s email for verification.

## Request Structure
The API expects a JSON payload adhering to the `UserDTO` structure. Below is the detailed request format with field descriptions and validation rules.

### Request Headers
- **Content-Type**: `application/json`

### Request Body
The request body must include the following fields, as defined in the `UserDTO` class:

| Field      | Type   | Description                                   | Constraints                                      | Required |
|------------|--------|-----------------------------------------------|--------------------------------------------------|----------|
| `username` | String | The user’s chosen username                    | 3–20 characters, cannot be blank                 | Yes      |
| `name`     | String | The user’s full name                          | Max 50 characters, cannot be blank               | Yes      |
| `email`    | String | The user’s email address                      | Must be a valid email format, cannot be blank    | Yes      |
| `bio`      | String | A short description of the user               | Max 200 characters, optional                     | No       |
| `picture`  | String | A Base64 string or URL for the user’s profile picture | Optional                                         | No       |
| `password` | String | The user’s password                           | Min 8 characters, cannot be blank                | Yes      |

### Example Request
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

### Validation Rules
- **Username**: Must be 3–20 characters. Cannot be empty.
- **Name**: Cannot exceed 50 characters. Cannot be empty.
- **Email**: Must follow a valid email format (e.g., `user@domain.com`). Cannot be empty.
- **Bio**: Optional, but if provided, cannot exceed 200 characters.
- **Picture**: Optional, can be a Base64 string or a URL.
- **Password**: Must be at least 8 characters. Additional strength checks (e.g., mix of characters) are enforced by the service logic.

## Response Structure
The API returns a JSON response with details about the registration outcome, including a timestamp, message, and HTTP status code.

### Success Response
**Status Code**: `201 Created`

**Response Body**:
```json
{
  "timestamp": "2025-07-25T11:51:00.123+01:00",
  "message": "Your account has been created successfully",
  "status": "CREATED"
}
```

### Error Responses
The API handles errors gracefully. Below are common error scenarios and their responses.

#### 1. Empty or Missing Fields
**Status Code**: `400 Bad Request`

**Response Body**:
```json
{
  "timestamp": "2025-07-25T11:51:00.123+01:00",
  "message": "Username cannot be empty",
  "status": "BAD_REQUEST"
}
```

**Possible Messages**:
- "Username cannot be empty"
- "Email cannot be empty"
- "Password cannot be empty"
- "Username is required"
- "Name is required"
- "Email is required"
- "Password is required"

#### 2. Invalid Email
**Status Code**: `400 Bad Request`

**Response Body**:
```json
{
  "timestamp": "2025-07-25T11:51:00.123+01:00",
  "message": "User email is invalid.",
  "status": "BAD_REQUEST"
}
```

#### 3. Weak or Short Password
**Status Code**: `400 Bad Request`

**Response Body**:
```json
{
  "timestamp": "2025-07-25T11:51:00.123+01:00",
  "message": "Password should be at least 5 characters.",
  "status": "BAD_REQUEST"
}
```

**Alternative Message**:
- "Password is too weak."

**Note**: The service logic enforces a minimum of 5 characters in some checks, while the DTO requires at least 8 characters. Ensure passwords meet both requirements and include a strong mix of characters.

#### 4. Email Already Exists
**Status Code**: `400 Bad Request`

**Response Body**:
```json
{
  "timestamp": "2025-07-25T11:51:00.123+01:00",
  "message": "Email already exists",
  "status": "BAD_REQUEST"
}
```

#### 5. Internal Server Error
**Status Code**: `500 Internal Server Error`

**Response Body**:
```json
{
  "timestamp": "2025-07-25T11:51:00.123+01:00",
  "message": "Failed to save user",
  "status": "INTERNAL_SERVER_ERROR"
}
```

**Possible Causes**:
- Failure to save the user to MongoDB.
- Failure to send the magic link email.
  
**Note**: Most times, this error is caused by a timeout. I increased the timeout from 5000 (5 seconds) to 20000 (20 seconds), so you might not likely get a 500 internal error.

## Testing the API
To test the API, you can use tools like Postman or cURL. Below is a cURL example for a successful registration:

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
  "timestamp": "2025-07-25T11:51:00.123+01:00",
  "message": "Your account has been created successfully",
  "status": "CREATED"
}
```
