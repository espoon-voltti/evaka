// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.absence.AbsenceCategory
import fi.espoo.evaka.assistance.DaycareAssistanceLevel
import fi.espoo.evaka.daycare.CareType
import fi.espoo.evaka.daycare.domain.ProviderType
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.serviceneed.ServiceNeedOption
import fi.espoo.evaka.serviceneed.ShiftCareType
import fi.espoo.evaka.shared.AssistanceActionOptionId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.PlacementId
import fi.espoo.evaka.shared.ServiceNeedId
import fi.espoo.evaka.shared.ServiceNeedOptionId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.dev.DevAbsence
import fi.espoo.evaka.shared.dev.DevAssistanceAction
import fi.espoo.evaka.shared.dev.DevAssistanceActionOption
import fi.espoo.evaka.shared.dev.DevAssistanceFactor
import fi.espoo.evaka.shared.dev.DevBackupCare
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
import kotlin.test.assertEquals
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

    val actionOption40 =
        DevAssistanceActionOption(
            value = "40",
            nameFi = "Henkilökuntalisäys tai -muutos",
            descriptionFi = "Henkilökuntalisäys tai -muutos",
            id = AssistanceActionOptionId(UUID.randomUUID()),
        )

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
            showForCitizen = true,
        )

    @Test
    fun `Admin can see monthly municipal report results`() {
        val testUnitData = initTestUnitData(startDate)
        initTestPlacementData(startDate, testUnitData[0])
        val results =
            tampereRegionalSurvey.getTampereRegionalSurveyMonthlyStatistics(
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
        val testChildData = initTestPlacementData(startDate, testUnitData[0])

        db.transaction { tx ->
            // add a family care placement that should not show up
            tx.insert(
                DevPlacement(
                    childId = testChildData[4].first.id,
                    unitId = testUnitData[2],
                    startDate = startDate,
                    endDate = startDate.plusYears(5),
                )
            )
        }

        val results =
            tampereRegionalSurvey.getTampereRegionalSurveyMonthlyStatistics(
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
    fun `Monthly municipal family daycare report results are correct`() {
        val testUnitData = initTestUnitData(startDate)
        val testChildren = initTestPlacementData(startDate, testUnitData[0])

        val testChildPauliina =
            DevPerson(
                firstName = "Pauliina",
                lastName = "Palveluseteli",
                ssn = null,
                dateOfBirth = startDate.minusYears(3),
            )

        val voucherArea = DevCareArea(name = "Area Voucher", shortName = "Area Voucher")
        val voucherFamilyUnit =
            DevDaycare(
                name = "Voucher family daycare",
                openingDate = startDate.minusYears(1),
                areaId = voucherArea.id,
                type = setOf(CareType.FAMILY),
                providerType = ProviderType.PRIVATE_SERVICE_VOUCHER,
            )

        db.transaction { tx ->
            // add a non-municipal placement that should not show up
            tx.insert(voucherArea)
            tx.insert(voucherFamilyUnit)

            tx.insert(testChildPauliina, DevPersonType.CHILD)
            tx.insert(
                DevPlacement(
                    startDate = startDate.plusMonths(2),
                    endDate = startDate.plusMonths(4),
                    unitId = voucherFamilyUnit.id,
                    childId = testChildPauliina.id,
                    type = PlacementType.DAYCARE,
                )
            )

            // add family daycare placements for 2 children: 1 under and over 3
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
            tampereRegionalSurvey.getTampereRegionalSurveyMonthlyStatistics(
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
            tampereRegionalSurvey.getTampereRegionalSurveyMonthlyStatistics(
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
            tampereRegionalSurvey.getTampereRegionalSurveyMonthlyStatistics(
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

    @Test
    fun `Private service voucher age distribution results are correct `() {
        val testUnitData = initTestUnitData(startDate)
        initTestPlacementData(
            startDate,
            testUnitData[3],
            FiniteDateRange(startDate, startDate.plusYears(1)),
        )

        // add some non-compliant data that should not show up
        val newArea = DevCareArea(name = "Testcarearea", shortName = "tca")
        val voucherSchool =
            DevDaycare(
                name = "Palsekoulu",
                type = setOf(CareType.PRESCHOOL),
                providerType = ProviderType.PRIVATE_SERVICE_VOUCHER,
                openingDate = startDate.minusYears(3),
                areaId = newArea.id,
            )

        val testChildTyyrikki =
            DevPerson(
                firstName = "Tyyrikki",
                lastName = "Täydentävässäoleva",
                dateOfBirth = startDate.minusYears(2),
            )

        db.transaction { tx ->
            tx.insert(newArea)
            tx.insert(voucherSchool)
            tx.insert(testChildTyyrikki, DevPersonType.CHILD)
            tx.insert(
                DevPlacement(
                    childId = testChildTyyrikki.id,
                    unitId = voucherSchool.id,
                    type = PlacementType.PRESCHOOL_DAYCARE,
                    startDate = startDate,
                    endDate = startDate.plusYears(2),
                )
            )

            val testChildCecilia =
                DevPerson(
                    dateOfBirth = startDate.minusYears(2),
                    firstName = "Cecilia",
                    lastName = "af Clubbenberg",
                    language = "sv",
                )

            val testChildVeikko =
                DevPerson(
                    dateOfBirth = startDate.minusYears(4),
                    firstName = "Veikko",
                    lastName = "Vakalapsi",
                    language = "fi",
                )

            tx.insert(testChildCecilia, DevPersonType.CHILD)
            tx.insert(testChildVeikko, DevPersonType.CHILD)

            val placementC =
                DevPlacement(
                    type = PlacementType.CLUB,
                    childId = testChildCecilia.id,
                    unitId = testUnitData[1],
                    startDate = startDate,
                    endDate = startDate.plusYears(1),
                )

            val placementV =
                DevPlacement(
                    type = PlacementType.DAYCARE,
                    childId = testChildVeikko.id,
                    unitId = testUnitData[0],
                    startDate = startDate,
                    endDate = startDate.plusYears(1),
                )

            tx.insert(placementC)
            tx.insert(placementV)
        }

        val results =
            tampereRegionalSurvey.getTampereRegionalSurveyAgeStatistics(
                dbInstance(),
                adminLoginUser,
                mockClock,
                year = startDate.year,
            )

        val expectedResults = Pair(1, 2)

        val assistanceResults =
            Pair(
                results.ageStatistics.first().voucherUnder3Count,
                results.ageStatistics.first().voucherOver3Count,
            )

        assertEquals(expectedResults, assistanceResults)
    }

    @Test
    fun `Purchased age distribution results are correct`() {
        val testUnitData = initTestUnitData(startDate)
        initTestPlacementData(
            startDate,
            testUnitData[4],
            FiniteDateRange(startDate, startDate.plusYears(1)),
        )

        // add some non-compliant data that should not show up
        val newArea = DevCareArea(name = "Testcarearea", shortName = "tca")
        val purchasedSchool =
            DevDaycare(
                name = "Ostokoulu",
                type = setOf(CareType.PRESCHOOL),
                providerType = ProviderType.PURCHASED,
                openingDate = startDate.minusYears(3),
                areaId = newArea.id,
            )

        val testChildTyyrikki =
            DevPerson(
                firstName = "Tyyrikki",
                lastName = "Täydentävässäoleva",
                dateOfBirth = startDate.minusYears(2),
            )

        db.transaction { tx ->
            tx.insert(newArea)
            tx.insert(purchasedSchool)
            tx.insert(testChildTyyrikki, DevPersonType.CHILD)
            tx.insert(
                DevPlacement(
                    childId = testChildTyyrikki.id,
                    unitId = purchasedSchool.id,
                    type = PlacementType.PRESCHOOL_DAYCARE,
                    startDate = startDate,
                    endDate = startDate.plusYears(2),
                )
            )

            val testChildCecilia =
                DevPerson(
                    dateOfBirth = startDate.minusYears(2),
                    firstName = "Cecilia",
                    lastName = "af Clubbenberg",
                    language = "sv",
                )

            val testChildVeikko =
                DevPerson(
                    dateOfBirth = startDate.minusYears(4),
                    firstName = "Veikko",
                    lastName = "Vakalapsi",
                    language = "fi",
                )

            tx.insert(testChildCecilia, DevPersonType.CHILD)
            tx.insert(testChildVeikko, DevPersonType.CHILD)

            val placementC =
                DevPlacement(
                    type = PlacementType.CLUB,
                    childId = testChildCecilia.id,
                    unitId = testUnitData[1],
                    startDate = startDate,
                    endDate = startDate.plusYears(1),
                )

            val placementV =
                DevPlacement(
                    type = PlacementType.DAYCARE,
                    childId = testChildVeikko.id,
                    unitId = testUnitData[0],
                    startDate = startDate,
                    endDate = startDate.plusYears(1),
                )

            tx.insert(placementC)
            tx.insert(placementV)
        }

        val results =
            tampereRegionalSurvey.getTampereRegionalSurveyAgeStatistics(
                dbInstance(),
                adminLoginUser,
                mockClock,
                year = startDate.year,
            )

        val expectedResults = Pair(1, 2)

        val assistanceResults =
            Pair(
                results.ageStatistics.first().purchasedUnder3Count,
                results.ageStatistics.first().purchasedOver3Count,
            )

        assertEquals(expectedResults, assistanceResults)
    }

    @Test
    fun `Non-native language results are correct `() {
        val testUnitData = initTestUnitData(startDate)
        initTestPlacementData(
            startDate,
            testUnitData[0],
            FiniteDateRange(startDate, startDate.plusYears(1)),
        )

        val testChildRodrik =
            DevPerson(
                firstName = "Rodrik",
                lastName = "Bokmal",
                language = "no",
                dateOfBirth = startDate.minusYears(2),
                ssn = null,
            )

        db.transaction { tx ->
            tx.insert(testChildRodrik, DevPersonType.CHILD)
            tx.insert(
                DevPlacement(
                    childId = testChildRodrik.id,
                    unitId = testUnitData[5],
                    type = PlacementType.PRESCHOOL_DAYCARE,
                    startDate = startDate,
                    endDate = startDate.plusYears(3),
                )
            )
        }

        val results =
            tampereRegionalSurvey.getTampereRegionalSurveyAgeStatistics(
                dbInstance(),
                adminLoginUser,
                mockClock,
                year = startDate.year,
            )

        val expectedResults = Pair(1, 1)

        val assistanceResults =
            Pair(
                results.ageStatistics.first().nonNativeLanguageUnder3Count,
                results.ageStatistics.first().nonNativeLanguageOver3Count,
            )

        assertEquals(expectedResults, assistanceResults)
    }

    @Test
    fun `Club results are correct `() {
        val testUnitData = initTestUnitData(startDate)
        initTestPlacementData(
            startDate,
            testUnitData[1],
            FiniteDateRange(startDate, startDate.plusYears(1)),
        )

        // add club placements
        db.transaction { tx ->
            val testChildCecilia =
                DevPerson(
                    dateOfBirth = startDate.minusYears(2),
                    firstName = "Cecilia",
                    lastName = "af Clubbenberg",
                    language = "sv",
                )

            val testChildKaarlo =
                DevPerson(
                    dateOfBirth = startDate.minusYears(4),
                    firstName = "Kaarlo",
                    lastName = "Kerholainen",
                    language = "fi",
                )

            tx.insert(testChildCecilia, DevPersonType.CHILD)
            tx.insert(testChildKaarlo, DevPersonType.CHILD)

            val placementC =
                DevPlacement(
                    type = PlacementType.CLUB,
                    childId = testChildCecilia.id,
                    unitId = testUnitData[1],
                    startDate = startDate,
                    endDate = startDate.plusYears(1),
                )

            val placementK =
                DevPlacement(
                    type = PlacementType.CLUB,
                    childId = testChildKaarlo.id,
                    unitId = testUnitData[1],
                    startDate = startDate,
                    endDate = startDate.plusYears(1),
                )

            tx.insert(placementC)
            tx.insert(placementK)
        }

        val results =
            tampereRegionalSurvey.getTampereRegionalSurveyAgeStatistics(
                dbInstance(),
                adminLoginUser,
                mockClock,
                year = startDate.year,
            )

        val expectedResults = Pair(1, 1)

        val assistanceResults =
            Pair(
                results.ageStatistics.first().clubUnder3Count,
                results.ageStatistics.first().clubOver3Count,
            )

        assertEquals(expectedResults, assistanceResults)
    }

    @Test
    fun `Daycare care day counts are correct `() {
        val octFirst = LocalDate.of(2024, 10, 1)
        val defaultPlacementRange = FiniteDateRange(octFirst, octFirst.plusMonths(2).minusDays(1))
        val testUnitData = initTestUnitData(startDate)
        // 2024-10-01 - 2024-11-30
        val testPlacementData =
            initTestPlacementData(octFirst, testUnitData[0], defaultPlacementRange)

        db.transaction { tx ->
            // add some non-compliant data that should not show up
            val testChildCecilia =
                DevPerson(
                    dateOfBirth = startDate.minusYears(2),
                    firstName = "Cecilia",
                    lastName = "af Clubbenberg",
                    language = "sv",
                )

            val testChildElmo =
                DevPerson(
                    dateOfBirth = startDate.minusYears(4),
                    firstName = "Elmo",
                    lastName = "Esiopetettava",
                    language = "fi",
                )

            val testChildKerstin =
                DevPerson(
                    dateOfBirth = startDate.minusYears(4),
                    firstName = "Kerstin",
                    lastName = "Koulullatäydentävässä",
                    language = "fi",
                )

            tx.insert(testChildCecilia, DevPersonType.CHILD)
            tx.insert(testChildElmo, DevPersonType.CHILD)
            tx.insert(testChildKerstin, DevPersonType.CHILD)

            val placementC =
                DevPlacement(
                    type = PlacementType.CLUB,
                    childId = testChildCecilia.id,
                    unitId = testUnitData[1],
                    startDate = startDate,
                    endDate = startDate.plusYears(1),
                )

            val placementV =
                DevPlacement(
                    type = PlacementType.PRESCHOOL,
                    childId = testChildElmo.id,
                    unitId = testUnitData[0],
                    startDate = startDate,
                    endDate = startDate.plusYears(1),
                )

            val placementK =
                DevPlacement(
                    type = PlacementType.PRESCHOOL_DAYCARE,
                    childId = testChildKerstin.id,
                    unitId = testUnitData[5],
                    startDate = startDate,
                    endDate = startDate.plusYears(1),
                )

            tx.insert(placementC)
            tx.insert(placementV)
            tx.insert(placementK)

            val aapo = testPlacementData[0].first
            // add full day absence to see that it is not counted as a care day
            tx.insert(
                DevAbsence(
                    childId = aapo.id,
                    date = LocalDate.of(2024, 11, 1),
                    absenceCategory = AbsenceCategory.BILLABLE,
                )
            )
            tx.insert(
                DevAbsence(
                    childId = aapo.id,
                    date = LocalDate.of(2024, 11, 1),
                    absenceCategory = AbsenceCategory.NONBILLABLE,
                )
            )

            // add shift care placement for Aapo during December to see that it counts
            val shiftCarePlacement =
                DevPlacement(
                    type = PlacementType.DAYCARE,
                    childId = aapo.id,
                    startDate = defaultPlacementRange.end.plusDays(1),
                    endDate = defaultPlacementRange.end.plusMonths(2),
                    unitId = testUnitData[1],
                )

            tx.insert(shiftCarePlacement)

            tx.insert(
                DevServiceNeed(
                    placementId = shiftCarePlacement.id,
                    optionId = fullTimeSno.id,
                    shiftCare = ShiftCareType.FULL,
                    startDate = shiftCarePlacement.startDate,
                    endDate = shiftCarePlacement.endDate,
                    confirmedBy = admin.evakaUserId,
                )
            )

            // add a backup care to see that it is not counted twice
            val aapoFirstPlacement = testPlacementData[0].second.first()

            tx.insert(
                DevBackupCare(
                    childId = aapo.id,
                    unitId = testUnitData[1],
                    period =
                        FiniteDateRange(
                            aapoFirstPlacement.startDate.plusDays(1),
                            aapoFirstPlacement.startDate.plusDays(2),
                        ),
                )
            )
        }

        val results =
            tampereRegionalSurvey.getTampereRegionalSurveyAgeStatistics(
                dbInstance(),
                adminLoginUser,
                mockClock,
                year = startDate.year,
            )
        //             oct   nov              dec
        // Aapo   (<3y) 23 + 21 - 1 absence + 31
        // Bertil (>3y) 23 + 21
        // Cecil  (>3y) 23 + 21
        val expectedResults = Pair(74, 88)

        // not included: Cecilia (placement type CLUB), Elmo (placement type PRESCHOOL), Kerstin
        // (non-CENTRE unit)
        val assistanceResults =
            Pair(
                results.ageStatistics.first().effectiveCareDaysUnder3Count,
                results.ageStatistics.first().effectiveCareDaysOver3Count,
            )

        assertEquals(expectedResults, assistanceResults)
    }

    @Test
    fun `Family daycare care day counts are correct`() {
        val octFirst = LocalDate.of(2024, 10, 1)
        val testUnitData = initTestUnitData(startDate)
        val testChildren = initTestPlacementData(octFirst, testUnitData[0])

        // add family daycare placements for 2 children: 1 under and over 3
        db.transaction { tx ->
            tx.insert(
                DevPlacement(
                    // 2024-12-01 - 2025-02-01
                    startDate = octFirst,
                    endDate = octFirst.plusMonths(4),
                    unitId = testUnitData[2],
                    childId = testChildren[4].first.id,
                    type = PlacementType.DAYCARE,
                )
            )
            val aapo = testChildren[0].first
            val aapoPlacementEnd = testChildren[0].second.last().endDate
            tx.insert(
                // 2024-12-02 - 2025-02-01
                DevPlacement(
                    startDate = aapoPlacementEnd.plusDays(1),
                    endDate = aapoPlacementEnd.plusMonths(4),
                    unitId = testUnitData[2],
                    childId = testChildren[0].first.id,
                    type = PlacementType.DAYCARE,
                )
            )

            // add full day absence to see that it is not counted as a care day
            tx.insert(
                DevAbsence(
                    childId = aapo.id,
                    date = LocalDate.of(2024, 12, 5),
                    absenceCategory = AbsenceCategory.BILLABLE,
                )
            )
            tx.insert(
                DevAbsence(
                    childId = aapo.id,
                    date = LocalDate.of(2024, 12, 5),
                    absenceCategory = AbsenceCategory.NONBILLABLE,
                )
            )
        }

        val results =
            tampereRegionalSurvey.getTampereRegionalSurveyAgeStatistics(
                dbInstance(),
                adminLoginUser,
                mockClock,
                year = startDate.year,
            )

        //             oct  nov  dec
        // Aapo  (<3y)           (22 - 4 - 1)
        // Fabio (>3y) 23 + 21 + (22 - 4)
        val expectedResults = Pair(17, 62)

        val careDayResults =
            Pair(
                results.ageStatistics.first().effectiveFamilyDaycareDaysUnder3Count,
                results.ageStatistics.first().effectiveFamilyDaycareDaysOver3Count,
            )

        assertEquals(expectedResults, careDayResults)
    }

    @Test
    fun `Daycare care day counts work for shift care unit`() {
        // test that non-shift care children count only for non-shift care days
        val octFirst = LocalDate.of(2024, 10, 1)
        initTestUnitData(octFirst)
        val testArea = DevCareArea(name = "test", shortName = "test")
        val shiftCareUnit =
            DevDaycare(
                name = "Shift care daycare",
                areaId = testArea.id,
                openingDate = octFirst.minusDays(7),
                type = setOf(CareType.CENTRE, CareType.PRESCHOOL),
                operationTimes =
                    List(5) { TimeRange(LocalTime.of(8, 0), LocalTime.of(18, 0)) } +
                        List(2) { null },
                shiftCareOperationTimes =
                    List(7) { TimeRange(LocalTime.of(8, 0), LocalTime.of(23, 0)) },
                shiftCareOpenOnHolidays = true,
                enabledPilotFeatures = setOf(PilotFeature.RESERVATIONS),
            )
        val testChildPaavo = DevPerson(firstName = "Paavo", lastName = "Päivälapsi")
        val testChildVeera = DevPerson(firstName = "Veera", lastName = "Vuorolapsi")
        val testChildSiiri = DevPerson(firstName = "Siiri", lastName = "Satunnainen")

        db.transaction { tx ->
            tx.insert(admin)
            tx.insert(fullTimeSno)
            tx.insert(testArea)
            tx.insert(shiftCareUnit)

            tx.insert(testChildPaavo, DevPersonType.CHILD)
            tx.insert(testChildVeera, DevPersonType.CHILD)
            tx.insert(testChildSiiri, DevPersonType.CHILD)

            val shiftCarePlacement =
                DevPlacement(
                    unitId = shiftCareUnit.id,
                    childId = testChildVeera.id,
                    startDate = LocalDate.of(2024, 1, 1),
                    endDate = LocalDate.of(2024, 1, 31),
                )

            val daycarePlacement =
                DevPlacement(
                    unitId = shiftCareUnit.id,
                    childId = testChildPaavo.id,
                    startDate = LocalDate.of(2024, 1, 1),
                    endDate = LocalDate.of(2024, 1, 31),
                )

            val shiftCareServiceNeed =
                DevServiceNeed(
                    shiftCare = ShiftCareType.FULL,
                    placementId = shiftCarePlacement.id,
                    startDate = shiftCarePlacement.startDate,
                    endDate = shiftCarePlacement.endDate,
                    optionId = fullTimeSno.id,
                    confirmedBy = admin.evakaUserId,
                )

            tx.insert(daycarePlacement)
            tx.insert(shiftCarePlacement)
            tx.insert(shiftCareServiceNeed)
            val siiriPlacementId =
                tx.insert(
                    shiftCarePlacement.copy(
                        id = PlacementId(UUID.randomUUID()),
                        childId = testChildSiiri.id,
                    )
                )
            tx.insert(
                shiftCareServiceNeed.copy(
                    id = ServiceNeedId(UUID.randomUUID()),
                    placementId = siiriPlacementId,
                    shiftCare = ShiftCareType.INTERMITTENT,
                )
            )
        }

        val results =
            tampereRegionalSurvey.getTampereRegionalSurveyAgeStatistics(
                dbInstance(),
                adminLoginUser,
                mockClock,
                year = startDate.year,
            )

        //                                       january 2024
        // Paavo (NONE) (>3y): 22 business days + 0 weekend days + 0 holidays = 22
        // Veera (FULL) (>3y): 22 business days + 8 weekend days + 1 holidays = 31
        // Siiri (INT.) (>3y): 22 business days + 8 weekend days + 1 holidays = 31
        val expectedResults = Pair(0, 84)

        val careDayResults =
            Pair(
                results.ageStatistics.first().effectiveCareDaysUnder3Count,
                results.ageStatistics.first().effectiveCareDaysOver3Count,
            )

        assertEquals(expectedResults, careDayResults)
    }

    @Test
    fun `Voucher total count is correct`() {
        val octFirst = LocalDate.of(2024, 10, 1)
        val testUnitData = initTestUnitData(octFirst)
        initTestPlacementData(
            octFirst,
            testUnitData[3],
            FiniteDateRange(octFirst, octFirst.plusMonths(3).minusDays(1)),
        )

        val results =
            tampereRegionalSurvey.getTampereRegionalSurveyYearlyStatistics(
                dbInstance(),
                adminLoginUser,
                mockClock,
                year = startDate.year,
            )

        // Aapo + Bertil + Cecil = 3
        assertEquals(3, results.yearlyStatistics.first().voucherTotalCount)
    }

    @Test
    fun `Five year old voucher counts are correct`() {
        val octFirst = LocalDate.of(2024, 10, 1)
        val testUnitData = initTestUnitData(octFirst)
        initTestPlacementData(
            start = octFirst,
            daycareId = testUnitData[3],
            defaultPlacementDuration =
                FiniteDateRange(octFirst, octFirst.plusMonths(3).minusDays(1)),
            baseAge = 5,
        )

        val results =
            tampereRegionalSurvey.getTampereRegionalSurveyYearlyStatistics(
                dbInstance(),
                adminLoginUser,
                mockClock,
                year = startDate.year,
            )

        // Bertil + Cecil = 2
        assertEquals(2, results.yearlyStatistics.first().voucher5YearOldCount)
    }

    @Test
    fun `Five year old purchased counts are correct`() {
        val octFirst = LocalDate.of(2024, 10, 1)
        val testUnitData = initTestUnitData(octFirst)
        initTestPlacementData(
            start = octFirst,
            daycareId = testUnitData[4],
            defaultPlacementDuration =
                FiniteDateRange(octFirst, octFirst.plusMonths(3).minusDays(1)),
            baseAge = 5,
        )

        val results =
            tampereRegionalSurvey.getTampereRegionalSurveyYearlyStatistics(
                dbInstance(),
                adminLoginUser,
                mockClock,
                year = startDate.year,
            )

        // Bertil + Cecil = 2
        assertEquals(2, results.yearlyStatistics.first().purchased5YearOldCount)
    }

    @Test
    fun `Five year old municipal counts are correct`() {
        val octFirst = LocalDate.of(2024, 10, 1)
        val testUnitData = initTestUnitData(octFirst)
        initTestPlacementData(
            start = octFirst,
            daycareId = testUnitData[0],
            defaultPlacementDuration =
                FiniteDateRange(octFirst, octFirst.plusMonths(3).minusDays(1)),
            baseAge = 5,
        )

        val results =
            tampereRegionalSurvey.getTampereRegionalSurveyYearlyStatistics(
                dbInstance(),
                adminLoginUser,
                mockClock,
                year = startDate.year,
            )

        // Bertil + Cecil = 2
        assertEquals(2, results.yearlyStatistics.first().municipal5YearOldCount)
    }

    @Test
    fun `Five year old family care counts are correct`() {
        val octFirst = LocalDate.of(2024, 10, 1)
        val testUnitData = initTestUnitData(octFirst)
        initTestPlacementData(
            start = octFirst,
            daycareId = testUnitData[2],
            defaultPlacementDuration =
                FiniteDateRange(octFirst, octFirst.plusMonths(3).minusDays(1)),
            baseAge = 5,
        )

        val results =
            tampereRegionalSurvey.getTampereRegionalSurveyYearlyStatistics(
                dbInstance(),
                adminLoginUser,
                mockClock,
                year = startDate.year,
            )

        // Bertil + Cecil = 2
        assertEquals(2, results.yearlyStatistics.first().familyCare5YearOldCount)
    }

    @Test
    fun `Five year club care counts are correct`() {
        val octFirst = LocalDate.of(2024, 10, 1)
        val testUnitData = initTestUnitData(octFirst)
        val defaultPlacementDuration =
            FiniteDateRange(octFirst, octFirst.plusMonths(3).minusDays(1))

        val testChildren =
            initTestPlacementData(
                start = octFirst,
                daycareId = testUnitData[1],
                defaultPlacementDuration = defaultPlacementDuration,
                baseAge = 5,
            )

        db.transaction { tx ->
            // add two 5-year-old clubbers
            tx.insert(
                DevPlacement(
                    childId = testChildren[3].first.id,
                    startDate = defaultPlacementDuration.start,
                    endDate = defaultPlacementDuration.end,
                    unitId = testUnitData[1],
                    type = PlacementType.CLUB,
                )
            )

            tx.insert(
                DevPlacement(
                    childId = testChildren[4].first.id,
                    startDate = defaultPlacementDuration.start,
                    endDate = defaultPlacementDuration.end,
                    unitId = testUnitData[1],
                    type = PlacementType.CLUB,
                )
            )
        }

        val results =
            tampereRegionalSurvey.getTampereRegionalSurveyYearlyStatistics(
                dbInstance(),
                adminLoginUser,
                mockClock,
                year = startDate.year,
            )

        // Ville + Fabio = 2
        assertEquals(2, results.yearlyStatistics.first().club5YearOldCount)
    }

    @Test
    fun `Voucher assistance count is correct`() {
        val octFirst = LocalDate.of(2024, 10, 1)
        val testUnitData = initTestUnitData(octFirst)
        val defaultPlacementDuration =
            FiniteDateRange(octFirst, octFirst.plusMonths(3).minusDays(1))

        val childTestData =
            initTestPlacementData(
                start = octFirst,
                daycareId = testUnitData[3],
                defaultPlacementDuration = defaultPlacementDuration,
                baseAge = 5,
            )

        db.transaction { tx ->
            // add an assistance factor for Aapo that should show up
            tx.insert(
                DevAssistanceFactor(
                    childId = childTestData[0].first.id,
                    validDuring =
                        FiniteDateRange(
                            defaultPlacementDuration.start,
                            defaultPlacementDuration.end,
                        ),
                    capacityFactor = 5.50,
                )
            )

            // add action 40 case that should show up

            val testChildTahto =
                DevPerson(
                    dateOfBirth = startDate.minusYears(2),
                    firstName = "Tahto",
                    lastName = "Tukitoimi",
                    language = "fi",
                )

            tx.insert(testChildTahto, DevPersonType.CHILD)
            tx.insert(
                DevPlacement(
                    childId = testChildTahto.id,
                    type = PlacementType.DAYCARE,
                    unitId = testUnitData[3],
                    startDate = defaultPlacementDuration.start,
                    endDate = defaultPlacementDuration.end,
                )
            )

            tx.insert(
                DevAssistanceAction(
                    childId = testChildTahto.id,
                    startDate = defaultPlacementDuration.start,
                    endDate = defaultPlacementDuration.end,
                    actions = setOf(actionOption40.value),
                )
            )

            // add a test child with an assistance factor that should not show up
            val testChildKaarina =
                DevPerson(
                    dateOfBirth = startDate.minusYears(2),
                    firstName = "Kaarina",
                    lastName = "Kunnallinen",
                    language = "fi",
                )

            tx.insert(testChildKaarina, DevPersonType.CHILD)
            tx.insert(
                DevPlacement(
                    childId = testChildKaarina.id,
                    type = PlacementType.DAYCARE,
                    unitId = testUnitData[0],
                    startDate = defaultPlacementDuration.start,
                    endDate = defaultPlacementDuration.end,
                )
            )
            tx.insert(
                DevAssistanceFactor(
                    childId = testChildKaarina.id,
                    validDuring =
                        FiniteDateRange(
                            defaultPlacementDuration.start,
                            defaultPlacementDuration.end,
                        ),
                    capacityFactor = 5.50,
                )
            )
        }

        val results =
            tampereRegionalSurvey.getTampereRegionalSurveyYearlyStatistics(
                dbInstance(),
                adminLoginUser,
                mockClock,
                year = startDate.year,
            )

        // shown: Tahto (action 40) + Cecil (action 10) + Aapo (factor)
        // not shown: Bertil (placement type), Kaarina (daycare provider type)
        assertEquals(3, results.yearlyStatistics.first().voucherAssistanceCount)
    }

    @Test
    fun `Municipal assistance level counts are correct`() {

        val octFirst = LocalDate.of(2024, 10, 1)
        val testUnitData = initTestUnitData(octFirst)
        val defaultPlacementDuration =
            FiniteDateRange(octFirst, octFirst.plusMonths(3).minusDays(1))

        // base data adds daycare assistance to Cecil with level GENERAL_SUPPORT that should NOT
        // show up
        val childTestData =
            initTestPlacementData(
                start = octFirst,
                daycareId = testUnitData[0],
                defaultPlacementDuration = defaultPlacementDuration,
                baseAge = 5,
            )

        db.transaction { tx ->
            // add daycare assistance period that should show up for Aapo
            tx.insert(
                DevDaycareAssistance(
                    childId = childTestData[0].first.id,
                    validDuring =
                        FiniteDateRange(
                            defaultPlacementDuration.start,
                            defaultPlacementDuration.end,
                        ),
                    level = DaycareAssistanceLevel.GENERAL_SUPPORT_WITH_DECISION,
                )
            )

            // add assistance to Bertil that should NOT show up because of placement type
            tx.insert(
                DevDaycareAssistance(
                    childId = childTestData[1].first.id,
                    validDuring =
                        FiniteDateRange(
                            defaultPlacementDuration.start,
                            defaultPlacementDuration.end,
                        ),
                    level = DaycareAssistanceLevel.INTENSIFIED_SUPPORT,
                )
            )

            // add daycare assistance period that should show up for Ville
            tx.insert(
                DevPlacement(
                    childId = childTestData[3].first.id,
                    type = PlacementType.DAYCARE,
                    unitId = testUnitData[0],
                    startDate = defaultPlacementDuration.start,
                    endDate = defaultPlacementDuration.end,
                )
            )
            tx.insert(
                DevDaycareAssistance(
                    childId = childTestData[3].first.id,
                    validDuring =
                        FiniteDateRange(
                            defaultPlacementDuration.start,
                            defaultPlacementDuration.end,
                        ),
                    level = DaycareAssistanceLevel.SPECIAL_SUPPORT,
                )
            )

            // add a daycare assistance period that should NOT show up for Fabio (family care)
            tx.insert(
                DevPlacement(
                    childId = childTestData[4].first.id,
                    type = PlacementType.DAYCARE,
                    unitId = testUnitData[2],
                    startDate = defaultPlacementDuration.start,
                    endDate = defaultPlacementDuration.end,
                )
            )
            tx.insert(
                DevDaycareAssistance(
                    childId = childTestData[4].first.id,
                    validDuring =
                        FiniteDateRange(
                            defaultPlacementDuration.start,
                            defaultPlacementDuration.end,
                        ),
                    level = DaycareAssistanceLevel.SPECIAL_SUPPORT,
                )
            )

            // add a voucher daycare assistance period that should NOT show up
            val testChildPirjo =
                DevPerson(
                    dateOfBirth = startDate.minusYears(2),
                    firstName = "Pirjo",
                    lastName = "Palse",
                    language = "fi",
                )

            tx.insert(testChildPirjo, DevPersonType.CHILD)
            tx.insert(
                DevPlacement(
                    childId = testChildPirjo.id,
                    type = PlacementType.DAYCARE,
                    unitId = testUnitData[3],
                    startDate = defaultPlacementDuration.start,
                    endDate = defaultPlacementDuration.end,
                )
            )
            tx.insert(
                DevDaycareAssistance(
                    childId = testChildPirjo.id,
                    validDuring =
                        FiniteDateRange(
                            defaultPlacementDuration.start,
                            defaultPlacementDuration.end,
                        ),
                    level = DaycareAssistanceLevel.SPECIAL_SUPPORT,
                )
            )

            // add INTENSIFIED SUPPORT for DAYCARE placement that should show up
            val testChildTerho =
                DevPerson(
                    dateOfBirth = startDate.minusYears(2),
                    firstName = "Terho",
                    lastName = "Tehostettu",
                    language = "fi",
                )

            tx.insert(testChildTerho, DevPersonType.CHILD)
            tx.insert(
                DevPlacement(
                    childId = testChildTerho.id,
                    type = PlacementType.DAYCARE,
                    unitId = testUnitData[0],
                    startDate = defaultPlacementDuration.start,
                    endDate = defaultPlacementDuration.end,
                )
            )
            tx.insert(
                DevDaycareAssistance(
                    childId = testChildTerho.id,
                    validDuring =
                        FiniteDateRange(
                            defaultPlacementDuration.start,
                            defaultPlacementDuration.end,
                        ),
                    level = DaycareAssistanceLevel.INTENSIFIED_SUPPORT,
                )
            )
        }

        val results =
            tampereRegionalSurvey.getTampereRegionalSurveyYearlyStatistics(
                dbInstance(),
                adminLoginUser,
                mockClock,
                year = startDate.year,
            )

        // shown: Aapo (GENERAL_WITH_DECISION), Terho (INTENSIFIED), Ville (SPECIAL)
        // not shown: Bertil (wrong placement type), Cecil (wrong level), Fabio (wrong unit type),
        // Pirjo (wrong provider type)
        assertEquals(1, results.yearlyStatistics.first().municipalGeneralAssistanceCount)
        assertEquals(1, results.yearlyStatistics.first().municipalSpecialAssistanceCount)
        assertEquals(1, results.yearlyStatistics.first().municipalEnhancedAssistanceCount)
    }

    @Test
    fun `Voucher assistance level counts are correct`() {
        val octFirst = LocalDate.of(2024, 10, 1)
        val testUnitData = initTestUnitData(octFirst)
        val defaultPlacementDuration =
            FiniteDateRange(octFirst, octFirst.plusMonths(3).minusDays(1))

        val childTestData =
            initTestPlacementData(
                start = octFirst,
                daycareId = testUnitData[3],
                defaultPlacementDuration = defaultPlacementDuration,
                baseAge = 5,
            )

        db.transaction { tx ->
            // add daycare assistance periods that should show up for Aapo, Terho and Ville
            tx.insert(
                DevDaycareAssistance(
                    childId = childTestData[0].first.id,
                    validDuring =
                        FiniteDateRange(
                            defaultPlacementDuration.start,
                            defaultPlacementDuration.end,
                        ),
                    level = DaycareAssistanceLevel.GENERAL_SUPPORT_WITH_DECISION,
                )
            )

            tx.insert(
                DevDaycareAssistance(
                    childId = childTestData[1].first.id,
                    validDuring =
                        FiniteDateRange(
                            defaultPlacementDuration.start,
                            defaultPlacementDuration.end,
                        ),
                    level = DaycareAssistanceLevel.INTENSIFIED_SUPPORT,
                )
            )

            tx.insert(
                DevPlacement(
                    childId = childTestData[3].first.id,
                    type = PlacementType.DAYCARE,
                    unitId = testUnitData[3],
                    startDate = defaultPlacementDuration.start,
                    endDate = defaultPlacementDuration.end,
                )
            )
            tx.insert(
                DevDaycareAssistance(
                    childId = childTestData[3].first.id,
                    validDuring =
                        FiniteDateRange(
                            defaultPlacementDuration.start,
                            defaultPlacementDuration.end,
                        ),
                    level = DaycareAssistanceLevel.SPECIAL_SUPPORT,
                )
            )

            // add INTENSIFIED SUPPORT for DAYCARE placement that should show up
            val testChildTerho =
                DevPerson(
                    dateOfBirth = startDate.minusYears(2),
                    firstName = "Terho",
                    lastName = "Tehostettu",
                    language = "fi",
                )

            tx.insert(testChildTerho, DevPersonType.CHILD)
            tx.insert(
                DevPlacement(
                    childId = testChildTerho.id,
                    type = PlacementType.DAYCARE,
                    unitId = testUnitData[3],
                    startDate = defaultPlacementDuration.start,
                    endDate = defaultPlacementDuration.end,
                )
            )
            tx.insert(
                DevDaycareAssistance(
                    childId = testChildTerho.id,
                    validDuring =
                        FiniteDateRange(
                            defaultPlacementDuration.start,
                            defaultPlacementDuration.end,
                        ),
                    level = DaycareAssistanceLevel.INTENSIFIED_SUPPORT,
                )
            )

            // add a daycare assistance period that should not show up for family care Fabio
            tx.insert(
                DevPlacement(
                    childId = childTestData[4].first.id,
                    type = PlacementType.DAYCARE,
                    unitId = testUnitData[2],
                    startDate = defaultPlacementDuration.start,
                    endDate = defaultPlacementDuration.end,
                )
            )
            tx.insert(
                DevDaycareAssistance(
                    childId = childTestData[4].first.id,
                    validDuring =
                        FiniteDateRange(
                            defaultPlacementDuration.start,
                            defaultPlacementDuration.end,
                        ),
                    level = DaycareAssistanceLevel.SPECIAL_SUPPORT,
                )
            )

            // add a municipal daycare assistance period that should not show up
            val testChildKaarina =
                DevPerson(
                    dateOfBirth = startDate.minusYears(2),
                    firstName = "Kaarina",
                    lastName = "Kunnallinen",
                    language = "fi",
                )

            tx.insert(testChildKaarina, DevPersonType.CHILD)
            tx.insert(
                DevPlacement(
                    childId = testChildKaarina.id,
                    type = PlacementType.DAYCARE,
                    unitId = testUnitData[0],
                    startDate = defaultPlacementDuration.start,
                    endDate = defaultPlacementDuration.end,
                )
            )
            tx.insert(
                DevDaycareAssistance(
                    childId = testChildKaarina.id,
                    validDuring =
                        FiniteDateRange(
                            defaultPlacementDuration.start,
                            defaultPlacementDuration.end,
                        ),
                    level = DaycareAssistanceLevel.SPECIAL_SUPPORT,
                )
            )
        }

        val results =
            tampereRegionalSurvey.getTampereRegionalSurveyYearlyStatistics(
                dbInstance(),
                adminLoginUser,
                mockClock,
                year = startDate.year,
            )

        // shown: Aapo (GENERAL), Terho (INTENSIFIED), Ville (SPECIAL)
        // not shown: Bertil (wrong placement type), Cecil (wrong level), Fabio (wrong unit type),
        // Kaarina (wrong provider type)
        assertEquals(1, results.yearlyStatistics.first().voucherGeneralAssistanceCount)
        assertEquals(1, results.yearlyStatistics.first().voucherSpecialAssistanceCount)
        assertEquals(1, results.yearlyStatistics.first().voucherEnhancedAssistanceCount)
    }

    @Test
    fun `Preschool daycare municipal unit and school counts are correct`() {
        val octFirst = LocalDate.of(2024, 10, 1)
        val defaultPlacementRange = FiniteDateRange(octFirst, octFirst.plusMonths(3).minusDays(1))
        val testUnitData = initTestUnitData(startDate)
        // 2024-10-01 - 2024-12-31
        initTestPlacementData(octFirst, testUnitData[0], defaultPlacementRange)

        db.transaction { tx ->
            // add some non-compliant data that should not show up
            val testChildCecilia =
                DevPerson(
                    dateOfBirth = startDate.minusYears(2),
                    firstName = "Cecilia",
                    lastName = "af Clubbenberg",
                    language = "sv",
                )

            val testChildElmo =
                DevPerson(
                    dateOfBirth = startDate.minusYears(4),
                    firstName = "Elmo",
                    lastName = "Esiopetettava",
                    language = "fi",
                )

            tx.insert(testChildCecilia, DevPersonType.CHILD)
            tx.insert(testChildElmo, DevPersonType.CHILD)

            val placementC =
                DevPlacement(
                    type = PlacementType.CLUB,
                    childId = testChildCecilia.id,
                    unitId = testUnitData[1],
                    startDate = startDate,
                    endDate = startDate.plusYears(1),
                )

            val placementV =
                DevPlacement(
                    type = PlacementType.PRESCHOOL,
                    childId = testChildElmo.id,
                    unitId = testUnitData[0],
                    startDate = startDate,
                    endDate = startDate.plusYears(1),
                )

            tx.insert(placementC)
            tx.insert(placementV)

            // add shift care placement to daycare
            val testChildVanja =
                DevPerson(
                    dateOfBirth = startDate.minusYears(4),
                    firstName = "Vanja",
                    lastName = "Vakayksikköläinen",
                    language = "fi",
                )
            tx.insert(testChildVanja, DevPersonType.CHILD)

            val vanjaPlacement =
                DevPlacement(
                    type = PlacementType.PRESCHOOL_DAYCARE,
                    childId = testChildVanja.id,
                    startDate = defaultPlacementRange.start,
                    endDate = defaultPlacementRange.end,
                    unitId = testUnitData[0],
                )

            tx.insert(vanjaPlacement)

            // add a preschool daycare placement to a school unit
            val testChildSirpa =
                DevPerson(
                    dateOfBirth = startDate.minusYears(4),
                    firstName = "Sirpa",
                    lastName = "Schüler",
                    language = "fi",
                )

            tx.insert(testChildSirpa, DevPersonType.CHILD)
            val sirpaPlacement =
                DevPlacement(
                    type = PlacementType.PRESCHOOL_DAYCARE,
                    childId = testChildSirpa.id,
                    startDate = defaultPlacementRange.start,
                    endDate = defaultPlacementRange.end,
                    unitId = testUnitData[5],
                )

            tx.insert(sirpaPlacement)
        }

        val results =
            tampereRegionalSurvey.getTampereRegionalSurveyYearlyStatistics(
                dbInstance(),
                adminLoginUser,
                mockClock,
                year = startDate.year,
            )

        // Vanja + Bertil in daycare and Sirpa in school
        val expectedResults = Pair(2, 1)

        val shiftCareCountResults =
            Pair(
                results.yearlyStatistics.first().preschoolDaycareUnitCareCount,
                results.yearlyStatistics.first().preschoolDaycareSchoolCareCount,
            )

        assertEquals(expectedResults, shiftCareCountResults)
    }

    @Test
    fun `Preschool daycare municipal unit and school shift care counts are correct`() {
        val octFirst = LocalDate.of(2024, 10, 1)
        val defaultPlacementRange = FiniteDateRange(octFirst, octFirst.plusMonths(3).minusDays(1))
        val testUnitData = initTestUnitData(startDate)
        // 2024-10-01 - 2024-12-31
        initTestPlacementData(octFirst, testUnitData[0], defaultPlacementRange)

        db.transaction { tx ->
            // add some non-compliant data that should not show up
            val testChildCecilia =
                DevPerson(
                    dateOfBirth = startDate.minusYears(2),
                    firstName = "Cecilia",
                    lastName = "af Clubbenberg",
                    language = "sv",
                )

            val testChildElmo =
                DevPerson(
                    dateOfBirth = startDate.minusYears(4),
                    firstName = "Elmo",
                    lastName = "Esiopetettava",
                    language = "fi",
                )

            tx.insert(testChildCecilia, DevPersonType.CHILD)
            tx.insert(testChildElmo, DevPersonType.CHILD)

            val placementC =
                DevPlacement(
                    type = PlacementType.CLUB,
                    childId = testChildCecilia.id,
                    unitId = testUnitData[1],
                    startDate = startDate,
                    endDate = startDate.plusYears(1),
                )

            val placementV =
                DevPlacement(
                    type = PlacementType.PRESCHOOL,
                    childId = testChildElmo.id,
                    unitId = testUnitData[0],
                    startDate = startDate,
                    endDate = startDate.plusYears(1),
                )

            tx.insert(placementC)
            tx.insert(placementV)

            // add shift care placement to daycare
            val testChildVanja =
                DevPerson(
                    dateOfBirth = startDate.minusYears(4),
                    firstName = "Vanja",
                    lastName = "Vakayksikköläinen",
                    language = "fi",
                )
            tx.insert(testChildVanja, DevPersonType.CHILD)

            val shiftCarePlacement =
                DevPlacement(
                    type = PlacementType.PRESCHOOL_DAYCARE,
                    childId = testChildVanja.id,
                    startDate = defaultPlacementRange.start,
                    endDate = defaultPlacementRange.end,
                    unitId = testUnitData[0],
                )

            tx.insert(shiftCarePlacement)

            tx.insert(
                DevServiceNeed(
                    placementId = shiftCarePlacement.id,
                    optionId = fullTimeSno.id,
                    shiftCare = ShiftCareType.FULL,
                    startDate = shiftCarePlacement.startDate,
                    endDate = shiftCarePlacement.endDate,
                    confirmedBy = admin.evakaUserId,
                )
            )

            // add a preschool daycare shift care placement to a school unit
            val testChildSirpa =
                DevPerson(
                    dateOfBirth = startDate.minusYears(4),
                    firstName = "Sirpa",
                    lastName = "Schüler",
                    language = "fi",
                )

            tx.insert(testChildSirpa, DevPersonType.CHILD)
            val shiftCarePlacement2 =
                DevPlacement(
                    type = PlacementType.PRESCHOOL_DAYCARE,
                    childId = testChildSirpa.id,
                    startDate = defaultPlacementRange.start,
                    endDate = defaultPlacementRange.end,
                    unitId = testUnitData[5],
                )

            tx.insert(shiftCarePlacement2)

            tx.insert(
                DevServiceNeed(
                    placementId = shiftCarePlacement2.id,
                    optionId = fullTimeSno.id,
                    shiftCare = ShiftCareType.INTERMITTENT,
                    startDate = shiftCarePlacement.startDate,
                    endDate = shiftCarePlacement.endDate,
                    confirmedBy = admin.evakaUserId,
                )
            )
        }

        val results =
            tampereRegionalSurvey.getTampereRegionalSurveyYearlyStatistics(
                dbInstance(),
                adminLoginUser,
                mockClock,
                year = startDate.year,
            )

        // Vanja + Bertil in daycare and Sirpa in school
        val expectedResults = Pair(2, 1)

        val shiftCareCountResults =
            Pair(
                results.yearlyStatistics.first().preschoolDaycareUnitShiftCareCount,
                results.yearlyStatistics.first().preschoolDaycareSchoolShiftCareCount,
            )

        assertEquals(expectedResults, shiftCareCountResults)
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
                        name = "Daycare and Club B",
                        areaId = areaBId,
                        openingDate = monday.minusDays(7),
                        type = setOf(CareType.CENTRE, CareType.CLUB),
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

            val daycareDId =
                tx.insert(
                    DevDaycare(
                        name = "Private service voucher daycare",
                        areaId = areaAId,
                        openingDate = monday.minusMonths(1),
                        type = setOf(CareType.CENTRE),
                        providerType = ProviderType.PRIVATE_SERVICE_VOUCHER,
                        operationTimes =
                            List(5) { TimeRange(LocalTime.of(8, 0), LocalTime.of(18, 0)) } +
                                List(2) { null },
                        enabledPilotFeatures = setOf(PilotFeature.RESERVATIONS),
                    )
                )

            val daycareEId =
                tx.insert(
                    DevDaycare(
                        name = "Purchased care daycare",
                        areaId = areaAId,
                        openingDate = monday.minusMonths(1),
                        type = setOf(CareType.CENTRE),
                        providerType = ProviderType.PURCHASED,
                        operationTimes =
                            List(5) { TimeRange(LocalTime.of(8, 0), LocalTime.of(18, 0)) } +
                                List(2) { null },
                        enabledPilotFeatures = setOf(PilotFeature.RESERVATIONS),
                    )
                )

            val daycareFId =
                tx.insert(
                    DevDaycare(
                        name = "School",
                        areaId = areaAId,
                        openingDate = monday.minusMonths(1),
                        type = setOf(CareType.PRESCHOOL),
                        providerType = ProviderType.MUNICIPAL,
                        operationTimes =
                            List(5) { TimeRange(LocalTime.of(8, 0), LocalTime.of(18, 0)) } +
                                List(2) { null },
                        shiftCareOperationTimes =
                            List(7) { TimeRange(LocalTime.of(0, 0), LocalTime.of(23, 59)) },
                        shiftCareOpenOnHolidays = true,
                        enabledPilotFeatures = setOf(PilotFeature.RESERVATIONS),
                    )
                )

            listOf(daycareAId, daycareBId, daycareCId, daycareDId, daycareEId, daycareFId)
        }
    }

    private fun initTestPlacementData(
        start: LocalDate,
        daycareId: DaycareId,
        defaultPlacementDuration: FiniteDateRange = FiniteDateRange(start, start.plusMonths(2)),
        baseAge: Long = 4,
    ): List<Pair<DevPerson, List<DevPlacement>>> {

        val actionOption10 =
            DevAssistanceActionOption(
                value = "10",
                nameFi = "Avustajapalvelut",
                descriptionFi = "Avustajapalvelut",
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
                showForCitizen = true,
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
                showForCitizen = true,
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
                    language = "si",
                )
            tx.insert(testChildAapo, DevPersonType.CHILD)

            val testChildBertil =
                DevPerson(
                    dateOfBirth = start.minusYears(baseAge),
                    firstName = "Bertil",
                    lastName = "Becker",
                    language = "99",
                )
            tx.insert(testChildBertil, DevPersonType.CHILD)

            val testChildCecil =
                DevPerson(
                    dateOfBirth = start.minusYears(baseAge),
                    firstName = "Cecil",
                    lastName = "Cilliacus",
                    language = "sv",
                )
            tx.insert(testChildCecil, DevPersonType.CHILD)

            val testChildVille =
                DevPerson(
                    dateOfBirth = start.minusYears(baseAge),
                    firstName = "Ville",
                    lastName = "Varahoidettava",
                    language = "se",
                )

            tx.insert(testChildVille, DevPersonType.CHILD)

            val testChildFabio =
                DevPerson(
                    dateOfBirth = start.minusYears(baseAge),
                    firstName = "Fabio",
                    lastName = "Familycare",
                )

            tx.insert(testChildFabio, DevPersonType.CHILD)

            // Aapo:   2v, no sn, DAYCARE placement, 09-15 -> 11-15
            // Bertil: 4v, 2x part time sn, PRESCHOOL_DAYCARE_ONLY placement, 09-15 -> 11-15
            // Cecil:  4v, no sn, 2x DAYCARE placement, 09-15 -> 11-15

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
                    level = DaycareAssistanceLevel.GENERAL_SUPPORT,
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
