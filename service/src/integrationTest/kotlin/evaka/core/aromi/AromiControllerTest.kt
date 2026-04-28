// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.aromi

import evaka.core.FullApplicationTest
import evaka.core.absence.AbsenceCategory
import evaka.core.daycare.CareType
import evaka.core.daycare.insertPreschoolTerm
import evaka.core.placement.PlacementType
import evaka.core.serviceneed.ShiftCareType
import evaka.core.shared.PersonId
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.auth.UserRole
import evaka.core.shared.data.DateSet
import evaka.core.shared.dev.DevAbsence
import evaka.core.shared.dev.DevBackupCare
import evaka.core.shared.dev.DevCareArea
import evaka.core.shared.dev.DevChild
import evaka.core.shared.dev.DevDaycare
import evaka.core.shared.dev.DevDaycareGroup
import evaka.core.shared.dev.DevDaycareGroupPlacement
import evaka.core.shared.dev.DevEmployee
import evaka.core.shared.dev.DevPerson
import evaka.core.shared.dev.DevPersonType
import evaka.core.shared.dev.DevPlacement
import evaka.core.shared.dev.DevReservation
import evaka.core.shared.dev.DevServiceNeed
import evaka.core.shared.dev.insert
import evaka.core.shared.dev.insertServiceNeedOption
import evaka.core.shared.domain.EvakaClock
import evaka.core.shared.domain.FiniteDateRange
import evaka.core.shared.domain.Forbidden
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.shared.domain.MockEvakaClock
import evaka.core.shared.domain.TimeRange
import evaka.core.snDefaultDaycare
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ClassPathResource

class AromiControllerTest : FullApplicationTest(resetDbBeforeEach = true) {

    @Autowired private lateinit var aromiController: AromiController

    private val admin = DevEmployee(roles = setOf(UserRole.ADMIN))
    private val today = LocalDate.of(2024, 11, 30)
    private val clock: EvakaClock = MockEvakaClock(HelsinkiDateTime.of(today, LocalTime.of(12, 18)))

    private val area = DevCareArea()
    private val daycare1 =
        DevDaycare(
            areaId = area.id,
            name = "Päiväkoti A",
            shiftCareOpenOnHolidays = true,
            shiftCareOperationTimes =
                List(7) { TimeRange(LocalTime.of(7, 0), LocalTime.of(23, 0)) },
            operationTimes =
                List(5) { TimeRange(LocalTime.of(7, 0), LocalTime.of(17, 0)) } + listOf(null, null),
        )
    private val daycare2 =
        DevDaycare(
            areaId = area.id,
            name = "EO-lafka A",
            type = setOf(CareType.PRESCHOOL, CareType.CENTRE),
            dailyPreschoolTime = TimeRange(LocalTime.of(9, 0), LocalTime.of(13, 0)),
        )
    private val group1 =
        DevDaycareGroup(daycareId = daycare1.id, name = "Ryhmä A1", aromiCustomerId = "PvkA_PK")
    private val group2 =
        DevDaycareGroup(daycareId = daycare1.id, name = "Ryhmä A2", aromiCustomerId = "PvkA_EO")
    private val group3 =
        DevDaycareGroup(daycareId = daycare1.id, name = "Väistöryhmä", aromiCustomerId = null)
    private val groupEO =
        DevDaycareGroup(daycareId = daycare2.id, name = "Ryhmä E1", aromiCustomerId = "EolA_EO")
    private val groupEO2 =
        DevDaycareGroup(daycareId = daycare2.id, name = "Ryhmä E2", aromiCustomerId = "EolA_EO")
    private val groupEO3 =
        DevDaycareGroup(daycareId = daycare2.id, name = "Ryhmä E3", aromiCustomerId = "EolA_PK")

    @BeforeEach
    fun setup() {
        db.transaction { tx ->
            tx.insert(admin)
            tx.insert(area)
            tx.insert(daycare1)
            tx.insert(daycare2)
            tx.insertServiceNeedOption(snDefaultDaycare)
            listOf(groupEO, groupEO2, groupEO3, group1, group2, group3).forEach { tx.insert(it) }
        }
    }

