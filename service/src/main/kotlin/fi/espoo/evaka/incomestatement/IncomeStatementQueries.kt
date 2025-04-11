// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.incomestatement

import fi.espoo.evaka.application.utils.exhaust
import fi.espoo.evaka.daycare.domain.ProviderType
import fi.espoo.evaka.invoicing.controller.SortDirection
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.IncomeStatementId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.DatabaseEnum
import fi.espoo.evaka.shared.db.Predicate
import fi.espoo.evaka.shared.db.PredicateSql
import fi.espoo.evaka.shared.db.QuerySql
import fi.espoo.evaka.shared.db.Row
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.mapToPaged
import fi.espoo.evaka.shared.pagedForPageSize
import java.time.LocalDate

enum class IncomeStatementStatus : DatabaseEnum {
    DRAFT,
    SENT,
    HANDLED;

    override val sqlType: String = "income_statement_status"
}

enum class IncomeStatementType : DatabaseEnum {
    HIGHEST_FEE,
    INCOME,
    CHILD_INCOME;

    override val sqlType: String = "income_statement_type"
}

private fun selectQuery(
    isCitizen: Boolean,
    extraPredicate: Predicate,
    ordering: QuerySql = QuerySql { sql("") },
) = QuerySql {
    val attachmentVisibility =
        if (isCitizen) {
            Predicate { where("$it.type <> 'EMPLOYEE'") }
        } else {
            Predicate.alwaysTrue()
        }
    val statusVisibility =
        if (isCitizen) {
            Predicate.alwaysTrue()
        } else {
            Predicate { where("$it.status <> 'DRAFT'") }
        }
    sql(
        """
SELECT
    ist.id,
    ist.person_id,
    p.first_name,
    p.last_name,
    start_date,
    end_date,
    type,
    gross_income_source,
    gross_no_income_description,
    gross_estimated_monthly_income,
    gross_other_income,
    gross_other_income_info,
    start_of_entrepreneurship,
    company_name,
    business_id,
    spouse_works_in_company,
    checkup_consent,
    self_employed_attachments,
    self_employed_estimated_monthly_income,
    self_employed_income_start_date,
    self_employed_income_end_date,
    limited_company_income_source,
    partnership,
    light_entrepreneur,
    accountant_name,
    accountant_address,
    accountant_phone,
    accountant_email,
    startup_grant,
    student,
    alimony_payer,
    other_info,
    ist.created_at,
    ist.modified_at,
    ist.sent_at,
    ist.handled_at,
    status,
    handler_note,
    (SELECT coalesce(jsonb_agg(jsonb_build_object(
        'id', id, 
        'name', name,
        'contentType', content_type,
        'type', CASE WHEN type = '' THEN NULL ELSE type END,
        'uploadedByEmployee', uploaded_by_employee
      )), '[]'::jsonb) FROM (
        SELECT a.id, a.name, a.content_type, a.type, eu.type = 'EMPLOYEE' AS uploaded_by_employee
        FROM attachment a
        JOIN evaka_user eu ON a.uploaded_by = eu.id
        WHERE a.income_statement_id = ist.id 
        AND ${predicate(attachmentVisibility.forTable("eu"))}
        ORDER BY a.created
    ) s) AS attachments,
    COUNT(*) OVER () AS count
FROM income_statement ist 
JOIN person p ON p.id = ist.person_id
WHERE ${predicate(statusVisibility.forTable("ist"))}
AND ${predicate(extraPredicate.forTable("ist"))}
${subquery(ordering)}
"""
    )
}

fun queryById(id: IncomeStatementId, isCitizen: Boolean) =
    selectQuery(isCitizen, Predicate { where("ist.id = ${bind(id)}") })

fun queryByPerson(personId: PersonId, isCitizen: Boolean, limit: Int, offset: Int) =
    selectQuery(
        isCitizen,
        Predicate { where("ist.person_id = ${bind(personId)}") },
        QuerySql { sql("ORDER BY start_date DESC LIMIT ${bind(limit)} OFFSET ${bind(offset)}") },
    )

