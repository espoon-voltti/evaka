// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.incomestatement

import fi.espoo.evaka.daycare.domain.ProviderType
import fi.espoo.evaka.invoicing.controller.SortDirection
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.IncomeStatementId
import fi.espoo.evaka.shared.Paged
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.mapColumn
import fi.espoo.evaka.shared.db.mapJsonColumn
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.mapToPaged
import java.time.LocalDate
import org.jdbi.v3.core.result.RowView

enum class IncomeStatementType {
    HIGHEST_FEE,
    INCOME,
    CHILD_INCOME
}

private fun selectQuery(single: Boolean, excludeEmployeeAttachments: Boolean): String {
    // language=SQL
    return """
SELECT
    ist.id,
    ist.person_id,
    p.first_name,
    p.last_name,
    start_date,
    end_date,
    type,
    gross_income_source,
    gross_estimated_monthly_income,
    gross_other_income,
    gross_other_income_info,
    entrepreneur_full_time,
    start_of_entrepreneurship,
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
    ist.created,
    ist.updated,
    handler_id IS NOT NULL AS handled,
    handler_note,
    (SELECT coalesce(jsonb_agg(json_build_object(
        'id', id, 
        'name', name,
        'contentType', content_type,
        'uploadedByEmployee', uploaded_by_employee
      )), '[]'::jsonb) FROM (
        SELECT a.id, a.name, content_type, eu.type = 'EMPLOYEE' AS uploaded_by_employee
        FROM attachment a
        JOIN evaka_user eu ON a.uploaded_by = eu.id
        WHERE a.income_statement_id = ist.id 
        ${if (excludeEmployeeAttachments) "AND eu.type != 'EMPLOYEE'" else ""}
        ORDER BY a.created
    ) s) AS attachments,
   COUNT(*) OVER () AS count
FROM income_statement ist 
    JOIN person p ON p.id = ist.person_id
WHERE person_id = :personId
${if (single) "AND ist.id = :id" else "ORDER BY start_date DESC LIMIT :limit OFFSET :offset"}
        """
}

