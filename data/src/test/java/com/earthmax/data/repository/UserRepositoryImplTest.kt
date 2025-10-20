package com.earthmax.data.repository

import com.earthmax.core.cache.AdvancedCacheManager
import com.earthmax.core.cache.CachePolicy
import com.earthmax.core.error.AdvancedErrorHandler
import com.earthmax.core.monitoring.Logger
import com.earthmax.core.monitoring.MetricsCollector
import com.earthmax.data.local.dao.UserDao
import com.earthmax.data.local.entity.UserEntity
import com.earthmax.data.mapper.UserMapper
import com.earthmax.data.remote.api.UserApi
import com.earthmax.data.remote.dto.UserDto
import com.earthmax.domain.model.User
import com.earthmax.domain.repository.UserRepository
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime

class UserRepositoryImplTest {

    private lateinit var userRepository: UserRepository
    private lateinit var userApi: UserApi
    private lateinit var userDao: UserDao
    private lateinit var cacheManager: AdvancedCacheManager
    private lateinit var errorHandler: AdvancedErrorHandler
    private lateinit var logger: Logger
    private lateinit var metricsCollector: MetricsCollector

    private val testUserDto = UserDto(
        id = "1",
        email = "test@example.com",
        username = "testuser",
        fullName = "Test User",
        avatarUrl = "https://example.com/avatar.jpg",
        bio = "Test bio",
        location = "Test City",
        joinedEvents = listOf("event1", "event2"),
        createdEvents = listOf("event3"),
        preferences = mapOf("notifications" to true, "theme" to "dark"),
        isActive = true,
        lastLoginAt = "2024-01-15T10:00:00",
        createdAt = "2024-01-01T00:00:00",
        updatedAt = "2024-01-15T10:00:00"
    )

    private val testUserEntity = UserEntity(
        id = "1",
        email = "test@example.com",
        username = "testuser",
        fullName = "Test User",
        avatarUrl = "https://example.com/avatar.jpg",
        bio = "Test bio",
        location = "Test City",
        joinedEvents = listOf("event1", "event2"),
        createdEvents = listOf("event3"),
        preferences = mapOf("notifications" to true, "theme" to "dark"),
        isActive = true,
        lastLoginAt = LocalDateTime.of(2024, 1, 15, 10, 0),
        createdAt = LocalDateTime.of(2024, 1, 1, 0, 0),
        updatedAt = LocalDateTime.of(2024, 1, 15, 10, 0)
    )

    private val testUser = User(
        id = "1",
        email = "test@example.com",
        username = "testuser",
        fullName = "Test User",
        avatarUrl = "https://example.com/avatar.jpg",
        bio = "Test bio",
        location = "Test City",
        joinedEvents = listOf("event1", "event2"),
        createdEvents = listOf("event3"),
        preferences = mapOf("notifications" to true, "theme" to "dark"),
        isActive = true,
        lastLoginAt = LocalDateTime.of(2024, 1, 15, 10, 0),
        createdAt = LocalDateTime.of(2024, 1, 1, 0, 0),
        updatedAt = LocalDateTime.of(2024, 1, 15, 10, 0)
    )

