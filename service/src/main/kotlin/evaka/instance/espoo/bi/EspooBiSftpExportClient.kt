// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.espoo.bi

import evaka.core.bi.BiExportClient
import evaka.core.bi.CsvInputStream
import evaka.core.shared.domain.EvakaClock
import evaka.core.shared.sftp.SftpClient
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.format.DateTimeFormatter

class EspooBiSftpExportClient(private val sftpClient: SftpClient) : BiExportClient {
    private val logger = KotlinLogging.logger {}

    override fun sendBiCsvFile(tableName: String, clock: EvakaClock, stream: CsvInputStream) {
        val date = clock.now().toLocalDate().format(DateTimeFormatter.ISO_DATE)
        val fileName = "$date/$tableName.csv"
        sftpClient.session { session ->
            session.createDirectoryIfMissing(date)
            session.put(stream, fileName)
        }
        logger.info { "BI table '$tableName' sent via SFTP to '$fileName'" }
    }
}
