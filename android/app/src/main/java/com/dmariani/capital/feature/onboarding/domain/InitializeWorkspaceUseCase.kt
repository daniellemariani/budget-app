package com.dmariani.capital.feature.onboarding.domain

import com.dmariani.capital.core.domain.Category
import com.dmariani.capital.core.domain.Workspace
import java.util.UUID
import javax.inject.Inject

class InitializeWorkspaceUseCase @Inject constructor(
    private val repository: OnboardingRepository,
) {

    suspend operator fun invoke(): Result<String> = try {
        val existing = repository.getFirstWorkspace()
        if (existing != null) {
            Result.success(existing.id)
        } else {
            val now = System.currentTimeMillis() / 1000
            val workspaceId = UUID.randomUUID().toString()
            val workspace = Workspace(
                id = workspaceId,
                name = "Personal",
                baseCurrency = "USD",
                createdAt = now,
                updatedAt = now,
                deletedAt = null,
                lastSyncedAt = null,
                syncStatus = null,
            )
            repository.insertWorkspace(workspace)
            repository.insertCategories(buildDefaultCategories(workspaceId, now))
            Result.success(workspaceId)
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    private fun buildDefaultCategories(workspaceId: String, timestamp: Long): List<Category> =
        DEFAULT_CATEGORY_SEEDS.map { seed ->
            Category(
                id = UUID.randomUUID().toString(),
                workspaceId = workspaceId,
                name = seed.name,
                icon = seed.icon,
                color = seed.color,
                isDefault = true,
                isHidden = false,
                createdAt = timestamp,
                updatedAt = timestamp,
                deletedAt = null,
                lastSyncedAt = null,
                syncStatus = null,
            )
        }

    private data class DefaultCategorySeed(
        val name: String,
        val icon: String,
        val color: String,
    )

    companion object {
        private val DEFAULT_CATEGORY_SEEDS = listOf(
            DefaultCategorySeed("Groceries", "🛒", "#1D9E75"),
            DefaultCategorySeed("Dining Out", "🍽️", "#D85A30"),
            DefaultCategorySeed("Transport", "🚗", "#378ADD"),
            DefaultCategorySeed("Fuel", "⛽", "#BA7517"),
            DefaultCategorySeed("Utilities", "💡", "#BA7517"),
            DefaultCategorySeed("Housing", "🏠", "#639922"),
            DefaultCategorySeed("Health", "💊", "#E24B4A"),
            DefaultCategorySeed("Fitness & Sports", "💪", "#1D9E75"),
            DefaultCategorySeed("Entertainment", "🎬", "#D4537E"),
            DefaultCategorySeed("Shopping", "🛍️", "#D85A30"),
            DefaultCategorySeed("Education", "📚", "#378ADD"),
            DefaultCategorySeed("Travel", "✈️", "#D4537E"),
            DefaultCategorySeed("Personal Care", "💆‍♀️", "#D4537E"),
            DefaultCategorySeed("Subscriptions", "📱", "#378ADD"),
            DefaultCategorySeed("Gifts & Donations", "🎁", "#D4537E"),
            DefaultCategorySeed("Pets", "🐶", "#BA7517"),
            DefaultCategorySeed("Taxes & Fees", "🧾", "#D85A30"),
            DefaultCategorySeed("Savings", "🏦", "#1D9E75"),
            DefaultCategorySeed("Income", "💰", "#639922"),
            DefaultCategorySeed("Other", "📦", "#378ADD"),
        )
    }
}
