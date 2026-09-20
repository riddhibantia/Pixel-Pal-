package com.pixelpal.app.db

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pixelpal.app.data.local.db.DatabaseMigrations
import com.pixelpal.app.data.local.db.PixelPalDatabase
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/**
 * Guards the Room upgrade path that once crashed prod
 * (migration 2->1 / 2->3 missing, see crash_log_utf8.txt).
 * Schemas are served from app/schemas via androidTest assets srcDir.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        PixelPalDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    @Throws(IOException::class)
    fun migrate11To12_createsPerformanceIndices() {
        helper.createDatabase(TEST_DB, 11).close()

        helper.runMigrationsAndValidate(
            TEST_DB,
            12,
            true,
            DatabaseMigrations.MIGRATION_11_12
        ).use { db ->
            val indices = mutableListOf<String>()
            db.query("SELECT name FROM sqlite_master WHERE type = 'index'").use { c ->
                while (c.moveToNext()) indices += c.getString(0)
            }
            assert(indices.contains("index_tasks_cloudId")) { "missing index_tasks_cloudId: $indices" }
            assert(indices.contains("index_tasks_companionId_updatedAt")) { "missing tasks companion index" }
            assert(indices.contains("index_reminders_cloudId")) { "missing reminders cloudId index" }
            assert(indices.contains("index_reminders_companionId_triggerTime")) { "missing reminders companion index" }
        }
    }

    @Test
    @Throws(IOException::class)
    fun migrate3To12_fullChainPreservesData() {
        var db = helper.createDatabase(TEST_DB, 3)
        db.execSQL(
            "INSERT INTO reminders (title, triggerTime, hour, minute, recurrence, category, status, snoozeCount, createdAt) " +
                "VALUES ('keep me', 12345, 0, 0, 'ONCE', 'CUSTOM', 'PENDING', 0, 12345)"
        )
        db.close()

        helper.runMigrationsAndValidate(
            TEST_DB,
            12,
            true,
            DatabaseMigrations.MIGRATION_3_4,
            DatabaseMigrations.MIGRATION_4_5,
            DatabaseMigrations.MIGRATION_5_6,
            DatabaseMigrations.MIGRATION_6_7,
            DatabaseMigrations.MIGRATION_7_8,
            DatabaseMigrations.MIGRATION_8_9,
            DatabaseMigrations.MIGRATION_9_10,
            DatabaseMigrations.MIGRATION_10_11,
            DatabaseMigrations.MIGRATION_11_12
        ).use { migrated ->
            migrated.query("SELECT title FROM reminders").use { c ->
                assert(c.count == 1) { "expected 1 reminder after 3->12, got ${c.count}" }
                c.moveToFirst()
                assert(c.getString(0) == "keep me")
            }
        }
    }

    companion object {
        private const val TEST_DB = "migration-test"
    }
}
