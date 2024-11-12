// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.calendarevent

import fi.espoo.evaka.daycare.domain.Language
import fi.espoo.evaka.emailclient.DiscussionTimeReminderData
import fi.espoo.evaka.shared.CalendarEventId
import fi.espoo.evaka.shared.CalendarEventTimeId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.PredicateSql
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import java.time.LocalDate
import java.time.LocalTime
import org.jdbi.v3.core.mapper.Nested

fun Database.Read.getCalendarEventsByUnit(
    unitId: DaycareId,
    range: FiniteDateRange,
): List<CalendarEvent> =
    getCalendarEventsQuery(unit(unitId).and(range(range))).toList<CalendarEvent>()

private fun Database.Read.getCalendarEventsQuery(where: PredicateSql) = createQuery {
    sql(
        """
SELECT
    ce.id,
    cea.unit_id,
    ce.title,
    ce.description,
    ce.period,
    ce.content_modified_at,
    eu.id AS content_modified_by_id,
    eu.name AS content_modified_by_name,
    eu.type AS content_modified_by_type,
    ce.event_type,
    (
        coalesce(jsonb_agg(DISTINCT jsonb_build_object(
            'id', cea.group_id,
            'name', dg.name
        )) FILTER (WHERE cea.group_id IS NOT NULL), '[]'::jsonb)
    ) AS groups,
    (
        coalesce(jsonb_agg(DISTINCT jsonb_build_object(
            'id', cea.child_id,
            'firstName', p.first_name,
            'lastName', p.last_name,
            'groupId', cea.group_id
        )) FILTER (WHERE cea.child_id IS NOT NULL), '[]'::jsonb)
    ) AS individual_children,
    (
        coalesce(jsonb_agg(DISTINCT jsonb_build_object(
            'id', cet.id,
            'date', cet.date,
            'startTime', cet.start_time,
            'endTime', cet.end_time,
            'childId', cet.child_id
        )) FILTER (WHERE cet.id IS NOT NULL), '[]'::jsonb)
    ) AS times
FROM calendar_event_attendee cea
JOIN calendar_event ce ON cea.calendar_event_id = ce.id
LEFT JOIN daycare_group dg ON dg.id = cea.group_id
LEFT JOIN person p ON p.id = cea.child_id
LEFT JOIN calendar_event_time cet ON cet.calendar_event_id = ce.id
LEFT JOIN evaka_user eu ON eu.id = ce.content_modified_by
WHERE ${predicate(where)}
AND (cea.child_id IS NULL OR EXISTS(
    -- filter out attendees that haven't been placed in the specified unit/group,
    -- for example due to changes in placements after the event creation or a new backup care
    SELECT 1 FROM generate_series(lower(ce.period), upper(ce.period) - INTERVAL '1 day', '1 day') d
    JOIN realized_placement_one(d::date) rp ON true
    WHERE rp.child_id = cea.child_id
      AND (cea.group_id IS NULL OR rp.group_id = cea.group_id)
      AND rp.unit_id = cea.unit_id
))
GROUP BY ce.id, cea.unit_id, eu.id
"""
    )
}

private fun unit(unitId: DaycareId) = PredicateSql { where("cea.unit_id = ${bind(unitId)}") }

private fun range(range: FiniteDateRange) = PredicateSql { where("ce.period && ${bind(range)}") }

private fun event(eventId: CalendarEventId) = PredicateSql { where("ce.id = ${bind(eventId)}") }

private fun events(eventIds: Set<CalendarEventId>) = PredicateSql {
    where("ce.id = ANY (${bind(eventIds)})")
}

private fun group(groupId: GroupId) = PredicateSql { where("cea.group_id = ${bind(groupId)}") }

private fun eventType(eventTypes: List<CalendarEventType>) = PredicateSql {
    where("ce.event_type = ANY(${bind(eventTypes)})")
}

