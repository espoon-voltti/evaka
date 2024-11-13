// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.calendarevent

import fi.espoo.evaka.Audit
import fi.espoo.evaka.AuditId
import fi.espoo.evaka.backupcare.getBackupCareChildrenInGroup
import fi.espoo.evaka.daycare.domain.Language
import fi.espoo.evaka.daycare.getDaycare
import fi.espoo.evaka.daycare.getDaycareGroups
import fi.espoo.evaka.pis.getPersonById
import fi.espoo.evaka.pis.service.PersonDTO
import fi.espoo.evaka.pis.service.getChildGuardiansAndFosterParents
import fi.espoo.evaka.placement.getDaycarePlacements
import fi.espoo.evaka.placement.getGroupPlacementChildren
import fi.espoo.evaka.shared.CalendarEventId
import fi.espoo.evaka.shared.CalendarEventTimeId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.Conflict
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.domain.getHolidays
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import java.time.DayOfWeek
import java.time.LocalDate
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class CalendarEventController(
    private val accessControl: AccessControl,
    private val asyncJobRunner: AsyncJobRunner<AsyncJob>,
) {
    @GetMapping("/employee/units/{unitId}/calendar-events")
    fun getUnitCalendarEvents(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable unitId: DaycareId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) start: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) end: LocalDate,
    ): List<CalendarEvent> {
        if (start.isAfter(end)) {
            throw BadRequest("Start must be before or equal to the end")
        }

        val range = FiniteDateRange(start, end)

        if (range.durationInDays() > 6 * 7) {
            throw BadRequest("Only 6 weeks of calendar events may be fetched at once")
        }

        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Unit.READ_CALENDAR_EVENTS,
                        unitId,
                    )
                    tx.getCalendarEventsByUnit(unitId, range)
                }
            }
            .also {
                Audit.UnitCalendarEventsRead.log(
                    targetId = AuditId(unitId),
                    meta = mapOf("start" to start, "end" to end, "count" to it.size),
                )
            }
    }

    @GetMapping("/employee/units/{unitId}/groups/{groupId}/discussion-surveys")
    fun getGroupDiscussionSurveys(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable unitId: DaycareId,
        @PathVariable groupId: GroupId,
    ): List<CalendarEvent> {
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Unit.READ_CALENDAR_EVENTS,
                        unitId,
                    )
                    tx.getCalendarEventsByGroupAndType(
                        groupId,
                        listOf(CalendarEventType.DISCUSSION_SURVEY),
                    )
                }
            }
            .also {
                Audit.GroupCalendarEventsRead.log(
                    targetId = AuditId(groupId),
                    meta = mapOf("count" to it.size),
                )
            }
    }

    @GetMapping("/employee/units/{unitId}/groups/{groupId}/discussion-reservation-days")
    fun getGroupDiscussionReservationDays(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable unitId: DaycareId,
        @PathVariable groupId: GroupId,
        @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) start: LocalDate,
        @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) end: LocalDate,
    ): Set<DiscussionReservationDay> {
        if (start.isAfter(end)) {
            throw BadRequest("Start must be before or equal to the end")
        }

        val range = FiniteDateRange(start, end)

        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Unit.READ_CALENDAR_EVENTS,
                        unitId,
                    )

                    val holidays = getHolidays(range)
                    val unitOperationDays =
                        tx.getDaycare(unitId)?.operationDays
                            ?: throw NotFound("Unit operation days not found")
                    val groupEvents =
                        tx.getCalendarEventsByUnitWithRange(unitId, range).filter {
                            it.groups.isEmpty() ||
                                it.groups.any { groupInfo -> groupInfo.id == groupId }
                        }

                    range
                        .dates()
                        .map { day ->
                            DiscussionReservationDay(
                                date = day,
                                events =
                                    groupEvents
                                        .filter { event -> event.period.includes(day) }
                                        .filter { event ->
                                            event.times.isEmpty() ||
                                                event.times.any { time -> time.date.isEqual(day) }
                                        }
                                        .toSet(),
                                isHoliday = holidays.contains(day),
                                isOperationalDay = unitOperationDays.contains(day.dayOfWeek.value),
                            )
                        }
                        .toSet()
                }
            }
            .also {
                Audit.GroupDiscussionReservationCalendarDaysRead.log(
                    targetId = AuditId(groupId),
                    meta = mapOf("start" to start, "end" to end, "count" to it.size),
                )
            }
    }

    @PostMapping("/employee/calendar-event")
    fun createCalendarEvent(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestBody body: CalendarEventForm,
    ): CalendarEventId {
        val eventId =
            db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Unit.CREATE_CALENDAR_EVENT,
                        body.unitId,
                    )

                    if (body.tree != null) {
                        accessControl.requirePermissionFor(
                            tx,
                            user,
                            clock,
                            Action.Group.CREATE_CALENDAR_EVENT,
                            body.tree.keys,
                        )

                        val unitGroupIds =
                            tx.getDaycareGroups(body.unitId, body.period.start, body.period.end)

                        if (
                            body.tree.keys.any { groupId ->
                                !unitGroupIds.any { unitGroup -> unitGroup.id == groupId }
                            }
                        ) {
                            throw BadRequest("Group ID is not of the specified unit's")
                        }

                        body.tree.forEach { (groupId, childIds) ->
                            if (childIds != null) {
                                val groupChildIds =
                                    tx.getGroupPlacementChildren(groupId, body.period)
                                val backupCareChildren =
                                    tx.getBackupCareChildrenInGroup(
                                        body.unitId,
                                        groupId,
                                        body.period,
                                    )

                                if (
                                    childIds.any {
                                        !groupChildIds.contains(it) &&
                                            !backupCareChildren.contains(it)
                                    }
                                ) {
                                    throw BadRequest("Child is not placed into the selected group")
                                }
                            }
                        }
                    }

                    tx.createCalendarEvent(body, clock.now(), user.evakaUserId)
                }
            }
        Audit.CalendarEventCreate.log(targetId = AuditId(body.unitId), objectId = AuditId(eventId))
        return eventId
    }

    @GetMapping("/employee/calendar-event/{id}")
    fun getCalendarEvent(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable id: CalendarEventId,
    ): CalendarEvent {
        return db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.CalendarEvent.READ,
                        id,
                    )
                    tx.getCalendarEventById(id) ?: throw NotFound()
                }
            }
            .also { Audit.CalendarEventRead.log(targetId = AuditId(id)) }
    }

    @DeleteMapping("/employee/calendar-event/{id}")
    fun deleteCalendarEvent(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable id: CalendarEventId,
    ) {
        return db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.CalendarEvent.DELETE,
                        id,
                    )
                    tx.deleteCalendarEvent(id)
                }
            }
            .also { Audit.CalendarEventDelete.log(targetId = AuditId(id)) }
    }

    @PatchMapping("/employee/calendar-event/{id}")
    fun modifyCalendarEvent(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable id: CalendarEventId,
        @RequestBody body: CalendarEventUpdateForm,
    ) {
        return db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.CalendarEvent.UPDATE,
                        id,
                    )
                    tx.updateCalendarEvent(id, clock.now(), user.evakaUserId, body)
                }
            }
            .also { Audit.CalendarEventUpdate.log(targetId = AuditId(id)) }
    }

    @PutMapping("/employee/calendar-event/{id}")
    fun updateCalendarEvent(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable id: CalendarEventId,
        @RequestBody body: CalendarEventUpdateForm,
    ) {
        return db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.CalendarEvent.UPDATE,
                        id,
                    )
                    val event = tx.getCalendarEventById(id) ?: throw NotFound()
                    val current = tx.getCalendarEventChildIds(event.id)
                    val updated = resolveAttendeeChildIds(tx, event.unitId, body.tree, event.period)
                    val removed = current.minus(updated.toSet())
                    removed.forEach { childId ->
                        tx.freeCalendarEventTimeReservationsByChildAndEvent(
                            user,
                            clock.now(),
                            calendarEventId = event.id,
                            childId = childId,
                        )
                    }
                    tx.updateCalendarEvent(id, clock.now(), user.evakaUserId, body)
                    tx.deleteCalendarEventAttendees(id)
                    tx.createCalendarEventAttendees(id, event.unitId, body.tree)
                }
            }
            .also { Audit.CalendarEventUpdate.log(targetId = AuditId(id)) }
    }

    @PostMapping("/employee/calendar-event/{id}/time")
    fun addCalendarEventTime(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable id: CalendarEventId,
        @RequestBody body: CalendarEventTimeForm,
    ): CalendarEventTimeId {
        return db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.CalendarEvent.UPDATE,
                        id,
                    )
                    val associatedEvent = tx.getCalendarEventById(id)
                    if (
                        associatedEvent == null ||
                            associatedEvent.eventType != CalendarEventType.DISCUSSION_SURVEY
                    ) {
                        throw NotFound("No corresponding discussion survey found")
                    }
                    val cetId = tx.createCalendarEventTime(id, body, clock.now(), user.evakaUserId)
                    val updatedEvent =
                        tx.getCalendarEventById(id)
                            ?: throw NotFound("No corresponding discussion survey found")

                    tx.updateCalendarEventPeriod(
                        eventId = id,
                        modifiedAt = clock.now(),
                        modifiedBy = user.evakaUserId,
                        period = getPeriodOfTimes(updatedEvent.times, clock.today()),
                    )
                    cetId
                }
            }
            .also { Audit.CalendarEventTimeCreate.log(targetId = AuditId(id)) }
    }

    @DeleteMapping("/employee/calendar-event-time/{id}")
    fun deleteCalendarEventTime(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable id: CalendarEventTimeId,
    ) {
        return db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.CalendarEventTime.DELETE,
                        id,
                    )

                    val preUpdateEventTimeDetails =
                        tx.getDiscussionTimeDetailsByEventTimeId(id)
                            ?: throw BadRequest("Calendar event time not found")

                    val calendarEventId =
                        tx.getCalendarEventIdByTimeId(id)
                            ?: throw NotFound("No corresponding discussion survey found")

                    tx.deleteCalendarEventTime(id)

                    val associatedEvent =
                        tx.getCalendarEventById(calendarEventId)
                            ?: throw NotFound("No corresponding calendar event found")

                    tx.updateCalendarEventPeriod(
                        eventId = calendarEventId,
                        modifiedAt = clock.now(),
                        modifiedBy = user.evakaUserId,
                        period = getPeriodOfTimes(associatedEvent.times, clock.today()),
                    )

                    if (preUpdateEventTimeDetails.eventTime.childId != null) {
                        val cancellationRecipients =
                            getRecipientsForChild(tx, preUpdateEventTimeDetails.eventTime.childId)
                        asyncJobRunner.plan(
                            tx,
                            cancellationRecipients.map {
                                AsyncJob.SendDiscussionSurveyReservationCancellationEmail(
                                    eventTitle = associatedEvent.title,
                                    childId = preUpdateEventTimeDetails.eventTime.childId,
                                    language = Language.fi,
                                    calendarEventTime = preUpdateEventTimeDetails.eventTime,
                                    recipientId = it.id,
                                )
                            },
                            runAt = clock.now(),
                        )
                    }
                }
            }
            .also { Audit.CalendarEventTimeDelete.log(targetId = AuditId(id)) }
    }

    @PostMapping("/employee/calendar-event/reservation")
    fun setCalendarEventTimeReservation(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestBody body: CalendarEventTimeEmployeeReservationForm,
    ) {
        return db.connect { dbc ->
            dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.CalendarEventTime.UPDATE_RESERVATION,
                        body.calendarEventTimeId,
                    )
                    validate(
                        tx = tx,
                        eventTimeId = body.calendarEventTimeId,
                        childId = body.childId,
                    )
                    val preUpdateEventTimeDetails =
                        tx.getDiscussionTimeDetailsByEventTimeId(body.calendarEventTimeId)
                            ?: throw BadRequest("Calendar event time not found")

                    tx.freeCalendarEventTimeReservation(user, clock.now(), body.calendarEventTimeId)

                    if (body.childId != null) {
                        val reservationRecipients = getRecipientsForChild(tx, body.childId)
                        tx.insertCalendarEventTimeReservation(
                            eventTimeId = body.calendarEventTimeId,
                            childId = body.childId,
                            modifiedAt = clock.now(),
                            modifiedBy = user.evakaUserId,
                        )

                        if (body.childId != preUpdateEventTimeDetails.eventTime.childId) {
                            asyncJobRunner.plan(
                                tx,
                                reservationRecipients.map {
                                    AsyncJob.SendDiscussionSurveyReservationEmail(
                                        eventTitle = preUpdateEventTimeDetails.title,
                                        childId = body.childId,
                                        language = Language.fi,
                                        calendarEventTime = preUpdateEventTimeDetails.eventTime,
                                        recipientId = it.id,
                                    )
                                },
                                runAt = clock.now(),
                            )
                        }
                    }
                    if (
                        preUpdateEventTimeDetails.eventTime.childId != null &&
                            preUpdateEventTimeDetails.eventTime.childId != body.childId
                    ) {
                        val cancellationRecipients =
                            getRecipientsForChild(tx, preUpdateEventTimeDetails.eventTime.childId)
                        asyncJobRunner.plan(
                            tx,
                            cancellationRecipients.map {
                                AsyncJob.SendDiscussionSurveyReservationCancellationEmail(
                                    eventTitle = preUpdateEventTimeDetails.title,
                                    childId = preUpdateEventTimeDetails.eventTime.childId,
                                    language = Language.fi,
                                    calendarEventTime = preUpdateEventTimeDetails.eventTime,
                                    recipientId = it.id,
                                )
                            },
                            runAt = clock.now(),
                        )
                    }
                }
                .also {
                    Audit.CalendarEventTimeReservationUpdate.log(
                        targetId = AuditId(body.calendarEventTimeId),
                        objectId = body.childId?.let(AuditId::invoke),
                    )
                }
        }
    }

    @PostMapping("/employee/calendar-event/clear-survey-reservations-for-child")
    fun clearEventTimesInEventForChild(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestBody body: CalendarEventTimeClearingForm,
    ) {
        val removedIds =
            db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.CalendarEvent.UPDATE,
                        body.calendarEventId,
                    )
                    val eventTimesToRemove =
                        tx.getCalendarEventTimesByChildAndEvent(body.childId, body.calendarEventId)
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.CalendarEventTime.UPDATE_RESERVATION,
                        eventTimesToRemove.map { it.id },
                    )
                    val discussionSurvey =
                        tx.getCalendarEventById(body.calendarEventId)
                            ?: throw BadRequest("Calendar event not found")
                    val cancellationRecipients = getRecipientsForChild(tx, body.childId)

                    tx.freeCalendarEventTimeReservations(
                        user,
                        clock.now(),
                        eventTimesToRemove.map { it.id }.toSet(),
                    )

                    // only discussion times in the future should result in cancellation messages
                    asyncJobRunner.plan(
                        tx,
                        eventTimesToRemove
                            .filter {
                                HelsinkiDateTime.of(it.date, it.endTime).isAfter(clock.now())
                            }
                            .flatMap { time ->
                                cancellationRecipients.map { recipient ->
                                    AsyncJob.SendDiscussionSurveyReservationCancellationEmail(
                                        eventTitle = discussionSurvey.title,
                                        childId = body.childId,
                                        language = Language.fi,
                                        calendarEventTime = time,
                                        recipientId = recipient.id,
                                    )
                                }
                            },
                        runAt = clock.now(),
                    )
                    eventTimesToRemove.map { it.id }
                }
            }
        Audit.CalendarEventChildTimesCancellation.log(
            targetId = AuditId(body.calendarEventId),
            objectId = AuditId(body.childId),
            meta = mapOf("eventTimes" to removedIds),
        )
    }

    @GetMapping("/citizen/calendar-events")
    fun getCitizenCalendarEvents(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) start: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) end: LocalDate,
    ): List<CitizenCalendarEvent> {
        if (start.isAfter(end)) {
            throw BadRequest("Start must be before or equal to the end")
        }
        val range = FiniteDateRange(start, end)
        if (range.durationInDays() > 450) {
            throw BadRequest("Only 450 days of calendar events may be fetched at once")
        }

        val today = clock.today()
        return db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Citizen.Person.READ_CALENDAR_EVENTS,
                        user.id,
                    )
                    val daycareEvents = tx.getDaycareEventsForGuardian(user.id, range)
                    val discussionEvents = tx.getDiscussionSurveysForGuardian(user.id, range)

                    val daycareEventResults =
                        daycareEvents
                            .groupBy { it.id }
                            .map { (eventId, attendees) ->
                                CitizenCalendarEvent(
                                    id = eventId,
                                    title = attendees[0].title,
                                    description = attendees[0].description,
                                    period = attendees[0].eventPeriod,
                                    eventType = CalendarEventType.DAYCARE_EVENT,
                                    attendingChildren =
                                        attendees
                                            .groupBy { it.childId }
                                            .mapValues { (_, attendee) ->
                                                attendee
                                                    .groupBy {
                                                        Triple(it.type, it.groupId, it.unitId)
                                                    }
                                                    .map { (t, attendance) ->
                                                        AttendingChild(
                                                            periods = attendance.map { it.period },
                                                            type = t.first,
                                                            groupName = attendance[0].groupName,
                                                            unitName = attendance[0].unitName,
                                                        )
                                                    }
                                            },
                                    timesByChild = emptyMap(),
                                )
                            }
                    val discussionEventResults =
                        discussionEvents
                            .groupBy { it.id }
                            .map { (eventId, attendeeRows) ->
                                CitizenCalendarEvent(
                                    id = eventId,
                                    title = attendeeRows[0].title,
                                    description = attendeeRows[0].description,
                                    eventType = CalendarEventType.DISCUSSION_SURVEY,
                                    period = attendeeRows[0].eventPeriod,
                                    attendingChildren =
                                        attendeeRows
                                            .groupBy { it.childId }
                                            .mapValues { (_, attendee) ->
                                                attendee
                                                    .groupBy {
                                                        Triple(it.type, it.groupId, it.unitId)
                                                    }
                                                    .map { (t, attendance) ->
                                                        AttendingChild(
                                                            periods =
                                                                attendance
                                                                    .map { it.period }
                                                                    .distinct(),
                                                            type = t.first,
                                                            groupName = attendance[0].groupName,
                                                            unitName = attendance[0].unitName,
                                                        )
                                                    }
                                            },
                                    timesByChild =
                                        attendeeRows
                                            .groupBy { it.childId }
                                            .map { (key, values) ->
                                                key to
                                                    values
                                                        .map {
                                                            CitizenCalendarEventTime(
                                                                id = it.eventTimeId,
                                                                childId = it.eventTimeOccupant,
                                                                startTime = it.eventTimeStart,
                                                                endTime = it.eventTimeEnd,
                                                                date = it.eventTimeDate,
                                                                isEditable =
                                                                    !it.eventTimeDate.isBefore(
                                                                        getManipulationWindowStart(
                                                                            today
                                                                        )
                                                                    ),
                                                            )
                                                        }
                                                        .toSet()
                                            }
                                            .toMap(),
                                )
                            }
                    daycareEventResults + discussionEventResults
                }
            }
            .also {
                Audit.UnitCalendarEventsRead.log(
                    targetId = AuditId(user.id),
                    meta = mapOf("start" to start, "end" to end, "count" to it.size),
                )
            }
    }

    @GetMapping("/citizen/calendar-event/{eventId}/time")
    fun getReservableCalendarEventTimes(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @PathVariable eventId: CalendarEventId,
        @RequestParam childId: ChildId,
    ): List<CalendarEventTime> {
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Citizen.CalendarEvent.READ,
                        eventId,
                    )
                    tx.getReservableCalendarEventTimes(eventId, childId)
                }
            }
            .also {
                Audit.CalendarEventTimeRead.log(
                    targetId = AuditId(eventId),
                    objectId = AuditId(childId),
                )
            }
    }

    @PostMapping("/citizen/calendar-event/reservation")
    fun addCalendarEventTimeReservation(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @RequestBody body: CalendarEventTimeCitizenReservationForm,
    ) {
        return db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Citizen.Child.CREATE_CALENDAR_EVENT_TIME_RESERVATION,
                        body.childId,
                    )
                    validate(
                        tx = tx,
                        eventTimeId = body.calendarEventTimeId,
                        childId = body.childId,
                    )
                    val eventTimeDetails =
                        tx.getDiscussionTimeDetailsByEventTimeId(body.calendarEventTimeId)
                            ?: throw BadRequest("Calendar event time not found")

                    val count =
                        tx.insertCalendarEventTimeReservation(
                            eventTimeId = body.calendarEventTimeId,
                            childId = body.childId,
                            modifiedAt = clock.now(),
                            modifiedBy = user.evakaUserId,
                        )
                    if (count != 1) {
                        throw Conflict(
                            "Calendar event time already reserved",
                            errorCode = "TIME_ALREADY_RESERVED",
                        )
                    }

                    // send reservation email if reservation changes
                    if (eventTimeDetails.eventTime.childId != body.childId) {
                        val recipients = getRecipientsForChild(tx, body.childId)
                        val finalEventTime = eventTimeDetails.eventTime.copy(childId = body.childId)
                        asyncJobRunner.plan(
                            tx,
                            recipients.map {
                                AsyncJob.SendDiscussionSurveyReservationEmail(
                                    eventTitle = eventTimeDetails.title,
                                    childId = body.childId,
                                    language = Language.fi,
                                    calendarEventTime = finalEventTime,
                                    recipientId = it.id,
                                )
                            },
                            runAt = clock.now(),
                        )
                    }
                }
            }
            .also {
                Audit.CalendarEventTimeReservationCreate.log(
                    targetId = AuditId(body.calendarEventTimeId),
                    objectId = AuditId(body.childId),
                )
            }
    }

    @DeleteMapping("/citizen/calendar-event/reservation")
    fun deleteCalendarEventTimeReservation(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @RequestParam calendarEventTimeId: CalendarEventTimeId,
        @RequestParam childId: ChildId,
    ) {
        val body = CalendarEventTimeCitizenReservationForm(calendarEventTimeId, childId)
        db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Citizen.Child.DELETE_CALENDAR_EVENT_TIME_RESERVATION,
                        body.childId,
                    )
                    val eventTimeDetails =
                        tx.getDiscussionTimeDetailsByEventTimeId(body.calendarEventTimeId)
                            ?: throw BadRequest("Calendar event time not found")
                    tx.freeCalendarEventTimeReservation(user, clock.now(), body.calendarEventTimeId)
                    val recipients = getRecipientsForChild(tx, body.childId)
                    asyncJobRunner.plan(
                        tx,
                        recipients.map {
                            AsyncJob.SendDiscussionSurveyReservationCancellationEmail(
                                eventTitle = eventTimeDetails.title,
                                childId = body.childId,
                                language = Language.fi,
                                calendarEventTime = eventTimeDetails.eventTime,
                                recipientId = it.id,
                            )
                        },
                        runAt = clock.now(),
                    )
                }
            }
            .also {
                Audit.CalendarEventTimeReservationDelete.log(
                    targetId = AuditId(body.calendarEventTimeId),
                    objectId = AuditId(body.childId),
                )
            }
    }
}