private fun Row.mapIncomeStatement(isCitizen: Boolean): IncomeStatement {
    val id = column<IncomeStatementId>("id")
    val personId = column<PersonId>("person_id")
    val firstName = column<String>("first_name")
    val lastName = column<String>("last_name")
    val startDate = column<LocalDate>("start_date")
    val endDate = column<LocalDate?>("end_date")
    val createdAt = column<HelsinkiDateTime>("created_at")
    val modifiedAt = column<HelsinkiDateTime>("modified_at")
    val sentAt = column<HelsinkiDateTime?>("sent_at")
    val handledAt = column<HelsinkiDateTime?>("handled_at")
    val status = column<IncomeStatementStatus>("status")
    val handlerNote = if (isCitizen) "" else column("handler_note")
    return when (column<IncomeStatementType>("type")) {
        IncomeStatementType.HIGHEST_FEE ->
            IncomeStatement.HighestFee(
                id = id,
                personId = personId,
                firstName = firstName,
                lastName = lastName,
                startDate = startDate,
                endDate = endDate,
                createdAt = createdAt,
                modifiedAt = modifiedAt,
                sentAt = sentAt,
                handledAt = handledAt,
                status = status,
                handlerNote = handlerNote,
            )
        IncomeStatementType.INCOME -> {
            val noIncomeDescription = column<String?>("gross_no_income_description")
            val gross =
                if (noIncomeDescription != null) {
                    Gross.NoIncome(noIncomeDescription)
                } else {
                    val grossIncomeSource = column<IncomeSource?>("gross_income_source")
                    if (grossIncomeSource != null) {
                        Gross.Income(
                            incomeSource = grossIncomeSource,
                            estimatedMonthlyIncome = column("gross_estimated_monthly_income"),
                            otherIncome = column("gross_other_income"),
                            otherIncomeInfo = column("gross_other_income_info"),
                        )
                    } else {
                        null
                    }
                }

            val selfEmployedAttachments = column<Boolean?>("self_employed_attachments")
            val selfEmployedEstimatedMonthlyIncome =
                column<Int?>("self_employed_estimated_monthly_income")
            val selfEmployed =
                if (selfEmployedAttachments != null) {
                    SelfEmployed(
                        attachments = selfEmployedAttachments,
                        estimatedIncome =
                            if (selfEmployedEstimatedMonthlyIncome != null) {
                                EstimatedIncome(
                                    selfEmployedEstimatedMonthlyIncome,
                                    incomeStartDate = column("self_employed_income_start_date"),
                                    incomeEndDate = column("self_employed_income_end_date"),
                                )
                            } else {
                                null
                            },
                    )
                } else {
                    null
                }

            val limitedCompanyIncomeSource = column<IncomeSource?>("limited_company_income_source")
            val limitedCompany =
                if (limitedCompanyIncomeSource != null) LimitedCompany(limitedCompanyIncomeSource)
                else null

            val accountantName = column<String>("accountant_name")
            val accountant =
                if (accountantName != "") {
                    Accountant(
                        name = accountantName,
                        address = column("accountant_address"),
                        phone = column("accountant_phone"),
                        email = column("accountant_email"),
                    )
                } else {
                    null
                }

            // If one of the entrepreneur columns is non-NULL, assume the entrepreneurship info has
            // been filled
            val startOfEntrepreneurship = column<LocalDate?>("start_of_entrepreneurship")
            val entrepreneur =
                if (startOfEntrepreneurship != null) {
                    Entrepreneur(
                        startOfEntrepreneurship = startOfEntrepreneurship,
                        companyName = column("company_name"),
                        businessId = column("business_id"),
                        spouseWorksInCompany = column("spouse_works_in_company"),
                        startupGrant = column("startup_grant"),
                        checkupConsent = column("checkup_consent"),
                        selfEmployed = selfEmployed,
                        limitedCompany = limitedCompany,
                        partnership = column("partnership"),
                        lightEntrepreneur = column("light_entrepreneur"),
                        accountant = accountant,
                    )
                } else {
                    null
                }

            IncomeStatement.Income(
                id = id,
                personId = personId,
                firstName = firstName,
                lastName = lastName,
                startDate = startDate,
                endDate = endDate,
                gross = gross,
                entrepreneur = entrepreneur,
                student = column("student"),
                alimonyPayer = column("alimony_payer"),
                otherInfo = column("other_info"),
                createdAt = createdAt,
                modifiedAt = modifiedAt,
                sentAt = sentAt,
                handledAt = handledAt,
                status = status,
                handlerNote = handlerNote,
                attachments = jsonColumn("attachments"),
            )
        }
        IncomeStatementType.CHILD_INCOME ->
            IncomeStatement.ChildIncome(
                id = id,
                personId = personId,
                firstName = firstName,
                lastName = lastName,
                startDate = startDate,
                endDate = endDate,
                createdAt = createdAt,
                modifiedAt = modifiedAt,
                sentAt = sentAt,
                handledAt = handledAt,
                status = status,
                handlerNote = handlerNote,
                otherInfo = column("other_info"),
                attachments = jsonColumn("attachments"),
            )
    }
}

