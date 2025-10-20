# EarthMAX Android Architecture Documentation

## Overview

EarthMAX is an environmental impact tracking Android application built with modern Android development practices, following Clean Architecture principles with a multi-module structure.

## Architecture Principles

### Clean Architecture
The application follows Uncle Bob's Clean Architecture with clear separation of concerns:

1. **Presentation Layer** (`presentation/`)
   - UI components (Activities, Fragments, Composables)
   - ViewModels for state management
   - Navigation and user interaction handling

2. **Domain Layer** (`domain/`)
   - Business logic and use cases
   - Domain models and entities
   - Repository interfaces (contracts)

3. **Data Layer** (`data/`)
   - Repository implementations
   - Data sources (local and remote)
   - Data models and mappers

4. **Core Layer** (`core/`)
   - Shared utilities and common functionality
   - Monitoring and performance tracking
   - Security and configuration

## Module Structure

```
EarthMAX/
├── app/                    # Main application module
├── core/                   # Shared utilities and infrastructure
├── data/                   # Data layer implementation
├── domain/                 # Business logic and contracts
├── presentation/           # UI layer
└── docs/                   # Documentation
```

### Core Module (`core/`)

The core module provides shared functionality across all layers:

#### Configuration (`core/config/`)
- `AppConfig.kt`: Centralized application configuration
- Feature flags and environment-specific settings
- Cache, network, and performance configurations

#### Security (`core/security/`)
- `SecurityUtils.kt`: Encryption, hashing, and validation utilities
- Password strength validation
- Input sanitization and token generation

#### Monitoring (`core/monitoring/`)
- `PerformanceMonitor.kt`: Application performance tracking
- `NetworkMonitor.kt`: Network connectivity monitoring
- `MetricsCollector.kt`: Application metrics collection

#### Utilities (`core/utils/`)
- `CacheManager.kt`: Intelligent caching with TTL support
- `ErrorHandler.kt`: Comprehensive error handling
- `Logger.kt`: Structured logging system

### Data Layer (`data/`)

Implements the repository pattern with comprehensive data management:

#### Local Storage (`data/local/`)
- Room database for offline-first architecture
- DAOs for data access operations
- Entity models for local persistence

#### Remote Data (`data/remote/`)
- Supabase integration for backend services
- GraphQL operations for efficient data fetching
- Network error handling and retry mechanisms

#### Repository Implementation (`data/repository/`)
- `EventRepositoryImpl.kt`: Event management with caching
- `UserRepositoryImpl.kt`: User profile and authentication
- Data mapping between local and domain models

### Domain Layer (`domain/`)

Contains business logic and defines contracts:

#### Models (`domain/model/`)
- Pure domain entities without framework dependencies
- Business rules and validation logic

#### Repository Interfaces (`domain/repository/`)
- Contracts for data operations
- Abstraction from implementation details

#### Use Cases (`domain/usecase/`)
- Single responsibility business operations
- Orchestration of repository calls
- Business logic validation

### Presentation Layer (`presentation/`)

Handles user interface and interaction:

#### UI Components (`presentation/ui/`)
- Jetpack Compose UI components
- Screen-specific composables
- Reusable UI elements

#### ViewModels (`presentation/viewmodel/`)
- State management with StateFlow
- UI event handling
- Integration with domain use cases

## Dependency Injection

The application uses Dagger Hilt for dependency injection with modular configuration:

### Core Module (`core/di/CoreModule.kt`)
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object CoreModule {
    @Provides @Singleton
    fun provideCacheManager(): CacheManager
    
    @Provides @Singleton
    fun provideErrorHandler(logger: Logger): ErrorHandler
    
    @Provides @Singleton
    fun provideLogger(): Logger
    
    @Provides @Singleton
    fun providePerformanceMonitor(): PerformanceMonitor
    
    @Provides @Singleton
    fun provideMetricsCollector(): MetricsCollector
    
    @Provides @Singleton
    fun provideNetworkMonitor(@ApplicationContext context: Context): NetworkMonitor
}
```

### Repository Module (`data/di/RepositoryModule.kt`)
```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindEventRepository(
        eventRepositoryImpl: EventRepositoryImpl
    ): EventRepository
    
    @Binds
    abstract fun bindUserRepository(
        userRepositoryImpl: UserRepositoryImpl
    ): UserRepository
}
```

### Use Case Module (`domain/di/UseCaseModule.kt`)
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {
    @Provides @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
    
    @Provides @MainDispatcher
    fun provideMainDispatcher(): CoroutineDispatcher = Dispatchers.Main
    
    @Provides @DefaultDispatcher
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default
}
```

## Data Flow

### Typical Data Flow Pattern
1. **UI Layer**: User interaction triggers ViewModel
2. **ViewModel**: Calls appropriate use case
3. **Use Case**: Orchestrates business logic and repository calls
4. **Repository**: Manages data sources (local cache + remote API)
5. **Data Sources**: Fetch/store data from Room DB or Supabase
6. **Response Flow**: Data flows back through layers with proper mapping

### Caching Strategy
- **First-level**: In-memory caching with TTL
- **Second-level**: Room database for offline access
- **Third-level**: Remote Supabase backend

### Error Handling
- Network errors with retry mechanisms
- Database errors with fallback strategies
- User-friendly error messages
- Comprehensive logging for debugging

## Performance Monitoring

### Metrics Collection
- Operation timing and performance statistics
- Network connectivity monitoring
- Memory usage tracking
- User interaction analytics

### Performance Optimization
- Lazy loading of data
- Image caching and optimization
- Database query optimization
- Background processing with WorkManager

## Security Considerations

### Data Protection
- AES encryption for sensitive data
- SHA-256 hashing for passwords
- Input sanitization and validation
- Secure token management

### Network Security
- HTTPS-only communication
- Certificate pinning
- Request/response validation
- Rate limiting and throttling

## Testing Strategy

### Unit Tests
- Domain layer business logic
- Repository implementations
- Utility functions and helpers

### Integration Tests
- Database operations
- Network API calls
- End-to-end data flow

### UI Tests
- User interaction scenarios
- Navigation flow testing
- Accessibility compliance

## Build Configuration

### Gradle Modules
Each module has its own `build.gradle.kts` with:
- Specific dependencies
- Build variants (debug/release)
- ProGuard/R8 configuration

### CI/CD Pipeline
- Automated testing on pull requests
- Code quality checks (Detekt, Ktlint)
- Automated deployment to staging/production

## Future Enhancements

### Planned Improvements
1. **Offline-First Architecture**: Enhanced sync capabilities
2. **Advanced Caching**: Intelligent cache invalidation
3. **Performance Optimization**: Memory management improvements
4. **Testing Coverage**: Comprehensive test suite
5. **CI/CD Pipeline**: Automated deployment workflow

### Scalability Considerations
- Modular architecture for feature additions
- Plugin-based extension system
- Microservice-ready backend integration
- Multi-platform code sharing potential

---

*This documentation is maintained alongside the codebase and should be updated with architectural changes.*