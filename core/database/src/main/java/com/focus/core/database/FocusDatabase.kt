package com.focus.core.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Local app database. When adding new tables (tasks, routines...), bump
 * [version] and write a Migration: never use destructive migration in
 * production, so the user's history is never wiped.
 */
@Database(
    entities = [FocusSessionEntity::class, SubjectEntity::class, TaskEntity::class, GoalEntity::class, RoutineBlockEntity::class, ReviewEntity::class],
    version = 4,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2), AutoMigration(from = 2, to = 3), AutoMigration(from = 3, to = 4)],
)
abstract class FocusDatabase : RoomDatabase() {
    abstract fun focusSessionDao(): FocusSessionDao
    abstract fun subjectDao(): SubjectDao
    abstract fun goalDao(): GoalDao
    abstract fun planDao(): PlanDao
}
