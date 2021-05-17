package fi.espoo.evaka

import fi.espoo.evaka.application.ApplicationStatus
import fi.espoo.evaka.daycare.service.AbsenceType
import fi.espoo.evaka.daycare.service.CareType
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.dev.DevAssistanceNeed
import fi.espoo.evaka.shared.dev.DevChild
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.insertTestAbsence
import fi.espoo.evaka.shared.dev.insertTestApplication
import fi.espoo.evaka.shared.dev.insertTestAssistanceNeed
import fi.espoo.evaka.shared.dev.insertTestBackUpCare
import fi.espoo.evaka.shared.dev.insertTestChild
import fi.espoo.evaka.shared.dev.insertTestDaycareGroupPlacement
import fi.espoo.evaka.shared.dev.insertTestNewServiceNeed
import fi.espoo.evaka.shared.dev.insertTestPerson
import fi.espoo.evaka.shared.dev.insertTestPlacement
import fi.espoo.evaka.shared.dev.insertTestPlacementPlan
import fi.espoo.evaka.shared.domain.FiniteDateRange
import java.lang.IllegalStateException
import java.time.LocalDate
import java.util.UUID

class FixtureBuilder(
    private val tx: Database.Transaction,
    private val today: LocalDate
) {
    fun addChild() = ChildBuilder(tx, today, this)

    class ChildBuilder(
        private val tx: Database.Transaction,
        private val today: LocalDate,
        private val fixtureBuilder: FixtureBuilder
    ) {
        private var dateOfBirth: LocalDate? = null

        fun withDateOfBirth(dateOfBirth: LocalDate) = this.apply {
            this.dateOfBirth = dateOfBirth
        }

        fun withAge(years: Int, months: Int = 0, days: Int = 0) = this.apply {
            this.dateOfBirth = today.minusYears(years.toLong()).minusMonths(months.toLong()).minusDays(days.toLong())
        }

        fun save(): FixtureBuilder {
            doInsert()
            return fixtureBuilder
        }

        fun saveAnd(f: ChildFixture.() -> Unit): FixtureBuilder {
            val childId = doInsert()

            f(ChildFixture(tx, today, childId))

            return fixtureBuilder
        }

        private fun doInsert(): UUID {
            val childId = tx.insertTestPerson(
                DevPerson(
                    dateOfBirth = dateOfBirth ?: throw IllegalStateException("date of birth not set")
                )
            )
            tx.insertTestChild(DevChild(childId))
            return childId
        }
    }

    @TestFixture
    class ChildFixture(
        private val tx: Database.Transaction,
        private val today: LocalDate,
        val childId: UUID
    ) {
        fun addAssistanceNeed() = AssistanceNeedBuilder(tx, today, this)
        fun addPlacementPlan() = PlacementPlanBuilder(tx, today, this)
        fun addBackupCare() = BackupCareBuilder(tx, today, this)
        fun addPlacement() = PlacementBuilder(tx, today, this)
        fun addAbsence() = AbsenceBuilder(tx, today, this)
    }

    class AssistanceNeedBuilder(
        private val tx: Database.Transaction,
        private val today: LocalDate,
        private val childFixture: ChildFixture
    ) {
        private var from: LocalDate = today
        private var to: LocalDate = today
        private var employeeId: UUID? = null
        private var factor: Double = 1.0

        fun fromDay(date: LocalDate) = this.apply { this.from = date }
        fun fromDay(relativeDays: Int) = this.apply { this.from = today.plusDays(relativeDays.toLong()) }
        fun toDay(relativeDays: Int) = this.apply { this.to = today.plusDays(relativeDays.toLong()) }
        fun toDay(date: LocalDate) = this.apply { this.to = date }
        fun withFactor(factor: Double) = this.apply { this.factor = factor }
        fun createdBy(employeeId: UUID) = this.apply { this.employeeId = employeeId }

        fun save(): ChildFixture {
            tx.insertTestAssistanceNeed(
                DevAssistanceNeed(
                    childId = childFixture.childId,
                    updatedBy = employeeId ?: throw IllegalStateException("createdBy not set"),
                    startDate = from,
                    endDate = to,
                    capacityFactor = factor
                )
            )
            return childFixture
        }
    }

    class AbsenceBuilder(
        private val tx: Database.Transaction,
        private val today: LocalDate,
        private val childFixture: ChildFixture
    ) {
        private var date: LocalDate = today
        private var type: AbsenceType? = null
        private var careTypes: List<CareType>? = null

        fun onDay(date: LocalDate) = this.apply { this.date = date }
        fun onDay(relativeDays: Int) = this.apply { this.date = today.plusDays(relativeDays.toLong()) }
        fun ofType(type: AbsenceType) = this.apply { this.type = type }
        fun forCareTypes(vararg careTypes: CareType) = this.apply { this.careTypes = careTypes.asList() }

        fun save(): ChildFixture {
            careTypes?.forEach { careType ->
                tx.insertTestAbsence(
                    childId = childFixture.childId,
                    date = date,
                    absenceType = type ?: throw IllegalStateException("absence type not set"),
                    careType = careType
                )
            } ?: throw IllegalStateException("care types not set")

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
        private var unitId: UUID? = null
        private var type: PlacementType? = null
        private var deleted = false

        fun fromDay(date: LocalDate) = this.apply { this.from = date }
        fun fromDay(relativeDays: Int) = this.apply { this.from = today.plusDays(relativeDays.toLong()) }
        fun toDay(relativeDays: Int) = this.apply { this.to = today.plusDays(relativeDays.toLong()) }
        fun toDay(date: LocalDate) = this.apply { this.to = date }
        fun toUnit(id: UUID) = this.apply { this.unitId = id }
        fun ofType(type: PlacementType) = this.apply { this.type = type }
        fun asDeleted() = this.apply { this.deleted = true }

        fun save(): ChildFixture {
            val applicationGuardianId = tx.insertTestPerson(DevPerson())
            val applicationId = tx.insertTestApplication(
                guardianId = applicationGuardianId,
                childId = childFixture.childId,
                status = ApplicationStatus.WAITING_DECISION
            )
            tx.insertTestPlacementPlan(
                applicationId = applicationId,
                unitId = unitId ?: throw IllegalStateException("unit not set"),
                type = type ?: throw IllegalStateException("type not set"),
                startDate = from,
                endDate = to,
                deleted = deleted
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
        private var unitId: UUID? = null
        private var groupId: UUID? = null

        fun fromDay(date: LocalDate) = this.apply { this.from = date }
        fun fromDay(relativeDays: Int) = this.apply { this.from = today.plusDays(relativeDays.toLong()) }
        fun toDay(relativeDays: Int) = this.apply { this.to = today.plusDays(relativeDays.toLong()) }
        fun toDay(date: LocalDate) = this.apply { this.to = date }
        fun toUnit(id: UUID) = this.apply { this.unitId = id }
        fun toGroup(id: UUID) = this.apply { this.groupId = id }

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
        private var unitId: UUID? = null
        private var type: PlacementType? = null

        fun fromDay(date: LocalDate) = this.apply { this.from = date }
        fun fromDay(relativeDays: Int) = this.apply { this.from = today.plusDays(relativeDays.toLong()) }
        fun toDay(relativeDays: Int) = this.apply { this.to = today.plusDays(relativeDays.toLong()) }
        fun toDay(date: LocalDate) = this.apply { this.to = date }
        fun toUnit(id: UUID) = this.apply { this.unitId = id }
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

        private fun doInsert() = tx.insertTestPlacement(
            childId = childFixture.childId,
            unitId = unitId ?: throw IllegalStateException("unit not set"),
            type = type ?: throw IllegalStateException("type not set"),
            startDate = from,
            endDate = to
        )
    }

    @TestFixture
    class PlacementFixture(
        private val tx: Database.Transaction,
        private val today: LocalDate,
        val placementId: UUID,
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
        private var groupId: UUID? = null

        fun fromDay(date: LocalDate) = this.apply { this.from = date }
        fun fromDay(relativeDays: Int) = this.apply { this.from = today.plusDays(relativeDays.toLong()) }
        fun toDay(date: LocalDate) = this.apply { this.to = date }
        fun toDay(relativeDays: Int) = this.apply { this.to = today.plusDays(relativeDays.toLong()) }
        fun toGroup(id: UUID) = this.apply { this.groupId = id }

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
        private var optionId: UUID? = null
        private var employeeId: UUID? = null

        fun fromDay(date: LocalDate) = this.apply { this.from = date }
        fun fromDay(relativeDays: Int) = this.apply { this.from = today.plusDays(relativeDays.toLong()) }
        fun toDay(date: LocalDate) = this.apply { this.to = date }
        fun toDay(relativeDays: Int) = this.apply { this.to = today.plusDays(relativeDays.toLong()) }
        fun withOption(id: UUID) = this.apply { this.optionId = id }
        fun createdBy(employeeId: UUID) = this.apply { this.employeeId = employeeId }

        fun save(): PlacementFixture {
            tx.insertTestNewServiceNeed(
                confirmedBy = employeeId ?: throw IllegalStateException("createdBy not set"),
                placementId = placementFixture.placementId,
                optionId = optionId ?: throw IllegalStateException("option not set"),
                period = FiniteDateRange(
                    from ?: placementFixture.placementPeriod.start,
                    to ?: placementFixture.placementPeriod.end
                )
            )
            return placementFixture
        }
    }
}

/**
 * This is needed for better control of implicit this references.
 *
 * See:
 * https://github.com/Kotlin/KEEP/pull/38
 * https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-dsl-marker/
 */
@DslMarker
annotation class TestFixture
