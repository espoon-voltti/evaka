// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.children

import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import org.jdbi.v3.core.kotlin.mapTo

fun Database.Read.getChildrenByGuardian(id: PersonId): List<Child> =
    this.createQuery(
        """
SELECT
    p.id,
    p.first_name,
    p.last_name,
    img.id AS image_id,
    dg.id AS group_id,
    dg.name AS group_name
FROM guardian g
INNER JOIN person p ON g.child_id = p.id
LEFT JOIN child_images img ON p.id = img.child_id
LEFT JOIN placement pl ON pl.child_id = p.id AND daterange(pl.start_date, pl.end_date, '[]') @> :today::date
LEFT JOIN daycare_group_placement dgp ON pl.id = dgp.daycare_placement_id AND daterange(dgp.start_date, dgp.end_date, '[]') @> :today::date
LEFT JOIN daycare_group dg ON dgp.daycare_group_id = dg.id
WHERE
    g.guardian_id = :userId
        """.trimIndent()
    )
        .bind("today", HelsinkiDateTime.now().toLocalDate())
        .bind("userId", id)
        .mapTo<Child>()
        .list()

fun Database.Read.getChild(id: PersonId): Child? =
    this.createQuery(
        """
SELECT
    p.id,
    p.first_name,
    p.last_name,
    img.id AS image_id,
    dg.id AS group_id,
    dg.name AS group_name
FROM person p
LEFT JOIN child_images img ON p.id = img.child_id
LEFT JOIN placement pl ON pl.child_id = p.id AND daterange(pl.start_date, pl.end_date, '[]') @> :today::date
LEFT JOIN daycare_group_placement dgp ON pl.id = dgp.daycare_placement_id AND daterange(dgp.start_date, dgp.end_date, '[]') @> :today::date
LEFT JOIN daycare_group dg ON dgp.daycare_group_id = dg.id
WHERE
    p.id = :childId
        """.trimIndent()
    )
        .bind("today", HelsinkiDateTime.now().toLocalDate())
        .bind("childId", id)
        .mapTo<Child>()
        .firstOrNull()
