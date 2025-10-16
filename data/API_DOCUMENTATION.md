# EarthMAX API Documentation

## Overview
This document provides comprehensive documentation for the EarthMAX RESTful API endpoints, including user management and event management operations.

## Base Configuration
- **Base URL**: Configured in `DataModule.kt` (default: placeholder URL)
- **Authentication**: Bearer token authentication via `AuthInterceptor`
- **Rate Limiting**: 100 requests per minute per endpoint
- **Content Type**: `application/json`

## Security Features
- **Authentication Interceptor**: Adds Bearer token to all requests
- **Error Interceptor**: Standardizes error responses
- **Rate Limit Interceptor**: Client-side rate limiting protection
- **Request Validation**: Comprehensive validation for all request data

## User Management API

### 1. Get All Users
- **Method**: `GET`
- **Endpoint**: `/users`
- **Query Parameters**:
  - `page` (optional): Page number for pagination (default: 0)
  - `limit` (optional): Number of items per page (default: 20)
- **Response**: `List<UserResponse>`
- **Validation**: Pagination parameters validated

### 2. Get User by ID
- **Method**: `GET`
- **Endpoint**: `/users/{id}`
- **Path Parameters**:
  - `id`: User ID (String)
- **Response**: `UserResponse`

### 3. Create User
- **Method**: `POST`
- **Endpoint**: `/users`
- **Request Body**: `CreateUserRequest`
```json
{
  "name": "string (required, 2-100 chars)",
  "email": "string (required, valid email format)",
  "phone": "string (optional, 10-15 digits)",
  "bio": "string (optional, max 500 chars)",
  "location": "string (optional)",
  "latitude": "double (optional, -90 to 90)",
  "longitude": "double (optional, -180 to 180)",
  "profile_image_url": "string (optional)"
}
```
- **Response**: `UserResponse`
- **Validation**: Email format, phone format, coordinate ranges, text lengths

### 4. Update User
- **Method**: `PUT`
- **Endpoint**: `/users/{id}`
- **Path Parameters**:
  - `id`: User ID (String)
- **Request Body**: `UpdateUserRequest`
```json
{
  "name": "string (optional, 2-100 chars)",
  "email": "string (optional, valid email format)",
  "phone": "string (optional, 10-15 digits)",
  "bio": "string (optional, max 500 chars)",
  "location": "string (optional)",
  "latitude": "double (optional, -90 to 90)",
  "longitude": "double (optional, -180 to 180)",
  "profile_image_url": "string (optional)"
}
```
- **Response**: `UserResponse`
- **Validation**: Same as create user for provided fields

### 5. Partial Update User
- **Method**: `PATCH`
- **Endpoint**: `/users/{id}`
- **Path Parameters**:
  - `id`: User ID (String)
- **Request Body**: `UpdateUserRequest` (partial)
- **Response**: `UserResponse`

### 6. Delete User
- **Method**: `DELETE`
- **Endpoint**: `/users/{id}`
- **Path Parameters**:
  - `id`: User ID (String)
- **Response**: `Unit` (204 No Content)

### 7. Get User Profile
- **Method**: `GET`
- **Endpoint**: `/users/{id}/profile`
- **Path Parameters**:
  - `id`: User ID (String)
- **Response**: `UserResponse`

### 8. Update User Profile
- **Method**: `PUT`
- **Endpoint**: `/users/{id}/profile`
- **Path Parameters**:
  - `id`: User ID (String)
- **Request Body**: `UpdateUserRequest`
- **Response**: `UserResponse`

### 9. Get Users by Location
- **Method**: `GET`
- **Endpoint**: `/users/location`
- **Query Parameters**:
  - `latitude`: Latitude coordinate (Double)
  - `longitude`: Longitude coordinate (Double)
  - `radius`: Search radius in kilometers (Double)
- **Response**: `List<UserResponse>`

### 10. Search Users
- **Method**: `GET`
- **Endpoint**: `/users/search`
- **Query Parameters**:
  - `query`: Search query string (required, 1-100 chars)
  - `page` (optional): Page number (default: 0)
  - `limit` (optional): Items per page (default: 20)
- **Response**: `List<UserResponse>`
- **Validation**: Query length, pagination parameters

## Event Management API

### 1. Get All Events
- **Method**: `GET`
- **Endpoint**: `/events`
- **Query Parameters**:
  - `page` (optional): Page number for pagination (default: 0)
  - `limit` (optional): Number of items per page (default: 20)
- **Response**: `List<EventResponse>`

### 2. Get Event by ID
- **Method**: `GET`
- **Endpoint**: `/events/{id}`
- **Path Parameters**:
  - `id`: Event ID (String)
- **Response**: `EventResponse`

### 3. Create Event
- **Method**: `POST`
- **Endpoint**: `/events`
- **Request Body**: `CreateEventRequest`
```json
{
  "title": "string (required, 3-100 chars)",
  "description": "string (required, 10-1000 chars)",
  "category": "string (required, 2-50 chars)",
  "date": "string (required, ISO date format)",
  "time": "string (required)",
  "location": "string (required, 3-200 chars)",
  "latitude": "double (optional, -90 to 90)",
  "longitude": "double (optional, -180 to 180)",
  "image_url": "string (optional)",
  "max_participants": "int (optional, 1-10000)",
  "tags": "array of strings (optional)"
}
```
- **Response**: `EventResponse`
- **Validation**: Text lengths, coordinate ranges, participant limits

### 4. Update Event
- **Method**: `PUT`
- **Endpoint**: `/events/{id}`
- **Path Parameters**:
  - `id`: Event ID (String)
