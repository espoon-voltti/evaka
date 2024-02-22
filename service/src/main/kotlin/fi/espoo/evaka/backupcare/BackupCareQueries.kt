// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.backupcare

import fi.espoo.evaka.placement.getPlacementsForChild
import fi.espoo.evaka.shared.BackupCareId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.data.DateMap
import fi.espoo.evaka.shared.data.DateSet
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.FiniteDateRange

fun Database.Read.getBackupCaresForChild(childId: ChildId): List<ChildBackupCare> =
    @Suppress("DEPRECATION")
    createQuery(
            // language=SQL
            """
SELECT
  backup_care.id,
  daycare.id AS unit_id,
  daycare.name AS unit_name,
  group_id,
  daycare_group.name AS group_name,
  daterange(backup_care.start_date, backup_care.end_date, '[]') AS period
FROM backup_care
JOIN daycare
ON daycare.id = unit_id
LEFT JOIN daycare_group
ON daycare_group.id = group_id
WHERE child_id = :childId
"""
        )
        .bind("childId", childId)
        .toList<ChildBackupCare>()

fun Database.Read.getBackupCaresForDaycare(
    daycareId: DaycareId,
    period: FiniteDateRange
): List<UnitBackupCare> =
    @Suppress("DEPRECATION")
    createQuery(
            // language=SQL
            """
SELECT
  backup_care.id,
  person.id AS child_id,
  person.date_of_birth AS child_birth_date,
  person.last_name AS child_last_name,
  person.first_name AS child_first_name,
  group_id,
  daycare_group.name AS group_name,
  daterange(backup_care.start_date, backup_care.end_date, '[]') AS period,
  placement.units AS from_units,
  '[]' AS service_needs,
  days_in_range(daterange(backup_care.start_date, backup_care.end_date, '[]') * daterange('2020-03-01', NULL)) - days_with_service_need AS missingServiceNeedDays
FROM backup_care
JOIN (
  SELECT
    bc.id,
    coalesce(sum(days_in_range(daterange(bc.start_date, bc.end_date, '[]') * daterange('2020-03-01', NULL) * daterange(sn.start_date, sn.end_date, '[]'))), 0) AS days_with_service_need
  FROM backup_care bc
  LEFT JOIN placement pl ON bc.child_id = pl.child_id AND daterange(bc.start_date, bc.end_date, '[]') && daterange(pl.start_date, pl.end_date, '[]')
  LEFT JOIN service_need sn ON pl.id = sn.placement_id
  GROUP BY bc.id
) AS service_need_stats
USING (id)
JOIN person
ON person.id = child_id
LEFT JOIN daycare_group
ON daycare_group.id = group_id
JOIN LATERAL (
  SELECT coalesce(jsonb_agg(DISTINCT u.name), '[]'::jsonb) AS units
  FROM placement p
  JOIN daycare u ON u.id = p.unit_id
  WHERE p.child_id = backup_care.child_id
    AND daterange(p.start_date, p.end_date, '[]') && daterange(backup_care.start_date, backup_care.end_date, '[]')
) placement ON TRUE
WHERE unit_id = :daycareId
AND daterange(backup_care.start_date, backup_care.end_date, '[]') && :period
"""
        )
        .bind("daycareId", daycareId)
        .bind("period", period)
        .toList<UnitBackupCare>()

fun Database.Read.getBackupCareChildrenInGroup(
    daycareId: DaycareId,
    groupId: GroupId,
    period: FiniteDateRange
): List<ChildId> =
    @Suppress("DEPRECATION")
    createQuery(
            """
    SELECT child_id FROM backup_care
    WHERE unit_id = :daycareId
      AND group_id = :groupId
      AND daterange(start_date, end_date, '[]') && :period
    """
                .trimIndent()
        )
        .bind("daycareId", daycareId)
        .bind("groupId", groupId)
        .bind("period", period)
        .toList<ChildId>()

data class BackupCareInfo(val childId: ChildId, val unitId: DaycareId, val period: FiniteDateRange)

fun Database.Read.getBackupCare(id: BackupCareId): BackupCareInfo? =
    @Suppress("DEPRECATION")
    createQuery(
            """
    SELECT child_id, unit_id, daterange(start_date, end_date, '[]') period FROM backup_care
    WHERE id = :id
    """
                .trimIndent()
        )
        .bind("id", id)
        .exactlyOneOrNull<BackupCareInfo>()

fun Database.Transaction.createBackupCare(
    childId: ChildId,
    backupCare: NewBackupCare
): BackupCareId =
    @Suppress("DEPRECATION")
    createUpdate(
            // language=SQL
            """
INSERT INTO backup_care (child_id, unit_id, group_id, start_date, end_date)
VALUES (:childId, :unitId, :groupId, :start, :end)
RETURNING id
"""
        )
        .bind("childId", childId)
        .bind("unitId", backupCare.unitId)
        .bind("groupId", backupCare.groupId)
        .bind("start", backupCare.period.start)
        .bind("end", backupCare.period.end)
        .executeAndReturnGeneratedKeys()
        .exactlyOne<BackupCareId>()

fun Database.Transaction.updateBackupCare(
    id: BackupCareId,
    period: FiniteDateRange,
    groupId: GroupId?
) =
    @Suppress("DEPRECATION")
    createUpdate(
            // language=SQL
            """
UPDATE backup_care
SET
  start_date = :start,
  end_date = :end,
  group_id = :groupId
WHERE id = :id
"""
        )
        .bind("id", id)
        .bind("start", period.start)
        .bind("end", period.end)
        .bind("groupId", groupId)
        .execute()

fun Database.Transaction.deleteBackupCare(id: BackupCareId) =
    @Suppress("DEPRECATION")
    createUpdate(
            // language=SQL
            """
DELETE FROM backup_care
WHERE id = :id
"""
        )
        .bind("id", id)
        .execute()

fun Database.Read.getBackupCareChildId(id: BackupCareId): ChildId =
    @Suppress("DEPRECATION")
    createQuery(
            // language=SQL
            """
    SELECT child_id FROM backup_care WHERE id = :id
    """
                .trimIndent()
        )
        .bind("id", id)
        .exactlyOne<ChildId>()

/** Recreates backup cares for a child so that they are always within placements. */
fun Database.Transaction.recreateBackupCares(childId: ChildId) {
    val placementDates =
        DateSet.of(
            getPlacementsForChild(childId).asSequence().map {
                FiniteDateRange(it.startDate, it.endDate)
            }
        )
    val placementSpan = placementDates.spanningRange()
    val backupCares = getBackupCaresForChild(childId)

    val desired =
        if (placementSpan == null)
            DateMap.empty() // no placements -> we shouldn't have backup cares
        else {
            // Invalid backup care ranges are either 1. outside placements completely, or 2. in gaps
            // between placements
            val backupCareDates = DateMap.of(backupCares.asSequence().map { it.period to it.id })
            val backupCareSpan = backupCareDates.spanningRange() ?: return
            val outsidePlacements = backupCareSpan - placementSpan
            backupCareDates - outsidePlacements - placementDates.gaps()
        }

    for (backupCare in backupCares) {
        deleteBackupCare(backupCare.id)
    }
    for ((id, newRanges) in desired.transpose()) {
        val backupCare = backupCares.find { it.id == id }!!
        for (range in newRanges.ranges()) {
            createBackupCare(
                childId,
                NewBackupCare(
                    unitId = backupCare.unit.id,
                    groupId = backupCare.group?.id,
                    period = range
                )
            )
        }
    }
}