    @Before
    fun setUp() {
        userApi = mockk()
        userDao = mockk()
        logger = mockk(relaxed = true)
        metricsCollector = mockk(relaxed = true)
        cacheManager = mockk()
        errorHandler = mockk()

        userRepository = UserRepositoryImpl(
            userApi = userApi,
            userDao = userDao,
            cacheManager = cacheManager,
            errorHandler = errorHandler,
            logger = logger,
            metricsCollector = metricsCollector
        )
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `getCurrentUser should return cached user when available`() = runBlocking {
        // Given
        coEvery { cacheManager.get<User>("current_user") } returns testUser

        // When
        val result = userRepository.getCurrentUser().toList()

        // Then
        assertEquals(1, result.size)
        assertEquals(testUser, result[0])
        verify { logger.d("UserRepository", "Loading current user from cache") }
    }

    @Test
    fun `getCurrentUser should fetch from API when cache is empty`() = runBlocking {
        // Given
        coEvery { cacheManager.get<User>("current_user") } returns null
        coEvery { userApi.getCurrentUser() } returns testUserDto
        coEvery { userDao.getCurrentUser() } returns flowOf(testUserEntity)
        coEvery { userDao.insertUser(any()) } just Runs
        coEvery { cacheManager.put(any(), any<User>(), any()) } just Runs

        // When
        val result = userRepository.getCurrentUser().toList()

        // Then
        assertEquals(1, result.size)
        assertEquals(testUser.id, result[0].id)
        
        coVerify { userApi.getCurrentUser() }
        coVerify { userDao.insertUser(any()) }
        coVerify { cacheManager.put("current_user", any<User>(), CachePolicy.TimeToLive(600000)) }
    }

    @Test
    fun `getUserById should return cached user when available`() = runBlocking {
        // Given
        val userId = "1"
        coEvery { cacheManager.get<User>("user_$userId") } returns testUser

        // When
        val result = userRepository.getUserById(userId)

        // Then
        assertEquals(testUser, result)
        verify { logger.d("UserRepository", "Loading user $userId from cache") }
    }

    @Test
    fun `getUserById should fetch from API when not cached`() = runBlocking {
        // Given
        val userId = "1"
        coEvery { cacheManager.get<User>("user_$userId") } returns null
        coEvery { userApi.getUserById(userId) } returns testUserDto
        coEvery { userDao.getUserById(userId) } returns testUserEntity
        coEvery { userDao.insertUser(any()) } just Runs
        coEvery { cacheManager.put(any(), any<User>(), any()) } just Runs

        // When
        val result = userRepository.getUserById(userId)

        // Then
        assertEquals(testUser.id, result?.id)
        
        coVerify { userApi.getUserById(userId) }
        coVerify { userDao.insertUser(any()) }
        coVerify { cacheManager.put("user_$userId", any<User>(), CachePolicy.TimeToLive(300000)) }
    }

    @Test
    fun `updateUserProfile should update user and invalidate cache`() = runBlocking {
        // Given
        val updatedUser = testUser.copy(fullName = "Updated Name")
        val updatedUserDto = testUserDto.copy(fullName = "Updated Name")
        
        coEvery { userApi.updateUserProfile(testUser.id, any()) } returns updatedUserDto
        coEvery { userDao.updateUser(any()) } just Runs
        coEvery { cacheManager.put(any(), any<User>(), any()) } just Runs
        coEvery { cacheManager.invalidate(any()) } just Runs

        // When
        val result = userRepository.updateUserProfile(updatedUser)

        // Then
        assertEquals("Updated Name", result?.fullName)
        
        coVerify { userApi.updateUserProfile(testUser.id, any()) }
        coVerify { userDao.updateUser(any()) }
        coVerify { cacheManager.put("user_${testUser.id}", any<User>(), CachePolicy.TimeToLive(300000)) }
        coVerify { cacheManager.invalidate(any()) }
    }

    @Test
    fun `updateUserPreferences should update preferences and cache`() = runBlocking {
        // Given
        val newPreferences = mapOf("notifications" to false, "theme" to "light")
        val updatedUserDto = testUserDto.copy(preferences = newPreferences)
        
        coEvery { userApi.updateUserPreferences(testUser.id, newPreferences) } returns updatedUserDto
        coEvery { userDao.updateUser(any()) } just Runs
        coEvery { cacheManager.put(any(), any<User>(), any()) } just Runs
        coEvery { cacheManager.invalidate(any()) } just Runs

        // When
        val result = userRepository.updateUserPreferences(testUser.id, newPreferences)

        // Then
        assertEquals(newPreferences, result?.preferences)
        
        coVerify { userApi.updateUserPreferences(testUser.id, newPreferences) }
        coVerify { userDao.updateUser(any()) }
        coVerify { cacheManager.put("user_${testUser.id}", any<User>(), CachePolicy.TimeToLive(300000)) }
        coVerify { cacheManager.invalidate(any()) }
    }

    @Test
    fun `getUsersByIds should return multiple users`() = runBlocking {
        // Given
        val userIds = listOf("1", "2")
        val users = listOf(testUser, testUser.copy(id = "2", username = "testuser2"))
        val userDtos = listOf(testUserDto, testUserDto.copy(id = "2", username = "testuser2"))
        val userEntities = listOf(testUserEntity, testUserEntity.copy(id = "2", username = "testuser2"))
        
        coEvery { cacheManager.get<List<User>>("users_${userIds.joinToString("_")}") } returns null
        coEvery { userApi.getUsersByIds(userIds) } returns userDtos
        coEvery { userDao.getUsersByIds(userIds) } returns userEntities
        coEvery { userDao.insertUsers(any()) } just Runs
        coEvery { cacheManager.put(any(), any<List<User>>(), any()) } just Runs

        // When
        val result = userRepository.getUsersByIds(userIds)

        // Then
        assertEquals(2, result.size)
        assertEquals("1", result[0].id)
        assertEquals("2", result[1].id)
        
        coVerify { userApi.getUsersByIds(userIds) }
        coVerify { userDao.insertUsers(any()) }
        coVerify { cacheManager.put("users_${userIds.joinToString("_")}", any<List<User>>(), CachePolicy.TimeToLive(300000)) }
    }

    @Test
    fun `searchUsers should return filtered users`() = runBlocking {
        // Given
        val query = "test"
        val users = listOf(testUser)
        val userDtos = listOf(testUserDto)
        val userEntities = listOf(testUserEntity)
        
        coEvery { cacheManager.get<List<User>>("search_users_$query") } returns null
        coEvery { userApi.searchUsers(query) } returns userDtos
        coEvery { userDao.searchUsers("%$query%") } returns userEntities
        coEvery { cacheManager.put(any(), any<List<User>>(), any()) } just Runs

        // When
        val result = userRepository.searchUsers(query)

        // Then
        assertEquals(1, result.size)
        assertTrue(result[0].username.contains(query, ignoreCase = true))
        
        coVerify { userApi.searchUsers(query) }
        coVerify { cacheManager.put("search_users_$query", any<List<User>>(), CachePolicy.TimeToLive(60000)) }
    }

    @Test
    fun `deleteUser should delete user and invalidate cache`() = runBlocking {
        // Given
        val userId = "1"
        coEvery { userApi.deleteUser(userId) } just Runs
        coEvery { userDao.deleteUser(userId) } just Runs
        coEvery { cacheManager.invalidate(any()) } just Runs

        // When
        userRepository.deleteUser(userId)

        // Then
        coVerify { userApi.deleteUser(userId) }
        coVerify { userDao.deleteUser(userId) }
        coVerify { cacheManager.invalidate(any()) }
        verify { logger.i("UserRepository", "User $userId deleted successfully") }
    }

    @Test
    fun `refreshUserData should force refresh from API`() = runBlocking {
        // Given
        val userId = "1"
        coEvery { userApi.getUserById(userId) } returns testUserDto
        coEvery { userDao.insertUser(any()) } just Runs
        coEvery { cacheManager.put(any(), any<User>(), any()) } just Runs
        coEvery { cacheManager.invalidate(any()) } just Runs

        // When
        val result = userRepository.refreshUserData(userId)

        // Then
        assertEquals(testUser.id, result?.id)
        
        coVerify { cacheManager.invalidate(any()) }
        coVerify { userApi.getUserById(userId) }
        coVerify { userDao.insertUser(any()) }
        coVerify { cacheManager.put("user_$userId", any<User>(), CachePolicy.TimeToLive(300000)) }
        verify { logger.i("UserRepository", "User data refreshed for user $userId") }
    }

    @Test
    fun `updateLastLoginTime should update timestamp`() = runBlocking {
        // Given
        val userId = "1"
        val loginTime = LocalDateTime.now()
        coEvery { userDao.updateLastLoginTime(userId, any()) } just Runs
        coEvery { cacheManager.invalidate(any()) } just Runs

        // When
        userRepository.updateLastLoginTime(userId, loginTime)

        // Then
        coVerify { userDao.updateLastLoginTime(userId, any()) }
        coVerify { cacheManager.invalidate(any()) }
        verify { logger.d("UserRepository", "Updated last login time for user $userId") }
    }
}