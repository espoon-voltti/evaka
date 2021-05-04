// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.data

import fi.espoo.evaka.invoicing.domain.FeeAlteration
import fi.espoo.evaka.shared.db.Database
import org.jdbi.v3.core.statement.StatementContext
import java.sql.ResultSet
import java.time.LocalDate
import java.util.UUID

fun Database.Transaction.upsertFeeAlteration(feeAlteration: FeeAlteration) {
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
                now()
            ) ON CONFLICT (id) DO UPDATE SET
                type = :type::fee_alteration_type,
                amount = :amount,
                is_absolute = :is_absolute,
                valid_from = :valid_from,
                valid_to = :valid_to,
                notes = :notes,
                updated_by = :updated_by,
                updated_at = now()
        """

    val update = createUpdate(sql)
        .bindMap(
            mapOf(
                "id" to feeAlteration.id,
                "person_id" to feeAlteration.personId,
                "type" to feeAlteration.type.toString(),
                "amount" to feeAlteration.amount,
                "is_absolute" to feeAlteration.isAbsolute,
                "valid_from" to feeAlteration.validFrom,
                "valid_to" to feeAlteration.validTo,
                "notes" to feeAlteration.notes,
                "updated_by" to feeAlteration.updatedBy
            )

        )

    handlingExceptions { update.execute() }
}

fun Database.Read.getFeeAlteration(id: UUID): FeeAlteration? {
    return createQuery("SELECT * FROM fee_alteration WHERE id = :id")
        .bind("id", id)
        .map(toFeeAlteration)
        .firstOrNull()
}

fun Database.Read.getFeeAlterationsForPerson(personId: UUID): List<FeeAlteration> {
    val sql = "SELECT * FROM fee_alteration WHERE person_id = :personId ORDER BY valid_from DESC, valid_to DESC"

    return createQuery(sql)
        .bind("personId", personId)
        .map(toFeeAlteration)
        .toList()
}

fun Database.Read.getFeeAlterationsFrom(personIds: List<UUID>, from: LocalDate): List<FeeAlteration> {
    if (personIds.isEmpty()) return emptyList()

    val sql =
        """
            SELECT * FROM fee_alteration
            WHERE
                person_id = ANY(:personIds)
                AND (valid_to IS NULL OR valid_to >= :from)
            """

    return createQuery(sql)
        .bind("personIds", personIds.toTypedArray())
        .bind("from", from)
        .map(toFeeAlteration)
        .toList()
}

fun Database.Transaction.deleteFeeAlteration(id: UUID) {
    val update = createUpdate("DELETE FROM fee_alteration WHERE id = :id")
        .bind("id", id)

    handlingExceptions { update.execute() }
}

val toFeeAlteration = { rs: ResultSet, _: StatementContext ->
    FeeAlteration(
        id = UUID.fromString(rs.getString("id")),
        personId = UUID.fromString(rs.getString("person_id")),
        type = FeeAlteration.Type.valueOf(rs.getString("type")),
        amount = rs.getInt("amount"),
        isAbsolute = rs.getBoolean("is_absolute"),
        validFrom = rs.getDate("valid_from").toLocalDate(),
        validTo = rs.getDate("valid_to")?.toLocalDate(),
        notes = rs.getString("notes"),
        updatedAt = rs.getTimestamp("updated_at").toInstant(),
        updatedBy = UUID.fromString((rs.getString("updated_by")))
    )
}
