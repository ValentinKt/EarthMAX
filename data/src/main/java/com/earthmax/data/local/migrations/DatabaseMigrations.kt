package com.earthmax.data.local.migrations

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(connection: SQLiteConnection) {
        // Create messages table
        connection.execSQL("""
            CREATE TABLE IF NOT EXISTS `messages` (
                `id` TEXT NOT NULL,
                `eventId` TEXT NOT NULL,
                `senderId` TEXT NOT NULL,
                `senderName` TEXT NOT NULL,
                `senderAvatarUrl` TEXT,
                `content` TEXT NOT NULL,
                `timestamp` INTEGER NOT NULL,
                `messageType` TEXT NOT NULL,
                `isRead` INTEGER NOT NULL DEFAULT 0,
                `replyToMessageId` TEXT,
                `isSynced` INTEGER NOT NULL DEFAULT 0,
                PRIMARY KEY(`id`)
            )
        """.trimIndent())
        
        // Create index for better query performance
        connection.execSQL("""
            CREATE INDEX IF NOT EXISTS `index_messages_eventId` ON `messages` (`eventId`)
        """.trimIndent())
        
        connection.execSQL("""
            CREATE INDEX IF NOT EXISTS `index_messages_timestamp` ON `messages` (`timestamp`)
        """.trimIndent())
    }
}