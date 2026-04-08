// SPDX-FileCopyrightText: 2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.user

import evaka.core.shared.EvakaUserId
import evaka.core.shared.db.Database

fun Database.Read.getEvakaUser(evakaUserId: EvakaUserId): EvakaUser? =
    createQuery {
            sql(
                """
SELECT id, name, type
FROM evaka_user
WHERE id = ${bind(evakaUserId)}
"""
            )
        }
        .exactlyOneOrNull<EvakaUser>()
