// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.backupcare

import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.bindNullable
import fi.espoo.evaka.shared.domain.FiniteDateRange
import org.jdbi.v3.core.kotlin.mapTo
import java.util.UUID

fun Database.Read.getBackupCaresForChild(childId: UUID): List<ChildBackupCare> = createQuery(
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
    .mapTo<ChildBackupCare>()
    .list()

fun Database.Read.getBackupCaresForDaycare(daycareId: UUID, period: FiniteDateRange): List<UnitBackupCare> = createQuery(
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
WHERE unit_id = :daycareId
AND daterange(backup_care.start_date, backup_care.end_date, '[]') && :period
"""
)
    .bind("daycareId", daycareId)
    .bind("period", period)
    .mapTo<UnitBackupCare>()
    .list()

fun Database.Transaction.createBackupCare(childId: UUID, backupCare: NewBackupCare): UUID = createUpdate(
    // language=SQL
    """
INSERT INTO backup_care (child_id, unit_id, group_id, start_date, end_date)
VALUES (:childId, :unitId, :groupId, :start, :end)
RETURNING id
"""
)
    .bind("childId", childId)
    .bind("unitId", backupCare.unitId)
    .bindNullable("groupId", backupCare.groupId)
    .bind("start", backupCare.period.start)
    .bind("end", backupCare.period.end)
    .executeAndReturnGeneratedKeys()
    .mapTo<UUID>()
    .one()

fun Database.Transaction.updateBackupCare(id: UUID, period: FiniteDateRange, groupId: UUID?) = createUpdate(
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
    .bindNullable("groupId", groupId)
    .execute()

fun Database.Transaction.deleteBackupCare(id: UUID) = createUpdate(
    // language=SQL
    """
DELETE FROM backup_care
WHERE id = :id
"""
).bind("id", id)
    .execute()
