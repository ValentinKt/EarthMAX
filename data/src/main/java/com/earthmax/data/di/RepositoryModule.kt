package com.earthmax.data.di

import com.earthmax.data.repository.EventRepositoryImpl
import com.earthmax.data.repository.TodoRepositoryImpl
import com.earthmax.data.repository.UserRepositoryImpl
import com.earthmax.domain.repository.EventRepository
import com.earthmax.domain.repository.TodoRepository
import com.earthmax.domain.repository.UserRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dagger Hilt module for providing repository implementations
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    /**
     * Binds EventRepositoryImpl to EventRepository interface
     */
    @Binds
    @Singleton
    abstract fun bindEventRepository(
        eventRepositoryImpl: EventRepositoryImpl
    ): EventRepository

    /**
     * Binds UserRepositoryImpl to UserRepository interface
     */
    @Binds
    @Singleton
    abstract fun bindUserRepository(
        userRepositoryImpl: UserRepositoryImpl
    ): UserRepository

    /**
     * Binds TodoRepositoryImpl to TodoRepository interface
     */
    @Binds
    @Singleton
    abstract fun bindTodoRepository(
        todoRepositoryImpl: TodoRepositoryImpl
    ): TodoRepository
}