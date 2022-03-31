// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.attendance

import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.db.Database
import org.jdbi.v3.core.kotlin.bindKotlin
import org.jdbi.v3.core.kotlin.mapTo
import java.math.BigDecimal

fun Database.Read.getOccupancyCoefficientForEmployee(employeeId: EmployeeId, groupId: GroupId): BigDecimal? =
    createQuery(
        """
SELECT soc.coefficient
FROM staff_occupancy_coefficient soc
JOIN daycare_group grp ON soc.daycare_id = grp.daycare_id AND grp.id = :groupId
WHERE soc.employee_id = :employeeId
        """.trimIndent()
    )
        .bind("employeeId", employeeId)
        .bind("groupId", groupId)
        .mapTo<BigDecimal>()
        .firstOrNull()

fun Database.Read.getOccupancyCoefficientsByUnit(unitId: DaycareId): List<StaffOccupancyCoefficient> =
    createQuery(
        """
SELECT soc.id, soc.coefficient, emp.id AS employeeId, emp.first_name, emp.last_name
FROM staff_occupancy_coefficient soc
JOIN employee emp ON soc.employee_id = emp.id
WHERE soc.daycare_id = :unitId
        """.trimIndent()
    )
        .bind("unitId", unitId)
        .mapTo<StaffOccupancyCoefficient>()
        .list()

fun Database.Transaction.upsertOccupancyCoefficient(params: OccupancyCoefficientUpsert) =
    createUpdate(
        """
INSERT INTO staff_occupancy_coefficient (daycare_id, employee_id, coefficient)
VALUES (:unitId, :employeeId, :coefficient)
ON CONFLICT (daycare_id, employee_id) DO UPDATE SET coefficient = EXCLUDED.coefficient
        """.trimIndent()
    )
        .bindKotlin(params)
        .execute()
