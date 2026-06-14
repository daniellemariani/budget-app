package com.dmariani.capital.core.domain

data class Workspace(
    val id: String,
    val name: String,
    val baseCurrency: String,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
    val lastSyncedAt: Long?,
    val syncStatus: String?
)