    @Test
    fun `shift care attendance prediction works`() {
        // both intermittent and full shift care service needs have attendance on weekends and
        // holidays
        val placementEnd = LocalDate.of(2027, 7, 31)
        db.transaction { tx ->
            val sannaTestPerson =
                DevPerson(
                    firstName = "Sanna",
                    lastName = "Satunnainen",
                    ssn = null,
                    dateOfBirth = LocalDate.of(2019, 3, 1),
                    id = PersonId(UUID.fromString("2d2afda1-2319-4d7c-a8b6-47045a5c82c2")),
                )
            val teemuTestPerson =
                DevPerson(
                    firstName = "Teemu",
                    lastName = "Täysivuoroinen",
                    ssn = null,
                    dateOfBirth = LocalDate.of(2020, 5, 1),
                    id = PersonId(UUID.fromString("b707a2ac-c411-4ac8-a38a-840d0767f8bb")),
                )

            tx.insert(sannaTestPerson, DevPersonType.CHILD).also { childId ->
                val placementId =
                    tx.insert(
                        DevPlacement(
                            childId = childId,
                            unitId = daycare1.id,
                            startDate = sannaTestPerson.dateOfBirth,
                            endDate = placementEnd,
                            type = PlacementType.DAYCARE,
                        )
                    )
                tx.insert(
                    DevServiceNeed(
                        placementId = placementId,
                        startDate = sannaTestPerson.dateOfBirth,
                        endDate = placementEnd,
                        shiftCare = ShiftCareType.INTERMITTENT,
                        optionId = snDefaultDaycare.id,
                        confirmedBy = admin.evakaUserId,
                    )
                )

                tx.insert(
                    DevDaycareGroupPlacement(
                        daycarePlacementId = placementId,
                        daycareGroupId = group2.id,
                        startDate = sannaTestPerson.dateOfBirth,
                        endDate = placementEnd,
                    )
                )
                tx.insert(
                    DevReservation(
                        childId = childId,
                        date = LocalDate.of(2024, 12, 2),
                        startTime = LocalTime.of(8, 0),
                        endTime = LocalTime.of(11, 0),
                        createdBy = admin.evakaUserId,
                    )
                )

                tx.insert(
                    DevAbsence(
                        childId = childId,
                        date = LocalDate.of(2024, 12, 3),
                        absenceCategory = AbsenceCategory.BILLABLE,
                    )
                )
            }

            tx.insert(teemuTestPerson, DevPersonType.CHILD).also { childId ->
                val placementId =
                    tx.insert(
                        DevPlacement(
                            childId = childId,
                            unitId = daycare1.id,
                            startDate = teemuTestPerson.dateOfBirth,
                            endDate = placementEnd,
                            type = PlacementType.DAYCARE,
                        )
                    )
                tx.insert(
                    DevServiceNeed(
                        placementId = placementId,
                        startDate = teemuTestPerson.dateOfBirth,
                        endDate = placementEnd,
                        shiftCare = ShiftCareType.FULL,
                        optionId = snDefaultDaycare.id,
                        confirmedBy = admin.evakaUserId,
                    )
                )

                tx.insert(
                    DevDaycareGroupPlacement(
                        daycarePlacementId = placementId,
                        daycareGroupId = group1.id,
                        startDate = teemuTestPerson.dateOfBirth,
                        endDate = placementEnd,
                    )
                )
                tx.insert(
                    DevReservation(
                        childId = childId,
                        date = LocalDate.of(2024, 12, 2),
                        startTime = LocalTime.of(18, 0),
                        endTime = LocalTime.of(23, 0),
                        createdBy = admin.evakaUserId,
                    )
                )

                tx.insert(
                    DevReservation(
                        childId = childId,
                        date = LocalDate.of(2024, 12, 2),
                        startTime = LocalTime.of(8, 0),
                        endTime = LocalTime.of(11, 0),
                        createdBy = admin.evakaUserId,
                    )
                )

                tx.insert(
                    DevAbsence(
                        childId = childId,
                        date = LocalDate.of(2024, 12, 4),
                        absenceCategory = AbsenceCategory.BILLABLE,
                    )
                )
            }
        }

        assertEquals(
            getExpectedCsv("aromi/shift_care_cases.csv"),
            getMealOrders(FiniteDateRange(LocalDate.of(2024, 11, 30), LocalDate.of(2024, 12, 8))),
        )
    }

