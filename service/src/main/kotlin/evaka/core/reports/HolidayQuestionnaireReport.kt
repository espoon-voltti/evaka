// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.reports

import evaka.core.Audit
import evaka.core.assistance.getAssistanceFactorsForChildrenOverRange
import evaka.core.attendance.occupancyCoefficientSeven
import evaka.core.daycare.getDaycare
import evaka.core.holidayperiod.HolidayQuestionnaire
import evaka.core.holidayperiod.QuestionnaireType
import evaka.core.holidayperiod.getFixedPeriodQuestionnaire
import evaka.core.holidayperiod.getOpenRangesQuestionnaire
import evaka.core.holidayperiod.getQuestionnaireAnswers
import evaka.core.reservations.getReservationBackupPlacements
import evaka.core.shared.DaycareId
import evaka.core.shared.FeatureConfig
import evaka.core.shared.GroupId
import evaka.core.shared.HolidayQuestionnaireId
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.data.DateMap
import evaka.core.shared.data.DateSet
import evaka.core.shared.db.Database
import evaka.core.shared.domain.BadRequest
import evaka.core.shared.domain.EvakaClock
import evaka.core.shared.domain.FiniteDateRange
import evaka.core.shared.domain.NotFound
import evaka.core.shared.domain.getHolidays
import evaka.core.shared.domain.getOperationalDatesForChildren
import evaka.core.shared.security.AccessControl
import evaka.core.shared.security.Action
import java.time.Period
import kotlin.math.ceil
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class HolidayQuestionnaireReport(
    private val accessControl: AccessControl,
    private val featureConfig: FeatureConfig,
) {
    @GetMapping("/employee/reports/holiday-questionnaire")
    fun getHolidayQuestionnaireReport(
        db: Database,
        clock: EvakaClock,
        user: AuthenticatedUser.Employee,
        @RequestParam(required = false) groupIds: Set<GroupId> = emptySet(),
        @RequestParam unitId: DaycareId,
        @RequestParam questionnaireId: HolidayQuestionnaireId,
    ): List<HolidayReportRow> {
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Unit.READ_HOLIDAY_QUESTIONNAIRE_REPORT,
                        unitId,
                    )
                    tx.setStatementTimeout(REPORT_STATEMENT_TIMEOUT)

                    val unit = tx.getDaycare(unitId) ?: throw BadRequest("No such unit $unitId")
                    val unitOperationalDays = unit.shiftCareOperationDays ?: unit.operationDays

                    val questionnaire =
                        when (featureConfig.holidayQuestionnaireType) {
                            QuestionnaireType.FIXED_PERIOD -> {
                                tx.getFixedPeriodQuestionnaire(questionnaireId)
                            }

                            QuestionnaireType.OPEN_RANGES -> {
                                tx.getOpenRangesQuestionnaire(questionnaireId)
                            }
                        } ?: throw NotFound()
                    val period =
                        when (questionnaire) {
                            is HolidayQuestionnaire.FixedPeriodQuestionnaire -> {
                                val options = questionnaire.periodOptions
                                FiniteDateRange(options.first().start, options.last().end)
                            }

                            is HolidayQuestionnaire.OpenRangesQuestionnaire -> {
                                questionnaire.period
                            }
                        }
                    val holidays = getHolidays(period)

                    // report result days
                    val questionnaireDays =
                        period
                            .dates()
                            .map { HolidayReportDay(it, holidays.contains(it)) }
                            .filter { day ->
                                (questionnaire is HolidayQuestionnaire.FixedPeriodQuestionnaire &&
                                    questionnaire.periodOptions.any { range ->
                                        range.includes(day.date)
                                    }) ||
                                    (questionnaire is
                                        HolidayQuestionnaire.OpenRangesQuestionnaire &&
                                        questionnaire.period.includes(day.date))
                            }
                            .filter {
                                unitOperationalDays.contains(it.date.dayOfWeek.value) &&
                                    (unit.shiftCareOpenOnHolidays || !it.isHoliday)
                            }
                            .toList()

                    // incoming back up children
                    val incomingBackupCaresByChild =
                        tx.getIncomingBackupCaresOverPeriodForGroupsInUnit(
                                unitId = unitId,
                                groupIds = groupIds,
                                period = period,
                            )
                            .groupBy { it.childId }
                            .mapValues { entry ->
                                DateMap.of(entry.value.map { it.validDuring to it })
                            }

                    val backupChildrenInUnit = incomingBackupCaresByChild.keys

                    val backupChildDataByChild =
                        tx.getServiceNeedOccupancyInfoOverRangeForChildren(
                                backupChildrenInUnit,
                                period,
                            )
                            .groupBy { it.child.id }

                    // directly placed children
                    val directlyPlacedChildData =
                        tx.getServiceNeedOccupancyInfoOverRangeForGroups(
                            range = period,
                            groupIds = groupIds,
                            unitId = unitId,
                        )

                    val directlyPlacedChildren = directlyPlacedChildData.map { it.child.id }.toSet()

                    // outgoing backup children
                    val backupCareOutgoingDataByChild =
                        tx.getReservationBackupPlacements(directlyPlacedChildren, period)

                    // all questionnaire answers
                    val questionnaireAnswers =
                        tx.getQuestionnaireAnswers(
                            questionnaireId,
                            (directlyPlacedChildren + backupChildrenInUnit).toList(),
                        )
                    val questionnaireAnswersByChild = questionnaireAnswers.groupBy { it.childId }

                    // all period assistance factor data
                    val assistanceFactorsByChild =
                        tx.getAssistanceFactorsForChildrenOverRange(
                                directlyPlacedChildren + backupChildrenInUnit,
                                period,
                            )
                            .groupBy { it.childId }
                            .mapValues { entry ->
                                DateMap.of(entry.value.map { it.validDuring to it.capacityFactor })
                            }

                    val assistanceRangesByChild =
                        tx.getAssistanceRanges(
                                directlyPlacedChildren + backupChildrenInUnit,
                                period,
                            )
                            .groupBy { it.childId }
                            .mapValues { entry -> DateSet.of(entry.value.map { it.validDuring }) }

                    val operationDaysByChild =
                        tx.getOperationalDatesForChildren(
                            period,
                            directlyPlacedChildren + backupChildrenInUnit,
                        )

                    questionnaireDays.map { h ->
                        val date = h.date
                        val dailyDirectlyPlacedData =
                            directlyPlacedChildData.filter { sn ->
                                sn.validity.includes(date) &&
                                    (backupCareOutgoingDataByChild[sn.child.id] ?: emptyList())
                                        .none { it.range.includes(date) }
                            }
                        val dailyBackupPlacedData =
                            incomingBackupCaresByChild.mapNotNull { (key, value) ->
                                val bc = value.getValue(date) ?: return@mapNotNull null
                                backupChildDataByChild[key]
                                    ?.firstOrNull { sn -> sn.validity.includes(date) }
                                    ?.copy(groupId = bc.groupId)
                            }

                        val dailyPlacedData = dailyDirectlyPlacedData + dailyBackupPlacedData

                        val dailyQuestionnaireAnswersByChild =
                            questionnaireAnswers
                                .filter { answer ->
                                    answer.fixedPeriod?.includes(date) == true ||
                                        answer.openRanges.any { it.includes(date) }
                                }
                                .groupBy { it.childId }

                        val (answered, noResponse) =
                            dailyPlacedData.partition {
                                questionnaireAnswersByChild.containsKey(it.child.id)
                            }
                        val present =
                            answered.filter {
                                !dailyQuestionnaireAnswersByChild.containsKey(it.child.id)
                            }

                        val dailyOccupancyCoefficient =
                            present.sumOf {
                                val ageAtDate = Period.between(it.child.dateOfBirth, date).years
                                val assistanceFactor =
                                    assistanceFactorsByChild[it.child.id]?.getValue(date) ?: 1.0
                                if (ageAtDate < 3) it.coefficientUnder3y * assistanceFactor
                                else it.coefficient * assistanceFactor
                            }
                        val staffNeedAtDate =
                            ceil(
                                    dailyOccupancyCoefficient.div(
                                        occupancyCoefficientSeven.toDouble()
                                    )
                                )
                                .toInt()

                        HolidayReportRow(
                            date = date,
                            presentChildren =
                                present.map { info ->
                                    ChildWithName(
                                        id = info.child.id,
                                        firstName = info.child.firstName,
                                        lastName = info.child.lastName,
                                    )
                                },
                            assistanceChildren =
                                present
                                    .filter {
                                        assistanceRangesByChild[it.child.id]?.includes(date) == true
                                    }
                                    .map { info ->
                                        ChildWithName(
                                            id = info.child.id,
                                            firstName = info.child.firstName,
                                            lastName = info.child.lastName,
                                        )
                                    },
                            presentOccupancyCoefficient = dailyOccupancyCoefficient,
                            requiredStaff = staffNeedAtDate,
                            absentCount = answered.size - present.size,
                            noResponseChildren =
                                noResponse
                                    .filter {
                                        operationDaysByChild[it.child.id]?.contains(date) == true
                                    }
                                    .map { info ->
                                        ChildWithName(
                                            id = info.child.id,
                                            firstName = info.child.firstName,
                                            lastName = info.child.lastName,
                                        )
                                    },
                        )
                    }
                }
            }
            .also {
                Audit.HolidayQuestionnaireReport.log(
                    meta = mapOf("unitId" to unitId, "questionnaireId" to questionnaireId)
                )
            }
    }
}
