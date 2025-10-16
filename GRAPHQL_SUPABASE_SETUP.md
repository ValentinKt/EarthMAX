# GraphQL with Supabase Integration Guide

## Overview
This guide explains how to use GraphQL with Supabase in the EarthMAX Android application. The integration combines Apollo GraphQL client with Supabase's auto-generated GraphQL API to provide efficient data querying capabilities.

## Prerequisites
- Supabase project with GraphQL API enabled
- Apollo GraphQL dependencies configured
- Proper authentication setup

## Configuration

### 1. Supabase GraphQL Endpoint
Supabase automatically generates a GraphQL API for your database. The endpoint follows this format:
```
https://<PROJECT_REF>.supabase.co/graphql/v1
```

### 2. Apollo Client Configuration
The Apollo client is configured in `DataModule.kt` to work with Supabase:

```kotlin
@Provides
@Singleton
fun provideApolloClient(okHttpClient: OkHttpClient): ApolloClient {
    return ApolloClient.Builder()
        .serverUrl("${BuildConfig.SUPABASE_URL}/graphql/v1")
        .okHttpClient(okHttpClient)
        .build()
}
```

### 3. Authentication Headers
GraphQL requests to Supabase require proper authentication headers:
- `Authorization: Bearer <jwt-token>` - User authentication
- `apikey: <supabase-anon-key>` - API key for Supabase

These are automatically handled by the configured OkHttpClient with interceptors.

## GraphQL Schema

### Schema Definition
The GraphQL schema is defined in `/data/src/main/graphql/com/earthmax/data/schema.graphqls`:

```graphql
type Query {
    getEcoTips(category: String, limit: Int): [EcoTip!]!
}

type EcoTip {
    id: ID!
    title: String!
    description: String!
    category: String!
    difficulty: String
    estimatedImpact: String
    steps: [String!]
    imageUrl: String
    tags: [String!]
    createdAt: String!
    updatedAt: String!
}
```

### Query Definitions
GraphQL queries are defined in `/data/src/main/graphql/com/earthmax/data/GetEcoTips.graphql`:

#### 1. Get Eco Tips (with optional filters)
```graphql
query GetEcoTips($category: String, $limit: Int) {
    getEcoTips(category: $category, limit: $limit) {
        id
        title
        description
        category
        difficulty
        estimatedImpact
        steps
        imageUrl
        tags
        createdAt
        updatedAt
    }
}
```

#### 2. Get Eco Tips by Category
```graphql
query GetEcoTipsByCategory($category: String!) {
    getEcoTips(category: $category) {
        id
        title
        description
        category
        difficulty
        estimatedImpact
        steps
        imageUrl
        tags
        createdAt
        updatedAt
    }
}
```

#### 3. Get Random Eco Tips
```graphql
query GetRandomEcoTips($limit: Int = 10) {
    getEcoTips(limit: $limit) {
        id
        title
        description
        category
        difficulty
        estimatedImpact
        steps
        imageUrl
        tags
        createdAt
        updatedAt
    }
}
```

## Implementation

### ApolloService
The `ApolloService` class wraps Apollo client operations and provides reactive Flow-based responses:

```kotlin
@Singleton
class ApolloService @Inject constructor(
    private val apolloClient: ApolloClient
) {
    suspend fun getEcoTips(
        category: String? = null,
        limit: Int? = null
    ): Flow<Result<List<GetEcoTipsQuery.GetEcoTip>>> = flow {
        try {
            val response: ApolloResponse<GetEcoTipsQuery.Data> = apolloClient
                .query(GetEcoTipsQuery(
                    category = Optional.presentIfNotNull(category),
                    limit = Optional.presentIfNotNull(limit)
                ))
                .execute()

            if (response.hasErrors()) {
                emit(Result.failure(Exception(response.errors?.firstOrNull()?.message)))
            } else {
                val ecoTips = response.data?.getEcoTips ?: emptyList()
                emit(Result.success(ecoTips))
            }
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
}
```

### EcoTipsRepository
The repository layer provides a clean interface for accessing eco-tips data:

