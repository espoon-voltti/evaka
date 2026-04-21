// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.holidayperiod

import evaka.core.Audit
import evaka.core.AuditId
import evaka.core.CitizenCalendarEnv
import evaka.core.EvakaEnv
import evaka.core.absence.AbsenceType
import evaka.core.absence.FullDayAbsenseUpsert
import evaka.core.absence.clearOldCitizenEditableAbsences
import evaka.core.absence.upsertFullDayAbsences
import evaka.core.daycare.Daycare
import evaka.core.daycare.domain.ProviderType
import evaka.core.daycare.getDaycare
import evaka.core.daycare.getDaycaresById
import evaka.core.daycare.isUnitOperationDay
import evaka.core.placement.Placement
import evaka.core.placement.PlacementType
import evaka.core.placement.getChildIdsWithPlacementInRange
import evaka.core.placement.getConsecutivePlacementRanges
import evaka.core.placement.getPlacementsForChildDuring
import evaka.core.reservations.clearOldReservations
import evaka.core.reservations.deleteAbsencesCreatedFromQuestionnaire
import evaka.core.reservations.getPlannedAbsenceEnabledRanges
import evaka.core.reservations.getReservableRange
import evaka.core.serviceneed.ServiceNeed
import evaka.core.serviceneed.ShiftCareType
import evaka.core.serviceneed.getServiceNeedsByChild
import evaka.core.shared.ChildId
import evaka.core.shared.DaycareId
import evaka.core.shared.FeatureConfig
import evaka.core.shared.HolidayQuestionnaireId
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.db.Database
import evaka.core.shared.domain.BadRequest
import evaka.core.shared.domain.EvakaClock
import evaka.core.shared.domain.FiniteDateRange
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.shared.domain.getHolidays
import evaka.core.shared.security.AccessControl
import evaka.core.shared.security.Action
import java.time.LocalDate
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

data class ActiveQuestionnaire(
    val questionnaire: HolidayQuestionnaire,
    val eligibleChildren: Map<ChildId, List<FiniteDateRange>>,
    val previousAnswers: List<HolidayQuestionnaireAnswer>,
)

