// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare.controllers

import fi.espoo.evaka.application.ApplicationType
import fi.espoo.evaka.daycare.CareArea
import fi.espoo.evaka.daycare.CareType
import fi.espoo.evaka.daycare.Location
import fi.espoo.evaka.daycare.UnitStub
import fi.espoo.evaka.daycare.domain.Language
import fi.espoo.evaka.daycare.domain.ProviderType
import fi.espoo.evaka.daycare.getAllApplicableUnits
import fi.espoo.evaka.daycare.getApplicationUnits
import fi.espoo.evaka.shared.AreaId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.bindNullable
import fi.espoo.evaka.shared.db.mapColumn
import fi.espoo.evaka.shared.domain.Coordinate
import fi.espoo.evaka.shared.domain.DateRange
import org.jdbi.v3.core.kotlin.mapTo
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.UUID

@RestController
class LocationController {
    @GetMapping("/units")
    fun getApplicationUnits(
        db: Database.Connection,
        user: AuthenticatedUser,
        @RequestParam type: ApplicationUnitType,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate,
        @RequestParam shiftCare: Boolean?
    ): ResponseEntity<List<PublicUnit>> {
        val units = db.read { it.getApplicationUnits(type, date, shiftCare, onlyApplicable = user.isEndUser) }
        return ResponseEntity.ok(units)
    }

    @GetMapping("/public/units/{applicationType}")
    fun getAllApplicableUnits(
        db: Database.Connection,
        @PathVariable applicationType: ApplicationType
    ): ResponseEntity<List<PublicUnit>> {
        return db
            .read { it.getAllApplicableUnits(applicationType) }
            .let { ResponseEntity.ok(it) }
    }

    @GetMapping("/areas")
    fun getAreas(db: Database.Connection, user: AuthenticatedUser): ResponseEntity<Collection<AreaJSON>> {
        return db
            .read {
                it.createQuery("SELECT id, name, short_name FROM care_area")
                    .mapTo<AreaJSON>()
                    .toList()
            }
            .let { ResponseEntity.ok(it) }
    }

    @GetMapping("/filters/units")
    fun getUnits(db: Database.Connection, @RequestParam type: UnitTypeFilter, @RequestParam area: String?): ResponseEntity<List<UnitStub>> {
        val areas = area?.split(",") ?: listOf()
        val units = db.read { it.getUnits(areas, type) }
        return ResponseEntity.ok(units)
    }
}

enum class ApplicationUnitType {
    CLUB, DAYCARE, PRESCHOOL, PREPARATORY;
}

data class PublicUnit(
    val id: UUID,
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
    val id: UUID,
    val name: String,
    val shortName: String
)

fun Database.Read.getAreas(): List<CareArea> = createQuery(
    // language=sql
    """
SELECT
  ca.id AS care_area_id, ca.name AS care_area_name, ca.short_name AS care_area_short_name,
  u.id, u.name, u.street_address, u.location, u.phone, u.postal_code, u.post_office,
  u.mailing_street_address, u.mailing_po_box, u.mailing_postal_code, u.mailing_post_office,
  u.type, u.url, u.provider_type, u.language, u.daycare_apply_period, u.preschool_apply_period, u.club_apply_period
FROM care_area ca
LEFT JOIN daycare u ON ca.id = u.care_area_id
WHERE (
    (u.club_apply_period && daterange(:now, null, '[]')) OR
    (u.daycare_apply_period && daterange(:now, null, '[]')) OR
    (u.preschool_apply_period && daterange(:now, null, '[]'))
)
    """.trimIndent()
)
    .bind("now", LocalDate.now())
    .reduceRows(mutableMapOf<AreaId, Pair<CareArea, MutableList<Location>>>()) { map, row ->
        val (_, locations) = map.computeIfAbsent(row.mapColumn("care_area_id")) { id ->
            Pair(
                CareArea(
                    id = id,
                    name = row.mapColumn("care_area_name"),
                    shortName = row.mapColumn("care_area_short_name"),
                    locations = listOf()
                ),
                mutableListOf()
            )
        }
        row.mapColumn<UUID?>("id")?.let {
            locations.add(row.getRow(Location::class.java))
        }

        map
    }
    .values
    .map { (area, locations) ->
        area.copy(locations = locations.toList())
    }

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