fun Database.Transaction.createCalendarEvent(
    event: CalendarEventForm,
    createdAt: HelsinkiDateTime,
    createdBy: EvakaUserId,
): CalendarEventId {
    val eventId =
        createUpdate {
                sql(
                    """
INSERT INTO calendar_event (created_at, created_by, title, description, period, modified_at, modified_by, content_modified_at, content_modified_by, event_type)
VALUES (${bind(createdAt)}, ${bind(createdBy)}, ${bind(event.title)}, ${bind(event.description)}, ${bind(event.period)}, ${bind(createdAt)}, ${bind(createdBy)}, ${bind(createdAt)}, ${bind(createdBy)}, ${bind(event.eventType)})
RETURNING id
"""
                )
            }
            .executeAndReturnGeneratedKeys()
            .exactlyOne<CalendarEventId>()

    createCalendarEventAttendees(eventId, event.unitId, event.tree)

    if (event.eventType == CalendarEventType.DISCUSSION_SURVEY && !event.times.isNullOrEmpty()) {
        event.times.forEach { time -> createCalendarEventTime(eventId, time, createdAt, createdBy) }
    }

    return eventId
}

fun Database.Transaction.createCalendarEventAttendees(
    eventId: CalendarEventId,
    unitId: DaycareId,
    tree: Map<GroupId, Set<ChildId>?>?,
) {
    if (tree != null) {
        val rows: Sequence<Pair<GroupId, ChildId?>> =
            tree.asSequence().flatMap { (groupId, childIds) ->
                childIds?.asSequence()?.map { childId -> Pair(groupId, childId) }
                    ?: sequenceOf(Pair(groupId, null))
            }
        executeBatch(rows) {
            sql(
                """
INSERT INTO calendar_event_attendee (calendar_event_id, unit_id, group_id, child_id)
VALUES (${bind(eventId)}, ${bind(unitId)}, ${bind { (groupId, _) -> groupId }}, ${bind { (_, childId) -> childId }})
"""
            )
        }
    } else {
        createCalendarEventAttendee(eventId, unitId, null, null)
    }
}

fun Database.Transaction.deleteCalendarEventAttendees(eventId: CalendarEventId) = execute {
    sql("DELETE FROM calendar_event_attendee WHERE calendar_event_id = ${bind(eventId)}")
}

fun Database.Read.getReservableCalendarEventTimes(
    calendarEventId: CalendarEventId,
    childId: ChildId,
) =
    createQuery {
            sql(
                """
SELECT id, date, start_time, end_time
FROM calendar_event_time
WHERE calendar_event_id = ${bind(calendarEventId)}
AND (child_id IS NULL OR child_id = ${bind(childId)})
"""
            )
        }
        .toList<CalendarEventTime>()

fun Database.Transaction.createCalendarEventTime(
    calendarEventId: CalendarEventId,
    time: CalendarEventTimeForm,
    createdAt: HelsinkiDateTime,
    createdBy: EvakaUserId,
) =
    createUpdate {
            sql(
                """
INSERT INTO calendar_event_time (created_at, created_by, updated_at, modified_at, modified_by, calendar_event_id, date, start_time, end_time)
VALUES (${bind(createdAt)}, ${bind(createdBy)}, ${bind(createdAt)}, ${bind(createdAt)}, ${bind(createdBy)}, ${bind(calendarEventId)}, ${bind(time.date)}, ${bind(time.timeRange.start)}, ${bind(time.timeRange.end)})
RETURNING id
"""
            )
        }
        .executeAndReturnGeneratedKeys()
        .mapTo<CalendarEventTimeId>()
        .exactlyOne()

fun Database.Transaction.deleteCalendarEventTime(id: CalendarEventTimeId) =
    createUpdate { sql("DELETE FROM calendar_event_time WHERE id = ${bind(id)}") }
        .updateExactlyOne()