data class PagedIncomeStatements(val data: List<IncomeStatement>, val total: Int, val pages: Int)

fun Database.Read.readIncomeStatementsForPerson(
    user: AuthenticatedUser,
    personId: PersonId,
    page: Int,
    pageSize: Int,
): PagedIncomeStatements {
    val isCitizen = user is AuthenticatedUser.Citizen
    return createQuery {
            queryByPerson(personId, isCitizen, limit = pageSize, offset = (page - 1) * pageSize)
        }
        .mapToPaged(::PagedIncomeStatements, pageSize) { mapIncomeStatement(isCitizen = isCitizen) }
}

fun Database.Read.readIncomeStatement(
    user: AuthenticatedUser,
    incomeStatementId: IncomeStatementId,
): IncomeStatement? {
    val isCitizen = user is AuthenticatedUser.Citizen
    return createQuery { queryById(incomeStatementId, isCitizen) }
        .exactlyOneOrNull { mapIncomeStatement(isCitizen = isCitizen) }
}

class IncomeStatementBindings(
    val startDate: LocalDate,
    val endDate: LocalDate?,
    var type: IncomeStatementType? = null,
    var grossIncomeSource: IncomeSource? = null,
    var grossNoIncomeDescription: String? = null,
    var grossEstimatedMonthlyIncome: Int? = null,
    var grossOtherIncome: Set<OtherIncome>? = null,
    var grossOtherIncomeInfo: String = "",
    var startOfEntrepreneurship: LocalDate? = null,
    var companyName: String = "",
    var businessId: String = "",
    var spouseWorksInCompany: Boolean? = null,
    var startupGrant: Boolean? = null,
    var checkupConsent: Boolean? = null,
    var selfEmployedAttachments: Boolean? = null,
    var selfEmployedEstimatedMonthlyIncome: Int? = null,
    var selfEmployedIncomeStartDate: LocalDate? = null,
    var selfEmployedIncomeEndDate: LocalDate? = null,
    var limitedCompanyIncomeSource: IncomeSource? = null,
    var partnership: Boolean? = null,
    var lightEntrepreneur: Boolean? = null,
    var accountantName: String = "",
    var accountantAddress: String = "",
    var accountantPhone: String = "",
    var accountantEmail: String = "",
    var student: Boolean? = null,
    var alimonyPayer: Boolean? = null,
    var otherInfo: String? = null,
) {
    companion object {
        fun of(body: IncomeStatementBody): IncomeStatementBindings {
            val bindings =
                IncomeStatementBindings(startDate = body.startDate, endDate = body.endDate)
            bindings.foo(body)
            return bindings
        }
    }
}

