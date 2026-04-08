// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.attendance

import evaka.core.shared.DaycareId
import evaka.core.shared.EmployeeId
import evaka.core.shared.StaffOccupancyCoefficientId
import java.math.BigDecimal

val occupancyCoefficientSeven = BigDecimal("7.0")
val occupancyCoefficientZero = BigDecimal.ZERO

data class StaffOccupancyCoefficient(
    val id: StaffOccupancyCoefficientId,
    val employeeId: EmployeeId,
    val firstName: String,
    val lastName: String,
    val coefficient: BigDecimal,
)

data class OccupancyCoefficientUpsert(
    val unitId: DaycareId,
    val employeeId: EmployeeId,
    val coefficient: BigDecimal,
)
