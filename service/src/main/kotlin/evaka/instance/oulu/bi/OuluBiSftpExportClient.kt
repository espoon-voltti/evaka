// SPDX-FileCopyrightText: 2026 City of Oulu
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.oulu.bi

import evaka.core.bi.BiExportClient
import evaka.core.bi.CsvInputStream
import evaka.core.shared.domain.EvakaClock
import evaka.core.shared.sftp.SftpClient
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.format.DateTimeFormatter

class OuluBiSftpExportClient(private val sftpClient: SftpClient, private val remotePath: String) :
    BiExportClient {
    private val logger = KotlinLogging.logger {}

    override fun sendBiCsvFile(tableName: String, clock: EvakaClock, stream: CsvInputStream) {
        val date = clock.now().toLocalDate()
        val fileName = "${tableName}_${date.format(DateTimeFormatter.ISO_DATE)}.csv"
        val target = "$remotePath$fileName"

        logger.info { "Sending BI content for '$tableName' via SFTP" }

        sftpClient.put(stream, target)

        logger.info { "BI file '$target' successfully sent via SFTP" }
    }
}
