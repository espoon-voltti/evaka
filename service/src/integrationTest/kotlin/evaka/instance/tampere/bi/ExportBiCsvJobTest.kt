// SPDX-FileCopyrightText: 2023 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.tampere.bi

import evaka.core.application.ApplicationType
import evaka.core.application.persistence.daycare.Adult
import evaka.core.application.persistence.daycare.Apply
import evaka.core.application.persistence.daycare.CareDetails
import evaka.core.application.persistence.daycare.Child
import evaka.core.application.persistence.daycare.DaycareFormV0
import evaka.core.bi.BiExportConfig
import evaka.core.bi.BiExportJob
import evaka.core.bi.BiTable
import evaka.core.bi.CSV_CHARSET
import evaka.core.invoicing.data.upsertFeeDecisions
import evaka.core.invoicing.data.upsertValueDecisions
import evaka.core.invoicing.domain.ChildWithDateOfBirth
import evaka.core.invoicing.domain.FeeDecision
import evaka.core.invoicing.domain.FeeDecisionChild
import evaka.core.invoicing.domain.FeeDecisionDifference
import evaka.core.invoicing.domain.FeeDecisionPlacement
import evaka.core.invoicing.domain.FeeDecisionServiceNeed
import evaka.core.invoicing.domain.FeeDecisionStatus
import evaka.core.invoicing.domain.FeeDecisionType
import evaka.core.invoicing.domain.FeeThresholds
import evaka.core.invoicing.domain.VoucherValueDecision
import evaka.core.invoicing.domain.VoucherValueDecisionDifference
import evaka.core.invoicing.domain.VoucherValueDecisionPlacement
import evaka.core.invoicing.domain.VoucherValueDecisionServiceNeed
import evaka.core.invoicing.domain.VoucherValueDecisionStatus
import evaka.core.invoicing.domain.VoucherValueDecisionType
import evaka.core.placement.PlacementType
import evaka.core.shared.AreaId
import evaka.core.shared.FeeDecisionId
import evaka.core.shared.ServiceNeedOptionId
import evaka.core.shared.VoucherValueDecisionId
import evaka.core.shared.db.QuerySql
import evaka.core.shared.dev.DevDaycare
import evaka.core.shared.dev.DevDaycareGroup
import evaka.core.shared.dev.DevEmployee
import evaka.core.shared.dev.DevPerson
import evaka.core.shared.dev.DevPersonType
import evaka.core.shared.dev.DevPlacement
import evaka.core.shared.dev.insert
import evaka.core.shared.dev.insertTestApplication
import evaka.core.shared.domain.DateRange
import evaka.core.shared.domain.FiniteDateRange
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.shared.domain.MockEvakaClock
import evaka.core.shared.security.PilotFeature
import evaka.instance.tampere.AbstractTampereIntegrationTest
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import java.time.Month
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.zip.ZipInputStream
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory

class ExportBiCsvJobTest : AbstractTampereIntegrationTest() {
    private val clock =
        MockEvakaClock(HelsinkiDateTime.of(LocalDate.of(2022, 10, 23), LocalTime.of(21, 0)))

    private lateinit var exportJob: BiExportJob

    @BeforeAll
    fun beforeAll() {
        val exportClient = FileBiExportS3Client(s3Client, properties)
        exportJob =
            BiExportJob(
                exportClient,
                BiExportConfig(includePII = true, includeLegacyColumns = true),
            )
    }

    @BeforeEach
    fun beforeEach() {
        insertCriticalTestData()
    }

    @TestFactory
    fun testBiTableExports() =
        BiTable.entries.map {
            DynamicTest.dynamicTest("Test '${it.fileName}' export") { sendAndAssertBiTableCsv(it) }
        }

    private fun sendAndAssertBiTableCsv(table: BiTable) {
        val bucket = properties.bucket.export
        val prefix = properties.biExport.prefix
        val fileName =
            "${table.fileName}_${clock.now().toLocalDate().format(DateTimeFormatter.ISO_DATE)}.zip"
        val key = "$prefix/$fileName"

        exportJob.sendBiTable(db, clock, table.fileName, table.query)

        val (data, name) = getZip(bucket, key)

        assertEquals("${table.fileName}.csv", name)
        assertTrue(data.isNotEmpty())
    }

    private fun getZip(bucket: String, key: String): Pair<String, String> =
        getS3Object(bucket, key).use {
            ZipInputStream(it).use { zip ->
                val entry = zip.nextEntry
                val content = zip.readAllBytes().toString(CSV_CHARSET)
                content to (entry?.name ?: "")
            }
        }

