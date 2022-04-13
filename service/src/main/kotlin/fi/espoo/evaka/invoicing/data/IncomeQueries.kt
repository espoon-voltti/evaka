// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.data

import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.kotlin.readValue
import fi.espoo.evaka.invoicing.domain.Income
import fi.espoo.evaka.invoicing.domain.IncomeType
import fi.espoo.evaka.invoicing.domain.IncomeValue
import fi.espoo.evaka.invoicing.service.IncomeTypesProvider
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.IncomeId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.bindNullable
import fi.espoo.evaka.shared.db.mapColumn
import fi.espoo.evaka.shared.db.mapJsonColumn
import fi.espoo.evaka.shared.domain.DateRange
import org.jdbi.v3.core.result.RowView
import org.postgresql.util.PGobject
import java.time.Instant
import java.time.LocalDate

fun Database.Transaction.upsertIncome(mapper: JsonMapper, income: Income, updatedBy: EvakaUserId) {
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
            updated_by,
            application_id
        ) VALUES (
            :id,
            :person_id,
            :effect::income_effect,
            :data,
            :is_entrepreneur,
            :works_at_echa,
            :valid_from,
            :valid_to,
            :notes,
            now(),
            :updated_by,
            :application_id
        ) ON CONFLICT (id) DO UPDATE SET
            effect = :effect::income_effect,
            data = :data,
            is_entrepreneur = :is_entrepreneur,
            works_at_echa = :works_at_echa,
            valid_from = :valid_from,
            valid_to = :valid_to,
            notes = :notes,
            updated_at = now(),
            updated_by = :updated_by,
            application_id = :application_id
    """

    val update = createUpdate(sql)
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
                "updated_by" to updatedBy,
                "application_id" to income.applicationId
            )
        )

    handlingExceptions { update.execute() }
}

fun Database.Read.getIncome(mapper: JsonMapper, incomeTypesProvider: IncomeTypesProvider, id: IncomeId): Income? {
    return createQuery(
        """
        SELECT income.*, evaka_user.name AS updated_by_name,
        (SELECT coalesce(jsonb_agg(json_build_object(
            'id', id,
            'name', name,
            'contentType', content_type
          )), '[]'::jsonb) FROM (
            SELECT a.id, a.name, a.content_type
            FROM attachment a
            WHERE a.income_id = income.id
            ORDER BY a.created
        ) s) AS attachments
        FROM income
        JOIN evaka_user ON income.updated_by = evaka_user.id
        WHERE income.id = :id
        """.trimIndent()
    )
        .bind("id", id)
        .map(toIncome(mapper, incomeTypesProvider.get()))
        .firstOrNull()
}

fun Database.Read.getIncomesForPerson(
    mapper: JsonMapper,
    incomeTypesProvider: IncomeTypesProvider,
    personId: PersonId,
    validAt: LocalDate? = null
): List<Income> {
    val sql =
        """
        SELECT income.*, evaka_user.name AS updated_by_name,
        (SELECT coalesce(jsonb_agg(json_build_object(
            'id', id,
            'name', name,
            'contentType', content_type
          )), '[]'::jsonb) FROM (
            SELECT a.id, a.name, a.content_type
            FROM attachment a
            WHERE a.income_id = income.id
            ORDER BY a.created
        ) s) AS attachments
        FROM income
        JOIN evaka_user ON income.updated_by = evaka_user.id
        WHERE person_id = :personId
        AND (:validAt::timestamp IS NULL OR tsrange(valid_from, valid_to) @> :validAt::timestamp)
        ORDER BY valid_from DESC
        """.trimIndent()

    return createQuery(sql)
        .bind("personId", personId)
        .bindNullable("validAt", validAt)
        .map(toIncome(mapper, incomeTypesProvider.get()))
        .toList()
}

fun Database.Read.getIncomesFrom(
    mapper: JsonMapper,
    incomeTypesProvider: IncomeTypesProvider,
    personIds: List<PersonId>,
    from: LocalDate
): List<Income> {
    val sql =
        """
        SELECT income.*, evaka_user.name AS updated_by_name, '[]' as attachments
        FROM income
        JOIN evaka_user ON income.updated_by = evaka_user.id
        WHERE
            person_id = ANY(:personIds)
            AND (valid_to IS NULL OR valid_to >= :from)
        """

    return createQuery(sql)
        .bind("personIds", personIds.toTypedArray())
        .bind("from", from)
        .map(toIncome(mapper, incomeTypesProvider.get()))
        .toList()
}

fun Database.Transaction.deleteIncome(incomeId: IncomeId) {
    val update = createUpdate("DELETE FROM income WHERE id = :id")
        .bind("id", incomeId)

    handlingExceptions { update.execute() }
}

fun Database.Transaction.splitEarlierIncome(personId: PersonId, period: DateRange) {
    val sql =
        """
        UPDATE income
        SET valid_to = :newValidTo
        WHERE
            person_id = :personId
            AND valid_from < :from
            AND valid_to IS NULL
        """

    val update = createUpdate(sql)
        .bind("personId", personId)
        .bind("newValidTo", period.start.minusDays(1))
        .bind("from", period.start)

    handlingExceptions { update.execute() }
}

fun toIncome(mapper: JsonMapper, incomeTypes: Map<String, IncomeType>) = { rv: RowView ->
    Income(
        id = rv.mapColumn<IncomeId>("id"),
        personId = rv.mapColumn("person_id"),
        effect = rv.mapColumn("effect"),
        data = parseIncomeDataJson(rv.mapColumn("data"), mapper, incomeTypes),
        isEntrepreneur = rv.mapColumn("is_entrepreneur"),
        worksAtECHA = rv.mapColumn("works_at_echa"),
        validFrom = rv.mapColumn("valid_from"),
        validTo = rv.mapColumn("valid_to"),
        notes = rv.mapColumn("notes"),
        updatedAt = rv.mapColumn<Instant>("updated_at"),
        updatedBy = rv.mapColumn<String>("updated_by_name"),
        applicationId = rv.mapColumn("application_id"),
        attachments = rv.mapJsonColumn("attachments")
    )
}

fun parseIncomeDataJson(
    json: String,
    jsonMapper: JsonMapper,
    incomeTypes: Map<String, IncomeType>
): Map<String, IncomeValue> {
    return jsonMapper.readValue<Map<String, IncomeValue>>(json)
        .mapValues { (type, value) ->
            value.copy(multiplier = incomeTypes[type]?.multiplier ?: error("Unknown income type $type"))
        }
}
