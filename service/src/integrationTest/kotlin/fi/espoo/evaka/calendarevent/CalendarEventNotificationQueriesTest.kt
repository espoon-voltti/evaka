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
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.dev.DevBackupCare
import fi.espoo.evaka.shared.dev.DevCalendarEvent
import fi.espoo.evaka.shared.dev.DevCalendarEventAttendee
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.DevDaycareGroupPlacement
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevFosterParent
import fi.espoo.evaka.shared.dev.DevGuardian
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test

class CalendarEventNotificationQueriesTest : PureJdbiTest(resetDbBeforeEach = true) {
    private val today: LocalDate = LocalDate.of(2023, 5, 1)
    private val now = HelsinkiDateTime.of(today, LocalTime.of(18, 0, 0))

    private val area = DevCareArea()
    private val daycare = DevDaycare(areaId = area.id)
    private val daycare2 = DevDaycare(areaId = area.id, name = "Test Daycare 2")
    private val group1 = DevDaycareGroup(daycareId = daycare.id, name = "Test group 1")
    private val group2 = DevDaycareGroup(daycareId = daycare.id, name = "Test group 2")
    private val employee = DevEmployee()

    private val child1 = DevPerson()
    private val child2 = DevPerson()
    private val child3 = DevPerson()
    private val adult1 = DevPerson()
    private val adult2 = DevPerson()
    private val adult3 = DevPerson(language = "sv")

    private fun insertTestData(
        placementStart: LocalDate = today.minusYears(1),
        placementEnd: LocalDate = today.plusYears(1),
    ) {
        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(daycare2)
            tx.insert(group1)
            tx.insert(group2)
            tx.insert(employee)

            tx.insert(child1, DevPersonType.CHILD)
            tx.insert(
                DevPlacement(
                    childId = child1.id,
                    unitId = daycare.id,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )

            tx.insert(adult1, DevPersonType.RAW_ROW)
            tx.insert(DevGuardian(guardianId = adult1.id, childId = child1.id))

            tx.insert(child2, DevPersonType.CHILD)
            tx.insert(
                    DevPlacement(
                        childId = child2.id,
                        unitId = daycare.id,
                        startDate = placementStart,
                        endDate = placementEnd,
                    )
                )
                .also { placementId ->
                    tx.insert(
                        DevDaycareGroupPlacement(
                            daycarePlacementId = placementId,
                            daycareGroupId = group1.id,
                            startDate = today.minusYears(1),
                            endDate = today.plusYears(1),
                        )
                    )
                }

            tx.insert(adult2, DevPersonType.RAW_ROW)
            tx.insert(DevGuardian(guardianId = adult2.id, childId = child2.id))

            tx.insert(child3, DevPersonType.CHILD)
            tx.insert(
                    DevPlacement(
                        childId = child3.id,
                        unitId = daycare.id,
                        startDate = placementStart,
                        endDate = placementEnd,
                    )
                )
                .also { placementId ->
                    tx.insert(
                        DevDaycareGroupPlacement(
                            daycarePlacementId = placementId,
                            daycareGroupId = group2.id,
                            startDate = today.minusYears(1),
                            endDate = today.plusYears(1),
                        )
                    )
                }

            tx.insert(adult3, DevPersonType.RAW_ROW)
            tx.insert(
                DevFosterParent(
                    parentId = adult3.id,
                    childId = child3.id,
                    validDuring = DateRange(today, today.plusYears(1)),
                    modifiedAt = now,
                    modifiedBy = employee.evakaUserId,
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
                unitId = daycare.id,
            )

        assertEquals(
            listOf(
                    ParentWithEvents(
                        parentId = adult1.id,
                        language = Language.fi,
                        events = listOf(eventId),
                    ),
                    ParentWithEvents(
                        parentId = adult2.id,
                        language = Language.fi,
                        events = listOf(eventId),
                    ),
                    ParentWithEvents(
                        parentId = adult3.id,
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
                unitId = daycare.id,
                groupIds = listOf(group1.id),
            )

        assertEquals(
            listOf(
                ParentWithEvents(
                    parentId = adult2.id,
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
                    childId = child1.id,
                    unitId = daycare2.id,
                    groupId = null,
                    period = FiniteDateRange(today, today.plusDays(1)),
                )
            )

            // Backup care in the same unit -> notification is sent
            tx.insert(
                DevBackupCare(
                    childId = child2.id,
                    unitId = daycare.id,
                    groupId = group2.id,
                    period = FiniteDateRange(today, today.plusDays(1)),
                )
            )

            // Backup care covers only part of the event -> notification is sent
            tx.insert(
                DevBackupCare(
                    childId = child3.id,
                    unitId = daycare2.id,
                    groupId = null,
                    period = FiniteDateRange(today, today),
                )
            )
        }
        val eventId =
            createCalendarEvent(
                title = "Backup care",
                period = FiniteDateRange(today, today.plusDays(1)),
                unitId = daycare.id,
            )

        assertEquals(
            listOf(
                    ParentWithEvents(
                        parentId = adult2.id,
                        language = Language.fi,
                        events = listOf(eventId),
                    ),
                    ParentWithEvents(
                        parentId = adult3.id,
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
        // child3 is in backup care, but the parent still gets notified about child-specific
        // events
        db.transaction { tx ->
            tx.insert(
                DevBackupCare(
                    childId = child3.id,
                    unitId = daycare2.id,
                    groupId = null,
                    period = FiniteDateRange(today, today),
                )
            )
        }
        val eventId =
            createCalendarEvent(
                title = "Child event",
                period = FiniteDateRange(today, today),
                unitId = daycare.id,
                groupChildIds = listOf(group1.id to child2.id, group2.id to child3.id),
            )

        assertEquals(
            listOf(
                    ParentWithEvents(
                        parentId = adult2.id,
                        language = Language.fi,
                        events = listOf(eventId),
                    ),
                    ParentWithEvents(
                        parentId = adult3.id,
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
            unitId = daycare.id,
            created = now.minusHours(24),
        )
        createCalendarEvent(
            title = "Group-wide event",
            period = FiniteDateRange(today.minusDays(1), today),
            unitId = daycare.id,
            groupIds = listOf(group1.id),
            created = now.minusHours(24),
        )
        createCalendarEvent(
            title = "Child event",
            period = FiniteDateRange(today.minusDays(1), today),
            unitId = daycare.id,
            groupIds = listOf(group2.id),
            groupChildIds = listOf(group2.id to child3.id),
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
            unitId = daycare.id,
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
        createdBy: EvakaUserId = employee.evakaUserId,
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
                        modifiedBy = createdBy,
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