    @Test
    fun `backup care attendance prediction works`() {
        // backup care changes group details and bc in a non-aromi group removes attendance entry
        val placementEnd = LocalDate.of(2027, 7, 31)
        db.transaction { tx ->
            val valtoTestPerson =
                DevPerson(
                    firstName = "Valto",
                    lastName = "Varasijoittuja",
                    ssn = null,
                    dateOfBirth = LocalDate.of(2019, 3, 1),
                    id = PersonId(UUID.fromString("2d2afda1-2319-4d7c-a8b6-47045a5c82c2")),
                )

            tx.insert(valtoTestPerson, DevPersonType.CHILD).also { childId ->
                val placementId =
                    tx.insert(
                        DevPlacement(
                            childId = childId,
                            unitId = daycare1.id,
                            startDate = valtoTestPerson.dateOfBirth,
                            endDate = placementEnd,
                            type = PlacementType.DAYCARE,
                        )
                    )
                tx.insert(
                    DevServiceNeed(
                        placementId = placementId,
                        startDate = valtoTestPerson.dateOfBirth,
                        endDate = placementEnd,
                        shiftCare = ShiftCareType.NONE,
                        optionId = snDefaultDaycare.id,
                        confirmedBy = admin.evakaUserId,
                    )
                )

                tx.insert(
                    DevDaycareGroupPlacement(
                        daycarePlacementId = placementId,
                        daycareGroupId = group2.id,
                        startDate = valtoTestPerson.dateOfBirth,
                        endDate = placementEnd,
                    )
                )
                tx.insert(
                    DevReservation(
                        childId = childId,
                        date = LocalDate.of(2024, 12, 2),
                        startTime = LocalTime.of(8, 0),
                        endTime = LocalTime.of(11, 0),
                        createdBy = admin.evakaUserId,
                    )
                )

                tx.insert(
                    DevAbsence(
                        childId = childId,
                        date = LocalDate.of(2024, 12, 3),
                        absenceCategory = AbsenceCategory.BILLABLE,
                    )
                )

                tx.insert(
                    DevBackupCare(
                        childId = childId,
                        unitId = daycare2.id,
                        groupId = groupEO.id,
                        period =
                            FiniteDateRange(LocalDate.of(2024, 12, 2), LocalDate.of(2024, 12, 3)),
                    )
                )

                tx.insert(
                    DevBackupCare(
                        childId = childId,
                        unitId = daycare1.id,
                        groupId = group3.id,
                        period =
                            FiniteDateRange(LocalDate.of(2024, 12, 5), LocalDate.of(2024, 12, 5)),
                    )
                )
            }
        }

        assertEquals(
            getExpectedCsv("aromi/backup_care_cases.csv"),
            getMealOrders(FiniteDateRange(LocalDate.of(2024, 11, 30), LocalDate.of(2024, 12, 8))),
        )
    }

