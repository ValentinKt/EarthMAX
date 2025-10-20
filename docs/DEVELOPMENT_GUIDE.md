# EarthMAX Development Guide

## Getting Started

### Prerequisites
- Android Studio Arctic Fox or later
- JDK 11 or higher
- Android SDK API 24+ (Android 7.0)
- Git for version control

### Project Setup
1. Clone the repository:
   ```bash
   git clone https://github.com/your-org/earthmax-android.git
   cd earthmax-android
   ```

2. Open in Android Studio and sync Gradle files

3. Configure environment variables:
   ```bash
   # Create local.properties file
   echo "SUPABASE_URL=your_supabase_url" >> local.properties
   echo "SUPABASE_ANON_KEY=your_anon_key" >> local.properties
   ```

4. Build and run:
   ```bash
   ./gradlew assembleDebug
   ```

## Development Workflow

### Branch Strategy
- `main`: Production-ready code
- `develop`: Integration branch for features
- `feature/*`: Individual feature development
- `hotfix/*`: Critical bug fixes

### Commit Convention
Follow conventional commits format:
```
feat: Add user profile caching mechanism
fix: Resolve network timeout issues
refactor: Improve repository error handling
docs: Update architecture documentation
test: Add unit tests for user repository
chore: Update dependencies to latest versions
```

### Code Style
- Follow Kotlin coding conventions
- Use ktlint for code formatting
- Maximum line length: 120 characters
- Use meaningful variable and function names

## Architecture Guidelines

### Module Dependencies
```
app → presentation → domain ← data
       ↓              ↑
     core ←-----------┘
```

**Dependency Rules:**
- `presentation` can depend on `domain` and `core`
- `data` can depend on `domain` and `core`
- `domain` can only depend on `core`
- `core` has no dependencies on other modules

### Creating New Features

#### 1. Domain Layer First
```kotlin
// 1. Define domain model
data class Event(
    val id: String,
    val title: String,
    val description: String,
    // ... other properties
)

// 2. Create repository interface
interface EventRepository {
    suspend fun getEvents(): Flow<List<Event>>
    suspend fun getEventById(id: String): Event?
    suspend fun createEvent(event: Event): Result<Event>
}

// 3. Implement use case
class GetEventsUseCase @Inject constructor(
    private val repository: EventRepository,
    @IoDispatcher private val dispatcher: CoroutineDispatcher
) {
    operator fun invoke(): Flow<List<Event>> = 
        repository.getEvents().flowOn(dispatcher)
}
```

#### 2. Data Layer Implementation
```kotlin
// 1. Create local entity
@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    // ... other properties
)

// 2. Create DAO
@Dao
interface EventDao {
    @Query("SELECT * FROM events")
    fun getAllEvents(): Flow<List<EventEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: EventEntity)
}

// 3. Implement repository
@Singleton
class EventRepositoryImpl @Inject constructor(
    private val localDataSource: EventDao,
    private val remoteDataSource: SupabaseEventRepository,
    private val cacheManager: CacheManager
) : EventRepository {
    // Implementation with caching logic
}
```

#### 3. Presentation Layer
```kotlin
// 1. Create ViewModel
@HiltViewModel
class EventsViewModel @Inject constructor(
    private val getEventsUseCase: GetEventsUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(EventsUiState())
    val uiState: StateFlow<EventsUiState> = _uiState.asStateFlow()
    
    init {
        loadEvents()
    }
    
    private fun loadEvents() {
        viewModelScope.launch {
            getEventsUseCase()
                .catch { error -> 
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message
                    )
                }
                .collect { events ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        events = events,
                        error = null
                    )
                }
        }
    }
}

// 2. Create UI State
data class EventsUiState(
    val isLoading: Boolean = true,
    val events: List<Event> = emptyList(),
    val error: String? = null
)

// 3. Create Composable
@Composable
fun EventsScreen(
    viewModel: EventsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    when {
        uiState.isLoading -> LoadingIndicator()
        uiState.error != null -> ErrorMessage(uiState.error)
        else -> EventsList(uiState.events)
    }
}
```

## Testing Guidelines

### Unit Testing Structure
```
src/test/java/
├── domain/
│   ├── usecase/
│   └── repository/
├── data/
│   ├── repository/
│   └── mapper/
└── presentation/
    └── viewmodel/
```

### Test Naming Convention
```kotlin
class EventRepositoryImplTest {
    
    @Test
    fun `getEvents should return cached data when available`() {
        // Given
        val cachedEvents = listOf(mockEvent1, mockEvent2)
        every { cacheManager.get<List<Event>>(any()) } returns cachedEvents
        
        // When
        val result = repository.getEvents().first()
        
        // Then
        assertEquals(cachedEvents, result)
    }
}
```

### Testing Best Practices
- Use descriptive test names with backticks
- Follow Given-When-Then structure
- Mock external dependencies
- Test both success and error scenarios
- Aim for 80%+ code coverage

## Performance Guidelines

### Memory Management
```kotlin
// ✅ Good: Use lazy initialization
class ExpensiveComponent {
    private val heavyObject by lazy { createHeavyObject() }
}

// ✅ Good: Clear references in onCleared()
class MyViewModel : ViewModel() {
    private var disposable: Disposable? = null
    
    override fun onCleared() {
        disposable?.dispose()
        super.onCleared()
    }
}

// ❌ Bad: Memory leaks
class BadViewModel(private val context: Context) : ViewModel()
```

