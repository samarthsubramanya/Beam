package com.beam.app.db

import com.beam.db.BeamDb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class TransferEntry(
    val peerName: String,
    val direction: String,
    val kind: String,
    val summary: String,
    val timestampEpochMillis: Long,
)

class BeamRepository(driverFactory: DatabaseDriverFactory) {
    private val db = BeamDb(driverFactory.createDriver())

    suspend fun isPaired(deviceId: String): Boolean = withContext(Dispatchers.Default) {
        db.pairedDeviceQueries.selectById(deviceId).executeAsOneOrNull() != null
    }

    suspend fun pairedDeviceIds(): Set<String> = withContext(Dispatchers.Default) {
        db.pairedDeviceQueries.selectAll().executeAsList().map { it.id }.toSet()
    }

    suspend fun addPairedDevice(deviceId: String, displayName: String, nowEpochMillis: Long) =
        withContext(Dispatchers.Default) {
            db.pairedDeviceQueries.insertOrReplace(deviceId, displayName, nowEpochMillis)
        }

    suspend fun recordHistory(entry: TransferEntry) = withContext(Dispatchers.Default) {
        db.transferHistoryQueries.insertEntry(
            id = "${entry.timestampEpochMillis}-${entry.peerName}",
            peerName = entry.peerName,
            direction = entry.direction,
            kind = entry.kind,
            summary = entry.summary,
            timestampEpochMillis = entry.timestampEpochMillis,
        )
    }

    suspend fun recentHistory(): List<TransferEntry> = withContext(Dispatchers.Default) {
        db.transferHistoryQueries.selectRecent().executeAsList().map {
            TransferEntry(it.peerName, it.direction, it.kind, it.summary, it.timestampEpochMillis)
        }
    }
}