private fun IncomeStatementBindings.foo(body: IncomeStatementBody) {
    when (body) {
        is IncomeStatementBody.HighestFee -> type = IncomeStatementType.HIGHEST_FEE
        is IncomeStatementBody.ChildIncome -> {
            type = IncomeStatementType.CHILD_INCOME
            otherInfo = body.otherInfo
        }
        is IncomeStatementBody.Income -> {
            type = IncomeStatementType.INCOME
            if (body.gross != null) bindGross(body.gross)
            if (body.entrepreneur != null) bindEntrepreneur(body.entrepreneur)
            student = body.student
            alimonyPayer = body.alimonyPayer
            otherInfo = body.otherInfo
        }
    }
}

private fun IncomeStatementBindings.bindGross(gross: Gross) {
    when (gross) {
        is Gross.Income -> {
            grossIncomeSource = gross.incomeSource
            grossEstimatedMonthlyIncome = gross.estimatedMonthlyIncome
            grossOtherIncome = gross.otherIncome
            grossOtherIncomeInfo = gross.otherIncomeInfo
        }
        is Gross.NoIncome -> {
            grossNoIncomeDescription = gross.noIncomeDescription
        }
    }.exhaust()
}

private fun IncomeStatementBindings.bindEntrepreneur(entrepreneur: Entrepreneur) {
    if (entrepreneur.selfEmployed != null) bindSelfEmployed(entrepreneur.selfEmployed)
    if (entrepreneur.limitedCompany != null) bindLimitedCompany(entrepreneur.limitedCompany)
    startOfEntrepreneurship = entrepreneur.startOfEntrepreneurship
    companyName = entrepreneur.companyName
    businessId = entrepreneur.businessId
    spouseWorksInCompany = entrepreneur.spouseWorksInCompany
    startupGrant = entrepreneur.startupGrant
    checkupConsent = entrepreneur.checkupConsent
    partnership = entrepreneur.partnership
    lightEntrepreneur = entrepreneur.lightEntrepreneur
    if (entrepreneur.accountant != null) bindAccountant(entrepreneur.accountant)
}

private fun IncomeStatementBindings.bindSelfEmployed(selfEmployed: SelfEmployed) {
    selfEmployedAttachments = selfEmployed.attachments
    if (selfEmployed.estimatedIncome != null)
        bindSelfEmployedEstimation(selfEmployed.estimatedIncome)
}

private fun IncomeStatementBindings.bindSelfEmployedEstimation(estimation: EstimatedIncome) {
    selfEmployedEstimatedMonthlyIncome = estimation.estimatedMonthlyIncome
    selfEmployedIncomeStartDate = estimation.incomeStartDate
    selfEmployedIncomeEndDate = estimation.incomeEndDate
}

private fun IncomeStatementBindings.bindLimitedCompany(limitedCompany: LimitedCompany) {
    limitedCompanyIncomeSource = limitedCompany.incomeSource
}

private fun IncomeStatementBindings.bindAccountant(accountant: Accountant) {
    accountantName = accountant.name
    accountantAddress = accountant.address
    accountantPhone = accountant.phone
    accountantEmail = accountant.email
}

