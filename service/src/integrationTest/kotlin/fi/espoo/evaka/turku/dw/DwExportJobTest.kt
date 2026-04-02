// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.turku.dw

import com.jcraft.jsch.JSch
import fi.espoo.evaka.BucketEnv
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.Sensitive
import fi.espoo.evaka.absence.AbsenceCategory
import fi.espoo.evaka.shared.dev.DevAbsence
import fi.espoo.evaka.shared.dev.DevAssistanceAction
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevChildAttendance
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.DevDaycareGroupPlacement
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevFeeDecision
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.DevReservation
import fi.espoo.evaka.shared.dev.DevVoucherValueDecision
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.turku.BucketProperties
import fi.espoo.evaka.turku.DwExportProperties
import fi.espoo.evaka.turku.SftpProperties
import fi.espoo.evaka.turku.TurkuEnv
import fi.espoo.evaka.turku.invoice.service.SftpConnector
import fi.espoo.evaka.turku.invoice.service.SftpSender
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
        private const val EXPORT_BUCKET = "evakaturku-export-it"
    }

    @BeforeAll
    fun setup() {
        val turkuEnv =
            TurkuEnv(
                sapInvoicing =
                    SftpProperties(
                        address = "localhost",
                        port = 22,
                        path = "path",
                        username = Sensitive("user"),
                        password = Sensitive("pass"),
                    ),
                sapPayments =
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

        val sftpSender = SftpSender(turkuEnv.dwExport.sftp, SftpConnector(JSch()))
        val exportClient = FileDWExportClient(s3Client, sftpSender, turkuEnv)
        job = DwExportJob(exportClient)
    }

    @BeforeEach
    fun beforeEach() {
        insertCriticalTestData()
    }

    @TestFactory
    fun testDwExports() =
        DwQuery.entries.map {
            DynamicTest.dynamicTest("Test '${it.queryName}' export") { sendAndAssertDwQueryCsv(it) }
        }

    private fun sendAndAssertDwQueryCsv(query: DwQuery) {
        job.sendDwQuery(db, clock, query.queryName, query.query)
    }

    private fun insertCriticalTestData() {
        db.transaction { tx ->
            val employee = DevEmployee()
            tx.insert(employee)
            val area = DevCareArea()
            tx.insert(area)
            val areaId = area.id
            val daycareId = tx.insert(DevDaycare(areaId = areaId))
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
            tx.insert(DevAssistanceAction(childId = childId, modifiedBy = employee.evakaUserId))
            tx.insert(
                DevReservation(
                    childId = childId,
                    date = LocalDate.of(2019, 7, 15),
                    startTime = LocalTime.of(8, 0),
                    endTime = LocalTime.of(16, 0),
                    createdBy = employee.evakaUserId,
                )
            )
            tx.insert(
                DevChildAttendance(
                    childId = childId,
                    unitId = daycareId,
                    date = LocalDate.of(2019, 7, 15),
                    arrived = LocalTime.of(8, 15),
                    departed = LocalTime.of(15, 45),
                    modifiedBy = employee.evakaUserId,
                )
            )
        }
    }
}
