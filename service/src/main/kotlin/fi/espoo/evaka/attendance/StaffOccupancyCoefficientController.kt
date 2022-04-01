// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.attendance

import fi.espoo.evaka.Audit
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/occupancy-coefficient")
class StaffOccupancyCoefficientController(
    private val ac: AccessControl
) {
    @GetMapping
    fun getOccupancyCoefficients(
        db: Database,
        user: AuthenticatedUser,
        @RequestParam unitId: DaycareId,
    ): List<StaffOccupancyCoefficient> {
        Audit.StaffOccupancyCoefficientRead.log(unitId)
        ac.requirePermissionFor(user, Action.Unit.READ_STAFF_OCCUPANCY_COEFFICIENTS, unitId)
        return db.connect { dbc -> dbc.read { it.getOccupancyCoefficientsByUnit(unitId) } }
    }

    @PostMapping
    fun upsertOccupancyCoefficient(
        db: Database,
        user: AuthenticatedUser,
        @RequestBody body: OccupancyCoefficientUpsert,
    ) {
        Audit.StaffOccupancyCoefficientUpsert.log(body.unitId, body.employeeId)
        ac.requirePermissionFor(user, Action.Unit.UPSERT_STAFF_OCCUPANCY_COEFFICIENTS, body.unitId)
        db.connect { dbc -> dbc.transaction { it.upsertOccupancyCoefficient(body) } }
    }
}
