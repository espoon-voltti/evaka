// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.incomestatement

import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.IncomeStatementId
import fi.espoo.evaka.shared.Paged
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.bindNullable
import fi.espoo.evaka.shared.db.mapColumn
import fi.espoo.evaka.shared.db.mapJsonColumn
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.mapToPaged
import org.jdbi.v3.core.kotlin.mapTo
import org.jdbi.v3.core.result.RowView
import org.jdbi.v3.core.statement.SqlStatement
import java.time.LocalDate

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
                handlerNote = handlerNote,
            )

        IncomeStatementType.INCOME -> {
            val grossIncomeSource = row.mapColumn<IncomeSource?>("gross_income_source")
            val gross = if (grossIncomeSource != null) Gross(
                incomeSource = grossIncomeSource,
                estimatedMonthlyIncome = row.mapColumn("gross_estimated_monthly_income"),
                otherIncome = row.mapColumn<Array<OtherIncome>>("gross_other_income").toSet(),
                otherIncomeInfo = row.mapColumn("gross_other_income_info"),
            ) else null

            val selfEmployedAttachments = row.mapColumn<Boolean?>("self_employed_attachments")
            val selfEmployedEstimatedMonthlyIncome = row.mapColumn<Int?>("self_employed_estimated_monthly_income")
            val selfEmployed = if (selfEmployedAttachments != null)
                SelfEmployed(
                    attachments = selfEmployedAttachments,
                    estimatedIncome = if (selfEmployedEstimatedMonthlyIncome != null) EstimatedIncome(
                        selfEmployedEstimatedMonthlyIncome,
                        incomeStartDate = row.mapColumn("self_employed_income_start_date"),
                        incomeEndDate = row.mapColumn("self_employed_income_end_date")
                    ) else null
                ) else null

            val limitedCompanyIncomeSource = row.mapColumn<IncomeSource?>("limited_company_income_source")
            val limitedCompany =
                if (limitedCompanyIncomeSource != null) LimitedCompany(limitedCompanyIncomeSource) else null

            val accountantName = row.mapColumn<String>("accountant_name")
            val accountant = if (accountantName != "") Accountant(
                name = accountantName,
                address = row.mapColumn("accountant_address"),
                phone = row.mapColumn("accountant_phone"),
                email = row.mapColumn("accountant_email"),
            ) else null

            // If one of the entrepreneur columns is non-NULL, assume the entrepreneurship info has been filled
            val fullTime = row.mapColumn<Boolean?>("entrepreneur_full_time")
            val entrepreneur = if (fullTime != null) Entrepreneur(
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
            ) else null

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

private fun <This : SqlStatement<This>> SqlStatement<This>.bindIncomeStatementBody(body: IncomeStatementBody): This =
    bind("startDate", body.startDate)
        .bindNullable("endDate", body.endDate)
        .bindNullable("grossIncomeSource", null as IncomeSource?)
        .bindNullable("grossEstimatedMonthlyIncome", null as Int?)
        .bindNullable("grossOtherIncome", null as Array<OtherIncome>?)
        .bind("grossOtherIncomeInfo", "")
        .bindNullable("fullTime", null as Boolean?)
        .bindNullable("startOfEntrepreneurship", null as LocalDate?)
        .bindNullable("spouseWorksInCompany", null as Boolean?)
        .bindNullable("startupGrant", null as Boolean?)
        .bindNullable("checkupConsent", null as Boolean?)
        .bindNullable("selfEmployedAttachments", null as Boolean?)
        .bindNullable("selfEmployedEstimatedMonthlyIncome", null as Int?)
        .bindNullable("selfEmployedIncomeStartDate", null as LocalDate?)
        .bindNullable("selfEmployedIncomeEndDate", null as LocalDate?)
        .bindNullable("limitedCompanyIncomeSource", null as IncomeSource?)
        .bindNullable("partnership", null as Boolean?)
        .bindNullable("lightEntrepreneur", null as Boolean?)
        .bind("accountantName", "")
        .bind("accountantAddress", "")
        .bind("accountantPhone", "")
        .bind("accountantEmail", "")
        .bindNullable("student", null as Boolean?)
        .bindNullable("alimonyPayer", null as Boolean?)
        .bindNullable("otherInfo", null as String?)
        .run {
            when (body) {
                is IncomeStatementBody.HighestFee ->
                    bind("type", IncomeStatementType.HIGHEST_FEE)
                is IncomeStatementBody.ChildIncome ->
                    bind("type", IncomeStatementType.CHILD_INCOME)
                        .bind("otherInfo", body.otherInfo)
                is IncomeStatementBody.Income -> {
                    bind("type", IncomeStatementType.INCOME)
                        .run { if (body.gross != null) bindGross(body.gross) else this }
                        .run { if (body.entrepreneur != null) bindEntrepreneur(body.entrepreneur) else this }
                        .bind("student", body.student)
                        .bind("alimonyPayer", body.alimonyPayer)
                        .bind("otherInfo", body.otherInfo)
                }
            }
        }

private fun <This : SqlStatement<This>> SqlStatement<This>.bindGross(gross: Gross): This =
    bind("grossIncomeSource", gross.incomeSource)
        .bind("grossEstimatedMonthlyIncome", gross.estimatedMonthlyIncome)
        .bind("grossOtherIncome", gross.otherIncome.toTypedArray())
        .bind("grossOtherIncomeInfo", gross.otherIncomeInfo)

private fun <This : SqlStatement<This>> SqlStatement<This>.bindEntrepreneur(entrepreneur: Entrepreneur): This =
    run { if (entrepreneur.selfEmployed != null) bindSelfEmployed(entrepreneur.selfEmployed) else this }
        .run { if (entrepreneur.limitedCompany != null) bindLimitedCompany(entrepreneur.limitedCompany) else this }
        .bind("fullTime", entrepreneur.fullTime)
        .bind("startOfEntrepreneurship", entrepreneur.startOfEntrepreneurship)
        .bind("spouseWorksInCompany", entrepreneur.spouseWorksInCompany)
        .bind("startupGrant", entrepreneur.startupGrant)
        .bind("checkupConsent", entrepreneur.checkupConsent)
        .bind("partnership", entrepreneur.partnership)
        .bind("lightEntrepreneur", entrepreneur.lightEntrepreneur)
        .run { if (entrepreneur.accountant != null) bindAccountant(entrepreneur.accountant) else this }

private fun <This : SqlStatement<This>> SqlStatement<This>.bindSelfEmployed(selfEmployed: SelfEmployed): This =
    bind("selfEmployedAttachments", selfEmployed.attachments)
        .run { if (selfEmployed.estimatedIncome != null) bindSelfEmployedEstimation(selfEmployed.estimatedIncome) else this }

private fun <This : SqlStatement<This>> SqlStatement<This>.bindSelfEmployedEstimation(estimation: EstimatedIncome): This =
    bind("selfEmployedEstimatedMonthlyIncome", estimation.estimatedMonthlyIncome)
        .bind("selfEmployedIncomeStartDate", estimation.incomeStartDate)
        .bindNullable("selfEmployedIncomeEndDate", estimation.incomeEndDate)

private fun <This : SqlStatement<This>> SqlStatement<This>.bindLimitedCompany(limitedCompany: LimitedCompany): This =
    bind("limitedCompanyIncomeSource", limitedCompany.incomeSource)

private fun <This : SqlStatement<This>> SqlStatement<This>.bindAccountant(accountant: Accountant): This =
    bind("accountantName", accountant.name)
        .bind("accountantAddress", accountant.address)
        .bind("accountantPhone", accountant.phone)
        .bind("accountantEmail", accountant.email)

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
        """.trimIndent()
    )
        .bind("personId", personId)
        .bindIncomeStatementBody(body)
        .mapTo<IncomeStatementId>()
        .one()
}

fun Database.Transaction.updateIncomeStatement(
    incomeStatementId: IncomeStatementId,
    body: IncomeStatementBody
): Boolean {
    val rowCount = createUpdate(
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
        """.trimIndent()
    )
        .bind("id", incomeStatementId)
        .bindIncomeStatementBody(body)
        .execute()

    return rowCount == 1
}