fun Database.Read.getCalendarEventById(id: CalendarEventId) =
    getCalendarEventsQuery(event(id)).exactlyOneOrNull<CalendarEvent>()

fun Database.Read.getCalendarEventsById(ids: Set<CalendarEventId>) =
    getCalendarEventsQuery(events(ids)).toList<CalendarEvent>()

fun Database.Read.getCalendarEventsByGroupAndType(
    groupId: GroupId,
    eventTypes: List<CalendarEventType>,
) = getCalendarEventsQuery(group(groupId).and(eventType(eventTypes))).toList<CalendarEvent>()

fun Database.Read.getCalendarEventsByUnitWithRange(unitId: DaycareId, range: FiniteDateRange) =
    getCalendarEventsQuery(unit(unitId).and(range(range))).toList<CalendarEvent>()

fun Database.Transaction.deleteCalendarEvent(eventId: CalendarEventId) =
    this.createUpdate { sql("DELETE FROM calendar_event WHERE id = ${bind(eventId)}") }
        .updateExactlyOne()

fun Database.Transaction.createCalendarEventAttendee(
    eventId: CalendarEventId,
    unitId: DaycareId,
    groupId: GroupId?,
    childId: ChildId?,
) =
    this.createUpdate {
            sql(
                """
INSERT INTO calendar_event_attendee (calendar_event_id, unit_id, group_id, child_id)
VALUES (${bind(eventId)}, ${bind(unitId)}, ${bind(groupId)}, ${bind(childId)})
"""
            )
        }
        .updateExactlyOne()

fun Database.Read.getCalendarEventIdByTimeId(id: CalendarEventTimeId) =
    createQuery { sql("SELECT calendar_event_id FROM calendar_event_time WHERE id = ${bind(id)}") }
        .exactlyOneOrNull<CalendarEventId>()

fun Database.Read.getCalendarEventChildIds(calendarEventId: CalendarEventId) =
    createQuery {
            sql(
                """
SELECT child_id
FROM calendar_event_attendee_child_view
WHERE calendar_event_id = ${bind(calendarEventId)}
"""
            )
        }
        .toList<ChildId>()

fun Database.Transaction.updateCalendarEvent(
    eventId: CalendarEventId,
    modifiedAt: HelsinkiDateTime,
    updateForm: CalendarEventUpdateForm,
) =
    createUpdate {
            sql(
                """
UPDATE calendar_event
SET title = ${bind(updateForm.title)}, description = ${bind(updateForm.description)}, modified_at = ${bind(modifiedAt)}, content_modified_at = ${bind(modifiedAt)}
WHERE id = ${bind(eventId)}
        """
            )
        }
        .updateExactlyOne()

fun Database.Transaction.insertCalendarEventTimeReservation(
    eventTimeId: CalendarEventTimeId,
    childId: ChildId?,
    modifiedAt: HelsinkiDateTime,
    modifiedBy: EvakaUserId,
) =
    createUpdate {
            sql(
                """
UPDATE calendar_event_time
SET child_id = ${bind(childId)}, modified_at = ${bind(modifiedAt)}, modified_by = ${bind(modifiedBy)}
WHERE id = ${bind(eventTimeId)} AND (child_id IS NULL OR child_id = ${bind(childId)})
"""
            )
        }
        .updateNoneOrOne()

fun Database.Transaction.freeCalendarEventTimeReservationsByChildAndEvent(
    user: AuthenticatedUser,
    now: HelsinkiDateTime,
    calendarEventId: CalendarEventId,
    childId: ChildId,
) = execute {
    sql(
        """
UPDATE calendar_event_time
SET child_id = NULL::uuid, modified_at = ${bind(now)}, modified_by = ${bind(user.evakaUserId)}
WHERE calendar_event_id = ${bind(calendarEventId)}
AND child_id = ${bind(childId)}
"""
    )
}

