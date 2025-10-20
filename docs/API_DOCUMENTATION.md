# EarthMAX API Documentation

## Overview

EarthMAX uses Supabase as the backend service, providing a PostgreSQL database with real-time subscriptions, authentication, and RESTful API endpoints. The application also implements GraphQL for efficient data fetching.

## Authentication

### Supabase Authentication
The app uses Supabase Auth for user management with email/password authentication.

#### Login
```kotlin
suspend fun signIn(email: String, password: String): AuthResult {
    return supabaseClient.auth.signInWith(Email) {
        this.email = email
        this.password = password
    }
}
```

#### Registration
```kotlin
suspend fun signUp(email: String, password: String): AuthResult {
    return supabaseClient.auth.signUpWith(Email) {
        this.email = email
        this.password = password
    }
}
```

#### Session Management
```kotlin
// Get current session
val session = supabaseClient.auth.currentSessionOrNull()

// Listen to auth state changes
supabaseClient.auth.sessionStatus.collect { status ->
    when (status) {
        is SessionStatus.Authenticated -> handleAuthenticated(status.session)
        is SessionStatus.NotAuthenticated -> handleNotAuthenticated()
        is SessionStatus.LoadingFromStorage -> showLoading()
        is SessionStatus.NetworkError -> handleNetworkError()
    }
}
```

## Database Schema

### Users Table
```sql
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email TEXT UNIQUE NOT NULL,
    display_name TEXT,
    profile_image_url TEXT,
    bio TEXT,
    location TEXT,
    joined_events INTEGER DEFAULT 0,
    organized_events INTEGER DEFAULT 0,
    eco_points INTEGER DEFAULT 0,
    badges JSONB DEFAULT '[]'::jsonb,
    preferences JSONB DEFAULT '{}'::jsonb,
    environmental_impact JSONB DEFAULT '{}'::jsonb,
    profile_customization JSONB DEFAULT '{}'::jsonb,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);
```

### Events Table
```sql
CREATE TABLE events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title TEXT NOT NULL,
    description TEXT,
    category TEXT NOT NULL,
    date_time TIMESTAMP WITH TIME ZONE NOT NULL,
    location TEXT NOT NULL,
    coordinates POINT,
    max_participants INTEGER,
    current_participants INTEGER DEFAULT 0,
    organizer_id UUID REFERENCES users(id),
    requirements TEXT[],
    eco_points_reward INTEGER DEFAULT 0,
    images TEXT[],
    tags TEXT[],
    status TEXT DEFAULT 'active',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);
```

### Event Participants Table
```sql
CREATE TABLE event_participants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id UUID REFERENCES events(id) ON DELETE CASCADE,
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    joined_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    status TEXT DEFAULT 'registered',
    UNIQUE(event_id, user_id)
);
```

## REST API Endpoints

### Base URL
```
https://your-project.supabase.co/rest/v1/
```

### Headers
```http
Authorization: Bearer <jwt_token>
apikey: <supabase_anon_key>
Content-Type: application/json
```

### Users Endpoints

#### Get Current User Profile
```http
GET /users?id=eq.<user_id>&select=*
```

Response:
```json
{
    "id": "uuid",
    "email": "user@example.com",
    "display_name": "John Doe",
    "profile_image_url": "https://...",
    "bio": "Environmental enthusiast",
    "location": "San Francisco, CA",
    "joined_events": 5,
    "organized_events": 2,
    "eco_points": 150,
    "badges": ["tree_planter", "ocean_cleaner"],
    "preferences": {
        "notifications": true,
        "privacy": "public"
    },
    "environmental_impact": {
        "carbon_saved": 25.5,
        "trees_planted": 3,
        "waste_collected": 10.2
    },
    "created_at": "2024-01-01T00:00:00Z",
    "updated_at": "2024-01-15T12:30:00Z"
}
```

#### Update User Profile
```http
PATCH /users?id=eq.<user_id>
Content-Type: application/json

{
    "display_name": "Updated Name",
    "bio": "Updated bio",
    "location": "New Location"
}
```

#### Search Users
```http
GET /users?or=(display_name.ilike.*search*,email.ilike.*search*)&select=id,display_name,profile_image_url,eco_points
```

### Events Endpoints

#### Get All Events
```http
GET /events?select=*,organizer:users(id,display_name,profile_image_url)&order=date_time.asc
```

#### Get Event by ID
```http
GET /events?id=eq.<event_id>&select=*,organizer:users(id,display_name,profile_image_url),participants:event_participants(user:users(id,display_name,profile_image_url))
```

#### Create Event
```http
POST /events
Content-Type: application/json

{
    "title": "Beach Cleanup",
    "description": "Join us for a community beach cleanup event",
    "category": "cleanup",
    "date_time": "2024-02-15T10:00:00Z",
    "location": "Santa Monica Beach",
    "coordinates": {"x": -118.4912, "y": 34.0195},
    "max_participants": 50,
    "requirements": ["Bring gloves", "Wear comfortable shoes"],
    "eco_points_reward": 25,
    "tags": ["beach", "cleanup", "community"]
}
```

