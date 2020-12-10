package fi.espoo.evaka.attendance

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.insertTestChildAttendance
import fi.espoo.evaka.shared.dev.insertTestDaycareGroup
import fi.espoo.evaka.shared.dev.insertTestDaycareGroupPlacement
import fi.espoo.evaka.shared.dev.insertTestPlacement
import fi.espoo.evaka.shared.utils.zoneId
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testDaycare
import org.jdbi.v3.core.kotlin.mapTo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import java.util.UUID

class AttendanceUpkeepIntegrationTest : FullApplicationTest() {
    private val groupId = UUID.randomUUID()
    private val daycarePlacementId = UUID.randomUUID()
    private val placementStart = LocalDate.now().minusDays(30)
    private val placementEnd = LocalDate.now().plusDays(30)

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.resetDatabase()
            insertGeneralTestFixtures(tx.handle)
            tx.handle.insertTestDaycareGroup(DevDaycareGroup(id = groupId, daycareId = testDaycare.id))
            insertTestPlacement(
                h = tx.handle,
                id = daycarePlacementId,
                childId = testChild_1.id,
                unitId = testDaycare.id,
                startDate = placementStart,
                endDate = placementEnd,
                type = PlacementType.PRESCHOOL_DAYCARE
            )
            insertTestDaycareGroupPlacement(
                h = tx.handle,
                daycarePlacementId = daycarePlacementId,
                groupId = groupId,
                startDate = placementStart,
                endDate = placementEnd
            )
        }
    }

    @Test
    fun `null departures are set to 2359 of arrival day`() {
        // test with daylight saving time change on 25.10.2020
        val arrived = ZonedDateTime.of(
            LocalDate.of(2020, 10, 25).atTime(LocalTime.of(9, 0)),
            zoneId
        ).toInstant()

        db.transaction {
            insertTestChildAttendance(
                h = it.handle,
                childId = testChild_1.id,
                unitId = testDaycare.id,
                arrived = arrived,
                departed = null
            )
        }

        val (_, res, _) = http.post("/scheduled/attendance-upkeep")
            .response()
        assertEquals(204, res.statusCode)

        val attendances = db.read {
            it.createQuery(
                // language=sql
                """
                    SELECT *
                    FROM child_attendance
                """.trimIndent()
            ).mapTo<ChildAttendance>().list()
        }

        assertEquals(1, attendances.size)
        val expectedDeparted = ZonedDateTime.of(
            LocalDate.of(2020, 10, 25).atTime(LocalTime.of(23, 59)),
            zoneId
        ).toInstant()
        assertEquals(expectedDeparted, attendances.first().departed)
    }
}
