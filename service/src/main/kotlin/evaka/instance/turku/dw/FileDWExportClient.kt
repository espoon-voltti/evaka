// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.turku.dw

import evaka.core.bi.CsvInputStream
import evaka.core.shared.domain.EvakaClock
import evaka.instance.turku.invoice.service.SftpSender
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.BufferedOutputStream
import java.nio.file.Files
import java.time.format.DateTimeFormatter
import kotlin.io.path.deleteIfExists

class FileDWExportClient(private val sftpSender: SftpSender) : DwExportClient {
    private val logger = KotlinLogging.logger {}

    override fun sendDwCsvFile(queryName: String, clock: EvakaClock, stream: CsvInputStream) {
        val date = clock.now().toLocalDate()
        val fileName = "$queryName${date.format(DateTimeFormatter.ISO_DATE)}.csv"
        val tempFile = Files.createTempFile("dw", fileName)

        try {
            logger.info { "Sending DW content for '$queryName' via SFTP" }
            Files.newOutputStream(tempFile).use { fos ->
                BufferedOutputStream(fos).use { bos -> stream.transferTo(bos) }
            }
            sftpSender.send(tempFile.toFile().readText(Charsets.UTF_8), fileName)
        } catch (e: Exception) {
            logger.warn(e) { "Failed to send DW content for '$queryName'" }
        } finally {
            val wasDeleted = tempFile.deleteIfExists()
            if (!wasDeleted) {
                logger.warn {
                    "DW temporary file clean up for '${tempFile.fileName}' did not find anything to delete"
                }
            }
        }
    }
}
