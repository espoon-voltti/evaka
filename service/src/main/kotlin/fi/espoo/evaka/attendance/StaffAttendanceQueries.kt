package fi.espoo.evaka.attendance

import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import org.jdbi.v3.core.kotlin.mapTo
import java.time.LocalTime

fun Database.Read.getStaffAttendances(unitId: DaycareId, now: HelsinkiDateTime): List<StaffMember> = createQuery(
    """
    SELECT DISTINCT 
        dacl.employee_id,
        e.first_name,
        e.last_name,
        att.employee_id AS attendance_employee_id,
        att.group_id AS attendance_group_id,
        att.arrived AS attendance_arrived,
        att.departed AS attendance_departed,
        (
            SELECT coalesce(array_agg(id), '{}'::UUID[])
            FROM daycare_group dg
            JOIN daycare_group_acl dgacl ON dg.id = dgacl.daycare_group_id
            WHERE dg.daycare_id = :unitId AND dgacl.employee_id = dacl.employee_id
        ) AS group_ids
    FROM daycare_acl dacl
    JOIN employee e on e.id = dacl.employee_id
    LEFT JOIN LATERAL (
        SELECT sa.employee_id, sa.arrived, sa.departed, sa.group_id
        FROM staff_attendance_2 sa
        WHERE sa.employee_id = dacl.employee_id AND tstzrange(sa.arrived, sa.departed) && tstzrange(:rangeStart, :rangeEnd)
        ORDER BY sa.arrived DESC 
        LIMIT 1
    ) att ON TRUE
    WHERE dacl.daycare_id = :unitId AND dacl.role IN ('STAFF', 'SPECIAL_EDUCATION_TEACHER')
    """.trimIndent()
)
    .bind("unitId", unitId)
    .bind("rangeStart", now.withTime(LocalTime.MIN))
    .bind("rangeEnd", now.withTime(LocalTime.MAX))
    .mapTo<StaffMember>()
    .list()

fun Database.Read.getExternalStaffAttendances(unitId: DaycareId): List<ExternalStaffMember> = createQuery(
    """
    SELECT sae.name, sae.group_id, sae.arrived
    FROM staff_attendance_externals sae
    JOIN daycare_group dg on sae.group_id = dg.id
    WHERE dg.daycare_id = :unitId AND departed IS NULL 
    """.trimIndent()
)
    .bind("unitId", unitId)
    .mapTo<ExternalStaffMember>()
    .list()

fun Database.Transaction.markStaffArrival(employeeId: EmployeeId, arrivalTime: HelsinkiDateTime) = createUpdate(
    """
    INSERT INTO staff_attendance_2 (employee_id, arrived, departed) VALUES (
        :employeeId, :arrived, NULL
    ) ON CONFLICT DO NOTHING 
    """.trimIndent()
)
    .bind("employeeId", employeeId)
    .bind("arrived", arrivalTime)
    .execute()

fun Database.Transaction.markStaffDeparture(employeeId: EmployeeId, departureTime: HelsinkiDateTime) = createUpdate(
    """
    UPDATE staff_attendance_2 
    SET departed = :departed
    WHERE employee_id = :employeeId AND departed IS NULL AND arrived < :departed
    """.trimIndent()
)
    .bind("employeeId", employeeId)
    .bind("departed", departureTime)
    .execute()