fun Database.Transaction.freeCalendarEventTimeReservation(
    user: AuthenticatedUser,
    now: HelsinkiDateTime,
    calendarEventTimeId: CalendarEventTimeId,
) = execute {
    sql(
        """
UPDATE calendar_event_time
SET child_id = NULL::uuid, modified_at = ${bind(now)}, modified_by = ${bind(user.evakaUserId)}
WHERE id = ${bind(calendarEventTimeId)}
"""
    )
}

fun Database.Transaction.freeCalendarEventTimeReservations(
    user: AuthenticatedUser,
    now: HelsinkiDateTime,
    calendarEventTimeIds: Set<CalendarEventTimeId>,
) =
    if (calendarEventTimeIds.isEmpty()) 0
    else
        execute {
            sql(
                """
UPDATE calendar_event_time
SET child_id = NULL::uuid, modified_at = ${bind(now)}, modified_by = ${bind(user.evakaUserId)}
WHERE id = ANY(${bind(calendarEventTimeIds)})
"""
            )
        }

fun Database.Read.getCalendarEventTimesByChildAndEvent(
    childId: PersonId,
    calendarEventId: CalendarEventId,
): List<CalendarEventTime> =
    createQuery {
            sql(
                """
SELECT id, date, start_time, end_time, child_id
FROM calendar_event_time
WHERE calendar_event_id = ${bind(calendarEventId)}
AND child_id = ${bind(childId)}
"""
            )
        }
        .toList<CalendarEventTime>()

data class CitizenCalendarEventRow(
    val id: CalendarEventId,
    val childId: ChildId,
    val period: FiniteDateRange,
    val eventPeriod: FiniteDateRange,
    val title: String,
    val description: String,
    val type: AttendanceType,
    val groupId: GroupId?,
    val groupName: String?,
    val unitId: DaycareId,
    val unitName: String,
)

enum class AttendanceType {
    INDIVIDUAL,
    GROUP,
    UNIT,
}

data class CitizenDiscussionSurveyRow(
    val id: CalendarEventId,
    val childId: ChildId,
    val period: FiniteDateRange,
    val eventPeriod: FiniteDateRange,
    val title: String,
    val description: String,
    val type: AttendanceType,
    val groupId: GroupId?,
    val groupName: String?,
    val unitId: DaycareId,
    val unitName: String,
    val eventTimeId: CalendarEventTimeId,
    val eventTimeOccupant: ChildId?,
    val eventTimeDate: LocalDate,
    val eventTimeStart: LocalTime,
    val eventTimeEnd: LocalTime,
)