fun Database.Transaction.updateIncomeStatementHandled(
    incomeStatementId: IncomeStatementId,
    note: String,
    handlerId: EmployeeId?
) {
    createUpdate("UPDATE income_statement SET handler_id = :handlerId, handler_note = :note WHERE id = :id")
        .bind("id", incomeStatementId)
        .bind("note", note)
        .bindNullable("handlerId", handlerId)
        .execute()
}

fun Database.Transaction.removeIncomeStatement(id: IncomeStatementId) {
    createUpdate("UPDATE attachment SET income_statement_id = NULL WHERE income_statement_id = :id")
        .bind("id", id)
        .execute()
    createUpdate("DELETE FROM income_statement WHERE id = :id")
        .bind("id", id)
        .execute()
}

data class IncomeStatementAwaitingHandler(
    val id: IncomeStatementId,
    val created: HelsinkiDateTime,
    val startDate: LocalDate,
    val type: IncomeStatementType,
    val personId: PersonId,
    val personName: String,
    val primaryCareArea: String?
)

fun Database.Read.fetchIncomeStatementsAwaitingHandler(
    today: LocalDate,
    areas: List<String>,
    page: Int,
    pageSize: Int
): Paged<IncomeStatementAwaitingHandler> =
    createQuery(
        """
WITH head_of_child_areas AS (
    SELECT puv.head_of_child as person_id, d.care_area_id
    FROM primary_units_view puv
    JOIN daycare d ON d.id = puv.unit_id
),
partnerships_areas AS (
    SELECT
        fp.partnership_id,
        d.care_area_id,
        row_number() OVER (PARTITION BY (fp.partnership_id) ORDER BY child.date_of_birth DESC) AS rownum
    FROM fridge_partner fp
    JOIN primary_units_view puv ON puv.head_of_child = fp.person_id
    JOIN daycare d ON puv.unit_id = d.id
    JOIN person child ON child.id = puv.child_id
    WHERE daterange(start_date, end_date, '[]') @> :today
),
family_areas AS (
    SELECT fp.person_id, pa.care_area_id
    FROM fridge_partner fp
    JOIN partnerships_areas pa ON pa.partnership_id = fp.partnership_id
    WHERE pa.rownum = 1  -- youngest child
),
person_areas AS (
    SELECT DISTINCT ON (pa.person_id) pa.person_id, ca.short_name, ca.name
    FROM (
        SELECT person_id, care_area_id FROM family_areas
        UNION ALL
        SELECT person_id, care_area_id FROM head_of_child_areas
    ) pa
    JOIN care_area ca ON ca.id = pa.care_area_id
)
SELECT i.id,
       i.type,
       i.created,
       i.start_date,
       p.id AS personId,
       p.first_name || ' ' || p.last_name AS personName,
       pa.name AS primaryCareArea,
       COUNT(*) OVER () AS count
FROM income_statement i
    JOIN person p ON i.person_id = p.id
    LEFT JOIN person_areas pa ON pa.person_id = i.person_id
WHERE handler_id IS NULL
AND (cardinality(:areas) = 0 OR pa.short_name = ANY(:areas))
ORDER BY created, start_date
LIMIT :pageSize OFFSET :offset
        """.trimIndent()
    )
        .bind("today", today)
        .bind("areas", areas.toTypedArray())
        .bind("pageSize", pageSize)
        .bind("offset", (page - 1) * pageSize)
        .mapToPaged(pageSize)

