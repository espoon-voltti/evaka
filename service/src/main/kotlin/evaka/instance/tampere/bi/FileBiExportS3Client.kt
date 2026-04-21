// SPDX-FileCopyrightText: 2024 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.tampere.bi

import evaka.core.bi.BiExportClient
import evaka.core.bi.CsvInputStream
import evaka.core.shared.domain.EvakaClock
import evaka.instance.tampere.TampereProperties
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.BufferedOutputStream
import java.nio.file.Files
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.deleteIfExists
import software.amazon.awssdk.services.s3.S3Client

class FileBiExportS3Client(
    private val s3Client: S3Client,
    private val properties: TampereProperties,
) : BiExportClient {
    private val logger = KotlinLogging.logger {}

    override fun sendBiCsvFile(
        tableName: String,
        clock: EvakaClock,
        stream: CsvInputStream,
    ): Pair<String, String> {
        val date = clock.now().toLocalDate()
        val entryName = "$tableName.csv"
        logger.info { "Sending BI content for '$tableName'" }
        val bucket = properties.bucket.export
        val prefix = properties.biExport.prefix
        val fileName = "${tableName}_${date.format(DateTimeFormatter.ISO_DATE)}.zip"
        val key = "$prefix/$fileName"

        val tempFile = Files.createTempFile("bi", fileName)
        try {
            Files.newOutputStream(tempFile).use { fos ->
                BufferedOutputStream(fos).use { bos ->
                    ZipOutputStream(bos).use { zip ->
                        zip.putNextEntry(ZipEntry(entryName))
                        stream.transferTo(zip)
                        zip.closeEntry()
                    }
                }
            }

            val response =
                s3Client
                    .putObject(
                        { r -> r.bucket(bucket).key(key).contentType("application/zip") },
                        tempFile,
                    )
                    .sdkHttpResponse()

            if (response.isSuccessful) {
                logger.info { "BI file '$key' successfully sent" }
            } else {
                logger.warn {
                    "BI file '$key' sending failed: ${response.statusCode()} ${response.statusText()}"
                }
            }
        } finally {
            val wasDeleted = tempFile.deleteIfExists()
            if (!wasDeleted) {
                logger.warn {
                    "BI temporary file clean up for '${tempFile.fileName}' did not find anything to delete"
                }
            }
        }

        return bucket to key
    }
}
