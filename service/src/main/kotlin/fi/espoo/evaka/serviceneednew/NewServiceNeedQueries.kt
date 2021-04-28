// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.serviceneednew

import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.bindNullable
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
            sn.id,
            sn.placement_id,
            sn.start_date,
            sn.end_date,
            sn.option_id,
            sno.name as option_name,
            sn.shift_care
        FROM new_service_need sn
        JOIN service_need_option sno on sn.option_id = sno.id
        WHERE sn.id = :id
    """.trimIndent()

    return createQuery(sql)
        .bind("id", id)
        .mapTo<NewServiceNeed>()
        .firstOrNull() ?: throw NotFound("Service need $id not found")
}

// language=sql
val migrationSqlCases = """
    CASE
        WHEN p.type = 'DAYCARE' AND sn.hours_per_week >= 35 AND NOT sn.part_day AND NOT sn.part_week THEN 'Kokopäiväinen, vähintään 35h'
        WHEN p.type = 'DAYCARE' AND sn.hours_per_week > 25 AND sn.hours_per_week < 35 AND NOT sn.part_day AND NOT sn.part_week THEN 'Kokopäiväinen, 25-35h'
        WHEN p.type = 'DAYCARE' AND sn.hours_per_week >= 35 AND NOT sn.part_day AND sn.part_week THEN 'Osaviikkoinen, vähintään 35h'
        WHEN p.type = 'DAYCARE' AND sn.hours_per_week > 25 AND sn.hours_per_week < 35 AND NOT sn.part_day AND sn.part_week THEN 'Osaviikkoinen, 25-35h'
        WHEN p.type = 'DAYCARE' AND sn.hours_per_week <= 25 AND NOT sn.part_day AND sn.part_week THEN 'Osaviikkoinen, enintään 25h'
        WHEN p.type = 'DAYCARE_PART_TIME' AND sn.hours_per_week <= 25 AND sn.part_day AND NOT sn.part_week THEN 'Osapäiväinen'
        WHEN p.type = 'DAYCARE_PART_TIME' AND sn.hours_per_week <= 25 AND sn.part_day AND sn.part_week THEN 'Osapäiväinen ja osaviikkoinen'
        WHEN p.type = 'DAYCARE_FIVE_YEAR_OLDS' AND sn.hours_per_week >= 45 AND NOT sn.part_day AND NOT sn.part_week THEN '5-vuotiaiden kokopäiväinen, vähintään 45h'
        WHEN p.type = 'DAYCARE_FIVE_YEAR_OLDS' AND sn.hours_per_week > 35 AND sn.hours_per_week < 45 AND NOT sn.part_day AND NOT sn.part_week THEN '5-vuotiaiden kokopäiväinen, 35-45h'
        WHEN p.type = 'DAYCARE_FIVE_YEAR_OLDS' AND sn.hours_per_week > 25 AND sn.hours_per_week <= 35 AND NOT sn.part_day AND NOT sn.part_week THEN '5-vuotiaiden kokopäiväinen, 25-35h'
        WHEN p.type = 'DAYCARE_FIVE_YEAR_OLDS' AND sn.hours_per_week > 25 AND sn.hours_per_week <= 35 AND NOT sn.part_day AND sn.part_week THEN '5-vuotiaiden osaviikkoinen, 25-35h'
        WHEN p.type = 'DAYCARE_FIVE_YEAR_OLDS' AND sn.hours_per_week > 20 AND sn.hours_per_week <= 25 AND NOT sn.part_day AND sn.part_week THEN '5-vuotiaiden osaviikkoinen, 20-25h'
        WHEN p.type = 'DAYCARE_FIVE_YEAR_OLDS' AND sn.hours_per_week <= 20 AND NOT sn.part_day AND sn.part_week THEN '5-vuotiaiden ilmainen osaviikkoinen'
        WHEN p.type = 'DAYCARE_PART_TIME_FIVE_YEAR_OLDS' AND sn.hours_per_week > 20 AND sn.hours_per_week <= 25 AND sn.part_day AND NOT sn.part_week THEN '5-vuotiaiden osapäiväinen, 20-25h'
        WHEN p.type = 'DAYCARE_PART_TIME_FIVE_YEAR_OLDS' AND sn.hours_per_week <= 20 AND sn.part_day AND NOT sn.part_week THEN '5-vuotiaiden ilmainen osapäiväinen'
        WHEN p.type = 'DAYCARE_PART_TIME_FIVE_YEAR_OLDS' AND sn.hours_per_week > 20 AND sn.hours_per_week <= 25 AND sn.part_day AND sn.part_week THEN '5-vuotiaiden osapäiväinen ja osaviikkoinen, 20-25h'
        WHEN p.type = 'DAYCARE_PART_TIME_FIVE_YEAR_OLDS' AND sn.hours_per_week <= 20 AND sn.part_day AND sn.part_week THEN '5-vuotiaiden ilmainen osapäiväinen ja osaviikkoinen'
        WHEN p.type = 'PRESCHOOL' AND sn.hours_per_week <= 20 THEN 'Esiopetus ilman liittyvää'
        WHEN p.type = 'PREPARATORY' AND sn.hours_per_week <= 25 THEN 'Valmistava opetus ilman liittyvää'
        WHEN p.type = 'CLUB' THEN 'Kerho'
        WHEN p.type = 'TEMPORARY_DAYCARE' THEN 'Kokopäiväinen tilapäinen'
        WHEN p.type = 'TEMPORARY_DAYCARE_PART_DAY' THEN 'Osapäiväinen tilapäinen'
        WHEN p.type = 'PRESCHOOL_DAYCARE' AND sn.hours_per_week >= 45 AND NOT sn.part_day AND NOT sn.part_week THEN 'Kokopäiväinen liittyvä, yhteensä vähintään 45h'
        WHEN p.type = 'PRESCHOOL_DAYCARE' AND sn.hours_per_week >= 45 AND NOT sn.part_day AND sn.part_week THEN 'Osaviikkoinen liittyvä, yhteensä vähintään 45h'
        WHEN p.type = 'PRESCHOOL_DAYCARE' AND sn.hours_per_week > 35 AND sn.hours_per_week < 45 AND NOT sn.part_day AND sn.part_week THEN 'Osaviikkoinen liittyvä, yhteensä 35-45h'
        WHEN p.type = 'PRESCHOOL_DAYCARE' AND sn.hours_per_week > 20 AND sn.hours_per_week <= 35 AND NOT sn.part_day AND sn.part_week THEN 'Osaviikkoinen liittyvä, yhteensä enintään 35h'
        WHEN p.type = 'PRESCHOOL_DAYCARE' AND sn.hours_per_week > 35 AND sn.hours_per_week < 45 AND sn.part_day AND NOT sn.part_week THEN 'Osapäiväinen liittyvä, yhteensä 35-45h'
        WHEN p.type = 'PRESCHOOL_DAYCARE' AND sn.hours_per_week > 20 AND sn.hours_per_week <= 35 AND sn.part_day AND NOT sn.part_week THEN 'Osapäiväinen liittyvä, yhteensä enintään 35h'
        WHEN p.type = 'PRESCHOOL_DAYCARE' AND sn.hours_per_week > 35 AND sn.hours_per_week < 45 AND sn.part_day AND sn.part_week THEN 'Osapäiväinen ja osaviikkoinen liittyvä, yhteensä 35-45h'
        WHEN p.type = 'PRESCHOOL_DAYCARE' AND sn.hours_per_week > 20 AND sn.hours_per_week <= 35 AND sn.part_day AND sn.part_week THEN 'Osapäiväinen ja osaviikkoinen liittyvä, yhteensä enintään 35h'
        WHEN p.type = 'PREPARATORY_DAYCARE' AND sn.hours_per_week >= 50 AND NOT sn.part_day AND NOT sn.part_week THEN 'Kokopäiväinen liittyvä, yhteensä vähintään 50h'
        WHEN p.type = 'PREPARATORY_DAYCARE' AND sn.hours_per_week >= 50 AND NOT sn.part_day AND sn.part_week THEN 'Osaviikkoinen liittyvä, yhteensä vähintään 50h'
        WHEN p.type = 'PREPARATORY_DAYCARE' AND sn.hours_per_week > 40 AND sn.hours_per_week < 50 AND NOT sn.part_day AND sn.part_week THEN 'Osaviikkoinen liittyvä, yhteensä 40-50h'
        WHEN p.type = 'PREPARATORY_DAYCARE' AND sn.hours_per_week > 25 AND sn.hours_per_week <= 40 AND NOT sn.part_day AND sn.part_week  THEN 'Osaviikkoinen liittyvä, yhteensä enintään 40h'
        WHEN p.type = 'PREPARATORY_DAYCARE' AND sn.hours_per_week > 40 AND sn.hours_per_week < 50 AND sn.part_day AND NOT sn.part_week THEN 'Osapäiväinen liittyvä, yhteensä 40-50h'
        WHEN p.type = 'PREPARATORY_DAYCARE' AND sn.hours_per_week > 25 AND sn.hours_per_week <= 40 AND sn.part_day AND NOT sn.part_week THEN 'Osapäiväinen liittyvä, yhteensä enintään 40h'
        WHEN p.type = 'PREPARATORY_DAYCARE' AND sn.hours_per_week > 40 AND sn.hours_per_week < 50 AND sn.part_day AND sn.part_week THEN 'Osapäiväinen ja osaviikkoinen liittyvä, yhteensä 40-50h'
        WHEN p.type = 'PREPARATORY_DAYCARE' AND sn.hours_per_week > 25 AND sn.hours_per_week <= 40 AND sn.part_day AND sn.part_week THEN 'Osapäiväinen ja osaviikkoinen liittyvä, yhteensä enintään 40h'

        -- invalid but easily fixable cases
        WHEN p.type = 'DAYCARE' AND sn.hours_per_week >= 35 AND sn.part_day AND NOT sn.part_week THEN 'Kokopäiväinen, vähintään 35h'
        WHEN p.type = 'DAYCARE' AND sn.hours_per_week > 25 AND sn.hours_per_week < 35 AND sn.part_day AND NOT sn.part_week THEN 'Kokopäiväinen, 25-35h'
        WHEN p.type = 'DAYCARE' AND sn.hours_per_week >= 35 AND sn.part_day AND sn.part_week THEN 'Osaviikkoinen, vähintään 35h'
        WHEN p.type = 'DAYCARE' AND sn.hours_per_week > 25 AND sn.hours_per_week < 35 AND sn.part_day AND sn.part_week THEN 'Osaviikkoinen, 25-35h'
        WHEN p.type = 'DAYCARE_FIVE_YEAR_OLDS' AND sn.hours_per_week >= 45 AND sn.part_day AND NOT sn.part_week THEN '5-vuotiaiden kokopäiväinen, vähintään 45h'
        WHEN p.type = 'DAYCARE_FIVE_YEAR_OLDS' AND sn.hours_per_week > 35 AND sn.hours_per_week < 45 AND sn.part_day AND NOT sn.part_week THEN '5-vuotiaiden kokopäiväinen, 35-45h'
        WHEN p.type = 'DAYCARE_FIVE_YEAR_OLDS' AND sn.hours_per_week > 25 AND sn.hours_per_week <= 35 AND sn.part_day AND NOT sn.part_week THEN '5-vuotiaiden kokopäiväinen, 25-35h'
        WHEN p.type = 'DAYCARE_FIVE_YEAR_OLDS' AND sn.hours_per_week > 25 AND sn.hours_per_week <= 35 AND sn.part_day AND sn.part_week THEN '5-vuotiaiden osaviikkoinen, 25-35h'
        WHEN p.type = 'PRESCHOOL_DAYCARE' AND sn.hours_per_week >= 45 AND sn.part_day AND NOT sn.part_week THEN 'Kokopäiväinen liittyvä, yhteensä vähintään 45h'
        WHEN p.type = 'PRESCHOOL_DAYCARE' AND sn.hours_per_week >= 45 AND sn.part_day AND sn.part_week THEN 'Osaviikkoinen liittyvä, yhteensä vähintään 45h'
        WHEN p.type = 'PREPARATORY_DAYCARE' AND sn.hours_per_week >= 50 AND sn.part_day AND NOT sn.part_week THEN 'Kokopäiväinen liittyvä, yhteensä vähintään 50h'
        WHEN p.type = 'PREPARATORY_DAYCARE' AND sn.hours_per_week >= 50 AND sn.part_day AND sn.part_week THEN 'Osaviikkoinen liittyvä, yhteensä vähintään 50h'

        ELSE 'undefined'
    END
