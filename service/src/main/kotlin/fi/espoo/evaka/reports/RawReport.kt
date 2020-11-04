// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.Audit
import fi.espoo.evaka.daycare.controllers.utils.ok
import fi.espoo.evaka.daycare.service.AbsenceType
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.config.Roles
import fi.espoo.evaka.shared.db.getEnum
import fi.espoo.evaka.shared.db.getNullableUUID
import fi.espoo.evaka.shared.db.getUUID
import fi.espoo.evaka.shared.domain.BadRequest
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.UUID

@RestController
class RawReportController(private val jdbc: NamedParameterJdbcTemplate) {
    @GetMapping("/reports/raw")
    fun getRawReport(
        user: AuthenticatedUser,
        @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate,
        @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate
    ): ResponseEntity<List<RawReportRow>> {
        Audit.RawReportRead.log()
        user.requireOneOfRoles(Roles.DIRECTOR, Roles.ADMIN)
        if (to.isBefore(from)) throw BadRequest("Inverted time range")
        if (to.isAfter(from.plusDays(7))) throw BadRequest("Time range too long")

        return getRawRows(jdbc, from, to).let(::ok)
    }
}

fun getRawRows(jdbc: NamedParameterJdbcTemplate, from: LocalDate, to: LocalDate): List<RawReportRow> {
    // language=sql
    val sql =
        """
            WITH data AS (
                SELECT
                    t::date as day,
                    p.id as child_id,
                    p.date_of_birth,
                    date_part('year', p.date_of_birth) as birth_year,
                    date_part('year', age(t::date, p.date_of_birth)) as age,
                    p.language,
            
                    pl.type as placement_type,
            
                    pl.unit_id,
                    u.name as unit_name,
                    ca.name AS care_area,
                    u.type as unit_type,
                    u.provider_type as unit_provider_type,
            
                    dgp.daycare_group_id,
                    dg.name as group_name,
                    dc.amount as caretakers_planned,
                    sa.count as caretakers_realized,
            
                    bc.unit_id as backup_unit_id,
                    bc.group_id as backup_group_id,
            
                    sn IS NOT NULL AS has_service_need,
                    sn.part_day,
                    sn.part_week,
                    sn.shift_care,
                    sn.hours_per_week,
                    an IS NOT NULL as has_assistance_need,
                    coalesce(an.capacity_factor, 1.0) as capacity_factor,
                    (CASE
                         WHEN date_part('month', t) <= 7 THEN date_part('year', t) - 1
                         ELSE date_part('year', t)
                    END) AS term_start_year,
                    u.type && array['FAMILY', 'GROUP_FAMILY']::care_types[] AS is_family_unit,
                    ab1.absence_type as absence_paid,
                    ab2.absence_type as absence_free
                FROM generate_series(:start_date, :end_date, '1 day') t
                JOIN placement pl ON daterange(pl.start_date, pl.end_date, '[]') @> t::date
                JOIN daycare u on u.id = pl.unit_id
                JOIN care_area ca ON ca.id = u.care_area_id
                JOIN person p ON p.id = pl.child_id
                LEFT JOIN daycare_group_placement dgp on pl.id = dgp.daycare_placement_id AND daterange(dgp.start_date, dgp.end_date, '[]') @> t::date
                LEFT JOIN daycare_group dg on dg.id = dgp.daycare_group_id
                LEFT JOIN daycare_caretaker dc on dg.id = dc.group_id AND daterange(dc.start_date, dc.end_date, '[]') @> t::date
                LEFT JOIN staff_attendance sa on dg.id = sa.group_id AND sa.date = t::date
                LEFT JOIN backup_care bc on bc.child_id = p.id AND daterange(bc.start_date, bc.end_date, '[]') @> t::date
                LEFT JOIN daycare bcu on bc.unit_id = bcu.id
                LEFT JOIN service_need sn on sn.child_id = p.id AND daterange(sn.start_date, sn.end_date, '[]') @> t::date
                LEFT JOIN assistance_need an on an.child_id = p.id AND daterange(an.start_date, an.end_date, '[]') @> t::date
                LEFT JOIN absence ab1 on ab1.child_id = p.id and ab1.date = t::date and ab1.absence_type != 'PRESENCE' AND ab1.care_type IN ('DAYCARE', 'PRESCHOOL_DAYCARE')
                LEFT JOIN absence ab2 on ab2.child_id = p.id and ab2.date = t::date and ab2.absence_type != 'PRESENCE' AND ab2.care_type NOT IN ('DAYCARE', 'PRESCHOOL_DAYCARE')
                WHERE date_part('isodow', t) = ANY(u.operation_days) OR date_part('isodow', t) = ANY(bcu.operation_days)
            )
            SELECT
                day,
                child_id,
                date_of_birth,
                age,
                language,
            
                placement_type,
            
                unit_id,
                unit_name,
                care_area,
                unit_type,
                unit_provider_type,
            
                daycare_group_id,
                group_name,
                caretakers_planned,
                caretakers_realized,
            
                backup_unit_id,
                backup_group_id,
            
                has_service_need,
                part_day,
                part_week,
                shift_care,
                hours_per_week,
            
                has_assistance_need,
                capacity_factor,
                coalesce(capacity_factor, 1.0) * (CASE
                    WHEN age IS NULL THEN 0.0
                    WHEN placement_type IS NULL THEN 0.0
                    WHEN is_family_unit THEN 1.75
                    WHEN age < 3 THEN 1.75
                    WHEN placement_type = 'DAYCARE_PART_TIME' THEN 0.54
                    WHEN placement_type = 'PRESCHOOL' THEN 0.5
                    WHEN placement_type = 'PREPARATORY' THEN 0.5
                    WHEN placement_type = 'DAYCARE_PART_TIME' AND (term_start_year - birth_year) = 5 AND hours_per_week IS NULL THEN 0.5
                    WHEN placement_type IN ('DAYCARE', 'DAYCARE_PART_TIME') AND (term_start_year - birth_year) = 5 AND hours_per_week <= 20 THEN 0.5
                    WHEN placement_type = 'PRESCHOOL_DAYCARE' AND hours_per_week <= 20.0 THEN 0.5
                    WHEN placement_type = 'PRESCHOOL_DAYCARE' AND hours_per_week >= 45.0 THEN 1.5
                    WHEN placement_type = 'PREPARATORY_DAYCARE' AND hours_per_week <= 25.0 THEN 0.5
                    WHEN placement_type = 'PREPARATORY_DAYCARE' AND hours_per_week >= 50.0 THEN 1.5
                    ELSE 1.0
                END) AS capacity,
            
                absence_paid,
                absence_free
            FROM data
            order by child_id, day;
        """.trimIndent()

    @Suppress("UNCHECKED_CAST")
    return jdbc.query(
        sql,
        mapOf(
            "start_date" to from,
            "end_date" to to
        )
    ) { rs, _ ->
        RawReportRow(
            day = rs.getDate("day").toLocalDate(),
            childId = rs.getUUID("child_id"),
            dateOfBirth = rs.getDate("date_of_birth").toLocalDate(),
            age = rs.getInt("age"),
            language = rs.getString("language"),
            placementType = rs.getEnum("placement_type"),
            unitId = rs.getUUID("unit_id"),
            unitName = rs.getString("unit_name"),
            careArea = rs.getString("care_area"),
            unitType = (rs.getArray("unit_type").array as Array<out Any>).map { it.toString() }.toSet().let(::getPrimaryUnitType),
            unitProviderType = rs.getString("unit_provider_type"),
            daycareGroupId = rs.getNullableUUID("daycare_group_id"),
            groupName = rs.getString("group_name"),
            caretakersPlanned = rs.getBigDecimal("caretakers_planned")?.toDouble(),
            caretakersRealized = rs.getBigDecimal("caretakers_realized")?.toDouble(),
            backupUnitId = rs.getNullableUUID("backup_unit_id"),
            backupGroupId = rs.getNullableUUID("backup_group_id"),
            hasServiceNeed = rs.getBoolean("has_service_need"),
            partDay = rs.getBoolean("part_day"),
            partWeek = rs.getBoolean("part_week"),
            shiftCare = rs.getBoolean("shift_care"),
            hoursPerWeek = rs.getDouble("hours_per_week"),
            hasAssistanceNeed = rs.getBoolean("has_assistance_need"),
            capacityFactor = rs.getBigDecimal("capacity_factor").toDouble(),
            capacity = rs.getBigDecimal("capacity").toDouble(),
            absencePaid = rs.getString("absence_paid")?.let { AbsenceType.valueOf(it) },
            absenceFree = rs.getString("absence_free")?.let { AbsenceType.valueOf(it) }
        )
    }
}

data class RawReportRow(
    val day: LocalDate,
    val childId: UUID,
    val dateOfBirth: LocalDate,
    val age: Int,
    val language: String?,
    val placementType: PlacementType,
    val unitId: UUID,
    val unitName: String,
    val careArea: String,
    val unitType: UnitType?,
    val unitProviderType: String,
    val daycareGroupId: UUID?,
    val groupName: String?,
    val caretakersPlanned: Double?,
    val caretakersRealized: Double?,
    val backupUnitId: UUID?,
    val backupGroupId: UUID?,
    val hasServiceNeed: Boolean,
    val partDay: Boolean?,
    val partWeek: Boolean?,
    val shiftCare: Boolean?,
    val hoursPerWeek: Double?,
    val hasAssistanceNeed: Boolean,
    val capacityFactor: Double,
    val capacity: Double,
    val absencePaid: AbsenceType?,
    val absenceFree: AbsenceType?
)