@RestController
@RequestMapping("/citizen/holiday-period")
class HolidayPeriodControllerCitizen(
    private val accessControl: AccessControl,
    private val featureConfig: FeatureConfig,
    private val citizenCalendarEnv: CitizenCalendarEnv,
    private val env: EvakaEnv,
) {
    @GetMapping
    fun getHolidayPeriods(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
    ): List<HolidayPeriod> {
        return db.connect { dbc ->
                dbc.read {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Global.READ_HOLIDAY_PERIODS,
                    )
                    it.getHolidayPeriods()
                }
            }
            .also { Audit.HolidayPeriodsList.log(meta = mapOf("count" to it.size)) }
    }

    @GetMapping("/questionnaire")
    fun getActiveQuestionnaires(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
    ): List<ActiveQuestionnaire> {
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Global.READ_ACTIVE_HOLIDAY_QUESTIONNAIRES,
                    )
                    val activeQuestionnaire =
                        when (featureConfig.holidayQuestionnaireType) {
                            QuestionnaireType.FIXED_PERIOD -> {
                                tx.getActiveFixedPeriodQuestionnaire(clock.today())
                            }

                            QuestionnaireType.OPEN_RANGES -> {
                                tx.getActiveOpenRangesQuestionnaire(clock.today())
                            }
                        } ?: return@read listOf()

                    val eligibleChildren =
                        getEligibleChildren(
                            tx,
                            user,
                            clock.today(),
                            activeQuestionnaire,
                            citizenCalendarEnv.calendarOpenBeforePlacementDays,
                        )
                    if (eligibleChildren.isEmpty()) {
                        listOf()
                    } else {
                        listOf(
                            ActiveQuestionnaire(
                                questionnaire = activeQuestionnaire,
                                eligibleChildren = eligibleChildren,
                                previousAnswers =
                                    tx.getQuestionnaireAnswers(
                                        activeQuestionnaire.id,
                                        eligibleChildren.keys.toList(),
                                    ),
                            )
                        )
                    }
                }
            }
            .also { Audit.HolidayQuestionnairesList.log(meta = mapOf("count" to it.size)) }
    }

    @PostMapping("/questionnaire/fixed-period/{id}")
    fun answerFixedPeriodQuestionnaire(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @PathVariable id: HolidayQuestionnaireId,
        @RequestBody body: FixedPeriodsBody,
    ) {
        val now = clock.now()
        val today = now.toLocalDate()
        val childIds = body.fixedPeriods.keys

        db.connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.Citizen.Child.CREATE_HOLIDAY_ABSENCE,
                    childIds,
                )
                val questionnaire =
                    tx.getFixedPeriodQuestionnaire(id)?.also {
                        if (!it.active.includes(today))
                            throw BadRequest("Questionnaire is not open")
                    } ?: throw BadRequest("Questionnaire not found")
                validate(
                    questionnaire,
                    tx,
                    today,
                    user,
                    body.fixedPeriods.mapValues { (_, period) -> period?.let { listOf(it) } },
                )

                val absences =
                    body.fixedPeriods.entries.flatMap { (childId, period) ->
                        if (period == null || period.dates().none()) emptySequence()
                        else {
                            val placements =
                                tx.getPlacementsForChildDuring(childId, period.start, period.end)
                            val daycares = tx.getDaycaresById(placements.map { it.unitId }.toSet())
                            val serviceNeeds = tx.getServiceNeedsByChild(childId)
                            val holidays = getHolidays(period)
                            period
                                .dates()
                                .filter {
                                    isOperationalDay(
                                        it,
                                        placements,
                                        daycares,
                                        serviceNeeds,
                                        holidays,
                                    )
                                }
                                .map {
                                    FullDayAbsenseUpsert(
                                        childId = childId,
                                        date = it,
                                        absenceTypeBillable = questionnaire.absenceType,
                                        absenceTypeNonbillable = questionnaire.absenceType,
                                        questionnaireId = questionnaire.id,
                                    )
                                }
                        }
                    }

                upsertAbsences(tx, now, user, absences, questionnaire, childIds)
                tx.insertQuestionnaireAnswers(
                    user.id,
                    body.fixedPeriods.entries.map { (childId, period) ->
                        HolidayQuestionnaireAnswer(questionnaire.id, childId, period, listOf())
                    },
                )
            }
        }
        Audit.HolidayAbsenceCreate.log(targetId = AuditId(id), objectId = AuditId(childIds.toSet()))
    }

    @PostMapping("/questionnaire/open-range/{id}")
    fun answerOpenRangeQuestionnaire(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @PathVariable id: HolidayQuestionnaireId,
        @RequestBody body: OpenRangesBody,
    ) {
        val now = clock.now()
        val today = now.toLocalDate()
        val childIds = body.openRanges.keys

        db.connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.Citizen.Child.CREATE_HOLIDAY_ABSENCE,
                    childIds,
                )
                val questionnaire =
                    tx.getOpenRangesQuestionnaire(id)?.also {
                        if (!it.active.includes(today))
                            throw BadRequest("Questionnaire is not open")
                    } ?: throw BadRequest("Questionnaire not found")
                validate(questionnaire, tx, today, user, body.openRanges)

                val allRanges = body.openRanges.values.flatten()
                val plannedAbsenceEnabledRanges =
                    if (allRanges.isNotEmpty()) {
                        tx.getPlannedAbsenceEnabledRanges(
                            childIds,
                            FiniteDateRange(
                                allRanges.minOf { it.start },
                                allRanges.maxOf { it.end },
                            ),
                            env.plannedAbsenceEnabledForHourBasedServiceNeeds,
                        )
                    } else emptyMap()

                val absences =
                    body.openRanges.entries.flatMap { (childId, ranges) ->
                        val placements =
                            tx.getPlacementsForChildDuring(
                                childId,
                                ranges.minOf { it.start },
                                ranges.maxOf { it.end },
                            )
                        val daycares = tx.getDaycaresById(placements.map { it.unitId }.toSet())
                        val serviceNeeds = tx.getServiceNeedsByChild(childId)
                        val holidays =
                            getHolidays(
                                FiniteDateRange(ranges.minOf { it.start }, ranges.maxOf { it.end })
                            )
                        ranges.flatMap { range ->
                            val absenceType =
                                when {
                                    range.durationInDays() >=
                                        questionnaire.absenceTypeThreshold -> {
                                        questionnaire.absenceType
                                    }

                                    else -> {
                                        AbsenceType.OTHER_ABSENCE
                                    }
                                }
                            range
                                .dates()
                                .filter {
                                    isOperationalDay(
                                        it,
                                        placements,
                                        daycares,
                                        serviceNeeds,
                                        holidays,
                                    )
                                }
                                .map { date ->
                                    val plannedAbsenceEnabled =
                                        plannedAbsenceEnabledRanges[childId]?.includes(date)
                                            ?: false
                                    val billableType =
                                        if (
                                            absenceType == AbsenceType.OTHER_ABSENCE &&
                                                plannedAbsenceEnabled
                                        ) {
                                            AbsenceType.PLANNED_ABSENCE
                                        } else {
                                            absenceType
                                        }
                                    FullDayAbsenseUpsert(
                                        childId = childId,
                                        date = date,
                                        absenceTypeBillable = billableType,
                                        absenceTypeNonbillable = absenceType,
                                        questionnaireId = questionnaire.id,
                                    )
                                }
                        }
                    }

                upsertAbsences(tx, now, user, absences, questionnaire, childIds)
                tx.insertQuestionnaireAnswers(
                    user.id,
                    body.openRanges.entries.map { (childId, ranges) ->
                        HolidayQuestionnaireAnswer(questionnaire.id, childId, null, ranges)
                    },
                )
            }
        }
        Audit.HolidayAbsenceCreate.log(targetId = AuditId(id), objectId = AuditId(childIds.toSet()))
    }

    private fun validate(
        questionnaire: HolidayQuestionnaire,
        tx: Database.Transaction,
        today: LocalDate,
        user: AuthenticatedUser.Citizen,
        data: Map<ChildId, List<FiniteDateRange>?>,
    ) {
        val eligibleChildren =
            getEligibleChildren(
                tx,
                user,
                today,
                questionnaire,
                citizenCalendarEnv.calendarOpenBeforePlacementDays,
            )
        val invalid =
            data
                .mapNotNull { (childId, periods) ->
                    if (periods == null) {
                        return@mapNotNull null
                    }
                    val validPeriods =
                        eligibleChildren[childId] ?: return@mapNotNull childId to periods
                    val invalidPeriods =
                        periods.filterNot { period ->
                            validPeriods.any { validPeriod -> validPeriod.contains(period) }
                        }
                    if (invalidPeriods.isNotEmpty()) childId to invalidPeriods else null
                }
                .toMap()
        if (invalid.isNotEmpty()) {
            throw BadRequest(
                "Some children are not eligible to answer or invalid option provided ($invalid)"
            )
        }
    }

    private fun getEligibleChildren(
        tx: Database.Read,
        user: AuthenticatedUser.Citizen,
        date: LocalDate,
        questionnaire: HolidayQuestionnaire,
        calendarOpenBeforePlacementDays: Int,
    ): Map<ChildId, List<FiniteDateRange>> {
        val continuousPlacementPeriod = questionnaire.conditions.continuousPlacement
        val eligibleChildren =
            if (continuousPlacementPeriod != null) {
                    tx.getChildrenWithContinuousPlacement(date, user.id, continuousPlacementPeriod)
                } else {
                    tx.getUserChildIds(date, user.id)
                }
                .filter { childId ->
                    tx.getPlacementsForChildDuring(childId, date, date).none { placement ->
                        tx.getDaycare(placement.unitId)?.providerType ==
                            ProviderType.PRIVATE_SERVICE_VOUCHER
                    }
                }
        return when (questionnaire) {
            is HolidayQuestionnaire.FixedPeriodQuestionnaire -> {
                val periodOptions = questionnaire.periodOptions
                val min = periodOptions.minOf { it.start }
                val max = periodOptions.maxOf { it.end }
                val placementRangesByChild =
                    tx.getConsecutivePlacementRanges(
                        eligibleChildren,
                        PlacementType.invoiced,
                        FiniteDateRange(min, max),
                    )
                eligibleChildren
                    .mapNotNull { childId ->
                        placementRangesByChild[childId]?.let { placementRanges ->
                            val dates =
                                periodOptions.filter { option -> placementRanges.contains(option) }
                            if (dates.isNotEmpty()) childId to dates else null
                        }
                    }
                    .toMap()
            }

            is HolidayQuestionnaire.OpenRangesQuestionnaire -> {
                val placementRangesByChild =
                    tx.getConsecutivePlacementRanges(
                        eligibleChildren,
                        PlacementType.invoiced,
                        questionnaire.period,
                    )
                val childrenWithPlacementInWindow =
                    tx.getChildIdsWithPlacementInRange(
                        eligibleChildren,
                        FiniteDateRange(
                            date,
                            date.plusDays(calendarOpenBeforePlacementDays.toLong()),
                        ),
                    )
                eligibleChildren
                    .filter { childId ->
                        placementRangesByChild[childId]?.isNotEmpty() == true &&
                            childId in childrenWithPlacementInWindow
                    }
                    .associateWith { listOf(questionnaire.period) }
            }
        }
    }

    private fun upsertAbsences(
        tx: Database.Transaction,
        now: HelsinkiDateTime,
        user: AuthenticatedUser.Citizen,
        absences: List<FullDayAbsenseUpsert>,
        questionnaire: HolidayQuestionnaire,
        childIds: Set<ChildId>,
        config: FeatureConfig = featureConfig,
    ) {
        val reservableRange = getReservableRange(now, config.citizenReservationThresholdHours)

        absences
            .map { absence -> absence.childId to absence.date }
            .let {
                tx.clearOldReservations(it)
                tx.clearOldCitizenEditableAbsences(it, reservableRange)
            }
        tx.deleteAbsencesCreatedFromQuestionnaire(questionnaire.id, childIds)
        tx.upsertFullDayAbsences(user.evakaUserId, now, absences)
    }

    private fun isOperationalDay(
        date: LocalDate,
        placements: List<Placement>,
        daycares: Map<DaycareId, Daycare>,
        serviceNeeds: List<ServiceNeed>,
        holidays: Set<LocalDate>,
    ): Boolean {
        val placement =
            placements.find { placement ->
                FiniteDateRange(placement.startDate, placement.endDate).includes(date)
            } ?: return false

        val daycare = daycares[placement.unitId] ?: return false
        val serviceNeed =
            serviceNeeds.find { serviceNeed ->
                serviceNeed.placementId == placement.id &&
                    FiniteDateRange(serviceNeed.startDate, serviceNeed.endDate).includes(date)
            }

        return isUnitOperationDay(
            daycare.operationDays,
            daycare.shiftCareOperationDays,
            daycare.shiftCareOpenOnHolidays,
            holidays,
            date,
            serviceNeed != null && serviceNeed.shiftCare != ShiftCareType.NONE,
        )
    }
}

data class FixedPeriodsBody(val fixedPeriods: Map<ChildId, FiniteDateRange?>)

data class OpenRangesBody(val openRanges: Map<ChildId, List<FiniteDateRange>>)
