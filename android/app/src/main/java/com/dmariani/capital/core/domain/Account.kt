package com.dmariani.capital.core.domain

data class Account(
    val id: String,
    val workspaceId: String,
    val name: String,
    val type: AccountType,
    val currencyCode: String,
    val initialBalance: Long,
    val creditLimit: Long?,
    val isPinned: Boolean,
    val pinnedAt: Long?,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
    val lastSyncedAt: Long?,
    val syncStatus: String?
)
