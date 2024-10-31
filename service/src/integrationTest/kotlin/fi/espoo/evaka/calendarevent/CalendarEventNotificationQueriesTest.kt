// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.calendarevent.CalendarEventType
import fi.espoo.evaka.calendarevent.ParentWithEvents
import fi.espoo.evaka.calendarevent.getParentsWithNewEventsAfter
import fi.espoo.evaka.daycare.domain.Language
import fi.espoo.evaka.shared.CalendarEventId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.dev.DevBackupCare
import fi.espoo.evaka.shared.dev.DevCalendarEvent
import fi.espoo.evaka.shared.dev.DevCalendarEventAttendee
import fi.espoo.evaka.shared.dev.DevDaycareGroupPlacement
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevFosterParent
import fi.espoo.evaka.shared.dev.DevGuardian
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testAdult_2
import fi.espoo.evaka.testAdult_3
import fi.espoo.evaka.testArea
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testChild_2
import fi.espoo.evaka.testChild_3
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDaycare2
import fi.espoo.evaka.testDaycareGroup
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test

class CalendarEventNotificationQueriesTest : PureJdbiTest(resetDbBeforeEach = true) {
    private val testDaycareGroup2 =
        testDaycareGroup.copy(id = GroupId(UUID.randomUUID()), name = "Test group 2")

    private val today: LocalDate = LocalDate.of(2023, 5, 1)
    private val now = HelsinkiDateTime.of(today, LocalTime.of(18, 0, 0))
    private val employee = DevEmployee()

