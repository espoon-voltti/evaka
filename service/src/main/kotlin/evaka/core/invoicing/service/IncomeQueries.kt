// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.invoicing.service

import evaka.core.placement.PlacementType
import evaka.core.shared.IncomeNotificationId
import evaka.core.shared.PersonId
import evaka.core.shared.db.Database
import evaka.core.shared.db.DatabaseEnum
import evaka.core.shared.db.PredicateSql
import evaka.core.shared.domain.FiniteDateRange
import evaka.core.shared.domain.HelsinkiDateTime
import java.time.LocalDate

fun Database.Read.personHasActiveIncomeOnDate(personId: PersonId, theDate: LocalDate): Boolean {
    return createQuery {
        sql(
            """
                SELECT 1
                FROM income
                WHERE daterange(valid_from, valid_to, '[]') @> ${bind(theDate)}
                    AND person_id = ${bind(personId)}
                """
        )
    }
        .toList<Int>()
        .isNotEmpty()
}

enum class IncomeNotificationType : DatabaseEnum {
    INITIAL_EMAIL,
    REMINDER_EMAIL,
    EXPIRED_EMAIL,
    NEW_CUSTOMER,
    INCOMPLETE_INCOME_REMINDER;

    override val sqlType: String = "income_notification_type"
}

data class PersonIncomeExpirationDate(val personId: PersonId, val expirationDate: LocalDate)

fun Database.Read.expiringIncomes(
    today: LocalDate,
    checkForExpirationRange: FiniteDateRange,
    checkForExistingRecentIncomeNotificationType: IncomeNotificationType? = null,
    aPersonId: PersonId? = null,
): List<PersonIncomeExpirationDate> {
    val dayAfterExpiration = checkForExpirationRange.end.plusDays(1)
    return createQuery {
        sql(
            """
WITH latest_income AS (
    SELECT DISTINCT ON (person_id)
    id, person_id, valid_to
    FROM income i 
    ORDER BY person_id, valid_to DESC
), expiring_income_with_billable_placement_day_after_expiration AS (
    SELECT DISTINCT i.person_id, i.valid_to
    FROM placement pl
        JOIN service_need sn ON pl.id = sn.placement_id AND daterange(sn.start_date, sn.end_date, '[]') @> ${bind(dayAfterExpiration)}
        JOIN service_need_option sno ON sn.option_id = sno.id AND sno.fee_coefficient > 0
    JOIN daycare u ON u.id = pl.unit_id
    
    -- head of child
    JOIN fridge_child fc_head ON (
        fc_head.child_id = pl.child_id AND
        ${bind(today)} BETWEEN fc_head.start_date AND fc_head.end_date AND fc_head.conflict = false
    )

    -- spouse of the head of child
    LEFT JOIN fridge_partner fp ON fp.person_id = fc_head.head_of_child AND daterange(fp.start_date, fp.end_date, '[]') @> ${bind(today)} AND fp.conflict = false
    LEFT JOIN fridge_partner fp_spouse ON (
        fp_spouse.partnership_id = fp.partnership_id AND
        fp_spouse.person_id <> fp.person_id AND
        daterange(fp_spouse.start_date, fp_spouse.end_date, '[]') @> ${bind(today)} AND fp_spouse.conflict = false
    )
    JOIN latest_income i ON i.person_id = fc_head.head_of_child OR i.person_id = fp_spouse.person_id
    WHERE between_start_and_end(${bind(checkForExpirationRange.asDateRange())}, i.valid_to)
     AND (i.valid_to + INTERVAL '1 day')::date BETWEEN pl.start_date AND pl.end_date
     AND (u.invoiced_by_municipality OR u.provider_type = 'PRIVATE_SERVICE_VOUCHER')
)
SELECT person_id, valid_to AS expiration_date
FROM expiring_income_with_billable_placement_day_after_expiration expiring_income 
WHERE NOT EXISTS (
    SELECT 1 FROM income_statement
    WHERE person_id = expiring_income.person_id
        AND (status = 'SENT'::income_statement_status OR status = 'HANDLING'::income_statement_status)
        AND sent_at > ${bind(today)} - INTERVAL '12 months'
        AND (end_date IS NULL OR ${bind(dayAfterExpiration)} <= end_date)
    
) 
${if (checkForExistingRecentIncomeNotificationType != null) """AND NOT EXISTS (
    SELECT 1 FROM income_notification
    WHERE receiver_id = expiring_income.person_id AND notification_type = ${bind(checkForExistingRecentIncomeNotificationType)}
    AND created > ${bind(today)} - INTERVAL '1 month'
)""" else ""}                
${if (aPersonId != null) " AND person_id = ${bind(aPersonId)}" else ""}
"""
        )
    }
        .toList<PersonIncomeExpirationDate>()
}

