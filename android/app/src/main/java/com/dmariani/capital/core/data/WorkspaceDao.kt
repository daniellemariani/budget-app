package com.dmariani.capital.core.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface WorkspaceDao {

    @Insert
    suspend fun insertWorkspace(entity: WorkspaceEntity)

    @Query("SELECT * FROM workspaces WHERE deleted_at IS NULL LIMIT 1")
    suspend fun getFirstWorkspace(): WorkspaceEntity?
}
