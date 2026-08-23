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

    /**
     * Version 5 introduces the multi-companion workspace:
     *  - `companions` replaces the legacy single-row `companion` table.
     *  - `bond` and `personality` are re-keyed from `id` to `companionId`.
     *  - New `tasks`, `agent_config`, `agent_status`, `activity_events` tables.
     *  - `reminders` gains a nullable `companionId` FK (ON DELETE SET NULL).
     *
     * Room-only data. The app-level CompanionBootstrapInitializer fills in the
     * legacy pet name/type from DataStore after migration.
     */
    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // 1. Companions (replaces the legacy single-row `companion` table).
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `companions` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `name` TEXT NOT NULL,
                    `petType` TEXT NOT NULL,
                    `role` TEXT NOT NULL,
                    `description` TEXT,
                    `createdAt` INTEGER NOT NULL,
                    `lastUsedAt` INTEGER,
                    `isFavorite` INTEGER NOT NULL,
                    `isArchived` INTEGER NOT NULL,
                    `hatId` TEXT,
                    `outfitId` TEXT,
                    `accessoryId` TEXT
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO `companions` (`id`, `name`, `petType`, `role`, `description`, `createdAt`,
                    `lastUsedAt`, `isFavorite`, `isArchived`, `hatId`, `outfitId`, `accessoryId`)
                SELECT `id`, 'Pixel', `petType`, 'GENERAL', NULL, 0, NULL, 0, 0, `hatId`, `outfitId`, `accessoryId`
                FROM `companion`
                """.trimIndent()
            )
            // Ensure a default companion exists even when the legacy table was empty
            // (bond/personality below reference companionId 1).
            db.execSQL(
                """
                INSERT INTO `companions` (`name`, `petType`, `role`, `createdAt`, `isFavorite`, `isArchived`)
                SELECT 'Pixel', 'cat', 'GENERAL', 0, 0, 0
                WHERE NOT EXISTS (SELECT 1 FROM `companions`)
                """.trimIndent()
            )

            // 2. Bond re-keyed to companionId (PK + FK CASCADE).
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `_new_bond` (
                    `companionId` INTEGER NOT NULL,
                    `level` INTEGER NOT NULL,
                    `totalInteractions` INTEGER NOT NULL,
                    `tapsToday` INTEGER NOT NULL,
                    `feedsToday` INTEGER NOT NULL,
                    `lastInteractionTime` INTEGER NOT NULL,
                    `streakDays` INTEGER NOT NULL,
                    `lastStreakDate` TEXT NOT NULL,
                    PRIMARY KEY(`companionId`),
                    FOREIGN KEY(`companionId`) REFERENCES `companions`(`id`)
                        ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO `_new_bond` (`companionId`, `level`, `totalInteractions`, `tapsToday`, `feedsToday`,
                    `lastInteractionTime`, `streakDays`, `lastStreakDate`)
                SELECT (SELECT `id` FROM `companions` ORDER BY `id` LIMIT 1), `level`, `totalInteractions`, `tapsToday`, `feedsToday`,
                    `lastInteractionTime`, `streakDays`, `lastStreakDate`
                FROM `bond`
                """.trimIndent()
            )
            db.execSQL("DROP TABLE `bond`")
            db.execSQL("ALTER TABLE `_new_bond` RENAME TO `bond`")

            // 3. Personality re-keyed to companionId (PK + FK CASCADE).
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `_new_personality` (
                    `companionId` INTEGER NOT NULL,
                    `friendliness` REAL NOT NULL,
                    `curiosity` REAL NOT NULL,
                    `playfulness` REAL NOT NULL,
                    `sleepiness` REAL NOT NULL,
                    `confidence` REAL NOT NULL,
                    `independence` REAL NOT NULL,
                    `lastUpdated` INTEGER NOT NULL,
                    PRIMARY KEY(`companionId`),
                    FOREIGN KEY(`companionId`) REFERENCES `companions`(`id`)
                        ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO `_new_personality` (`companionId`, `friendliness`, `curiosity`, `playfulness`,
                    `sleepiness`, `confidence`, `independence`, `lastUpdated`)
                SELECT (SELECT `id` FROM `companions` ORDER BY `id` LIMIT 1), `friendliness`, `curiosity`, `playfulness`,
                    `sleepiness`, `confidence`, `independence`, `lastUpdated`
                FROM `personality`
                """.trimIndent()
            )
            db.execSQL("DROP TABLE `personality`")
            db.execSQL("ALTER TABLE `_new_personality` RENAME TO `personality`")

            // 4. Tasks.
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `tasks` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `companionId` INTEGER NOT NULL,
                    `title` TEXT NOT NULL,
                    `isDone` INTEGER NOT NULL,
                    `dueAt` INTEGER,
                    `createdAt` INTEGER NOT NULL,
                    `completedAt` INTEGER,
                    FOREIGN KEY(`companionId`) REFERENCES `companions`(`id`)
                        ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_companionId` ON `tasks`(`companionId`)")

            // 5. Agent config.
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `agent_config` (
                    `companionId` INTEGER NOT NULL,
                    `endpointUrl` TEXT NOT NULL,
                    `enabled` INTEGER NOT NULL,
                    `pollIntervalMinutes` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    PRIMARY KEY(`companionId`),
                    FOREIGN KEY(`companionId`) REFERENCES `companions`(`id`)
                        ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent()
            )

            // 6. Agent status.
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `agent_status` (
                    `companionId` INTEGER NOT NULL,
                    `state` TEXT NOT NULL,
                    `message` TEXT,
                    `lastCheckedAt` INTEGER,
                    `lastSuccessfulCheckAt` INTEGER,
                    `consecutiveFailureCount` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    PRIMARY KEY(`companionId`),
                    FOREIGN KEY(`companionId`) REFERENCES `companions`(`id`)
                        ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent()
            )

            // 7. Activity timeline.
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `activity_events` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `companionId` INTEGER NOT NULL,
                    `type` TEXT NOT NULL,
                    `title` TEXT NOT NULL,
                    `description` TEXT,
                    `createdAt` INTEGER NOT NULL,
                    FOREIGN KEY(`companionId`) REFERENCES `companions`(`id`)
                        ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_activity_events_companionId` ON `activity_events`(`companionId`)")

            // 8. Reminders: nullable companionId FK (ON DELETE SET NULL) + index.
            db.execSQL(
                "ALTER TABLE `reminders` ADD COLUMN `companionId` INTEGER REFERENCES `companions`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL"
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_reminders_companionId` ON `reminders`(`companionId`)")

            // 9. Drop the legacy single-row companion table.
            db.execSQL("DROP TABLE IF EXISTS `companion`")
        }
    }

    /**
     * Version 6 adds the in-app Activity Center:
     *  - `activity_events.isRead` for the unread badge / mark-as-read behavior.
     * Legacy TAP/FEED rows are kept but hidden by the Activity Center query.
     */
    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `activity_events` ADD COLUMN `isRead` INTEGER NOT NULL DEFAULT 0")
        }
    }
}
