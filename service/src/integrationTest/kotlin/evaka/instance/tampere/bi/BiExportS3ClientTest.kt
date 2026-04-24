// SPDX-FileCopyrightText: 2023 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.tampere.bi

import evaka.core.bi.BiExportConfig
import evaka.core.bi.BiExportJob
import evaka.core.bi.BiTable
import evaka.core.bi.CSV_CHARSET
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.shared.domain.MockEvakaClock
import evaka.instance.tampere.AbstractTampereIntegrationTest
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.zip.ZipInputStream
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test

/**
 * Verifies the Tampere-specific S3 transport for BI exports: CSV is wrapped in a zip and uploaded
 * to S3 with a date-suffixed key.
 */
class BiExportS3ClientTest : AbstractTampereIntegrationTest() {
    private val clock =
        MockEvakaClock(HelsinkiDateTime.of(LocalDate.of(2022, 10, 23), LocalTime.of(21, 0)))

    @Test
    fun `zips CSV and uploads to S3 with date-suffixed key`() {
        val table = BiTable.CareArea
        val client = FileBiExportS3Client(s3Client, properties)
        val job =
            BiExportJob(client, BiExportConfig(includePII = true, includeLegacyColumns = true))

        job.sendBiTable(db, clock, table.fileName, table.query)

        val bucket = properties.bucket.export
        val prefix = properties.biExport.prefix
        val fileName =
            "${table.fileName}_${clock.now().toLocalDate().format(DateTimeFormatter.ISO_DATE)}.zip"
        val key = "$prefix/$fileName"

        getS3Object(bucket, key).use {
            ZipInputStream(it).use { zip ->
                val entry = zip.nextEntry
                val content = zip.readAllBytes().toString(CSV_CHARSET)
                assertEquals("${table.fileName}.csv", entry?.name)
                assertTrue(content.isNotEmpty())
            }
        }
    }
}