fun Database.Read.getDaycareEventsForGuardian(
    guardianId: PersonId,
    range: FiniteDateRange,
): List<CitizenCalendarEventRow> =
    createQuery {
            sql(
                """
WITH child AS MATERIALIZED (
    SELECT g.child_id id FROM guardian g WHERE g.guardian_id = ${bind(guardianId)}
    UNION
    SELECT fp.child_id FROM foster_parent fp WHERE parent_id = ${bind(guardianId)} AND valid_during && ${bind(range)}
),
child_placement AS MATERIALIZED (
    SELECT p.id, p.unit_id, p.child_id, placement_without_backup.range period, null backup_group_id
    FROM placement p
    LEFT JOIN LATERAL (
        -- remove all backup care placements from the placement's range; this may result in
        -- multiple ranges (if the backup care is in the middle of the actual placement)
        SELECT range
        FROM unnest(
            datemultirange(daterange(p.start_date, p.end_date, '[]')) - (
                SELECT coalesce(range_agg(daterange(bc.start_date, bc.end_date, '[]')), datemultirange())
                FROM backup_care bc
                WHERE p.child_id = bc.child_id
                  AND daterange(bc.start_date, bc.end_date, '[]') && daterange(p.start_date, p.end_date, '[]')
            )
        ) range
    ) placement_without_backup ON true
    WHERE EXISTS(SELECT 1 FROM child WHERE p.child_id = child.id)
      AND placement_without_backup.range IS NOT NULL
      AND daterange(p.start_date, p.end_date, '[]') && ${bind(range)}
      
    UNION ALL
    
    -- add backup cares
    SELECT null id, bc.unit_id, bc.child_id, daterange(bc.start_date, bc.end_date, '[]') period, bc.group_id backup_group_id
    FROM backup_care bc
    WHERE EXISTS(SELECT 1 FROM child WHERE bc.child_id = child.id)
      AND daterange(bc.start_date, bc.end_date, '[]') && ${bind(range)}
)
SELECT ce.id, cp.child_id, ce.period * daterange(dgp.start_date, dgp.end_date, '[]') * cp.period period, ce.period event_period, ce.title, ce.description, (
    CASE WHEN cea.child_id IS NOT NULL THEN 'INDIVIDUAL'
         WHEN cea.group_id IS NOT NULL THEN 'GROUP'
         ELSE 'UNIT' END
) type, dg.id group_id, dg.name group_name, unit.id unit_id, unit.name unit_name
FROM child_placement cp
LEFT JOIN daycare_group_placement dgp ON cp.backup_group_id IS NULL AND dgp.daycare_placement_id = cp.id
LEFT JOIN calendar_event_attendee cea
    ON cea.unit_id = cp.unit_id
    AND (cea.child_id IS NULL OR cea.child_id = cp.child_id)
    AND (cea.group_id IS NULL OR cea.group_id = coalesce(cp.backup_group_id, dgp.daycare_group_id))
JOIN calendar_event ce ON ce.id = cea.calendar_event_id
LEFT JOIN daycare_group dg ON dg.id = cea.group_id
LEFT JOIN daycare unit ON unit.id = cea.unit_id
WHERE cp.period && ce.period
  AND ce.period && ${bind(range)}
  AND daterange(dgp.start_date, dgp.end_date, '[]') && ce.period
  AND daterange(dgp.start_date, dgp.end_date, '[]') && cp.period
  AND ce.event_type = 'DAYCARE_EVENT'
"""
            )
        }
        .toList<CitizenCalendarEventRow>()

fun Database.Read.getDiscussionSurveysForGuardian(
    guardianId: PersonId,
    range: FiniteDateRange,
): List<CitizenDiscussionSurveyRow> =
    createQuery {
            sql(
                """
WITH children_of_guardian AS MATERIALIZED (SELECT g.child_id id
                                FROM guardian g
                                WHERE g.guardian_id = ${bind(guardianId)}
                                UNION
                                SELECT fp.child_id
                                FROM foster_parent fp
                                WHERE parent_id = ${bind(guardianId)}
                                  AND valid_during && ${bind(range)}),
     child_placement AS MATERIALIZED (SELECT p.id,
                                                 p.unit_id,
                                                 p.child_id,
                                                 daterange(p.start_date, p.end_date, '[]') as period
                                          FROM placement p
                                          WHERE EXISTS(SELECT 1 FROM children_of_guardian WHERE p.child_id = children_of_guardian.id)
                                            AND daterange(p.start_date, p.end_date, '[]') && ${bind(range)})
SELECT ce.id,
       cp.child_id,
       ce.period * daterange(dgp.start_date, dgp.end_date, '[]') * cp.period as period,
       ce.period                                                             as event_period,
       ce.title,
       ce.description,
       (
           CASE
               WHEN cea.child_id IS NOT NULL THEN 'INDIVIDUAL'
               WHEN cea.group_id IS NOT NULL THEN 'GROUP'
               ELSE 'UNIT' END
           )                                                                 as type,
       dg.id                                                                 as group_id,
       dg.name                                                               as group_name,
       unit.id                                                               as unit_id,
       unit.name                                                             as unit_name,
       cet.id                                                                as event_time_id,
       cet.date                                                              as event_time_date,
       cet.start_time                                                        as event_time_start,
       cet.end_time                                                          as event_time_end,
       cet.child_id                                                          as event_time_occupant
FROM child_placement cp
         LEFT JOIN daycare_group_placement dgp ON dgp.daycare_placement_id = cp.id
         LEFT JOIN calendar_event_attendee cea
                   ON cea.unit_id = cp.unit_id
                       AND (cea.child_id IS NULL OR cea.child_id = cp.child_id)
                       AND (cea.group_id IS NULL OR cea.group_id = dgp.daycare_group_id)
         JOIN calendar_event ce ON ce.id = cea.calendar_event_id
         LEFT JOIN daycare_group dg ON dg.id = cea.group_id
         LEFT JOIN daycare unit ON unit.id = cea.unit_id
         JOIN calendar_event_time cet
                  ON ce.id = cet.calendar_event_id
                       AND (cet.child_id IS NULL OR cet.child_id = cp.child_id)
                       AND cp.period @> cet.date
                       AND daterange(dgp.start_date, dgp.end_date, '[]') @> cet.date
WHERE cp.period && ce.period
  AND ce.period && ${bind(range)}
  AND daterange(dgp.start_date, dgp.end_date, '[]') && ce.period
  AND daterange(dgp.start_date, dgp.end_date, '[]') && cp.period
  AND ce.event_type = 'DISCUSSION_SURVEY';
"""
            )
        }
        .toList<CitizenDiscussionSurveyRow>()

