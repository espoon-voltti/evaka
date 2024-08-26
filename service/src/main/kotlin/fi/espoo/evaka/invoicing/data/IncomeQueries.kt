// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.data

import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.kotlin.readValue
import fi.espoo.evaka.invoicing.calculateIncomeTotal
import fi.espoo.evaka.invoicing.calculateMonthlyAmount
import fi.espoo.evaka.invoicing.calculateTotalExpense
import fi.espoo.evaka.invoicing.calculateTotalIncome
import fi.espoo.evaka.invoicing.domain.Income
import fi.espoo.evaka.invoicing.domain.IncomeRequest
import fi.espoo.evaka.invoicing.domain.IncomeType
import fi.espoo.evaka.invoicing.domain.IncomeValue
import fi.espoo.evaka.invoicing.service.IncomeCoefficientMultiplierProvider
import fi.espoo.evaka.invoicing.service.IncomeTypesProvider
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.IncomeId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.Row
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.EvakaClock
import java.time.LocalDate

fun Database.Transaction.insertIncome(
    clock: EvakaClock,
    mapper: JsonMapper,
    income: IncomeRequest,
    updatedBy: EvakaUserId,
): IncomeId {
    val update = createQuery {
        sql(
            """
        INSERT INTO income (
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
            ${bind(income.personId)},
            ${bind(income.effect)},
            ${bind(mapper.writeValueAsString(income.data))}::jsonb,
            ${bind(income.isEntrepreneur)},
            ${bind(income.worksAtECHA)},
            ${bind(income.validFrom)},
            ${bind(income.validTo)},
            ${bind(income.notes)},
            ${bind(clock.now())},
            ${bind(updatedBy)},
            NULL
        )
        RETURNING id
    """
        )
    }

    return handlingExceptions { update.exactlyOne() }
}

fun Database.Transaction.updateIncome(
    clock: EvakaClock,
    mapper: JsonMapper,
    id: IncomeId,
    income: IncomeRequest,
    updatedBy: EvakaUserId,
) {
    val update = createUpdate {
        sql(
            """
        UPDATE income
        SET
            effect = ${bind(income.effect)},
            data = ${bind(mapper.writeValueAsString(income.data))}::jsonb,
            is_entrepreneur = ${bind(income.isEntrepreneur)},
            works_at_echa = ${bind(income.worksAtECHA)},
            valid_from = ${bind(income.validFrom)},
            valid_to = ${bind(income.validTo)},
            notes = ${bind(income.notes)},
            updated_at = ${bind(clock.now())},
            updated_by = ${bind(updatedBy)},
            application_id = NULL
        WHERE id = ${bind(id)}
    """
        )
    }

    handlingExceptions { update.updateExactlyOne() }
}

fun Database.Read.getIncome(
    mapper: JsonMapper,
    incomeTypesProvider: IncomeTypesProvider,
    coefficientMultiplierProvider: IncomeCoefficientMultiplierProvider,
    id: IncomeId,
): Income? {
    return createQuery {
            sql(
                """
SELECT income.*, evaka_user.name AS updated_by_name,
(SELECT coalesce(jsonb_agg(jsonb_build_object(
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
WHERE income.id = ${bind(id)}
"""
            )
        }
        .exactlyOneOrNull {
            toIncome(mapper, incomeTypesProvider.get(), coefficientMultiplierProvider)
        }
}

fun Database.Read.getIncomesForPerson(
    mapper: JsonMapper,
    incomeTypesProvider: IncomeTypesProvider,
    coefficientMultiplierProvider: IncomeCoefficientMultiplierProvider,
    personId: PersonId,
    validAt: LocalDate? = null,
): List<Income> {
    return createQuery {
            sql(
                """
SELECT income.*, evaka_user.name AS updated_by_name,
(SELECT coalesce(jsonb_agg(jsonb_build_object(
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
WHERE person_id = ${bind(personId)}
AND (${bind(validAt)}::timestamp IS NULL OR tsrange(valid_from, valid_to) @> ${bind(validAt)}::timestamp)
ORDER BY valid_from DESC
        """
            )
        }
        .toList { toIncome(mapper, incomeTypesProvider.get(), coefficientMultiplierProvider) }
}

fun Database.Read.getIncomesFrom(
    mapper: JsonMapper,
    incomeTypesProvider: IncomeTypesProvider,
    coefficientMultiplierProvider: IncomeCoefficientMultiplierProvider,
    personIds: List<PersonId>,
    from: LocalDate,
): List<Income> {
    if (personIds.isEmpty()) return emptyList()

    return createQuery {
            sql(
                """
SELECT income.*, evaka_user.name AS updated_by_name, '[]' as attachments
FROM income
JOIN evaka_user ON income.updated_by = evaka_user.id
WHERE
    person_id = ANY(${bind(personIds)})
    AND (valid_to IS NULL OR valid_to >= ${bind(from)})
"""
            )
        }
        .toList { toIncome(mapper, incomeTypesProvider.get(), coefficientMultiplierProvider) }
}

fun Database.Transaction.deleteIncome(incomeId: IncomeId) {
    val update = createUpdate { sql("DELETE FROM income WHERE id = ${bind(incomeId)}") }

    handlingExceptions { update.execute() }
}

fun Database.Transaction.splitEarlierIncome(personId: PersonId, period: DateRange) {
    val update = createUpdate {
        sql(
            """
            UPDATE income
            SET valid_to = ${bind(period.start.minusDays(1))}
            WHERE
                person_id = ${bind(personId)}
                AND valid_from < ${bind(period.start)}
                AND valid_to IS NULL
            """
        )
    }

    handlingExceptions { update.execute() }
}

fun Row.toIncome(
    mapper: JsonMapper,
    incomeTypes: Map<String, IncomeType>,
    coefficientMultiplierProvider: IncomeCoefficientMultiplierProvider,
): Income {
    val data =
        parseIncomeDataJson(column("data"), mapper, incomeTypes, coefficientMultiplierProvider)
    return Income(
        id = column<IncomeId>("id"),
        personId = column("person_id"),
        effect = column("effect"),
        data = data,
        isEntrepreneur = column("is_entrepreneur"),
        worksAtECHA = column("works_at_echa"),
        validFrom = column("valid_from"),
        validTo = column("valid_to"),
        notes = column("notes"),
        updatedAt = column("updated_at"),
        updatedBy = column("updated_by_name"),
        applicationId = column("application_id"),
        attachments = jsonColumn("attachments"),
        totalIncome = calculateTotalIncome(data, coefficientMultiplierProvider),
        totalExpenses = calculateTotalExpense(data, coefficientMultiplierProvider),
        total = calculateIncomeTotal(data, coefficientMultiplierProvider),
    )
}

fun parseIncomeDataJson(
    json: String,
    jsonMapper: JsonMapper,
    incomeTypes: Map<String, IncomeType>,
    coefficientMultiplierProvider: IncomeCoefficientMultiplierProvider,
): Map<String, IncomeValue> {
    return jsonMapper.readValue<Map<String, IncomeValue>>(json).mapValues { (type, value) ->
        value.copy(
            multiplier = incomeTypes[type]?.multiplier ?: error("Unknown income type $type"),
            monthlyAmount =
                calculateMonthlyAmount(
                    value.amount,
                    coefficientMultiplierProvider.multiplier(value.coefficient),
                ),
        )
    }
}
