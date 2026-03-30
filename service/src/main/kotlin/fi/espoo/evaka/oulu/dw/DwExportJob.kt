// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.oulu.dw

import fi.espoo.evaka.espoo.bi.CSV_CHARSET
import fi.espoo.evaka.espoo.bi.EspooBiJob
import fi.espoo.evaka.oulu.EvakaOuluAsyncJob
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import java.time.Duration

class DwExportJob(private val client: DwExportClient) {
    fun sendDwQuery(
        db: Database.Connection,
        clock: EvakaClock,
        msg: EvakaOuluAsyncJob.SendDWQuery,
    ) = sendQuery(db, clock, msg.query.queryName, msg.query.query, prefix = "")

    fun sendFabricQuery(
        db: Database.Connection,
        clock: EvakaClock,
        msg: EvakaOuluAsyncJob.SendFabricQuery,
    ) = sendQuery(db, clock, msg.query.queryName, msg.query.query, "fabric_")

    fun sendFabricHistoryQuery(
        db: Database.Connection,
        clock: EvakaClock,
        msg: EvakaOuluAsyncJob.SendFabricHistoryQuery,
    ) = sendQuery(db, clock, msg.query.queryName, msg.query.query, "fabric-history_")

    fun sendQuery(
        db: Database.Connection,
        clock: EvakaClock,
        queryName: String,
        query: CsvQuery,
        prefix: String,
    ) {
        db.read { tx ->
            tx.setStatementTimeout(Duration.ofMinutes(10))

            query(tx) { records ->
                val stream = EspooBiJob.CsvInputStream(CSV_CHARSET, records)
                client.sendDwCsvFile(queryName, clock, stream, prefix)
            }
        }
    }
}
