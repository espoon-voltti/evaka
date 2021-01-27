// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.serviceneed

import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.NotFound
import org.jdbi.v3.core.Handle
import java.time.LocalDate
import java.util.UUID

fun insertServiceNeed(h: Handle, user: AuthenticatedUser, childId: UUID, data: ServiceNeedRequest): ServiceNeed {
    //language=sql
    val sql =
        """
        INSERT INTO service_need (
            child_id, 
            start_date, 
            end_date,
            updated_by, 
            hours_per_week,
            part_day,
            part_week,
            shift_care
        )
        VALUES (
            :childId, 
            :startDate, 
            :endDate,
            :updatedBy, 
            :hoursPerWeek,
            :partDay,
            :partWeek,
            :shiftCare
        )
        RETURNING 
            *, 
            (SELECT concat_ws(' ', first_name, last_name) FROM employee WHERE id = :updatedBy) AS updated_by_name
        """.trimIndent()

    return h
        .createQuery(sql)
        .bind("childId", childId)
        .bind("startDate", data.startDate)
        .bind("endDate", data.endDate)
        .bind("updatedBy", user.id)
        .bind("hoursPerWeek", data.hoursPerWeek)
        .bind("partDay", data.partDay)
        .bind("partWeek", data.partWeek)
        .bind("shiftCare", data.shiftCare)
        .mapTo(ServiceNeed::class.java)
        .first()
}

fun getServiceNeedsByChild(h: Handle, childId: UUID): List<ServiceNeed> {
    //language=sql
    val sql =
        """
        SELECT sn.*, concat(e.first_name, ' ', e.last_name) AS updated_by_name
        FROM service_need sn
        JOIN employee e ON e.id = sn.updated_by
        WHERE child_id = :childId 
        ORDER BY start_date DESC
        """.trimIndent()
    return h.createQuery(sql)
        .bind("childId", childId)
        .mapTo(ServiceNeed::class.java)
        .list()
}

fun getServiceNeedsByChildDuringPeriod(h: Handle, childId: UUID, startDate: LocalDate, endDate: LocalDate?): List<ServiceNeed> {
    //language=sql
    val sql =
        """
        SELECT sn.*, concat(e.first_name, ' ', e.last_name) AS updated_by_name
        FROM service_need sn
        JOIN employee e ON e.id = sn.updated_by
        WHERE child_id = :childId AND daterange(start_date, end_date, '[]') && :period
        ORDER BY start_date DESC 
        """.trimIndent()
    return h.createQuery(sql)
        .bind("childId", childId)
        .bind("period", DateRange(startDate, endDate))
        .mapTo(ServiceNeed::class.java)
        .list()
}

fun updateServiceNeed(h: Handle, user: AuthenticatedUser, id: UUID, data: ServiceNeedRequest): ServiceNeed {
    //language=sql
    val sql =
        """
        UPDATE service_need SET 
            start_date = :startDate,
            end_date = :endDate,
            updated_by = :updatedBy,
            hours_per_week = :hoursPerWeek,
            part_day = :partDay,
            part_week = :partWeek,
            shift_care = :shiftCare
        WHERE id = :id
        RETURNING 
            *, 
            (SELECT concat_ws(' ', first_name, last_name) FROM employee WHERE id = :updatedBy) AS updated_by_name
        """.trimIndent()

    return h.createQuery(sql)
        .bind("id", id)
        .bind("startDate", data.startDate)
        .bind("endDate", data.endDate)
        .bind("updatedBy", user.id)
        .bind("hoursPerWeek", data.hoursPerWeek)
        .bind("partDay", data.partDay)
        .bind("partWeek", data.partWeek)
        .bind("shiftCare", data.shiftCare)
        .mapTo(ServiceNeed::class.java)
        .firstOrNull() ?: throw NotFound("Service need $id not found")
}

fun shortenOverlappingServiceNeed(h: Handle, user: AuthenticatedUser, childId: UUID, startDate: LocalDate, endDate: LocalDate?) {
    //language=sql
    val sql =
        """
        UPDATE service_need 
        SET end_date = :startDate - interval '1 day', updated_by = :updatedBy
        WHERE child_id = :childId AND daterange(start_date, end_date, '[]') @> :startDate AND start_date <> :startDate
        RETURNING *
        """.trimIndent()

    h.createUpdate(sql)
        .bind("childId", childId)
        .bind("startDate", startDate)
        .bind("endDate", endDate)
        .bind("updatedBy", user.id)
        .execute()
}

fun deleteServiceNeed(h: Handle, id: UUID): ServiceNeed {
    //language=sql
    val sql =
        """DELETE FROM service_need WHERE id = :id
        RETURNING 
            *, 
            (SELECT concat_ws(' ', first_name, last_name) FROM employee WHERE id = service_need.updated_by) AS updated_by_name
    """.trimMargin()

    return h
        .createQuery(sql)
        .bind("id", id)
        .mapTo(ServiceNeed::class.java)
        .firstOrNull() ?: throw NotFound("Service need $id not found")
}
