// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.jamix

import fi.espoo.evaka.absence.AbsenceCategory
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.TimeRange
import java.time.LocalDate
import org.jdbi.v3.json.Json

data class JamixChildData(
    val childId: ChildId,
    val unitId: DaycareId,
    val placementType: PlacementType,
    val hasShiftCare: Boolean,
    val firstName: String,
    val lastName: String,
    @Json val reservations: List<TimeRange>,
    val absences: Set<AbsenceCategory>
)

fun Database.Read.getJamixCustomerNumbers(): Set<Int> =
    createQuery {
        sql(
            "SELECT DISTINCT jamix_customer_number FROM daycare_group WHERE jamix_customer_number IS NOT NULL"
        )
    }.toSet()

fun Database.Read.getJamixChildData(
    jamixCustomerNumber: Int,
    date: LocalDate
): List<JamixChildData> =
    createQuery {
        sql(
            """
SELECT
    rp.child_id,
    rp.unit_id,
    rp.placement_type,
    (sn.shift_care IS NOT NULL AND sn.shift_care != 'NONE') AS has_shift_care,
    p.first_name,
    p.last_name,
    coalesce((
        SELECT jsonb_agg(jsonb_build_object('start', ar.start_time, 'end', ar.end_time))
        FROM attendance_reservation ar
        JOIN evaka_user eu ON ar.created_by = eu.id
        WHERE
            ar.child_id = rp.child_id AND
            ar.date = ${bind(date)} AND
            -- Ignore NO_TIMES reservations
            ar.start_time IS NOT NULL AND ar.end_time IS NOT NULL
    ), '[]'::jsonb) AS reservations,
    coalesce((
        SELECT array_agg(a.category)
        FROM absence a
        WHERE a.child_id = rp.child_id AND a.date = ${bind(date)}
    ), '{}'::absence_category[]) AS absences
FROM realized_placement_one(${bind(date)}) rp
JOIN daycare_group dg ON dg.id = rp.group_id
JOIN person p ON p.id = rp.child_id
LEFT JOIN service_need sn ON sn.placement_id = rp.placement_id AND daterange(sn.start_date, sn.end_date, '[]') @> ${bind(date)}
WHERE dg.jamix_customer_number = ${bind(jamixCustomerNumber)}
                    """
        )
    }.toList()
