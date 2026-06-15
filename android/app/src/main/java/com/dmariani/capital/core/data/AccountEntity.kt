package com.dmariani.capital.core.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "accounts",
    foreignKeys = [
        ForeignKey(
            entity = WorkspaceEntity::class,
            parentColumns = ["id"],
            childColumns = ["workspace_id"]
        )
    ],
    indices = [
        Index("workspace_id"),
        Index(value = ["is_pinned", "pinned_at"])
    ]
)
data class AccountEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "workspace_id")
    val workspaceId: String,

    @ColumnInfo(name = "name")
    val name: String,

    // Stored as TEXT (enum name, e.g. "CHECKING")
    @ColumnInfo(name = "type")
    val type: String,

    @ColumnInfo(name = "currency_code")
    val currencyCode: String,

    @ColumnInfo(name = "initial_balance")
    val initialBalance: Long,

    @ColumnInfo(name = "credit_limit")
    val creditLimit: Long?,

    @ColumnInfo(name = "is_pinned")
    val isPinned: Boolean,

    @ColumnInfo(name = "pinned_at")
    val pinnedAt: Long?,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,

    @ColumnInfo(name = "deleted_at")
    val deletedAt: Long?,

    @ColumnInfo(name = "last_synced_at")
    val lastSyncedAt: Long?,

    @ColumnInfo(name = "sync_status")
    val syncStatus: String?
)