private fun validate(tx: Database.Read, eventTimeId: CalendarEventTimeId, childId: ChildId?) {
    val calendarEventId =
        tx.getCalendarEventIdByTimeId(eventTimeId)
            ?: throw BadRequest("Calendar event time not found")

    if (childId != null && !tx.getCalendarEventChildIds(calendarEventId).contains(childId)) {
        throw BadRequest("Child $childId is not attendee in calendar event")
    }
}

private fun resolveAttendeeChildIds(
    tx: Database.Read,
    unitId: DaycareId,
    tree: Map<GroupId, Set<ChildId>?>?,
    range: FiniteDateRange,
): List<ChildId> {
    return tree?.flatMap {
        if (it.value != null) it.value!!.toList() else tx.getGroupPlacementChildren(it.key, range)
    }
        ?: tx.getDaycarePlacements(
                daycareId = unitId,
                childId = null,
                startDate = range.start,
                endDate = range.end,
            )
            .map { placement -> placement.child.id }
}

private fun getPeriodOfTimes(
    dateList: Set<CalendarEventTime>,
    default: LocalDate,
): FiniteDateRange {
    val minDate = dateList.minOfOrNull { it.date }
    val maxDate = dateList.maxOfOrNull { it.date }
    return if (minDate != null && maxDate != null) FiniteDateRange(minDate, maxDate)
    else FiniteDateRange(default, default)
}

private fun getRecipientsForChild(tx: Database.Transaction, childId: ChildId): List<PersonDTO> {
    return tx.getChildGuardiansAndFosterParents(childId, LocalDate.now()).mapNotNull {
        tx.getPersonById(it)
    }
}

// manipulation allowed if there is a full business day "buffer" from today
// i.e. for times on the first business day after the next business day
// today | FRI | SAT | SUN | MON
//   f   |  f  | N/A | N/A |  t
private fun getManipulationWindowStart(originalDate: LocalDate) =
    when (originalDate.dayOfWeek) {
        DayOfWeek.THURSDAY -> originalDate.plusDays(4) // MONDAY
        DayOfWeek.FRIDAY -> originalDate.plusDays(4) // TUESDAY
        DayOfWeek.SATURDAY -> originalDate.plusDays(3) // TUESDAY
        else -> originalDate.plusDays(2)
    }
