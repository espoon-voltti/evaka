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
import java.time.LocalDate

fun Database.Transaction.upsertApplicationPlacementDraft(
    applicationId: ApplicationId,
    unitId: DaycareId,
    startDate: LocalDate?,
    now: HelsinkiDateTime,
    userId: EvakaUserId,
): LocalDate {
    val startDateWithDefault =
        startDate
            ?: createQuery {
                    sql(
                        """
        SELECT LEAST(
            a.document ->> 'preferredStartDate', 
            a.document ->> 'connectedDaycarePreferredStartDate'
        )::date as start_date
        FROM application a
        WHERE a.id = ${bind(applicationId)}
    """
                    )
                }
                .exactlyOneOrNull<LocalDate?>()
            ?: now.toLocalDate()

    createUpdate {
            sql(
                """
            INSERT INTO placement_draft (application_id, unit_id, start_date, created_at, created_by, modified_at, modified_by)
            VALUES (${bind(applicationId)}, ${bind(unitId)}, ${bind(startDateWithDefault)}, ${bind(now)}, ${bind(userId)}, ${bind(now)}, ${bind(userId)})
            ON CONFLICT (application_id) DO UPDATE SET
                unit_id = ${bind(unitId)},
                start_date = ${bind(startDateWithDefault)},
                modified_at = ${bind(now)},
                modified_by = ${bind(userId)}
        """
            )
        }
        .updateExactlyOne()

    return startDateWithDefault
}

fun Database.Transaction.deleteApplicationPlacementDraft(applicationId: ApplicationId) = execute {
    sql(
        """
            DELETE FROM placement_draft
            WHERE application_id = ${bind(applicationId)}
        """
    )
}

fun Database.Read.getPlacementDesktopDaycaresWithoutOccupancies(unitIds: Set<DaycareId>) =
    createQuery {
            sql(
                """
    SELECT 
        d.id,
        d.name,
        d.service_worker_note,
        coalesce((
            SELECT jsonb_agg(jsonb_build_object(
                'applicationId', pd.application_id, 
                'unitId', pd.unit_id,
                'startDate', pd.start_date,
                'childId', c.id,
                'childName', c.last_name || ' ' || c.first_name,
                'modifiedAt', pd.modified_at,
                'modifiedBy', jsonb_build_object(
                    'id', eu.id, 
                    'name', eu.name,
                    'type', eu.type
                ),
                'serviceWorkerNote', a.service_worker_note
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
