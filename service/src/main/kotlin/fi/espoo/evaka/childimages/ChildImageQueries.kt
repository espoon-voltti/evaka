// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.childimages

import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.ChildImageId
import fi.espoo.evaka.shared.db.Database

fun Database.Transaction.insertChildImage(childId: ChildId): ChildImageId {
    // language=sql
    val sql =
        """
        INSERT INTO child_images (child_id) VALUES (:childId) RETURNING id;
    """
            .trimIndent()
    @Suppress("DEPRECATION")
    return createQuery(sql).bind("childId", childId).exactlyOne<ChildImageId>()
}

fun Database.Transaction.deleteChildImage(childId: ChildId): ChildImageId? {
    @Suppress("DEPRECATION")
    return createQuery("DELETE FROM child_images WHERE child_id = :childId RETURNING id")
        .bind("childId", childId)
        .exactlyOneOrNull<ChildImageId>()
}
