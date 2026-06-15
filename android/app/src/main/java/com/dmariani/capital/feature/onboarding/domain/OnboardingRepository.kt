package com.dmariani.capital.feature.onboarding.domain

import com.dmariani.capital.core.domain.Account
import com.dmariani.capital.core.domain.Category
import com.dmariani.capital.core.domain.Workspace

interface OnboardingRepository {

    suspend fun getFirstWorkspace(): Workspace?

    suspend fun insertWorkspace(workspace: Workspace)

    suspend fun getCategoriesForWorkspace(workspaceId: String): List<Category>

    suspend fun insertCategories(categories: List<Category>)

    suspend fun getAccountByName(workspaceId: String, name: String): Account?

    suspend fun insertAccount(account: Account)
}
