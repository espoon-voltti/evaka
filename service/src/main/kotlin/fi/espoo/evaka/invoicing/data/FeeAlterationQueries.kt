// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.data

import fi.espoo.evaka.invoicing.domain.FeeAlteration
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.FeeAlterationId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import java.time.LocalDate

fun Database.Transaction.upsertFeeAlteration(clock: EvakaClock, feeAlteration: FeeAlteration) {
    val sql =
        """
            INSERT INTO fee_alteration (
                id,
                person_id,
                type,
                amount,
                is_absolute,
                valid_from,
                valid_to,
                notes,
                updated_by,
                updated_at
            ) VALUES (
                :id,
                :person_id,
                :type::fee_alteration_type,
                :amount,
                :is_absolute,
                :valid_from,
                :valid_to,
                :notes,
                :updated_by,
                :now
            ) ON CONFLICT (id) DO UPDATE SET
                type = :type::fee_alteration_type,
                amount = :amount,
                is_absolute = :is_absolute,
                valid_from = :valid_from,
                valid_to = :valid_to,
                notes = :notes,
                updated_by = :updated_by,
                updated_at = :now
        """

    val update =
        createUpdate(sql)
            .bind("now", clock.now())
            .bind("id", feeAlteration.id)
            .bind("person_id", feeAlteration.personId)
            .bind("type", feeAlteration.type.toString())
            .bind("amount", feeAlteration.amount)
            .bind("is_absolute", feeAlteration.isAbsolute)
            .bind("valid_from", feeAlteration.validFrom)
            .bind("valid_to", feeAlteration.validTo)
            .bind("notes", feeAlteration.notes)
            .bind("updated_by", feeAlteration.updatedBy)

    handlingExceptions { update.execute() }
}

fun Database.Read.getFeeAlteration(id: FeeAlterationId): FeeAlteration? {
    return createQuery(
            """
SELECT
    id,
    person_id,
    type,
    amount,
    is_absolute,
    valid_from,
    valid_to,
    notes,
    updated_at,
    updated_by
FROM fee_alteration
WHERE id = :id
        """.trimIndent(
            )
        )
        .bind("id", id)
        .mapTo<FeeAlteration>()
        .firstOrNull()
}

fun Database.Read.getFeeAlterationsForPerson(personId: PersonId): List<FeeAlteration> {
    return createQuery(
            """
SELECT
    id,
    person_id,
    type,
    amount,
    is_absolute,
    valid_from,
    valid_to,
    notes,
    updated_at,
    updated_by
FROM fee_alteration
WHERE person_id = :personId
ORDER BY valid_from DESC, valid_to DESC
        """.trimIndent(
            )
        )
        .bind("personId", personId)
        .mapTo<FeeAlteration>()
        .toList()
}

fun Database.Read.getFeeAlterationsFrom(
    personIds: List<ChildId>,
    from: LocalDate
): List<FeeAlteration> {
    if (personIds.isEmpty()) return emptyList()

    return createQuery(
            """
SELECT
    id,
    person_id,
    type,
    amount,
    is_absolute,
    valid_from,
    valid_to,
    notes,
    updated_at,
    updated_by
FROM fee_alteration
WHERE
    person_id = ANY(:personIds)
    AND (valid_to IS NULL OR valid_to >= :from)
        """.trimIndent(
            )
        )
        .bind("personIds", personIds)
        .bind("from", from)
        .mapTo<FeeAlteration>()
        .toList()
}

fun Database.Transaction.deleteFeeAlteration(id: FeeAlterationId) {
    val update = createUpdate("DELETE FROM fee_alteration WHERE id = :id").bind("id", id)

    handlingExceptions { update.execute() }
}
