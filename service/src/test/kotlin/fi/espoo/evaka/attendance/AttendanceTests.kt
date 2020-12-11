package fi.espoo.evaka.attendance

import fi.espoo.evaka.placement.PlacementType
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
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

    val dob5years: LocalDate = LocalDate.of(LocalDate.now().year - 5, 5, 1)

    @Test
    fun `wasAbsentFromPaidDaycare - false`() {
        assertFalse(
            wasAbsentFromPaidDaycare(ChildPlacementBasics(PlacementType.DAYCARE, dob5years), LocalTime.of(9, 0), LocalTime.of(13, 16))
        )
        assertFalse(
            wasAbsentFromPaidDaycare(ChildPlacementBasics(PlacementType.DAYCARE_PART_TIME, dob5years), LocalTime.of(9, 0), LocalTime.of(13, 0))
        )
        assertFalse(
            wasAbsentFromPaidDaycare(ChildPlacementBasics(PlacementType.DAYCARE, LocalDate.now().minusYears(3)), LocalTime.of(9, 0), LocalTime.of(13, 0))
        )
    }

    @Test
    fun `wasAbsentFromPaidDaycare - true`() {
        assertTrue(
            wasAbsentFromPaidDaycare(ChildPlacementBasics(PlacementType.DAYCARE, dob5years), LocalTime.of(9, 0), LocalTime.of(13, 15))
        )
    }
}
