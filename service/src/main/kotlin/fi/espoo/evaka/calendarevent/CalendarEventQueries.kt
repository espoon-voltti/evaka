// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.calendarevent

import fi.espoo.evaka.daycare.domain.Language
import fi.espoo.evaka.emailclient.CalendarEventNotificationData
import fi.espoo.evaka.shared.CalendarEventId
import fi.espoo.evaka.shared.CalendarEventTimeId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import java.time.LocalDate
import java.time.LocalTime
import org.jdbi.v3.core.mapper.Nested

fun Database.Read.getCalendarEventsByUnit(
    unitId: DaycareId,
    range: FiniteDateRange,
): List<CalendarEvent> =
    getCalendarEventsQuery(unitId = unitId, range = range).toList<CalendarEvent>()

private fun Database.Read.getCalendarEventsQuery(
    calendarEventId: CalendarEventId? = null,
    unitId: DaycareId? = null,
    range: FiniteDateRange? = null,
    groupId: GroupId? = null,
    eventTypes: List<CalendarEventType>? = null,
) =
    createQuery {
            sql(
                """
SELECT
    ce.id, cea.unit_id, ce.title, ce.description, ce.period, ce.content_modified_at, ce.event_type,
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
WHERE (:calendarEventId IS NULL OR ce.id = :calendarEventId) 
AND (:unitId IS NULL OR cea.unit_id = :unitId) 
AND (:groupId IS NULL OR cea.group_id = :groupId)
AND (:range IS NULL OR ce.period && :range)
AND (:eventTypes::calendar_event_type[] IS NULL OR ce.event_type = ANY(:eventTypes::calendar_event_type[]))
AND (cea.child_id IS NULL OR EXISTS(
    -- filter out attendees that haven't been placed in the specified unit/group,
    -- for example due to changes in placements after the event creation or a new backup care
    SELECT 1 FROM generate_series(lower(ce.period), upper(ce.period) - INTERVAL '1 day', '1 day') d
    JOIN realized_placement_one(d::date) rp ON true
    WHERE rp.child_id = cea.child_id
      AND (cea.group_id IS NULL OR rp.group_id = cea.group_id)
      AND rp.unit_id = cea.unit_id
))
GROUP BY ce.id, cea.unit_id
        """
            )
        }
        .bind("calendarEventId", calendarEventId)
        .bind("unitId", unitId)
        .bind("groupId", groupId)
        .bind("range", range)
        .bind("eventTypes", eventTypes.takeIf { !it.isNullOrEmpty() })

fun Database.Transaction.createCalendarEvent(
    event: CalendarEventForm,
    createdAt: HelsinkiDateTime,
    createdBy: EvakaUserId,
): CalendarEventId {
    val eventId =
        createUpdate {
                sql(
                    """
INSERT INTO calendar_event (created_at, title, description, period, modified_at, content_modified_at, event_type)
VALUES (${bind(createdAt)}, ${bind(event.title)}, ${bind(event.description)}, ${bind(event.period)}, ${bind(createdAt)}, ${bind(createdAt)}, ${bind(event.eventType)})
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
    getCalendarEventsQuery(calendarEventId = id).exactlyOneOrNull<CalendarEvent>()

fun Database.Read.getCalendarEventsByGroupAndType(
    groupId: GroupId,
    eventTypes: List<CalendarEventType>,
) = getCalendarEventsQuery(groupId = groupId, eventTypes = eventTypes).toList<CalendarEvent>()

fun Database.Read.getCalendarEventsByUnitWithRange(unitId: DaycareId, range: FiniteDateRange) =
    getCalendarEventsQuery(unitId = unitId, range = range).toList<CalendarEvent>()

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

fun Database.Transaction.deleteCalendarEventTimeReservations(
    calendarEventId: CalendarEventId,
    childId: ChildId,
) = execute {
    sql(
        """
UPDATE calendar_event_time
SET child_id = NULL::uuid
WHERE calendar_event_id = ${bind(calendarEventId)}
AND child_id = ${bind(childId)}
"""
    )
}

fun Database.Transaction.deleteCalendarEventTimeReservation(
    calendarEventTimeId: CalendarEventTimeId
) = execute {
    sql(
        """
UPDATE calendar_event_time
SET child_id = NULL::uuid
WHERE id = ${bind(calendarEventTimeId)}
"""
    )
}

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
WITH child AS NOT MATERIALIZED (
    SELECT g.child_id id FROM guardian g WHERE g.guardian_id = ${bind(guardianId)}
    UNION
    SELECT fp.child_id FROM foster_parent fp WHERE parent_id = ${bind(guardianId)} AND valid_during && ${bind(range)}
),
child_placement AS NOT MATERIALIZED (
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
WITH children_of_guardian AS NOT MATERIALIZED (SELECT g.child_id id
                                FROM guardian g
                                WHERE g.guardian_id = ${bind(guardianId)}
                                UNION
                                SELECT fp.child_id
                                FROM foster_parent fp
                                WHERE parent_id = ${bind(guardianId)}
                                  AND valid_during && ${bind(range)}),
     child_placement AS NOT MATERIALIZED (SELECT p.id,
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
    val events: List<CalendarEventNotificationData>,
)

fun Database.Read.getParentsWithNewEventsAfter(cutoff: HelsinkiDateTime): List<ParentWithEvents> {
    return createQuery {
            sql(
                """
WITH matching_events AS (
    SELECT id, period FROM calendar_event WHERE created_at >= ${bind(cutoff)}
), matching_children AS (
    SELECT ce.id AS event_id, ce.period * daterange(pl.start_date, pl.end_date, '[]') AS period, pl.child_id
    FROM matching_events ce
    JOIN calendar_event_attendee cea ON cea.calendar_event_id = ce.id
    JOIN placement pl ON pl.unit_id = cea.unit_id AND daterange(pl.start_date, pl.end_date, '[]') && ce.period
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
    JOIN placement pl ON pl.id = dgp.daycare_placement_id
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
SELECT
    mp.parent_id,
    p.language,
    jsonb_agg(
        jsonb_build_object(
            'title', ce.title,
            'period', jsonb_build_object(
                'start', lower(ce.period),
                'end', upper(ce.period) - 1
            )
        )
        ORDER BY lower(ce.period)
    ) AS events
FROM matching_parents mp
JOIN person p ON p.id = mp.parent_id
JOIN calendar_event ce ON ce.id = mp.event_id
GROUP BY mp.parent_id, p.language
"""
            )
        }
        .toList {
            ParentWithEvents(
                parentId = column("parent_id"),
                language = Language.tryValueOf(column<String?>("language")) ?: Language.fi,
                events = jsonColumn<List<CalendarEventNotificationData>>("events"),
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
    val unitName: String,
)

fun Database.Read.getDiscussionTimeDetailsByEventTimeId(id: CalendarEventTimeId) =
    createQuery {
            sql(
                """
SELECT distinct cet.id as et_id, cet.date as et_date, cet.start_time as et_start_time, cet.end_time as et_end_time, cet.child_id as et_child_id, ce.title, d.name as unit_name
FROM calendar_event_time cet
JOIN calendar_event ce ON cet.calendar_event_id = ce.id 
JOIN calendar_event_attendee cea ON cea.calendar_event_id = ce.id
JOIN daycare d ON d.id = cea.unit_id
WHERE cet.id = ${bind(id)}
        
        """
                    .trimIndent()
            )
        }
        .exactlyOneOrNull<DiscussionTimeDetailsRow>()
