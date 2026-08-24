package com.beam.app.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.beam.app.discovery.appContext
import com.beam.db.BeamDb

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver = AndroidSqliteDriver(BeamDb.Schema, appContext, "beam.db")
}
