// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reservations

import fi.espoo.evaka.absence.AbsenceCategory
import fi.espoo.evaka.daycare.PreschoolTerm
import fi.espoo.evaka.daycare.domain.Language
import fi.espoo.evaka.daycare.getDaycare
import fi.espoo.evaka.daycare.getPreschoolTerms
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.placement.getPlacementsForChildDuring
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.HelsinkiDateTimeRange
import fi.espoo.evaka.shared.domain.TimeRange
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime

fun getExpectedAbsenceCategories(
    tx: Database.Read,
    date: LocalDate,
    childId: ChildId,
    attendanceTimes: List<TimeRange>,
): Set<AbsenceCategory>? {
    val placement =
        tx.getPlacementsForChildDuring(childId, date, date).firstOrNull()
            ?: throw BadRequest("child has no placement")
    val daycare = tx.getDaycare(placement.unitId)!!

    return getExpectedAbsenceCategories(
        date = date,
        attendanceTimes = attendanceTimes,
        placementType = placement.type,
        unitLanguage = daycare.language,
        dailyPreschoolTime = daycare.dailyPreschoolTime,
        dailyPreparatoryTime = daycare.dailyPreparatoryTime,
        preschoolTerms = tx.getPreschoolTerms(),
    )
}

// null return value means that absences should not be deducted from reservations or attendances
fun getExpectedAbsenceCategories(
    date: LocalDate,
    attendanceTimes: List<TimeRange>,
    placementType: PlacementType,
    unitLanguage: Language,
    dailyPreschoolTime: TimeRange?,
    dailyPreparatoryTime: TimeRange?,
    preschoolTerms: List<PreschoolTerm>,
): Set<AbsenceCategory>? {
    val presences = attendanceTimes.map { it.asHelsinkiDateTimeRange(date) }

    val preschoolEducationOnGoing =
        preschoolTerms.any {
            val term = if (unitLanguage == Language.sv) it.swedishPreschool else it.finnishPreschool
            term.includes(date) && !it.termBreaks.includes(date)
        }
    val preparatoryEducationOnGoing = preschoolEducationOnGoing

    val preschoolTime =
        (dailyPreschoolTime ?: TimeRange(LocalTime.of(9, 0), LocalTime.of(13, 0)))
            .asHelsinkiDateTimeRange(date)
    val beforePreschoolTime =
        HelsinkiDateTimeRange.of(
            date = date,
            startTime = LocalTime.of(0, 0),
            endTime = preschoolTime.start.toLocalTime(),
        )
    val afterPreschoolTime =
        HelsinkiDateTimeRange.of(
            date = date,
            startTime = preschoolTime.end.toLocalTime(),
            endTime = LocalTime.of(23, 59),
        )

    val preparatoryTime =
        (dailyPreparatoryTime ?: TimeRange(LocalTime.of(9, 0), LocalTime.of(14, 0)))
            .asHelsinkiDateTimeRange(date)
    val beforePreparatoryTime =
        HelsinkiDateTimeRange.of(
            date = date,
            startTime = LocalTime.of(0, 0),
            endTime = preparatoryTime.start.toLocalTime(),
        )
    val afterPreparatoryTime =
        HelsinkiDateTimeRange.of(
            date = date,
            startTime = preparatoryTime.end.toLocalTime(),
            endTime = LocalTime.of(23, 59),
        )

    return when (placementType) {
        PlacementType.PRESCHOOL ->
            setOfNotNull(
                    AbsenceCategory.NONBILLABLE.takeIf {
                        overlapTime(presences, preschoolTime) < Duration.ofMinutes(60)
                    }
                )
                .takeIf { preschoolEducationOnGoing }
        PlacementType.PRESCHOOL_DAYCARE,
        PlacementType.PRESCHOOL_CLUB ->
            if (preschoolEducationOnGoing) {
                setOfNotNull(
                    AbsenceCategory.NONBILLABLE.takeIf {
                        overlapTime(presences, preschoolTime) < Duration.ofMinutes(60)
                    },
                    AbsenceCategory.BILLABLE.takeIf {
                        overlapTime(presences, beforePreschoolTime) < Duration.ofMinutes(15) &&
                            overlapTime(presences, afterPreschoolTime) < Duration.ofMinutes(15)
                    },
                )
            } else {
                setOfNotNull(
                    AbsenceCategory.BILLABLE.takeIf {
                        totalTime(presences) < Duration.ofMinutes(15)
                    }
                )
            }
        PlacementType.PREPARATORY ->
            setOfNotNull(
                    AbsenceCategory.NONBILLABLE.takeIf {
                        overlapTime(presences, preparatoryTime) < Duration.ofMinutes(60)
                    }
                )
                .takeIf { preparatoryEducationOnGoing }
        PlacementType.PREPARATORY_DAYCARE ->
            if (preparatoryEducationOnGoing) {
                setOfNotNull(
                    AbsenceCategory.NONBILLABLE.takeIf {
                        overlapTime(presences, preparatoryTime) < Duration.ofMinutes(60)
                    },
                    AbsenceCategory.BILLABLE.takeIf {
                        overlapTime(presences, beforePreparatoryTime) < Duration.ofMinutes(15) &&
                            overlapTime(presences, afterPreparatoryTime) < Duration.ofMinutes(15)
                    },
                )
            } else {
                setOfNotNull(
                    AbsenceCategory.BILLABLE.takeIf {
                        totalTime(presences) < Duration.ofMinutes(15)
                    }
                )
            }
        PlacementType.DAYCARE,
        PlacementType.DAYCARE_PART_TIME,
        PlacementType.PRESCHOOL_DAYCARE_ONLY,
        PlacementType.PREPARATORY_DAYCARE_ONLY,
        PlacementType.TEMPORARY_DAYCARE,
        PlacementType.TEMPORARY_DAYCARE_PART_DAY ->
            setOfNotNull(
                AbsenceCategory.BILLABLE.takeIf { totalTime(presences) < Duration.ofMinutes(15) }
            )
        PlacementType.DAYCARE_FIVE_YEAR_OLDS,
        PlacementType.DAYCARE_PART_TIME_FIVE_YEAR_OLDS ->
            setOfNotNull(
                AbsenceCategory.NONBILLABLE.takeIf {
                    totalTime(presences) < Duration.ofMinutes(15)
                },
                AbsenceCategory.BILLABLE.takeIf {
                    totalTime(presences) < Duration.ofMinutes(4 * 60 + 15)
                },
            )
        PlacementType.SCHOOL_SHIFT_CARE ->
            setOfNotNull(
                AbsenceCategory.NONBILLABLE.takeIf { totalTime(presences) < Duration.ofMinutes(15) }
            )
        PlacementType.CLUB -> null
    }
}

private fun totalTime(times: List<HelsinkiDateTimeRange>): Duration =
    times.fold(Duration.ZERO) { totalDuration, range -> totalDuration + range.getDuration() }

private fun overlapTime(
    times: List<HelsinkiDateTimeRange>,
    schedule: HelsinkiDateTimeRange,
): Duration = totalTime(times.mapNotNull { it.intersection(schedule) })