fun Database.Read.newCustomerIdsForIncomeNotifications(
    today: LocalDate,
    guardianId: PersonId?,
): List<PersonId> {
    val currentMonth = FiniteDateRange.ofMonth(today)

    val guardianPredicate =
        if (guardianId != null) {
            PredicateSql {
                where(
                    "fc_head.head_of_child = ${bind(guardianId)} OR fp_spouse.person_id = ${bind(guardianId)}"
                )
            }
        } else {
            PredicateSql.alwaysTrue()
        }

    return createQuery {
        sql(
            """
WITH previously_placed_children AS (
    SELECT pl.child_id, fc.head_of_child
    FROM placement pl
    JOIN fridge_child fc ON pl.child_id = fc.child_id AND ${bind(today)} BETWEEN fc.start_date AND fc.end_date
    WHERE
        pl.start_date < ${bind(currentMonth.start)} AND
        pl.created_at < ${bind(today)} - INTERVAL '1 month' AND
        pl.type = ANY(${bind(PlacementType.invoiced)})
), fridge_parents AS (
    SELECT fc_head.head_of_child AS parent_id, fp_spouse.person_id AS spouse_id
    FROM placement pl
    
    -- placement must be billable
        JOIN service_need sn ON pl.id = sn.placement_id AND daterange(sn.start_date, sn.end_date, '[]') @> pl.start_date
        JOIN service_need_option sno ON sn.option_id = sno.id AND sno.fee_coefficient > 0
    
    -- head of child
    JOIN fridge_child fc_head ON (
        fc_head.child_id = pl.child_id AND
        ${bind(today)} BETWEEN fc_head.start_date AND fc_head.end_date AND fc_head.conflict = false
    )
    
    -- spouse of the head of child
    LEFT JOIN fridge_partner fp ON fp.person_id = fc_head.head_of_child AND daterange(fp.start_date, fp.end_date, '[]') @> ${bind(today)} AND fp.conflict = false
    LEFT JOIN fridge_partner fp_spouse ON (
        fp_spouse.partnership_id = fp.partnership_id AND
        fp_spouse.person_id <> fp.person_id AND
        daterange(fp_spouse.start_date, fp_spouse.end_date, '[]') @> ${bind(today)} AND fp_spouse.conflict = false
    )
    WHERE
        (
            (pl.start_date BETWEEN ${bind(currentMonth.start)} AND ${bind(currentMonth.end)}) OR
            (
                pl.created_at >= ${bind(today)} - INTERVAL '1 month' AND
                pl.start_date < ${bind(currentMonth.start)} AND
                pl.end_date >= ${bind(today)}
            )
        ) AND
        pl.type = ANY(${bind(PlacementType.invoiced)}) AND
        NOT EXISTS(
            SELECT 1
            FROM previously_placed_children
            WHERE child_id != pl.child_id
            AND (head_of_child = fc_head.head_of_child OR head_of_child = fp_spouse.person_id)
        ) AND
        NOT EXISTS(
            SELECT 1
            FROM income i
            WHERE (i.person_id = fc_head.head_of_child OR i.person_id = fp_spouse.person_id)
            AND (coalesce(i.valid_to, 'infinity') >= pl.end_date OR (i.valid_to < pl.end_date AND i.valid_to > (${bind(today)} + INTERVAL '4 weeks')))
        ) AND
        ${predicate(guardianPredicate)}
)
SELECT DISTINCT person_id FROM (
    SELECT parent_id AS person_id 
    FROM fridge_parents
    UNION ALL
    SELECT spouse_id AS person_id
    FROM fridge_parents
    WHERE spouse_id IS NOT NULL
) AS parent
WHERE NOT EXISTS (
    SELECT 1 FROM income_statement
    WHERE person_id = parent.person_id
      AND (status = 'SENT'::income_statement_status OR status = 'HANDLING'::income_statement_status)
      AND sent_at > ${bind(today)} - INTERVAL '12 months'
)
AND NOT EXISTS (
    SELECT 1 FROM income_notification
    WHERE receiver_id = parent.person_id
      AND notification_type = 'NEW_CUSTOMER'::income_notification_type
      AND created > ${bind(today)} - INTERVAL '1 month'
)
"""
        )
    }
        .toList<PersonId>()
}

