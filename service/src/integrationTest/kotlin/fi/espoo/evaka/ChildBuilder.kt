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

class ChildBuilder(
    private val tx: Database.Transaction,
    private val today: LocalDate
) {
    fun childOfAge(years: Int, months: Int = 0, days: Int = 0): ChildFixture {
        val dob = today.minusYears(years.toLong()).minusMonths(months.toLong()).minusDays(days.toLong())
        val childId = tx.insertTestPerson(DevPerson(dateOfBirth = dob))
        tx.insertTestChild(DevChild(childId))
        return ChildFixture(tx, today, childId)
    }

    class ChildFixture(
        private val tx: Database.Transaction,
        private val today: LocalDate,
        val childId: UUID
    ) {
        fun hasAssistanceNeed() = AssistanceNeedBuilder(tx, today, this)
        fun hasPlacementPlan() = PlacementPlanBuilder(tx, today, this)
        fun hasBackupCare() = BackupCareBuilder(tx, today, this)
        fun hasPlacement() = PlacementBuilder(tx, today, this)
        fun hasAbsence() = AbsenceBuilder(tx, today, this)
    }

    class AssistanceNeedBuilder(
        private val tx: Database.Transaction,
        private val today: LocalDate,
        private val childFixture: ChildFixture
    ) {
        private var employeeId: UUID? = null
        private var factor: Double = 1.0
        private var from: Int = 0
        private var to: Int = 0

        fun withFactor(factor: Double) = this.apply { this.factor = factor }
        fun fromDay(day: Int) = this.apply { this.from = day }
        fun toDay(day: Int) = this.apply { this.to = day }
        fun createdBy(employeeId: UUID) = this.apply { this.employeeId = employeeId }

        fun execAndReturn(): ChildFixture {
            tx.insertTestAssistanceNeed(
                DevAssistanceNeed(
                    childId = childFixture.childId,
                    updatedBy = employeeId ?: throw IllegalStateException("createdBy not set"),
                    startDate = today.plusDays(from.toLong()),
                    endDate = today.plusDays(to.toLong()),
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
        private var day: Int = 0
        private var type: AbsenceType? = null
        private var careTypes: List<CareType>? = null

        fun onDay(day: Int) = this.apply { this.day = day }
        fun ofType(type: AbsenceType) = this.apply { this.type = type }
        fun forCareTypes(vararg careTypes: CareType) = this.apply { this.careTypes = careTypes.asList() }

        private fun exec() {
            careTypes?.forEach { careType ->
                tx.insertTestAbsence(
                    childId = childFixture.childId,
                    date = today.plusDays(day.toLong()),
                    absenceType = type ?: throw IllegalStateException("absence type not set"),
                    careType = careType
                )
            } ?: throw IllegalStateException("care types not set")
        }

        fun execAndReturn(): ChildFixture {
            exec()
            return childFixture
        }

        fun andAnother(): AbsenceBuilder {
            exec()
            return AbsenceBuilder(tx, today, childFixture)
        }
    }

    class PlacementPlanBuilder(
        private val tx: Database.Transaction,
        private val today: LocalDate,
        private val childFixture: ChildFixture
    ) {
        private var unitId: UUID? = null
        private var type: PlacementType? = null
        private var from: Int = 0
        private var to: Int = 0
        private var deleted = false

        fun toUnit(id: UUID) = this.apply { this.unitId = id }
        fun ofType(type: PlacementType) = this.apply { this.type = type }
        fun fromDay(day: Int) = this.apply { this.from = day }
        fun toDay(day: Int) = this.apply { this.to = day }
        fun asDeleted() = this.apply { this.deleted = true }

        fun execAndReturn(): ChildFixture {
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
                startDate = today.plusDays(from.toLong()),
                endDate = today.plusDays(to.toLong()),
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
        private var unitId: UUID? = null
        private var groupId: UUID? = null
        private var from: Int = 0
        private var to: Int = 0

        fun toUnit(id: UUID) = this.apply { this.unitId = id }
        fun toGroup(id: UUID) = this.apply { this.groupId = id }
        fun fromDay(day: Int) = this.apply { this.from = day }
        fun toDay(day: Int) = this.apply { this.to = day }

        fun execAndReturn(): ChildFixture {
            tx.insertTestBackUpCare(
                childId = childFixture.childId,
                unitId = unitId ?: throw IllegalStateException("unit not set"),
                startDate = today.plusDays(from.toLong()),
                endDate = today.plusDays(to.toLong()),
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
        private var unitId: UUID? = null
        private var type: PlacementType? = null
        private var from: Int = 0
        private var to: Int = 0

        fun toUnit(id: UUID) = this.apply { this.unitId = id }
        fun ofType(type: PlacementType) = this.apply { this.type = type }
        fun fromDay(day: Int) = this.apply { this.from = day }
        fun toDay(day: Int) = this.apply { this.to = day }

        fun exec(): PlacementFixture {
            val placementId = tx.insertTestPlacement(
                childId = childFixture.childId,
                unitId = unitId ?: throw IllegalStateException("unit not set"),
                type = type ?: throw IllegalStateException("type not set"),
                startDate = today.plusDays(from.toLong()),
                endDate = today.plusDays(to.toLong())
            )
            return PlacementFixture(
                tx = tx,
                today = today,
                childFixture = childFixture,
                placementId = placementId,
                placementPeriod = FiniteDateRange(today.plusDays(from.toLong()), today.plusDays(to.toLong()))
            )
        }

        fun execAndReturn(): ChildFixture {
            exec()
            return childFixture
        }
    }

    class PlacementFixture(
        private val tx: Database.Transaction,
        private val today: LocalDate,
        val childFixture: ChildFixture,
        val placementId: UUID,
        val placementPeriod: FiniteDateRange
    ) {
        fun withGroupPlacement() = GroupPlacementBuilder(tx, today, this)

        fun withServiceNeed() = ServiceNeedBuilder(tx, today, this)

        fun returnFromPlacement() = childFixture
    }

    class GroupPlacementBuilder(
        private val tx: Database.Transaction,
        private val today: LocalDate,
        private val placementFixture: PlacementFixture
    ) {
        private var groupId: UUID? = null
        private var from: LocalDate? = null
        private var to: LocalDate? = null

        fun toGroup(id: UUID) = this.apply { this.groupId = id }
        fun fromDay(day: Int) = this.apply { this.from = today.plusDays(day.toLong()) }
        fun toDay(day: Int) = this.apply { this.to = today.plusDays(day.toLong()) }

        fun execAndReturn(): PlacementFixture {
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
        private var optionId: UUID? = null
        private var from: LocalDate? = null
        private var to: LocalDate? = null
        private var employeeId: UUID? = null

        fun withOption(id: UUID) = this.apply { this.optionId = id }
        fun fromDay(day: Int) = this.apply { this.from = today.plusDays(day.toLong()) }
        fun toDay(day: Int) = this.apply { this.to = today.plusDays(day.toLong()) }
        fun createdBy(employeeId: UUID) = this.apply { this.employeeId = employeeId }

        fun execAndReturn(): PlacementFixture {
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
