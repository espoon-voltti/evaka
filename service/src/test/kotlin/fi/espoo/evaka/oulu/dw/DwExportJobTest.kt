// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.oulu.dw

import fi.espoo.evaka.absence.AbsenceCategory
import fi.espoo.evaka.oulu.AbstractIntegrationTest
import fi.espoo.evaka.oulu.OuluEnv
import fi.espoo.evaka.oulu.invoice.service.SftpSender
import fi.espoo.evaka.shared.AreaId
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.ServiceNeedOptionId
import fi.espoo.evaka.shared.db.QuerySql
import fi.espoo.evaka.shared.dev.DevAbsence
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
import software.amazon.awssdk.services.s3.S3AsyncClient

class DwExportJobTest : AbstractIntegrationTest() {
    private val clock =
        MockEvakaClock(HelsinkiDateTime.of(LocalDate.of(2019, 7, 15), LocalTime.of(23, 0)))

    @Autowired private lateinit var s3AsyncClient: S3AsyncClient

    @Autowired private lateinit var sftpSender: SftpSender

    @Autowired private lateinit var ouluEnv: OuluEnv

    private lateinit var job: DwExportJob

    @BeforeAll
    fun beforeAll() {
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
            val employeeId = tx.insert(DevEmployee())
            val areaId =
                tx.createQuery(
                        QuerySql { sql("select id from care_area order by short_name limit 1") }
                    )
                    .exactlyOne<AreaId>()
            val snoId =
                tx.createQuery(
                        QuerySql {
                            sql("select id from service_need_option order by name_fi limit 1")
                        }
                    )
                    .exactlyOne<ServiceNeedOptionId>()
            val daycareId = tx.insert(DevDaycare(areaId = areaId))
            val groupId = tx.insert(DevDaycareGroup(daycareId = daycareId))
            val childId = tx.insert(DevPerson(), DevPersonType.CHILD)
            val guardianId = tx.insert(DevPerson(), DevPersonType.RAW_ROW)
            val placementId =
                tx.insert(
                    DevPlacement(
                        childId = childId,
                        unitId = daycareId,
                        createdBy = EvakaUserId(employeeId.raw),
                        modifiedBy = EvakaUserId(employeeId.raw),
                    )
                )
            tx.insert(
                DevDaycareGroupPlacement(daycarePlacementId = placementId, daycareGroupId = groupId)
            )

            tx.insert(
                DevAbsence(
                    childId = childId,
                    date = LocalDate.of(2019, 7, 15),
                    modifiedBy = EvakaUserId(employeeId.raw),
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
