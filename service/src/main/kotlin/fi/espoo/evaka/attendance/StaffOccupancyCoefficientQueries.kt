// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.attendance

import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.StaffOccupancyCoefficientId
import fi.espoo.evaka.shared.db.Database
import java.math.BigDecimal

fun Database.Read.getOccupancyCoefficientForEmployeeInUnit(
    employeeId: EmployeeId,
    unitId: DaycareId
): BigDecimal? =
    @Suppress("DEPRECATION")
    createQuery(
            """
SELECT coefficient
FROM staff_occupancy_coefficient
WHERE daycare_id = :unitId AND employee_id = :employeeId
        """
                .trimIndent()
        )
        .bind("unitId", unitId)
        .bind("employeeId", employeeId)
        .exactlyOneOrNull<BigDecimal>()

fun Database.Read.getOccupancyCoefficientForEmployee(
    employeeId: EmployeeId,
    groupId: GroupId
): BigDecimal? =
    @Suppress("DEPRECATION")
    createQuery(
            """
SELECT soc.coefficient
FROM staff_occupancy_coefficient soc
JOIN daycare_group grp ON soc.daycare_id = grp.daycare_id AND grp.id = :groupId
WHERE soc.employee_id = :employeeId
        """
                .trimIndent()
        )
        .bind("employeeId", employeeId)
        .bind("groupId", groupId)
        .exactlyOneOrNull<BigDecimal>()

fun Database.Read.getOccupancyCoefficientsByUnit(
    unitId: DaycareId
): List<StaffOccupancyCoefficient> =
    @Suppress("DEPRECATION")
    createQuery(
            """
SELECT soc.id, soc.coefficient, emp.id AS employeeId, emp.first_name, emp.last_name
FROM staff_occupancy_coefficient soc
JOIN employee emp ON soc.employee_id = emp.id
WHERE soc.daycare_id = :unitId
        """
                .trimIndent()
        )
        .bind("unitId", unitId)
        .toList<StaffOccupancyCoefficient>()

fun Database.Transaction.upsertOccupancyCoefficient(
    params: OccupancyCoefficientUpsert
): StaffOccupancyCoefficientId =
    @Suppress("DEPRECATION")
    createUpdate(
            """
INSERT INTO staff_occupancy_coefficient (daycare_id, employee_id, coefficient)
VALUES (:unitId, :employeeId, :coefficient)
ON CONFLICT (daycare_id, employee_id) DO UPDATE SET coefficient = EXCLUDED.coefficient
RETURNING id
        """
                .trimIndent()
        )
        .bindKotlin(params)
        .executeAndReturnGeneratedKeys()
        .exactlyOne<StaffOccupancyCoefficientId>()
