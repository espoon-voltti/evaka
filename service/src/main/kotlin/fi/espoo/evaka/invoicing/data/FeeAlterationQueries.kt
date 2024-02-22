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
        @Suppress("DEPRECATION")
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
    @Suppress("DEPRECATION")
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
    updated_by,
    (SELECT coalesce(jsonb_agg(json_build_object(
            'id', id,
            'name', name,
            'contentType', content_type
          )), '[]'::jsonb) FROM (
            SELECT a.id, a.name, a.content_type
            FROM attachment a
            WHERE a.fee_alteration_id = :id
            ORDER BY a.created
        ) s) AS attachments
FROM fee_alteration
WHERE id = :id
        """
                .trimIndent()
        )
        .bind("id", id)
        .exactlyOneOrNull<FeeAlteration>()
}

fun Database.Read.getFeeAlterationsForPerson(personId: PersonId): List<FeeAlteration> {
    @Suppress("DEPRECATION")
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
    updated_by,
    (SELECT coalesce(jsonb_agg(json_build_object(
            'id', id,
            'name', name,
            'contentType', content_type
          )), '[]'::jsonb) FROM (
            SELECT a.id, a.name, a.content_type
            FROM attachment a
            WHERE a.fee_alteration_id = fee_alteration.id
            ORDER BY a.created
        ) s) AS attachments
FROM fee_alteration
WHERE person_id = :personId
ORDER BY valid_from DESC, valid_to DESC
        """
                .trimIndent()
        )
        .bind("personId", personId)
        .toList<FeeAlteration>()
}

fun Database.Read.getFeeAlterationsFrom(
    personIds: List<ChildId>,
    from: LocalDate
): List<FeeAlteration> {
    if (personIds.isEmpty()) return emptyList()

    @Suppress("DEPRECATION")
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
    updated_by,
    '[]' as attachments
FROM fee_alteration
WHERE
    person_id = ANY(:personIds)
    AND (valid_to IS NULL OR valid_to >= :from)
        """
                .trimIndent()
        )
        .bind("personIds", personIds)
        .bind("from", from)
        .toList<FeeAlteration>()
}

fun Database.Transaction.deleteFeeAlteration(id: FeeAlterationId) {
    @Suppress("DEPRECATION")
    val update = createUpdate("DELETE FROM fee_alteration WHERE id = :id").bind("id", id)

    handlingExceptions { update.execute() }
}
