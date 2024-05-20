// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare.controllers

import fi.espoo.evaka.application.ApplicationType
import fi.espoo.evaka.daycare.CareType
import fi.espoo.evaka.daycare.UnitStub
import fi.espoo.evaka.daycare.domain.Language
import fi.espoo.evaka.daycare.domain.ProviderType
import fi.espoo.evaka.daycare.getAllApplicableUnits
import fi.espoo.evaka.daycare.getApplicationUnits
import fi.espoo.evaka.shared.AreaId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.Coordinate
import fi.espoo.evaka.shared.domain.DateRange
import java.time.LocalDate
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class LocationController {
    @GetMapping(
        value =
            [
                "/units", // deprecated
                "/public/units", // deprecated
                "/employee/units",
            ]
    )
    fun getApplicationUnits(
        db: Database,
        user: AuthenticatedUser.Employee,
        @RequestParam type: ApplicationUnitType,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate,
        @RequestParam shiftCare: Boolean?
    ): List<PublicUnit> {
        return db.connect { dbc ->
            dbc.read { it.getApplicationUnits(type, date, shiftCare, onlyApplicable = false) }
        }
    }

    @GetMapping(
        path =
            [
                "/public/units/{applicationType}", // deprecated
                "/employee/public/units/{applicationType}"
            ]
    )
    fun getAllApplicableUnits(
        db: Database,
        @PathVariable applicationType: ApplicationType
    ): List<PublicUnit> {
        return db.connect { dbc -> dbc.read { it.getAllApplicableUnits(applicationType) } }
    }

    @GetMapping(
        "/areas", // deprecated
        "/employee/areas"
    )
    fun getAreas(db: Database, user: AuthenticatedUser.Employee): List<AreaJSON> {
        return db.connect { dbc ->
            dbc.read {
                it.createQuery { sql("SELECT id, name, short_name FROM care_area") }.toList()
            }
        }
    }

    @GetMapping(
        "/filters/units", // deprecated
        "/employee/filters/units"
    )
    fun getUnits(
        db: Database,
        user: AuthenticatedUser.Employee,
        @RequestParam type: UnitTypeFilter,
        @RequestParam areaIds: List<AreaId>?,
        @RequestParam from: LocalDate?,
    ): List<UnitStub> {
        return db.connect { dbc -> dbc.read { it.getUnits(areaIds, type, from) } }
            .sortedBy { it.name.lowercase() }
    }
}

enum class ApplicationUnitType {
    CLUB,
    DAYCARE,
    PRESCHOOL,
    PREPARATORY
}

data class PublicUnit(
    val id: DaycareId,
    val name: String,
    val type: Set<CareType>,
    val providerType: ProviderType,
    val language: Language,
    val streetAddress: String,
    val postalCode: String,
    val postOffice: String,
    val phone: String?,
    val email: String?,
    val url: String?,
    val location: Coordinate?,
    val ghostUnit: Boolean?,
    val roundTheClock: Boolean,
    val daycareApplyPeriod: DateRange?,
    val preschoolApplyPeriod: DateRange?,
    val clubApplyPeriod: DateRange?
)

data class AreaJSON(val id: AreaId, val name: String, val shortName: String)

enum class UnitTypeFilter {
    ALL,
    CLUB,
    DAYCARE,
    PRESCHOOL
}

private fun Database.Read.getUnits(
    areaIds: List<AreaId>?,
    type: UnitTypeFilter,
    from: LocalDate?
): List<UnitStub> {
    val areaIdsParam = areaIds?.takeIf { it.isNotEmpty() }
    return createQuery {
            sql(
                """
SELECT unit.id, unit.name, unit.type as care_types
FROM daycare unit
JOIN care_area area ON unit.care_area_id = area.id
WHERE (${bind(areaIdsParam)}::uuid[] IS NULL OR area.id = ANY(${bind(areaIdsParam)}))
AND (${bind(type)} = 'ALL' OR
    (${bind(type)} = 'CLUB' AND 'CLUB' = ANY(unit.type)) OR
    (${bind(type)} = 'DAYCARE' AND '{CENTRE, FAMILY, GROUP_FAMILY}' && unit.type) OR
    (${bind(type)} = 'PRESCHOOL' AND '{PRESCHOOL, PREPARATORY_EDUCATION}' && unit.type)
)
AND (${bind(from)}::date IS NULL OR daterange(unit.opening_date, unit.closing_date, '[]') && daterange(${bind(from)}, NULL))
    """
            )
        }
        .toList()
}
