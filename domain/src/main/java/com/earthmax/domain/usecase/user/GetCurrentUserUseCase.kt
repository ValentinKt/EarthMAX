package com.earthmax.domain.usecase.user

import com.earthmax.domain.model.DomainUser
import com.earthmax.domain.model.Result
import com.earthmax.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for getting the current authenticated user.
 */
class GetCurrentUserUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    
    operator fun invoke(): Flow<Result<DomainUser?>> {
        return userRepository.getCurrentUser()
    }
}