// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.serviceneed

import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.PlacementId
import fi.espoo.evaka.shared.ServiceNeedId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.bindNullable
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.NotFound
import org.jdbi.v3.core.kotlin.mapTo
import java.time.LocalDate
import java.util.UUID

fun Database.Read.getServiceNeedsByChild(
    childId: UUID
): List<ServiceNeed> {
    // language=SQL
    val sql =
        """
        SELECT 
            sn.id, sn.placement_id, sn.start_date, sn.end_date, sn.shift_care, sn.updated,
            sno.id as option_id, sno.name as option_name, sno.updated as option_updated,
            sn.confirmed_by as confirmed_employee_id, e.first_name as confirmed_first_name, e.last_name as confirmed_last_name, sn.confirmed_at
        FROM service_need sn
        JOIN service_need_option sno on sno.id = sn.option_id
        JOIN placement pl ON pl.id = sn.placement_id
        JOIN employee e on e.id = sn.confirmed_by
        WHERE pl.child_id = :childId
        """.trimIndent()

    return createQuery(sql)
        .bind("childId", childId)
        .mapTo<ServiceNeed>()
        .toList()
}

fun Database.Read.getServiceNeedsByUnit(
    unitId: DaycareId,
    startDate: LocalDate?,
    endDate: LocalDate?
): List<ServiceNeed> {
    // language=SQL
    val sql =
        """
        SELECT 
            sn.id, sn.placement_id, sn.start_date, sn.end_date, sn.shift_care, sn.updated,
            sno.id as option_id, sno.name as option_name, sno.updated AS option_updated,
            sn.confirmed_by as confirmed_employee_id, e.first_name as confirmed_first_name, e.last_name as confirmed_last_name, sn.confirmed_at
        FROM service_need sn
        JOIN service_need_option sno on sno.id = sn.option_id
        JOIN placement pl ON pl.id = sn.placement_id
        JOIN employee e on e.id = sn.confirmed_by
        WHERE pl.unit_id = :unitId AND daterange(:start, :end, '[]') && daterange(sn.start_date, sn.end_date, '[]')
        """.trimIndent()

    return createQuery(sql)
        .bind("unitId", unitId)
        .bindNullable("start", startDate)
        .bindNullable("end", endDate)
        .mapTo<ServiceNeed>()
        .toList()
}

fun Database.Read.getServiceNeed(id: ServiceNeedId): ServiceNeed {
    // language=sql
    val sql = """
        SELECT 
            sn.id, sn.placement_id, sn.start_date, sn.end_date, sn.shift_care, sn.updated,
            sno.id as option_id, sno.name as option_name, sno.updated AS option_updated,
            sn.confirmed_by as confirmed_employee_id, e.first_name as confirmed_first_name, e.last_name as confirmed_last_name, sn.confirmed_at
        FROM service_need sn
        JOIN service_need_option sno on sn.option_id = sno.id
        JOIN employee e on e.id = sn.confirmed_by
        WHERE sn.id = :id
    """.trimIndent()

    return createQuery(sql)
        .bind("id", id)
        .mapTo<ServiceNeed>()
        .firstOrNull() ?: throw NotFound("Service need $id not found")
}

fun Database.Read.getServiceNeedChildRange(id: ServiceNeedId): ServiceNeedChildRange {
    // language=sql
    val sql = """
        SELECT p.child_id, daterange(sn.start_date, sn.end_date, '[]')
        FROM service_need sn
        JOIN placement p on sn.placement_id = p.id
        WHERE sn.id = :id
    """.trimIndent()

    return createQuery(sql)
        .bind("id", id)
        .mapTo<ServiceNeedChildRange>()
        .firstOrNull() ?: throw NotFound("Service need $id not found")
}

