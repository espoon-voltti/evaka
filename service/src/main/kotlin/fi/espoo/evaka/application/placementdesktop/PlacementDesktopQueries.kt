// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.application.placementdesktop

import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.NotFound

fun Database.Transaction.updateApplicationPlacementDraft(
    applicationId: ApplicationId,
    unitId: DaycareId?,
    now: HelsinkiDateTime,
    userId: EvakaUserId,
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

fun Database.Read.getPlacementDesktopDaycaresWithoutOccupancies(unitIds: Set<DaycareId>) =
    createQuery {
            sql(
                """
    SELECT 
        d.id,
        d.name,
        coalesce((
            SELECT jsonb_agg(jsonb_build_object(
                'applicationId', pd.application_id, 
                'unitId', pd.unit_id,
                'childId', c.id,
                'childName', c.last_name || ' ' || c.first_name,
                'modifiedAt', pd.modified_at,
                'modifiedBy', jsonb_build_object(
                    'id', eu.id, 
                    'name', eu.name,
                    'type', eu.type
                )
            ))
            FROM placement_draft pd
            JOIN application a ON a.id = pd.application_id
            JOIN person c ON c.id = a.child_id
            JOIN evaka_user eu ON eu.id = pd.modified_by
            WHERE d.id = pd.unit_id
        ), '[]'::jsonb) AS placement_drafts
    FROM daycare d
    WHERE d.id = ANY(${bind(unitIds)})
"""
            )
        }
        .toList<PlacementDesktopDaycare>()
        .also { if (it.size < unitIds.size) throw NotFound() }
        .also { if (it.size > unitIds.size) throw IllegalStateException() }

fun Database.Read.getPlacementDesktopDaycareWithoutOccupancies(unitId: DaycareId) =
    getPlacementDesktopDaycaresWithoutOccupancies(setOf(unitId)).first()
