# Authentication API Documentation

This document describes the authentication endpoints for the BJJ Tournament System.

## Overview

The authentication system uses JWT (JSON Web Tokens) for stateless authentication. After successful login or registration, clients receive a JWT token that must be included in the `Authorization` header for subsequent requests.

## Base URL

```
http://localhost:8080/api/auth
```

## Endpoints

### 1. Register New User

Register a new user account.

**Endpoint:** `POST /api/auth/register`

**Request Headers:**
```
Content-Type: application/json
```

**Request Body:**
```json
{
  "username": "johndoe",
  "email": "john.doe@example.com",
  "password": "securePassword123",
  "fullName": "John Doe"
}
```

**Validation Rules:**
- `username`: Required, 3-50 characters
- `email`: Required, valid email format
- `password`: Required, 6-100 characters
- `fullName`: Required, 2-100 characters

**Success Response (201 Created):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "type": "Bearer",
  "id": 1,
  "username": "johndoe",
  "email": "john.doe@example.com",
  "fullName": "John Doe",
  "role": "ROLE_USER"
}
```

**Error Response (400 Bad Request):**
```json
{
  "message": "Username 'johndoe' is already taken"
}
```

### 2. Login

Authenticate an existing user.

**Endpoint:** `POST /api/auth/login`

**Request Headers:**
```
Content-Type: application/json
```

**Request Body:**
```json
{
  "username": "johndoe",
  "password": "securePassword123"
}
```

**Success Response (200 OK):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "type": "Bearer",
  "id": 1,
  "username": "johndoe",
  "email": "john.doe@example.com",
  "fullName": "John Doe",
  "role": "ROLE_USER"
}
```

**Error Response (401 Unauthorized):**
```json
{
  "message": "Invalid username or password"
}
```

### 3. Get Current User

Get information about the currently authenticated user.

**Endpoint:** `GET /api/auth/me`

**Request Headers:**
```
Authorization: Bearer {token}
```

**Success Response (200 OK):**
```json
{
  "token": null,
  "type": "Bearer",
  "id": 1,
  "username": "johndoe",
  "email": "john.doe@example.com",
  "fullName": "John Doe",
  "role": "ROLE_USER"
}
```

**Error Response (401 Unauthorized):**
```json
{
  "message": "Not authenticated"
}
```

### 4. Logout

Logout the current user (client-side operation).

**Endpoint:** `POST /api/auth/logout`

**Request Headers:**
```
Authorization: Bearer {token}
```

**Success Response (200 OK):**
```json
{
  "message": "Logged out successfully"
}
```

**Note:** Since JWT is stateless, logout is primarily handled on the client side by removing the token from storage.

## Using JWT Tokens

After successful login or registration, include the JWT token in all subsequent requests:

```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

## Security Considerations

1. **Token Expiration:** Tokens expire after 24 hours by default
2. **HTTPS:** Always use HTTPS in production
3. **Secret Key:** Change the JWT secret key in production (see application.properties)
4. **Password Storage:** Passwords are encrypted using BCrypt
5. **CORS:** Frontend origins must be whitelisted in SecurityConfig

## Example Usage with React

### Register User

```javascript
const register = async (userData) => {
  const response = await fetch('http://localhost:8080/api/auth/register', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(userData),
  });

  const data = await response.json();

  if (response.ok) {
    // Store token in localStorage or sessionStorage
    localStorage.setItem('token', data.token);
    localStorage.setItem('user', JSON.stringify({
      id: data.id,
      username: data.username,
      email: data.email,
      fullName: data.fullName,
      role: data.role,
    }));
    return data;
  } else {
    throw new Error(data.message);
  }
};
```

### Login User

```javascript
const login = async (credentials) => {
  const response = await fetch('http://localhost:8080/api/auth/login', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(credentials),
  });

  const data = await response.json();

  if (response.ok) {
    localStorage.setItem('token', data.token);
    localStorage.setItem('user', JSON.stringify({
      id: data.id,
      username: data.username,
      email: data.email,
      fullName: data.fullName,
      role: data.role,
    }));
    return data;
  } else {
    throw new Error(data.message);
  }
};
```

### Make Authenticated Request

```javascript
const getProtectedResource = async () => {
  const token = localStorage.getItem('token');

  const response = await fetch('http://localhost:8080/api/protected-endpoint', {
    method: 'GET',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json',
    },
  });

  if (response.ok) {
    return await response.json();
  } else {
    // Handle unauthorized (token expired or invalid)
    if (response.status === 401) {
      // Redirect to login
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      window.location.href = '/login';
    }
    throw new Error('Request failed');
  }
};
```

### Logout User

```javascript
const logout = async () => {
  const token = localStorage.getItem('token');

  await fetch('http://localhost:8080/api/auth/logout', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`,
    },
  });

  // Remove token from storage
  localStorage.removeItem('token');
  localStorage.removeItem('user');

  // Redirect to login
  window.location.href = '/login';
};
```

## Error Codes

| Status Code | Description |
|-------------|-------------|
| 200 | Success |
| 201 | Created (registration successful) |
| 400 | Bad Request (validation error or duplicate username/email) |
| 401 | Unauthorized (invalid credentials or missing/invalid token) |
| 403 | Forbidden (insufficient permissions) |
| 500 | Internal Server Error |

## Protected Endpoints

All endpoints except `/api/auth/**` and `/api/public/**` require authentication. Include the JWT token in the Authorization header:

```
Authorization: Bearer {token}
```

## User Roles

Currently supported roles:
- `ROLE_USER`: Standard user (default for new registrations)
- `ROLE_ADMIN`: Administrator with elevated privileges

Admin-only endpoints are prefixed with `/api/admin/**`.
