// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.oulu.dw

import com.jcraft.jsch.JSch
import fi.espoo.evaka.BucketEnv
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.Sensitive
import fi.espoo.evaka.absence.AbsenceCategory
import fi.espoo.evaka.oulu.BucketProperties
import fi.espoo.evaka.oulu.DwExportProperties
import fi.espoo.evaka.oulu.OuluEnv
import fi.espoo.evaka.oulu.SftpProperties
import fi.espoo.evaka.oulu.invoice.service.SftpConnector
import fi.espoo.evaka.oulu.invoice.service.SftpSender
import fi.espoo.evaka.shared.dev.DevAbsence
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.DevDaycareGroupPlacement
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevFeeDecision
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.DevVoucherValueDecision
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import java.time.LocalDate
import java.time.LocalTime
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import org.springframework.beans.factory.annotation.Autowired
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.CreateBucketRequest

class DwExportJobTest : FullApplicationTest(resetDbBeforeEach = true) {
    private val clock =
        MockEvakaClock(HelsinkiDateTime.of(LocalDate.of(2019, 7, 15), LocalTime.of(23, 0)))

    @Autowired private lateinit var bucketEnv: BucketEnv
    @Autowired private lateinit var s3Client: S3Client

    private lateinit var job: DwExportJob

    companion object {
        private val sftpPort = System.getenv("EVAKA_SFTP_PORT")?.toIntOrNull() ?: 2222
        private const val EXPORT_BUCKET = "evakaoulu-export-it"
    }

    @BeforeAll
    fun setup() {
        val ouluEnv =
            OuluEnv(
                intimeInvoices =
                    SftpProperties(
                        address = "localhost",
                        port = 22,
                        path = "path",
                        username = Sensitive("user"),
                        password = Sensitive("pass"),
                    ),
                intimePayments =
                    SftpProperties(
                        address = "localhost",
                        port = 22,
                        path = "path",
                        username = Sensitive("user"),
                        password = Sensitive("pass"),
                    ),
                bucket = BucketProperties(export = EXPORT_BUCKET),
                dwExport =
                    DwExportProperties(
                        prefix = "reports",
                        sftp =
                            SftpProperties(
                                address = "localhost",
                                port = sftpPort,
                                path = "upload",
                                username = Sensitive("foo"),
                                password = Sensitive("pass"),
                            ),
                    ),
            )

        val existingBuckets = s3Client.listBuckets().buckets().map { it.name()!! }
        if (EXPORT_BUCKET !in existingBuckets) {
            s3Client.createBucket(CreateBucketRequest.builder().bucket(EXPORT_BUCKET).build())
        }

        val s3AsyncClient =
            S3AsyncClient.crtBuilder()
                .httpConfiguration { it.trustAllCertificatesEnabled(true) }
                .region(Region.EU_WEST_1)
                .forcePathStyle(true)
                .endpointOverride(bucketEnv.localS3Url)
                .credentialsProvider(
                    StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(
                            bucketEnv.localS3AccessKeyId,
                            bucketEnv.localS3SecretAccessKey,
                        )
                    )
                )
                .build()

        val sftpSender = SftpSender(ouluEnv.dwExport.sftp, SftpConnector(JSch()))
        val exportClient = FileDwExportClient(s3AsyncClient, sftpSender, ouluEnv)
        job = DwExportJob(exportClient)
    }

    @BeforeEach
    fun beforeEach() {
        insertCriticalTestData()
    }

    @TestFactory
    fun testDwExports() =
        DwQuery.entries.map {
            DynamicTest.dynamicTest("Test DW '${it.queryName}' export") {
                sendAndAssertQueryCsv(it.queryName, it.query)
            }
        }

    @TestFactory
    fun testFabricExports() =
        FabricQuery.entries.map {
            DynamicTest.dynamicTest("Test Fabric '${it.queryName}' export") {
                sendAndAssertQueryCsv(it.queryName, it.query)
            }
        }

    @TestFactory
    fun testFabricHistoryExports() =
        FabricHistoryQuery.entries.map {
            DynamicTest.dynamicTest("Test Fabric History '${it.queryName}' export") {
                sendAndAssertQueryCsv(it.queryName, it.query)
            }
        }

    private fun sendAndAssertQueryCsv(name: String, query: CsvQuery) {
        job.sendQuery(db, clock, name, query, "test")
    }

    private fun insertCriticalTestData() {
        db.transaction { tx ->
            val employee = DevEmployee()
            tx.insert(employee)
            val area = DevCareArea()
            tx.insert(area)
            val daycareId = tx.insert(DevDaycare(areaId = area.id))
            val groupId = tx.insert(DevDaycareGroup(daycareId = daycareId))
            val childId = tx.insert(DevPerson(), DevPersonType.CHILD)
            val guardianId = tx.insert(DevPerson(), DevPersonType.RAW_ROW)
            val placementId =
                tx.insert(
                    DevPlacement(
                        childId = childId,
                        unitId = daycareId,
                        createdBy = employee.evakaUserId,
                        modifiedBy = employee.evakaUserId,
                    )
                )
            tx.insert(
                DevDaycareGroupPlacement(daycarePlacementId = placementId, daycareGroupId = groupId)
            )
            tx.insert(
                DevAbsence(
                    childId = childId,
                    date = LocalDate.of(2019, 7, 15),
                    modifiedBy = employee.evakaUserId,
                    absenceCategory = AbsenceCategory.BILLABLE,
                )
            )
            tx.insert(
                DevVoucherValueDecision(
                    childId = childId,
                    headOfFamilyId = guardianId,
                    placementUnitId = daycareId,
                    validFrom = LocalDate.of(2019, 1, 1),
                    validTo = LocalDate.of(2019, 12, 31),
                )
            )
            tx.insert(
                DevFeeDecision(
                    validDuring =
                        FiniteDateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 31)),
                    headOfFamilyId = guardianId,
                )
            )
        }
    }
}
