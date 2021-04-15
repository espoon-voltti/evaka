// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.messaging

import fi.espoo.evaka.invoicing.service.FinanceDecisionGenerator
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.async.NotifyFamilyUpdated
import fi.espoo.evaka.shared.async.NotifyFeeAlterationUpdated
import fi.espoo.evaka.shared.async.NotifyIncomeUpdated
import fi.espoo.evaka.shared.async.NotifyPlacementPlanApplied
import fi.espoo.evaka.shared.async.NotifyServiceNeedUpdated
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.DateRange
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.util.UUID

private val logger = KotlinLogging.logger {}

@Component
class FeeDecisionGenerationJobProcessor(
    private val generator: FinanceDecisionGenerator,
    asyncJobRunner: AsyncJobRunner
) {
    init {
        asyncJobRunner.notifyFamilyUpdated = ::runJob
        asyncJobRunner.notifyFeeAlterationUpdated = ::runJob
        asyncJobRunner.notifyIncomeUpdated = ::runJob
        asyncJobRunner.notifyPlacementPlanApplied = ::runJob
        asyncJobRunner.notifyServiceNeedUpdated = ::runJob
    }

    fun runJob(db: Database, msg: NotifyFamilyUpdated) = db.transaction { tx ->
        logger.info { "Handling family updated event for person (id: ${msg.adultId})" }
        generator.handleFamilyUpdate(tx.handle, msg.adultId, DateRange(msg.startDate, msg.endDate))
    }

    fun runJob(db: Database, msg: NotifyFeeAlterationUpdated) = db.transaction { tx ->
        logger.info { "Handling fee alteration updated event ($msg)" }
        generator.handleFeeAlterationChange(tx.handle, msg.personId, DateRange(msg.startDate, msg.endDate))
    }

    fun runJob(db: Database, msg: NotifyIncomeUpdated) = db.transaction { tx ->
        logger.info { "Handling income updated event ($msg)" }
        generator.handleIncomeChange(tx.handle, msg.personId, DateRange(msg.startDate, msg.endDate))
    }

    fun runJob(db: Database, msg: NotifyPlacementPlanApplied) = db.transaction { tx ->
        logger.info { "Handling placement plan accepted event ($msg)" }
        generator.handlePlacement(tx.handle, msg.childId, DateRange(msg.startDate, msg.endDate))
    }

    fun runJob(db: Database, msg: NotifyServiceNeedUpdated) {
        logger.info { "Handling service need updated event for child (id: ${msg.childId})" }
        db.transaction { syncNewServiceNeed(it, msg.childId) }
        db.transaction { generator.handleServiceNeed(it.handle, msg.childId, DateRange(msg.startDate, msg.endDate)) }
    }
}

fun syncNewServiceNeed(tx: Database.Transaction, childId: UUID) {
    try {
        tx.createUpdate("DELETE FROM new_service_need WHERE placement_id IN (SELECT id FROM placement WHERE child_id = :childId)")
            .bind("childId", childId)
            .execute()

        tx
            .createUpdate(
                """
WITH data AS (
    SELECT
        (CASE
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
        END) AS option_name,
        p.id AS placement_id,
        greatest(p.start_date, sn.start_date) AS start_date,
        least(p.end_date, sn.end_date) AS end_date,
        sn.shift_care
    FROM placement p
    JOIN service_need sn ON p.child_id = sn.child_id AND daterange(p.start_date, p.end_date, '[]') && daterange(sn.start_date, sn.end_date, '[]')
    WHERE p.child_id = :childId
)
INSERT INTO new_service_need (option_id, placement_id, start_date, end_date, shift_care)
SELECT (SELECT id FROM service_need_option WHERE name = option_name), placement_id, start_date, end_date, shift_care
FROM data
WHERE option_name != 'undefined'
"""
            )
            .bind("childId", childId)
            .execute()
    } catch (e: Exception) {
        logger.warn("Service need sync failed for child ($childId)", e)
    }
}
