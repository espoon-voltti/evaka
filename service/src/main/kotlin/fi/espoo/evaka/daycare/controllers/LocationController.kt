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
import fi.espoo.evaka.shared.db.bindNullable
import fi.espoo.evaka.shared.domain.Coordinate
import fi.espoo.evaka.shared.domain.DateRange
import org.jdbi.v3.core.kotlin.mapTo
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
class LocationController {
    @GetMapping("/units")
    fun getApplicationUnits(
        db: Database,
        user: AuthenticatedUser,
        @RequestParam type: ApplicationUnitType,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate,
        @RequestParam shiftCare: Boolean?
    ): List<PublicUnit> {
        return db.connect { dbc -> dbc.read { it.getApplicationUnits(type, date, shiftCare, onlyApplicable = user is AuthenticatedUser.Citizen) } }
    }

    @GetMapping("/public/units/{applicationType}")
    fun getAllApplicableUnits(
        db: Database,
        @PathVariable applicationType: ApplicationType
    ): List<PublicUnit> {
        return db.connect { dbc -> dbc.read { it.getAllApplicableUnits(applicationType) } }
    }

    @GetMapping("/areas")
    fun getAreas(db: Database, user: AuthenticatedUser): List<AreaJSON> {
        return db.connect { dbc ->
            dbc.read {
                it.createQuery("SELECT id, name, short_name FROM care_area")
                    .mapTo<AreaJSON>()
                    .toList()
            }
        }
    }

    @GetMapping("/filters/units")
    fun getUnits(db: Database, @RequestParam type: UnitTypeFilter, @RequestParam area: String?): List<UnitStub> {
        val areas = area?.split(",") ?: listOf()
        return db.connect { dbc -> dbc.read { it.getUnits(areas, type) } }
    }
}

enum class ApplicationUnitType {
    CLUB, DAYCARE, PRESCHOOL, PREPARATORY;
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

data class AreaJSON(
    val id: AreaId,
    val name: String,
    val shortName: String
)

enum class UnitTypeFilter {
    ALL, CLUB, DAYCARE, PRESCHOOL
}

fun Database.Read.getUnits(areaShortNames: Collection<String>, type: UnitTypeFilter): List<UnitStub> = createQuery(
    // language=SQL
    """
SELECT unit.id, unit.name
FROM daycare unit
JOIN care_area area ON unit.care_area_id = area.id
WHERE (:areaShortNames::text[] IS NULL OR area.short_name = ANY(:areaShortNames))
AND (:type = 'ALL' OR
    (:type = 'CLUB' AND 'CLUB' = ANY(unit.type)) OR
    (:type = 'DAYCARE' AND '{CENTRE, FAMILY, GROUP_FAMILY}' && unit.type) OR
    (:type = 'PRESCHOOL' AND '{PRESCHOOL, PREPARATORY_EDUCATION}' && unit.type)
)
ORDER BY name
    """.trimIndent()
).bindNullable("areaShortNames", areaShortNames.toTypedArray().takeIf { it.isNotEmpty() })
    .bind("type", type)
    .mapTo<UnitStub>()
    .list()
