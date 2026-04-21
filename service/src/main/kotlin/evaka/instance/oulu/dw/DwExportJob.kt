// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.oulu.dw

import evaka.core.bi.CSV_CHARSET
import evaka.core.bi.CsvInputStream
import evaka.core.shared.db.Database
import evaka.core.shared.domain.EvakaClock
import evaka.instance.oulu.OuluAsyncJob
import java.time.Duration

class DwExportJob(private val client: DwExportClient) {
    fun sendDwQuery(db: Database.Connection, clock: EvakaClock, msg: OuluAsyncJob.SendDWQuery) =
        sendQuery(db, clock, msg.query.queryName, msg.query.query)

    fun sendQuery(db: Database.Connection, clock: EvakaClock, queryName: String, query: CsvQuery) {
        db.read { tx ->
            tx.setStatementTimeout(Duration.ofMinutes(10))

            query(tx) { records ->
                val stream = CsvInputStream(CSV_CHARSET, records)
                client.sendDwCsvFile(queryName, clock, stream)
            }
        }
    }
}
