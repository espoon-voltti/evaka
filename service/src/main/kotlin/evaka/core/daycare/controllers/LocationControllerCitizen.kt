// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.daycare.controllers

import evaka.core.application.ApplicationType
import evaka.core.daycare.getAllApplicableUnits
import evaka.core.daycare.getApplicationUnits
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.db.Database
import java.time.LocalDate
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class LocationControllerCitizen {
    @GetMapping("/citizen/units")
    fun getApplicationUnits(
        db: Database,
        user: AuthenticatedUser.Citizen,
        @RequestParam type: ApplicationUnitType,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate,
        @RequestParam shiftCare: Boolean?,
    ): List<PublicUnit> {
        return db.connect { dbc ->
            dbc.read { it.getApplicationUnits(type, date, shiftCare, onlyApplicable = true) }
        }
    }

    @GetMapping("/citizen/public/units/{applicationType}")
    fun getAllApplicableUnits(
        db: Database,
        @PathVariable applicationType: ApplicationType,
    ): List<PublicUnit> {
        return db.connect { dbc -> dbc.read { it.getAllApplicableUnits(applicationType) } }
    }
}