    @Test
    fun `basic attendance prediction works`() {
        db.transaction { tx ->
            val dateOfBirthHarri = LocalDate.of(2021, 7, 2)
            val dateOfBirthElina = LocalDate.of(2020, 3, 1)
            val placementEnd = LocalDate.of(2027, 7, 31)
            tx.insert(
                    DevPerson(
                        lastName = "Hämäläinen",
                        firstName = "Harri",
                        ssn = null,
                        dateOfBirth = dateOfBirthHarri,
                        id = PersonId(UUID.fromString("f800197e-336c-4e98-ac2c-da305e3d605b")),
                    ),
                    DevPersonType.CHILD,
                )
                .also { childId ->
                    val placementId =
                        tx.insert(
                            DevPlacement(
                                childId = childId,
                                unitId = daycare1.id,
                                startDate = dateOfBirthHarri,
                                endDate = placementEnd,
                            )
                        )

                    tx.insert(
                        DevServiceNeed(
                            placementId = placementId,
                            startDate = dateOfBirthHarri,
                            endDate = placementEnd,
                            optionId = snDefaultDaycare.id,
                            confirmedBy = admin.evakaUserId,
                        )
                    )
                    tx.insert(
                        DevDaycareGroupPlacement(
                            daycarePlacementId = placementId,
                            daycareGroupId = group1.id,
                            startDate = dateOfBirthHarri,
                            endDate = LocalDate.of(2024, 12, 4),
                        )
                    )
                    tx.insert(
                        DevDaycareGroupPlacement(
                            daycarePlacementId = placementId,
                            daycareGroupId = group2.id,
                            startDate = LocalDate.of(2024, 12, 5),
                            endDate = placementEnd,
                        )
                    )
                    tx.insert(
                        DevReservation(
                            childId = childId,
                            date = LocalDate.of(2024, 12, 2),
                            startTime = LocalTime.of(8, 0),
                            endTime = LocalTime.of(16, 0),
                            createdBy = admin.evakaUserId,
                        )
                    )
                    tx.insert(
                        DevReservation(
                            childId = childId,
                            date = LocalDate.of(2024, 12, 3),
                            startTime = LocalTime.of(8, 0),
                            endTime = LocalTime.of(12, 0),
                            createdBy = admin.evakaUserId,
                        )
                    )
                    tx.insert(
                        DevReservation(
                            childId = childId,
                            date = LocalDate.of(2024, 12, 3),
                            startTime = LocalTime.of(14, 0),
                            endTime = LocalTime.of(16, 0),
                            createdBy = admin.evakaUserId,
                        )
                    )
                    tx.insert(
                        DevAbsence(
                            childId = childId,
                            date = LocalDate.of(2024, 12, 4),
                            absenceCategory = AbsenceCategory.BILLABLE,
                        )
                    )
                }
            tx.insert(
                    DevPerson(
                        lastName = "Eskelinen",
                        firstName = "Elina",
                        ssn = null,
                        dateOfBirth = dateOfBirthElina,
                        id = PersonId(UUID.fromString("7eb09ea9-0bb1-4f23-a7be-67f069168771")),
                    ),
                    DevPersonType.CHILD,
                )
                .also { childId ->
                    val placementId =
                        tx.insert(
                            DevPlacement(
                                childId = childId,
                                unitId = daycare2.id,
                                startDate = dateOfBirthElina,
                                endDate = placementEnd,
                            )
                        )
                    tx.insert(
                        DevServiceNeed(
                            placementId = placementId,
                            startDate = dateOfBirthElina,
                            endDate = placementEnd,
                            shiftCare = ShiftCareType.FULL,
                            optionId = snDefaultDaycare.id,
                            confirmedBy = admin.evakaUserId,
                        )
                    )
                    tx.insert(
                        DevDaycareGroupPlacement(
                            daycarePlacementId = placementId,
                            daycareGroupId = groupEO.id,
                            startDate = dateOfBirthElina,
                            endDate = LocalDate.of(2024, 12, 2),
                        )
                    )
                    tx.insert(
                        DevDaycareGroupPlacement(
                            daycarePlacementId = placementId,
                            daycareGroupId = groupEO2.id,
                            startDate = LocalDate.of(2024, 12, 3),
                            endDate = placementEnd,
                        )
                    )
                    tx.insert(
                        DevReservation(
                            childId = childId,
                            date = LocalDate.of(2024, 12, 2),
                            startTime = LocalTime.of(8, 0),
                            endTime = LocalTime.of(16, 0),
                            createdBy = admin.evakaUserId,
                        )
                    )
                    tx.insert(
                        DevReservation(
                            childId = childId,
                            date = LocalDate.of(2024, 12, 3),
                            startTime = LocalTime.of(8, 0),
                            endTime = LocalTime.of(16, 0),
                            createdBy = admin.evakaUserId,
                        )
                    )
                    tx.insert(
                        DevAbsence(
                            childId = childId,
                            date = LocalDate.of(2024, 12, 4),
                            absenceCategory = AbsenceCategory.BILLABLE,
                        )
                    )
                }
        }

        assertEquals(
            getExpectedCsv("aromi/basic_cases.csv"),
            getMealOrders(FiniteDateRange(LocalDate.of(2024, 11, 30), LocalDate.of(2024, 12, 8))),
        )
    }

