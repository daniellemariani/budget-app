package com.dmariani.capital.feature.onboarding.data.local

import com.dmariani.capital.core.data.AccountDao
import com.dmariani.capital.core.data.AccountEntity
import com.dmariani.capital.core.data.CategoryDao
import com.dmariani.capital.core.data.CategoryEntity
import com.dmariani.capital.core.data.WorkspaceDao
import com.dmariani.capital.core.data.WorkspaceEntity
import javax.inject.Inject

class OnboardingLocalDataSource @Inject constructor(
    private val workspaceDao: WorkspaceDao,
    private val categoryDao: CategoryDao,
    private val accountDao: AccountDao,
) {

    suspend fun getFirstWorkspace(): WorkspaceEntity? =
        workspaceDao.getFirstWorkspace()

    suspend fun insertWorkspace(entity: WorkspaceEntity) =
        workspaceDao.insertWorkspace(entity)

    suspend fun getCategoriesForWorkspace(workspaceId: String): List<CategoryEntity> =
        categoryDao.getCategoriesForWorkspace(workspaceId)

    suspend fun insertCategories(entities: List<CategoryEntity>) =
        categoryDao.insertCategories(entities)

    suspend fun getAccountByName(workspaceId: String, name: String): AccountEntity? =
        accountDao.getAccountByName(workspaceId, name)

    suspend fun insertAccount(entity: AccountEntity) =
        accountDao.insertAccount(entity)
}
