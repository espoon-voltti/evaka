// SPDX-FileCopyrightText: 2023 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.tampere.export

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import evaka.core.shared.DaycareId
import evaka.core.shared.db.Database
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.instance.tampere.TampereProperties
import java.time.format.DateTimeFormatter
import org.springframework.stereotype.Service
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import tools.jackson.dataformat.csv.CsvMapper

@JsonPropertyOrder(
    value = ["unitId", "unitName", "firstName", "lastName", "email", "employeeNumber"]
)
data class UnitAclRow(
    @get:JsonProperty("unit_id") val unitId: DaycareId,
    @get:JsonProperty("unit_name") val unitName: String,
    @get:JsonProperty("first_name") val firstName: String,
    @get:JsonProperty("last_name") val lastName: String,
    @get:JsonProperty("email") val email: String?,
    @get:JsonProperty("employee_number") val employeeNumber: String?,
)

private val TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd")

@Service
class ExportUnitsAclService(
    private val s3Client: S3Client,
    private val properties: TampereProperties,
) {

    private val mapper = CsvMapper()

    fun exportUnitsAcl(tx: Database.Read, timestamp: HelsinkiDateTime): Pair<String, String> {
        val unitsAcl = tx.getUnitAclRows()

        val schema =
            mapper
                .schemaFor(UnitAclRow::class.java)
                .withHeader()
                .withoutQuoteChar()
                .withColumnSeparator(';')
        val csv = mapper.writer(schema).writeValueAsString(unitsAcl)

        val bucket = properties.bucket.export
        val key =
            "reporting/acl/tampere_evaka_acl_${timestamp.toLocalDate().format(TIMESTAMP_FORMATTER)}.csv"
        val request =
            PutObjectRequest.builder().bucket(bucket).key(key).contentType("text/csv").build()
        val body = RequestBody.fromString(csv)
        s3Client.putObject(request, body)

        return bucket to key
    }
}

fun Database.Read.getUnitAclRows() =
    createQuery {
            sql(
                """
SELECT
    d.id AS unit_id,
    d.name AS unit_name,
    e.first_name,
    e.last_name,
    e.email,
    e.employee_number
FROM daycare_acl
JOIN daycare d ON daycare_acl.daycare_id = d.id
JOIN employee e ON daycare_acl.employee_id = e.id
"""
            )
        }
        .toList<UnitAclRow>()
