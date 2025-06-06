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
import fi.espoo.evaka.emailclient.MockEmailClient
import fi.espoo.evaka.pis.PersonNameDetails
import fi.espoo.evaka.pis.service.insertGuardian
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.AbsenceApplicationId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.CitizenAuthLevel
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.data.DateSet
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.DevDaycareGroupPlacement
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.DevPreschoolTerm
import fi.espoo.evaka.shared.dev.DevServiceNeed
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.dev.insertServiceNeedOption
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.Forbidden
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.domain.toFiniteDateRange
import fi.espoo.evaka.shared.security.Action
import fi.espoo.evaka.snPreschoolDaycareContractDays13
import fi.espoo.evaka.toEvakaUser
import fi.espoo.evaka.user.EvakaUserType
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlin.test.assertEquals
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.groups.Tuple
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
    @Autowired private lateinit var asyncJobRunner: AsyncJobRunner<AsyncJob>

    @Nested
    inner class CrudTest {
        private val area = DevCareArea()
        private val unit = DevDaycare(areaId = area.id)
        private val unitSupervisor = DevEmployee()
        private val child = DevPerson()
        private val adult = DevPerson(email = "test@example.com")

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
            }
        }

        @Test
        fun `accepted flow`() {
            db.transaction { tx ->
                tx.insert(
                    DevPlacement(
                        type = PlacementType.PRESCHOOL,
                        childId = child.id,
                        unitId = unit.id,
                        startDate = LocalDate.of(2022, 1, 1),
                        endDate = LocalDate.of(2022, 12, 31),
                    )
                )
            }

            val id =
                absenceApplicationControllerCitizen.postAbsenceApplication(
                    dbInstance(),
                    adult.user(CitizenAuthLevel.STRONG),
                    clock,
                    AbsenceApplicationCreateRequest(
                        childId = child.id,
                        startDate = LocalDate.of(2022, 8, 10),
                        endDate = LocalDate.of(2022, 8, 10),
                        description = "Lapinreissu",
                    ),
                )

            val expected1 =
                AbsenceApplicationSummary(
                    id = id,
                    createdAt = clock.now(),
                    createdBy = adult.toEvakaUser(EvakaUserType.CITIZEN),
                    child = PersonNameDetails(child.id, child.firstName, child.lastName),
                    startDate = LocalDate.of(2022, 8, 10),
                    endDate = LocalDate.of(2022, 8, 10),
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
                        date = LocalDate.of(2022, 8, 10),
                        category = AbsenceCategory.NONBILLABLE,
                        absenceType = AbsenceType.OTHER_ABSENCE,
                        modifiedByStaff = true,
                        modifiedAt = clock.now(),
                        belongsToQuestionnaire = false,
                    )
                ),
                db.transaction { tx ->
                    tx.getAbsencesOfChildByDate(child.id, LocalDate.of(2022, 8, 10))
                },
            )

            asyncJobRunner.runPendingJobsSync(clock)
            assertThat(MockEmailClient.emails)
                .extracting({ it.toAddress }, { it.content.subject })
                .containsExactly(
                    Tuple("test@example.com", "Esiopetuksen poissaolohakemus hyväksytty")
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
        fun `absences not inserted if application dates are in the past`() {
            db.transaction { tx ->
                tx.insert(
                    DevPlacement(
                        type = PlacementType.PRESCHOOL,
                        childId = child.id,
                        unitId = unit.id,
                        startDate = LocalDate.of(2022, 1, 1),
                        endDate = LocalDate.of(2022, 12, 31),
                    )
                )
            }

            val range = clock.today().minusDays(1).toFiniteDateRange()
            val data =
                AbsenceApplication(
                    id = AbsenceApplicationId(UUID.randomUUID()),
                    createdAt = clock.now(),
                    createdBy = citizenUser.evakaUserId,
                    modifiedAt = clock.now(),
                    modifiedBy = citizenUser.evakaUserId,
                    childId = child.id,
                    startDate = range.start,
                    endDate = range.end,
                    description = "test",
                    status = AbsenceApplicationStatus.WAITING_DECISION,
                    decidedAt = null,
                    decidedBy = null,
                    rejectedReason = null,
                )
            db.transaction { tx -> tx.insert(data) }

            absenceApplicationControllerEmployee.acceptAbsenceApplication(
                dbInstance(),
                employeeUser,
                clock,
                data.id,
            )

            assertEquals(
                emptyList(),
                db.transaction { tx -> tx.getAbsencesOfChildByRange(child.id, range.asDateRange()) },
            )
        }

        @Test
        fun `past dates are not inserted`() {
            db.transaction { tx ->
                tx.insert(
                    DevPlacement(
                        type = PlacementType.PRESCHOOL,
                        childId = child.id,
                        unitId = unit.id,
                        startDate = LocalDate.of(2022, 1, 1),
                        endDate = LocalDate.of(2022, 12, 31),
                    )
                )
            }

            val range = FiniteDateRange(clock.today().minusDays(1), clock.today().plusDays(1))
            val data =
                AbsenceApplication(
                    id = AbsenceApplicationId(UUID.randomUUID()),
                    createdAt = clock.now(),
                    createdBy = citizenUser.evakaUserId,
                    modifiedAt = clock.now(),
                    modifiedBy = citizenUser.evakaUserId,
                    childId = child.id,
                    startDate = range.start,
                    endDate = range.end,
                    description = "test",
                    status = AbsenceApplicationStatus.WAITING_DECISION,
                    decidedAt = null,
                    decidedBy = null,
                    rejectedReason = null,
                )
            db.transaction { tx -> tx.insert(data) }

            absenceApplicationControllerEmployee.acceptAbsenceApplication(
                dbInstance(),
                employeeUser,
                clock,
                data.id,
            )

            assertEquals(
                listOf(
                    Absence(
                        childId = child.id,
                        date = clock.today(),
                        category = AbsenceCategory.NONBILLABLE,
                        absenceType = AbsenceType.OTHER_ABSENCE,
                        modifiedByStaff = true,
                        modifiedAt = clock.now(),
                        belongsToQuestionnaire = false,
                    ),
                    Absence(
                        childId = child.id,
                        date = clock.today().plusDays(1),
                        category = AbsenceCategory.NONBILLABLE,
                        absenceType = AbsenceType.OTHER_ABSENCE,
                        modifiedByStaff = true,
                        modifiedAt = clock.now(),
                        belongsToQuestionnaire = false,
                    ),
                ),
                db.transaction { tx ->
                    tx.getAbsencesOfChildByRange(child.id, range.asDateRange()).sortedBy { it.date }
                },
            )
        }

        @Test
        fun `child with contract days absences are inserted correctly`() {
            db.transaction { tx ->
                val placement =
                    DevPlacement(
                        type = PlacementType.PRESCHOOL_DAYCARE,
                        childId = child.id,
                        unitId = unit.id,
                        startDate = LocalDate.of(2022, 1, 1),
                        endDate = LocalDate.of(2022, 12, 31),
                    )
                val placementId = tx.insert(placement)
                tx.insertServiceNeedOption(snPreschoolDaycareContractDays13)
                tx.insert(
                    DevServiceNeed(
                        placementId = placementId,
                        startDate = placement.startDate,
                        endDate = placement.endDate,
                        optionId = snPreschoolDaycareContractDays13.id,
                        confirmedBy = unitSupervisor.evakaUserId,
                    )
                )
            }
            val id =
                absenceApplicationControllerCitizen.postAbsenceApplication(
                    dbInstance(),
                    adult.user(CitizenAuthLevel.STRONG),
                    clock,
                    AbsenceApplicationCreateRequest(
                        childId = child.id,
                        startDate = LocalDate.of(2022, 9, 9),
                        endDate = LocalDate.of(2022, 9, 9),
                        description = "Lapinreissu",
                    ),
                )

            absenceApplicationControllerEmployee.acceptAbsenceApplication(
                dbInstance(),
                employeeUser,
                clock,
                id,
            )

            assertEquals(
                listOf(
                    Absence(
                        childId = child.id,
                        date = LocalDate.of(2022, 9, 9),
                        category = AbsenceCategory.BILLABLE,
                        absenceType = AbsenceType.PLANNED_ABSENCE,
                        modifiedByStaff = true,
                        modifiedAt = clock.now(),
                        belongsToQuestionnaire = false,
                    ),
                    Absence(
                        childId = child.id,
                        date = LocalDate.of(2022, 9, 9),
                        category = AbsenceCategory.NONBILLABLE,
                        absenceType = AbsenceType.OTHER_ABSENCE,
                        modifiedByStaff = true,
                        modifiedAt = clock.now(),
                        belongsToQuestionnaire = false,
                    ),
                ),
                db.transaction { tx ->
                    tx.getAbsencesOfChildByDate(child.id, LocalDate.of(2022, 9, 9)).sortedBy {
                        it.category
                    }
                },
            )
        }

        @Test
        fun `rejected flow`() {
            db.transaction { tx ->
                tx.insert(
                    DevPlacement(
                        type = PlacementType.PRESCHOOL,
                        childId = child.id,
                        unitId = unit.id,
                        startDate = LocalDate.of(2022, 1, 1),
                        endDate = LocalDate.of(2022, 12, 31),
                    )
                )
            }

            val id =
                absenceApplicationControllerCitizen.postAbsenceApplication(
                    dbInstance(),
                    citizenUser,
                    clock,
                    AbsenceApplicationCreateRequest(
                        childId = child.id,
                        startDate = LocalDate.of(2022, 8, 10),
                        endDate = LocalDate.of(2022, 8, 10),
                        description = "Lapinreissu",
                    ),
                )

            val expected1 =
                AbsenceApplicationSummary(
                    id = id,
                    createdAt = clock.now(),
                    createdBy = adult.toEvakaUser(EvakaUserType.CITIZEN),
                    child = PersonNameDetails(child.id, child.firstName, child.lastName),
                    startDate = LocalDate.of(2022, 8, 10),
                    endDate = LocalDate.of(2022, 8, 10),
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
                    tx.getAbsencesOfChildByDate(child.id, LocalDate.of(2022, 8, 10))
                },
            )

            asyncJobRunner.runPendingJobsSync(clock)
            assertThat(MockEmailClient.emails)
                .extracting({ it.toAddress }, { it.content.subject })
                .containsExactly(Tuple("test@example.com", "Esiopetuksen poissaolohakemus hylätty"))

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
        private val group11 = DevDaycareGroup(daycareId = unit1.id)
        private val group12 = DevDaycareGroup(daycareId = unit1.id)
        private val unit2 = DevDaycare(areaId = area.id)
        private val child11 = DevPerson()
        private val child12 = DevPerson()
        private val child2 = DevPerson()
        private val clock =
            MockEvakaClock(HelsinkiDateTime.of(LocalDate.of(2022, 8, 10), LocalTime.of(8, 0)))

        @BeforeEach
        fun setup() {
            db.transaction { tx ->
                val termRange = FiniteDateRange(clock.today(), clock.today().plusYears(1))
                tx.insert(
                    DevPreschoolTerm(
                        finnishPreschool = termRange,
                        swedishPreschool = termRange,
                        extendedTerm = termRange,
                        applicationPeriod = termRange.copy(start = clock.today().minusMonths(2)),
                        termBreaks = DateSet.empty(),
                    )
                )
                tx.insert(area)
                tx.insert(unit1)
                tx.insert(unit2)
                tx.insert(group11)
                tx.insert(group12)
                tx.insert(child11, DevPersonType.CHILD)
                val placement1 =
                    DevPlacement(
                        type = PlacementType.PRESCHOOL,
                        childId = child11.id,
                        unitId = unit1.id,
                        startDate = termRange.start,
                        endDate = termRange.end,
                    )
                tx.insert(placement1)
                tx.insert(
                    DevDaycareGroupPlacement(
                        daycarePlacementId = placement1.id,
                        daycareGroupId = group11.id,
                        startDate = placement1.startDate,
                        endDate = placement1.endDate,
                    )
                )
                tx.insert(child12, DevPersonType.CHILD)
                val placement2 =
                    DevPlacement(
                        type = PlacementType.PRESCHOOL,
                        childId = child12.id,
                        unitId = unit1.id,
                        startDate = termRange.start,
                        endDate = termRange.end,
                    )
                tx.insert(placement2)
                tx.insert(
                    DevDaycareGroupPlacement(
                        daycarePlacementId = placement2.id,
                        daycareGroupId = group12.id,
                        startDate = placement2.startDate,
                        endDate = placement2.endDate,
                    )
                )
                tx.insert(child2, DevPersonType.CHILD)
                tx.insert(
                    DevPlacement(
                        type = PlacementType.PRESCHOOL,
                        childId = child2.id,
                        unitId = unit2.id,
                        startDate = termRange.start,
                        endDate = termRange.end,
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
            assertEquals(emptyList(), getAbsenceApplications(admin.user, childId = child11.id))
            assertEquals(emptyList(), getAbsenceApplications(admin.user, childId = child2.id))
            assertEquals(
                emptyList(),
                getAbsenceApplications(admin.user, unitId = unit1.id, childId = child11.id),
            )
            assertEquals(
                emptyList(),
                getAbsenceApplications(admin.user, unitId = unit1.id, childId = child2.id),
            )
            assertEquals(
                emptyList(),
                getAbsenceApplications(admin.user, unitId = unit2.id, childId = child11.id),
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
                    modifiedAt = clock.now(),
                    modifiedBy = admin.evakaUserId,
                    childId = child11.id,
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
                db.read { tx -> tx.selectAbsenceApplication(id) },
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
                getAbsenceApplications(unitSupervisor.user, childId = child11.id),
            )
            assertThrows<Forbidden> {
                getAbsenceApplications(unitSupervisor.user, childId = child2.id)
            }
            assertEquals(
                emptyList(),
                getAbsenceApplications(unitSupervisor.user, unitId = unit1.id, childId = child11.id),
            )
            assertThrows<Forbidden> {
                getAbsenceApplications(unitSupervisor.user, unitId = unit1.id, childId = child2.id)
            }
            assertThrows<Forbidden> {
                getAbsenceApplications(unitSupervisor.user, unitId = unit2.id, childId = child11.id)
            }
            assertThrows<Forbidden> {
                getAbsenceApplications(unitSupervisor.user, unitId = unit2.id, childId = child2.id)
            }
        }

        @Test
        fun staff() {
            val staff = DevEmployee()
            db.transaction { tx ->
                tx.insert(
                    staff,
                    unitRoles = mapOf(unit1.id to UserRole.STAFF),
                    groupAcl = mapOf(unit1.id to listOf(group11.id)),
                )
            }
            val base =
                AbsenceApplication(
                    id = AbsenceApplicationId(UUID.randomUUID()),
                    createdAt = clock.now(),
                    createdBy = staff.evakaUserId,
                    modifiedAt = clock.now(),
                    modifiedBy = staff.evakaUserId,
                    childId = child11.id,
                    startDate = clock.today(),
                    endDate = clock.today(),
                    description = "test",
                    status = AbsenceApplicationStatus.WAITING_DECISION,
                    decidedAt = null,
                    decidedBy = null,
                    rejectedReason = null,
                )
            val application1 =
                db.transaction { tx ->
                    tx.insert(
                        base.copy(
                            id = AbsenceApplicationId(UUID.randomUUID()),
                            endDate = clock.today().plusWeeks(1).minusDays(1),
                        )
                    )
                }
            val application2 =
                db.transaction { tx ->
                    tx.insert(
                        base.copy(
                            id = AbsenceApplicationId(UUID.randomUUID()),
                            endDate = clock.today().plusWeeks(1),
                        )
                    )
                }
            val application3 =
                db.transaction { tx ->
                    tx.insert(
                        base.copy(
                            id = AbsenceApplicationId(UUID.randomUUID()),
                            childId = child12.id,
                        )
                    )
                }

            assertThat(getAbsenceApplications(staff.user, unitId = unit1.id))
                .extracting({ it.data.child.id }, { it.data.endDate })
                .containsExactlyInAnyOrder(
                    Tuple(child11.id, clock.today().plusWeeks(1).minusDays(1)),
                    Tuple(child11.id, clock.today().plusWeeks(1)),
                )
            assertThat(getAbsenceApplications(staff.user, childId = child11.id))
                .extracting({ it.data.endDate }, { it.actions })
                .containsExactlyInAnyOrder(
                    Tuple(
                        clock.today().plusWeeks(1).minusDays(1),
                        setOf(
                            Action.AbsenceApplication.READ,
                            Action.AbsenceApplication.DECIDE_MAX_WEEK,
                        ),
                    ),
                    Tuple(clock.today().plusWeeks(1), setOf(Action.AbsenceApplication.READ)),
                )
            acceptAbsenceApplication(staff.user, application1)
            assertThrows<Forbidden> { acceptAbsenceApplication(staff.user, application2) }
            assertThrows<Forbidden> { acceptAbsenceApplication(staff.user, application3) }
            assertThrows<Forbidden> { getAbsenceApplications(staff.user, unitId = unit2.id) }
            assertThrows<Forbidden> { getAbsenceApplications(staff.user, childId = child2.id) }
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
