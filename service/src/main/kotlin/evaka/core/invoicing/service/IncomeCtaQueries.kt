// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.invoicing.service

import evaka.core.shared.IncomeNotificationId
import evaka.core.shared.PersonId
import evaka.core.shared.db.Database
import evaka.core.shared.domain.HelsinkiDateTime
import java.time.LocalDate

fun Database.Read.getActiveIncompleteIncomeReminderCta(
    personId: PersonId,
    now: HelsinkiDateTime,
): IncomeNotificationId? {
    val today = now.toLocalDate()
    return createQuery {
        sql(
            """
SELECT n.id
FROM income_notification n
WHERE n.receiver_id = ${bind(personId)}
  AND n.notification_type = 'INCOMPLETE_INCOME_REMINDER'
  AND n.created > ${bind(now.minusWeeks(4))}
  AND EXISTS (
      SELECT FROM income i
      WHERE i.person_id = n.receiver_id
        AND i.effect = 'INCOMPLETE'
        AND i.valid_from <= ${bind(today)}
        AND i.valid_to IS NULL
  )
  AND NOT EXISTS (
      SELECT FROM income_statement s
      WHERE s.person_id = n.receiver_id
        AND s.status IN ('SENT', 'HANDLING')
        AND s.sent_at > ${bind(today)} - INTERVAL '12 months'
  )
  AND NOT EXISTS (SELECT FROM income_cta_dismissal d WHERE d.income_notification_id = n.id)
ORDER BY n.created DESC
LIMIT 1
"""
        )
    }
        .exactlyOneOrNull<IncomeNotificationId>()
}

fun Database.Read.getDismissedIncomeExpirationDates(personId: PersonId): Set<LocalDate> =
    createQuery {
        sql(
            """
SELECT income_expiration_date
FROM income_cta_dismissal
WHERE person_id = ${bind(personId)} AND income_expiration_date IS NOT NULL
"""
        )
    }
    .toSet<LocalDate>()

fun Database.Transaction.dismissExpiringIncomeCta(personId: PersonId, expirationDate: LocalDate) {
    execute {
        sql(
            """
INSERT INTO income_cta_dismissal (person_id, income_expiration_date)
VALUES (${bind(personId)}, ${bind(expirationDate)})
ON CONFLICT (person_id, income_expiration_date) DO NOTHING
"""
        )
    }
}

fun Database.Read.isOwnIncompleteIncomeReminder(
    personId: PersonId,
    notificationId: IncomeNotificationId,
): Boolean = createQuery {
    sql(
        """
SELECT EXISTS (
    SELECT FROM income_notification
    WHERE id = ${bind(notificationId)}
      AND receiver_id = ${bind(personId)}
      AND notification_type = 'INCOMPLETE_INCOME_REMINDER'
)
"""
    )
}
    .exactlyOne<Boolean>()

fun Database.Transaction.dismissIncompleteIncomeReminderCta(
    personId: PersonId,
    notificationId: IncomeNotificationId,
) {
    execute {
        sql(
            """
INSERT INTO income_cta_dismissal (person_id, income_notification_id)
VALUES (${bind(personId)}, ${bind(notificationId)})
ON CONFLICT (income_notification_id) DO NOTHING
"""
        )
    }
}