    private fun insertTestData(
        placementStart: LocalDate = today.minusYears(1),
        placementEnd: LocalDate = today.plusYears(1),
    ) {
        db.transaction { tx ->
            tx.insert(testArea)
            tx.insert(testDaycare)
            tx.insert(testDaycare2.copy(financeDecisionHandler = null))
            tx.insert(testDaycareGroup)
            tx.insert(testDaycareGroup2)
            tx.insert(employee)

            tx.insert(testChild_1, DevPersonType.CHILD)
            tx.insert(
                DevPlacement(
                    childId = testChild_1.id,
                    unitId = testDaycare.id,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )

            tx.insert(testAdult_1, DevPersonType.RAW_ROW)
            tx.insert(DevGuardian(guardianId = testAdult_1.id, childId = testChild_1.id))

            tx.insert(testChild_2, DevPersonType.CHILD)
            tx.insert(
                    DevPlacement(
                        childId = testChild_2.id,
                        unitId = testDaycare.id,
                        startDate = placementStart,
                        endDate = placementEnd,
                    )
                )
                .also { placementId ->
                    tx.insert(
                        DevDaycareGroupPlacement(
                            daycarePlacementId = placementId,
                            daycareGroupId = testDaycareGroup.id,
                            startDate = today.minusYears(1),
                            endDate = today.plusYears(1),
                        )
                    )
                }

            tx.insert(testAdult_2, DevPersonType.RAW_ROW)
            tx.insert(DevGuardian(guardianId = testAdult_2.id, childId = testChild_2.id))

            tx.insert(testChild_3, DevPersonType.CHILD)
            tx.insert(
                    DevPlacement(
                        childId = testChild_3.id,
                        unitId = testDaycare.id,
                        startDate = placementStart,
                        endDate = placementEnd,
                    )
                )
                .also { placementId ->
                    tx.insert(
                        DevDaycareGroupPlacement(
                            daycarePlacementId = placementId,
                            daycareGroupId = testDaycareGroup2.id,
                            startDate = today.minusYears(1),
                            endDate = today.plusYears(1),
                        )
                    )
                }

            tx.insert(testAdult_3.copy(language = "sv"), DevPersonType.RAW_ROW)
            tx.insert(
                DevFosterParent(
                    parentId = testAdult_3.id,
                    childId = testChild_3.id,
                    validDuring = DateRange(today, today.plusYears(1)),
                    createdAt = now,
                    createdBy = employee.evakaUserId,
                )
            )
        }
    }

    @Test
    fun `No events`() {
        insertTestData()
        assertEquals(
            listOf(),
            db.read { tx -> tx.getParentsWithNewEventsAfter(today, now.minusHours(24)) },
        )
    }

    @Test
    fun `Unit-wide event`() {
        insertTestData()
        val eventId =
            createCalendarEvent(
                title = "Unit-wide event",
                period = FiniteDateRange(today, today),
                unitId = testDaycare.id,
            )

        assertEquals(
            listOf(
                    ParentWithEvents(
                        parentId = testAdult_1.id,
                        language = Language.fi,
                        events = listOf(eventId),
                    ),
                    ParentWithEvents(
                        parentId = testAdult_2.id,
                        language = Language.fi,
                        events = listOf(eventId),
                    ),
                    ParentWithEvents(
                        parentId = testAdult_3.id,
                        language = Language.sv,
                        events = listOf(eventId),
                    ),
                )
                .sortedBy { it.parentId },
            db.read { tx -> tx.getParentsWithNewEventsAfter(today, now.minusHours(24)) }
                .sortedBy { it.parentId },
        )
    }

    @Test
    fun `Group event`() {
        insertTestData()
        val eventId =
            createCalendarEvent(
                title = "Group event",
                period = FiniteDateRange(today, today),
                unitId = testDaycare.id,
                groupIds = listOf(testDaycareGroup.id),
            )

        assertEquals(
            listOf(
                ParentWithEvents(
                    parentId = testAdult_2.id,
                    language = Language.fi,
                    events = listOf(eventId),
                )
            ),
            db.read { tx -> tx.getParentsWithNewEventsAfter(today, now.minusHours(24)) },
        )
    }

    @Test
    fun `Notification plus backup care`() {
        insertTestData()
        db.transaction { tx ->
            // Backup care covers the whole event -> no notification
            tx.insert(
                DevBackupCare(
                    childId = testChild_1.id,
                    unitId = testDaycare2.id,
                    groupId = null,
                    period = FiniteDateRange(today, today.plusDays(1)),
                )
            )

            // Backup care in the same unit -> notification is sent
            tx.insert(
                DevBackupCare(
                    childId = testChild_2.id,
                    unitId = testDaycare.id,
                    groupId = testDaycareGroup2.id,
                    period = FiniteDateRange(today, today.plusDays(1)),
                )
            )

            // Backup care covers only part of the event -> notification is sent
            tx.insert(
                DevBackupCare(
                    childId = testChild_3.id,
                    unitId = testDaycare2.id,
                    groupId = null,
                    period = FiniteDateRange(today, today),
                )
            )
        }
        val eventId =
            createCalendarEvent(
                title = "Backup care",
                period = FiniteDateRange(today, today.plusDays(1)),
                unitId = testDaycare.id,
            )

        assertEquals(
            listOf(
                    ParentWithEvents(
                        parentId = testAdult_2.id,
                        language = Language.fi,
                        events = listOf(eventId),
                    ),
                    ParentWithEvents(
                        parentId = testAdult_3.id,
                        language = Language.sv,
                        events = listOf(eventId),
                    ),
                )
                .sortedBy { it.parentId },
            db.read { tx -> tx.getParentsWithNewEventsAfter(today, now.minusHours(24)) }
                .sortedBy { it.parentId },
        )
    }

    @Test
    fun `Specific children event`() {
        insertTestData()
        // testChild_3 is in backup care, but the parent still gets notified about child-specific
        // events
        db.transaction { tx ->
            tx.insert(
                DevBackupCare(
                    childId = testChild_3.id,
                    unitId = testDaycare2.id,
                    groupId = null,
                    period = FiniteDateRange(today, today),
                )
            )
        }
        val eventId =
            createCalendarEvent(
                title = "Child event",
                period = FiniteDateRange(today, today),
                unitId = testDaycare.id,
                groupChildIds =
                    listOf(
                        testDaycareGroup.id to testChild_2.id,
                        testDaycareGroup2.id to testChild_3.id,
                    ),
            )

        assertEquals(
            listOf(
                    ParentWithEvents(
                        parentId = testAdult_2.id,
                        language = Language.fi,
                        events = listOf(eventId),
                    ),
                    ParentWithEvents(
                        parentId = testAdult_3.id,
                        language = Language.sv,
                        events = listOf(eventId),
                    ),
                )
                .sortedBy { it.parentId },
            db.read { tx -> tx.getParentsWithNewEventsAfter(today, now.minusHours(24)) }
                .sortedBy { it.parentId },
        )
    }

    @Test
    fun `No notification if placement has ended`() {
        insertTestData(placementEnd = today.minusDays(1))

        createCalendarEvent(
            title = "Unit-wide event",
            period = FiniteDateRange(today.minusDays(1), today),
            unitId = testDaycare.id,
            created = now.minusHours(24),
        )
        createCalendarEvent(
            title = "Group-wide event",
            period = FiniteDateRange(today.minusDays(1), today),
            unitId = testDaycare.id,
            groupIds = listOf(testDaycareGroup.id),
            created = now.minusHours(24),
        )
        createCalendarEvent(
            title = "Child event",
            period = FiniteDateRange(today.minusDays(1), today),
            unitId = testDaycare.id,
            groupIds = listOf(testDaycareGroup2.id),
            groupChildIds = listOf(testDaycareGroup2.id to testChild_3.id),
            created = now.minusHours(24),
        )
        assertEquals(
            listOf(),
            db.read { tx -> tx.getParentsWithNewEventsAfter(today, now.minusHours(24)) },
        )
    }

    @Test
    fun `No notification for events created over 24 hours ago`() {
        insertTestData()
        createCalendarEvent(
            title = "Unit-wide event",
            period = FiniteDateRange(today, today),
            unitId = testDaycare.id,
            created = now.minusHours(24).minusMinutes(1),
        )

        assertEquals(
            listOf(),
            db.read { tx -> tx.getParentsWithNewEventsAfter(today, now.minusHours(24)) },
        )
    }

    private fun createCalendarEvent(
        title: String,
        description: String = "description",
        period: FiniteDateRange,
        created: HelsinkiDateTime = now,
        unitId: DaycareId,
        groupIds: List<GroupId> = listOf(),
        groupChildIds: List<Pair<GroupId, ChildId>> = listOf(),
    ): CalendarEventId =
        db.transaction { tx ->
            val eventId =
                tx.insert(
                    DevCalendarEvent(
                        title = title,
                        description = description,
                        period = period,
                        modifiedAt = created,
                        eventType = CalendarEventType.DAYCARE_EVENT,
                    )
                )

            tx.createUpdate {
                    sql(
                        """
                    UPDATE calendar_event
                    SET created_at = ${bind(created)} WHERE id = ${bind(eventId)}
                    """
                    )
                }
                .execute()

            groupIds.forEach { groupId ->
                tx.insert(
                    DevCalendarEventAttendee(
                        calendarEventId = eventId,
                        unitId = unitId,
                        groupId = groupId,
                    )
                )
            }
            groupChildIds.forEach { (groupId, childId) ->
                tx.insert(
                    DevCalendarEventAttendee(
                        calendarEventId = eventId,
                        unitId = unitId,
                        groupId = groupId,
                        childId = childId,
                    )
                )
            }

            if (groupIds.isEmpty() && groupChildIds.isEmpty()) {
                // Unit-wide event
                tx.insert(DevCalendarEventAttendee(calendarEventId = eventId, unitId = unitId))
            }

            eventId
        }
}
