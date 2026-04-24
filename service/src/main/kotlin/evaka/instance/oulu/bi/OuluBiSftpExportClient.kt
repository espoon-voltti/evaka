// SPDX-FileCopyrightText: 2026 City of Oulu
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.oulu.bi

import evaka.core.bi.BiExportClient
import evaka.core.bi.CsvInputStream
import evaka.core.shared.domain.EvakaClock
import evaka.core.shared.sftp.SftpClient
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.BufferedOutputStream
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class OuluBiSftpExportClient(private val sftpClient: SftpClient, private val remotePath: String) :
    BiExportClient {
    private val logger = KotlinLogging.logger {}

    override fun sendBiCsvFile(
        tableName: String,
        clock: EvakaClock,
        stream: CsvInputStream,
    ): Pair<String, String> {
        val date = clock.now().toLocalDate()
        val entryName = "$tableName.csv"
        val fileName = "${tableName}_${date.format(DateTimeFormatter.ISO_DATE)}.zip"
        val target = "$remotePath$fileName"

        logger.info { "Sending BI content for '$tableName' via SFTP" }

        sftpClient.put(target) { os ->
            BufferedOutputStream(os).use { bos ->
                ZipOutputStream(bos).use { zip ->
                    zip.putNextEntry(ZipEntry(entryName))
                    stream.transferTo(zip)
                    zip.closeEntry()
                }
            }
        }

        logger.info { "BI file '$target' successfully sent via SFTP" }
        return remotePath to fileName
    }
}