fun Database.Transaction.insertServiceNeed(
    placementId: PlacementId,
    startDate: LocalDate,
    endDate: LocalDate,
    optionId: UUID,
    shiftCare: Boolean,
    confirmedBy: UUID,
    confirmedAt: HelsinkiDateTime
): ServiceNeedId {
    // language=sql
    val sql = """
        INSERT INTO service_need (placement_id, start_date, end_date, option_id, shift_care, confirmed_by, confirmed_at) 
        VALUES (:placementId, :startDate, :endDate, :optionId, :shiftCare, :confirmedBy, :confirmedAt)
        RETURNING id;
    """.trimIndent()
    return createQuery(sql)
        .bind("placementId", placementId)
        .bind("startDate", startDate)
        .bind("endDate", endDate)
        .bind("optionId", optionId)
        .bind("shiftCare", shiftCare)
        .bind("confirmedBy", confirmedBy)
        .bind("confirmedAt", confirmedAt)
        .mapTo<ServiceNeedId>()
        .one()
}

fun Database.Transaction.updateServiceNeed(
    id: ServiceNeedId,
    startDate: LocalDate,
    endDate: LocalDate,
    optionId: UUID,
    shiftCare: Boolean,
    confirmedBy: UUID,
    confirmedAt: HelsinkiDateTime
) {
    // language=sql
    val sql = """
        UPDATE service_need
        SET start_date = :startDate, end_date = :endDate, option_id = :optionId, shift_care = :shiftCare, confirmed_by = :confirmedBy, confirmed_at = :confirmedAt
        WHERE id = :id
    """.trimIndent()

    createUpdate(sql)
        .bind("id", id)
        .bind("startDate", startDate)
        .bind("endDate", endDate)
        .bind("optionId", optionId)
        .bind("shiftCare", shiftCare)
        .bind("confirmedBy", confirmedBy)
        .bind("confirmedAt", confirmedAt)
        .execute()
}

fun Database.Transaction.deleteServiceNeed(
    id: ServiceNeedId
) {
    createUpdate("DELETE FROM service_need WHERE id = :id")
        .bind("id", id)
        .execute()
}

fun Database.Read.getOverlappingServiceNeeds(
    placementId: PlacementId,
    startDate: LocalDate,
    endDate: LocalDate,
    excluding: ServiceNeedId?
): List<ServiceNeed> {
    // language=sql
    val sql = """
        SELECT 
            sn.id, sn.placement_id, sn.start_date, sn.end_date, sn.shift_care, sn.updated,
            sno.id as option_id, sno.name as option_name, sno.updated as option_updated,
            sn.confirmed_by as confirmed_employee_id, e.first_name as confirmed_first_name, e.last_name as confirmed_last_name, sn.confirmed_at
        FROM service_need sn
        JOIN service_need_option sno on sn.option_id = sno.id
        JOIN employee e on e.id = sn.confirmed_by
        WHERE placement_id = :placementId AND daterange(sn.start_date, sn.end_date, '[]') && daterange(:startDate, :endDate, '[]')
    """.trimIndent()

    return createQuery(sql)
        .bind("placementId", placementId)
        .bind("startDate", startDate)
        .bind("endDate", endDate)
        .mapTo<ServiceNeed>()
        .list()
        .filter { it.id != excluding }
}

fun Database.Read.getServiceNeedOptions(): List<ServiceNeedOption> {
    return createQuery("SELECT * FROM service_need_option ORDER BY display_order")
        .mapTo<ServiceNeedOption>()
        .list()
}

fun Database.Read.getServiceNeedOptionPublicInfos(placementTypes: List<PlacementType>): List<ServiceNeedOptionPublicInfo> {
    val sql = """
        SELECT
            id,
            name,
            valid_placement_type
        FROM service_need_option
        WHERE default_option IS FALSE AND valid_placement_type = ANY(:placementTypes::placement_type[])
        ORDER BY display_order
    """.trimIndent()
    return createQuery(sql)
        .bind("placementTypes", placementTypes.toTypedArray())
        .mapTo<ServiceNeedOptionPublicInfo>()
        .list()
}
