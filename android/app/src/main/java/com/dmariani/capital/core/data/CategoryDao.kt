package com.dmariani.capital.core.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface CategoryDao {

    @Transaction
    @Insert
    suspend fun insertCategories(entities: List<CategoryEntity>)

    @Query("SELECT * FROM categories WHERE workspace_id = :workspaceId AND deleted_at IS NULL")
    suspend fun getCategoriesForWorkspace(workspaceId: String): List<CategoryEntity>
}