```kotlin
@Singleton
class EcoTipsRepository @Inject constructor(
    private val apolloService: ApolloService
) {
    suspend fun getEcoTips(
        category: String? = null,
        limit: Int? = null
    ): Flow<Result<List<GetEcoTipsQuery.GetEcoTip>>> {
        return apolloService.getEcoTips(category, limit)
    }

    suspend fun getEcoTipsByCategory(category: String): Flow<Result<List<GetEcoTipsQuery.GetEcoTip>>> {
        return apolloService.getEcoTipsByCategory(category)
    }

    suspend fun getRandomEcoTips(limit: Int = 10): Flow<Result<List<GetEcoTipsQuery.GetEcoTip>>> {
        return apolloService.getRandomEcoTips(limit)
    }

    suspend fun getDailyEcoTip(): Flow<Result<List<GetEcoTipsQuery.GetEcoTip>>> {
        return getRandomEcoTips(1)
    }
}
```

## Usage Examples

### In a ViewModel
```kotlin
class EcoTipsViewModel @Inject constructor(
    private val ecoTipsRepository: EcoTipsRepository
) : ViewModel() {
    
    fun loadEcoTips(category: String? = null) {
        viewModelScope.launch {
            ecoTipsRepository.getEcoTips(category = category, limit = 20)
                .collect { result ->
                    result.onSuccess { ecoTips ->
                        // Update UI state with eco tips
                    }.onFailure { error ->
                        // Handle error
                    }
                }
        }
    }
}
```

### In a Composable
```kotlin
@Composable
fun EcoTipsScreen(viewModel: EcoTipsViewModel = hiltViewModel()) {
    LaunchedEffect(Unit) {
        viewModel.loadEcoTips()
    }
    
    // UI implementation
}
```

## Build Configuration

### Dependencies
Ensure these dependencies are included in your `data/build.gradle.kts`:

```kotlin
dependencies {
    implementation(libs.apollo.runtime)
    implementation(libs.apollo.cache)
    implementation(libs.supabase.postgrest.kt)
    implementation(libs.supabase.auth.kt)
}
```

### Apollo Plugin
The Apollo plugin is configured to generate Kotlin classes from GraphQL schemas:

```kotlin
apollo {
    service("service") {
        packageName.set("com.earthmax.data.graphql")
        introspection {
            endpointUrl.set("${project.findProperty("SUPABASE_URL")}/graphql/v1")
            headers.put("apikey", project.findProperty("SUPABASE_ANON_KEY") as String)
        }
    }
}
```

## Testing

### Build Verification
To test the GraphQL integration, run:
```bash
./gradlew :data:build
```

This will:
1. Generate GraphQL classes from schema files
2. Compile the Apollo service and repository classes
3. Verify all dependencies are correctly configured

### Integration Testing
Create integration tests to verify GraphQL queries work correctly:

```kotlin
@Test
fun testGetEcoTips() = runTest {
    val result = ecoTipsRepository.getEcoTips(category = "energy", limit = 5)
    result.collect { 
        assertTrue(it.isSuccess)
        val ecoTips = it.getOrNull()
        assertNotNull(ecoTips)
        assertTrue(ecoTips!!.size <= 5)
    }
}
```

## Troubleshooting

### Common Issues

1. **Duplicate Operation Error**
   - Ensure GraphQL files are in the correct directory structure
   - Avoid duplicate query names across different files

2. **Authentication Errors**
   - Verify Supabase API key is correctly configured
   - Check JWT token is valid and not expired

3. **Build Failures**
   - Clean and rebuild: `./gradlew clean build`
   - Check GraphQL schema syntax
   - Verify all required dependencies are included

### File Structure
```
data/src/main/graphql/com/earthmax/data/
├── GetEcoTips.graphql          # Query definitions
└── schema.graphqls             # GraphQL schema
```

## Benefits of GraphQL with Supabase

1. **Efficient Querying**: Request only the data you need
2. **Type Safety**: Auto-generated Kotlin classes provide compile-time safety
3. **Real-time Subscriptions**: Support for real-time data updates (future enhancement)
4. **Automatic API Generation**: Supabase generates GraphQL API from database schema
5. **Integrated Authentication**: Seamless integration with Supabase auth

## Next Steps

1. **Add Subscriptions**: Implement real-time data updates using GraphQL subscriptions
2. **Caching**: Configure Apollo cache for offline support
3. **Error Handling**: Enhance error handling for network failures
4. **Performance**: Implement query batching and optimization

This integration provides a robust foundation for efficient data querying in the EarthMAX application while maintaining type safety and excellent developer experience.