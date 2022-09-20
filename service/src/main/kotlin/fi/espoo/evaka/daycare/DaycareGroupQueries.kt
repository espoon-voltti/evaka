// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare

import fi.espoo.evaka.daycare.service.DaycareGroup
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.NotFound
import java.time.LocalDate

private fun Database.Read.createDaycareGroupQuery(
    groupId: GroupId?,
    daycareId: DaycareId?,
    period: DateRange?
) =
    createQuery(
            // language=SQL
            """
SELECT
  id, daycare_id, name, start_date, end_date,
  (NOT exists(SELECT 1 FROM backup_care WHERE group_id = daycare_group.id) AND
  NOT exists(SELECT 1 FROM daycare_group_placement WHERE daycare_group_id = daycare_group.id)) AS deletable
FROM daycare_group
WHERE (:groupId::uuid IS NULL OR id = :groupId)
AND (:daycareId::uuid IS NULL OR daycare_id = :daycareId)
AND (:period::daterange IS NULL OR daterange(start_date, end_date, '[]') && :period)
"""
        )
        .bind("groupId", groupId)
        .bind("daycareId", daycareId)
        .bind("period", period)

fun Database.Transaction.createDaycareGroup(
    daycareId: DaycareId,
    name: String,
    startDate: LocalDate
): DaycareGroup =
    createUpdate(
            // language=SQL
            """
INSERT INTO daycare_group (daycare_id, name, start_date, end_date)
VALUES (:daycareId, :name, :startDate, NULL)
RETURNING id, daycare_id, name, start_date, end_date, true AS deletable
"""
        )
        .bind("daycareId", daycareId)
        .bind("name", name)
        .bind("startDate", startDate)
        .executeAndReturnGeneratedKeys()
        .mapTo<DaycareGroup>()
        .asSequence()
        .first()

fun Database.Transaction.updateGroup(
    groupId: GroupId,
    name: String,
    startDate: LocalDate,
    endDate: LocalDate?
) {
    // language=SQL
    val sql =
        "UPDATE daycare_group SET name = :name, start_date = :startDate, end_date = :endDate WHERE id = :id"
    this.createUpdate(sql)
        .bind("id", groupId)
        .bind("name", name)
        .bind("startDate", startDate)
        .bind("endDate", endDate)
        .execute()
        .let { if (it != 1) throw NotFound("Group $groupId not found") }
}

fun Database.Read.getDaycareGroup(groupId: GroupId): DaycareGroup? =
    createDaycareGroupQuery(groupId = groupId, daycareId = null, period = null)
        .mapTo<DaycareGroup>()
        .asSequence()
        .firstOrNull()

fun Database.Read.getDaycareGroups(
    daycareId: DaycareId,
    startDate: LocalDate?,
    endDate: LocalDate?
): List<DaycareGroup> =
    createDaycareGroupQuery(
            groupId = null,
            daycareId = daycareId,
            period = DateRange(startDate ?: LocalDate.of(2000, 1, 1), endDate)
        )
        .mapTo<DaycareGroup>()
        .asSequence()
        .toList()

fun Database.Transaction.deleteDaycareGroup(groupId: GroupId) =
    createUpdate(
            // language=SQL
            """
DELETE FROM group_note WHERE group_id = :groupId;        
DELETE FROM daycare_group
WHERE id = :groupId
"""
        )
        .bind("groupId", groupId)
        .execute()
