// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.calendarevent

import fi.espoo.evaka.shared.CalendarEventId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.domain.FiniteDateRange
import org.jdbi.v3.json.Json

data class GroupInfo(
    val id: GroupId,
    val name: String
)

data class IndividualChild(
    val id: ChildId,
    val name: String,
    val groupId: GroupId
)

data class CalendarEvent(
    val id: CalendarEventId,
    val unitId: DaycareId,
    @Json
    val groups: Set<GroupInfo>,
    @Json
    val individualChildren: Set<IndividualChild>,
    val title: String,
    val description: String,
    val period: FiniteDateRange
)

data class CitizenIndividualChild(
    val id: ChildId,
    val groupId: GroupId
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
    @Json
    val attendingChildren: Map<ChildId, List<AttendingChild>>
)

data class CalendarEventForm(
    val unitId: DaycareId,
    val tree: Map<GroupId, Set<ChildId>?>?,
    val title: String,
    val description: String,
    val period: FiniteDateRange
)

data class CalendarEventUpdateForm(
    val title: String,
    val description: String
)