fun Database.Transaction.insertIncomeStatement(
    userId: EvakaUserId,
    now: HelsinkiDateTime,
    personId: PersonId, // may be either the user or their child
    body: IncomeStatementBody,
    draft: Boolean,
): IncomeStatementId {
    val bindings = IncomeStatementBindings.of(body)
    return createQuery {
            sql(
                """
INSERT INTO income_statement (
    created_at,
    created_by,
    modified_at,
    modified_by,
    status,
    sent_at,
    person_id,
    start_date,
    end_date,
    type,
    gross_income_source,
    gross_no_income_description,
    gross_estimated_monthly_income,
    gross_other_income,
    gross_other_income_info,
    start_of_entrepreneurship,
    company_name,
    business_id,
    spouse_works_in_company,
    startup_grant,
    checkup_consent,
    self_employed_attachments,
    self_employed_estimated_monthly_income,
    self_employed_income_start_date,
    self_employed_income_end_date,
    limited_company_income_source,
    partnership,
    light_entrepreneur,
    accountant_name,
    accountant_address,
    accountant_phone,
    accountant_email,
    student,
    alimony_payer,
    other_info
) VALUES (
    ${bind(now)},
    ${bind(userId)},
    ${bind(now)},
    ${bind(userId)},
    ${bind(if (draft) IncomeStatementStatus.DRAFT else IncomeStatementStatus.SENT)},
    ${bind(if (draft) null else now)},
    ${bind(personId)},
    ${bind(bindings.startDate)},
    ${bind(bindings.endDate)},
    ${bind(bindings.type)},
    ${bind(bindings.grossIncomeSource)},
    ${bind(bindings.grossNoIncomeDescription)},
    ${bind(bindings.grossEstimatedMonthlyIncome)},
    ${bind(bindings.grossOtherIncome)},
    ${bind(bindings.grossOtherIncomeInfo)},
    ${bind(bindings.startOfEntrepreneurship)},
    ${bind(bindings.companyName)},
    ${bind(bindings.businessId)},
    ${bind(bindings.spouseWorksInCompany)},
    ${bind(bindings.startupGrant)},
    ${bind(bindings.checkupConsent)},
    ${bind(bindings.selfEmployedAttachments)},
    ${bind(bindings.selfEmployedEstimatedMonthlyIncome)},
    ${bind(bindings.selfEmployedIncomeStartDate)},
    ${bind(bindings.selfEmployedIncomeEndDate)},
    ${bind(bindings.limitedCompanyIncomeSource)},
    ${bind(bindings.partnership)},
    ${bind(bindings.lightEntrepreneur)},
    ${bind(bindings.accountantName)},
    ${bind(bindings.accountantAddress)},
    ${bind(bindings.accountantPhone)},
    ${bind(bindings.accountantEmail)},
    ${bind(bindings.student)},
    ${bind(bindings.alimonyPayer)},
    ${bind(bindings.otherInfo)}
)
RETURNING id
        """
            )
        }
        .exactlyOne()
}

fun Database.Transaction.updateIncomeStatement(
    userId: EvakaUserId,
    now: HelsinkiDateTime,
    incomeStatementId: IncomeStatementId,
    body: IncomeStatementBody,
    draft: Boolean,
) {
    val bindings = IncomeStatementBindings.of(body)
    createUpdate {
            sql(
                """
UPDATE income_statement SET
    modified_at = ${bind(now)},
    modified_by = ${bind(userId)},
    status = CASE WHEN status = 'DRAFT'::income_statement_status THEN ${bind(if (draft) IncomeStatementStatus.DRAFT else IncomeStatementStatus.SENT)} ELSE status END,
    sent_at = coalesce(sent_at, ${bind(if (draft) null else now)}),
    start_date = ${bind(bindings.startDate)},
    end_date = ${bind(bindings.endDate)},
    type = ${bind(bindings.type)},
    gross_income_source = ${bind(bindings.grossIncomeSource)},
    gross_no_income_description = ${bind(bindings.grossNoIncomeDescription)},
    gross_estimated_monthly_income = ${bind(bindings.grossEstimatedMonthlyIncome)},
    gross_other_income = ${bind(bindings.grossOtherIncome)},
    gross_other_income_info = ${bind(bindings.grossOtherIncomeInfo)},
    start_of_entrepreneurship = ${bind(bindings.startOfEntrepreneurship)},
    company_name = ${bind(bindings.companyName)},
    business_id = ${bind(bindings.businessId)},
    spouse_works_in_company = ${bind(bindings.spouseWorksInCompany)},
    startup_grant = ${bind(bindings.startupGrant)},
    checkup_consent = ${bind(bindings.checkupConsent)},
    self_employed_attachments = ${bind(bindings.selfEmployedAttachments)},
    self_employed_estimated_monthly_income = ${bind(bindings.selfEmployedEstimatedMonthlyIncome)},
    self_employed_income_start_date = ${bind(bindings.selfEmployedIncomeStartDate)},
    self_employed_income_end_date = ${bind(bindings.selfEmployedIncomeEndDate)},
    limited_company_income_source = ${bind(bindings.limitedCompanyIncomeSource)},
    partnership = ${bind(bindings.partnership)},
    light_entrepreneur = ${bind(bindings.lightEntrepreneur)},
    accountant_name = ${bind(bindings.accountantName)},
    accountant_address = ${bind(bindings.accountantAddress)},
    accountant_phone = ${bind(bindings.accountantPhone)},
    accountant_email = ${bind(bindings.accountantEmail)},
    student = ${bind(bindings.student)},
    alimony_payer = ${bind(bindings.alimonyPayer)},
    other_info = ${bind(bindings.otherInfo)}
WHERE id = ${bind(incomeStatementId)}
        """
            )
        }
        .updateExactlyOne()
}

