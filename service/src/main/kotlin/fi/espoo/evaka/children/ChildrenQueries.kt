// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.children

import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.db.Database
import java.time.LocalDate

fun Database.Read.getChildrenByParent(id: PersonId, today: LocalDate): List<Child> =
    this.createQuery(
        """
WITH children AS (
    SELECT child_id FROM guardian WHERE guardian_id = :userId
    UNION
    SELECT child_id FROM foster_parent WHERE parent_id = :userId AND valid_during @> :today
)
SELECT
    p.id,
    p.first_name,
    p.preferred_name,
    p.last_name,
    img.id AS image_id,
    dg.id AS group_id,
    dg.name AS group_name,
    EXISTS (SELECT 1 FROM placement pl WHERE p.id = pl.child_id AND :today <= pl.end_date) AS has_upcoming_placements,
    EXISTS (SELECT 1 FROM pedagogical_document doc WHERE p.id = doc.child_id) has_pedagogical_documents,
    EXISTS (SELECT 1 FROM curriculum_document doc WHERE p.id = doc.child_id) has_curriculums
FROM children c
INNER JOIN person p ON c.child_id = p.id
LEFT JOIN child_images img ON p.id = img.child_id
LEFT JOIN placement pl ON pl.child_id = p.id AND daterange(pl.start_date, pl.end_date, '[]') @> :today::date
LEFT JOIN daycare_group_placement dgp ON pl.id = dgp.daycare_placement_id AND daterange(dgp.start_date, dgp.end_date, '[]') @> :today::date
LEFT JOIN daycare_group dg ON dgp.daycare_group_id = dg.id
ORDER BY p.date_of_birth, p.last_name, p.first_name
        """.trimIndent()
    )
        .bind("today", today)
        .bind("userId", id)
        .mapTo<Child>()
        .list()
