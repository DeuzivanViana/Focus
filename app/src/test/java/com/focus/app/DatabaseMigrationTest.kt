package com.focus.app

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import com.focus.core.database.FocusDatabase
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Makes sure updating the app keeps every recorded session. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DatabaseMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), FocusDatabase::class.java)

    @Test
    fun migrate1To2KeepsSessions() {
        helper.createDatabase(DB, 1).use { db ->
            db.execSQL("INSERT INTO focus_sessions (started_at, ended_at, focused_seconds, completed) VALUES (1000, 2000, 1500, 1)")
            db.execSQL("INSERT INTO focus_sessions (started_at, ended_at, focused_seconds, completed) VALUES (3000, 4000, 600, 0)")
        }
        helper.runMigrationsAndValidate(DB, 2, true).use { db ->
            db.query("SELECT COUNT(*), SUM(focused_seconds), COUNT(subject_id) FROM focus_sessions").use { c ->
                c.moveToFirst()
                assertEquals(2, c.getInt(0))
                assertEquals(2100, c.getInt(1))
                assertEquals(0, c.getInt(2)) // old sessions have no subject
            }
            db.execSQL("INSERT INTO subjects (name, color, archived, created_at) VALUES ('English', 'TEAL', 0, 5000)")
        }
    }

    @Test
    fun migrate2To3KeepsSessionsAndAddsReflection() {
        helper.createDatabase(DB3, 2).use { db ->
            db.execSQL("INSERT INTO subjects (id, name, color, archived, created_at) VALUES (1, 'English', 'TEAL', 0, 5000)")
            db.execSQL("INSERT INTO focus_sessions (started_at, ended_at, focused_seconds, completed, subject_id) VALUES (1000, 2000, 1500, 1, 1)")
        }
        helper.runMigrationsAndValidate(DB3, 3, true).use { db ->
            db.query("SELECT COUNT(*), MAX(subject_id), COUNT(rating) FROM focus_sessions").use { c ->
                c.moveToFirst()
                assertEquals(1, c.getInt(0))
                assertEquals(1, c.getInt(1))
                assertEquals(0, c.getInt(2))
            }
            db.execSQL("INSERT INTO goals (title, subject_id, target_minutes, start_day, deadline_day, created_at) VALUES ('100h', 1, 6000, 20000, 20100, 1)")
        }
    }

    @Test
    fun migrate3To4KeepsDataAndAddsPlanTables() {
        helper.createDatabase(DB4, 3).use { db ->
            db.execSQL("INSERT INTO focus_sessions (started_at, ended_at, focused_seconds, completed, rating, note) VALUES (1000, 2000, 1500, 1, 4, 'ok')")
            db.execSQL("INSERT INTO goals (title, subject_id, target_minutes, start_day, deadline_day, created_at) VALUES ('100h', NULL, 6000, 20000, 20100, 1)")
        }
        helper.runMigrationsAndValidate(DB4, 4, true).use { db ->
            db.query("SELECT COUNT(*), MAX(rating) FROM focus_sessions").use { c ->
                c.moveToFirst()
                assertEquals(1, c.getInt(0))
                assertEquals(4, c.getInt(1))
            }
            db.query("SELECT COUNT(*) FROM goals").use { c -> c.moveToFirst(); assertEquals(1, c.getInt(0)) }
            db.execSQL("INSERT INTO routine_blocks (title, subject_id, days_mask, start_minute, duration_minutes, remind_before) VALUES ('Math', NULL, 5, 1140, 50, 10)")
            db.execSQL("INSERT INTO reviews (title, subject_id, studied_day, stage, created_at) VALUES ('Ch. 3', NULL, 20000, 0, 1)")
        }
    }

    private companion object {
        const val DB4 = "migration-test-4"
        const val DB3 = "migration-test-3"
        const val DB = "migration-test"
    }
}