fun Database.Transaction.updateIncomeStatementOtherInfo(
    incomeStatementId: IncomeStatementId,
    userId: EvakaUserId,
    now: HelsinkiDateTime,
    otherInfo: String,
) {
    execute {
        sql(
            """
UPDATE income_statement
SET modified_at = ${bind(now)},
    modified_by = ${bind(userId)},
    other_info = ${bind(otherInfo)}
WHERE id = ${bind(incomeStatementId)}
"""
        )
    }
}

fun Database.Transaction.updateIncomeStatementHandled(
    user: AuthenticatedUser.Employee,
    now: HelsinkiDateTime,
    incomeStatementId: IncomeStatementId,
    note: String,
    handled: Boolean,
) {
    execute {
        sql(
            """
UPDATE income_statement 
SET modified_at = ${bind(now)},
    modified_by = ${bind(user.evakaUserId)},
    handler_note = ${bind(note)},
    handler_id = ${bind(user.id.takeIf { handled })}, 
    handled_at = ${bind(now.takeIf { handled })},
    status = ${bind(if (handled) IncomeStatementStatus.HANDLED else IncomeStatementStatus.SENT)}
WHERE id = ${bind(incomeStatementId)}
"""
        )
    }
}

fun Database.Transaction.removeIncomeStatement(id: IncomeStatementId) {
    execute {
        sql(
            "UPDATE attachment SET income_statement_id = NULL WHERE income_statement_id = ${bind(id)}"
        )
    }
    execute { sql("DELETE FROM income_statement WHERE id = ${bind(id)}") }
}

data class IncomeStatementAwaitingHandler(
    val id: IncomeStatementId,
    val sentAt: HelsinkiDateTime,
    val startDate: LocalDate,
    val incomeEndDate: LocalDate?,
    val handlerNote: String,
    val type: IncomeStatementType,
    val personId: PersonId,
    val personLastName: String,
    val personFirstName: String,
    val personName: String = "$personLastName $personFirstName",
    val primaryCareArea: String?,
)

