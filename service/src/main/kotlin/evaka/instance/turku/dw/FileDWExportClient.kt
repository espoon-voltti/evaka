// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.turku.dw

import evaka.core.shared.domain.EvakaClock
import evaka.instance.espoo.bi.EspooBiJob
import evaka.instance.turku.TurkuEnv
import evaka.instance.turku.invoice.service.SftpSender
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.BufferedOutputStream
import java.nio.file.Files
import java.time.format.DateTimeFormatter
import kotlin.io.path.deleteIfExists
import software.amazon.awssdk.services.s3.S3Client

class FileDWExportClient(
    private val s3Client: S3Client,
    private val sftpSender: SftpSender,
    private val properties: TurkuEnv,
) : DwExportClient {
    private val logger = KotlinLogging.logger {}

    override fun sendDwCsvFile(
        queryName: String,
        clock: EvakaClock,
        stream: EspooBiJob.CsvInputStream,
    ): Pair<String, String> {
        val date = clock.now().toLocalDate()
        val fileName = "$queryName${date.format(DateTimeFormatter.ISO_DATE)}.csv"
        val tempFile = Files.createTempFile("dw", fileName)
        val bucket = properties.bucket.export
        val prefix = properties.dwExport.prefix
        val key = "$prefix/$fileName"

        try {
            Files.newOutputStream(tempFile).use { fos ->
                BufferedOutputStream(fos).use { bos -> stream.transferTo(bos) }
            }

            logger.info { "Sending DW content for '$queryName' via SFTP" }

            sftpSender.send(tempFile.toFile().readText(Charsets.UTF_8), fileName)

            logger.info { "Sending DW content for '$queryName' to S3" }

            val response =
                s3Client
                    .putObject({ r -> r.bucket(bucket).key(key).contentType("text/csv") }, tempFile)
                    .sdkHttpResponse()

            if (response.isSuccessful) {
                logger.info { "DW file '$key' successfully sent" }
            } else {
                logger.warn {
                    "DW file '$key' sending failed: ${response.statusCode()} ${response.statusText()}"
                }
            }
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

        return bucket to key
    }
}