""".trimIndent()

fun Database.Transaction.insertNewServiceNeed(
    placementId: UUID,
    startDate: LocalDate,
    endDate: LocalDate,
    optionId: UUID,
    shiftCare: Boolean
) {
    // language=sql
    val sql = """
        INSERT INTO new_service_need (placement_id, start_date, end_date, option_id, shift_care) 
        VALUES (:placementId, :startDate, :endDate, :optionId, :shiftCare);
    """.trimIndent()
    createUpdate(sql)
        .bind("placementId", placementId)
        .bind("startDate", startDate)
        .bind("endDate", endDate)
        .bind("optionId", optionId)
        .bind("shiftCare", shiftCare)
        .execute()
}

fun Database.Transaction.updateNewServiceNeed(
    id: UUID,
    startDate: LocalDate,
    endDate: LocalDate,
    optionId: UUID,
    shiftCare: Boolean
) {
    // language=sql
    val sql = """
        UPDATE new_service_need
        SET start_date = :startDate, end_date = :endDate, option_id = :optionId, shift_care = :shiftCare
        WHERE id = :id
    """.trimIndent()

    createUpdate(sql)
        .bind("id", id)
        .bind("startDate", startDate)
        .bind("endDate", endDate)
        .bind("optionId", optionId)
        .bind("shiftCare", shiftCare)
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
    endDate: LocalDate
): List<NewServiceNeed> {
    // language=sql
    val sql = """
        SELECT 
            sn.id,
            sn.placement_id,
            sn.start_date,
            sn.end_date,
            sn.option_id,
            sno.name as option_name,
            sn.shift_care
        FROM new_service_need sn
        JOIN service_need_option sno on sn.option_id = sno.id
        WHERE placement_id = :placementId AND daterange(sn.start_date, sn.end_date, '[]') && daterange(:startDate, :endDate, '[]')
    """.trimIndent()

    return createQuery(sql)
        .bind("placementId", placementId)
        .bind("startDate", startDate)
        .bind("endDate", endDate)
        .mapTo<NewServiceNeed>()
        .list()
}

fun Database.Read.getServiceNeedOptions(): List<ServiceNeedOption> {
    return createQuery("SELECT * FROM service_need_option")
        .mapTo<ServiceNeedOption>()
        .list()
}
