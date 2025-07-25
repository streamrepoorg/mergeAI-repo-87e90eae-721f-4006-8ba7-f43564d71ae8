

# User Registration API Documentation

## Introduction
This document provides a comprehensive guide to the User Registration API for the Stream application, hosted at `https://stream-repo-l30u.onrender.com`. The API enables users to create an account by submitting essential details like username, email, and password. Built with Spring Boot and a robust backend architecture, it ensures secure and reliable user registration. This article covers the API’s functionality, request structure, response details, and error handling in clear, simple English.

The goal is to help developers integrate with the API easily while keeping a professional yet approachable tone. Let’s get started!

## API Overview
The User Registration API is a POST endpoint designed for user account creation. It validates input, checks email uniqueness, enforces password strength, and sends a magic link email for verification. The endpoint is part of the authentication module, accessible at `/api/auth/register`.

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
  "username": "john_doe",
  "name": "John Doe",
  "email": "john.doe@example.com",
  "bio": "A passionate coder and tech enthusiast",
  "picture": "https://example.com/profile.jpg",
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

## Backend Logic
The registration process involves several steps, handled by the `AuthServiceImpl` and `AuthController` classes:

1. **Input Validation**: The `UserDTO` is validated using Jakarta Bean Validation annotations (`@NotBlank`, `@Email`, `@Size`). The service layer performs additional checks for email validity and password strength.
2. **Email Uniqueness**: The service checks if the email already exists in the database.
3. **Password Encryption**: The password is encrypted before storage using a secure encryption method.
4. **User Storage**: The user is saved to MongoDB. If this fails, an exception is thrown.
5. **Magic Link Email**: A magic link is sent to the user’s email for verification. If this fails, the registration is rolled back.
6. **Response Generation**: The controller returns a success or error response based on the outcome.

### Key Components
- **UserDTO**: Defines the structure and validation rules for the request payload.
- **AuthServiceImpl**: Contains the core registration logic, including validation, encryption, and email triggering.
- **AuthController**: Handles HTTP requests, delegates to the service layer, and formats responses.
- **ResponseDetails**: A custom class for consistent response formatting (timestamp, message, status).

## Testing the API
To test the API, you can use tools like Postman or cURL. Below is a cURL example for a successful registration:

```bash
curl -X POST https://stream-repo-l30u.onrender.com/api/auth/register \
-H "Content-Type: application/json" \
-d '{
  "username": "john_doe",
  "name": "John Doe",
  "email": "john.doe@example.com",
  "bio": "A passionate coder and tech enthusiast",
  "picture": "https://example.com/profile.jpg",
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

## Error Handling Tips
- **Client-Side Validation**: Validate inputs on the client side to reduce unnecessary API calls.
- **Retry Logic**: For `500 Internal Server Error`, implement retry logic with exponential backoff.
- **User Feedback**: Display error messages (e.g., “Email already exists”) clearly to users.

## Security Considerations
- **Password Strength**: The API enforces strong passwords, but clients should encourage users to use unique passwords.
- **Email Verification**: The magic link ensures only valid emails are registered.
- **Data Encryption**: Passwords are encrypted before storage, and HTTPS is used to protect data in transit.
- **Rate Limiting**: Consider adding rate limiting to prevent brute-force attacks.

## Conclusion
The User Registration API at `https://stream-repo-l30u.onrender.com/api/auth/register` provides a secure and straightforward way to create user accounts. With clear validation rules, detailed error handling, and a magic link verification, it ensures a robust user onboarding experience. Developers can easily integrate this API into their applications using the provided request and response examples. For further assistance or feedback, feel free to reach out to the Stream support team!

