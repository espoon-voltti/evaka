// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare.service

import fi.espoo.evaka.daycare.CareArea
import fi.espoo.evaka.daycare.Location
import fi.espoo.evaka.daycare.UnitStub
import fi.espoo.evaka.shared.db.bindNullable
import fi.espoo.evaka.shared.db.mapColumn
import fi.espoo.evaka.shared.db.withSpringHandle
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.kotlin.mapTo
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID
import javax.sql.DataSource

@Service
class LocationService(private val dataSource: DataSource) {
    @Transactional
    fun getAreas() = withSpringHandle(dataSource) { it.getAreas() }

    @Transactional
    fun getDaycares(areaShortNames: List<String>) = withSpringHandle(dataSource) { it.getDaycares(areaShortNames) }

    @Transactional
    fun getClubs(areaShortNames: List<String>) = withSpringHandle(dataSource) { it.getClubs(areaShortNames) }
}

fun Handle.getAreas(): List<CareArea> = createQuery(
    // language=SQL
    """
SELECT
  ca.id AS care_area_id, ca.name AS care_area_name, ca.short_name AS care_area_short_name,
  u.id, u.name, u.street_address, u.location, u.phone, u.postal_code, u.post_office,
  u.mailing_street_address, u.mailing_po_box, u.mailing_postal_code, u.mailing_post_office,
  u.type, u.url, u.provider_type, u.language, u.can_apply_daycare, u.can_apply_preschool, u.can_apply_club
FROM care_area ca
LEFT JOIN daycare u ON ca.id = u.care_area_id
WHERE (u.can_apply_daycare OR u.can_apply_preschool OR u.can_apply_club)
AND daterange(u.opening_date, u.closing_date, '[]') @> current_date
    """.trimIndent()
)
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

fun Handle.getDaycares(areaShortNames: Collection<String>): List<UnitStub> = createQuery(
    // language=SQL
    """
SELECT daycare.id, daycare.name
FROM daycare
JOIN care_area ON care_area_id = care_area.id
WHERE :areaShortNames::text[] IS NULL OR care_area.short_name = ANY(:areaShortNames)
ORDER BY name
    """.trimIndent()
).bindNullable("areaShortNames", areaShortNames.toTypedArray().takeIf { it.isNotEmpty() })
    .mapTo<UnitStub>()
    .list()

fun Handle.getClubs(areaShortNames: Collection<String>): List<UnitStub> = createQuery(
    // language=SQL
    """
SELECT club_group.id, club_group.name
FROM club_group
JOIN care_area ON care_area_id = care_area.id
WHERE :areaShortNames::text[] IS NULL OR care_area.short_name = ANY(:areaShortNames)
ORDER BY name
    """.trimIndent()
).bindNullable("areaShortNames", areaShortNames.toTypedArray().takeIf { it.isNotEmpty() })
    .mapTo<UnitStub>()
    .list()