fun Database.Read.devCalendarEventUnitAttendeeCount(unitId: DaycareId): Int =
    this.createQuery {
            sql("SELECT COUNT(*) FROM calendar_event_attendee WHERE unit_id = ${bind(unitId)}")
        }
        .exactlyOne()

data class ParentWithEvents(
    val parentId: PersonId,
    val language: Language,
    val events: List<CalendarEventId>,
)

data class ParentWithEventTimes(
    val parentId: PersonId,
    val eventTimeId: CalendarEventTimeId,
    val language: Language,
)

data class ParentWithDiscussionSurveys(
    val parentId: PersonId,
    val language: Language,
    val surveys: List<CalendarEventId>,
)

fun Database.Read.getRecipientsForEventTimeRemindersAt(
    date: LocalDate
): List<ParentWithEventTimes> {
    return createQuery {
            sql(
                """
SELECT par.parent_id,
       par.language,
       cet.id as event_time_id
FROM calendar_event_time cet
JOIN LATERAL (
    SELECT g.guardian_id AS parent_id, p.language
    FROM guardian g
             JOIN person p ON p.id = g.guardian_id
    WHERE cet.child_id = g.child_id

    UNION

    SELECT fp.parent_id, p.language
    FROM foster_parent fp
             JOIN person p ON p.id = fp.parent_id
    WHERE cet.child_id = fp.child_id
      AND fp.valid_during @> ${bind(date)}
    ) par ON TRUE
WHERE cet.date = ${bind(date)}
  AND cet.child_id IS NOT NULL
"""
            )
        }
        .toList {
            ParentWithEventTimes(
                parentId = column("parent_id"),
                language = Language.tryValueOf(column<String?>("language")) ?: Language.fi,
                eventTimeId = column("event_time_id"),
            )
        }
}

fun Database.Read.getEventTimeReminderInfo(
    eventTimeId: CalendarEventTimeId
): DiscussionTimeReminderData? {
    data class RawRow(
        val title: String,
        val firstName: String,
        val lastName: String,
        val date: LocalDate,
        val startTime: LocalTime,
        val endTime: LocalTime,
        val childId: ChildId,
    )

    return createQuery {
            sql(
                """
SELECT p.id as child_id, 
      ce.title,
      p.first_name,
      p.last_name,
      cet.date,
      cet.start_time,
      cet.end_time
FROM calendar_event_time cet
JOIN person p on cet.child_id = p.id
JOIN calendar_event ce ON cet.calendar_event_id = ce.id
WHERE cet.id = ${bind(eventTimeId)}
"""
            )
        }
        .exactlyOneOrNull<RawRow>()
        ?.let {
            DiscussionTimeReminderData(
                date = it.date,
                startTime = it.startTime,
                endTime = it.endTime,
            )
        }
}

