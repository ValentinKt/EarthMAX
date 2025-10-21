package com.earthmax.data.di

import android.content.Context
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.network.okHttpClient
import com.earthmax.data.local.EarthMaxDatabase
import com.earthmax.data.local.dao.EventDao
import com.earthmax.data.local.dao.MessageDao
import com.earthmax.data.local.dao.UserDao
import com.earthmax.data.local.dao.PerformanceDao
import com.earthmax.data.auth.SupabaseAuthRepository
import com.earthmax.data.chat.SupabaseChatRepository
import com.earthmax.data.chat.ChatRepository
import com.earthmax.data.chat.ChatRepositoryImpl
import com.earthmax.data.events.SupabaseEventsRepository
import com.earthmax.data.repository.SupabaseUserRepository
import com.earthmax.data.repository.UserRepository
import com.earthmax.data.api.UserApiService
import com.earthmax.data.api.EventApiService
import com.earthmax.data.api.repository.UserApiRepository
import com.earthmax.data.api.validation.ApiValidator
import com.earthmax.data.api.repository.EventApiRepository
import com.earthmax.data.api.interceptor.AuthInterceptor
import com.earthmax.data.api.interceptor.ErrorInterceptor
import com.earthmax.data.api.interceptor.RateLimitInterceptor
import com.earthmax.data.todo.SupabaseTodoRepository
import com.earthmax.core.network.SupabaseClient
import com.earthmax.core.network.BuildConfig
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    
    @Provides
    @Singleton
    fun provideEarthMaxDatabase(@ApplicationContext context: Context): EarthMaxDatabase {
        return EarthMaxDatabase.create(context)
    }
    
    @Provides
    fun provideEventDao(database: EarthMaxDatabase): EventDao {
        return database.eventDao()
    }
    
    @Provides
    fun provideUserDao(database: EarthMaxDatabase): UserDao {
        return database.userDao()
    }
    
    @Provides
    fun provideMessageDao(database: EarthMaxDatabase): MessageDao {
        return database.messageDao()
    }
    
    @Provides
    fun providePerformanceDao(database: EarthMaxDatabase): PerformanceDao {
        return database.performanceDao()
    }
    
    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient {
        return SupabaseClient
    }
    
    @Provides
    @Singleton
    fun provideSupabaseAuthRepository(): SupabaseAuthRepository {
        return SupabaseAuthRepository()
    }
    
    @Provides
    @Singleton
    fun provideSupabaseEventsRepository(): SupabaseEventsRepository {
        return SupabaseEventsRepository()
    }
    
    @Provides
    @Singleton
    fun provideSupabaseUserRepository(
        userDao: UserDao,
        authRepository: SupabaseAuthRepository
    ): SupabaseUserRepository {
        return SupabaseUserRepository(userDao, authRepository)
    }
    
    @Provides
    @Singleton
    fun provideUserRepository(
        supabaseUserRepository: SupabaseUserRepository
    ): UserRepository {
        return supabaseUserRepository
    }
    
    @Provides
    @Singleton
    fun provideGson(): Gson {
        return Gson()
    }
    
    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        errorInterceptor: ErrorInterceptor,
        rateLimitInterceptor: RateLimitInterceptor
    ): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(rateLimitInterceptor)
            .addInterceptor(errorInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    
    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, gson: Gson): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://your-api-base-url.com/api/v1/") // Replace with your actual API base URL
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    @Provides
    @Singleton
    fun provideUserApiService(retrofit: Retrofit): UserApiService {
        return retrofit.create(UserApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideEventApiService(retrofit: Retrofit): EventApiService {
        return retrofit.create(EventApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideUserApiRepository(
        userApiService: UserApiService,
        apiValidator: ApiValidator
    ): UserApiRepository {
        return UserApiRepository(userApiService, apiValidator)
    }

    @Provides
    @Singleton
    fun provideApiValidator(): ApiValidator {
        return ApiValidator()
    }

    @Provides
    @Singleton
    fun provideEventApiRepository(
        eventApiService: EventApiService,
        apiValidator: ApiValidator
    ): EventApiRepository {
        return EventApiRepository(eventApiService, apiValidator)
    }
    
    @Provides
    @Singleton
    fun provideApolloClient(okHttpClient: OkHttpClient): ApolloClient {
        return ApolloClient.Builder()
            .serverUrl("${BuildConfig.SUPABASE_URL}/graphql/v1")
            .okHttpClient(okHttpClient)
            .build()
    }
    
    @Provides
    @Singleton
    fun provideSupabaseChatRepository(): SupabaseChatRepository {
        return SupabaseChatRepository()
    }
    
    @Provides
    @Singleton
    fun provideChatRepository(
        supabaseChatRepository: SupabaseChatRepository,
        messageDao: MessageDao
    ): ChatRepository {
        return ChatRepositoryImpl(supabaseChatRepository, messageDao)
    }
    
    @Provides
    @Singleton
    fun providePerformanceRepository(
        performanceDao: PerformanceDao
    ): com.earthmax.data.repository.PerformanceRepository {
        return com.earthmax.data.repository.PerformanceRepository(performanceDao)
    }
    
    @Provides
    @Singleton
    fun provideSupabaseTodoRepository(): SupabaseTodoRepository {
        return SupabaseTodoRepository()
    }
}