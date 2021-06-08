// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.serviceneednew

import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.bindNullable
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.NotFound
import org.jdbi.v3.core.kotlin.mapTo
import java.time.LocalDate
import java.util.UUID

fun Database.Read.getServiceNeedsByChild(
    childId: UUID
): List<NewServiceNeed> {
    // language=SQL
    val sql =
        """
        SELECT 
            sn.id, sn.placement_id, sn.start_date, sn.end_date, sn.shift_care, 
            sno.id as option_id, sno.name as option_name,
            sn.confirmed_by as confirmed_employee_id, e.first_name as confirmed_first_name, e.last_name as confirmed_last_name, sn.confirmed_at
        FROM new_service_need sn
        JOIN service_need_option sno on sno.id = sn.option_id
        JOIN placement pl ON pl.id = sn.placement_id
        JOIN employee e on e.id = sn.confirmed_by
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
        SELECT 
            sn.id, sn.placement_id, sn.start_date, sn.end_date, sn.shift_care, 
            sno.id as option_id, sno.name as option_name,
            sn.confirmed_by as confirmed_employee_id, e.first_name as confirmed_first_name, e.last_name as confirmed_last_name, sn.confirmed_at
        FROM new_service_need sn
        JOIN service_need_option sno on sno.id = sn.option_id
        JOIN placement pl ON pl.id = sn.placement_id
        JOIN employee e on e.id = sn.confirmed_by
        WHERE pl.unit_id = :unitId AND daterange(:start, :end, '[]') && daterange(sn.start_date, sn.end_date, '[]')
        """.trimIndent()

    return createQuery(sql)
        .bind("unitId", unitId)
        .bindNullable("start", startDate)
        .bindNullable("end", endDate)
        .mapTo<NewServiceNeed>()
        .toList()
}

fun Database.Read.getNewServiceNeed(id: UUID): NewServiceNeed {
    // language=sql
    val sql = """
        SELECT 
            sn.id, sn.placement_id, sn.start_date, sn.end_date, sn.shift_care, 
            sno.id as option_id, sno.name as option_name,
            sn.confirmed_by as confirmed_employee_id, e.first_name as confirmed_first_name, e.last_name as confirmed_last_name, sn.confirmed_at
        FROM new_service_need sn
        JOIN service_need_option sno on sn.option_id = sno.id
        JOIN employee e on e.id = sn.confirmed_by
        WHERE sn.id = :id
    """.trimIndent()

    return createQuery(sql)
        .bind("id", id)
        .mapTo<NewServiceNeed>()
        .firstOrNull() ?: throw NotFound("Service need $id not found")
}

fun Database.Read.getNewServiceNeedChildRange(id: UUID): NewServiceNeedChildRange {
    // language=sql
    val sql = """
        SELECT p.child_id, sn.start_date, sn.end_date
        FROM new_service_need sn
        JOIN placement p on sn.placement_id = p.id
        WHERE sn.id = :id
    """.trimIndent()

    return createQuery(sql)
        .bind("id", id)
        .mapTo<NewServiceNeedChildRange>()
        .firstOrNull() ?: throw NotFound("Service need $id not found")
}

