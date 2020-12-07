// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare

import fi.espoo.evaka.daycare.service.DaycareGroup
import fi.espoo.evaka.shared.db.PGConstants
import fi.espoo.evaka.shared.db.PGConstants.maxDate
import fi.espoo.evaka.shared.db.bindNullable
import fi.espoo.evaka.shared.domain.ClosedPeriod
import fi.espoo.evaka.shared.domain.NotFound
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.kotlin.mapTo
import java.time.LocalDate
import java.util.UUID

private fun Handle.createDaycareGroupQuery(groupId: UUID?, daycareId: UUID?, period: ClosedPeriod?) = createQuery(
    // language=SQL
    """
SELECT
  id, daycare_id, name, start_date, (CASE WHEN end_date < :maxDate THEN end_date END) AS end_date,
  (NOT exists(SELECT 1 FROM backup_care WHERE group_id = daycare_group.id) AND
  NOT exists(SELECT 1 FROM daycare_group_placement WHERE daycare_group_id = daycare_group.id)) AS deletable
FROM daycare_group
WHERE (:groupId::uuid IS NULL OR id = :groupId)
AND (:daycareId::uuid IS NULL OR daycare_id = :daycareId)
AND (:period::daterange IS NULL OR daterange(start_date, end_date, '[]') && :period)
"""
).bind("maxDate", maxDate)
    .bindNullable("groupId", groupId)
    .bindNullable("daycareId", daycareId)
    .bindNullable("period", period)

fun Handle.createDaycareGroup(daycareId: UUID, name: String, startDate: LocalDate): DaycareGroup = createUpdate(
    // language=SQL
    """
INSERT INTO daycare_group (daycare_id, name, start_date)
VALUES (:daycareId, :name, :startDate)
RETURNING id, daycare_id, name, start_date, NULL::date AS end_date, true AS deletable
"""
).bind("daycareId", daycareId)
    .bind("name", name)
    .bind("startDate", startDate)
    .executeAndReturnGeneratedKeys()
    .mapTo<DaycareGroup>()
    .asSequence()
    .first()

fun Handle.updateGroup(groupId: UUID, name: String, startDate: LocalDate, endDate: LocalDate?) {
    // language=SQL
    val sql = "UPDATE daycare_group SET name = :name, start_date = :startDate, end_date = COALESCE(:endDate, 'infinity') WHERE id = :id"
    this.createUpdate(sql)
        .bind("id", groupId)
        .bind("name", name)
        .bind("startDate", startDate)
        .bind("endDate", endDate ?: PGConstants.infinity)
        .execute()
        .let { if (it != 1) throw NotFound("Group $groupId not found") }
}

fun Handle.getDaycareGroup(groupId: UUID): DaycareGroup? =
    createDaycareGroupQuery(groupId = groupId, daycareId = null, period = null)
        .mapTo<DaycareGroup>()
        .asSequence()
        .firstOrNull()

fun Handle.getDaycareGroups(daycareId: UUID, startDate: LocalDate?, endDate: LocalDate?): List<DaycareGroup> =
    createDaycareGroupQuery(
        groupId = null,
        daycareId = daycareId,
        period = ClosedPeriod(startDate ?: LocalDate.of(2000, 1, 1), endDate ?: maxDate)
    )
        .mapTo<DaycareGroup>()
        .asSequence()
        .toList()

fun Handle.deleteDaycareGroup(groupId: UUID) = createUpdate(
    // language=SQL
    """
DELETE FROM daycare_group
WHERE id = :groupId
"""
).bind("groupId", groupId)
    .execute()
