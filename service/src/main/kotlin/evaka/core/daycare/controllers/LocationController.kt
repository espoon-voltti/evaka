// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.daycare.controllers

import evaka.core.daycare.CareType
import evaka.core.daycare.UnitStub
import evaka.core.daycare.domain.Language
import evaka.core.daycare.domain.ProviderType
import evaka.core.daycare.getApplicationUnits
import evaka.core.shared.AreaId
import evaka.core.shared.DaycareId
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.db.Database
import evaka.core.shared.db.Predicate
import evaka.core.shared.domain.Coordinate
import evaka.core.shared.domain.DateRange
import java.time.LocalDate
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class LocationController {
    @GetMapping("/employee/units")
    fun getApplicationUnits(
        db: Database,
        user: AuthenticatedUser.Employee,
        @RequestParam type: ApplicationUnitType,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate,
        @RequestParam shiftCare: Boolean?,
    ): List<PublicUnit> {
        return db.connect { dbc ->
            dbc.read { it.getApplicationUnits(type, date, shiftCare, onlyApplicable = false) }
        }
    }

    @GetMapping("/employee/areas")
    fun getAreas(db: Database, user: AuthenticatedUser.Employee): List<AreaJSON> {
        return db.connect { dbc ->
            dbc.read {
                it.createQuery { sql("SELECT id, name, short_name FROM care_area") }.toList()
            }
        }
    }

    @GetMapping("/employee/filters/units")
    fun getUnits(
        db: Database,
        user: AuthenticatedUser.Employee,
        @RequestParam type: Set<UnitTypeFilter>?,
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
    PREPARATORY,
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
    val providesShiftCare: Boolean,
    val daycareApplyPeriod: DateRange?,
    val preschoolApplyPeriod: DateRange?,
    val clubApplyPeriod: DateRange?,
)

data class AreaJSON(val id: AreaId, val name: String, val shortName: String)

enum class UnitTypeFilter(val careTypes: Set<CareType>) {
    CLUB(setOf(CareType.CLUB)),
    DAYCARE(setOf(CareType.CENTRE, CareType.FAMILY, CareType.GROUP_FAMILY)),
    PRESCHOOL(setOf(CareType.PRESCHOOL, CareType.PREPARATORY_EDUCATION)),
}

private fun Database.Read.getUnits(
    areaIds: List<AreaId>?,
    types: Set<UnitTypeFilter>?,
    from: LocalDate?,
): List<UnitStub> {
    val areaIdsParam = areaIds?.takeIf { it.isNotEmpty() }
    val careTypes = types?.flatMap { it.careTypes }
    val unitPredicates =
        Predicate.allNotNull(
            careTypes?.let { Predicate { table -> where("$table.type && ${bind(it)}") } },
            from?.let {
                Predicate { table ->
                    where(
                        "daterange($table.opening_date, $table.closing_date, '[]') && daterange(${bind(it)}, NULL)"
                    )
                }
            },
        )
    return createQuery {
            sql(
                """
SELECT unit.id, unit.name, unit.type as care_types, unit.closing_date
FROM daycare unit
JOIN care_area area ON unit.care_area_id = area.id
WHERE (${bind(areaIdsParam)}::uuid[] IS NULL OR area.id = ANY(${bind(areaIdsParam)}))
AND ${predicate(unitPredicates.forTable("unit"))}
    """
            )
        }
        .toList()
}
