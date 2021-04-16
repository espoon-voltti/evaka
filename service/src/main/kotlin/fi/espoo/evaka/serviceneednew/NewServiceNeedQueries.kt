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
        WHERE pl.unit_id = :unitId AND daterange(:start, :end, '[]') && daterange(sn.start_date, sn.end_date, '[]')
        """.trimIndent()

    return createQuery(sql)
        .bind("unitId", unitId)
        .bindNullable("start", startDate)
        .bindNullable("end", endDate)
        .mapTo<NewServiceNeed>()
        .toList()
}

val migrationSqlCases = """
    CASE
        WHEN p.type = 'DAYCARE' AND sn.hours_per_week >= 35 AND NOT sn.part_day AND NOT sn.part_week THEN 'Kokopäiväinen, vähintään 35h'
        WHEN p.type = 'DAYCARE' AND sn.hours_per_week > 25 AND sn.hours_per_week < 35 AND NOT sn.part_day AND NOT sn.part_week THEN 'Kokopäiväinen, 25-35h'
        WHEN p.type = 'DAYCARE' AND sn.hours_per_week >= 35 AND NOT sn.part_day AND sn.part_week THEN 'Osaviikkoinen, vähintään 35h'
        WHEN p.type = 'DAYCARE' AND sn.hours_per_week > 25 AND sn.hours_per_week < 35 AND NOT sn.part_day AND sn.part_week THEN 'Osaviikkoinen, 25-35h'
        WHEN p.type = 'DAYCARE' AND sn.hours_per_week <= 25 AND NOT sn.part_day AND sn.part_week THEN 'Osaviikkoinen, enintään 25h'
        WHEN p.type = 'DAYCARE_PART_TIME' AND sn.hours_per_week <= 25 AND sn.part_day AND NOT sn.part_week THEN 'Osapäiväinen'
        WHEN p.type = 'DAYCARE_PART_TIME' AND sn.hours_per_week <= 25 AND sn.part_day AND sn.part_week THEN 'Osapäiväinen ja osaviikkoinen'
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
        ELSE 'undefined'
    END
""".trimIndent()
