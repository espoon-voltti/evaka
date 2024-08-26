// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.security

import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.MobileDeviceId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.QuerySql
import fi.espoo.evaka.shared.domain.HelsinkiDateTime

data class ChildAclConfig(
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

    /**
     * Returns a list of employee ACL queries based on this configuration.
     *
     * The queries return (child_id, unit_id, role) rows for the given user.
     */
    fun aclQueries(user: AuthenticatedUser.Employee, now: HelsinkiDateTime) =
        listOfNotNull(
            if (this.placement) employeeChildAclViaPlacement(user.id, now) else null,
            if (this.backupCare) employeeChildAclViaBackupCare(user.id, now) else null,
            if (this.application) employeeChildAclViaApplication(user.id) else null,
        )

    /**
     * Returns a list of mobile device ACL queries based on this configuration.
     *
     * The queries return (child_id) rows for the given mobile device.
     */
    fun aclQueries(user: AuthenticatedUser.MobileDevice, now: HelsinkiDateTime) =
        listOfNotNull(
            if (this.placement) mobileChildAclViaPlacement(user.id, now) else null,
            if (this.backupCare) mobileChildAclViaBackupCare(user.id, now) else null,
            if (this.application) mobileChildAclViaApplication(user.id) else null,
        )
}

fun employeeChildAclViaPlacement(employee: EmployeeId, now: HelsinkiDateTime) = QuerySql {
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

fun employeeChildAclViaBackupCare(employee: EmployeeId, now: HelsinkiDateTime) = QuerySql {
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

fun employeeChildAclViaApplication(employee: EmployeeId) = QuerySql {
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

fun mobileChildAclViaPlacement(mobileDevice: MobileDeviceId, now: HelsinkiDateTime) = QuerySql {
    sql(
        """
SELECT pl.child_id
FROM placement pl
WHERE ${bind(now.toLocalDate())} < pl.end_date + INTERVAL '1 month'
AND EXISTS (
    SELECT FROM mobile_device md
    LEFT JOIN daycare_acl acl ON md.employee_id = acl.employee_id
    WHERE md.id = ${bind(mobileDevice)} AND (md.unit_id = pl.unit_id OR acl.daycare_id = pl.unit_id)
)
"""
    )
}

fun mobileChildAclViaBackupCare(mobileDevice: MobileDeviceId, now: HelsinkiDateTime) = QuerySql {
    sql(
        """
SELECT bc.child_id
FROM backup_care bc
WHERE ${bind(now.toLocalDate())} < bc.end_date + INTERVAL '1 month'
AND EXISTS (
    SELECT FROM mobile_device md
    LEFT JOIN daycare_acl acl ON md.employee_id = acl.employee_id
    WHERE md.id = ${bind(mobileDevice)} AND (md.unit_id = bc.unit_id OR acl.daycare_id = bc.unit_id)
)
"""
    )
}

fun mobileChildAclViaApplication(mobileDevice: MobileDeviceId) = QuerySql {
    sql(
        """
SELECT a.child_id
FROM placement_plan pp
JOIN application a ON pp.application_id = a.id
WHERE a.status = ANY ('{SENT,WAITING_PLACEMENT,WAITING_CONFIRMATION,WAITING_DECISION,WAITING_MAILING,WAITING_UNIT_CONFIRMATION}'::application_status_type[])
AND EXISTS (
    SELECT FROM mobile_device md
    LEFT JOIN daycare_acl acl ON md.employee_id = acl.employee_id
    WHERE md.id = ${bind(mobileDevice)} AND (md.unit_id = pp.unit_id OR acl.daycare_id = pp.unit_id)
)
"""
    )
}
