// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.calendarevent

import fi.espoo.evaka.shared.CalendarEventId
import fi.espoo.evaka.shared.CalendarEventTimeId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.TimeRange
import java.time.LocalDate
import java.time.LocalTime
import org.jdbi.v3.json.Json

data class GroupInfo(val id: GroupId, val name: String)

data class IndividualChild(
    val id: ChildId,
    val firstName: String,
    val lastName: String,
    val groupId: GroupId
)

data class CalendarEvent(
    val id: CalendarEventId,
    val unitId: DaycareId,
    @Json val groups: Set<GroupInfo>,
    @Json val individualChildren: Set<IndividualChild>,
    val title: String,
    val description: String,
    val period: FiniteDateRange,
    @Json val times: Set<CalendarEventTime>,
    val contentModifiedAt: HelsinkiDateTime,
    val eventType: CalendarEventType
)

data class CalendarEventInfo(val id: CalendarEventId, val title: String, val description: String)

data class DiscussionReservationDay(
    val date: LocalDate,
    val events: Set<CalendarEvent>,
    val isHoliday: Boolean,
    val isOperationalDay: Boolean
)

data class CalendarEventTime(
    val id: CalendarEventTimeId,
    val date: LocalDate,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val childId: ChildId?
)

data class AttendingChild(
    val periods: List<FiniteDateRange>,
    val type: String,
    val groupName: String?,
    val unitName: String?
)

data class CitizenCalendarEvent(
    val id: CalendarEventId,
    val title: String,
    val description: String,
    @Json val attendingChildren: Map<ChildId, List<AttendingChild>>
)

data class CalendarEventForm(
    val unitId: DaycareId,
    val tree: Map<GroupId, Set<ChildId>?>?,
    val title: String,
    val description: String,
    val period: FiniteDateRange,
    val times: List<CalendarEventTimeForm>? = null,
    val eventType: CalendarEventType
)

data class CalendarEventTimeForm(val date: LocalDate, val timeRange: TimeRange)

data class CalendarEventTimeEmployeeReservationForm(
    val calendarEventTimeId: CalendarEventTimeId,
    val childId: ChildId?
)

data class CalendarEventTimeCitizenReservationForm(
    val calendarEventTimeId: CalendarEventTimeId,
    val childId: ChildId
)

data class CalendarEventUpdateForm(
    val title: String,
    val description: String,
    val tree: Map<GroupId, Set<ChildId>?>? = null
)
