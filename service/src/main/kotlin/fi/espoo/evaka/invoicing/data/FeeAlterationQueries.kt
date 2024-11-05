// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.data

import fi.espoo.evaka.invoicing.domain.FeeAlteration
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.FeeAlterationId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import java.time.LocalDate

fun Database.Transaction.upsertFeeAlteration(
    clock: EvakaClock,
    modifiedBy: EvakaUserId,
    feeAlteration: FeeAlteration,
) {
    val now = clock.now()
    val update = createUpdate {
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
                modified_by,
                modified_at
            ) VALUES (
                ${bind(feeAlteration.id)},
                ${bind(feeAlteration.personId)},
                ${bind(feeAlteration.type.toString())}::fee_alteration_type,
                ${bind(feeAlteration.amount)},
                ${bind(feeAlteration.isAbsolute)},
                ${bind(feeAlteration.validFrom)},
                ${bind(feeAlteration.validTo)},
                ${bind(feeAlteration.notes)},
                ${bind(modifiedBy)},
                ${bind(now)}
            ) ON CONFLICT (id) DO UPDATE SET
                type = ${bind(feeAlteration.type.toString())}::fee_alteration_type,
                amount = ${bind(feeAlteration.amount)},
                is_absolute = ${bind(feeAlteration.isAbsolute)},
                valid_from = ${bind(feeAlteration.validFrom)},
                valid_to = ${bind(feeAlteration.validTo)},
                notes = ${bind(feeAlteration.notes)},
                modified_by = ${bind(modifiedBy)},
                modified_at = ${bind(now)}
            """
        )
    }

    handlingExceptions { update.execute() }
}

fun Database.Read.getFeeAlteration(id: FeeAlterationId): FeeAlteration? {
    return createQuery {
            sql(
                """
SELECT
    fa.id,
    fa.person_id,
    fa.type,
    fa.amount,
    fa.is_absolute,
    fa.valid_from,
    fa.valid_to,
    fa.notes,
    fa.modified_at,
    e.id AS modified_by_id,
    e.name AS modified_by_name,
    e.type AS modified_by_type,
    (SELECT coalesce(jsonb_agg(jsonb_build_object(
            'id', s.id,
            'name', s.name,
            'contentType', s.content_type
          )), '[]'::jsonb) FROM (
            SELECT a.id, a.name, a.content_type
            FROM attachment a
            WHERE a.fee_alteration_id = ${bind(id)}
            ORDER BY a.created
        ) s) AS attachments
FROM fee_alteration fa
LEFT JOIN evaka_user e ON fa.modified_by = e.id
WHERE fa.id = ${bind(id)}
"""
            )
        }
        .exactlyOneOrNull<FeeAlteration>()
}

fun Database.Read.getFeeAlterationsForPerson(personId: PersonId): List<FeeAlteration> {
    return createQuery {
            sql(
                """
SELECT
    fa.id,
    fa.person_id,
    fa.type,
    fa.amount,
    fa.is_absolute,
    fa.valid_from,
    fa.valid_to,
    fa.notes,
    fa.modified_at,
    e.id AS modified_by_id,
    e.name AS modified_by_name,
    e.type AS modified_by_type,
    (SELECT coalesce(jsonb_agg(jsonb_build_object(
            'id', s.id,
            'name', s.name,
            'contentType', s.content_type
          )), '[]'::jsonb) FROM (
            SELECT a.id, a.name, a.content_type
            FROM attachment a
            WHERE a.fee_alteration_id = fa.id
            ORDER BY a.created
        ) s) AS attachments
FROM fee_alteration fa
LEFT JOIN evaka_user e ON fa.modified_by = e.id
WHERE fa.person_id = ${bind(personId)}
ORDER BY fa.valid_from DESC, fa.valid_to DESC
"""
            )
        }
        .toList<FeeAlteration>()
}

fun Database.Read.getFeeAlterationsFrom(
    personIds: List<ChildId>,
    from: LocalDate,
): List<FeeAlteration> {
    if (personIds.isEmpty()) return emptyList()

    return createQuery {
            sql(
                """
SELECT
    fa.id,
    fa.person_id,
    fa.type,
    fa.amount,
    fa.is_absolute,
    fa.valid_from,
    fa.valid_to,
    fa.notes,
    fa.modified_at,
    e.id AS modified_by_id,
    e.name AS modified_by_name,
    e.type AS modified_by_type,
    '[]' as attachments
FROM fee_alteration fa
LEFT JOIN evaka_user e ON fa.modified_by = e.id
WHERE
    fa.person_id = ANY(${bind(personIds)})
    AND (fa.valid_to IS NULL OR fa.valid_to >= ${bind(from)})
"""
            )
        }
        .toList<FeeAlteration>()
}

fun Database.Transaction.deleteFeeAlteration(id: FeeAlterationId) {
    val update = createUpdate { sql("DELETE FROM fee_alteration WHERE id = ${bind(id)}") }
    handlingExceptions { update.execute() }
}