private fun mapIncomeStatement(row: RowView, includeEmployeeContent: Boolean): IncomeStatement {
    val id = row.mapColumn<IncomeStatementId>("id")
    val personId = row.mapColumn<PersonId>("person_id")
    val firstName = row.mapColumn<String>("first_name")
    val lastName = row.mapColumn<String>("last_name")
    val startDate = row.mapColumn<LocalDate>("start_date")
    val endDate = row.mapColumn<LocalDate?>("end_date")
    val created = row.mapColumn<HelsinkiDateTime>("created")
    val updated = row.mapColumn<HelsinkiDateTime>("updated")
    val handled = row.mapColumn<Boolean>("handled")
    val handlerNote = if (includeEmployeeContent) row.mapColumn("handler_note") else ""
    return when (row.mapColumn<IncomeStatementType>("type")) {
        IncomeStatementType.HIGHEST_FEE ->
            IncomeStatement.HighestFee(
                id = id,
                personId = personId,
                firstName = firstName,
                lastName = lastName,
                startDate = startDate,
                endDate = endDate,
                created = created,
                updated = updated,
                handled = handled,
                handlerNote = handlerNote
            )
        IncomeStatementType.INCOME -> {
            val grossIncomeSource = row.mapColumn<IncomeSource?>("gross_income_source")
            val gross =
                if (grossIncomeSource != null) {
                    Gross(
                        incomeSource = grossIncomeSource,
                        estimatedMonthlyIncome = row.mapColumn("gross_estimated_monthly_income"),
                        otherIncome = row.mapColumn("gross_other_income"),
                        otherIncomeInfo = row.mapColumn("gross_other_income_info")
                    )
                } else {
                    null
                }

            val selfEmployedAttachments = row.mapColumn<Boolean?>("self_employed_attachments")
            val selfEmployedEstimatedMonthlyIncome =
                row.mapColumn<Int?>("self_employed_estimated_monthly_income")
            val selfEmployed =
                if (selfEmployedAttachments != null) {
                    SelfEmployed(
                        attachments = selfEmployedAttachments,
                        estimatedIncome =
                            if (selfEmployedEstimatedMonthlyIncome != null) {
                                EstimatedIncome(
                                    selfEmployedEstimatedMonthlyIncome,
                                    incomeStartDate =
                                        row.mapColumn("self_employed_income_start_date"),
                                    incomeEndDate = row.mapColumn("self_employed_income_end_date")
                                )
                            } else {
                                null
                            }
                    )
                } else {
                    null
                }

            val limitedCompanyIncomeSource =
                row.mapColumn<IncomeSource?>("limited_company_income_source")
            val limitedCompany =
                if (limitedCompanyIncomeSource != null) LimitedCompany(limitedCompanyIncomeSource)
                else null

            val accountantName = row.mapColumn<String>("accountant_name")
            val accountant =
                if (accountantName != "") {
                    Accountant(
                        name = accountantName,
                        address = row.mapColumn("accountant_address"),
                        phone = row.mapColumn("accountant_phone"),
                        email = row.mapColumn("accountant_email")
                    )
                } else {
                    null
                }

            // If one of the entrepreneur columns is non-NULL, assume the entrepreneurship info has
            // been filled
            val fullTime = row.mapColumn<Boolean?>("entrepreneur_full_time")
            val entrepreneur =
                if (fullTime != null) {
                    Entrepreneur(
                        fullTime = fullTime,
                        startOfEntrepreneurship = row.mapColumn("start_of_entrepreneurship"),
                        spouseWorksInCompany = row.mapColumn("spouse_works_in_company"),
                        startupGrant = row.mapColumn("startup_grant"),
                        checkupConsent = row.mapColumn("checkup_consent"),
                        selfEmployed = selfEmployed,
                        limitedCompany = limitedCompany,
                        partnership = row.mapColumn("partnership"),
                        lightEntrepreneur = row.mapColumn("light_entrepreneur"),
                        accountant = accountant
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
                student = row.mapColumn("student"),
                alimonyPayer = row.mapColumn("alimony_payer"),
                otherInfo = row.mapColumn("other_info"),
                created = created,
                updated = updated,
                handled = handled,
                handlerNote = handlerNote,
                attachments = row.mapJsonColumn("attachments")
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
                created = created,
                updated = updated,
                handled = handled,
                handlerNote = handlerNote,
                otherInfo = row.mapColumn("other_info"),
                attachments = row.mapJsonColumn("attachments")
            )
    }
}

fun Database.Read.readIncomeStatementsForPerson(
    personId: PersonId,
    includeEmployeeContent: Boolean,
    page: Int,
    pageSize: Int
): Paged<IncomeStatement> =
    createQuery(selectQuery(single = false, excludeEmployeeAttachments = !includeEmployeeContent))
        .bind("personId", personId)
        .bind("offset", (page - 1) * pageSize)
        .bind("limit", pageSize)
        .mapToPaged(pageSize) { row -> mapIncomeStatement(row, includeEmployeeContent) }

fun Database.Read.readIncomeStatementForPerson(
    personId: PersonId,
    incomeStatementId: IncomeStatementId,
    includeEmployeeContent: Boolean
): IncomeStatement? =
    createQuery(selectQuery(single = true, excludeEmployeeAttachments = !includeEmployeeContent))
        .bind("personId", personId)
        .bind("id", incomeStatementId)
        .map { row -> mapIncomeStatement(row, includeEmployeeContent) }
        .firstOrNull()

private fun Database.SqlStatement<*>.bindIncomeStatementBody(body: IncomeStatementBody) {
    this.bind("startDate", body.startDate)
    bind("endDate", body.endDate)
    bind("grossIncomeSource", null as IncomeSource?)
    bind("grossEstimatedMonthlyIncome", null as Int?)
    bind("grossOtherIncome", null as Array<OtherIncome>?)
    this.bind("grossOtherIncomeInfo", "")
    bind("fullTime", null as Boolean?)
    bind("startOfEntrepreneurship", null as LocalDate?)
    bind("spouseWorksInCompany", null as Boolean?)
    bind("startupGrant", null as Boolean?)
    bind("checkupConsent", null as Boolean?)
    bind("selfEmployedAttachments", null as Boolean?)
    bind("selfEmployedEstimatedMonthlyIncome", null as Int?)
    bind("selfEmployedIncomeStartDate", null as LocalDate?)
    bind("selfEmployedIncomeEndDate", null as LocalDate?)
    bind("limitedCompanyIncomeSource", null as IncomeSource?)
    bind("partnership", null as Boolean?)
    bind("lightEntrepreneur", null as Boolean?)
    this.bind("accountantName", "")
    this.bind("accountantAddress", "")
    this.bind("accountantPhone", "")
    this.bind("accountantEmail", "")
    bind("student", null as Boolean?)
    bind("alimonyPayer", null as Boolean?)
    bind("otherInfo", null as String?)
    when (body) {
        is IncomeStatementBody.HighestFee -> this.bind("type", IncomeStatementType.HIGHEST_FEE)
        is IncomeStatementBody.ChildIncome -> {
            this.bind("type", IncomeStatementType.CHILD_INCOME)
            this.bind("otherInfo", body.otherInfo)
        }
        is IncomeStatementBody.Income -> {
            this.bind("type", IncomeStatementType.INCOME)
            if (body.gross != null) bindGross(body.gross)
            if (body.entrepreneur != null) bindEntrepreneur(body.entrepreneur)
            this.bind("student", body.student)
            this.bind("alimonyPayer", body.alimonyPayer)
            this.bind("otherInfo", body.otherInfo)
        }
    }
}

private fun Database.SqlStatement<*>.bindGross(gross: Gross) {
    this.bind("grossIncomeSource", gross.incomeSource)
    this.bind("grossEstimatedMonthlyIncome", gross.estimatedMonthlyIncome)
    this.bind("grossOtherIncome", gross.otherIncome)
    this.bind("grossOtherIncomeInfo", gross.otherIncomeInfo)
}

private fun Database.SqlStatement<*>.bindEntrepreneur(entrepreneur: Entrepreneur) {
    if (entrepreneur.selfEmployed != null) bindSelfEmployed(entrepreneur.selfEmployed)
    if (entrepreneur.limitedCompany != null) bindLimitedCompany(entrepreneur.limitedCompany)
    this.bind("fullTime", entrepreneur.fullTime)
    this.bind("startOfEntrepreneurship", entrepreneur.startOfEntrepreneurship)
    this.bind("spouseWorksInCompany", entrepreneur.spouseWorksInCompany)
    this.bind("startupGrant", entrepreneur.startupGrant)
    this.bind("checkupConsent", entrepreneur.checkupConsent)
    this.bind("partnership", entrepreneur.partnership)
    this.bind("lightEntrepreneur", entrepreneur.lightEntrepreneur)
    if (entrepreneur.accountant != null) bindAccountant(entrepreneur.accountant)
}

private fun Database.SqlStatement<*>.bindSelfEmployed(selfEmployed: SelfEmployed) {
    this.bind("selfEmployedAttachments", selfEmployed.attachments)
    if (selfEmployed.estimatedIncome != null)
        bindSelfEmployedEstimation(selfEmployed.estimatedIncome)
}

private fun Database.SqlStatement<*>.bindSelfEmployedEstimation(estimation: EstimatedIncome) {
    this.bind("selfEmployedEstimatedMonthlyIncome", estimation.estimatedMonthlyIncome)
    this.bind("selfEmployedIncomeStartDate", estimation.incomeStartDate)
    bind("selfEmployedIncomeEndDate", estimation.incomeEndDate)
}

private fun Database.SqlStatement<*>.bindLimitedCompany(limitedCompany: LimitedCompany) {
    this.bind("limitedCompanyIncomeSource", limitedCompany.incomeSource)
}

private fun Database.SqlStatement<*>.bindAccountant(accountant: Accountant) {
    this.bind("accountantName", accountant.name)
    this.bind("accountantAddress", accountant.address)
    this.bind("accountantPhone", accountant.phone)
    this.bind("accountantEmail", accountant.email)
}

fun Database.Transaction.createIncomeStatement(
    personId: PersonId,
    body: IncomeStatementBody
): IncomeStatementId {
    return createQuery(
            """
INSERT INTO income_statement (
    person_id,
    start_date, 
    end_date,
    type, 
    gross_income_source, 
    gross_estimated_monthly_income,
    gross_other_income, 
    gross_other_income_info,
    entrepreneur_full_time,
    start_of_entrepreneurship,
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
    :personId,
    :startDate,
    :endDate,
    :type,
    :grossIncomeSource,
    :grossEstimatedMonthlyIncome,
    :grossOtherIncome :: other_income_type[],
    :grossOtherIncomeInfo,
    :fullTime,
    :startOfEntrepreneurship,
    :spouseWorksInCompany,
    :startupGrant,
    :checkupConsent,
    :selfEmployedAttachments,
    :selfEmployedEstimatedMonthlyIncome,
    :selfEmployedIncomeStartDate,
    :selfEmployedIncomeEndDate,
    :limitedCompanyIncomeSource,
    :partnership,
    :lightEntrepreneur,
    :accountantName,
    :accountantAddress,
    :accountantPhone,
    :accountantEmail,
    :student,
    :alimonyPayer,
    :otherInfo
)
RETURNING id
        """
                .trimIndent()
        )
        .bind("personId", personId)
        .also { it.bindIncomeStatementBody(body) }
        .mapTo<IncomeStatementId>()
        .one()
}

fun Database.Transaction.updateIncomeStatement(
    incomeStatementId: IncomeStatementId,
    body: IncomeStatementBody
): Boolean {
    val rowCount =
        createUpdate(
                """
UPDATE income_statement SET
    start_date = :startDate,
    end_date = :endDate,
    type = :type,
    gross_income_source = :grossIncomeSource,
    gross_estimated_monthly_income = :grossEstimatedMonthlyIncome,
    gross_other_income = :grossOtherIncome :: other_income_type[],
    gross_other_income_info = :grossOtherIncomeInfo,
    entrepreneur_full_time = :fullTime,
    start_of_entrepreneurship = :startOfEntrepreneurship,
    spouse_works_in_company = :spouseWorksInCompany,
    startup_grant = :startupGrant,
    checkup_consent = :checkupConsent,
    self_employed_attachments = :selfEmployedAttachments,
    self_employed_estimated_monthly_income = :selfEmployedEstimatedMonthlyIncome,
    self_employed_income_start_date = :selfEmployedIncomeStartDate,
    self_employed_income_end_date = :selfEmployedIncomeEndDate,
    limited_company_income_source = :limitedCompanyIncomeSource,
    partnership = :partnership,
    light_entrepreneur = :lightEntrepreneur,
    accountant_name = :accountantName,
    accountant_address = :accountantAddress,
    accountant_phone = :accountantPhone,
    accountant_email = :accountantEmail,
    student = :student,
    alimony_payer = :alimonyPayer,
    other_info = :otherInfo
WHERE id = :id
        """
                    .trimIndent()
            )
            .bind("id", incomeStatementId)
            .also { it.bindIncomeStatementBody(body) }
            .execute()

    return rowCount == 1
}

fun Database.Transaction.updateIncomeStatementHandled(
    incomeStatementId: IncomeStatementId,
    note: String,
    handlerId: EmployeeId?
) {
    createUpdate(
            "UPDATE income_statement SET handler_id = :handlerId, handler_note = :note WHERE id = :id"
        )
        .bind("id", incomeStatementId)
        .bind("note", note)
        .bind("handlerId", handlerId)
        .execute()
}

fun Database.Transaction.removeIncomeStatement(id: IncomeStatementId) {
    createUpdate("UPDATE attachment SET income_statement_id = NULL WHERE income_statement_id = :id")
        .bind("id", id)
        .execute()
    createUpdate("DELETE FROM income_statement WHERE id = :id").bind("id", id).execute()
}

data class IncomeStatementAwaitingHandler(
    val id: IncomeStatementId,
    val created: HelsinkiDateTime,
    val startDate: LocalDate,
    val handlerNote: String,
    val type: IncomeStatementType,
    val personId: PersonId,
    val personName: String,
    val primaryCareArea: String?
)

// language=SQL
private const val awaitingHandlerQuery =
    """
SELECT DISTINCT ON (i.created, i.start_date, i.id)
    i.id,
    i.type,
    i.created,
    i.start_date,
    i.handler_note,
    person.id AS personId,
    person.last_name || ' ' || person.first_name AS personName,
    ca.name AS primaryCareArea
FROM income_statement i
JOIN person ON person.id = i.person_id

-- guardian
LEFT JOIN guardian g ON g.guardian_id = i.person_id

-- head of child
LEFT JOIN fridge_child fc_head ON (
    fc_head.head_of_child = i.person_id AND
    :today BETWEEN fc_head.start_date AND fc_head.end_date
)

-- spouse of the head of child
LEFT JOIN fridge_partner fp ON fp.person_id = i.person_id AND :today BETWEEN fp.start_date AND fp.end_date
LEFT JOIN fridge_partner fp_spouse ON (
    fp_spouse.partnership_id = fp.partnership_id AND
    fp_spouse.person_id <> i.person_id AND
    :today BETWEEN fp_spouse.start_date AND fp_spouse.end_date
)
LEFT JOIN fridge_child fc_spouse ON (
    fc_spouse.head_of_child = fp_spouse.person_id AND
    :today BETWEEN fc_spouse.start_date AND fc_spouse.end_date
)

LEFT JOIN placement p ON :today BETWEEN p.start_date AND p.end_date AND p.child_id IN (
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

LEFT JOIN daycare d ON d.id IN (
    p.unit_id, 
    (a.document -> 'apply' -> 'preferredUnits' ->> 0)::uuid
)
LEFT JOIN care_area ca ON ca.id = d.care_area_id

WHERE handler_id IS NULL
AND (cardinality(:areas) = 0 OR ca.short_name = ANY(:areas))
AND (cardinality(:providerTypes) = 0 OR d.provider_type = ANY(:providerTypes::unit_provider_type[]))
AND daterange(:sentStartDate, :sentEndDate, '[]') @> i.created::date
AND (:placementValidDate IS NULL OR (p.start_date IS NOT NULL AND p.end_date IS NOT NULL AND daterange(p.start_date, p.end_date, '[]') @> :placementValidDate))
"""

fun Database.Read.fetchIncomeStatementsAwaitingHandler(
    today: LocalDate,
    areas: List<String>,
    providerTypes: List<ProviderType>,
    sentStartDate: LocalDate?,
    sentEndDate: LocalDate?,
    placementValidDate: LocalDate?,
    page: Int,
    pageSize: Int,
    sortBy: IncomeStatementSortParam,
    sortDirection: SortDirection
): Paged<IncomeStatementAwaitingHandler> {
    val count =
        createQuery("""SELECT COUNT(*) FROM ($awaitingHandlerQuery) q""")
            .bind("today", today)
            .bind("areas", areas)
            .bind("providerTypes", providerTypes)
            .bind("sentStartDate", sentStartDate)
            .bind("sentEndDate", sentEndDate)
            .bind("placementValidDate", placementValidDate)
            .mapTo<Int>()
            .one()
    val sortColumn =
        when (sortBy) {
            IncomeStatementSortParam.CREATED -> "i.created ${sortDirection.name}, i.start_date"
            IncomeStatementSortParam.START_DATE -> "i.start_date ${sortDirection.name}, i.created"
        }
    val rows =
        createQuery(
                """
$awaitingHandlerQuery
ORDER BY $sortColumn, i.id, ca.id, person.last_name, person.first_name  -- order by area to get the same result each time
LIMIT :pageSize OFFSET :offset
        """
                    .trimIndent()
            )
            .bind("today", today)
            .bind("areas", areas)
            .bind("providerTypes", providerTypes)
            .bind("sentStartDate", sentStartDate)
            .bind("sentEndDate", sentEndDate)
            .bind("placementValidDate", placementValidDate)
            .bind("pageSize", pageSize)
            .bind("offset", (page - 1) * pageSize)
            .mapTo<IncomeStatementAwaitingHandler>()
            .list()

    return if (rows.isEmpty()) {
        Paged(listOf(), 0, 1)
    } else {
        Paged.forPageSize(rows, count, pageSize)
    }
}

fun Database.Read.readIncomeStatementStartDates(personId: PersonId): List<LocalDate> =
    createQuery("SELECT start_date FROM income_statement WHERE person_id = :personId")
        .bind("personId", personId)
        .mapTo<LocalDate>()
        .list()

fun Database.Read.incomeStatementExistsForStartDate(
    personId: PersonId,
    startDate: LocalDate
): Boolean =
    createQuery(
            "SELECT 1 FROM income_statement WHERE person_id = :personId AND start_date = :startDate"
        )
        .bind("personId", personId)
        .bind("startDate", startDate)
        .mapTo<Int>()
        .any()

data class ChildBasicInfo(val id: ChildId, val firstName: String, val lastName: String)

fun Database.Read.getIncomeStatementChildrenByGuardian(
    guardianId: PersonId,
    today: LocalDate
): List<ChildBasicInfo> =
    this.createQuery(
            """
SELECT
    DISTINCT p.id, p.first_name, p.last_name, p.date_of_birth
FROM guardian g
    INNER JOIN person p ON g.child_id = p.id
    LEFT JOIN placement pl ON pl.child_id = p.id AND pl.end_date >= :today::date
WHERE g.guardian_id = :guardianId
    AND pl.type = ANY(:invoicedPlacementTypes::placement_type[])
ORDER BY p.date_of_birth, p.last_name, p.first_name, p.id
        """
                .trimIndent()
        )
        .bind("today", today)
        .bind("guardianId", guardianId)
        .bind("invoicedPlacementTypes", PlacementType.invoiced)
        .mapTo<ChildBasicInfo>()
        .list()