// language=sql
val migrationSqlCases = """
CASE
    WHEN p.type = 'CLUB' THEN 'Kerho'
    WHEN p.type = 'TEMPORARY_DAYCARE' THEN 'Kokopäiväinen tilapäinen'
    WHEN p.type = 'TEMPORARY_DAYCARE_PART_DAY' THEN 'Osapäiväinen tilapäinen'

    -- daycare
    WHEN (p.type = 'DAYCARE' OR p.type = 'DAYCARE_PART_TIME') AND sn.hours_per_week >= 35 AND NOT sn.part_week THEN 'Kokopäiväinen, vähintään 35h'
    WHEN (p.type = 'DAYCARE' OR p.type = 'DAYCARE_PART_TIME') AND sn.hours_per_week >= 35 AND sn.part_week THEN 'Osaviikkoinen, vähintään 35h'

    WHEN (p.type = 'DAYCARE' OR p.type = 'DAYCARE_PART_TIME') AND sn.hours_per_week > 25 AND sn.hours_per_week < 35 AND NOT sn.part_week THEN 'Kokopäiväinen, yli 25h alle 35h'
    WHEN (p.type = 'DAYCARE' OR p.type = 'DAYCARE_PART_TIME') AND sn.hours_per_week > 25 AND sn.hours_per_week < 35 AND sn.part_week THEN 'Osaviikkoinen, yli 25h alle 35h'

    WHEN (p.type = 'DAYCARE' OR p.type = 'DAYCARE_PART_TIME') AND sn.hours_per_week <= 25 AND sn.part_day AND sn.part_week THEN 'Osapäiväinen ja osaviikkoinen, enintään 25h'
    WHEN (p.type = 'DAYCARE' OR p.type = 'DAYCARE_PART_TIME') AND sn.hours_per_week <= 25 AND sn.part_day THEN 'Osapäiväinen, enintään 25h'
    WHEN (p.type = 'DAYCARE' OR p.type = 'DAYCARE_PART_TIME') AND sn.hours_per_week <= 25 THEN 'Osaviikkoinen, enintään 25h'

    -- daycare for five-year-olds
    WHEN (p.type = 'DAYCARE_FIVE_YEAR_OLDS' OR p.type = 'DAYCARE_PART_TIME_FIVE_YEAR_OLDS') AND sn.hours_per_week >= 45 THEN '5-vuotiaiden kokopäiväinen, vähintään 45h'

    WHEN (p.type = 'DAYCARE_FIVE_YEAR_OLDS' OR p.type = 'DAYCARE_PART_TIME_FIVE_YEAR_OLDS') AND sn.hours_per_week > 35 AND sn.hours_per_week < 45 AND NOT sn.part_week THEN '5-vuotiaiden kokopäiväinen, yli 35h alle 45h'
    WHEN (p.type = 'DAYCARE_FIVE_YEAR_OLDS' OR p.type = 'DAYCARE_PART_TIME_FIVE_YEAR_OLDS') AND sn.hours_per_week > 35 AND sn.hours_per_week < 45 AND sn.part_week THEN '5-vuotiaiden osaviikkoinen, yli 35h alle 45h'

    WHEN (p.type = 'DAYCARE_FIVE_YEAR_OLDS' OR p.type = 'DAYCARE_PART_TIME_FIVE_YEAR_OLDS') AND sn.hours_per_week > 25 AND sn.hours_per_week <= 35 AND NOT sn.part_week THEN '5-vuotiaiden kokopäiväinen, yli 25h alle 35h'
    WHEN (p.type = 'DAYCARE_FIVE_YEAR_OLDS' OR p.type = 'DAYCARE_PART_TIME_FIVE_YEAR_OLDS') AND sn.hours_per_week > 25 AND sn.hours_per_week <= 35 AND sn.part_week THEN '5-vuotiaiden osaviikkoinen, yli 25h alle 35h'

    WHEN (p.type = 'DAYCARE_FIVE_YEAR_OLDS' OR p.type = 'DAYCARE_PART_TIME_FIVE_YEAR_OLDS') AND sn.hours_per_week > 20 AND sn.hours_per_week <= 25 AND sn.part_day AND sn.part_week THEN '5-vuotiaiden osapäiväinen ja osaviikkoinen, yli 20h enintään 25h'
    WHEN (p.type = 'DAYCARE_FIVE_YEAR_OLDS' OR p.type = 'DAYCARE_PART_TIME_FIVE_YEAR_OLDS') AND sn.hours_per_week > 20 AND sn.hours_per_week <= 25 AND sn.part_day THEN '5-vuotiaiden osapäiväinen, yli 20h enintään 25h'
    WHEN (p.type = 'DAYCARE_FIVE_YEAR_OLDS' OR p.type = 'DAYCARE_PART_TIME_FIVE_YEAR_OLDS') AND sn.hours_per_week > 20 AND sn.hours_per_week <= 25 THEN '5-vuotiaiden osaviikkoinen, yli 20h enintään 25h'

    WHEN (p.type = 'DAYCARE_FIVE_YEAR_OLDS' OR p.type = 'DAYCARE_PART_TIME_FIVE_YEAR_OLDS') AND sn.hours_per_week <= 20 AND sn.part_day AND sn.part_week THEN '5-vuotiaiden maksuton osapäiväinen ja osaviikkoinen'
    WHEN (p.type = 'DAYCARE_FIVE_YEAR_OLDS' OR p.type = 'DAYCARE_PART_TIME_FIVE_YEAR_OLDS') AND sn.hours_per_week <= 20 AND sn.part_day THEN '5-vuotiaiden maksuton osapäiväinen'
    WHEN (p.type = 'DAYCARE_FIVE_YEAR_OLDS' OR p.type = 'DAYCARE_PART_TIME_FIVE_YEAR_OLDS') AND sn.hours_per_week <= 20 THEN '5-vuotiaiden maksuton osaviikkoinen'

    -- preschool
    WHEN (p.type = 'PRESCHOOL' OR p.type = 'PRESCHOOL_DAYCARE') AND sn.hours_per_week >= 45 AND NOT sn.part_week THEN 'Kokopäiväinen liittyvä, yhteensä vähintään 45h'
    WHEN (p.type = 'PRESCHOOL' OR p.type = 'PRESCHOOL_DAYCARE') AND sn.hours_per_week >= 45 AND sn.part_week THEN 'Osaviikkoinen liittyvä, yhteensä vähintään 45h'

    WHEN (p.type = 'PRESCHOOL' OR p.type = 'PRESCHOOL_DAYCARE') AND sn.hours_per_week > 35 AND sn.hours_per_week < 45 AND sn.part_day AND sn.part_week THEN 'Osapäiväinen ja osaviikkoinen liittyvä, yhteensä yli 35h alle 45h'
    WHEN (p.type = 'PRESCHOOL' OR p.type = 'PRESCHOOL_DAYCARE') AND sn.hours_per_week > 35 AND sn.hours_per_week < 45 AND sn.part_day THEN 'Osapäiväinen liittyvä, yhteensä yli 35h alle 45h'
    WHEN (p.type = 'PRESCHOOL' OR p.type = 'PRESCHOOL_DAYCARE') AND sn.hours_per_week > 35 AND sn.hours_per_week < 45 THEN 'Osaviikkoinen liittyvä, yhteensä yli 35h alle 45h'

    WHEN (p.type = 'PRESCHOOL' OR p.type = 'PRESCHOOL_DAYCARE') AND sn.hours_per_week > 20 AND sn.hours_per_week <= 35 AND sn.part_day AND sn.part_week THEN 'Osapäiväinen ja osaviikkoinen liittyvä, yhteensä enintään 35h'
    WHEN (p.type = 'PRESCHOOL' OR p.type = 'PRESCHOOL_DAYCARE') AND sn.hours_per_week > 20 AND sn.hours_per_week <= 35 AND sn.part_day THEN 'Osapäiväinen liittyvä, yhteensä enintään 35h'
    WHEN (p.type = 'PRESCHOOL' OR p.type = 'PRESCHOOL_DAYCARE') AND sn.hours_per_week > 20 AND sn.hours_per_week <= 35 THEN 'Osaviikkoinen liittyvä, yhteensä enintään 35h'

    WHEN (p.type = 'PRESCHOOL' OR p.type = 'PRESCHOOL_DAYCARE') AND sn.hours_per_week <= 20 THEN 'Esiopetus'

    -- preparatory
    WHEN (p.type = 'PREPARATORY' OR p.type = 'PREPARATORY_DAYCARE') AND sn.hours_per_week >= 50 AND NOT sn.part_week THEN 'Kokopäiväinen liittyvä, yhteensä vähintään 50h'
    WHEN (p.type = 'PREPARATORY' OR p.type = 'PREPARATORY_DAYCARE') AND sn.hours_per_week >= 50 AND sn.part_week THEN 'Osaviikkoinen liittyvä, yhteensä vähintään 50h'

    WHEN (p.type = 'PREPARATORY' OR p.type = 'PREPARATORY_DAYCARE') AND sn.hours_per_week > 40 AND sn.hours_per_week < 50 AND sn.part_day AND sn.part_week THEN 'Osapäiväinen ja osaviikkoinen liittyvä, yhteensä yli 40h alle 50h'
    WHEN (p.type = 'PREPARATORY' OR p.type = 'PREPARATORY_DAYCARE') AND sn.hours_per_week > 40 AND sn.hours_per_week < 50 AND sn.part_day THEN 'Osapäiväinen liittyvä, yhteensä yli 40h alle 50h'
    WHEN (p.type = 'PREPARATORY' OR p.type = 'PREPARATORY_DAYCARE') AND sn.hours_per_week > 40 AND sn.hours_per_week < 50 THEN 'Osaviikkoinen liittyvä, yhteensä yli 40h alle 50h'

    WHEN (p.type = 'PREPARATORY' OR p.type = 'PREPARATORY_DAYCARE') AND sn.hours_per_week > 25 AND sn.hours_per_week <= 40 AND sn.part_day AND sn.part_week THEN 'Osapäiväinen ja osaviikkoinen liittyvä, yhteensä enintään 40h'
    WHEN (p.type = 'PREPARATORY' OR p.type = 'PREPARATORY_DAYCARE') AND sn.hours_per_week > 25 AND sn.hours_per_week <= 40 AND sn.part_day THEN 'Osapäiväinen liittyvä, yhteensä enintään 40h'
    WHEN (p.type = 'PREPARATORY' OR p.type = 'PREPARATORY_DAYCARE') AND sn.hours_per_week > 25 AND sn.hours_per_week <= 40 THEN 'Osaviikkoinen liittyvä, yhteensä enintään 40h'

    WHEN (p.type = 'PREPARATORY' OR p.type = 'PREPARATORY_DAYCARE') AND sn.hours_per_week <= 25 THEN 'Valmistava opetus'

    ELSE 'undefined'
END
"""

