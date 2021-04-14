package fi.espoo.evaka.serviceneednew

import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.bindNullable
import org.jdbi.v3.core.kotlin.mapTo
import java.time.LocalDate
import java.util.UUID

fun Database.Read.getServiceNeedsByChild(
    childId: UUID
): List<NewServiceNeed> {
    // language=SQL
    val sql =
        """
        SELECT sn.id, sn.placement_id, sn.start_date, sn.end_date, sn.shift_care, sno.id as option_id, sno.name as option_name
        FROM new_service_need sn
        JOIN service_need_option sno on sno.id = sn.option_id
        JOIN placement pl ON pl.id = sn.placement_id
        WHERE pl.child_id = :childId
        """.trimIndent()

    return createQuery(sql)
        .bind("childId", childId)
        .mapTo<NewServiceNeed>()
        .toList()
}

fun Database.Read.getServiceNeedsByUnit(
    unitId: UUID,
    startDate: LocalDate?,
    endDate: LocalDate?
): List<NewServiceNeed> {
    // language=SQL
    val sql =
        """
        SELECT sn.id, sn.placement_id, sn.start_date, sn.end_date, sn.shift_care, sno.id as option_id, sno.name as option_name
        FROM new_service_need sn
        JOIN service_need_option sno on sno.id = sn.option_id
        JOIN placement pl ON pl.id = sn.placement_id
        WHERE pl.unit_id = :unitId
            AND ((:start IS NULL AND :end IS NULL) OR daterange(:start, :end, '[]') && daterange(sn.start_date, sn.end_date, '[]'))
        """.trimIndent()

    return createQuery(sql)
        .bind("unitId", unitId)
        .bindNullable("start", startDate)
        .bindNullable("end", endDate)
        .mapTo<NewServiceNeed>()
        .toList()
}
