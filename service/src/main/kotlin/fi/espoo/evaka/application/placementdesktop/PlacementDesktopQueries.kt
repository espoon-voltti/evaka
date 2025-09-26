// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.application.placementdesktop

import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.NotFound
import org.jdbi.v3.json.Json

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

data class PlacementDesktopDaycare(
    val id: DaycareId,
    val name: String,
    @Json val placementDrafts: List<PlacementDraft>,
)

data class PlacementDraft(
    val applicationId: ApplicationId,
    val unitId: DaycareId,
    val childId: String,
    val childName: String,
)

fun Database.Read.getPlacementDesktopDaycares(unitIds: Set<DaycareId>) =
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
                'childName', c.first_name || ' ' || c.last_name
            ))
            FROM placement_draft pd
            JOIN application a ON a.id = pd.application_id
            JOIN person c ON c.id = a.child_id
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

fun Database.Read.getPlacementDesktopDaycare(unitId: DaycareId) =
    getPlacementDesktopDaycares(setOf(unitId)).first()
