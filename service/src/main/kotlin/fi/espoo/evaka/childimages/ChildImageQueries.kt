// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.childimages

import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.NotFound
import org.jdbi.v3.core.kotlin.mapTo
import java.util.UUID

fun Database.Transaction.insertChildImage(childId: UUID): UUID {
    // language=sql
    val sql = """
        INSERT INTO child_images (child_id) VALUES (:childId) RETURNING id;
    """.trimIndent()
    return createQuery(sql)
        .bind("childId", childId)
        .mapTo<UUID>()
        .one()
}

fun Database.Transaction.deleteChildImage(childId: UUID): UUID? {
    return createQuery("DELETE FROM child_images WHERE child_id = :childId RETURNING id")
        .bind("childId", childId)
        .mapTo<UUID>()
        .firstOrNull()
}

fun Database.Read.getChildImage(imageId: UUID): ChildImage {
    return createQuery("SELECT * FROM child_images WHERE id = :id;")
        .bind("id", imageId)
        .mapTo<ChildImage>()
        .firstOrNull() ?: throw NotFound("Image $imageId not found")
}
