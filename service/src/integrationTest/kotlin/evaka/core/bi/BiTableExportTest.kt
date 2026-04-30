// SPDX-FileCopyrightText: 2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.bi

import evaka.core.PureJdbiTest
import evaka.core.application.ApplicationType
import evaka.core.application.persistence.daycare.Adult
import evaka.core.application.persistence.daycare.Apply
import evaka.core.application.persistence.daycare.CareDetails
import evaka.core.application.persistence.daycare.Child
import evaka.core.application.persistence.daycare.DaycareFormV0
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
import evaka.core.shared.FeeDecisionId
import evaka.core.shared.ServiceNeedOptionId
import evaka.core.shared.VoucherValueDecisionId
import evaka.core.shared.dev.DevCareArea
import evaka.core.shared.dev.DevDaycare
import evaka.core.shared.dev.DevDaycareGroup
import evaka.core.shared.dev.DevEmployee
import evaka.core.shared.dev.DevPerson
import evaka.core.shared.dev.DevPersonType
import evaka.core.shared.dev.DevPlacement
import evaka.core.shared.dev.insert
import evaka.core.shared.dev.insertServiceNeedOptions
import evaka.core.shared.dev.insertTestApplication
import evaka.core.shared.domain.DateRange
import evaka.core.shared.domain.EvakaClock
import evaka.core.shared.domain.FiniteDateRange
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.shared.domain.MockEvakaClock
import evaka.core.shared.security.PilotFeature
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import java.time.Month
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory

class BiTableExportTest : PureJdbiTest(resetDbBeforeEach = false) {
    private val clock =
        MockEvakaClock(HelsinkiDateTime.of(LocalDate.of(2022, 10, 23), LocalTime.of(21, 0)))

    @BeforeAll
    override fun beforeAll() {
        super.beforeAll()
        insertCriticalTestData()
    }

    @TestFactory
    fun `each BiTable exports a non-empty CSV with full export config`() =
        exportTests(
            BiExportConfig(includePII = true, includeLegacyColumns = true, deltaWindowDays = 60)
        )

    @TestFactory
    fun `each BiTable exports a non-empty CSV with PII and legacy columns stripped`() =
        exportTests(
            BiExportConfig(includePII = false, includeLegacyColumns = false, deltaWindowDays = 60)
        )

    private fun exportTests(config: BiExportConfig) =
        BiTable.entries.map { table ->
            DynamicTest.dynamicTest("${table.name} export") {
                val client = CapturingBiExportClient()
                BiExportJob(client, config).sendBiTable(db, clock, table)
                assertEquals(listOf(table.fileName), client.captured.keys.toList())
                val csv = client.captured.getValue(table.fileName).toString(CSV_CHARSET)
                assertTrue(
                    csv.lineSequence().first().isNotBlank(),
                    "${table.fileName} produced an empty CSV header",
                )
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

            val areaId = tx.insert(DevCareArea())
            tx.insertServiceNeedOptions()
            val snoId =
                tx.createQuery {
                        sql("SELECT id FROM service_need_option ORDER BY name_fi LIMIT 1")
                    }
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

            tx.upsertFeeDecisions(
                listOf(
                    FeeDecision(
                        id = FeeDecisionId(UUID.randomUUID()),
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

            tx.upsertValueDecisions(
                listOf(
                    VoucherValueDecision(
                        id = VoucherValueDecisionId(UUID.randomUUID()),
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

private class CapturingBiExportClient : BiExportClient {
    val captured = linkedMapOf<String, ByteArray>()

    override fun sendBiCsvFile(
        tableName: String,
        clock: EvakaClock,
        stream: CsvInputStream,
    ): Pair<String, String> {
        captured[tableName] = stream.readAllBytes()
        return "fake-bucket" to tableName
    }
}
