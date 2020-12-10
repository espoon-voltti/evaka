// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.attendance

import fi.espoo.evaka.placement.PlacementType
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalTime

class AttendanceTests {
    @Test
    fun `wasAbsentFromPreschool - true`() {
        assertTrue(
            wasAbsentFromPreschool(PlacementType.PRESCHOOL_DAYCARE, LocalTime.of(8, 0), LocalTime.of(9, 59))
        )
        assertTrue(
            wasAbsentFromPreschool(PlacementType.PRESCHOOL_DAYCARE, LocalTime.of(12, 1), LocalTime.of(19, 0))
        )
        assertTrue(
            wasAbsentFromPreschool(PlacementType.PRESCHOOL, LocalTime.of(12, 1), LocalTime.of(19, 0))
        )
        assertTrue(
            wasAbsentFromPreschool(PlacementType.PREPARATORY_DAYCARE, LocalTime.of(13, 1), LocalTime.of(19, 0))
        )
        assertTrue(
            wasAbsentFromPreschool(PlacementType.PREPARATORY, LocalTime.of(13, 1), LocalTime.of(19, 0))
        )
    }

    @Test
    fun `wasAbsentFromPreschool - false`() {
        assertFalse(
            wasAbsentFromPreschool(PlacementType.PRESCHOOL_DAYCARE, LocalTime.of(9, 0), LocalTime.of(13, 0))
        )
        assertFalse(
            wasAbsentFromPreschool(PlacementType.PRESCHOOL_DAYCARE, LocalTime.of(8, 0), LocalTime.of(10, 0))
        )
        assertFalse(
            wasAbsentFromPreschool(PlacementType.PRESCHOOL_DAYCARE, LocalTime.of(12, 0), LocalTime.of(13, 0))
        )
        assertFalse(
            wasAbsentFromPreschool(PlacementType.PRESCHOOL, LocalTime.of(12, 0), LocalTime.of(13, 0))
        )
        assertFalse(
            wasAbsentFromPreschool(PlacementType.PREPARATORY_DAYCARE, LocalTime.of(13, 0), LocalTime.of(14, 0))
        )
        assertFalse(
            wasAbsentFromPreschool(PlacementType.PREPARATORY, LocalTime.of(13, 0), LocalTime.of(14, 0))
        )
    }

    @Test
    fun `wasAbsentFromPreschoolDaycare - false`() {
        assertFalse(
            wasAbsentFromPreschoolDaycare(PlacementType.PRESCHOOL_DAYCARE, LocalTime.of(8, 0), LocalTime.of(17, 0))
        )
        assertFalse(
            wasAbsentFromPreschoolDaycare(PlacementType.PRESCHOOL_DAYCARE, LocalTime.of(8, 44), LocalTime.of(13, 15))
        )
        assertFalse(
            wasAbsentFromPreschoolDaycare(PlacementType.PRESCHOOL_DAYCARE, LocalTime.of(8, 45), LocalTime.of(13, 16))
        )
        assertFalse(
            wasAbsentFromPreschoolDaycare(PlacementType.PRESCHOOL, LocalTime.of(9, 0), LocalTime.of(13, 0))
        )
        assertFalse(
            wasAbsentFromPreschoolDaycare(PlacementType.DAYCARE, LocalTime.of(9, 0), LocalTime.of(13, 0))
        )
    }

    @Test
    fun `wasAbsentFromPreschoolDaycare - true`() {
        assertTrue(
            wasAbsentFromPreschoolDaycare(PlacementType.PRESCHOOL_DAYCARE, LocalTime.of(9, 0), LocalTime.of(13, 0))
        )
        assertTrue(
            wasAbsentFromPreschoolDaycare(PlacementType.PRESCHOOL_DAYCARE, LocalTime.of(8, 45), LocalTime.of(13, 15))
        )
    }
}
