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

fun Database.Transaction.upsertFeeAlteration(
    clock: EvakaClock,
    feeAlteration: FeeAlteration
) {
    val now = clock.now()
    val update =
        createUpdate {
            sql(
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
                ${bind(feeAlteration.id)},
                ${bind(feeAlteration.personId)},
                ${bind(feeAlteration.type.toString())}::fee_alteration_type,
                ${bind(feeAlteration.amount)},
                ${bind(feeAlteration.isAbsolute)},
                ${bind(feeAlteration.validFrom)},
                ${bind(feeAlteration.validTo)},
                ${bind(feeAlteration.notes)},
                ${bind(feeAlteration.updatedBy)},
                ${bind(now)}
            ) ON CONFLICT (id) DO UPDATE SET
                type = ${bind(feeAlteration.type.toString())}::fee_alteration_type,
                amount = ${bind(feeAlteration.amount)},
                is_absolute = ${bind(feeAlteration.isAbsolute)},
                valid_from = ${bind(feeAlteration.validFrom)},
                valid_to = ${bind(feeAlteration.validTo)},
                notes = ${bind(feeAlteration.notes)},
                updated_by = ${bind(feeAlteration.updatedBy)},
                updated_at = ${bind(now)}
            """
            )
        }

    handlingExceptions { update.execute() }
}

fun Database.Read.getFeeAlteration(id: FeeAlterationId): FeeAlteration? =
    createQuery {
        sql(
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
            WHERE a.fee_alteration_id = ${bind(id)}
            ORDER BY a.created
        ) s) AS attachments
FROM fee_alteration
WHERE id = ${bind(id)}
"""
        )
    }.exactlyOneOrNull<FeeAlteration>()

fun Database.Read.getFeeAlterationsForPerson(personId: PersonId): List<FeeAlteration> =
    createQuery {
        sql(
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
WHERE person_id = ${bind(personId)}
ORDER BY valid_from DESC, valid_to DESC
"""
        )
    }.toList<FeeAlteration>()

fun Database.Read.getFeeAlterationsFrom(
    personIds: List<ChildId>,
    from: LocalDate
): List<FeeAlteration> {
    if (personIds.isEmpty()) return emptyList()

    return createQuery {
        sql(
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
    person_id = ANY(${bind(personIds)})
    AND (valid_to IS NULL OR valid_to >= ${bind(from)})
"""
        )
    }.toList<FeeAlteration>()
}

fun Database.Transaction.deleteFeeAlteration(id: FeeAlterationId) {
    val update = createUpdate { sql("DELETE FROM fee_alteration WHERE id = ${bind(id)}") }
    handlingExceptions { update.execute() }
}