fun Database.Read.getParentsWithNewEventsAfter(
    today: LocalDate,
    cutoff: HelsinkiDateTime,
): List<ParentWithEvents> =
    createQuery {
            sql(
                """
WITH matching_events AS (
    SELECT id, period FROM calendar_event WHERE created_at >= ${bind(cutoff)} AND event_type = 'DAYCARE_EVENT'
), matching_children AS (
    SELECT ce.id AS event_id, ce.period * daterange(pl.start_date, pl.end_date, '[]') AS period, pl.child_id
    FROM matching_events ce
    JOIN calendar_event_attendee cea ON cea.calendar_event_id = ce.id
    JOIN placement pl ON pl.unit_id = cea.unit_id AND daterange(pl.start_date, pl.end_date, '[]') && ce.period AND daterange(pl.start_date, pl.end_date, '[]') @> ${bind(today)}
    WHERE
        -- Affects the whole unit
        cea.group_id IS NULL AND
        cea.child_id IS NULL AND
        NOT EXISTS (
            SELECT 1 FROM backup_care bc
            WHERE
                bc.child_id = pl.child_id AND
                bc.unit_id <> pl.unit_id AND
                daterange(bc.start_date, bc.end_date, '[]') @> ce.period
        )

    UNION ALL

    SELECT ce.id AS event_id, ce.period * daterange(dgp.start_date, dgp.end_date, '[]') AS period, pl.child_id
    FROM matching_events ce
    JOIN calendar_event_attendee cea ON cea.calendar_event_id = ce.id
    JOIN daycare_group_placement dgp ON dgp.daycare_group_id = cea.group_id AND daterange(dgp.start_date, dgp.end_date, '[]') && ce.period
    JOIN placement pl ON pl.id = dgp.daycare_placement_id AND daterange(pl.start_date, pl.end_date, '[]') @> ${bind(today)}
    WHERE
        -- Affects a single group
        cea.group_id IS NOT NULL AND
        cea.child_id IS NULL AND
        NOT EXISTS (
            SELECT 1 FROM backup_care bc
            WHERE
                bc.child_id = pl.child_id AND
                (bc.group_id IS NULL OR bc.group_id <> dgp.daycare_group_id) AND
                daterange(bc.start_date, bc.end_date, '[]') @> ce.period
        )

    UNION ALL

    SELECT ce.id AS event_id, ce.period, cea.child_id
    FROM matching_events ce
    JOIN calendar_event_attendee cea ON cea.calendar_event_id = ce.id
    WHERE
        EXISTS (SELECT FROM placement pl WHERE daterange(pl.start_date, pl.end_date, '[]') @> ${bind(today)}) AND
        -- Affects a single child (in a single group)
        cea.group_id IS NOT NULL AND
        cea.child_id IS NOT NULL
        -- We don't filter out backup care here because the child was "hand-picked"
        -- to participate in this event
), matching_parents AS (
    -- List each event at most once per parent

    SELECT DISTINCT g.guardian_id AS parent_id, mc.event_id
    FROM matching_children mc
    JOIN guardian g ON g.child_id = mc.child_id

    UNION

    SELECT DISTINCT fp.parent_id, mc.event_id
    FROM matching_children mc
    JOIN foster_parent fp ON fp.child_id = mc.child_id AND fp.valid_during && mc.period
)
SELECT mp.parent_id, p.language, array_agg(mp.event_id) AS events
FROM matching_parents mp
JOIN person p ON p.id = mp.parent_id
GROUP BY mp.parent_id, p.language
"""
            )
        }
        .toList {
            ParentWithEvents(
                parentId = column("parent_id"),
                language = Language.tryValueOf(column<String?>("language")) ?: Language.fi,
                events = column("events"),
            )
        }

