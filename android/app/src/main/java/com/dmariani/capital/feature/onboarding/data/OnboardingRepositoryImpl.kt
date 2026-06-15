package com.dmariani.capital.feature.onboarding.data

import com.dmariani.capital.core.data.AccountEntity
import com.dmariani.capital.core.data.CategoryEntity
import com.dmariani.capital.core.data.WorkspaceEntity
import com.dmariani.capital.core.domain.Account
import com.dmariani.capital.core.domain.AccountType
import com.dmariani.capital.core.domain.Category
import com.dmariani.capital.core.domain.Workspace
import com.dmariani.capital.feature.onboarding.data.local.OnboardingLocalDataSource
import com.dmariani.capital.feature.onboarding.domain.OnboardingRepository
import javax.inject.Inject

class OnboardingRepositoryImpl @Inject constructor(
    private val localDataSource: OnboardingLocalDataSource,
) : OnboardingRepository {

    override suspend fun getFirstWorkspace(): Workspace? =
        localDataSource.getFirstWorkspace()?.toDomain()

    override suspend fun insertWorkspace(workspace: Workspace) =
        localDataSource.insertWorkspace(workspace.toEntity())

    override suspend fun getCategoriesForWorkspace(workspaceId: String): List<Category> =
        localDataSource.getCategoriesForWorkspace(workspaceId).map { it.toDomain() }

    override suspend fun insertCategories(categories: List<Category>) =
        localDataSource.insertCategories(categories.map { it.toEntity() })

    override suspend fun getAccountByName(workspaceId: String, name: String): Account? =
        localDataSource.getAccountByName(workspaceId, name)?.toDomain()

    override suspend fun insertAccount(account: Account) =
        localDataSource.insertAccount(account.toEntity())
}

private fun WorkspaceEntity.toDomain(): Workspace = Workspace(
    id = id,
    name = name,
    baseCurrency = baseCurrency,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    lastSyncedAt = lastSyncedAt,
    syncStatus = syncStatus,
)

private fun Workspace.toEntity(): WorkspaceEntity = WorkspaceEntity(
    id = id,
    name = name,
    baseCurrency = baseCurrency,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    lastSyncedAt = lastSyncedAt,
    syncStatus = syncStatus,
)

private fun CategoryEntity.toDomain(): Category = Category(
    id = id,
    workspaceId = workspaceId,
    name = name,
    icon = icon,
    color = color,
    isDefault = isDefault,
    isHidden = isHidden,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    lastSyncedAt = lastSyncedAt,
    syncStatus = syncStatus,
)

private fun Category.toEntity(): CategoryEntity = CategoryEntity(
    id = id,
    workspaceId = workspaceId,
    name = name,
    icon = icon,
    color = color,
    isDefault = isDefault,
    isHidden = isHidden,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    lastSyncedAt = lastSyncedAt,
    syncStatus = syncStatus,
)

private fun AccountEntity.toDomain(): Account = Account(
    id = id,
    workspaceId = workspaceId,
    name = name,
    type = AccountType.valueOf(type),
    currencyCode = currencyCode,
    initialBalance = initialBalance,
    creditLimit = creditLimit,
    isPinned = isPinned,
    pinnedAt = pinnedAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    lastSyncedAt = lastSyncedAt,
    syncStatus = syncStatus,
)

private fun Account.toEntity(): AccountEntity = AccountEntity(
    id = id,
    workspaceId = workspaceId,
    name = name,
    type = type.name,
    currencyCode = currencyCode,
    initialBalance = initialBalance,
    creditLimit = creditLimit,
    isPinned = isPinned,
    pinnedAt = pinnedAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    lastSyncedAt = lastSyncedAt,
    syncStatus = syncStatus,
)
