// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.koski

import fi.espoo.evaka.absence.AbsenceType
import fi.espoo.evaka.shared.data.DateSet
import fi.espoo.evaka.shared.domain.FiniteDateRange
import java.time.LocalDate
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test

/*
AA = absence
HH = holiday
 */
class KoskiTest {
    private val preschoolTerm2020 =
        FiniteDateRange(LocalDate.of(2020, 8, 13), LocalDate.of(2021, 6, 4))

    /*
             Mo Tu We Th Fr Sa Su
    Week 12  AA AA AA AA AA 27 28
    Week 13  29 30 31  1 HH  3 HH
    Week 14  HH  6  7  8  9 10 11
    Week 15  12 13 14 15 16 17 18

    Absent 22.3 - 26.3, but no Koski absence is generated:
        - the actual absence period is just 5 days
     */
    @Test
    fun testSimpleAbsenceScenario1() {
        val timelines =
            calculatePreparatoryAbsences(
                placements = DateSet.of(preschoolTerm2020),
                holidays = holidays,
                absences =
                    mapOf(
                        AbsenceType.PLANNED_ABSENCE to
                            setOf(
                                LocalDate.of(2021, 3, 22),
                                LocalDate.of(2021, 3, 23),
                                LocalDate.of(2021, 3, 24),
                                LocalDate.of(2021, 3, 25),
                                LocalDate.of(2021, 3, 26)
                            )
                    )
            )
        assertEquals(
            PreparatoryAbsences(
                plannedAbsence = DateSet.of(),
                sickLeaveAbsence = DateSet.of(),
                unknownAbsence = DateSet.of()
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
        val timelines =
            calculatePreparatoryAbsences(
                placements = DateSet.of(preschoolTerm2020),
                holidays = holidays,
                absences =
                    mapOf(
                        AbsenceType.UNKNOWN_ABSENCE to
                            setOf(
                                LocalDate.of(2021, 3, 22),
                                LocalDate.of(2021, 3, 23),
                                LocalDate.of(2021, 3, 24),
                                LocalDate.of(2021, 3, 25),
                                LocalDate.of(2021, 3, 26),
                                LocalDate.of(2021, 3, 29)
                            )
                    )
            )
        assertEquals(
            PreparatoryAbsences(
                plannedAbsence = DateSet.of(),
                sickLeaveAbsence = DateSet.of(),
                unknownAbsence =
                    DateSet.of(
                        FiniteDateRange(LocalDate.of(2021, 3, 22), LocalDate.of(2021, 3, 29))
                    )
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

    Sick leave 22.3 - 29.3, and a Koski absence is generated
     */
    @Test
    fun testSimpleAbsenceScenario3() {
        val timelines =
            calculatePreparatoryAbsences(
                placements = DateSet.of(preschoolTerm2020),
                holidays = holidays,
                absences =
                    mapOf(
                        AbsenceType.SICKLEAVE to
                            setOf(
                                LocalDate.of(2021, 3, 22),
                                LocalDate.of(2021, 3, 23),
                                LocalDate.of(2021, 3, 24),
                                LocalDate.of(2021, 3, 25),
                                LocalDate.of(2021, 3, 26),
                                LocalDate.of(2021, 3, 29)
                            )
                    )
            )
        assertEquals(
            PreparatoryAbsences(
                plannedAbsence = DateSet.of(),
                sickLeaveAbsence =
                    DateSet.of(
                        FiniteDateRange(LocalDate.of(2021, 3, 22), LocalDate.of(2021, 3, 29))
                    ),
                unknownAbsence = DateSet.of()
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
        val timelines =
            calculatePreparatoryAbsences(
                placements = DateSet.of(preschoolTerm2020),
                holidays = holidays,
                absences =
                    mapOf(
                        AbsenceType.PLANNED_ABSENCE to
                            setOf(
                                LocalDate.of(2021, 3, 26),
                                LocalDate.of(2021, 3, 29),
                                LocalDate.of(2021, 3, 30),
                                LocalDate.of(2021, 3, 31),
                                LocalDate.of(2021, 4, 1)
                            )
                    )
            )
        assertEquals(
            PreparatoryAbsences(
                plannedAbsence = DateSet.of(),
                sickLeaveAbsence = DateSet.of(),
                unknownAbsence = DateSet.of()
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
        val timelines =
            calculatePreparatoryAbsences(
                placements = DateSet.of(preschoolTerm2020),
                holidays = holidays,
                absences =
                    mapOf(
                        AbsenceType.PLANNED_ABSENCE to
                            setOf(
                                LocalDate.of(2021, 3, 29),
                                LocalDate.of(2021, 3, 30),
                                LocalDate.of(2021, 3, 31),
                                LocalDate.of(2021, 4, 1)
                            ),
                        AbsenceType.OTHER_ABSENCE to setOf(LocalDate.of(2021, 4, 6))
                    )
            )
        assertEquals(
            PreparatoryAbsences(
                plannedAbsence =
                    DateSet.of(
                        FiniteDateRange(LocalDate.of(2021, 3, 29), LocalDate.of(2021, 4, 6))
                    ),
                sickLeaveAbsence = DateSet.of(),
                unknownAbsence = DateSet.of()
            ),
            timelines
        )
    }
}

private val holidays =
    setOf(LocalDate.of(2021, 4, 2), LocalDate.of(2021, 4, 4), LocalDate.of(2021, 4, 5))