#### Update Event
```http
PATCH /events?id=eq.<event_id>
Content-Type: application/json

{
    "title": "Updated Event Title",
    "description": "Updated description",
    "max_participants": 75
}
```

#### Join Event
```http
POST /event_participants
Content-Type: application/json

{
    "event_id": "<event_id>",
    "user_id": "<user_id>"
}
```

#### Leave Event
```http
DELETE /event_participants?event_id=eq.<event_id>&user_id=eq.<user_id>
```

### Filtering and Sorting

#### Filter Events by Category
```http
GET /events?category=eq.cleanup&select=*
```

#### Filter Events by Date Range
```http
GET /events?date_time=gte.2024-02-01T00:00:00Z&date_time=lte.2024-02-29T23:59:59Z&select=*
```

#### Filter Events by Location (Proximity)
```http
GET /events?coordinates=near.<longitude>,<latitude>,<radius_km>&select=*
```

#### Sort Events
```http
GET /events?order=date_time.asc,created_at.desc&select=*
```

## GraphQL API

### Endpoint
```
https://your-project.supabase.co/graphql/v1
```

### Queries

#### Get Events with Participants
```graphql
query GetEvents($limit: Int, $offset: Int) {
    eventsCollection(first: $limit, offset: $offset) {
        edges {
            node {
                id
                title
                description
                category
                dateTime
                location
                maxParticipants
                currentParticipants
                ecoPointsReward
                organizer {
                    id
                    displayName
                    profileImageUrl
                }
                participants {
                    user {
                        id
                        displayName
                        profileImageUrl
                    }
                    joinedAt
                }
            }
        }
        pageInfo {
            hasNextPage
            hasPreviousPage
            startCursor
            endCursor
        }
    }
}
```

#### Get User Profile with Events
```graphql
query GetUserProfile($userId: UUID!) {
    usersCollection(filter: { id: { eq: $userId } }) {
        edges {
            node {
                id
                email
                displayName
                profileImageUrl
                bio
                location
                ecoPoints
                badges
                organizedEvents: eventsCollection(filter: { organizerId: { eq: $userId } }) {
                    edges {
                        node {
                            id
                            title
                            dateTime
                            category
                        }
                    }
                }
                joinedEvents: eventParticipantsCollection(filter: { userId: { eq: $userId } }) {
                    edges {
                        node {
                            event {
                                id
                                title
                                dateTime
                                category
                            }
                            joinedAt
                        }
                    }
                }
            }
        }
    }
}
```

### Mutations

#### Create Event
```graphql
mutation CreateEvent($input: EventsInsertInput!) {
    insertIntoEventsCollection(objects: [$input]) {
        records {
            id
            title
            description
            category
            dateTime
            location
            maxParticipants
            ecoPointsReward
            createdAt
        }
    }
}
```

#### Update User Profile
```graphql
mutation UpdateUserProfile($userId: UUID!, $input: UsersUpdateInput!) {
    updateUsersCollection(filter: { id: { eq: $userId } }, set: $input) {
        records {
            id
            displayName
            bio
            location
            updatedAt
        }
    }
}
```

## Real-time Subscriptions

### Event Updates
```kotlin
// Subscribe to event changes
supabaseClient.from("events")
    .select()
    .subscribe { payload ->
        when (payload) {
            is Subscriptions.Status -> handleConnectionStatus(payload)
            is Subscriptions.PostgresAction -> {
                when (payload.eventType) {
                    "INSERT" -> handleNewEvent(payload.record)
                    "UPDATE" -> handleEventUpdate(payload.record)
                    "DELETE" -> handleEventDeletion(payload.oldRecord)
                }
            }
        }
    }
```

### User Presence
```kotlin
// Track user presence in events
supabaseClient.from("event_participants")
    .select()
    .eq("event_id", eventId)
    .subscribe { payload ->
        when (payload.eventType) {
            "INSERT" -> handleUserJoined(payload.record)
            "DELETE" -> handleUserLeft(payload.oldRecord)
        }
    }
```

## Error Handling

### HTTP Status Codes
- `200` - Success
- `201` - Created
- `400` - Bad Request
- `401` - Unauthorized
- `403` - Forbidden
- `404` - Not Found
- `409` - Conflict
- `422` - Unprocessable Entity
- `500` - Internal Server Error

### Error Response Format
```json
{
    "code": "23505",
    "details": "Key (email)=(user@example.com) already exists.",
    "hint": null,
    "message": "duplicate key value violates unique constraint \"users_email_key\""
}
```

### Common Error Scenarios

