package com.beam.app.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.beam.db.BeamDb
import java.io.File

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        val dbFile = File(System.getProperty("user.home"), ".beam/beam.db")
        dbFile.parentFile.mkdirs()
        return JdbcSqliteDriver("jdbc:sqlite:${dbFile.absolutePath}", schema = BeamDb.Schema)
    }
}
