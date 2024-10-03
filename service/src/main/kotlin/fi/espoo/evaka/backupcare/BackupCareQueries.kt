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
    createQuery {
            sql(
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
WHERE child_id = ${bind(childId)}
"""
            )
        }
        .toList<ChildBackupCare>()

fun Database.Read.getBackupCaresForDaycare(
    daycareId: DaycareId,
    period: FiniteDateRange,
): List<UnitBackupCare> =
    createQuery {
            sql(
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
JOIN LATERAL (
  SELECT
    coalesce(sum(days_in_range(daterange(bc.start_date, bc.end_date, '[]') * daterange('2020-03-01', NULL) * daterange(sn.start_date, sn.end_date, '[]'))), 0) AS days_with_service_need
  FROM backup_care bc
  LEFT JOIN placement pl ON bc.child_id = pl.child_id AND daterange(bc.start_date, bc.end_date, '[]') && daterange(pl.start_date, pl.end_date, '[]')
  LEFT JOIN service_need sn ON pl.id = sn.placement_id
  WHERE bc.id = backup_care.id
) AS service_need_stats ON true
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
WHERE unit_id = ${bind(daycareId)}
AND daterange(backup_care.start_date, backup_care.end_date, '[]') && ${bind(period)}
"""
            )
        }
        .toList<UnitBackupCare>()

fun Database.Read.getBackupCareChildrenInGroup(
    daycareId: DaycareId,
    groupId: GroupId,
    period: FiniteDateRange,
): List<ChildId> =
    createQuery {
            sql(
                """
SELECT child_id FROM backup_care
WHERE unit_id = ${bind(daycareId)}
  AND group_id = ${bind(groupId)}
  AND daterange(start_date, end_date, '[]') && ${bind(period)}
"""
            )
        }
        .toList<ChildId>()

data class BackupCareInfo(val childId: ChildId, val unitId: DaycareId, val period: FiniteDateRange)

fun Database.Read.getBackupCare(id: BackupCareId): BackupCareInfo? =
    createQuery {
            sql(
                """
SELECT child_id, unit_id, daterange(start_date, end_date, '[]') period FROM backup_care
WHERE id = ${bind(id)}
"""
            )
        }
        .exactlyOneOrNull<BackupCareInfo>()

fun Database.Transaction.createBackupCare(
    childId: ChildId,
    backupCare: NewBackupCare,
): BackupCareId =
    createUpdate {
            sql(
                """
INSERT INTO backup_care (child_id, unit_id, group_id, start_date, end_date)
VALUES (
  ${bind(childId)},
  ${bind(backupCare.unitId)},
  ${bind(backupCare.groupId)},
  ${bind(backupCare.period.start)},
  ${bind(backupCare.period.end)}
)
RETURNING id
"""
            )
        }
        .executeAndReturnGeneratedKeys()
        .exactlyOne<BackupCareId>()

fun Database.Transaction.updateBackupCare(
    id: BackupCareId,
    period: FiniteDateRange,
    groupId: GroupId?,
) = execute {
    sql(
        """
UPDATE backup_care
SET
  start_date = ${bind(period.start)},
  end_date = ${bind(period.end)},
  group_id = ${bind(groupId)}
WHERE id = ${bind(id)}
"""
    )
}

fun Database.Transaction.deleteBackupCare(id: BackupCareId) = execute {
    sql(
        """
DELETE FROM backup_care
WHERE id = ${bind(id)}
"""
    )
}

fun Database.Read.getBackupCareChildId(id: BackupCareId): ChildId =
    createQuery {
            sql(
                """
SELECT child_id FROM backup_care WHERE id = ${bind(id)}
"""
            )
        }
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
                    period = range,
                ),
            )
        }
    }
}
