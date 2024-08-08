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
) = createQuery {
    sql(
        """
SELECT
  id, daycare_id, name, start_date, end_date, jamix_customer_number,
  (
    NOT exists(SELECT FROM backup_care WHERE group_id = daycare_group.id) AND
    NOT exists(SELECT FROM daycare_group_placement WHERE daycare_group_id = daycare_group.id) AND
    NOT exists(SELECT FROM staff_attendance WHERE group_id = daycare_group.id) AND
    NOT exists(SELECT FROM staff_attendance_external WHERE group_id = daycare_group.id) AND
    NOT exists(SELECT FROM staff_attendance_realtime WHERE group_id = daycare_group.id) AND
    NOT exists(SELECT FROM group_note WHERE group_id = daycare_group.id) AND
    NOT exists(SELECT FROM daycare_group_acl WHERE daycare_group_id = daycare_group.id) AND
    NOT exists(SELECT FROM calendar_event_attendee WHERE group_id = daycare_group.id) AND
    NOT exists(
        SELECT FROM message_account ma
        WHERE ma.daycare_group_id = daycare_group.id AND (
            exists(SELECT FROM message m WHERE m.sender_id = ma.id) OR
            exists(SELECT FROM message_content mc WHERE mc.author_id = ma.id) OR
            exists(SELECT FROM message_draft md WHERE md.account_id = ma.id) OR
            exists(SELECT FROM message_recipients mr WHERE mr.recipient_id = ma.id) OR
            exists(SELECT FROM message_thread_folder mtf WHERE mtf.owner_id = ma.id) OR
            exists(SELECT FROM message_thread_participant mtp WHERE mtp.participant_id = ma.id)
        )
    )
  ) AS deletable
FROM daycare_group
WHERE (${bind(groupId)}::uuid IS NULL OR id = ${bind(groupId)})
AND (${bind(daycareId)}::uuid IS NULL OR daycare_id = ${bind(daycareId)})
AND (${bind(period)}::daterange IS NULL OR daterange(start_date, end_date, '[]') && ${bind(period)})
"""
    )
}

fun Database.Transaction.createDaycareGroup(
    daycareId: DaycareId,
    name: String,
    startDate: LocalDate
): DaycareGroup =
    createUpdate {
            sql(
                """
INSERT INTO daycare_group (daycare_id, name, start_date, end_date)
VALUES (${bind(daycareId)}, ${bind(name)}, ${bind(startDate)}, NULL)
RETURNING id, daycare_id, name, start_date, end_date, true AS deletable, jamix_customer_number
"""
            )
        }
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

fun Database.Transaction.deleteDaycareGroup(groupId: GroupId) = execute {
    sql(
        """
DELETE FROM message_account WHERE daycare_group_id = ${bind(groupId)};
DELETE FROM daycare_caretaker WHERE group_id = ${bind(groupId)};
DELETE FROM mobile_device_push_group WHERE daycare_group = ${bind(groupId)};
DELETE FROM daycare_group WHERE id = ${bind(groupId)}
"""
    )
}
