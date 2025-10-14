// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.children

import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.FiniteDateRange
import java.time.LocalDate

fun Database.Read.getChildrenByParent(id: PersonId, today: LocalDate, calendarOpenBeforePlacementDays: Int): List<Child> =
    createQuery {
            sql(
                """
WITH children AS (
    SELECT child_id FROM guardian WHERE guardian_id = :id
    UNION
    SELECT child_id FROM foster_parent WHERE parent_id = :id AND valid_during @> :today
)
SELECT
    p.id,
    p.first_name,
    p.preferred_name,
    p.last_name,
    p.duplicate_of,
    img.id AS image_id,
    dg.id AS group_id,
    dg.name AS group_name,
    u.id AS unit_id,
    u.name AS unit_name,
    upcoming_pl.type AS upcoming_placement_type,
    upcoming_pl.type IS NOT NULL AS has_upcoming_placements,
    upcoming_pl.start_date AS upcoming_placement_start_date,
    upcoming_unit.id AS upcoming_unit_id,
    upcoming_unit.name AS upcoming_unit_name,
    upcoming_pl.is_calendar_open AS upcoming_placement_is_calendar_open
FROM children c
INNER JOIN person p ON c.child_id = p.id
LEFT JOIN child_images img ON p.id = img.child_id
LEFT JOIN placement current_pl ON current_pl.child_id = p.id AND daterange(current_pl.start_date, current_pl.end_date, '[]') @> :today::date
LEFT JOIN daycare_group_placement dgp ON current_pl.id = dgp.daycare_placement_id AND daterange(dgp.start_date, dgp.end_date, '[]') @> :today::date
LEFT JOIN daycare_group dg ON dgp.daycare_group_id = dg.id
LEFT JOIN daycare u ON u.id = dg.daycare_id
LEFT JOIN LATERAL (
  SELECT child_id, type, start_date, unit_id,
  (start_date <= (:today + (:calendarOpenBeforePlacementDays::int * INTERVAL '1 day'))) AS is_calendar_open
  FROM placement
  WHERE child_id = p.id AND :today <= end_date
  ORDER BY start_date
  LIMIT 1
) upcoming_pl ON true
LEFT JOIN daycare upcoming_unit ON upcoming_pl.unit_id = upcoming_unit.id
ORDER BY p.date_of_birth, p.last_name, p.first_name, p.duplicate_of
"""
            )
        }
        .bind("id", id)
        .bind("today", today)
        .bind("calendarOpenBeforePlacementDays", calendarOpenBeforePlacementDays)
        .toList()

fun Database.Read.getCitizenChildIds(today: LocalDate, userId: PersonId): List<ChildId> =
    createQuery {
            sql(
                """
SELECT child_id FROM guardian WHERE guardian_id = ${bind(userId)}
UNION ALL
SELECT child_id FROM foster_parent WHERE parent_id = ${bind(userId)} AND valid_during @> ${bind(today)}
"""
            )
        }
        .toList()

fun Database.Read.getChildIdsByGuardians(guardianIds: Set<PersonId>): Map<PersonId, Set<ChildId>> =
    createQuery {
            sql(
                """
SELECT guardian_id, child_id
FROM guardian
WHERE guardian_id = ANY (${bind(guardianIds)})
"""
            )
        }
        .map { columnPair<PersonId, ChildId>("guardian_id", "child_id") }
        .useSequence { rows ->
            rows.groupBy({ it.first }, { it.second }).mapValues { it.value.toSet() }
        }

fun Database.Read.getChildIdsByHeadsOfFamily(
    headOfFamilyIds: Set<PersonId>,
    range: FiniteDateRange,
): Map<PersonId, Map<ChildId, FiniteDateRange>> =
    createQuery {
            sql(
                """
SELECT head_of_child, child_id, daterange(start_date, end_date, '[]') * ${bind(range)} AS valid_during
FROM fridge_child
WHERE
    NOT conflict AND
    head_of_child = ANY (${bind(headOfFamilyIds)}) AND
    daterange(start_date, end_date, '[]') && ${bind(range)}
"""
            )
        }
        .map {
            Triple(
                column<PersonId>("head_of_child"),
                column<PersonId>("child_id"),
                column<FiniteDateRange>("valid_during"),
            )
        }
        .useSequence { rows ->
            rows.groupBy({ it.first }, { it.second to it.third }).mapValues { (_, value) ->
                value.associateBy({ it.first }, { it.second })
            }
        }
