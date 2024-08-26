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
    unitId: DaycareId,
): BigDecimal? =
    createQuery {
            sql(
                """
SELECT coefficient
FROM staff_occupancy_coefficient
WHERE daycare_id = ${bind(unitId)} AND employee_id = ${bind(employeeId)}
"""
            )
        }
        .exactlyOneOrNull<BigDecimal>()

fun Database.Read.getOccupancyCoefficientForEmployee(
    employeeId: EmployeeId,
    groupId: GroupId,
): BigDecimal? =
    createQuery {
            sql(
                """
SELECT soc.coefficient
FROM staff_occupancy_coefficient soc
JOIN daycare_group grp ON soc.daycare_id = grp.daycare_id AND grp.id = ${bind(groupId)}
WHERE soc.employee_id = ${bind(employeeId)}
"""
            )
        }
        .exactlyOneOrNull<BigDecimal>()

fun Database.Read.getOccupancyCoefficientsByUnit(
    unitId: DaycareId
): List<StaffOccupancyCoefficient> =
    createQuery {
            sql(
                """
SELECT soc.id, soc.coefficient, emp.id AS employeeId, emp.first_name, emp.last_name
FROM staff_occupancy_coefficient soc
JOIN employee emp ON soc.employee_id = emp.id
WHERE soc.daycare_id = ${bind(unitId)}
"""
            )
        }
        .toList<StaffOccupancyCoefficient>()

fun Database.Transaction.upsertOccupancyCoefficient(
    params: OccupancyCoefficientUpsert
): StaffOccupancyCoefficientId =
    createUpdate {
            sql(
                """
INSERT INTO staff_occupancy_coefficient (daycare_id, employee_id, coefficient)
VALUES (${bind(params.unitId)}, ${bind(params.employeeId)}, ${bind(params.coefficient)})
ON CONFLICT (daycare_id, employee_id) DO UPDATE SET coefficient = EXCLUDED.coefficient
RETURNING id
"""
            )
        }
        .executeAndReturnGeneratedKeys()
        .exactlyOne<StaffOccupancyCoefficientId>()
