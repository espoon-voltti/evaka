// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.daycare.CareType
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.serviceneed.ServiceNeedOption
import fi.espoo.evaka.serviceneed.ShiftCareType
import fi.espoo.evaka.shared.AssistanceActionOptionId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.ServiceNeedOptionId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.dev.DevAssistanceAction
import fi.espoo.evaka.shared.dev.DevAssistanceActionOption
import fi.espoo.evaka.shared.dev.DevAssistanceFactor
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevDaycareAssistance
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.DevServiceNeed
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.shared.domain.TimeRange
import fi.espoo.evaka.shared.security.PilotFeature
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class TampereRegionalSurveyTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var tampereRegionalSurvey: TampereRegionalSurvey

    private val admin = DevEmployee(roles = setOf(UserRole.ADMIN))
    private val adminLoginUser = AuthenticatedUser.Employee(admin.id, admin.roles)

    private final val mockClock =
        MockEvakaClock(HelsinkiDateTime.of(LocalDate.of(2025, 1, 1), LocalTime.of(12, 15)))

    private val startDate = LocalDate.of(2024, 9, 15)

    private final val fullTimeSno =
        ServiceNeedOption(
            id = ServiceNeedOptionId(UUID.randomUUID()),
            nameFi = "Kokoaikainen (seutu)",
            nameSv = "Kokoaikainen (seutu)",
            nameEn = "Kokoaikainen (seutu)",
            validPlacementType = PlacementType.DAYCARE,
            defaultOption = false,
            feeCoefficient = BigDecimal("1.00"),
            occupancyCoefficient = BigDecimal("1.00"),
            occupancyCoefficientUnder3y = BigDecimal("1.75"),
            realizedOccupancyCoefficient = BigDecimal("1.00"),
            realizedOccupancyCoefficientUnder3y = BigDecimal("1.75"),
            daycareHoursPerWeek = 40,
            contractDaysPerMonth = null,
            daycareHoursPerMonth = 120,
            partDay = false,
            partWeek = false,
            feeDescriptionFi = "",
            feeDescriptionSv = "",
            voucherValueDescriptionFi = "Kokopäiväinen (seutu)",
            voucherValueDescriptionSv = "Kokopäiväinen (seutu)",
            validFrom = LocalDate.of(2000, 1, 1),
            validTo = null,
        )

    @Test
    fun `Admin can see monthly municipal report results`() {
        val testUnitData = initTestUnitData(startDate)
        initTestPlacementData(startDate, testUnitData[0])
        val results =
            tampereRegionalSurvey.getTampereRegionalSurvey(
                dbInstance(),
                adminLoginUser,
                mockClock,
                year = 2024,
            )

        assertThat(results.monthlyCounts.isNotEmpty())
        assertThat(results.monthlyCounts.isNotEmpty())
    }

    @Test
    fun `Monthly municipal age and part time report results are correct`() {
        val testUnitData = initTestUnitData(startDate)
        initTestPlacementData(startDate, testUnitData[0])
        val results =
            tampereRegionalSurvey.getTampereRegionalSurvey(
                dbInstance(),
                adminLoginUser,
                mockClock,
                year = startDate.year,
            )

        val empties =
            ((1..8) + (11..12)).map { TampereRegionalSurvey.MonthlyMunicipalDaycareResult(it) }
        val expectedResults =
            empties +
                listOf(
                    TampereRegionalSurvey.MonthlyMunicipalDaycareResult(
                        month = 9,
                        municipalUnder3FullTimeCount = 1,
                        municipalUnder3PartTimeCount = 0,
                        municipalOver3FullTimeCount = 1,
                        municipalOver3PartTimeCount = 1,
                    ),
                    TampereRegionalSurvey.MonthlyMunicipalDaycareResult(
                        month = 10,
                        municipalUnder3FullTimeCount = 1,
                        municipalUnder3PartTimeCount = 0,
                        municipalOver3FullTimeCount = 1,
                        municipalOver3PartTimeCount = 1,
                    ),
                )

        val municipalDaycareResults =
            results.monthlyCounts.map {
                TampereRegionalSurvey.MonthlyMunicipalDaycareResult(
                    month = it.month,
                    municipalUnder3FullTimeCount = it.municipalUnder3FullTimeCount,
                    municipalUnder3PartTimeCount = it.municipalUnder3PartTimeCount,
                    municipalOver3FullTimeCount = it.municipalOver3FullTimeCount,
                    municipalOver3PartTimeCount = it.municipalOver3PartTimeCount,
                )
            }

        assertThat(municipalDaycareResults).containsExactlyInAnyOrderElementsOf(expectedResults)
    }

    @Test
    fun `Monthly family daycare report results are correct`() {
        val testUnitData = initTestUnitData(startDate)
        val testChildren = initTestPlacementData(startDate, testUnitData[0])

        // add family daycare placements for 2 children: 1 under and over 3
        db.transaction { tx ->
            tx.insert(
                DevPlacement(
                    startDate = startDate.plusMonths(2),
                    endDate = startDate.plusMonths(4),
                    unitId = testUnitData[2],
                    childId = testChildren[4].first.id,
                    type = PlacementType.DAYCARE,
                )
            )

            val aapoPlacementEnd = testChildren[0].second.last().endDate
            tx.insert(
                DevPlacement(
                    startDate = aapoPlacementEnd.plusDays(1),
                    endDate = aapoPlacementEnd.plusMonths(4),
                    unitId = testUnitData[2],
                    childId = testChildren[0].first.id,
                    type = PlacementType.DAYCARE,
                )
            )
        }

        val results =
            tampereRegionalSurvey.getTampereRegionalSurvey(
                dbInstance(),
                adminLoginUser,
                mockClock,
                year = startDate.year,
            )

        val empties = (1..10).map { TampereRegionalSurvey.MonthlyFamilyDaycareResult(it) }
        val expectedResults =
            empties +
                listOf(
                    TampereRegionalSurvey.MonthlyFamilyDaycareResult(
                        month = 11,
                        familyUnder3Count = 1,
                        familyOver3Count = 1,
                    ),
                    TampereRegionalSurvey.MonthlyFamilyDaycareResult(
                        month = 12,
                        familyUnder3Count = 1,
                        familyOver3Count = 1,
                    ),
                )

        val familyDaycareResults =
            results.monthlyCounts.map {
                TampereRegionalSurvey.MonthlyFamilyDaycareResult(
                    month = it.month,
                    familyUnder3Count = it.familyUnder3Count,
                    familyOver3Count = it.familyOver3Count,
                )
            }

        assertThat(familyDaycareResults).containsExactlyInAnyOrderElementsOf(expectedResults)
    }

    @Test
    fun `Monthly shift care report results are correct`() {
        val testUnitData = initTestUnitData(startDate)
        val testChildren = initTestPlacementData(startDate, testUnitData[0])

        val aapoPlacement = testChildren[0].second.first()

        // add shift care placement for Aapo in October
        db.transaction { tx ->
            tx.insert(
                DevServiceNeed(
                    startDate = aapoPlacement.startDate.plusMonths(1),
                    endDate = aapoPlacement.endDate,
                    placementId = testChildren[0].second.first().id,
                    shiftCare = ShiftCareType.FULL,
                    optionId = fullTimeSno.id,
                    confirmedBy = admin.evakaUserId,
                )
            )
        }

        val results =
            tampereRegionalSurvey.getTampereRegionalSurvey(
                dbInstance(),
                adminLoginUser,
                mockClock,
                year = startDate.year,
            )

        val empties =
            ((1..8) + (11..12)).map { TampereRegionalSurvey.MonthlyMunicipalShiftCareResult(it) }
        val expectedResults =
            empties +
                listOf(
                    TampereRegionalSurvey.MonthlyMunicipalShiftCareResult(
                        month = 9,
                        municipalShiftCareCount = 1,
                    ),
                    TampereRegionalSurvey.MonthlyMunicipalShiftCareResult(
                        month = 10,
                        municipalShiftCareCount = 2,
                    ),
                )

        val shiftCareResults =
            results.monthlyCounts.map {
                TampereRegionalSurvey.MonthlyMunicipalShiftCareResult(
                    month = it.month,
                    municipalShiftCareCount = it.municipalShiftCareCount,
                )
            }

        assertThat(shiftCareResults).containsExactlyInAnyOrderElementsOf(expectedResults)
    }

    @Test
    fun `Monthly assistance report results are correct`() {
        val testUnitData = initTestUnitData(startDate)
        initTestPlacementData(startDate, testUnitData[0])

        val results =
            tampereRegionalSurvey.getTampereRegionalSurvey(
                dbInstance(),
                adminLoginUser,
                mockClock,
                year = startDate.year,
            )

        val empties = ((1..8) + (11..12)).map { TampereRegionalSurvey.MonthlyAssistanceResult(it) }
        val expectedResults =
            empties +
                listOf(
                    TampereRegionalSurvey.MonthlyAssistanceResult(month = 9, assistanceCount = 2),
                    TampereRegionalSurvey.MonthlyAssistanceResult(month = 10, assistanceCount = 2),
                )

        val assistanceResults =
            results.monthlyCounts.map {
                TampereRegionalSurvey.MonthlyAssistanceResult(
                    month = it.month,
                    assistanceCount = it.assistanceCount,
                )
            }

        assertThat(assistanceResults).containsExactlyInAnyOrderElementsOf(expectedResults)
    }

    private fun initTestUnitData(monday: LocalDate): List<DaycareId> {
        return db.transaction { tx ->
            val areaAId = tx.insert(DevCareArea(name = "Area A", shortName = "Area A"))
            val areaBId = tx.insert(DevCareArea(name = "Area B", shortName = "Area B"))

            val daycareAId =
                tx.insert(
                    DevDaycare(
                        name = "Daycare A",
                        areaId = areaAId,
                        openingDate = monday.minusDays(7),
                        type = setOf(CareType.CENTRE, CareType.PRESCHOOL),
                        operationTimes =
                            List(5) { TimeRange(LocalTime.of(8, 0), LocalTime.of(18, 0)) } +
                                List(2) { null },
                        enabledPilotFeatures = setOf(PilotFeature.RESERVATIONS),
                    )
                )

            val daycareBId =
                tx.insert(
                    DevDaycare(
                        name = "Daycare B",
                        areaId = areaBId,
                        openingDate = monday.minusDays(7),
                        type = setOf(CareType.CENTRE),
                        operationTimes =
                            List(5) { TimeRange(LocalTime.of(8, 0), LocalTime.of(18, 0)) } +
                                List(2) { null },
                        shiftCareOperationTimes =
                            List(7) { TimeRange(LocalTime.of(0, 0), LocalTime.of(23, 59)) },
                        shiftCareOpenOnHolidays = true,
                        enabledPilotFeatures = setOf(PilotFeature.RESERVATIONS),
                    )
                )

            val daycareCId =
                tx.insert(
                    DevDaycare(
                        name = "Family daycare",
                        areaId = areaAId,
                        openingDate = monday.minusMonths(1),
                        type = setOf(CareType.FAMILY),
                        operationTimes =
                            List(5) { TimeRange(LocalTime.of(8, 0), LocalTime.of(18, 0)) } +
                                List(2) { null },
                        enabledPilotFeatures = setOf(PilotFeature.RESERVATIONS),
                    )
                )

            listOf(daycareAId, daycareBId, daycareCId)
        }
    }

    private fun initTestPlacementData(
        start: LocalDate,
        daycareId: DaycareId,
    ): List<Pair<DevPerson, List<DevPlacement>>> {

        val actionOption10 =
            DevAssistanceActionOption(
                value = "10",
                nameFi = "Avustajapalvelut",
                descriptionFi = "Avustajapalvelut",
                id = AssistanceActionOptionId(UUID.randomUUID()),
            )

        val actionOption40 =
            DevAssistanceActionOption(
                value = "40",
                nameFi = "Henkilökuntalisäys tai -muutos",
                descriptionFi = "Henkilökuntalisäys tai -muutos",
                id = AssistanceActionOptionId(UUID.randomUUID()),
            )

        val partTimeSno =
            ServiceNeedOption(
                id = ServiceNeedOptionId(UUID.randomUUID()),
                nameFi = "Osa-aikainen (seutu)",
                nameSv = "Osa-aikainen (seutu)",
                nameEn = "Osa-aikainen (seutu)",
                validPlacementType = PlacementType.DAYCARE,
                defaultOption = false,
                feeCoefficient = BigDecimal("1.00"),
                occupancyCoefficient = BigDecimal("1.00"),
                occupancyCoefficientUnder3y = BigDecimal("1.75"),
                realizedOccupancyCoefficient = BigDecimal("1.00"),
                realizedOccupancyCoefficientUnder3y = BigDecimal("1.75"),
                daycareHoursPerWeek = 40,
                contractDaysPerMonth = null,
                daycareHoursPerMonth = 85,
                partDay = true,
                partWeek = true,
                feeDescriptionFi = "",
                feeDescriptionSv = "",
                voucherValueDescriptionFi = "Osa-aikainen (seutu)",
                voucherValueDescriptionSv = "Osa-aikainen (seutu)",
                validFrom = LocalDate.of(2000, 1, 1),
                validTo = null,
            )

        val dailyPartTimeSno =
            ServiceNeedOption(
                id = ServiceNeedOptionId(UUID.randomUUID()),
                nameFi = "Kokoaikainen (seutu)",
                nameSv = "Kokoaikainen (seutu)",
                nameEn = "Kokoaikainen (seutu)",
                validPlacementType = PlacementType.DAYCARE,
                defaultOption = false,
                feeCoefficient = BigDecimal("1.00"),
                occupancyCoefficient = BigDecimal("1.00"),
                occupancyCoefficientUnder3y = BigDecimal("1.75"),
                realizedOccupancyCoefficient = BigDecimal("1.00"),
                realizedOccupancyCoefficientUnder3y = BigDecimal("1.75"),
                daycareHoursPerWeek = 40,
                contractDaysPerMonth = 10,
                daycareHoursPerMonth = null,
                partDay = false,
                partWeek = false,
                feeDescriptionFi = "",
                feeDescriptionSv = "",
                voucherValueDescriptionFi = "Kokopäiväinen (seutu)",
                voucherValueDescriptionSv = "Kokopäiväinen (seutu)",
                validFrom = LocalDate.of(2000, 1, 1),
                validTo = null,
            )

        val default =
            fullTimeSno.copy(id = ServiceNeedOptionId(UUID.randomUUID()), defaultOption = true)

        return db.transaction { tx ->
            listOf(partTimeSno, dailyPartTimeSno, fullTimeSno, default).forEach { tx.insert(it) }

            tx.insert(actionOption10)
            tx.insert(actionOption40)

            tx.insert(admin)

            val testChildAapo =
                DevPerson(
                    dateOfBirth = start.minusYears(2),
                    firstName = "Aapo",
                    lastName = "Aarnio",
                )
            tx.insert(testChildAapo, DevPersonType.CHILD)

            val testChildBertil =
                DevPerson(
                    dateOfBirth = start.minusYears(4),
                    firstName = "Bertil",
                    lastName = "Becker",
                )
            tx.insert(testChildBertil, DevPersonType.CHILD)

            val testChildCecil =
                DevPerson(
                    dateOfBirth = start.minusYears(4),
                    firstName = "Cecil",
                    lastName = "Cilliacus",
                )
            tx.insert(testChildCecil, DevPersonType.CHILD)

            val testChildVille =
                DevPerson(
                    dateOfBirth = start.minusYears(4),
                    firstName = "Ville",
                    lastName = "Varahoidettava",
                )

            tx.insert(testChildVille, DevPersonType.CHILD)

            val testChildFabio =
                DevPerson(
                    dateOfBirth = start.minusYears(4),
                    firstName = "Fabio",
                    lastName = "Familycare",
                )

            tx.insert(testChildFabio, DevPersonType.CHILD)

            // Aapo:   2v, no sn, DAYCARE placement, 09-15 -> 11-15
            // Bertil: 4v, 2x part time sn, DAYCARE placement, 09-15 -> 11-15
            // Cecil:  4v, no sn, 2x DAYCARE placement, 09-15 -> 11-15
            val defaultPlacementDuration = FiniteDateRange(start, start.plusMonths(2))

            val placementA =
                DevPlacement(
                    type = PlacementType.DAYCARE,
                    childId = testChildAapo.id,
                    unitId = daycareId,
                    startDate = defaultPlacementDuration.start,
                    endDate = defaultPlacementDuration.end,
                )

            tx.insert(placementA)

            val placementB =
                DevPlacement(
                    type = PlacementType.PRESCHOOL_DAYCARE_ONLY,
                    childId = testChildBertil.id,
                    unitId = daycareId,
                    startDate = defaultPlacementDuration.start,
                    endDate = defaultPlacementDuration.end,
                )
            tx.insert(placementB)

            tx.insert(
                DevServiceNeed(
                    placementId = placementB.id,
                    startDate = defaultPlacementDuration.start,
                    endDate = defaultPlacementDuration.end.minusMonths(1),
                    confirmedBy = admin.evakaUserId,
                    optionId = partTimeSno.id,
                    shiftCare = ShiftCareType.FULL,
                )
            )
            tx.insert(
                DevServiceNeed(
                    placementId = placementB.id,
                    startDate = defaultPlacementDuration.end.minusMonths(1).plusDays(1),
                    endDate = defaultPlacementDuration.end,
                    confirmedBy = admin.evakaUserId,
                    optionId = dailyPartTimeSno.id,
                    shiftCare = ShiftCareType.INTERMITTENT,
                )
            )

            tx.insert(
                DevAssistanceAction(
                    childId = testChildBertil.id,
                    startDate = defaultPlacementDuration.start,
                    endDate = defaultPlacementDuration.end,
                    actions = setOf(actionOption40.value),
                )
            )

            val placementC1 =
                DevPlacement(
                    type = PlacementType.DAYCARE,
                    childId = testChildCecil.id,
                    unitId = daycareId,
                    startDate = defaultPlacementDuration.start,
                    endDate = start.plusMonths(1),
                )
            tx.insert(placementC1)

            val placementC2 =
                DevPlacement(
                    type = PlacementType.DAYCARE,
                    childId = testChildCecil.id,
                    unitId = daycareId,
                    startDate = start.plusMonths(1).plusDays(1),
                    endDate = defaultPlacementDuration.end,
                )

            tx.insert(placementC2)

            tx.insert(
                DevAssistanceFactor(
                    childId = testChildCecil.id,
                    validDuring =
                        FiniteDateRange(
                            defaultPlacementDuration.start,
                            defaultPlacementDuration.start.plusMonths(1),
                        ),
                    capacityFactor = 5.50,
                )
            )

            val action1 =
                DevAssistanceAction(
                    childId = testChildCecil.id,
                    startDate = defaultPlacementDuration.start.plusMonths(1).plusDays(1),
                    endDate = defaultPlacementDuration.end,
                    actions = setOf(actionOption10.value),
                )

            tx.insert(action1)

            tx.insert(
                DevDaycareAssistance(
                    childId = testChildCecil.id,
                    validDuring =
                        FiniteDateRange(
                            defaultPlacementDuration.start,
                            defaultPlacementDuration.end,
                        ),
                )
            )

            listOf(
                Pair(testChildAapo, listOf(placementA)),
                Pair(testChildBertil, listOf(placementB)),
                Pair(testChildCecil, listOf(placementC1, placementC2)),
                Pair(testChildVille, emptyList()),
                Pair(testChildFabio, emptyList()),
            )
        }
    }
}
