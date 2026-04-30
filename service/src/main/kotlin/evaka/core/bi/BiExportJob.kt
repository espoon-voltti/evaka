// SPDX-FileCopyrightText: 2024 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.bi

import evaka.core.shared.db.Database
import evaka.core.shared.domain.EvakaClock
import java.time.Duration

class BiExportJob(private val client: BiExportClient, private val config: BiExportConfig) {
    fun sendBiTable(db: Database.Connection, clock: EvakaClock, table: BiTable) {
        db.read { tx ->
            tx.setStatementTimeout(Duration.ofMinutes(10))

            table.query(tx, config) { records ->
                val stream = CsvInputStream(CSV_CHARSET, records)
                client.sendBiCsvFile(table.fileName, clock, stream)
            }
        }
    }
}
