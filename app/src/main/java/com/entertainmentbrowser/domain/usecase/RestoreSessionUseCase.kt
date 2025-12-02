package com.entertainmentbrowser.domain.usecase

import com.entertainmentbrowser.domain.repository.SessionRepository
import javax.inject.Inject

class RestoreSessionUseCase @Inject constructor(
    private val sessionRepository: SessionRepository
) {
    suspend operator fun invoke(sessionId: Int): List<String> {
        return sessionRepository.restoreSession(sessionId)
    }
}
