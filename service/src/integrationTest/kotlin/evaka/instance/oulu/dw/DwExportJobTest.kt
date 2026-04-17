// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.oulu.dw

import com.jcraft.jsch.JSch
import evaka.core.BucketEnv
import evaka.core.FullApplicationTest
import evaka.core.Sensitive
import evaka.core.absence.AbsenceCategory
import evaka.core.shared.dev.DevAbsence
import evaka.core.shared.dev.DevCareArea
import evaka.core.shared.dev.DevDaycare
import evaka.core.shared.dev.DevDaycareGroup
import evaka.core.shared.dev.DevDaycareGroupPlacement
import evaka.core.shared.dev.DevEmployee
import evaka.core.shared.dev.DevFeeDecision
import evaka.core.shared.dev.DevPerson
import evaka.core.shared.dev.DevPersonType
import evaka.core.shared.dev.DevPlacement
import evaka.core.shared.dev.DevVoucherValueDecision
import evaka.core.shared.dev.insert
import evaka.core.shared.domain.FiniteDateRange
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.shared.domain.MockEvakaClock
import evaka.instance.oulu.BucketProperties
import evaka.instance.oulu.DwExportProperties
import evaka.instance.oulu.OuluEnv
import evaka.instance.oulu.SftpProperties
import evaka.instance.oulu.invoice.service.SftpConnector
import evaka.instance.oulu.invoice.service.SftpSender
import java.time.LocalDate
import java.time.LocalTime
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import org.springframework.beans.factory.annotation.Autowired
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

        val sftpSender = SftpSender(ouluEnv.dwExport.sftp, SftpConnector(JSch()))
        val exportClient = FileDwExportClient(s3Client, sftpSender, ouluEnv)
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