fun Database.Read.incompleteIncomeReminderRecipients(
    today: LocalDate,
    personId: PersonId? = null,
): List<PersonId> {
    val personPredicate =
        if (personId != null) PredicateSql { where("i.person_id = ${bind(personId)}") }
        else PredicateSql.alwaysTrue()

    return createQuery {
        sql(
            """
WITH long_incomplete_income AS (
    SELECT i.person_id
    FROM income i
    WHERE i.effect = 'INCOMPLETE'
      AND i.valid_from <= ${bind(today.minusYears(1))}
      -- a finite INCOMPLETE income usually means the next income has already been entered,
      -- and its end is covered by the expiring income notifications
      AND i.valid_to IS NULL
      AND ${predicate(personPredicate)}
), billable_family_adult AS (
    SELECT DISTINCT adult.id AS person_id
    FROM placement pl
    JOIN daycare u ON u.id = pl.unit_id
    LEFT JOIN service_need_option default_sno ON default_sno.valid_placement_type = pl.type AND default_sno.default_option
    JOIN fridge_child fc_head ON fc_head.child_id = pl.child_id
        AND daterange(fc_head.start_date, fc_head.end_date, '[]') @> ${bind(today)}
        AND fc_head.conflict = false
    LEFT JOIN fridge_partner fp ON fp.person_id = fc_head.head_of_child
        AND daterange(fp.start_date, fp.end_date, '[]') @> ${bind(today)}
        AND fp.conflict = false
    LEFT JOIN fridge_partner fp_partner ON fp_partner.partnership_id = fp.partnership_id
        AND fp_partner.person_id <> fp.person_id
        AND daterange(fp_partner.start_date, fp_partner.end_date, '[]') @> ${bind(today)}
        AND fp_partner.conflict = false
    -- one row per adult (head and possible partner), each paired with the other adult for the max-fee check
    CROSS JOIN LATERAL (VALUES (fc_head.head_of_child, fp_partner.person_id), (fp_partner.person_id, fc_head.head_of_child)) AS adult(id, other_id)
    WHERE adult.id IN (SELECT person_id FROM long_incomplete_income)
      AND daterange(pl.start_date, pl.end_date, '[]') @> ${bind(today)}
      AND (u.invoiced_by_municipality OR u.provider_type = 'PRIVATE_SERVICE_VOUCHER')
      -- placement is paid if its current service need has a fee,
      -- or it has no current service need and the placement type's default option is paid
      AND COALESCE(
          (
              SELECT bool_or(sno.fee_coefficient > 0)
              FROM service_need sn
              JOIN service_need_option sno ON sno.id = sn.option_id
              WHERE sn.placement_id = pl.id
                AND daterange(sn.start_date, sn.end_date, '[]') @> ${bind(today)}
          ),
          default_sno.fee_coefficient > 0
      )
      AND NOT EXISTS (
          SELECT FROM income other_income
          WHERE other_income.person_id = adult.other_id
            AND other_income.effect = 'MAX_FEE_ACCEPTED'
            AND daterange(other_income.valid_from, other_income.valid_to, '[]') @> ${bind(today)}
      )
)
SELECT a.person_id
FROM billable_family_adult a
WHERE NOT EXISTS (
    SELECT FROM income_statement s
    WHERE s.person_id = a.person_id
      AND s.status IN ('SENT', 'HANDLING')
      AND s.sent_at > ${bind(today)} - INTERVAL '12 months'
)
AND NOT EXISTS (
    SELECT FROM income_notification n
    WHERE n.receiver_id = a.person_id
      AND n.notification_type = 'INCOMPLETE_INCOME_REMINDER'
      AND (n.created AT TIME ZONE 'Europe/Helsinki')::date > ${bind(today.minusYears(1))}
)
"""
        )
    }
        .toList<PersonId>()
}

data class IncomeNotification(
    val receiverId: PersonId,
    val notificationType: IncomeNotificationType,
    val created: HelsinkiDateTime,
)

@IgnorableReturnValue
fun Database.Transaction.createIncomeNotification(
    receiverId: PersonId,
    notificationType: IncomeNotificationType,
    created: HelsinkiDateTime? = null,
): IncomeNotificationId {
    return createUpdate {
        sql(
            """
INSERT INTO income_notification(receiver_id, notification_type, created)
VALUES (${bind(receiverId)}, ${bind(notificationType)}, coalesce(${bind(created)}, now()))
RETURNING id
"""
        )
    }
        .executeAndReturnGeneratedKeys()
        .exactlyOne<IncomeNotificationId>()
}

fun Database.Read.getIncomeNotifications(receiverId: PersonId): List<IncomeNotification> =
    createQuery {
        sql(
            "SELECT receiver_id, notification_type, created FROM income_notification WHERE receiver_id = ${bind(receiverId)}"
        )
    }
    .toList<IncomeNotification>()
