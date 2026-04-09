// SPDX-FileCopyrightText: 2024 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.tampere.bi

import evaka.core.shared.db.Database
import evaka.core.shared.domain.EvakaClock
import evaka.instance.espoo.bi.CSV_CHARSET
import evaka.instance.espoo.bi.EspooBiJob
import evaka.instance.tampere.TampereAsyncJob
import java.time.Duration

class BiExportJob(private val client: BiExportClient) {
    fun sendBiTable(db: Database.Connection, clock: EvakaClock, msg: TampereAsyncJob.SendBiTable) =
        sendBiTable(db, clock, msg.table.fileName, msg.table.query)

    fun sendBiTable(
        db: Database.Connection,
        clock: EvakaClock,
        tableName: String,
        query: BiQueries.CsvQuery,
    ) {
        db.read { tx ->
            tx.setStatementTimeout(Duration.ofMinutes(10))

            query(tx) { records ->
                val stream = EspooBiJob.CsvInputStream(CSV_CHARSET, records)
                client.sendBiCsvFile(tableName, clock, stream)
            }
        }
    }
}
