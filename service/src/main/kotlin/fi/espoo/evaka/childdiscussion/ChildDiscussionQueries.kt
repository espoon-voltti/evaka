// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.childdiscussion

import fi.espoo.evaka.shared.ChildDiscussionId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.db.Database

fun Database.Read.getChildDiscussionById(id: ChildDiscussionId): ChildDiscussionData? {
    // language=sql
    val sql =
        """
        SELECT id, child_id, offered_date, held_date, counseling_date
        FROM child_discussion
        WHERE id = :id
        """
            .trimIndent()
    return createQuery(sql).bind("id", id).mapTo<ChildDiscussionData>().firstOrNull()
}

fun Database.Read.getChildDiscussions(childId: ChildId): List<ChildDiscussionData> {
    val sql =
        """
        SELECT id, child_id, offered_date, held_date, counseling_date
        FROM child_discussion
        WHERE child_id = :childId
        ORDER BY created DESC
        """
            .trimIndent()
    return createQuery(sql).bind("childId", childId).mapTo<ChildDiscussionData>().toList()
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
        .exactlyOne()
}

fun Database.Transaction.updateChildDiscussion(id: ChildDiscussionId, dto: ChildDiscussionBody) {
    // language=sql
    val sql =
        """
        UPDATE child_discussion SET 
            offered_date = :offeredDate, 
            held_date = :heldDate, 
            counseling_date = :counselingDate
        WHERE id = :id
        """
            .trimIndent()

    createUpdate(sql).bind("id", id).bindKotlin(dto).updateExactlyOne()
}

fun Database.Transaction.deleteChildDiscussion(id: ChildDiscussionId) {
    // language=sql
    val sql =
        """
        DELETE FROM child_discussion
        WHERE id = :id 
        """
            .trimIndent()

    createUpdate(sql).bind("id", id).updateExactlyOne()
}