fun Database.Read.getParentsWithNewDiscussionSurveysAfter(
    cutoff: HelsinkiDateTime
): List<ParentWithDiscussionSurveys> {
    return createQuery {
            sql(
                """
WITH matching_events AS (
    SELECT id, period FROM calendar_event WHERE created_at >= ${bind(cutoff)} AND event_type = 'DISCUSSION_SURVEY'
), matching_children AS (
    SELECT ce.id AS event_id, ce.period * daterange(pl.start_date, pl.end_date, '[]') AS period, pl.child_id
    FROM matching_events ce
             JOIN calendar_event_attendee cea ON cea.calendar_event_id = ce.id
             JOIN daycare_group_placement dgp ON dgp.daycare_group_id = cea.group_id AND daterange(dgp.start_date, dgp.end_date, '[]') && ce.period
             JOIN placement pl ON pl.id = dgp.daycare_placement_id
    WHERE
      -- Affects a single group
        cea.group_id IS NOT NULL AND
        cea.child_id IS NULL

    UNION ALL

    SELECT ce.id AS event_id, ce.period, cea.child_id
    FROM matching_events ce
             JOIN calendar_event_attendee cea ON cea.calendar_event_id = ce.id
    WHERE
      -- Affects a single child (in a single group)
        cea.group_id IS NOT NULL AND
        cea.child_id IS NOT NULL

), matching_parents AS (
    -- List each event at most once per parent

    SELECT DISTINCT g.guardian_id AS parent_id, mc.event_id
    FROM matching_children mc
             JOIN guardian g ON g.child_id = mc.child_id

    UNION

    SELECT DISTINCT fp.parent_id, mc.event_id
    FROM matching_children mc
             JOIN foster_parent fp ON fp.child_id = mc.child_id AND fp.valid_during && mc.period
)
SELECT
    mp.parent_id,
    p.language,
    array_agg(
        mp.event_id
    ) AS surveys
FROM matching_parents mp
         JOIN person p ON p.id = mp.parent_id
         JOIN calendar_event ce ON ce.id = mp.event_id
GROUP BY mp.parent_id, p.language
"""
            )
        }
        .toList {
            ParentWithDiscussionSurveys(
                parentId = column("parent_id"),
                language = Language.tryValueOf(column<String?>("language")) ?: Language.fi,
                surveys = column("surveys"),
            )
        }
}

fun Database.Transaction.updateCalendarEventPeriod(
    eventId: CalendarEventId,
    modifiedAt: HelsinkiDateTime,
    period: FiniteDateRange,
) =
    this.createUpdate {
            sql(
                """
UPDATE calendar_event
SET period = ${bind(period)}, modified_at = ${bind(modifiedAt)}, content_modified_at = ${bind(modifiedAt)}
WHERE id = ${bind(eventId)}
        """
            )
        }
        .updateExactlyOne()

data class DiscussionTimeDetailsRow(
    @Nested("et") val eventTime: CalendarEventTime,
    val title: String,
)

fun Database.Read.getDiscussionTimeDetailsByEventTimeId(id: CalendarEventTimeId) =
    createQuery {
            sql(
                """
SELECT distinct cet.id as et_id, cet.date as et_date, cet.start_time as et_start_time, cet.end_time as et_end_time, cet.child_id as et_child_id, ce.title
FROM calendar_event_time cet
JOIN calendar_event ce ON cet.calendar_event_id = ce.id 
JOIN calendar_event_attendee cea ON cea.calendar_event_id = ce.id
WHERE cet.id = ${bind(id)}
        
        """
                    .trimIndent()
            )
        }
        .exactlyOneOrNull<DiscussionTimeDetailsRow>()
