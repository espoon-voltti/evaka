// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.childimages

import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.ChildImageId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import org.jdbi.v3.core.kotlin.mapTo

fun Database.Transaction.insertChildImage(childId: ChildId): ChildImageId {
    // language=sql
    val sql = """
        INSERT INTO child_images (child_id) VALUES (:childId) RETURNING id;
    """.trimIndent()
    return createQuery(sql)
        .bind("childId", childId)
        .mapTo<ChildImageId>()
        .one()
}

fun Database.Transaction.deleteChildImage(childId: ChildId): ChildImageId? {
    return createQuery("DELETE FROM child_images WHERE child_id = :childId RETURNING id")
        .bind("childId", childId)
        .mapTo<ChildImageId>()
        .firstOrNull()
}

fun Database.Read.hasPermissionForChildImage(user: AuthenticatedUser.Citizen, id: ChildImageId): Boolean {
    return createQuery(
        """
SELECT EXISTS(
    SELECT 1
    FROM child_images img
    JOIN person child ON img.child_id = child.id
    JOIN guardian g ON child.id = g.child_id AND g.guardian_id = :guardianId
    WHERE
        img.id = :imageId
)

        """.trimIndent()
    ).bind("guardianId", user.id).bind("imageId", id).mapTo<Boolean>().one()
}
