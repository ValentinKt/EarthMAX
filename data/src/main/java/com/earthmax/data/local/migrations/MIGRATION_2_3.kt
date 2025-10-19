package com.earthmax.data.local.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Create performance_metrics table
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS performance_metrics (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                operation TEXT NOT NULL,
                tag TEXT NOT NULL,
                duration INTEGER NOT NULL,
                timestamp INTEGER NOT NULL,
                metadata TEXT NOT NULL
            )
        """)
        
        // Create log_entries table
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS log_entries (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                level TEXT NOT NULL,
                tag TEXT NOT NULL,
                message TEXT NOT NULL,
                timestamp INTEGER NOT NULL,
                exception TEXT,
                metadata TEXT NOT NULL
            )
        """)
        
        // Create indexes for better performance
        database.execSQL("CREATE INDEX IF NOT EXISTS index_performance_metrics_timestamp ON performance_metrics(timestamp)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_performance_metrics_operation ON performance_metrics(operation)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_performance_metrics_tag ON performance_metrics(tag)")
        
        database.execSQL("CREATE INDEX IF NOT EXISTS index_log_entries_timestamp ON log_entries(timestamp)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_log_entries_level ON log_entries(level)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_log_entries_tag ON log_entries(tag)")
    }
}