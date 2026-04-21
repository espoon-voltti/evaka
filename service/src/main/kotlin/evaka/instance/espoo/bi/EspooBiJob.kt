// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.espoo.bi

import evaka.core.bi.CSV_CHARSET
import evaka.core.bi.CsvInputStream
import evaka.core.shared.db.Database
import evaka.core.shared.domain.EvakaClock
import evaka.instance.espoo.EspooAsyncJob
import java.time.Duration

class EspooBiJob(private val client: EspooBiHttpClient) {
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
}
