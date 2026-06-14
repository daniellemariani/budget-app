package com.dmariani.capital.core.domain

data class Category(
    val id: String,
    val workspaceId: String,
    val name: String,
    val icon: String?,
    val color: String,
    val isDefault: Boolean,
    val isHidden: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
    val lastSyncedAt: Long?,
    val syncStatus: String?
)
