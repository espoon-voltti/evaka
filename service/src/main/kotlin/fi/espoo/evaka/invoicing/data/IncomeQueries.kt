// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.data

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
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.user.EvakaUser
import java.time.LocalDate

fun Database.Transaction.insertIncome(
    now: HelsinkiDateTime,
    income: IncomeRequest,
    createdBy: EvakaUserId,
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
            created_at,
            created_by,
            modified_at,
            modified_by,
            application_id
        ) VALUES (
            ${bind(income.personId)},
            ${bind(income.effect)},
            ${bindJson(income.data)},
            ${bind(income.isEntrepreneur)},
            ${bind(income.worksAtECHA)},
            ${bind(income.validFrom)},
            ${bind(income.validTo)},
            ${bind(income.notes)},
            ${bind(now)},
            ${bind(createdBy)},
            ${bind(now)},
            ${bind(createdBy)},
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
    id: IncomeId,
    income: IncomeRequest,
    modifiedBy: EvakaUserId,
) {
    val update = createUpdate {
        sql(
            """
        UPDATE income
        SET
            effect = ${bind(income.effect)},
            data = ${bindJson(income.data)},
            is_entrepreneur = ${bind(income.isEntrepreneur)},
            works_at_echa = ${bind(income.worksAtECHA)},
            valid_from = ${bind(income.validFrom)},
            valid_to = ${bind(income.validTo)},
            notes = ${bind(income.notes)},
            modified_at = ${bind(clock.now())},
            modified_by = ${bind(modifiedBy)},
            application_id = NULL
        WHERE id = ${bind(id)}
    """
        )
    }

    handlingExceptions { update.updateExactlyOne() }
}

fun Database.Read.getIncome(
    incomeTypesProvider: IncomeTypesProvider,
    coefficientMultiplierProvider: IncomeCoefficientMultiplierProvider,
    id: IncomeId,
): Income? {
    return createQuery {
            sql(
                """
SELECT 
    income.*,
    created_by.id AS created_by_id,
    created_by.name AS created_by_name,
    created_by.type AS created_by_type,
    modified_by.id AS modified_by_id,
    modified_by.name AS modified_by_name,
    modified_by.type AS modified_by_type,
    (SELECT coalesce(jsonb_agg(jsonb_build_object(
        'id', id,
        'name', name,
        'contentType', content_type
    )), '[]'::jsonb)
FROM (
    SELECT a.id, a.name, a.content_type
    FROM attachment a
    WHERE a.income_id = income.id
    ORDER BY a.created
) s) AS attachments
FROM income
JOIN evaka_user created_by ON income.created_by = created_by.id
JOIN evaka_user modified_by ON income.modified_by = modified_by.id
WHERE income.id = ${bind(id)}
"""
            )
        }
        .exactlyOneOrNull { toIncome(incomeTypesProvider.get(), coefficientMultiplierProvider) }
}

fun Database.Read.getIncomesForPerson(
    incomeTypesProvider: IncomeTypesProvider,
    coefficientMultiplierProvider: IncomeCoefficientMultiplierProvider,
    personId: PersonId,
    validAt: LocalDate? = null,
): List<Income> {
    return createQuery {
            sql(
                """
SELECT
    income.*,
    created_by.id AS created_by_id,
    created_by.name AS created_by_name,
    created_by.type AS created_by_type,
    modified_by.id AS modified_by_id,
    modified_by.name AS modified_by_name,
    modified_by.type AS modified_by_type,
    (SELECT coalesce(jsonb_agg(jsonb_build_object(
        'id', id,
        'name', name,
        'contentType', content_type
    )), '[]'::jsonb)
FROM (
    SELECT a.id, a.name, a.content_type
    FROM attachment a
    WHERE a.income_id = income.id
    ORDER BY a.created
) s) AS attachments
FROM income
JOIN evaka_user created_by ON income.created_by = created_by.id
JOIN evaka_user modified_by ON income.modified_by = modified_by.id
WHERE person_id = ${bind(personId)}
AND (${bind(validAt)}::timestamp IS NULL OR tsrange(valid_from, valid_to) @> ${bind(validAt)}::timestamp)
ORDER BY valid_from DESC
        """
            )
        }
        .toList { toIncome(incomeTypesProvider.get(), coefficientMultiplierProvider) }
}

fun Database.Read.getIncomesFrom(
    incomeTypesProvider: IncomeTypesProvider,
    coefficientMultiplierProvider: IncomeCoefficientMultiplierProvider,
    personIds: List<PersonId>,
    from: LocalDate,
): List<Income> {
    if (personIds.isEmpty()) return emptyList()

    return createQuery {
            sql(
                """
SELECT
    income.*,
    created_by.id AS created_by_id,
    created_by.name AS created_by_name,
    created_by.type AS created_by_type,
    modified_by.id AS modified_by_id,
    modified_by.name AS modified_by_name,
    modified_by.type AS modified_by_type,
    '[]' as attachments
FROM income
JOIN evaka_user created_by ON income.created_by = created_by.id
JOIN evaka_user modified_by ON income.modified_by = modified_by.id
WHERE
    person_id = ANY(${bind(personIds)})
    AND (valid_to IS NULL OR valid_to >= ${bind(from)})
"""
            )
        }
        .toList { toIncome(incomeTypesProvider.get(), coefficientMultiplierProvider) }
}

fun Database.Transaction.deleteIncome(incomeId: IncomeId) {
    val update = createUpdate { sql("DELETE FROM income WHERE id = ${bind(incomeId)}") }

    handlingExceptions { update.execute() }
}

fun Database.Transaction.splitEarlierIncome(
    now: HelsinkiDateTime,
    personId: PersonId,
    period: DateRange,
    modifiedBy: EvakaUserId,
) {
    val update = createUpdate {
        sql(
            """
            UPDATE income SET
                valid_to = ${bind(period.start.minusDays(1))},
                modified_at = ${bind(now)},
                modified_by = ${bind(modifiedBy)}
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
    incomeTypes: Map<String, IncomeType>,
    coefficientMultiplierProvider: IncomeCoefficientMultiplierProvider,
): Income {
    val data = parseIncomeDataJson(jsonColumn("data"), incomeTypes, coefficientMultiplierProvider)
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
        createdAt = column("created_at"),
        createdBy =
            EvakaUser(
                column("created_by_id"),
                column("created_by_name"),
                column("created_by_type"),
            ),
        modifiedAt = column("modified_at"),
        modifiedBy =
            EvakaUser(
                column("modified_by_id"),
                column("modified_by_name"),
                column("modified_by_type"),
            ),
        applicationId = column("application_id"),
        attachments = jsonColumn("attachments"),
        totalIncome = calculateTotalIncome(data, coefficientMultiplierProvider),
        totalExpenses = calculateTotalExpense(data, coefficientMultiplierProvider),
        total = calculateIncomeTotal(data, coefficientMultiplierProvider),
    )
}

fun parseIncomeDataJson(
    json: Map<String, IncomeValue>,
    incomeTypes: Map<String, IncomeType>,
    coefficientMultiplierProvider: IncomeCoefficientMultiplierProvider,
): Map<String, IncomeValue> {
    return json.mapValues { (type, value) ->
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