fun Database.Transaction.insertNewServiceNeed(
    placementId: UUID,
    startDate: LocalDate,
    endDate: LocalDate,
    optionId: UUID,
    shiftCare: Boolean,
    confirmedBy: UUID,
    confirmedAt: HelsinkiDateTime
): UUID {
    // language=sql
    val sql = """
        INSERT INTO new_service_need (placement_id, start_date, end_date, option_id, shift_care, confirmed_by, confirmed_at) 
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
        .mapTo<UUID>()
        .one()
}

fun Database.Transaction.updateNewServiceNeed(
    id: UUID,
    startDate: LocalDate,
    endDate: LocalDate,
    optionId: UUID,
    shiftCare: Boolean,
    confirmedBy: UUID,
    confirmedAt: HelsinkiDateTime
) {
    // language=sql
    val sql = """
        UPDATE new_service_need
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

fun Database.Transaction.deleteNewServiceNeed(
    id: UUID
) {
    createUpdate("DELETE FROM new_service_need WHERE id = :id")
        .bind("id", id)
        .execute()
}

fun Database.Read.getOverlappingServiceNeeds(
    placementId: UUID,
    startDate: LocalDate,
    endDate: LocalDate,
    excluding: UUID?
): List<NewServiceNeed> {
    // language=sql
    val sql = """
        SELECT 
            sn.id, sn.placement_id, sn.start_date, sn.end_date, sn.shift_care, 
            sno.id as option_id, sno.name as option_name,
            sn.confirmed_by as confirmed_employee_id, e.first_name as confirmed_first_name, e.last_name as confirmed_last_name, sn.confirmed_at
        FROM new_service_need sn
        JOIN service_need_option sno on sn.option_id = sno.id
        JOIN employee e on e.id = sn.confirmed_by
        WHERE placement_id = :placementId AND daterange(sn.start_date, sn.end_date, '[]') && daterange(:startDate, :endDate, '[]')
    """.trimIndent()

    return createQuery(sql)
        .bind("placementId", placementId)
        .bind("startDate", startDate)
        .bind("endDate", endDate)
        .mapTo<NewServiceNeed>()
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