### Database Optimization
```kotlin
// ✅ Good: Use indices for frequent queries
@Entity(
    tableName = "events",
    indices = [Index(value = ["userId", "date"])]
)
data class EventEntity(...)

// ✅ Good: Use pagination for large datasets
@Query("SELECT * FROM events ORDER BY date DESC LIMIT :limit OFFSET :offset")
suspend fun getEventsPaginated(limit: Int, offset: Int): List<EventEntity>
```

### Network Optimization
```kotlin
// ✅ Good: Implement retry with exponential backoff
suspend fun <T> retryWithBackoff(
    maxRetries: Int = 3,
    initialDelay: Long = 1000,
    block: suspend () -> T
): T {
    repeat(maxRetries) { attempt ->
        try {
            return block()
        } catch (e: Exception) {
            if (attempt == maxRetries - 1) throw e
            delay(initialDelay * (2.0.pow(attempt).toLong()))
        }
    }
    throw IllegalStateException("Should not reach here")
}
```

## Security Best Practices

### Data Protection
```kotlin
// ✅ Good: Encrypt sensitive data
class SecurePreferences @Inject constructor(
    private val securityUtils: SecurityUtils
) {
    fun storeSecureData(key: String, value: String) {
        val encrypted = securityUtils.encrypt(value)
        preferences.edit().putString(key, encrypted).apply()
    }
}

// ✅ Good: Validate input
fun validateEmail(email: String): Boolean {
    return email.matches(Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"))
}
```

### Network Security
```kotlin
// ✅ Good: Use certificate pinning
val okHttpClient = OkHttpClient.Builder()
    .certificatePinner(
        CertificatePinner.Builder()
            .add("api.earthmax.com", "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
            .build()
    )
    .build()
```

## Debugging and Monitoring

### Logging Best Practices
```kotlin
// ✅ Good: Structured logging
logger.info("User action completed") {
    put("userId", userId)
    put("action", "create_event")
    put("duration", duration)
}

// ❌ Bad: Sensitive data in logs
logger.debug("User password: $password") // Never log passwords!
```

### Performance Monitoring
```kotlin
// ✅ Good: Monitor critical operations
suspend fun createEvent(event: Event): Result<Event> {
    return performanceMonitor.measureSuspend("create_event") {
        try {
            val result = repository.createEvent(event)
            metricsCollector.incrementCounter("events_created")
            result
        } catch (e: Exception) {
            metricsCollector.incrementCounter("events_creation_failed")
            throw e
        }
    }
}
```

## Common Patterns

### Repository Pattern with Caching
```kotlin
@Singleton
class EventRepositoryImpl @Inject constructor(
    private val localDataSource: EventDao,
    private val remoteDataSource: SupabaseEventRepository,
    private val cacheManager: CacheManager,
    private val networkMonitor: NetworkMonitor
) : EventRepository {
    
    override fun getEvents(): Flow<List<Event>> = flow {
        // 1. Emit cached data immediately
        val cached = cacheManager.get<List<Event>>("events")
        if (cached != null) {
            emit(cached)
        }
        
        // 2. Fetch from local database
        val local = localDataSource.getAllEvents().first().map { it.toEvent() }
        if (local.isNotEmpty()) {
            emit(local)
            cacheManager.put("events", local, ttl = 5.minutes)
        }
        
        // 3. Fetch from remote if connected
        if (networkMonitor.isConnected.first()) {
            try {
                val remote = remoteDataSource.getEvents()
                localDataSource.insertEvents(remote.map { it.toEntity() })
                cacheManager.put("events", remote, ttl = 5.minutes)
                emit(remote)
            } catch (e: Exception) {
                // Log error but don't fail if we have local data
                logger.error("Failed to fetch remote events", e)
            }
        }
    }
}
```

### Error Handling Pattern
```kotlin
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Throwable) : Result<Nothing>()
    object Loading : Result<Nothing>()
}

// Usage in ViewModel
class EventsViewModel @Inject constructor(
    private val getEventsUseCase: GetEventsUseCase,
    private val errorHandler: ErrorHandler
) : ViewModel() {
    
    fun loadEvents() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            getEventsUseCase()
                .catch { error ->
                    val userMessage = errorHandler.handleError(error)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = userMessage
                    )
                }
                .collect { events ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        events = events,
                        error = null
                    )
                }
        }
    }
}
```

## Troubleshooting

### Common Issues

#### Build Errors
```bash
# Clear Gradle cache
./gradlew clean
rm -rf ~/.gradle/caches/

# Invalidate Android Studio caches
# File → Invalidate Caches and Restart
```

#### Database Migration Issues
```kotlin
// Always provide migration paths
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE events ADD COLUMN category TEXT DEFAULT ''")
    }
}
```

#### Memory Leaks
```bash
# Use LeakCanary for detection
debugImplementation 'com.squareup.leakcanary:leakcanary-android:2.10'

# Profile with Android Studio
# View → Tool Windows → Profiler
```

### Performance Issues
```kotlin
// Use Systrace for UI performance
adb shell atrace -t 10 -b 32768 -o ~/trace.html gfx input view

// Monitor network calls
adb shell setprop log.tag.OkHttp DEBUG
```

## Resources

### Documentation
- [Android Architecture Guide](https://developer.android.com/jetpack/guide)
- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Dagger Hilt](https://dagger.dev/hilt/)

### Tools
- [Detekt](https://detekt.github.io/detekt/) - Static code analysis
- [ktlint](https://ktlint.github.io/) - Kotlin linter
- [LeakCanary](https://square.github.io/leakcanary/) - Memory leak detection
- [Flipper](https://fbflipper.com/) - Mobile debugging platform

---

*Keep this guide updated as the project evolves and new patterns emerge.*