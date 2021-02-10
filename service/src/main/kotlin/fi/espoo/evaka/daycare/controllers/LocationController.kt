// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare.controllers

import fi.espoo.evaka.daycare.CareArea
import fi.espoo.evaka.daycare.CareType
import fi.espoo.evaka.daycare.Location
import fi.espoo.evaka.daycare.MailingAddress
import fi.espoo.evaka.daycare.UnitStub
import fi.espoo.evaka.daycare.VisitingAddress
import fi.espoo.evaka.daycare.domain.Language
import fi.espoo.evaka.daycare.domain.ProviderType
import fi.espoo.evaka.daycare.getAllApplicableUnits
import fi.espoo.evaka.daycare.getApplicationUnits
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.bindNullable
import fi.espoo.evaka.shared.db.mapColumn
import fi.espoo.evaka.shared.domain.Coordinate
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.utils.zoneId
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.kotlin.mapTo
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
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
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate
    ): ResponseEntity<List<PublicUnit>> {
        val units = db.read { it.handle.getApplicationUnits(type, date, onlyApplicable = user.isEndUser()) }
        return ResponseEntity.ok(units)
    }

    @GetMapping("/public/units/all")
    fun getAllApplicableUnits(
        db: Database.Connection
    ): ResponseEntity<List<PublicUnit>> {
        return db
            .read { it.getAllApplicableUnits() }
            .let { ResponseEntity.ok(it) }
    }

    // Units by areas, only including units that can be applied to
    @GetMapping("/public/areas")
    fun getEnduserUnitsByArea(db: Database.Connection): ResponseEntity<Collection<CareAreaResponseJSON>> {
        val future = DateRange(LocalDate.now(zoneId), null)
        val areas = db.read { it.handle.getAreas() }
            .map { area: CareArea ->
                CareArea(
                    area.id,
                    area.name,
                    area.shortName,
                    area.locations.filter {
                        (it.daycareApplyPeriod != null && it.daycareApplyPeriod.overlaps(future)) ||
                            (it.preschoolApplyPeriod != null && it.preschoolApplyPeriod.overlaps(future)) ||
                            (it.clubApplyPeriod != null && it.clubApplyPeriod.overlaps(future))
                    }
                )
            }
            .toSet()

        return ResponseEntity.ok(toCareAreaResponses(areas))
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
        val units = db.read { it.handle.getUnits(areas, type) }
        return ResponseEntity.ok(units)
    }

    companion object DomainMapping {
        private fun toCareAreaResponses(areas: Collection<CareArea>) = areas.map { area ->
            CareAreaResponseJSON(
                area.id,
                area.name,
                area.shortName,
                area.locations.map(::toLocationResponseJSON)
            )
        }

        private fun toLocationResponseJSON(location: Location) =
            LocationResponseJSON(
                id = location.id,
                name = location.name,
                address = location.visitingAddress.streetAddress,
                location = location.location,
                phone = location.phone,
                postalCode = location.visitingAddress.postalCode,
                POBox = location.mailingAddress.poBox,
                type = location.type,
                care_area_id = location.care_area_id,
                url = location.url,
                provider_type = location.provider_type,
                language = location.language,
                visitingAddress = location.visitingAddress,
                mailingAddress = location.mailingAddress,
                daycareApplyPeriod = location.daycareApplyPeriod,
                preschoolApplyPeriod = location.preschoolApplyPeriod,
                clubApplyPeriod = location.clubApplyPeriod
            )
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
    val roundTheClock: Boolean
)

data class CareAreaResponseJSON(
    val id: UUID,
    val name: String,
    val shortName: String,
    val daycares: List<LocationResponseJSON>
)

data class LocationResponseJSON(
    val id: UUID,
    val name: String,

    @Deprecated("Use separate mailing/vising addresses that match data")
    val address: String,

    val location: Coordinate?, // @Todo remove nullability when data is complete
    val phone: String?, // @Todo remove nullability when data is complete

    @Deprecated("Use separate mailing/vising addresses that match the data")
    val postalCode: String?, // @Todo remove nullability when data is complete
    @Deprecated("Use separate mailing/vising addresses that match the data")
    val POBox: String?, // @Todo remove nullability when data is complete

    val type: Set<CareType>,
    val care_area_id: UUID,
    val url: String?,
    val provider_type: ProviderType?,
    val language: Language?,
    val visitingAddress: VisitingAddress,
    val mailingAddress: MailingAddress,
    val daycareApplyPeriod: DateRange?,
    val preschoolApplyPeriod: DateRange?,
    val clubApplyPeriod: DateRange?
)

data class AreaJSON(
    val id: UUID,
    val name: String,
    val shortName: String
)

fun Handle.getAreas(): List<CareArea> = createQuery(
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
    .reduceRows(mutableMapOf<UUID, Pair<CareArea, MutableList<Location>>>()) { map, row ->
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

fun Handle.getUnits(areaShortNames: Collection<String>, type: UnitTypeFilter): List<UnitStub> = createQuery(
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