    private fun insertCriticalTestData() {
        db.transaction { tx ->
            tx.insert(DevEmployee())
            tx.insert(DevEmployee()).also { employeeId ->
                tx.createUpdate {
                        sql("UPDATE employee SET last_login = NULL WHERE id = ${bind(employeeId)}")
                    }
                    .updateExactlyOne()
            }

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

            val daycareId =
                tx.insert(
                    DevDaycare(
                        areaId = areaId,
                        enabledPilotFeatures = setOf(PilotFeature.MESSAGING, PilotFeature.MOBILE),
                    )
                )
            tx.insert(DevDaycareGroup(daycareId = daycareId))
            val childId = tx.insert(DevPerson(), DevPersonType.CHILD)
            val guardianId = tx.insert(DevPerson(), DevPersonType.RAW_ROW)
            tx.insert(DevPlacement(childId = childId, unitId = daycareId))

            val testFeeThresholds =
                FeeThresholds(
                    validDuring = DateRange(LocalDate.of(2000, 1, 1), null),
                    maxFee = 28900,
                    minFee = 2700,
                    minIncomeThreshold2 = 210200,
                    minIncomeThreshold3 = 271300,
                    minIncomeThreshold4 = 308000,
                    minIncomeThreshold5 = 344700,
                    minIncomeThreshold6 = 381300,
                    maxIncomeThreshold2 = 479900,
                    maxIncomeThreshold3 = 541000,
                    maxIncomeThreshold4 = 577700,
                    maxIncomeThreshold5 = 614400,
                    maxIncomeThreshold6 = 651000,
                    incomeMultiplier2 = BigDecimal("0.1070"),
                    incomeMultiplier3 = BigDecimal("0.1070"),
                    incomeMultiplier4 = BigDecimal("0.1070"),
                    incomeMultiplier5 = BigDecimal("0.1070"),
                    incomeMultiplier6 = BigDecimal("0.1070"),
                    incomeThresholdIncrease6Plus = 14200,
                    siblingDiscount2 = BigDecimal("0.5000"),
                    siblingDiscount2Plus = BigDecimal("0.8000"),
                    temporaryFee = 2900,
                    temporaryFeePartDay = 1500,
                    temporaryFeeSibling = 1500,
                    temporaryFeeSiblingPartDay = 800,
                )
            tx.insert(testFeeThresholds)

            val applicationId =
                tx.insertTestApplication(
                    type = ApplicationType.DAYCARE,
                    childId = childId,
                    guardianId = guardianId,
                    document =
                        DaycareFormV0(
                            type = ApplicationType.DAYCARE,
                            connectedDaycare = false,
                            urgent = true,
                            careDetails = CareDetails(assistanceNeeded = true),
                            extendedCare = true,
                            child = Child(dateOfBirth = null),
                            guardian = Adult(),
                            apply = Apply(preferredUnits = listOf(daycareId)),
                            preferredStartDate = LocalDate.of(2019, 1, 1),
                        ),
                )

            FeeDecisionId(UUID.randomUUID()).also { id ->
                tx.upsertFeeDecisions(
                    listOf(
                        FeeDecision(
                            id = id,
                            children =
                                listOf(
                                    FeeDecisionChild(
                                        child =
                                            ChildWithDateOfBirth(
                                                id = childId,
                                                dateOfBirth = LocalDate.of(2020, 1, 1),
                                            ),
                                        placement =
                                            FeeDecisionPlacement(
                                                unitId = daycareId,
                                                type = PlacementType.DAYCARE,
                                            ),
                                        serviceNeed =
                                            FeeDecisionServiceNeed(
                                                optionId = snoId,
                                                feeCoefficient = BigDecimal.ONE,
                                                contractDaysPerMonth = null,
                                                descriptionFi = "",
                                                descriptionSv = "",
                                                missing = false,
                                            ),
                                        baseFee = 10_000,
                                        siblingDiscount = 0,
                                        fee = 10_000,
                                        finalFee = 10_000,
                                        feeAlterations = emptyList(),
                                        childIncome = null,
                                    )
                                ),
                            headOfFamilyId = guardianId,
                            validDuring = FiniteDateRange.ofMonth(2019, Month.JANUARY),
                            status = FeeDecisionStatus.SENT,
                            decisionNumber = 999L,
                            decisionType = FeeDecisionType.NORMAL,
                            partnerId = null,
                            headOfFamilyIncome = null,
                            partnerIncome = null,
                            familySize = 1,
                            feeThresholds = testFeeThresholds.getFeeDecisionThresholds(1),
                            difference = setOf(FeeDecisionDifference.PLACEMENT),
                        )
                    )
                )
            }

            VoucherValueDecisionId(UUID.randomUUID()).also { id ->
                tx.upsertValueDecisions(
                    listOf(
                        VoucherValueDecision(
                            id = id,
                            validFrom = LocalDate.of(2022, 1, 1),
                            validTo = LocalDate.of(2022, 2, 1),
                            headOfFamilyId = guardianId,
                            status = VoucherValueDecisionStatus.SENT,
                            decisionNumber = 999L,
                            decisionType = VoucherValueDecisionType.NORMAL,
                            partnerId = null,
                            headOfFamilyIncome = null,
                            partnerIncome = null,
                            childIncome = null,
                            familySize = 1,
                            feeThresholds = testFeeThresholds.getFeeDecisionThresholds(1),
                            child =
                                ChildWithDateOfBirth(
                                    id = childId,
                                    dateOfBirth = LocalDate.of(2020, 1, 1),
                                ),
                            placement =
                                VoucherValueDecisionPlacement(
                                    unitId = daycareId,
                                    type = PlacementType.DAYCARE,
                                ),
                            serviceNeed =
                                VoucherValueDecisionServiceNeed(
                                    feeCoefficient = BigDecimal.ONE,
                                    voucherValueCoefficient = BigDecimal.ONE,
                                    feeDescriptionFi = "",
                                    feeDescriptionSv = "",
                                    voucherValueDescriptionFi = "",
                                    voucherValueDescriptionSv = "",
                                    missing = false,
                                ),
                            baseCoPayment = 1,
                            siblingDiscount = 0,
                            coPayment = 1,
                            feeAlterations = listOf(),
                            finalCoPayment = 1,
                            baseValue = 1,
                            assistanceNeedCoefficient = BigDecimal.ONE,
                            voucherValue = 1,
                            difference = setOf(VoucherValueDecisionDifference.PLACEMENT),
                        )
                    )
                )
            }
        }
    }
}
