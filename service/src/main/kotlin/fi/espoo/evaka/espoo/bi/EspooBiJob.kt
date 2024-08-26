// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.espoo.bi

import fi.espoo.evaka.espoo.EspooAsyncJob
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.time.Duration

class EspooBiJob(private val client: EspooBiClient) {
    fun sendBiTable(db: Database.Connection, clock: EvakaClock, msg: EspooAsyncJob.SendBiTable) =
        sendBiTable(db, clock, msg.table.fileName, msg.table.query)

    fun sendBiTable(
        db: Database.Connection,
        clock: EvakaClock,
        tableName: String,
        query: CsvQuery,
    ) {
        val timestamp = clock.now().toInstant().toEpochMilli()
        val fileName = "evaka_${tableName}_$timestamp.csv"
        db.read { tx ->
            tx.setStatementTimeout(Duration.ofMinutes(5))
            query(tx) { records ->
                val stream = CsvInputStream(CSV_CHARSET, records)
                client.sendBiCsvFile(fileName, stream)
            }
        }
    }

    class CsvInputStream(private val charset: Charset, records: Sequence<String>) : InputStream() {
        var totalBytes: Int = 0
            private set

        private val iterator = records.iterator()
        private var buffer: ByteBuffer? = null

        private fun acquireBuffer(): ByteBuffer? {
            if (buffer?.hasRemaining() != true) {
                buffer = null
                while (iterator.hasNext()) {
                    val bytes = iterator.next().toByteArray(charset)
                    if (bytes.isNotEmpty()) {
                        buffer = ByteBuffer.wrap(bytes)
                        break
                    }
                }
            }
            return buffer
        }

        override fun read(): Int =
            when (val buffer = acquireBuffer()) {
                null -> -1 // end of stream
                else -> buffer.get().toInt().also { totalBytes += 1 }
            }

        override fun available(): Int = acquireBuffer()?.remaining() ?: 0
    }
}