fun Database.Read.readIncomeStatementStartDates(personId: PersonId): List<LocalDate> =
    createQuery("SELECT start_date FROM income_statement WHERE person_id = :personId")
        .bind("personId", personId)
        .mapTo<LocalDate>()
        .list()

fun Database.Read.isOwnIncomeStatement(user: AuthenticatedUser.Citizen, id: IncomeStatementId): Boolean =
    createQuery("SELECT 1 FROM income_statement WHERE id = :id AND person_id = :personId")
        .bind("id", id)
        .bind("personId", user.id)
        .mapTo<Int>()
        .any()

fun Database.Read.isChildIncomeStatement(guardian: AuthenticatedUser.Citizen, id: IncomeStatementId): Boolean =
    createQuery("SELECT 1 FROM income_statement statement LEFT JOIN guardian g on g.child_id = statement.person_id WHERE id = :id AND g.guardian_id = :personId")
        .bind("id", id)
        .bind("personId", guardian.id)
        .mapTo<Int>()
        .any()

fun Database.Read.incomeStatementExistsForStartDate(personId: PersonId, startDate: LocalDate): Boolean =
    createQuery("SELECT 1 FROM income_statement WHERE person_id = :personId AND start_date = :startDate")
        .bind("personId", personId)
        .bind("startDate", startDate)
        .mapTo<Int>()
        .any()
