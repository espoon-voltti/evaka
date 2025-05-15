// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.absence

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.absence.application.AbsenceApplication
import fi.espoo.evaka.absence.application.AbsenceApplicationControllerCitizen
import fi.espoo.evaka.absence.application.AbsenceApplicationControllerEmployee
import fi.espoo.evaka.absence.application.AbsenceApplicationCreateRequest
import fi.espoo.evaka.absence.application.AbsenceApplicationRejectRequest
import fi.espoo.evaka.absence.application.AbsenceApplicationStatus
import fi.espoo.evaka.absence.application.AbsenceApplicationSummary
import fi.espoo.evaka.absence.application.selectAbsenceApplication
import fi.espoo.evaka.pis.PersonNameDetails
import fi.espoo.evaka.pis.service.insertGuardian
import fi.espoo.evaka.shared.AbsenceApplicationId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.CitizenAuthLevel
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.Forbidden
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.toEvakaUser
import fi.espoo.evaka.user.EvakaUserType
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired

class AbsenceApplicationControllersTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired
    private lateinit var absenceApplicationControllerEmployee: AbsenceApplicationControllerEmployee
    @Autowired
    private lateinit var absenceApplicationControllerCitizen: AbsenceApplicationControllerCitizen

    @Nested
    inner class CrudTest {
        private val area = DevCareArea()
        private val unit = DevDaycare(areaId = area.id)
        private val unitSupervisor = DevEmployee()
        private val child = DevPerson()
        private val adult = DevPerson()

        private val citizenUser = adult.user(CitizenAuthLevel.STRONG)
        private val employeeUser = unitSupervisor.user
        private val clock =
            MockEvakaClock(HelsinkiDateTime.of(LocalDate.of(2022, 8, 10), LocalTime.of(8, 0)))

        @BeforeEach
        fun setup() {
            db.transaction { tx ->
                tx.insert(area)
                tx.insert(unit)
                tx.insert(unitSupervisor, unitRoles = mapOf(unit.id to UserRole.UNIT_SUPERVISOR))
                tx.insert(child, DevPersonType.CHILD)
                tx.insert(adult, DevPersonType.ADULT)
                tx.insertGuardian(adult.id, child.id)
                tx.insert(
                    DevPlacement(
                        childId = child.id,
                        unitId = unit.id,
                        startDate = LocalDate.of(2022, 1, 1),
                        endDate = LocalDate.of(2022, 12, 31),
                    )
                )
            }
        }

        @Test
        fun `accepted flow`() {
            val id =
                absenceApplicationControllerCitizen.postAbsenceApplication(
                    dbInstance(),
                    adult.user(CitizenAuthLevel.STRONG),
                    clock,
                    AbsenceApplicationCreateRequest(
                        childId = child.id,
                        startDate = LocalDate.of(2022, 1, 1),
                        endDate = LocalDate.of(2022, 1, 1),
                        description = "Lapinreissu",
                    ),
                )

            val expected1 =
                AbsenceApplicationSummary(
                    id = id,
                    createdAt = clock.now(),
                    createdBy = adult.toEvakaUser(EvakaUserType.CITIZEN),
                    child = PersonNameDetails(child.id, child.firstName, child.lastName),
                    startDate = LocalDate.of(2022, 1, 1),
                    endDate = LocalDate.of(2022, 1, 1),
                    description = "Lapinreissu",
                    status = AbsenceApplicationStatus.WAITING_DECISION,
                    decidedAt = null,
                    decidedBy = null,
                    rejectedReason = null,
                )
            assertEquals(
                listOf(expected1),
                absenceApplicationControllerCitizen
                    .getAbsenceApplications(dbInstance(), citizenUser, clock, child.id)
                    .map { it.data.copy(createdAt = clock.now()) },
            )
            assertEquals(
                listOf(expected1),
                absenceApplicationControllerEmployee
                    .getAbsenceApplications(
                        dbInstance(),
                        employeeUser,
                        clock,
                        unitId = unit.id,
                        childId = null,
                        status = null,
                    )
                    .map { it.data.copy(createdAt = clock.now()) },
            )

            absenceApplicationControllerEmployee.acceptAbsenceApplication(
                dbInstance(),
                employeeUser,
                clock,
                id,
            )
            assertThrows<BadRequest> {
                absenceApplicationControllerEmployee.acceptAbsenceApplication(
                    dbInstance(),
                    employeeUser,
                    clock,
                    id,
                )
            }

            val expected2 =
                expected1.copy(
                    status = AbsenceApplicationStatus.ACCEPTED,
                    decidedAt = clock.now(),
                    decidedBy = unitSupervisor.toEvakaUser(),
                )
            assertEquals(
                listOf(expected2),
                absenceApplicationControllerEmployee
                    .getAbsenceApplications(
                        dbInstance(),
                        employeeUser,
                        clock,
                        unitId = unit.id,
                        childId = null,
                        status = null,
                    )
                    .map { it.data.copy(createdAt = clock.now()) },
            )
            assertEquals(
                listOf(
                    Absence(
                        childId = child.id,
                        date = LocalDate.of(2022, 1, 1),
                        category = AbsenceCategory.BILLABLE,
                        absenceType = AbsenceType.OTHER_ABSENCE,
                        modifiedByStaff = true,
                        modifiedAt = clock.now(),
                        belongsToQuestionnaire = false,
                    )
                ),
                db.transaction { tx ->
                    tx.getAbsencesOfChildByDate(child.id, LocalDate.of(2022, 1, 1))
                },
            )

            assertThrows<BadRequest> {
                absenceApplicationControllerCitizen.deleteAbsenceApplication(
                    dbInstance(),
                    citizenUser,
                    clock,
                    id,
                )
            }
        }

        @Test
        fun `rejected flow`() {
            val id =
                absenceApplicationControllerCitizen.postAbsenceApplication(
                    dbInstance(),
                    citizenUser,
                    clock,
                    AbsenceApplicationCreateRequest(
                        childId = child.id,
                        startDate = LocalDate.of(2022, 1, 1),
                        endDate = LocalDate.of(2022, 1, 1),
                        description = "Lapinreissu",
                    ),
                )

            val expected1 =
                AbsenceApplicationSummary(
                    id = id,
                    createdAt = clock.now(),
                    createdBy = adult.toEvakaUser(EvakaUserType.CITIZEN),
                    child = PersonNameDetails(child.id, child.firstName, child.lastName),
                    startDate = LocalDate.of(2022, 1, 1),
                    endDate = LocalDate.of(2022, 1, 1),
                    description = "Lapinreissu",
                    status = AbsenceApplicationStatus.WAITING_DECISION,
                    decidedAt = null,
                    decidedBy = null,
                    rejectedReason = null,
                )
            assertEquals(
                listOf(expected1),
                absenceApplicationControllerCitizen
                    .getAbsenceApplications(dbInstance(), citizenUser, clock, child.id)
                    .map { it.data.copy(createdAt = clock.now()) },
            )
            assertEquals(
                listOf(expected1),
                absenceApplicationControllerEmployee
                    .getAbsenceApplications(
                        dbInstance(),
                        employeeUser,
                        clock,
                        unitId = unit.id,
                        childId = null,
                        status = null,
                    )
                    .map { it.data.copy(createdAt = clock.now()) },
            )

            absenceApplicationControllerEmployee.rejectAbsenceApplication(
                dbInstance(),
                employeeUser,
                clock,
                id,
                AbsenceApplicationRejectRequest("ei käy"),
            )
            assertThrows<BadRequest> {
                absenceApplicationControllerEmployee.rejectAbsenceApplication(
                    dbInstance(),
                    employeeUser,
                    clock,
                    id,
                    AbsenceApplicationRejectRequest("ei käy"),
                )
            }

            val expected2 =
                expected1.copy(
                    status = AbsenceApplicationStatus.REJECTED,
                    rejectedReason = "ei käy",
                    decidedAt = clock.now(),
                    decidedBy = unitSupervisor.toEvakaUser(),
                )
            assertEquals(
                listOf(expected2),
                absenceApplicationControllerEmployee
                    .getAbsenceApplications(
                        dbInstance(),
                        employeeUser,
                        clock,
                        unitId = unit.id,
                        childId = null,
                        status = null,
                    )
                    .map { it.data.copy(createdAt = clock.now()) },
            )
            assertEquals(
                emptyList(),
                db.transaction { tx ->
                    tx.getAbsencesOfChildByDate(child.id, LocalDate.of(2022, 1, 1))
                },
            )

            assertThrows<BadRequest> {
                absenceApplicationControllerCitizen.deleteAbsenceApplication(
                    dbInstance(),
                    citizenUser,
                    clock,
                    id,
                )
            }
        }
    }

    @Nested
    inner class PermissionTest {
        private val area = DevCareArea()
        private val unit1 = DevDaycare(areaId = area.id)
        private val unit2 = DevDaycare(areaId = area.id)
        private val child1 = DevPerson()
        private val child2 = DevPerson()
        private val clock =
            MockEvakaClock(HelsinkiDateTime.of(LocalDate.of(2022, 8, 10), LocalTime.of(8, 0)))

        @BeforeEach
        fun setup() {
            db.transaction { tx ->
                tx.insert(area)
                tx.insert(unit1)
                tx.insert(unit2)
                tx.insert(child1, DevPersonType.CHILD)
                tx.insert(
                    DevPlacement(
                        childId = child1.id,
                        unitId = unit1.id,
                        startDate = clock.today(),
                        endDate = clock.today(),
                    )
                )
                tx.insert(child2, DevPersonType.CHILD)
                tx.insert(
                    DevPlacement(
                        childId = child2.id,
                        unitId = unit2.id,
                        startDate = clock.today(),
                        endDate = clock.today(),
                    )
                )
            }
        }

        @Test
        fun admin() {
            val admin = DevEmployee(roles = setOf(UserRole.ADMIN))
            db.transaction { tx -> tx.insert(admin) }

            assertThrows<Forbidden> { getAbsenceApplications(admin.user) }
            assertThrows<Forbidden> {
                getAbsenceApplications(
                    admin.user,
                    status = AbsenceApplicationStatus.WAITING_DECISION,
                )
            }
            assertEquals(emptyList(), getAbsenceApplications(admin.user, unitId = unit1.id))
            assertEquals(emptyList(), getAbsenceApplications(admin.user, unitId = unit2.id))
            assertEquals(emptyList(), getAbsenceApplications(admin.user, childId = child1.id))
            assertEquals(emptyList(), getAbsenceApplications(admin.user, childId = child2.id))
            assertEquals(
                emptyList(),
                getAbsenceApplications(admin.user, unitId = unit1.id, childId = child1.id),
            )
            assertEquals(
                emptyList(),
                getAbsenceApplications(admin.user, unitId = unit1.id, childId = child2.id),
            )
            assertEquals(
                emptyList(),
                getAbsenceApplications(admin.user, unitId = unit2.id, childId = child1.id),
            )
            assertEquals(
                emptyList(),
                getAbsenceApplications(admin.user, unitId = unit2.id, childId = child2.id),
            )

            assertThrows<NotFound> {
                acceptAbsenceApplication(admin.user, AbsenceApplicationId(UUID.randomUUID()))
            }

            val data =
                AbsenceApplication(
                    id = AbsenceApplicationId(UUID.randomUUID()),
                    createdAt = clock.now(),
                    createdBy = admin.evakaUserId,
                    updatedAt = clock.now(),
                    modifiedAt = clock.now(),
                    modifiedBy = admin.evakaUserId,
                    childId = child1.id,
                    startDate = clock.today(),
                    endDate = clock.today(),
                    description = "test",
                    status = AbsenceApplicationStatus.WAITING_DECISION,
                    decidedAt = null,
                    decidedBy = null,
                    rejectedReason = null,
                )
            val id = db.transaction { tx -> tx.insert(data) }
            acceptAbsenceApplication(admin.user, id)
            assertEquals(
                data.copy(
                    status = AbsenceApplicationStatus.ACCEPTED,
                    decidedAt = clock.now(),
                    decidedBy = admin.evakaUserId,
                ),
                db.read { tx -> tx.selectAbsenceApplication(id)?.copy(updatedAt = clock.now()) },
            )
            assertThrows<BadRequest> { acceptAbsenceApplication(admin.user, id) }
        }

        @Test
        fun `unit supervisor`() {
            val unitSupervisor = DevEmployee()
            db.transaction { tx ->
                tx.insert(unitSupervisor, unitRoles = mapOf(unit1.id to UserRole.UNIT_SUPERVISOR))
            }

            assertThrows<Forbidden> { getAbsenceApplications(unitSupervisor.user) }
            assertThrows<Forbidden> {
                getAbsenceApplications(
                    unitSupervisor.user,
                    status = AbsenceApplicationStatus.WAITING_DECISION,
                )
            }
            assertEquals(
                emptyList(),
                getAbsenceApplications(unitSupervisor.user, unitId = unit1.id),
            )
            assertThrows<Forbidden> {
                getAbsenceApplications(unitSupervisor.user, unitId = unit2.id)
            }
            assertEquals(
                emptyList(),
                getAbsenceApplications(unitSupervisor.user, childId = child1.id),
            )
            assertThrows<Forbidden> {
                getAbsenceApplications(unitSupervisor.user, childId = child2.id)
            }
            assertEquals(
                emptyList(),
                getAbsenceApplications(unitSupervisor.user, unitId = unit1.id, childId = child1.id),
            )
            assertThrows<Forbidden> {
                getAbsenceApplications(unitSupervisor.user, unitId = unit1.id, childId = child2.id)
            }
            assertThrows<Forbidden> {
                getAbsenceApplications(unitSupervisor.user, unitId = unit2.id, childId = child1.id)
            }
            assertThrows<Forbidden> {
                getAbsenceApplications(unitSupervisor.user, unitId = unit2.id, childId = child2.id)
            }
        }

        private fun getAbsenceApplications(
            user: AuthenticatedUser.Employee,
            unitId: DaycareId? = null,
            childId: ChildId? = null,
            status: AbsenceApplicationStatus? = null,
        ) =
            absenceApplicationControllerEmployee.getAbsenceApplications(
                dbInstance(),
                user,
                clock,
                unitId,
                childId,
                status,
            )

        private fun acceptAbsenceApplication(
            user: AuthenticatedUser.Employee,
            id: AbsenceApplicationId,
        ) =
            absenceApplicationControllerEmployee.acceptAbsenceApplication(
                dbInstance(),
                user,
                clock,
                id,
            )
    }
}