    @Test
    fun `preschool attendance prediction works, daily preschool times, term break`() {
        val preschoolTerm = FiniteDateRange(LocalDate.of(2024, 8, 3), LocalDate.of(2025, 5, 31))
        val termBreak = FiniteDateRange(LocalDate.of(2024, 12, 2), LocalDate.of(2024, 12, 3))
        val testPersonPawol =
            DevPerson(
                firstName = "Pawol",
                lastName = "Pelkkä-Esiopetus",
                ssn = null,
                dateOfBirth = LocalDate.of(2019, 3, 1),
                id = PersonId(UUID.fromString("94720a00-e83f-45ac-a51c-016404a52d37")),
            )

        val testPersonTytti =
            DevPerson(
                firstName = "Tytti",
                lastName = "Täydentävä",
                ssn = null,
                dateOfBirth = LocalDate.of(2019, 5, 13),
                id = PersonId(UUID.fromString("2b9dc19f-b12f-4008-9b92-0b6bb9dbaeef")),
            )
        db.transaction { tx ->
            tx.insertPreschoolTerm(
                preschoolTerm,
                preschoolTerm,
                preschoolTerm,
                preschoolTerm,
                DateSet.of(termBreak),
            )

            tx.insert(testPersonPawol, DevPersonType.CHILD).also { childId ->
                val placement =
                    DevPlacement(
                        childId = childId,
                        unitId = daycare2.id,
                        startDate = LocalDate.of(2024, 8, 5),
                        endDate = LocalDate.of(2025, 5, 5),
                        type = PlacementType.PRESCHOOL,
                    )
                tx.insert(placement)

                tx.insert(
                    DevDaycareGroupPlacement(
                        daycarePlacementId = placement.id,
                        daycareGroupId = groupEO2.id,
                        startDate = placement.startDate,
                        endDate = placement.endDate,
                    )
                )

                tx.insert(
                    DevAbsence(
                        childId = childId,
                        date = LocalDate.of(2024, 12, 3),
                        absenceCategory = AbsenceCategory.NONBILLABLE,
                    )
                )
            }

            tx.insert(testPersonTytti, DevPersonType.CHILD).also { childId ->
                val placement =
                    DevPlacement(
                        childId = childId,
                        unitId = daycare2.id,
                        startDate = LocalDate.of(2024, 8, 5),
                        endDate = LocalDate.of(2025, 5, 5),
                        type = PlacementType.PRESCHOOL_DAYCARE,
                    )
                tx.insert(placement)

                tx.insert(
                    DevDaycareGroupPlacement(
                        daycarePlacementId = placement.id,
                        daycareGroupId = groupEO2.id,
                        startDate = placement.startDate,
                        endDate = placement.endDate,
                    )
                )
                tx.insert(
                    DevAbsence(
                        childId = childId,
                        date = LocalDate.of(2024, 12, 4),
                        absenceCategory = AbsenceCategory.NONBILLABLE,
                    )
                )
                tx.insert(
                    DevAbsence(
                        childId = childId,
                        date = LocalDate.of(2024, 12, 4),
                        absenceCategory = AbsenceCategory.BILLABLE,
                    )
                )

                tx.insert(
                    DevReservation(
                        childId = childId,
                        date = LocalDate.of(2024, 12, 3),
                        startTime = LocalTime.of(8, 0),
                        endTime = LocalTime.of(13, 0),
                        createdBy = admin.evakaUserId,
                    )
                )
                tx.insert(
                    DevReservation(
                        childId = childId,
                        date = LocalDate.of(2024, 12, 3),
                        startTime = LocalTime.of(14, 0),
                        endTime = LocalTime.of(17, 0),
                        createdBy = admin.evakaUserId,
                    )
                )
            }
        }

        assertEquals(
            getExpectedCsv("aromi/preschool_cases.csv"),
            getMealOrders(FiniteDateRange(LocalDate.of(2024, 11, 30), LocalDate.of(2024, 12, 8))),
        )
    }