private fun awaitingHandlerQuery(
    today: LocalDate,
    areas: List<String>,
    unit: DaycareId?,
    providerTypes: List<ProviderType>,
    sentStartDate: LocalDate?,
    sentEndDate: LocalDate?,
    placementValidDate: LocalDate?,
) = QuerySql {
    val filters =
        PredicateSql.allNotNull(
            PredicateSql { where("ca.short_name = ANY(${bind(areas)})") }
                .takeIf { areas.isNotEmpty() },
            PredicateSql { where("d.id = ${bind(unit)}") }.takeIf { unit != null },
            PredicateSql { where("d.provider_type = ANY(${bind(providerTypes)})") }
                .takeIf { providerTypes.isNotEmpty() },
            PredicateSql {
                    where(
                        "p.start_date IS NOT NULL AND p.end_date IS NOT NULL AND daterange(p.start_date, p.end_date, '[]') @> ${bind(placementValidDate)}"
                    )
                }
                .takeIf { placementValidDate != null },
        )
    val sentStart = sentStartDate?.let { HelsinkiDateTime.atStartOfDay(it) }
    val sentEnd = sentEndDate?.let { HelsinkiDateTime.atStartOfDay(it.plusDays(1)) }

    sql(
        """
SELECT DISTINCT ON (sent_at, start_date, income_end_date, type, handler_note, last_name, first_name, id)
    i.id,
    i.type,
    i.sent_at,
    i.start_date,
    i.handler_note,
    person.id AS personId,
    person.last_name AS person_last_name,
    person.first_name AS person_first_name,
    ca.name AS primaryCareArea,
    (
        SELECT valid_to FROM income
        WHERE person_id = i.person_id AND effect <> 'INCOMPLETE'
        ORDER BY valid_to DESC
        LIMIT 1
    ) AS income_end_date
FROM income_statement i
JOIN person ON person.id = i.person_id

-- guardian
LEFT JOIN guardian g ON g.guardian_id = i.person_id

-- head of child
LEFT JOIN fridge_child fc_head ON (
    fc_head.head_of_child = i.person_id AND
    ${bind(today)} BETWEEN fc_head.start_date AND fc_head.end_date
)

-- spouse of the head of child
LEFT JOIN fridge_partner fp ON fp.person_id = i.person_id AND ${bind(today)} BETWEEN fp.start_date AND fp.end_date
LEFT JOIN fridge_partner fp_spouse ON (
    fp_spouse.partnership_id = fp.partnership_id AND
    fp_spouse.person_id <> i.person_id AND
    ${bind(today)} BETWEEN fp_spouse.start_date AND fp_spouse.end_date
)
LEFT JOIN fridge_child fc_spouse ON (
    fc_spouse.head_of_child = fp_spouse.person_id AND
    ${bind(today)} BETWEEN fc_spouse.start_date AND fc_spouse.end_date
)

LEFT JOIN placement p ON ${bind(today)} BETWEEN p.start_date AND p.end_date AND p.child_id IN (
    i.person_id,  -- child's own income statement
    fc_head.child_id,
    fc_spouse.child_id
)

-- find an application too, but only if a placement was not found
LEFT JOIN application a ON p.id IS NULL AND a.child_id IN (
    i.person_id,
    g.child_id,
    fc_head.child_id,
    fc_spouse.child_id
)

LEFT JOIN daycare d ON d.id IN (p.unit_id, a.primary_preferred_unit)
LEFT JOIN care_area ca ON ca.id = d.care_area_id
WHERE
    i.status = 'SENT'::income_statement_status AND
    between_start_and_end(tstzrange(${bind(sentStart)}, ${bind(sentEnd)}, '[)'), i.sent_at) AND
    ${predicate(filters)}
"""
    )
}

data class PagedIncomeStatementsAwaitingHandler(
    val data: List<IncomeStatementAwaitingHandler>,
    val total: Int,
    val pages: Int,
)

