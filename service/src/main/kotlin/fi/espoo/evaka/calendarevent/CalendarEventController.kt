// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.calendarevent

import fi.espoo.evaka.Audit
import fi.espoo.evaka.backupcare.getBackupCareChildrenInGroup
import fi.espoo.evaka.daycare.getDaycareGroups
import fi.espoo.evaka.placement.getGroupPlacementChildren
import fi.espoo.evaka.shared.CalendarEventId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
class CalendarEventController(private val accessControl: AccessControl) {
    @GetMapping("/units/{unitId}/calendar-events")
    fun getUnitCalendarEvents(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable unitId: DaycareId,
        @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) start: LocalDate,
        @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) end: LocalDate
    ): List<CalendarEvent> {
        Audit.UnitCalendarEventsRead.log(targetId = unitId)
        accessControl.requirePermissionFor(user, Action.Unit.READ_CALENDAR_EVENTS, unitId)

        if (start.isAfter(end)) {
            throw BadRequest("Start must be before or equal to the end")
        }

        val range = FiniteDateRange(start, end)

        if (range.durationInDays() > 7) {
            throw BadRequest("Only 7 days of calendar events may be fetched at once")
        }

        return db.connect { dbc ->
            dbc.transaction { tx -> tx.getCalendarEventsByUnit(unitId, range) }
        }
    }

    @PostMapping("/calendar-event")
    fun createCalendarEvent(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestBody body: CalendarEventForm
    ) {
        Audit.CalendarEventCreate.log(targetId = body.unitId)

        return db.connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(user, Action.Unit.CREATE_CALENDAR_EVENT, body.unitId)

                if (body.tree != null) {
                    accessControl.requirePermissionFor(user, clock, Action.Group.CREATE_CALENDAR_EVENT, body.tree.keys)

                    val unitGroupIds = tx.getDaycareGroups(body.unitId, body.period.start, body.period.end)

                    if (body.tree.keys.any { groupId -> !unitGroupIds.any { unitGroup -> unitGroup.id == groupId } }) {
                        throw BadRequest("Group ID is not of the specified unit's")
                    }

                    body.tree.forEach { (groupId, childIds) ->
                        if (childIds != null) {
                            val groupChildIds = tx.getGroupPlacementChildren(groupId, body.period)
                            val backupCareChildren = tx.getBackupCareChildrenInGroup(body.unitId, groupId, body.period)

                            if (childIds.any { !groupChildIds.contains(it) && !backupCareChildren.contains(it) }) {
                                throw BadRequest("Child is not placed into the selected group")
                            }
                        }
                    }
                }

                tx.createCalendarEvent(body)
            }
        }
    }

    @DeleteMapping("/calendar-event/{id}")
    fun deleteCalendarEvent(
        db: Database,
        user: AuthenticatedUser.Employee,
        @PathVariable id: CalendarEventId
    ) {
        Audit.CalendarEventDelete.log(targetId = id)
        accessControl.requirePermissionFor(user, Action.CalendarEvent.DELETE, id)
        return db.connect { dbc ->
            dbc.transaction { tx -> tx.deleteCalendarEvent(id) }
        }
    }

    @PatchMapping("/calendar-event/{id}")
    fun modifyCalendarEvent(
        db: Database,
        user: AuthenticatedUser.Employee,
        @PathVariable id: CalendarEventId,
        @RequestBody body: CalendarEventUpdateForm
    ) {
        Audit.CalendarEventUpdate.log(targetId = id)
        accessControl.requirePermissionFor(user, Action.CalendarEvent.UPDATE, id)
        return db.connect { dbc ->
            dbc.transaction { tx -> tx.updateCalendarEvent(id, body) }
        }
    }

    @GetMapping("/citizen/calendar-events")
    fun getCitizenCalendarEvents(
        db: Database,
        user: AuthenticatedUser.Citizen,
        @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) start: LocalDate,
        @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) end: LocalDate
    ): List<CitizenCalendarEvent> {
        Audit.UnitCalendarEventsRead.log(targetId = user.id)
        accessControl.requirePermissionFor(user, Action.Citizen.Person.READ_CALENDAR_EVENTS, user.id)

        if (start.isAfter(end)) {
            throw BadRequest("Start must be before or equal to the end")
        }

        val range = FiniteDateRange(start, end)

        if (range.durationInDays() > 450) {
            throw BadRequest("Only 450 days of calendar events may be fetched at once")
        }

        return db.connect { dbc ->
            dbc.transaction { tx ->
                tx.getCalendarEventsForGuardian(user.id, range).groupBy { it.id }.map { (eventId, attendees) ->
                    CitizenCalendarEvent(
                        id = eventId,
                        title = attendees[0].title,
                        description = attendees[0].description,
                        attendingChildren = attendees.groupBy { it.childId }.mapValues { (_, attendee) ->
                            attendee.groupBy { Triple(it.type, it.groupId, it.unitId) }.map { (t, attendance) ->
                                AttendingChild(
                                    periods = attendance.map { it.period },
                                    type = t.first,
                                    groupName = attendance[0].groupName,
                                    unitName = attendance[0].unitName
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}
