// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.calendarevent

import evaka.core.FullApplicationTest
import evaka.core.pis.service.insertGuardian
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.auth.CitizenAuthLevel
import evaka.core.shared.auth.UserRole
import evaka.core.shared.dev.DevCalendarEvent
import evaka.core.shared.dev.DevCalendarEventAttendee
import evaka.core.shared.dev.DevCalendarEventTime
import evaka.core.shared.dev.DevCareArea
import evaka.core.shared.dev.DevDaycare
import evaka.core.shared.dev.DevDaycareGroup
import evaka.core.shared.dev.DevDaycareGroupPlacement
import evaka.core.shared.dev.DevEmployee
import evaka.core.shared.dev.DevPerson
import evaka.core.shared.dev.DevPersonType
import evaka.core.shared.dev.DevPlacement
import evaka.core.shared.dev.insert
import evaka.core.shared.domain.FiniteDateRange
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.shared.domain.MockEvakaClock
import evaka.core.shared.domain.NotFound
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertContains
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired

class CalendarEventIcsExportIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired lateinit var calendarEventController: CalendarEventController

    private val area = DevCareArea()
    private val daycare = DevDaycare(areaId = area.id, name = "Test Daycare")
    private val group1 = DevDaycareGroup(daycareId = daycare.id, name = "TestGroup1")
    private val group2 = DevDaycareGroup(daycareId = daycare.id, name = "TestGroup2")
    private val adminEmployee = DevEmployee(roles = setOf(UserRole.ADMIN))
    private val adult = DevPerson(firstName = "John", lastName = "Doe")
    private val child = DevPerson(firstName = "Ricky", lastName = "Doe")
    private val guardian = AuthenticatedUser.Citizen(adult.id, CitizenAuthLevel.STRONG)

    private val today = LocalDate.of(2026, 6, 24)
    private val clock = MockEvakaClock(HelsinkiDateTime.of(today, LocalTime.of(12, 0)))

    private val placement =
        DevPlacement(
            childId = child.id,
            unitId = daycare.id,
            startDate = today.minusDays(100),
            endDate = today.plusDays(100),
        )

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(group1)
            tx.insert(group2)
            tx.insert(adminEmployee)
            tx.insert(adult, DevPersonType.ADULT)
            tx.insert(child, DevPersonType.CHILD)
            tx.insertGuardian(adult.id, child.id)
            tx.insert(placement)
            tx.insert(
                DevDaycareGroupPlacement(
                    daycarePlacementId = placement.id,
                    daycareGroupId = group1.id,
                    startDate = today.minusDays(100),
                    endDate = today.plusDays(100),
                )
            )
        }
    }

    @Test
    fun `citizen can export an all-day daycare event as ics`() {
        val period = FiniteDateRange(today.plusDays(3), today.plusDays(4))
        val eventId = insertDaycareEvent(period = period, title = "Kevätjuhla", groupId = group1.id)

        val response =
            calendarEventController.exportCitizenCalendarEventIcs(
                dbInstance(),
                guardian,
                clock,
                eventId,
                child.id,
                period.start,
            )

        assertTrue(response.statusCode.is2xxSuccessful)
        assertTrue(
            response.headers.contentType.toString().startsWith("text/calendar"),
            "unexpected content type: ${response.headers.contentType}",
        )
        val disposition = response.headers.getFirst("Content-Disposition") ?: ""
        assertContains(disposition, "attachment")
        assertContains(disposition, ".ics")

        val body = String(response.body!!, Charsets.UTF_8)
        assertContains(body, "BEGIN:VCALENDAR")
        assertContains(body, "SUMMARY:Kevätjuhla")
        assertContains(body, "DTSTART;VALUE=DATE:20260627")
        assertContains(body, "DTEND;VALUE=DATE:20260629")
        assertContains(body, "Test Daycare")
        assertContains(body, "TestGroup1")
    }

    @Test
    fun `citizen can export a reserved discussion time as ics`() {
        val date = today.plusDays(3)
        val eventId =
            insertDaycareEvent(
                period = FiniteDateRange(date, date),
                title = "Keskusteluaika",
                groupId = group1.id,
                eventType = CalendarEventType.DISCUSSION_SURVEY,
            )
        val eventTimeId = db.transaction { tx ->
            tx.insert(
                DevCalendarEventTime(
                    calendarEventId = eventId,
                    date = date,
                    start = LocalTime.of(8, 20),
                    end = LocalTime.of(8, 40),
                    childId = child.id,
                    modifiedBy = adminEmployee.evakaUserId,
                    modifiedAt = clock.now(),
                )
            )
        }

        val response =
            calendarEventController.exportCitizenDiscussionReservationIcs(
                dbInstance(),
                guardian,
                clock,
                eventTimeId,
            )

        assertTrue(response.statusCode.is2xxSuccessful)
        assertTrue(response.headers.contentType.toString().startsWith("text/calendar"))

        val body = String(response.body!!, Charsets.UTF_8)
        assertContains(body, "BEGIN:VTIMEZONE")
        assertContains(body, "TZID:Europe/Helsinki")
        assertContains(body, "SUMMARY:Keskusteluaika")
        assertContains(body, "DTSTART;TZID=Europe/Helsinki:20260627T082000")
        assertContains(body, "DTEND;TZID=Europe/Helsinki:20260627T084000")
    }

    @Test
    fun `citizen cannot export an event their child does not attend`() {
        val period = FiniteDateRange(today.plusDays(3), today.plusDays(4))
        // event targets group2, but the child is in group1
        val eventId =
            insertDaycareEvent(period = period, title = "Other group", groupId = group2.id)

        assertThrows<NotFound> {
            calendarEventController.exportCitizenCalendarEventIcs(
                dbInstance(),
                guardian,
                clock,
                eventId,
                child.id,
                period.start,
            )
        }
    }

    private fun insertDaycareEvent(
        period: FiniteDateRange,
        title: String,
        groupId: evaka.core.shared.GroupId,
        eventType: CalendarEventType = CalendarEventType.DAYCARE_EVENT,
    ) = db.transaction { tx ->
        val eventId =
            tx.insert(
                DevCalendarEvent(
                    title = title,
                    description = "description",
                    period = period,
                    modifiedAt = clock.now(),
                    modifiedBy = adminEmployee.evakaUserId,
                    eventType = eventType,
                )
            )
        tx.insert(
            DevCalendarEventAttendee(
                calendarEventId = eventId,
                unitId = daycare.id,
                groupId = groupId,
            )
        )
        eventId
    }
}
