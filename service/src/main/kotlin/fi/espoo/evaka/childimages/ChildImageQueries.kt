// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.childimages

import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.ChildImageId
import fi.espoo.evaka.shared.db.Database

fun Database.Transaction.insertChildImage(childId: ChildId): ChildImageId =
    createQuery {
            sql(
                """
        INSERT INTO child_images (child_id) VALUES (${bind(childId)}) RETURNING id
    """
            )
        }
        .exactlyOne()

fun Database.Transaction.deleteChildImage(childId: ChildId): ChildImageId? =
    createQuery { sql("DELETE FROM child_images WHERE child_id = ${bind(childId)} RETURNING id") }
        .exactlyOneOrNull<ChildImageId>()
