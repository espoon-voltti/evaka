// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.data

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import fi.espoo.evaka.invoicing.domain.Income
import fi.espoo.evaka.invoicing.domain.IncomeEffect
import fi.espoo.evaka.shared.db.bindNullable
import fi.espoo.evaka.shared.domain.Period
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.statement.StatementContext
import org.postgresql.util.PGobject
import java.sql.ResultSet
import java.time.LocalDate
import java.util.UUID

fun upsertIncome(h: Handle, mapper: ObjectMapper, income: Income, updatedBy: UUID) {
    val sql =
        """
        INSERT INTO income (
            id,
            person_id,
            effect,
            data,
            is_entrepreneur,
            works_at_echa,
            valid_from,
            valid_to,
            notes,
            updated_at,
            updated_by
        ) VALUES (
            :id,
            :person_id,
            :effect,
            :data,
            :is_entrepreneur,
            :works_at_echa,
            :valid_from,
            :valid_to,
            :notes,
            now(),
            :updated_by
        ) ON CONFLICT (id) DO UPDATE SET
            effect = :effect,
            data = :data,
            is_entrepreneur = :is_entrepreneur,
            works_at_echa = :works_at_echa,
            valid_from = :valid_from,
            valid_to = :valid_to,
            notes = :notes,
            updated_at = now(),
            updated_by = :updated_by
    """

    val update = h.createUpdate(sql)
        .bindMap(
            mapOf(
                "id" to income.id,
                "person_id" to income.personId,
                "effect" to income.effect.toString(),
                "data" to PGobject().apply {
                    type = "jsonb"
                    value = mapper.writeValueAsString(income.data)
                },
                "is_entrepreneur" to income.isEntrepreneur,
                "works_at_echa" to income.worksAtECHA,
                "valid_from" to income.validFrom,
                "valid_to" to income.validTo,
                "notes" to income.notes,
                "updated_by" to updatedBy
            )
        )

    handlingExceptions { update.execute() }
}

fun getIncome(h: Handle, mapper: ObjectMapper, id: UUID): Income? {
    return h.createQuery(
        """
        SELECT income.*, employee.first_name || ' ' || employee.last_name AS updated_by_employee
        FROM income
        LEFT JOIN employee ON income.updated_by = employee.id
        WHERE income.id = :id
        """.trimIndent()
    )
        .bind("id", id)
        .map(toIncome(mapper))
        .firstOrNull()
}

fun getIncomesForPerson(h: Handle, mapper: ObjectMapper, personId: UUID, validAt: LocalDate? = null): List<Income> {
    val sql =
        """
        SELECT income.*, employee.first_name || ' ' || employee.last_name AS updated_by_employee
        FROM income
        LEFT JOIN employee ON income.updated_by = employee.id
        WHERE person_id = :personId
        AND (:validAt::timestamp IS NULL OR tsrange(valid_from, valid_to) @> :validAt::timestamp)
        ORDER BY valid_from DESC
        """.trimIndent()

    return h.createQuery(sql)
        .bind("personId", personId)
        .bindNullable("validAt", validAt)
        .map(toIncome(mapper))
        .toList()
}

fun getIncomesFrom(h: Handle, mapper: ObjectMapper, personIds: List<UUID>, from: LocalDate): List<Income> {
    val sql =
        """
        SELECT income.*, employee.first_name || ' ' || employee.last_name AS updated_by_employee
        FROM income
        LEFT JOIN employee ON income.updated_by = employee.id
        WHERE
            person_id = ANY(:personIds)
            AND (valid_to IS NULL OR valid_to >= :from)
        """

    return h.createQuery(sql)
        .bind("personIds", personIds.toTypedArray())
        .bind("from", from)
        .map(toIncome(mapper))
        .toList()
}

fun deleteIncome(h: Handle, incomeId: UUID) {
    val update = h.createUpdate("DELETE FROM income WHERE id = :id")
        .bind("id", incomeId)

    handlingExceptions { update.execute() }
}

fun splitEarlierIncome(h: Handle, personId: UUID, period: Period) {
    val sql =
        """
        UPDATE income
        SET valid_to = :newValidTo
        WHERE
            person_id = :personId
            AND valid_from < :from
            AND valid_to IS NULL
        """

    val update = h.createUpdate(sql)
        .bind("personId", personId)
        .bind("newValidTo", period.start.minusDays(1))
        .bind("from", period.start)

    handlingExceptions { update.execute() }
}

fun toIncome(objectMapper: ObjectMapper) = { rs: ResultSet, _: StatementContext ->
    Income(
        id = UUID.fromString(rs.getString("id")),
        personId = UUID.fromString(rs.getString("person_id")),
        effect = IncomeEffect.valueOf(rs.getString("effect")),
        data = objectMapper.readValue(rs.getString("data")),
        isEntrepreneur = rs.getBoolean("is_entrepreneur"),
        worksAtECHA = rs.getBoolean("works_at_echa"),
        validFrom = rs.getDate("valid_from").toLocalDate(),
        validTo = rs.getDate("valid_to")?.toLocalDate(),
        notes = rs.getString("notes"),
        updatedAt = rs.getTimestamp("updated_at").toInstant(),
        updatedBy = (rs.getString("updated_by_employee"))
    )
}
