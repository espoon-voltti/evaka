// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.children

import fi.espoo.evaka.Audit
import fi.espoo.evaka.AuditId
import fi.espoo.evaka.absence.AbsenceCategory
import fi.espoo.evaka.absence.AbsenceType
import fi.espoo.evaka.absence.getAbsencesOfChildByRange
import fi.espoo.evaka.dailyservicetimes.DailyServiceTimes
import fi.espoo.evaka.dailyservicetimes.getChildDailyServiceTimes
import fi.espoo.evaka.placement.getPlacementSummary
import fi.espoo.evaka.serviceneed.ServiceNeedOptionPublicInfo
import fi.espoo.evaka.serviceneed.ServiceNeedSummary
import fi.espoo.evaka.serviceneed.getServiceNeedOptions
import fi.espoo.evaka.serviceneed.getServiceNeedSummary
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.getOperationalDatesForChild
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import java.time.YearMonth
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/citizen/children")
class ChildControllerCitizen(
    private val accessControl: AccessControl
) {
    @GetMapping
    fun getChildren(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock
    ): List<ChildAndPermittedActions> =
        db
            .connect { dbc ->
                dbc.read {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Citizen.Person.READ_CHILDREN,
                        user.id
                    )
                    val children = it.getChildrenByParent(user.id, clock.today())
                    val childIds = children.map { it.id }
                    val permittedActions =
                        accessControl.getPermittedActions<ChildId, Action.Citizen.Child>(
                            it,
                            user,
                            clock,
                            childIds
                        )
                    children.map { c ->
                        ChildAndPermittedActions.fromChild(c, permittedActions[c.id]!!)
                    }
                }
            }.also {
                Audit.CitizenChildrenRead.log(
                    targetId = AuditId(user.id),
                    meta = mapOf("count" to it.size)
                )
            }

    @GetMapping("/{childId}/service-needs")
    fun getChildServiceNeeds(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @PathVariable childId: ChildId
    ): List<ServiceNeedSummary> =
        db
            .connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Citizen.Child.READ_SERVICE_NEEDS,
                        childId
                    )
                    val serviceNeeds = tx.getServiceNeedSummary(childId)
                    val missingServiceNeeds = getMissingServiceNeeds(tx, childId, serviceNeeds)
                    serviceNeeds + missingServiceNeeds
                }
            }.also { Audit.CitizenChildServiceNeedRead.log(targetId = AuditId(childId)) }

    private fun getMissingServiceNeeds(
        tx: Database.Read,
        childId: ChildId,
        serviceNeeds: List<ServiceNeedSummary>
    ): List<ServiceNeedSummary> {
        val defaultServiceNeedOptions =
            tx
                .getServiceNeedOptions()
                .filter { it.defaultOption }
                .associateBy { it.validPlacementType }
        val serviceNeedDateRanges = serviceNeeds.map { FiniteDateRange(it.startDate, it.endDate) }
        return tx.getPlacementSummary(childId).flatMap { placement ->
            val placementRange = FiniteDateRange(placement.startDate, placement.endDate)
            placementRange.complement(serviceNeedDateRanges).map {
                val defaultServiceNeedOption = defaultServiceNeedOptions[placement.type]
                ServiceNeedSummary(
                    it.start,
                    it.end,
                    defaultServiceNeedOption?.let { sno -> ServiceNeedOptionPublicInfo.of(sno) },
                    defaultServiceNeedOption?.contractDaysPerMonth,
                    placement.unit.name
                )
            }
        }
    }

    @GetMapping("/{childId}/attendance-summary/{yearMonth}")
    fun getChildAttendanceSummary(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @PathVariable childId: ChildId,
        @PathVariable yearMonth: YearMonth
    ): AttendanceSummary =
        db
            .connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Citizen.Child.READ_ATTENDANCE_SUMMARY,
                        childId
                    )

                    val range = FiniteDateRange(yearMonth.atDay(1), yearMonth.atEndOfMonth())
                    val operationalDates = tx.getOperationalDatesForChild(range, childId)
                    val plannedAbsences =
                        tx.getAbsencesOfChildByRange(childId, range.asDateRange()).filter { absence ->
                            operationalDates.contains(absence.date) &&
                                absence.category == AbsenceCategory.BILLABLE &&
                                setOf(
                                    AbsenceType.PLANNED_ABSENCE,
                                    AbsenceType.FREE_ABSENCE,
                                    AbsenceType.PARENTLEAVE
                                ).contains(absence.absenceType)
                        }

                    AttendanceSummary(
                        attendanceDays = operationalDates.count() - plannedAbsences.count()
                    )
                }
            }.also { Audit.CitizenChildAttendanceSummaryRead.log(targetId = AuditId(childId)) }

    @GetMapping("/{childId}/daily-service-times")
    fun getChildDailyServiceTimes(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @PathVariable childId: ChildId
    ): List<DailyServiceTimes> =
        db
            .connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Citizen.Child.READ_DAILY_SERVICE_TIMES,
                        childId
                    )
                    tx.getChildDailyServiceTimes(childId)
                }
            }.also { Audit.CitizenChildDailyServiceTimeRead.log(targetId = AuditId(childId)) }
}

data class AttendanceSummary(
    val attendanceDays: Int
)
