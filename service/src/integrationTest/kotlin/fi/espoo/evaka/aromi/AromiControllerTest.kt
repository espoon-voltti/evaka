// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.aromi

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.absence.AbsenceCategory
import fi.espoo.evaka.daycare.CareType
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.serviceneed.ShiftCareType
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.dev.DevAbsence
import fi.espoo.evaka.shared.dev.DevBackupCare
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.DevDaycareGroupPlacement
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.DevReservation
import fi.espoo.evaka.shared.dev.DevServiceNeed
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.dev.insertServiceNeedOption
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.Forbidden
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.shared.domain.TimeRange
import fi.espoo.evaka.snDefaultDaycare
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

    private lateinit var admin: AuthenticatedUser.Employee
    private lateinit var today: LocalDate
    private lateinit var clock: EvakaClock

    private lateinit var unitId: DaycareId
    private lateinit var unitId2: DaycareId
    private lateinit var groupId1: GroupId
    private lateinit var groupId2: GroupId

    @BeforeEach
    fun setup() {
        db.transaction { tx -> tx.insertServiceNeedOption(snDefaultDaycare) }
        admin =
            db.transaction { tx ->
                val employee = DevEmployee(roles = setOf(UserRole.ADMIN))
                tx.insert(employee)
                employee.user
            }
        today = LocalDate.of(2024, 11, 30)
        clock = MockEvakaClock(HelsinkiDateTime.of(today, LocalTime.of(12, 18)))

        val areaId = db.transaction { tx -> tx.insert(DevCareArea()) }
        unitId =
            db.transaction { tx ->
                tx.insert(
                    DevDaycare(
                        areaId = areaId,
                        name = "Päiväkoti A",
                        shiftCareOpenOnHolidays = true,
                        shiftCareOperationTimes =
                            List(7) { TimeRange(LocalTime.of(7, 0), LocalTime.of(17, 0)) },
                        operationTimes =
                            List(5) { TimeRange(LocalTime.of(7, 0), LocalTime.of(17, 0)) } +
                                listOf(null, null),
                    )
                )
            }
        unitId2 =
            db.transaction { tx ->
                tx.insert(
                    DevDaycare(
                        areaId = areaId,
                        name = "EO-lafka A",
                        type = setOf(CareType.PRESCHOOL, CareType.CENTRE),
                    )
                )
            }
        groupId1 =
            db.transaction { tx ->
                tx.insert(
                    DevDaycareGroup(
                        daycareId = unitId,
                        name = "Ryhmä A1",
                        aromiCustomerId = "PvkA_PK",
                    )
                )
            }
        groupId2 =
            db.transaction { tx ->
                tx.insert(
                    DevDaycareGroup(
                        daycareId = unitId,
                        name = "Ryhmä A2",
                        aromiCustomerId = "PvkA_EO",
                    )
                )
            }
        val groupId3 =
            db.transaction { tx ->
                tx.insert(
                    DevDaycareGroup(daycareId = unitId, name = "Ryhmä A3", aromiCustomerId = null)
                )
            }
        db.transaction { tx ->
            val eoGroupAId =
                tx.insert(
                    DevDaycareGroup(
                        daycareId = unitId2,
                        name = "Ryhmä E1",
                        aromiCustomerId = "EolA_EO",
                    )
                )
            val eoGroupBId =
                tx.insert(
                    DevDaycareGroup(
                        daycareId = unitId2,
                        name = "Ryhmä E2",
                        aromiCustomerId = "EolA_EO",
                    )
                )

            val eoGroupCId =
                tx.insert(
                    DevDaycareGroup(
                        daycareId = unitId2,
                        name = "Ryhmä E3",
                        aromiCustomerId = "EolA_PK",
                    )
                )

            val dateOfBirthHarri = LocalDate.of(2021, 7, 2)
            val dateOfBirthElina = LocalDate.of(2020, 3, 1)
            val dateOfBirthSanna = LocalDate.of(2019, 3, 1)
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
                                unitId = unitId,
                                startDate = dateOfBirthHarri,
                                endDate = placementEnd,
                            )
                        )

                    tx.insert(
                        DevServiceNeed(
                            placementId = placementId,
                            startDate = dateOfBirthHarri,
                            endDate = placementEnd,
                            shiftCare = ShiftCareType.FULL,
                            optionId = snDefaultDaycare.id,
                            confirmedBy = admin.evakaUserId,
                        )
                    )
                    tx.insert(
                        DevDaycareGroupPlacement(
                            daycarePlacementId = placementId,
                            daycareGroupId = groupId1,
                            startDate = dateOfBirthHarri,
                            endDate = LocalDate.of(2024, 12, 4),
                        )
                    )
                    tx.insert(
                        DevDaycareGroupPlacement(
                            daycarePlacementId = placementId,
                            daycareGroupId = groupId2,
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
                    tx.insert(
                        DevBackupCare(
                            childId = childId,
                            unitId = unitId2,
                            groupId = eoGroupCId,
                            period =
                                FiniteDateRange(
                                    LocalDate.of(2024, 12, 5),
                                    LocalDate.of(2024, 12, 5),
                                ),
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
                                unitId = unitId2,
                                startDate = dateOfBirthElina,
                                endDate = placementEnd,
                            )
                        )
                    tx.insert(
                        DevDaycareGroupPlacement(
                            daycarePlacementId = placementId,
                            daycareGroupId = eoGroupAId,
                            startDate = dateOfBirthElina,
                            endDate = LocalDate.of(2024, 12, 2),
                        )
                    )
                    tx.insert(
                        DevDaycareGroupPlacement(
                            daycarePlacementId = placementId,
                            daycareGroupId = eoGroupBId,
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

            tx.insert(
                    DevPerson(
                        lastName = "Sanna",
                        firstName = "Satunnainen",
                        ssn = null,
                        dateOfBirth = dateOfBirthSanna,
                        id = PersonId(UUID.fromString("2d2afda1-2319-4d7c-a8b6-47045a5c82c2")),
                    ),
                    DevPersonType.CHILD,
                )
                .also { childId ->
                    val placementId =
                        tx.insert(
                            DevPlacement(
                                childId = childId,
                                unitId = unitId2,
                                startDate = dateOfBirthSanna,
                                endDate = placementEnd,
                                type = PlacementType.PRESCHOOL_DAYCARE,
                            )
                        )
                    tx.insert(
                        DevServiceNeed(
                            placementId = placementId,
                            startDate = dateOfBirthSanna,
                            endDate = placementEnd,
                            shiftCare = ShiftCareType.INTERMITTENT,
                            optionId = snDefaultDaycare.id,
                            confirmedBy = admin.evakaUserId,
                        )
                    )

                    tx.insert(
                        DevDaycareGroupPlacement(
                            daycarePlacementId = placementId,
                            daycareGroupId = eoGroupBId,
                            startDate = dateOfBirthSanna,
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
                        DevAbsence(
                            childId = childId,
                            date = LocalDate.of(2024, 12, 3),
                            absenceCategory = AbsenceCategory.NONBILLABLE,
                        )
                    )
                    // non-relevant absence category
                    tx.insert(
                        DevAbsence(
                            childId = childId,
                            date = LocalDate.of(2024, 12, 4),
                            absenceCategory = AbsenceCategory.NONBILLABLE,
                        )
                    )
                    // bc to a non-aromi group
                    tx.insert(
                        DevBackupCare(
                            childId = childId,
                            unitId = unitId,
                            groupId = groupId3,
                            period =
                                FiniteDateRange(
                                    LocalDate.of(2024, 12, 5),
                                    LocalDate.of(2024, 12, 5),
                                ),
                        )
                    )
                    tx.insert(
                        DevBackupCare(
                            childId = childId,
                            unitId = unitId,
                            groupId = groupId2,
                            period =
                                FiniteDateRange(
                                    LocalDate.of(2024, 12, 6),
                                    LocalDate.of(2024, 12, 8),
                                ),
                        )
                    )
                }
        }
    }

    @Test
    fun `ok admin`() {
        assertEquals(
            getExpectedCsv("aromi/ok-admin.csv"),
            getMealOrders(FiniteDateRange(LocalDate.of(2024, 11, 30), LocalDate.of(2024, 12, 8))),
        )
    }

    @Test
    fun `forbidden director`() {
        val director =
            db.transaction { tx ->
                val employee = DevEmployee(roles = setOf(UserRole.DIRECTOR))
                tx.insert(employee)
                employee.user
            }

        assertThrows<Forbidden> { getMealOrders(director, FiniteDateRange.ofYear(2024)) }
    }

    @Test
    fun `forbidden staff`() {
        val staff =
            db.transaction { tx ->
                val employee = DevEmployee()
                tx.insert(
                    employee,
                    unitRoles = mapOf(unitId to UserRole.STAFF),
                    groupAcl = mapOf(unitId to listOf(groupId1, groupId2)),
                )
                employee.user
            }

        assertThrows<Forbidden> { getMealOrders(staff, FiniteDateRange.ofYear(2024)) }
    }

    private fun getMealOrders(range: FiniteDateRange): String = getMealOrders(admin, range)

    private fun getMealOrders(user: AuthenticatedUser.Employee, range: FiniteDateRange): String =
        aromiController
            .getMealOrders(dbInstance(), user, clock, range.start, range.end)
            .toString(StandardCharsets.ISO_8859_1)

    private fun getExpectedCsv(path: String): String =
        ClassPathResource(path).getContentAsString(StandardCharsets.ISO_8859_1)
}
