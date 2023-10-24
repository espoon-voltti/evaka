// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka

import fi.espoo.evaka.application.ApplicationStatus
import fi.espoo.evaka.attendance.StaffAttendanceType
import fi.espoo.evaka.daycare.service.AbsenceCategory
import fi.espoo.evaka.daycare.service.AbsenceType
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.serviceneed.ServiceNeedOption
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.PlacementId
import fi.espoo.evaka.shared.ServiceNeedId
import fi.espoo.evaka.shared.ServiceNeedOptionId
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.insertDaycareAclRow
import fi.espoo.evaka.shared.auth.insertDaycareGroupAcl
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevEmployeePin
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevStaffAttendancePlan
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.dev.insertTestAbsence
import fi.espoo.evaka.shared.dev.insertTestApplication
import fi.espoo.evaka.shared.dev.insertTestBackUpCare
import fi.espoo.evaka.shared.dev.insertTestChildAttendance
import fi.espoo.evaka.shared.dev.insertTestDaycareGroupPlacement
import fi.espoo.evaka.shared.dev.insertTestPlacement
import fi.espoo.evaka.shared.dev.insertTestPlacementPlan
import fi.espoo.evaka.shared.dev.insertTestServiceNeed
import fi.espoo.evaka.shared.dev.upsertServiceNeedOption
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

