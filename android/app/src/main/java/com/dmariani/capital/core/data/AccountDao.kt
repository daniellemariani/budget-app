package com.dmariani.capital.core.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface AccountDao {

    @Insert
    suspend fun insertAccount(entity: AccountEntity)

    // Filters deleted_at IS NULL to enforce the unique (workspace_id, name) constraint
    // for non-deleted accounts, since SQLite partial indexes are not supported by Room.
    @Query("SELECT * FROM accounts WHERE workspace_id = :workspaceId AND name = :name AND deleted_at IS NULL LIMIT 1")
    suspend fun getAccountByName(workspaceId: String, name: String): AccountEntity?
}