#### Authentication Errors
```kotlin
try {
    val result = supabaseClient.auth.signInWith(Email) { /* ... */ }
} catch (e: RestException) {
    when (e.error) {
        "invalid_credentials" -> showInvalidCredentialsError()
        "email_not_confirmed" -> showEmailNotConfirmedError()
        "too_many_requests" -> showRateLimitError()
    }
}
```

#### Database Constraint Violations
```kotlin
try {
    supabaseClient.from("events").insert(eventData)
} catch (e: RestException) {
    when {
        e.message?.contains("foreign key constraint") == true -> 
            showInvalidReferenceError()
        e.message?.contains("unique constraint") == true -> 
            showDuplicateError()
        e.message?.contains("check constraint") == true -> 
            showValidationError()
    }
}
```

## Rate Limiting

### Limits
- Authentication: 30 requests per hour per IP
- Database operations: 100 requests per minute per user
- Real-time connections: 200 concurrent connections per project

### Handling Rate Limits
```kotlin
suspend fun <T> withRetry(
    maxRetries: Int = 3,
    initialDelay: Long = 1000,
    block: suspend () -> T
): T {
    repeat(maxRetries) { attempt ->
        try {
            return block()
        } catch (e: RestException) {
            if (e.status == 429 && attempt < maxRetries - 1) {
                delay(initialDelay * (2.0.pow(attempt).toLong()))
            } else {
                throw e
            }
        }
    }
    throw IllegalStateException("Should not reach here")
}
```

## Security Considerations

### Row Level Security (RLS)
```sql
-- Users can only update their own profile
CREATE POLICY "Users can update own profile" ON users
    FOR UPDATE USING (auth.uid() = id);

-- Users can view public events
CREATE POLICY "Anyone can view active events" ON events
    FOR SELECT USING (status = 'active');

-- Only organizers can update their events
CREATE POLICY "Organizers can update own events" ON events
    FOR UPDATE USING (auth.uid() = organizer_id);
```

### Data Validation
```kotlin
// Client-side validation
fun validateEventData(event: CreateEventRequest): ValidationResult {
    return when {
        event.title.isBlank() -> ValidationResult.Error("Title is required")
        event.dateTime.isBefore(LocalDateTime.now()) -> 
            ValidationResult.Error("Event date must be in the future")
        event.maxParticipants < 1 -> 
            ValidationResult.Error("Max participants must be at least 1")
        else -> ValidationResult.Success
    }
}
```

## Performance Optimization

### Caching Strategy
```kotlin
// Cache frequently accessed data
class EventRepository {
    private val cache = LruCache<String, List<Event>>(maxSize = 100)
    
    suspend fun getEvents(): List<Event> {
        val cacheKey = "events_${System.currentTimeMillis() / 300000}" // 5min cache
        
        return cache.get(cacheKey) ?: run {
            val events = fetchEventsFromApi()
            cache.put(cacheKey, events)
            events
        }
    }
}
```

### Pagination
```kotlin
// Implement cursor-based pagination
suspend fun getEventsPaginated(
    cursor: String? = null,
    limit: Int = 20
): PaginatedResponse<Event> {
    val query = supabaseClient.from("events")
        .select()
        .order("created_at", ascending = false)
        .limit(limit)
    
    cursor?.let { query.gt("created_at", it) }
    
    val response = query.decodeList<EventDto>()
    val events = response.map { it.toEvent() }
    
    return PaginatedResponse(
        data = events,
        nextCursor = events.lastOrNull()?.createdAt,
        hasMore = events.size == limit
    )
}
```

## Testing

### API Testing
```kotlin
@Test
fun `should create event successfully`() = runTest {
    // Given
    val eventRequest = CreateEventRequest(
        title = "Test Event",
        description = "Test Description",
        category = "cleanup",
        dateTime = LocalDateTime.now().plusDays(1),
        location = "Test Location",
        maxParticipants = 50
    )
    
    // When
    val result = eventRepository.createEvent(eventRequest)
    
    // Then
    assertTrue(result.isSuccess)
    val event = result.getOrNull()
    assertNotNull(event)
    assertEquals(eventRequest.title, event?.title)
}
```

### Mock Server Setup
```kotlin
// Use MockWebServer for testing
class EventRepositoryTest {
    private lateinit var mockWebServer: MockWebServer
    private lateinit var repository: EventRepository
    
    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        
        val supabaseClient = createSupabaseClient {
            url = mockWebServer.url("/").toString()
            // ... other config
        }
        
        repository = EventRepositoryImpl(supabaseClient)
    }
    
    @Test
    fun `should handle network error gracefully`() = runTest {
        // Given
        mockWebServer.enqueue(MockResponse().setResponseCode(500))
        
        // When
        val result = repository.getEvents()
        
        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is NetworkException)
    }
}
```

---

*This API documentation should be kept in sync with the actual implementation and updated when new endpoints or features are added.*