    @Test
    fun `closing unit and group attendance prediction works`() {
        // unit closes 2024-12-04,
        // one group closes 2024-12-03, another stays open,
        // report range 2024-11-30 - 2024-12-08

        val suleimanTestPerson =
            DevPerson(
                firstName = "Suleiman",
                lastName = "Sulkeutuva",
                ssn = null,
                dateOfBirth = LocalDate.of(2022, 1, 1),
                id = PersonId(UUID.fromString("73304871-cae2-42dc-a347-7409351a5c73")),
            )
        val tariqTestPerson =
            DevPerson(
                firstName = "Tariq",
                lastName = "Tavallinen",
                ssn = null,
                dateOfBirth = LocalDate.of(2022, 1, 1),
                id = PersonId(UUID.fromString("5054840e-6da7-4f0c-86a7-4d56ee399880")),
            )

        val closingUnit =
            DevDaycare(
                areaId = area.id,
                name = "Sulkeutuva yksikkö",
                type = setOf(CareType.CENTRE),
                dailyPreschoolTime = TimeRange(LocalTime.of(9, 0), LocalTime.of(13, 0)),
                openingDate = LocalDate.of(2024, 1, 1),
                closingDate = LocalDate.of(2024, 12, 4),
                operationTimes =
                    List(5) { TimeRange(LocalTime.of(7, 0), LocalTime.of(17, 0)) } +
                        listOf(null, null),
            )
        val closingGroup =
            DevDaycareGroup(
                daycareId = closingUnit.id,
                name = "Closing group",
                aromiCustomerId = "CC_PK",
                startDate = LocalDate.of(2024, 1, 1),
                endDate = LocalDate.of(2024, 12, 3),
            )
        val regularGroup =
            DevDaycareGroup(
                daycareId = closingUnit.id,
                name = "Regular group",
                aromiCustomerId = "CR_PK",
                startDate = LocalDate.of(2024, 1, 1),
                endDate = LocalDate.of(2024, 12, 4),
            )

        db.transaction { tx ->
            tx.insert(closingUnit)
            tx.insert(closingGroup)
            tx.insert(regularGroup)

            tx.insert(suleimanTestPerson, DevPersonType.CHILD).also { childId ->
                val placement =
                    DevPlacement(
                        childId = childId,
                        unitId = closingUnit.id,
                        type = PlacementType.DAYCARE,
                        startDate = LocalDate.of(2024, 11, 30),
                        endDate = LocalDate.of(2024, 12, 31),
                    )
                tx.insert(placement)
                tx.insert(
                    DevDaycareGroupPlacement(
                        daycarePlacementId = placement.id,
                        startDate = placement.startDate,
                        endDate = placement.endDate,
                        daycareGroupId = closingGroup.id,
                    )
                )
            }
            tx.insert(tariqTestPerson, DevPersonType.CHILD).also { childId ->
                val placement =
                    DevPlacement(
                        childId = childId,
                        unitId = closingUnit.id,
                        type = PlacementType.DAYCARE,
                        startDate = LocalDate.of(2024, 11, 30),
                        endDate = LocalDate.of(2024, 12, 31),
                    )
                tx.insert(placement)
                tx.insert(
                    DevDaycareGroupPlacement(
                        daycarePlacementId = placement.id,
                        startDate = placement.startDate,
                        endDate = placement.endDate,
                        daycareGroupId = regularGroup.id,
                    )
                )
            }
        }

        assertEquals(
            getExpectedCsv("aromi/unit_closing_cases.csv"),
            getMealOrders(FiniteDateRange(LocalDate.of(2024, 11, 30), LocalDate.of(2024, 12, 8))),
        )
    }

