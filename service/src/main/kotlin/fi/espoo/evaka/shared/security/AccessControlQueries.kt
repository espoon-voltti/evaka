// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.security

import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.db.QuerySql
import fi.espoo.evaka.shared.domain.HelsinkiDateTime

data class EmployeeChildAclConfig(
    /** Enables access via direct placements */
    val placement: Boolean = true,
    /** Enables access via backup care placements */
    val backupCare: Boolean = true,
    /** Enables access via placement plans originating from applications */
    val application: Boolean = true,
) {
    init {
        require(placement || backupCare || application) {
            "At least one access mechanism must be enabled"
        }
    }
}

fun employeeChildAclViaPlacement(employee: EmployeeId, now: HelsinkiDateTime) =
    QuerySql.of<Any> {
        sql(
            """
SELECT pl.child_id, pl.unit_id, role
FROM placement pl
JOIN daycare_acl ON pl.unit_id = daycare_acl.daycare_id
WHERE ${bind(now.toLocalDate())} < pl.end_date + INTERVAL '1 month'
AND daycare_acl.employee_id = ${bind(employee)}
"""
        )
    }

fun employeeChildAclViaBackupCare(employee: EmployeeId, now: HelsinkiDateTime) =
    QuerySql.of<Any> {
        sql(
            """
SELECT bc.child_id, bc.unit_id, role
FROM backup_care bc
JOIN daycare_acl ON unit_id = daycare_acl.daycare_id
WHERE ${bind(now.toLocalDate())} < bc.end_date + INTERVAL '1 month'
AND daycare_acl.employee_id = ${bind(employee)}
"""
        )
    }

fun employeeChildAclViaApplication(employee: EmployeeId) =
    QuerySql.of<Any> {
        sql(
            """
SELECT a.child_id, pp.unit_id, role
FROM placement_plan pp
JOIN application a ON pp.application_id = a.id
JOIN daycare_acl ON pp.unit_id = daycare_acl.daycare_id
WHERE a.status = ANY ('{SENT,WAITING_PLACEMENT,WAITING_CONFIRMATION,WAITING_DECISION,WAITING_MAILING,WAITING_UNIT_CONFIRMATION}'::application_status_type[])
AND NOT (role = 'SPECIAL_EDUCATION_TEACHER' AND coalesce((a.document -> 'careDetails' ->> 'assistanceNeeded')::boolean, FALSE) IS FALSE)
AND daycare_acl.employee_id = ${bind(employee)}
"""
        )
    }
