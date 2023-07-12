// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.childdiscussion

import fi.espoo.evaka.shared.ChildDiscussionId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.db.Database

fun Database.Read.getChildDiscussionDataForChild(childId: ChildId): ChildDiscussion? {
    // language=sql
    val sql =
        """
        SELECT id, child_id, offered_date, held_date, counseling_date
        FROM child_discussion
        WHERE child_id = :childId
        """
            .trimIndent()
    return createQuery(sql).bind("childId", childId).mapTo<ChildDiscussion>().firstOrNull()
}

fun Database.Transaction.createChildDiscussion(
    childId: ChildId,
    dto: ChildDiscussionBody
): ChildDiscussionId {
    // language=sql
    val sql =
        """
        INSERT INTO child_discussion (child_id, offered_date, held_date, counseling_date)
        VALUES(:childId, :offeredDate, :heldDate, :counselingDate)
        RETURNING id
        """
            .trimIndent()
    return createUpdate(sql)
        .bind("childId", childId)
        .bindKotlin(dto)
        .executeAndReturnGeneratedKeys()
        .mapTo<ChildDiscussionId>()
        .one()
}

fun Database.Transaction.updateChildDiscussion(childId: ChildId, dto: ChildDiscussionBody) {
    // language=sql
    val sql =
        """
        UPDATE child_discussion SET 
            offered_date = :offeredDate, 
            held_date = :heldDate, 
            counseling_date = :counselingDate
        WHERE child_id = :childId
        RETURNING *
        """
            .trimIndent()

    createUpdate(sql).bind("childId", childId).bindKotlin(dto).updateExactlyOne()
}
