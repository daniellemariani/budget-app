package com.dmariani.capital.feature.onboarding.domain

import com.dmariani.capital.core.domain.Account
import com.dmariani.capital.core.domain.AccountType
import java.util.UUID
import javax.inject.Inject

class CreateAccountUseCase @Inject constructor(
    private val repository: OnboardingRepository,
) {

    suspend operator fun invoke(params: CreateAccountParams): Result<Unit> = try {
        if (repository.getAccountByName(params.workspaceId, params.name.trim()) != null) {
            Result.failure(DuplicateAccountNameException())
        } else {
            val now = System.currentTimeMillis() / 1000
            val account = Account(
                id = UUID.randomUUID().toString(),
                workspaceId = params.workspaceId,
                name = params.name.trim(),
                type = params.type,
                currencyCode = params.currencyCode,
                initialBalance = params.initialBalanceCents,
                creditLimit = params.creditLimitCents,
                isPinned = false,
                pinnedAt = null,
                createdAt = now,
                updatedAt = now,
                deletedAt = null,
                lastSyncedAt = null,
                syncStatus = null,
            )
            repository.insertAccount(account)
            Result.success(Unit)
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}

data class CreateAccountParams(
    val workspaceId: String,
    val name: String,
    val type: AccountType,
    val currencyCode: String,
    val initialBalanceCents: Long,
    val creditLimitCents: Long?,
)

class DuplicateAccountNameException : Exception()