- **Request Body**: `UpdateEventRequest`
```json
{
  "title": "string (optional, 3-100 chars)",
  "description": "string (optional, 10-1000 chars)",
  "category": "string (optional, 2-50 chars)",
  "date": "string (optional, ISO date format)",
  "time": "string (optional)",
  "location": "string (optional, 3-200 chars)",
  "latitude": "double (optional, -90 to 90)",
  "longitude": "double (optional, -180 to 180)",
  "image_url": "string (optional)",
  "max_participants": "int (optional, 1-10000)",
  "is_featured": "boolean (optional)",
  "tags": "array of strings (optional)"
}
```
- **Response**: `EventResponse`

### 5. Delete Event
- **Method**: `DELETE`
- **Endpoint**: `/events/{id}`
- **Path Parameters**:
  - `id`: Event ID (String)
- **Response**: `Unit` (204 No Content)

### 6. Search Events
- **Method**: `GET`
- **Endpoint**: `/events/search`
- **Query Parameters**:
  - `query`: Search query string (required, 1-100 chars)
  - `page` (optional): Page number (default: 0)
  - `limit` (optional): Items per page (default: 20)
- **Response**: `List<EventResponse>`

### 7. Filter Events
- **Method**: `GET`
- **Endpoint**: `/events/filter`
- **Query Parameters**:
  - `category` (optional): Event category
  - `date` (optional): Event date
  - `location` (optional): Event location
  - `page` (optional): Page number (default: 0)
  - `limit` (optional): Items per page (default: 20)
- **Response**: `List<EventResponse>`

### 8. Join Event
- **Method**: `POST`
- **Endpoint**: `/events/{id}/join`
- **Path Parameters**:
  - `id`: Event ID (String)
- **Response**: `Unit` (200 OK)

### 9. Leave Event
- **Method**: `DELETE`
- **Endpoint**: `/events/{id}/leave`
- **Path Parameters**:
  - `id`: Event ID (String)
- **Response**: `Unit` (200 OK)

### 10. Get Event Participants
- **Method**: `GET`
- **Endpoint**: `/events/{id}/participants`
- **Path Parameters**:
  - `id`: Event ID (String)
- **Response**: `List<UserResponse>`

### 11. Get User Events
- **Method**: `GET`
- **Endpoint**: `/users/{userId}/events`
- **Path Parameters**:
  - `userId`: User ID (String)
- **Response**: `List<EventResponse>`

## Data Transfer Objects

### UserResponse
```json
{
  "id": "string",
  "name": "string",
  "email": "string",
  "phone": "string",
  "bio": "string",
  "location": "string",
  "latitude": "double",
  "longitude": "double",
  "profile_image_url": "string",
  "created_at": "string (ISO datetime)",
  "updated_at": "string (ISO datetime)"
}
```

### EventResponse
```json
{
  "id": "string",
  "title": "string",
  "description": "string",
  "category": "string",
  "date": "string",
  "time": "string",
  "location": "string",
  "latitude": "double",
  "longitude": "double",
  "image_url": "string",
  "organizer_id": "string",
  "organizer_name": "string",
  "max_participants": "int",
  "current_participants": "int",
  "is_featured": "boolean",
  "tags": "array of strings",
  "created_at": "string (ISO datetime)",
  "updated_at": "string (ISO datetime)"
}
```

## Error Handling

### Standard Error Response
```json
{
  "error": {
    "type": "string",
    "message": "string",
    "code": "int"
  }
}
```

### Error Types
- **VALIDATION_ERROR**: Request validation failed
- **AUTHENTICATION_ERROR**: Authentication required or invalid
- **AUTHORIZATION_ERROR**: Insufficient permissions
- **NOT_FOUND_ERROR**: Resource not found
- **RATE_LIMIT_ERROR**: Rate limit exceeded
- **SERVER_ERROR**: Internal server error
- **NETWORK_ERROR**: Network connectivity issues

### HTTP Status Codes
- **200**: Success
- **201**: Created
- **204**: No Content
- **400**: Bad Request (validation errors)
- **401**: Unauthorized
- **403**: Forbidden
- **404**: Not Found
- **429**: Too Many Requests (rate limit)
- **500**: Internal Server Error

## Validation Rules

### General Rules
- All required fields must be provided
- String fields have minimum and maximum length constraints
- Numeric fields have range constraints
- Email fields must match valid email pattern
- Phone fields must match valid phone pattern (10-15 digits)

### Coordinate Validation
- Latitude: -90.0 to 90.0
- Longitude: -180.0 to 180.0

### Pagination Validation
- Page: >= 0
- Limit: 1 to 100

### Search Query Validation
- Length: 1 to 100 characters
- Cannot be empty or only whitespace

## Rate Limiting
- **Limit**: 100 requests per minute per endpoint
- **Implementation**: Client-side rate limiting via `RateLimitInterceptor`
- **Exception**: `RateLimitExceededException` thrown when limit exceeded

## Authentication
- **Type**: Bearer token authentication
- **Header**: `Authorization: Bearer <token>`
- **Implementation**: `AuthInterceptor` adds token to all requests
- **Token Source**: Configurable via dependency injection

## Repository Implementation
- **User Operations**: `UserApiRepository`
- **Event Operations**: `EventApiRepository`
- **Return Type**: `Flow<Result<T>>` for reactive programming
- **Error Handling**: Comprehensive try-catch with proper error mapping
- **Validation**: Pre-request validation using `ApiValidator`

## Dependencies
- **Retrofit**: HTTP client for API communication
- **Gson**: JSON serialization/deserialization
- **OkHttp**: HTTP client with interceptor support
- **Hilt**: Dependency injection
- **Kotlinx Coroutines**: Asynchronous programming
- **Flow**: Reactive streams for data handling