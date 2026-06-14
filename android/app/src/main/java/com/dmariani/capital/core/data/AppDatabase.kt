package com.dmariani.capital.core.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [WorkspaceEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun workspaceDao(): WorkspaceDao
}
