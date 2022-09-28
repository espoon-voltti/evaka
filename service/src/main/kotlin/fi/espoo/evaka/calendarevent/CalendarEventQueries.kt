// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.calendarevent

import fi.espoo.evaka.shared.CalendarEventId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.FiniteDateRange

fun Database.Read.getCalendarEventsByUnit(unitId: DaycareId, range: FiniteDateRange): List<CalendarEvent> =
    this.createQuery(
        """
SELECT
    ce.id, cea.unit_id, ce.title, ce.description, ce.period,
    (
        coalesce(jsonb_agg(DISTINCT jsonb_build_object(
            'id', cea.group_id,
            'name', dg.name
        )) FILTER (WHERE cea.group_id IS NOT NULL), '[]'::jsonb)
    ) AS groups,
    (
        coalesce(jsonb_agg(DISTINCT jsonb_build_object(
            'id', cea.child_id,
            'name', concat(p.first_name, ' ', p.last_name),
            'groupId', cea.group_id
        )) FILTER (WHERE cea.child_id IS NOT NULL), '[]'::jsonb)
    ) AS individual_children
FROM calendar_event_attendee cea
JOIN calendar_event ce ON cea.calendar_event_id = ce.id
LEFT JOIN daycare_group dg ON dg.id = cea.group_id
LEFT JOIN person p ON p.id = cea.child_id
WHERE cea.unit_id = :unitId AND ce.period && :range AND (cea.child_id IS NULL OR EXISTS(
    -- filter out attendees that haven't been placed in the specified unit/group,
    -- for example due to changes in placements after the event creation or a new backup care
    SELECT 1 FROM generate_series(lower(ce.period), upper(ce.period) - INTERVAL '1 day', '1 day') d
    JOIN realized_placement_one(d::date) rp ON true
    WHERE rp.child_id = cea.child_id
      AND (cea.group_id IS NULL OR rp.group_id = cea.group_id)
      AND rp.unit_id = cea.unit_id
))
GROUP BY ce.id, cea.unit_id
        """.trimIndent()
    )
        .bind("unitId", unitId)
        .bind("range", range)
        .mapTo<CalendarEvent>()
        .list()

fun Database.Transaction.createCalendarEvent(event: CalendarEventForm) {
    val eventId = this.createUpdate(
        """
INSERT INTO calendar_event (title, description, period)
VALUES (:title, :description, :period)
RETURNING id
        """.trimIndent()
    )
        .bindKotlin(event)
        .executeAndReturnGeneratedKeys()
        .mapTo<CalendarEventId>()
        .first()

    if (event.tree != null) {
        event.tree.forEach { (groupId, childIds) ->
            // TODO: batching
            if (childIds != null) {
                childIds.forEach { childId -> createCalendarEventAttendee(eventId, event.unitId, groupId, childId) }
            } else {
                createCalendarEventAttendee(eventId, event.unitId, groupId, null)
            }
        }
    } else {
        createCalendarEventAttendee(eventId, event.unitId, null, null)
    }
}

fun Database.Transaction.deleteCalendarEvent(eventId: CalendarEventId) =
    this.createUpdate(
        """
DELETE FROM calendar_event WHERE id = :id
        """.trimIndent()
    )
        .bind("id", eventId)
        .updateExactlyOne()

fun Database.Transaction.createCalendarEventAttendee(
    eventId: CalendarEventId,
    unitId: DaycareId,
    groupId: GroupId?,
    childId: ChildId?
) =
    this.createUpdate(
        """
INSERT INTO calendar_event_attendee (calendar_event_id, unit_id, group_id, child_id)
VALUES (:eventId, :unitId, :groupId, :childId)
        """.trimIndent()
    )
        .bind("eventId", eventId)
        .bind("unitId", unitId)
        .bind("groupId", groupId)
        .bind("childId", childId)
        .updateExactlyOne()

fun Database.Transaction.updateCalendarEvent(eventId: CalendarEventId, updateForm: CalendarEventUpdateForm) =
    this.createUpdate(
        """
UPDATE calendar_event
SET title = :title, description = :description
WHERE id = :eventId
        """.trimIndent()
    )
        .bind("eventId", eventId)
        .bindKotlin(updateForm)
        .updateExactlyOne()

data class CitizenCalendarEventRow(
    val id: CalendarEventId,
    val childId: ChildId,
    val period: FiniteDateRange,
    val title: String,
    val description: String,
    val type: String,
    val groupId: GroupId?,
    val groupName: String?,
    val unitId: DaycareId,
    val unitName: String
)

fun Database.Read.getCalendarEventsForGuardian(guardianId: PersonId, range: FiniteDateRange): List<CitizenCalendarEventRow> =
    this.createQuery(
        """
WITH child AS NOT MATERIALIZED (SELECT g.child_id id FROM guardian g WHERE g.guardian_id = :guardianId),
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
      AND daterange(p.start_date, p.end_date, '[]') && :range
      
    UNION ALL
    
    -- add backup cares
    SELECT null id, bc.unit_id, bc.child_id, daterange(bc.start_date, bc.end_date, '[]') period, bc.group_id backup_group_id
    FROM backup_care bc
    WHERE EXISTS(SELECT 1 FROM child WHERE bc.child_id = child.id)
      AND daterange(bc.start_date, bc.end_date, '[]') && :range
)
SELECT ce.id, cp.child_id, ce.period * daterange(dgp.start_date, dgp.end_date, '[]') * cp.period period, ce.title, ce.description, (
    CASE WHEN cea.child_id IS NOT NULL THEN 'individual'
         WHEN cea.group_id IS NOT NULL THEN 'group'
         ELSE 'unit' END
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
  AND ce.period && :range
  AND daterange(dgp.start_date, dgp.end_date, '[]') && ce.period
  AND daterange(dgp.start_date, dgp.end_date, '[]') && cp.period
        """.trimIndent()
    )
        .bind("guardianId", guardianId)
        .bind("range", range)
        .mapTo<CitizenCalendarEventRow>()
        .list()

fun Database.Read.devCalendarEventUnitAttendeeCount(unitId: DaycareId): Int =
    this.createQuery(
        """
SELECT COUNT(*) FROM calendar_event_attendee WHERE unit_id = :unitId
        """.trimIndent()
    )
        .bind("unitId", unitId)
        .mapTo<Int>()
        .first()
