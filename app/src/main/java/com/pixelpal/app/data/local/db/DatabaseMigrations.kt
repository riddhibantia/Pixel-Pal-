package com.pixelpal.app.data.local.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseMigrations {

    /**
     * Version 3 added `hour`, `minute`, `soundUri` columns to the reminders table.
     * Version history jumped 1 -> 3 in a single change, so both migrations apply the
     * same additive ALTER statements (covers any device that briefly saw version 2).
     */
    val MIGRATION_1_3 = object : Migration(1, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE reminders ADD COLUMN hour INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE reminders ADD COLUMN minute INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE reminders ADD COLUMN soundUri TEXT")
        }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE reminders ADD COLUMN hour INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE reminders ADD COLUMN minute INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE reminders ADD COLUMN soundUri TEXT")
        }
    }

    /**
     * Version 4 aligned the `hour`/`minute` column defaults with what the v1/v2
     * migrations actually produced. SQLite cannot ALTER a column default in place,
     * so the table is rebuilt with the exact v4 schema.
     */
    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `_new_reminders` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `title` TEXT NOT NULL,
                    `message` TEXT,
                    `triggerTime` INTEGER NOT NULL,
                    `hour` INTEGER NOT NULL DEFAULT 0,
                    `minute` INTEGER NOT NULL DEFAULT 0,
                    `soundUri` TEXT,
                    `recurrence` TEXT NOT NULL,
                    `recurrenceInterval` INTEGER,
                    `category` TEXT NOT NULL,
                    `status` TEXT NOT NULL,
                    `snoozeCount` INTEGER NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    `completedAt` INTEGER
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO `_new_reminders` (`id`, `title`, `message`, `triggerTime`, `hour`, `minute`,
                    `soundUri`, `recurrence`, `recurrenceInterval`, `category`, `status`, `snoozeCount`,
                    `createdAt`, `completedAt`)
                SELECT `id`, `title`, `message`, `triggerTime`, `hour`, `minute`,
                    `soundUri`, `recurrence`, `recurrenceInterval`, `category`, `status`, `snoozeCount`,
                    `createdAt`, `completedAt`
                FROM `reminders`
                """.trimIndent()
            )
            db.execSQL("DROP TABLE `reminders`")
            db.execSQL("ALTER TABLE `_new_reminders` RENAME TO `reminders`")
        }
    }
}
