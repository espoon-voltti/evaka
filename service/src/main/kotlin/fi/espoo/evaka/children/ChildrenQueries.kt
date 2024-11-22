// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.children

import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.FiniteDateRange
import java.time.LocalDate

fun Database.Read.getChildrenByParent(id: PersonId, today: LocalDate): List<Child> =
    createQuery {
            sql(
                """
WITH children AS (
    SELECT child_id FROM guardian WHERE guardian_id = ${bind(id)}
    UNION
    SELECT child_id FROM foster_parent WHERE parent_id = ${bind(id)} AND valid_during @> ${bind(today)}
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
    EXISTS (SELECT 1 FROM pedagogical_document doc WHERE p.id = doc.child_id) has_pedagogical_documents
FROM children c
INNER JOIN person p ON c.child_id = p.id
LEFT JOIN child_images img ON p.id = img.child_id
LEFT JOIN placement current_pl ON current_pl.child_id = p.id AND daterange(current_pl.start_date, current_pl.end_date, '[]') @> ${bind(today)}::date
LEFT JOIN daycare_group_placement dgp ON current_pl.id = dgp.daycare_placement_id AND daterange(dgp.start_date, dgp.end_date, '[]') @> ${bind(today)}::date
LEFT JOIN daycare_group dg ON dgp.daycare_group_id = dg.id
LEFT JOIN daycare u ON u.id = dg.daycare_id
LEFT JOIN LATERAL (
  SELECT child_id, type
  FROM placement
  WHERE child_id = p.id AND ${bind(today)} <= end_date
  ORDER BY start_date
  LIMIT 1
) upcoming_pl ON true
ORDER BY p.date_of_birth, p.last_name, p.first_name, p.duplicate_of
"""
            )
        }
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
