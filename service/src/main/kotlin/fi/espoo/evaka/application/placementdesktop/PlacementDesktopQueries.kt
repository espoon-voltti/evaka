// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.application.placementdesktop

import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.HelsinkiDateTime

fun Database.Transaction.updateApplicationPlacementDraft(
    applicationId: ApplicationId,
    unitId: DaycareId?,
    now: HelsinkiDateTime,
    userId: EmployeeId,
) {
    if (unitId == null) {
        execute {
            sql(
                """
            DELETE FROM placement_draft
            WHERE application_id = ${bind(applicationId)}
        """
            )
        }
    } else {
        createUpdate {
                sql(
                    """
            INSERT INTO placement_draft (application_id, unit_id, created_at, created_by, modified_at, modified_by)
            VALUES (${bind(applicationId)}, ${bind(unitId)}, ${bind(now)}, ${bind(userId)}, ${bind(now)}, ${bind(userId)})
            ON CONFLICT (application_id) DO UPDATE SET
                unit_id = ${bind(unitId)},
                modified_at = ${bind(now)},
                modified_by = ${bind(userId)}
        """
                )
            }
            .updateExactlyOne()
    }
}