fun Database.Read.fetchIncomeStatementsAwaitingHandler(
    today: LocalDate,
    areas: List<String>,
    unit: DaycareId?,
    providerTypes: List<ProviderType>,
    sentStartDate: LocalDate?,
    sentEndDate: LocalDate?,
    placementValidDate: LocalDate?,
    page: Int,
    pageSize: Int,
    sortBy: IncomeStatementSortParam,
    sortDirection: SortDirection,
): PagedIncomeStatementsAwaitingHandler {
    val query =
        awaitingHandlerQuery(
            today,
            areas,
            unit,
            providerTypes,
            sentStartDate,
            sentEndDate,
            placementValidDate,
        )
    val count = createQuery { sql("SELECT COUNT(*) FROM (${subquery(query)}) q") }.exactlyOne<Int>()
    val sortColumn =
        when (sortBy) {
            IncomeStatementSortParam.SENT_AT ->
                "i.sent_at ${sortDirection.name}, i.start_date, income_end_date, i.type, i.handler_note, person.last_name, person.first_name"
            IncomeStatementSortParam.START_DATE ->
                "i.start_date ${sortDirection.name}, i.sent_at, income_end_date, i.type, i.handler_note, person.last_name, person.first_name"
            IncomeStatementSortParam.INCOME_END_DATE ->
                "income_end_date ${sortDirection.name}, i.sent_at, i.start_date, i.type, i.handler_note, person.last_name, person.first_name"
            IncomeStatementSortParam.TYPE ->
                "i.type ${sortDirection.name}, i.sent_at, i.start_date, income_end_date, i.handler_note, person.last_name, person.first_name"
            IncomeStatementSortParam.HANDLER_NOTE ->
                "i.handler_note ${sortDirection.name}, i.sent_at, i.start_date, income_end_date, i.type, person.last_name, person.first_name"
            IncomeStatementSortParam.PERSON_NAME ->
                "person.last_name ${sortDirection.name}, person.first_name ${sortDirection.name}, i.sent_at, i.start_date, income_end_date, i.type, i.handler_note"
        }
    val rows =
        createQuery {
                sql(
                    """
${subquery(query)}
ORDER BY $sortColumn, i.id, ca.id  -- order by area to get the same result each time
LIMIT ${bind(pageSize)} OFFSET ${bind((page - 1) * pageSize)}
"""
                )
            }
            .toList<IncomeStatementAwaitingHandler>()

    return if (rows.isEmpty()) {
        PagedIncomeStatementsAwaitingHandler(listOf(), 0, 1)
    } else {
        rows.pagedForPageSize(::PagedIncomeStatementsAwaitingHandler, count, pageSize)
    }
}

fun Database.Read.readIncomeStatementStartDates(personId: PersonId): List<LocalDate> =
    createQuery {
            sql("SELECT start_date FROM income_statement WHERE person_id = ${bind(personId)}")
        }
        .toList()

fun Database.Read.unhandledIncomeStatementExistsForStartDate(
    personId: PersonId,
    startDate: LocalDate,
): Boolean =
    createQuery {
            sql(
                """
                SELECT EXISTS (
                    SELECT FROM income_statement 
                    WHERE person_id = ${bind(personId)} AND start_date = ${bind(startDate)} AND status != 'HANDLED'
                )
            """
            )
        }
        .exactlyOne()

data class ChildBasicInfo(val id: ChildId, val firstName: String, val lastName: String)

fun Database.Read.getIncomeStatementChildrenByGuardian(
    guardianId: PersonId,
    today: LocalDate,
): List<ChildBasicInfo> =
    this.createQuery {
            sql(
                """
SELECT
    DISTINCT p.id, p.first_name, p.last_name, p.date_of_birth
FROM guardian g
    INNER JOIN person p ON g.child_id = p.id
    LEFT JOIN placement pl ON pl.child_id = p.id AND pl.end_date >= ${bind(today)}
WHERE g.guardian_id = ${bind(guardianId)}
    AND pl.type = ANY(${bind(PlacementType.invoiced)})
ORDER BY p.date_of_birth, p.last_name, p.first_name, p.id
        """
            )
        }
        .toList()

data class PartnerIncomeStatementStatus(val name: String, val hasIncomeStatement: Boolean)

fun Database.Read.getPartnerIncomeStatementStatus(
    personId: PersonId,
    today: LocalDate,
): PartnerIncomeStatementStatus? =
    createQuery {
            sql(
                """
    SELECT 
        partner_first_name || ' ' || partner_last_name AS name,
        EXISTS (
            SELECT FROM income_statement i
            WHERE i.person_id = fp.partner_person_id AND i.status <> 'DRAFT'::income_statement_status
        ) AS has_income_statement
    FROM fridge_partner_view fp
    WHERE fp.person_id = ${bind(personId)} 
        AND daterange(fp.start_date, fp.end_date, '[]') @> ${bind(today)}
        AND NOT fp.conflict 
"""
            )
        }
        .exactlyOneOrNull()
