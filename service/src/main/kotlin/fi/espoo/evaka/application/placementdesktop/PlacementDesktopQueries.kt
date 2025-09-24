// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.application.placementdesktop

import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.HelsinkiDateTime

fun Database.Transaction.updateApplicationTrialPlacement(
    applicationId: ApplicationId,
    trialUnitId: DaycareId?,
    now: HelsinkiDateTime,
    user: AuthenticatedUser.Employee,
) =
    createUpdate {
            sql(
                """
    UPDATE application
    SET trial_placement_unit = ${bind(trialUnitId)}, 
        modified_at = ${bind(now)}, 
        modified_by = ${bind(user.evakaUserId)}
    WHERE id = ${bind(applicationId)}
"""
            )
        }
        .updateExactlyOne()