class FixtureBuilder(
    private val tx: Database.Transaction,
    private val today: LocalDate = LocalDate.now()
) {
    fun addChild() = ChildBuilder(tx, today, this)

    fun addEmployee() = EmployeeBuilder(tx, today, this)

    class ChildBuilder(
        private val tx: Database.Transaction,
        private val today: LocalDate,
        private val fixtureBuilder: FixtureBuilder
    ) {
        private var dateOfBirth: LocalDate? = null
        private var person: DevPerson? = null

        fun withDateOfBirth(dateOfBirth: LocalDate) = this.apply { this.dateOfBirth = dateOfBirth }

        fun withAge(years: Int, months: Int = 0, days: Int = 0) =
            this.apply {
                this.dateOfBirth =
                    today
                        .minusYears(years.toLong())
                        .minusMonths(months.toLong())
                        .minusDays(days.toLong())
            }

        fun usePerson(person: DevPerson) = this.apply { this.person = person }

        fun save(): FixtureBuilder {
            doInsert()
            return fixtureBuilder
        }

        fun saveAnd(f: ChildFixture.() -> Unit): FixtureBuilder {
            val childId = doInsert()

            f(ChildFixture(tx, today, childId))

            return fixtureBuilder
        }

        private fun doInsert(): ChildId =
            person?.id
                ?: tx.insert(
                    DevPerson(
                        dateOfBirth = dateOfBirth
                                ?: throw IllegalStateException("date of birth not set")
                    ),
                    DevPersonType.CHILD
                )
    }

    @TestFixture
    class ChildFixture(
        private val tx: Database.Transaction,
        private val today: LocalDate,
        val childId: ChildId
    ) {
        fun addPlacementPlan() = PlacementPlanBuilder(tx, today, this)

        fun addBackupCare() = BackupCareBuilder(tx, today, this)

        fun addPlacement() = PlacementBuilder(tx, today, this)

        fun addAbsence() = AbsenceBuilder(tx, today, this)

        fun addAttendance() = ChildAttendanceBuilder(tx, today, this)
    }

    class AbsenceBuilder(
        private val tx: Database.Transaction,
        private val today: LocalDate,
        private val childFixture: ChildFixture
    ) {
        private var date: LocalDate = today
        private var type: AbsenceType? = null
        private var categories: List<AbsenceCategory>? = null

        fun onDay(date: LocalDate) = this.apply { this.date = date }

        fun onDay(relativeDays: Int) =
            this.apply { this.date = today.plusDays(relativeDays.toLong()) }

        fun ofType(type: AbsenceType) = this.apply { this.type = type }

        fun forCategories(vararg categories: AbsenceCategory) =
            this.apply { this.categories = categories.toList() }

        fun save(): ChildFixture {
            categories?.forEach { category ->
                tx.insertTestAbsence(
                    childId = childFixture.childId,
                    date = date,
                    absenceType = type ?: throw IllegalStateException("absence type not set"),
                    category = category
                )
            }
                ?: throw IllegalStateException("care types not set")

            return childFixture
        }
    }

    class PlacementPlanBuilder(
        private val tx: Database.Transaction,
        private val today: LocalDate,
        private val childFixture: ChildFixture
    ) {
        private var from: LocalDate = today
        private var to: LocalDate = today
        private var unitId: DaycareId? = null
        private var type: PlacementType? = null
        private var deleted = false
        private var preschoolDaycareDates: FiniteDateRange? = null

        fun fromDay(date: LocalDate) = this.apply { this.from = date }

        fun fromDay(relativeDays: Int) =
            this.apply { this.from = today.plusDays(relativeDays.toLong()) }

        fun toDay(relativeDays: Int) =
            this.apply { this.to = today.plusDays(relativeDays.toLong()) }

        fun toDay(date: LocalDate) = this.apply { this.to = date }

        fun toUnit(id: DaycareId) = this.apply { this.unitId = id }

        fun ofType(type: PlacementType) = this.apply { this.type = type }

        fun asDeleted() = this.apply { this.deleted = true }

        fun withPreschoolDaycareDates(range: FiniteDateRange) =
            this.apply { this.preschoolDaycareDates = range }

        fun save(): ChildFixture {
            val applicationGuardianId = tx.insert(DevPerson(), DevPersonType.RAW_ROW)
            val applicationId =
                tx.insertTestApplication(
                    guardianId = applicationGuardianId,
                    childId = childFixture.childId,
                    status = ApplicationStatus.WAITING_DECISION,
                    type = type?.toApplicationType() ?: error("type not set")
                )
            tx.insertTestPlacementPlan(
                applicationId = applicationId,
                unitId = unitId ?: throw IllegalStateException("unit not set"),
                type = type ?: throw IllegalStateException("type not set"),
                startDate = from,
                endDate = to,
                deleted = deleted,
                preschoolDaycareStartDate = preschoolDaycareDates?.start,
                preschoolDaycareEndDate = preschoolDaycareDates?.end
            )
            return childFixture
        }
    }

    class BackupCareBuilder(
        private val tx: Database.Transaction,
        private val today: LocalDate,
        private val childFixture: ChildFixture
    ) {
        private var from: LocalDate = today
        private var to: LocalDate = today
        private var unitId: DaycareId? = null
        private var groupId: GroupId? = null

        fun fromDay(date: LocalDate) = this.apply { this.from = date }

        fun fromDay(relativeDays: Int) =
            this.apply { this.from = today.plusDays(relativeDays.toLong()) }

        fun toDay(relativeDays: Int) =
            this.apply { this.to = today.plusDays(relativeDays.toLong()) }

        fun toDay(date: LocalDate) = this.apply { this.to = date }

        fun toUnit(id: DaycareId) = this.apply { this.unitId = id }

        fun toGroup(id: GroupId) = this.apply { this.groupId = id }

        fun save(): ChildFixture {
            tx.insertTestBackUpCare(
                childId = childFixture.childId,
                unitId = unitId ?: throw IllegalStateException("unit not set"),
                startDate = from,
                endDate = to,
                groupId = groupId
            )
            return childFixture
        }
    }

    class PlacementBuilder(
        private val tx: Database.Transaction,
        private val today: LocalDate,
        private val childFixture: ChildFixture
    ) {
        private var from: LocalDate = today
        private var to: LocalDate = today
        private var unitId: DaycareId? = null
        private var type: PlacementType = PlacementType.DAYCARE

        fun fromDay(date: LocalDate) = this.apply { this.from = date }

        fun fromDay(relativeDays: Int) =
            this.apply { this.from = today.plusDays(relativeDays.toLong()) }

        fun toDay(relativeDays: Int) =
            this.apply { this.to = today.plusDays(relativeDays.toLong()) }

        fun toDay(date: LocalDate) = this.apply { this.to = date }

        fun toUnit(id: DaycareId) = this.apply { this.unitId = id }

        fun ofType(type: PlacementType) = this.apply { this.type = type }

        fun save(): ChildFixture {
            doInsert()
            return childFixture
        }

        fun saveAnd(f: PlacementFixture.() -> Unit): ChildFixture {
            val placementId = doInsert()

            f(
                PlacementFixture(
                    tx = tx,
                    today = today,
                    placementId = placementId,
                    placementPeriod = FiniteDateRange(from, to)
                )
            )

            return childFixture
        }

        private fun doInsert() =
            tx.insertTestPlacement(
                childId = childFixture.childId,
                unitId = unitId ?: throw IllegalStateException("unit not set"),
                type = type,
                startDate = from,
                endDate = to
            )
    }

    @TestFixture
    class PlacementFixture(
        private val tx: Database.Transaction,
        private val today: LocalDate,
        val placementId: PlacementId,
        val placementPeriod: FiniteDateRange
    ) {
        fun addGroupPlacement() = GroupPlacementBuilder(tx, today, this)

        fun addServiceNeed() = ServiceNeedBuilder(tx, today, this)
    }

    class GroupPlacementBuilder(
        private val tx: Database.Transaction,
        private val today: LocalDate,
        private val placementFixture: PlacementFixture
    ) {
        private var from: LocalDate? = null
        private var to: LocalDate? = null
        private var groupId: GroupId? = null

        fun fromDay(date: LocalDate) = this.apply { this.from = date }

        fun fromDay(relativeDays: Int) =
            this.apply { this.from = today.plusDays(relativeDays.toLong()) }

        fun toDay(date: LocalDate) = this.apply { this.to = date }

        fun toDay(relativeDays: Int) =
            this.apply { this.to = today.plusDays(relativeDays.toLong()) }

        fun toGroup(id: GroupId) = this.apply { this.groupId = id }

        fun save(): PlacementFixture {
            tx.insertTestDaycareGroupPlacement(
                daycarePlacementId = placementFixture.placementId,
                groupId = groupId ?: throw IllegalStateException("group not set"),
                startDate = from ?: placementFixture.placementPeriod.start,
                endDate = to ?: placementFixture.placementPeriod.end
            )
            return placementFixture
        }
    }

    class ServiceNeedBuilder(
        private val tx: Database.Transaction,
        private val today: LocalDate,
        private val placementFixture: PlacementFixture
    ) {
        private var from: LocalDate? = null
        private var to: LocalDate? = null
        private var optionId: ServiceNeedOptionId? = null
        private var serviceNeedOption: ServiceNeedOption? = null
        private var employeeId: EvakaUserId? = null
        private var updated: HelsinkiDateTime = HelsinkiDateTime.now()
        private var id: ServiceNeedId? = null

        fun fromDay(date: LocalDate) = this.apply { this.from = date }

        fun fromDay(relativeDays: Int) =
            this.apply { this.from = today.plusDays(relativeDays.toLong()) }

        fun toDay(date: LocalDate) = this.apply { this.to = date }

        fun toDay(relativeDays: Int) =
            this.apply { this.to = today.plusDays(relativeDays.toLong()) }

        fun withOption(id: ServiceNeedOptionId) = this.apply { this.optionId = id }

        fun withOption(serviceNeedOption: ServiceNeedOption) =
            this.apply { this.serviceNeedOption = serviceNeedOption }

        fun createdBy(employeeId: EvakaUserId) = this.apply { this.employeeId = employeeId }

        fun withUpdated(updated: HelsinkiDateTime) = this.apply { this.updated = updated }

        fun withId(id: ServiceNeedId) = this.apply { this.id = id }

        fun save(): PlacementFixture {
            if (serviceNeedOption != null) tx.upsertServiceNeedOption(serviceNeedOption!!)

            tx.insertTestServiceNeed(
                confirmedBy = employeeId ?: throw IllegalStateException("createdBy not set"),
                placementId = placementFixture.placementId,
                optionId = optionId
                        ?: serviceNeedOption?.id ?: throw IllegalStateException("option not set"),
                period =
                    FiniteDateRange(
                        from ?: placementFixture.placementPeriod.start,
                        to ?: placementFixture.placementPeriod.end
                    ),
                id = id ?: ServiceNeedId(UUID.randomUUID())
            )

            return placementFixture
        }
    }

    class ChildAttendanceBuilder(
        private val tx: Database.Transaction,
        private val today: LocalDate,
        private val childFixture: ChildFixture
    ) {
        private var from: HelsinkiDateTime? = null
        private var to: HelsinkiDateTime? = null
        private var unitId: DaycareId? = null

        fun arriving(time: HelsinkiDateTime) = this.apply { this.from = time }

        fun arriving(date: LocalDate, time: LocalTime) =
            this.apply { this.from = HelsinkiDateTime.Companion.of(date, time) }

        fun arriving(time: LocalTime) = arriving(today, time)

        fun departing(time: HelsinkiDateTime) = this.apply { this.to = time }

        fun departing(date: LocalDate, time: LocalTime) =
            this.apply { this.to = HelsinkiDateTime.Companion.of(date, time) }

        fun departing(time: LocalTime) = departing(from?.toLocalDate() ?: today, time)

        fun inUnit(unitId: DaycareId) = this.apply { this.unitId = unitId }

        fun save(): ChildFixture {
            tx.insertTestChildAttendance(
                childId = childFixture.childId,
                unitId = unitId ?: error("unit must be set"),
                arrived = from ?: error("arrival time must be set"),
                departed = to
            )
            return childFixture
        }
    }

    class EmployeeBuilder(
        private val tx: Database.Transaction,
        private val today: LocalDate,
        private val fixtureBuilder: FixtureBuilder
    ) {
        private var globalRoles: Set<UserRole> = emptySet()
        private val unitRoles: MutableSet<Pair<UserRole, DaycareId>> = mutableSetOf()
        private val groups: MutableSet<Pair<DaycareId, GroupId>> = mutableSetOf()
        private var firstName: String = "First"
        private var lastName: String = "Last"
        private var pinCode: String? = null
        private var lastLogin: HelsinkiDateTime? = HelsinkiDateTime.now()

        fun withGlobalRoles(roles: Set<UserRole>) = this.apply { this.globalRoles = roles }

        fun withScopedRole(role: UserRole, unitId: DaycareId) =
            this.apply { this.unitRoles.add(Pair(role, unitId)) }

        fun withGroupAccess(unitId: DaycareId, groupId: GroupId) =
            this.apply { this.groups.add(Pair(unitId, groupId)) }

        fun withName(firstName: String, lastName: String) =
            this.apply {
                this.firstName = firstName
                this.lastName = lastName
            }

        fun withPinCode(pinCode: String) = this.apply { this.pinCode = pinCode }

        fun withLastLogin(lastLogin: HelsinkiDateTime) = this.apply { this.lastLogin = lastLogin }

        fun save(): FixtureBuilder {
            doInsert()
            return fixtureBuilder
        }

        fun saveAnd(f: EmployeeFixture.() -> Unit): FixtureBuilder {
            val employeeId = doInsert()

            f(EmployeeFixture(tx, today, employeeId))

            return fixtureBuilder
        }

        private fun doInsert(): EmployeeId {
            val employee =
                DevEmployee(
                    roles = globalRoles,
                    firstName = firstName,
                    lastName = lastName,
                    lastLogin = lastLogin
                )
            val employeeId = tx.insert(employee)
            unitRoles.forEach { (role, unitId) -> tx.insertDaycareAclRow(unitId, employeeId, role) }
            groups.forEach { (unitId, groupId) ->
                tx.insertDaycareGroupAcl(unitId, employeeId, listOf(groupId))
            }
            pinCode.also {
                if (it != null) tx.insert(DevEmployeePin(userId = employeeId, pin = it))
            }
            return employeeId
        }
    }

    @TestFixture
    class EmployeeFixture(
        private val tx: Database.Transaction,
        private val today: LocalDate,
        val employeeId: EmployeeId
    ) {
        fun addRealtimeAttendance() = StaffAttendanceBuilder(tx, today, this)

        fun addStaffAttendancePlan() = StaffAttendancePlanBuilder(tx, today, this)

        fun addAttendancePlan() = StaffAttendancePlanBuilder(tx, today, this)
    }

    class StaffAttendanceBuilder(
        private val tx: Database.Transaction,
        private val today: LocalDate,
        private val employeeFixture: EmployeeFixture
    ) {
        private var from: HelsinkiDateTime? = null
        private var to: HelsinkiDateTime? = null
        private var groupId: GroupId? = null
        private var coefficient: BigDecimal? = null
        private var type: StaffAttendanceType? = null

        fun arriving(time: HelsinkiDateTime) = this.apply { this.from = time }

        fun arriving(date: LocalDate, time: LocalTime) =
            this.apply { this.from = HelsinkiDateTime.Companion.of(date, time) }

        fun arriving(time: LocalTime) = arriving(today, time)

        fun departing(time: HelsinkiDateTime) = this.apply { this.to = time }

        fun departing(date: LocalDate, time: LocalTime) =
            this.apply { this.to = HelsinkiDateTime.Companion.of(date, time) }

        fun departing(time: LocalTime) = departing(from?.toLocalDate() ?: today, time)

        fun withCoefficient(coefficient: BigDecimal) = this.apply { this.coefficient = coefficient }

        fun withType(type: StaffAttendanceType) = this.apply { this.type = type }

        fun inGroup(groupId: GroupId) = this.apply { this.groupId = groupId }

        fun save(): EmployeeFixture {
            tx.createUpdate(
                    """
                INSERT INTO staff_attendance_realtime (employee_id, group_id, arrived, departed, occupancy_coefficient, type)
                VALUES (:employeeId, :groupId, :arrived, :departed, :coefficient, :type)
                """
                        .trimIndent()
                )
                .bind("employeeId", employeeFixture.employeeId)
                .bind("groupId", groupId ?: error("group must be set"))
                .bind("arrived", from ?: error("arrival time must be set"))
                .bind("departed", to)
                .bind("coefficient", coefficient ?: error("occupancyCoefficient must be set"))
                .bind("type", type ?: error("type must be set"))
                .updateExactlyOne()

            return employeeFixture
        }
    }

    class StaffAttendancePlanBuilder(
        private val tx: Database.Transaction,
        private val today: LocalDate,
        private val employeeFixture: EmployeeFixture
    ) {
        private var from: HelsinkiDateTime? = null
        private var to: HelsinkiDateTime? = null
        private var type: StaffAttendanceType? = null

        fun withTime(from: HelsinkiDateTime, to: HelsinkiDateTime) =
            this.apply {
                this.from = from
                this.to = to
            }

        fun save(): EmployeeFixture {
            tx.insert(
                DevStaffAttendancePlan(
                    employeeId = employeeFixture.employeeId,
                    type = this.type ?: StaffAttendanceType.PRESENT,
                    startTime = this.from ?: error("staff attendance plan start time must be set"),
                    endTime = this.to ?: error("staff attendance plan end time must be set"),
                    description = null
                )
            )
            return employeeFixture
        }
    }
}

/**
 * This is needed for better control of implicit this references.
 *
 * See: https://github.com/Kotlin/KEEP/pull/38
 * https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-dsl-marker/
 */
@DslMarker annotation class TestFixture
