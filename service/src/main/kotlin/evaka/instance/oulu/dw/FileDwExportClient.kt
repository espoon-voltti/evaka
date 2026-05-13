// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.oulu.dw

import evaka.core.bi.CsvInputStream
import evaka.core.shared.domain.EvakaClock
import evaka.core.shared.sftp.SftpClient
import evaka.instance.oulu.OuluEnv
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.BufferedOutputStream
import java.nio.file.Files
import java.time.format.DateTimeFormatter
import kotlin.io.path.deleteIfExists
import software.amazon.awssdk.services.s3.S3Client

class FileDwExportClient(
    private val s3Client: S3Client,
    private val sftpClient: SftpClient,
    private val ouluEnv: OuluEnv,
) : DwExportClient {
    private val logger = KotlinLogging.logger {}

    override fun sendDwCsvFile(
        queryName: String,
        clock: EvakaClock,
        stream: CsvInputStream,
    ): Pair<String, String> {
        val date = clock.now().toLocalDate()
        val fileName = "$queryName${date.format(DateTimeFormatter.ISO_DATE)}.csv"
        val tempFile = Files.createTempFile("", fileName)
        val bucket = ouluEnv.bucket.export
        val prefix = ouluEnv.dwExport.prefix
        val key = "$prefix/$fileName"

        try {
            Files.newOutputStream(tempFile).use { fos ->
                BufferedOutputStream(fos).use { bos -> stream.transferTo(bos) }
            }

            logger.info { "Sending DW content for '$queryName' via SFTP" }

            tempFile.toFile().inputStream().use { input -> sftpClient.put(input, fileName) }

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
