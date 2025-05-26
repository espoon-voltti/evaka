// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.Audit
import fi.espoo.evaka.assistance.getAssistanceFactorsForChildrenOverRange
import fi.espoo.evaka.attendance.occupancyCoefficientSeven
import fi.espoo.evaka.daycare.getDaycare
import fi.espoo.evaka.daycare.getPreschoolTerms
import fi.espoo.evaka.holidayperiod.HolidayQuestionnaire
import fi.espoo.evaka.holidayperiod.QuestionnaireType
import fi.espoo.evaka.holidayperiod.getFixedPeriodQuestionnaire
import fi.espoo.evaka.holidayperiod.getOpenRangesQuestionnaire
import fi.espoo.evaka.holidayperiod.getQuestionnaireAnswers
import fi.espoo.evaka.reservations.getReservationBackupPlacements
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.FeatureConfig
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.HolidayQuestionnaireId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.data.DateMap
import fi.espoo.evaka.shared.data.DateSet
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.domain.getHolidays
import fi.espoo.evaka.shared.domain.getOperationalDatesForChildren
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
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
                            QuestionnaireType.FIXED_PERIOD ->
                                tx.getFixedPeriodQuestionnaire(questionnaireId)
                            QuestionnaireType.OPEN_RANGES ->
                                tx.getOpenRangesQuestionnaire(questionnaireId)
                        } ?: throw NotFound()
                    val period =
                        when (questionnaire.type) {
                            QuestionnaireType.FIXED_PERIOD -> {
                                val options =
                                    (questionnaire as HolidayQuestionnaire.FixedPeriodQuestionnaire)
                                        .periodOptions
                                FiniteDateRange(options.first().start, options.last().end)
                            }
                            QuestionnaireType.OPEN_RANGES ->
                                (questionnaire as HolidayQuestionnaire.OpenRangesQuestionnaire)
                                    .period
                        }
                    val holidays = getHolidays(period)

                    val preschoolTerms = tx.getPreschoolTerms()

                    // report result days
                    val questionnaireDays =
                        period
                            .dates()
                            .map { HolidayReportDay(it, holidays.contains(it)) }
                            .filter { day ->
                                questionnaire.type == QuestionnaireType.FIXED_PERIOD &&
                                    (questionnaire as HolidayQuestionnaire.FixedPeriodQuestionnaire)
                                        .periodOptions
                                        .any { range -> range.includes(day.date) }
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

                    questionnaireDays.map { (date) ->
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
                                present.map { (child) ->
                                    ChildWithName(
                                        id = child.id,
                                        firstName = child.firstName,
                                        lastName = child.lastName,
                                    )
                                },
                            assistanceChildren =
                                present
                                    .filter {
                                        assistanceRangesByChild[it.child.id]?.includes(date) == true
                                    }
                                    .map { (child) ->
                                        ChildWithName(
                                            id = child.id,
                                            firstName = child.firstName,
                                            lastName = child.lastName,
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
                                    .map { (child) ->
                                        ChildWithName(
                                            id = child.id,
                                            firstName = child.firstName,
                                            lastName = child.lastName,
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
