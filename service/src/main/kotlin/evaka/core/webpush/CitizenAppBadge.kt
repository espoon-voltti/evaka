// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.webpush

import evaka.core.application.fetchApplicationNotificationCountForCitizen
import evaka.core.children.getCitizenChildIds
import evaka.core.document.childdocument.getUnreadChildDocumentCounts
import evaka.core.messaging.citizenMessageAccountIds
import evaka.core.messaging.getUnreadMessagesCountsCitizen
import evaka.core.pedagogicaldocument.countUnreadDocumentsByUser
import evaka.core.shared.PersonId
import evaka.core.shared.db.Database
import evaka.core.shared.security.actionrule.AccessControlFilter
import java.time.LocalDate

/** Must match the sum of the citizen frontend's menu attention dots, excluding personal details */
fun Database.Read.getCitizenAppBadgeCount(citizenId: PersonId, today: LocalDate): Int {
    val unreadMessages =
        getUnreadMessagesCountsCitizen(
                AccessControlFilter.Some(citizenMessageAccountIds(citizenId))
            )
            .sumOf { it.unreadCount }
    val unreadDecisions = fetchApplicationNotificationCountForCitizen(citizenId, today)
    val unreadPedagogicalDocuments = countUnreadDocumentsByUser(today, citizenId).values.sum()
    val unreadChildDocuments =
        getUnreadChildDocumentCounts(citizenId, getCitizenChildIds(today, citizenId)).values.sum()
    return unreadMessages + unreadDecisions + unreadPedagogicalDocuments + unreadChildDocuments
}

fun pushAppBadge(count: Int): Int = maxOf(1, count)
