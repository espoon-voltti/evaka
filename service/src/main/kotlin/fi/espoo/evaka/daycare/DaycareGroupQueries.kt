// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare

import fi.espoo.evaka.daycare.service.DaycareGroup
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.DateRange
import java.time.LocalDate

private fun Database.Read.createDaycareGroupQuery(
    groupId: GroupId?,
    daycareId: DaycareId?,
    period: DateRange?
) =
    @Suppress("DEPRECATION")
    createQuery(
            // language=SQL
            """
SELECT
  id, daycare_id, name, start_date, end_date, jamix_customer_number,
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
    @Suppress("DEPRECATION")
    createUpdate(
            // language=SQL
            """
INSERT INTO daycare_group (daycare_id, name, start_date, end_date)
VALUES (:daycareId, :name, :startDate, NULL)
RETURNING id, daycare_id, name, start_date, end_date, true AS deletable, jamix_customer_number
"""
        )
        .bind("daycareId", daycareId)
        .bind("name", name)
        .bind("startDate", startDate)
        .executeAndReturnGeneratedKeys()
        .exactlyOne<DaycareGroup>()

fun Database.Transaction.updateGroup(
    groupId: GroupId,
    name: String,
    startDate: LocalDate,
    endDate: LocalDate?,
    jamixCustomerNumber: Int?
) {
    createUpdate {
            sql(
                """
UPDATE daycare_group
SET name = ${bind(name)}, start_date = ${bind(startDate)}, end_date = ${bind(endDate)}, jamix_customer_number = ${bind(jamixCustomerNumber)}
WHERE id = ${bind(groupId)}
        """
            )
        }
        .updateExactlyOne(notFoundMsg = "Group $groupId not found")
}

fun Database.Read.getDaycareGroup(groupId: GroupId): DaycareGroup? =
    createDaycareGroupQuery(groupId = groupId, daycareId = null, period = null)
        .exactlyOneOrNull<DaycareGroup>()

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
        .toList<DaycareGroup>()

fun Database.Transaction.deleteDaycareGroup(groupId: GroupId) =
    @Suppress("DEPRECATION")
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