    @Test
    fun `breakfast exceptions work`() {
        db.transaction { tx ->
            val dateOfBirthHarri = LocalDate.of(2021, 7, 2)
            val placementEnd = LocalDate.of(2027, 7, 31)
            tx.insert(
                    DevPerson(
                        lastName = "Hämäläinen",
                        firstName = "Harri",
                        ssn = null,
                        dateOfBirth = dateOfBirthHarri,
                        id = PersonId(UUID.fromString("f800197e-336c-4e98-ac2c-da305e3d605b")),
                    ),
                    DevPersonType.RAW_ROW,
                )
                .also { childId ->
                    tx.insert(DevChild(id = childId, participatesInBreakfast = false))
                    val placementId =
                        tx.insert(
                            DevPlacement(
                                childId = childId,
                                unitId = daycare1.id,
                                startDate = dateOfBirthHarri,
                                endDate = placementEnd,
                            )
                        )

                    tx.insert(
                        DevServiceNeed(
                            placementId = placementId,
                            startDate = dateOfBirthHarri,
                            endDate = placementEnd,
                            optionId = snDefaultDaycare.id,
                            confirmedBy = admin.evakaUserId,
                        )
                    )
                    tx.insert(
                        DevDaycareGroupPlacement(
                            daycarePlacementId = placementId,
                            daycareGroupId = group2.id,
                            startDate = dateOfBirthHarri,
                            endDate = placementEnd,
                        )
                    )
                    // 2024-11-28 expectation 10:00-14:00
                    tx.insert(
                        DevReservation(
                            childId = childId,
                            date = LocalDate.of(2024, 11, 28),
                            startTime = LocalTime.of(9, 0),
                            endTime = LocalTime.of(14, 0),
                            createdBy = admin.evakaUserId,
                        )
                    )

                    // 2024-11-29 expectation -
                    tx.insert(
                        DevReservation(
                            childId = childId,
                            date = LocalDate.of(2024, 11, 29),
                            startTime = LocalTime.of(7, 0),
                            endTime = LocalTime.of(8, 0),
                            createdBy = admin.evakaUserId,
                        )
                    )
                    tx.insert(
                        DevReservation(
                            childId = childId,
                            date = LocalDate.of(2024, 11, 29),
                            startTime = LocalTime.of(9, 0),
                            endTime = LocalTime.of(10, 0),
                            createdBy = admin.evakaUserId,
                        )
                    )
                    // 2024-12-02 expectation 10:00-16:00
                    tx.insert(
                        DevReservation(
                            childId = childId,
                            date = LocalDate.of(2024, 12, 2),
                            startTime = LocalTime.of(7, 0),
                            endTime = LocalTime.of(8, 0),
                            createdBy = admin.evakaUserId,
                        )
                    )
                    tx.insert(
                        DevReservation(
                            childId = childId,
                            date = LocalDate.of(2024, 12, 2),
                            startTime = LocalTime.of(9, 0),
                            endTime = LocalTime.of(16, 0),
                            createdBy = admin.evakaUserId,
                        )
                    )
                    // 2024-12-03 expectation 10:00-16:00
                    tx.insert(
                        DevReservation(
                            childId = childId,
                            date = LocalDate.of(2024, 12, 3),
                            startTime = LocalTime.of(8, 0),
                            endTime = LocalTime.of(9, 0),
                            createdBy = admin.evakaUserId,
                        )
                    )
                    tx.insert(
                        DevReservation(
                            childId = childId,
                            date = LocalDate.of(2024, 12, 3),
                            startTime = LocalTime.of(10, 0),
                            endTime = LocalTime.of(16, 0),
                            createdBy = admin.evakaUserId,
                        )
                    )
                    // 2024-12-04 expectation 10:00-11:00 + 12:00-16:00
                    tx.insert(
                        DevReservation(
                            childId = childId,
                            date = LocalDate.of(2024, 12, 4),
                            startTime = LocalTime.of(8, 0),
                            endTime = LocalTime.of(11, 0),
                            createdBy = admin.evakaUserId,
                        )
                    )
                    tx.insert(
                        DevReservation(
                            childId = childId,
                            date = LocalDate.of(2024, 12, 4),
                            startTime = LocalTime.of(12, 0),
                            endTime = LocalTime.of(16, 0),
                            createdBy = admin.evakaUserId,
                        )
                    )
                    // 2024-12-05 expectation 10:00-23:59
                }
        }
        assertEquals(
            getExpectedCsv("aromi/breakfast_exception_cases.csv"),
            getMealOrders(FiniteDateRange(LocalDate.of(2024, 11, 28), LocalDate.of(2024, 12, 8))),
        )
    }

    @Test
    fun `forbidden director`() {
        val director = db.transaction { tx ->
            val employee = DevEmployee(roles = setOf(UserRole.DIRECTOR))
            tx.insert(employee)
            employee.user
        }

        assertThrows<Forbidden> { getMealOrders(director, FiniteDateRange.ofYear(2024)) }
    }

    @Test
    fun `forbidden staff`() {
        val staff = db.transaction { tx ->
            val employee = DevEmployee()
            tx.insert(
                employee,
                unitRoles = mapOf(daycare1.id to UserRole.STAFF),
                groupAcl = mapOf(daycare1.id to listOf(group1.id, group2.id)),
            )
            employee.user
        }

        assertThrows<Forbidden> { getMealOrders(staff, FiniteDateRange.ofYear(2024)) }
    }

    @Test
    fun `won't create an empty csv`() {
        assertThrows<IllegalStateException> { getMealOrders(FiniteDateRange.ofYear(2024)) }
    }

    private fun getMealOrders(range: FiniteDateRange): String = getMealOrders(admin.user, range)

    private fun getMealOrders(user: AuthenticatedUser.Employee, range: FiniteDateRange): String =
        aromiController
            .getMealOrders(dbInstance(), user, clock, range.start, range.end)
            .toString(StandardCharsets.ISO_8859_1)

    private fun getExpectedCsv(path: String): String =
        ClassPathResource(path).getContentAsString(StandardCharsets.ISO_8859_1)
}
