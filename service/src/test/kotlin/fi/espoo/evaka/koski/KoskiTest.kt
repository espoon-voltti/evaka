// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.koski

import fi.espoo.evaka.daycare.service.AbsenceType
import fi.espoo.evaka.preschoolTerm2020
import fi.espoo.evaka.shared.Timeline
import fi.espoo.evaka.shared.domain.ClosedPeriod
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

/*
AA = absence
HH = holiday
 */
class KoskiTest {
    /*
         Mo Tu We Th Fr Sa Su
Week 12  AA AA AA AA AA 27 28
Week 13  29 30 31  1 HH  3 HH
Week 14  HH  6  7  8  9 10 11
Week 15  12 13 14 15 16 17 18

Absent 22.3 - 26.3, but no Koski absence is not generated:
    - the actual absence period is just 5 days
     */
    @Test
    fun testSimpleAbsenceScenario1() {
        val timelines = calculateStudyRightTimelines(
            placementRanges = sequenceOf(preschoolTerm2020),
            holidays = holidays,
            absences = sequenceOf(
                KoskiPreparatoryAbsence(LocalDate.of(2021, 3, 22), AbsenceType.PLANNED_ABSENCE),
                KoskiPreparatoryAbsence(LocalDate.of(2021, 3, 23), AbsenceType.PLANNED_ABSENCE),
                KoskiPreparatoryAbsence(LocalDate.of(2021, 3, 24), AbsenceType.PLANNED_ABSENCE),
                KoskiPreparatoryAbsence(LocalDate.of(2021, 3, 25), AbsenceType.PLANNED_ABSENCE),
                KoskiPreparatoryAbsence(LocalDate.of(2021, 3, 26), AbsenceType.PLANNED_ABSENCE)
            )
        )
        assertEquals(
            StudyRightTimelines(
                placement = Timeline.of(preschoolTerm2020),
                present = Timeline.of(preschoolTerm2020),
                plannedAbsence = Timeline.of(),
                unknownAbsence = Timeline.of()
            ),
            timelines
        )
    }

    /*
         Mo Tu We Th Fr Sa Su
Week 12  AA AA AA AA AA 27 28
Week 13  AA 30 31  1 HH  3 HH
Week 14  HH  6  7  8  9 10 11
Week 15  12 13 14 15 16 17 18

Absent 22.3 - 29.3, and a Koski absence is generated
     */
    @Test
    fun testSimpleAbsenceScenario2() {
        val timelines = calculateStudyRightTimelines(
            placementRanges = sequenceOf(preschoolTerm2020),
            holidays = holidays,
            absences = sequenceOf(
                KoskiPreparatoryAbsence(LocalDate.of(2021, 3, 22), AbsenceType.UNKNOWN_ABSENCE),
                KoskiPreparatoryAbsence(LocalDate.of(2021, 3, 23), AbsenceType.UNKNOWN_ABSENCE),
                KoskiPreparatoryAbsence(LocalDate.of(2021, 3, 24), AbsenceType.UNKNOWN_ABSENCE),
                KoskiPreparatoryAbsence(LocalDate.of(2021, 3, 25), AbsenceType.UNKNOWN_ABSENCE),
                KoskiPreparatoryAbsence(LocalDate.of(2021, 3, 26), AbsenceType.UNKNOWN_ABSENCE),
                KoskiPreparatoryAbsence(LocalDate.of(2021, 3, 29), AbsenceType.UNKNOWN_ABSENCE)
            )
        )
        assertEquals(
            StudyRightTimelines(
                placement = Timeline.of(preschoolTerm2020),
                present = Timeline.of(
                    ClosedPeriod(preschoolTerm2020.start, LocalDate.of(2021, 3, 21)),
                    ClosedPeriod(LocalDate.of(2021, 3, 30), preschoolTerm2020.end)
                ),
                plannedAbsence = Timeline.of(),
                unknownAbsence = Timeline.of(ClosedPeriod(LocalDate.of(2021, 3, 22), LocalDate.of(2021, 3, 29)))
            ),
            timelines
        )
    }

    /*
         Mo Tu We Th Fr Sa Su
Week 12  22 23 24 25 AA 27 28
Week 13  AA AA AA AA HH  3 HH
Week 14  HH  6  7  8  9 10 11
Week 15  12 13 14 15 16 17 18

Absent 26.3 - 5.4, but no Koski absence is generated:
    - the actual absence period is just 7 days (26.3 - 1.4), since public holidays and weekend days are skipped
     */
    @Test
    fun testComplexAbsenceScenario1() {
        val timelines = calculateStudyRightTimelines(
            placementRanges = sequenceOf(preschoolTerm2020),
            holidays = holidays,
            absences = sequenceOf(
                KoskiPreparatoryAbsence(LocalDate.of(2021, 3, 26), AbsenceType.PLANNED_ABSENCE),
                KoskiPreparatoryAbsence(LocalDate.of(2021, 3, 29), AbsenceType.PLANNED_ABSENCE),
                KoskiPreparatoryAbsence(LocalDate.of(2021, 3, 30), AbsenceType.PLANNED_ABSENCE),
                KoskiPreparatoryAbsence(LocalDate.of(2021, 3, 31), AbsenceType.PLANNED_ABSENCE),
                KoskiPreparatoryAbsence(LocalDate.of(2021, 4, 1), AbsenceType.PLANNED_ABSENCE)
            )
        )
        assertEquals(
            StudyRightTimelines(
                placement = Timeline.of(preschoolTerm2020),
                present = Timeline.of(preschoolTerm2020),
                plannedAbsence = Timeline.of(),
                unknownAbsence = Timeline.of()
            ),
            timelines
        )
    }

    /*
         Mo Tu We Th Fr Sa Su
Week 12  22 23 24 25 26 27 28
Week 13  AA AA AA AA HH  3 HH
Week 14  HH AA  7  8  9 10 11
Week 15  12 13 14 15 16 17 18

Absent 29.3 - 6.4, and a Koski absence is generated
     */
    @Test
    fun testComplexAbsenceScenario2() {
        val timelines = calculateStudyRightTimelines(
            placementRanges = sequenceOf(preschoolTerm2020),
            holidays = holidays,
            absences = sequenceOf(
                KoskiPreparatoryAbsence(LocalDate.of(2021, 3, 29), AbsenceType.PLANNED_ABSENCE),
                KoskiPreparatoryAbsence(LocalDate.of(2021, 3, 30), AbsenceType.PLANNED_ABSENCE),
                KoskiPreparatoryAbsence(LocalDate.of(2021, 3, 31), AbsenceType.PLANNED_ABSENCE),
                KoskiPreparatoryAbsence(LocalDate.of(2021, 4, 1), AbsenceType.PLANNED_ABSENCE),
                KoskiPreparatoryAbsence(LocalDate.of(2021, 4, 6), AbsenceType.PLANNED_ABSENCE)
            )
        )
        assertEquals(
            StudyRightTimelines(
                placement = Timeline.of(preschoolTerm2020),
                present = Timeline.of(
                    ClosedPeriod(preschoolTerm2020.start, LocalDate.of(2021, 3, 28)),
                    ClosedPeriod(LocalDate.of(2021, 4, 7), preschoolTerm2020.end)
                ),
                plannedAbsence = Timeline.of(ClosedPeriod(LocalDate.of(2021, 3, 29), LocalDate.of(2021, 4, 6))),
                unknownAbsence = Timeline.of()
            ),
            timelines
        )
    }
}

private val holidays = setOf(
    LocalDate.of(2021, 4, 2),
    LocalDate.of(2021, 4, 4),
    LocalDate.of(2021, 4, 5)
)
