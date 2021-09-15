// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.incomestatement

import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.IncomeStatementId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.bindNullable
import fi.espoo.evaka.shared.db.mapColumn
import fi.espoo.evaka.shared.db.mapJsonColumn
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import org.jdbi.v3.core.kotlin.mapTo
import org.jdbi.v3.core.result.RowView
import org.jdbi.v3.core.statement.SqlStatement
import java.time.LocalDate
import java.util.UUID

enum class IncomeStatementType {
    HIGHEST_FEE,
    INCOME,
}

private fun selectQuery(single: Boolean, excludeEmployeeAttachments: Boolean): String {
    // language=SQL
    return """
SELECT
    ist.id,
    start_date,
    end_date,
    type,
    gross_income_source,
    gross_estimated_monthly_income,
    gross_income_start_date,
    gross_income_end_date,
    gross_other_income,
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
    e.first_name || ' ' || e.last_name as handler_name,
    (SELECT coalesce(jsonb_agg(json_build_object(
        'id', id, 
        'name', name,
        'contentType', content_type,
        'uploadedByEmployee', uploaded_by_employee
      )), '[]'::jsonb) FROM (
        SELECT id, name, content_type, uploaded_by_employee IS NOT NULL AS uploaded_by_employee
        FROM attachment a
        WHERE a.income_statement_id = ist.id 
        ${if (excludeEmployeeAttachments) "AND uploaded_by_employee IS NULL" else ""}
        ORDER BY a.created
    ) s) AS attachments
FROM income_statement ist
LEFT JOIN employee e on ist.handler_id = e.id
WHERE person_id = :personId
${if (single) "AND ist.id = :id" else ""}
ORDER BY start_date DESC
        """
}

private fun mapIncomeStatement(row: RowView): IncomeStatement {
    val id = row.mapColumn<IncomeStatementId>("id")
    val startDate = row.mapColumn<LocalDate>("start_date")
    val endDate = row.mapColumn<LocalDate?>("end_date")
    val created = row.mapColumn<HelsinkiDateTime>("created")
    val updated = row.mapColumn<HelsinkiDateTime>("updated")
    val handlerName = row.mapColumn<String?>("handler_name")
    return when (row.mapColumn<IncomeStatementType>("type")) {
        IncomeStatementType.HIGHEST_FEE ->
            IncomeStatement.HighestFee(
                id = id,
                startDate = startDate,
                endDate = endDate,
                created = created,
                updated = updated,
                handlerName = handlerName,
            )

        IncomeStatementType.INCOME -> {
            val grossIncomeSource = row.mapColumn<IncomeSource?>("gross_income_source")
            val grossEstimatedMonthlyIncome = row.mapColumn<Int?>("gross_estimated_monthly_income")
            val gross = if (grossIncomeSource != null) Gross(
                incomeSource = grossIncomeSource,
                estimatedIncome = if (grossEstimatedMonthlyIncome != null) EstimatedIncome(
                    grossEstimatedMonthlyIncome,
                    incomeStartDate = row.mapColumn("gross_income_start_date"),
                    incomeEndDate = row.mapColumn("gross_income_end_date")
                ) else null,
                otherIncome = row.mapColumn<Array<OtherIncome>>("gross_other_income").toSet(),
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
                startDate = startDate,
                endDate = endDate,
                gross = gross,
                entrepreneur = entrepreneur,
                student = row.mapColumn("student"),
                alimonyPayer = row.mapColumn("alimony_payer"),
                otherInfo = row.mapColumn("other_info"),
                created = created,
                updated = updated,
                handlerName = handlerName,
                attachments = row.mapJsonColumn("attachments")
            )
        }
    }
}

fun Database.Read.readIncomeStatementsForPerson(
    personId: UUID,
    excludeEmployeeAttachments: Boolean
): List<IncomeStatement> =
    createQuery(selectQuery(false, excludeEmployeeAttachments))
        .bind("personId", personId)
        .map { row -> mapIncomeStatement(row) }
        .list()

fun Database.Read.readIncomeStatementForPerson(
    personId: UUID,
    incomeStatementId: IncomeStatementId,
    excludeEmployeeAttachments: Boolean
): IncomeStatement? =
    createQuery(selectQuery(true, excludeEmployeeAttachments))
        .bind("personId", personId)
        .bind("id", incomeStatementId)
        .map { row -> mapIncomeStatement(row) }
        .firstOrNull()

private fun <This : SqlStatement<This>> SqlStatement<This>.bindIncomeStatementBody(body: IncomeStatementBody): This =
    bind("startDate", body.startDate)
        .bindNullable("endDate", body.endDate)
        .bindNullable("grossIncomeSource", null as IncomeSource?)
        .bindNullable("grossEstimatedMonthlyIncome", null as Int?)
        .bindNullable("grossIncomeStartDate", null as LocalDate?)
        .bindNullable("grossIncomeEndDate", null as LocalDate?)
        .bindNullable("grossOtherIncome", null as Array<OtherIncome>?)
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
        .run { if (gross.estimatedIncome != null) bindGrossEstimation(gross.estimatedIncome) else this }
        .bind("grossOtherIncome", gross.otherIncome.toTypedArray())

private fun <This : SqlStatement<This>> SqlStatement<This>.bindGrossEstimation(estimation: EstimatedIncome): This =
    bind("grossEstimatedMonthlyIncome", estimation.estimatedMonthlyIncome)
        .bind("grossIncomeStartDate", estimation.incomeStartDate)
        .bindNullable("grossIncomeEndDate", estimation.incomeEndDate)

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
    personId: UUID,
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
    gross_income_start_date,
    gross_income_end_date,
    gross_other_income, 
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
    :grossIncomeStartDate,
    :grossIncomeEndDate,
    :grossOtherIncome :: other_income_type[],
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
    personId: UUID,
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
    gross_income_start_date = :grossIncomeStartDate,
    gross_income_end_date = :grossIncomeEndDate,
    gross_other_income = :grossOtherIncome :: other_income_type[],
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
  AND person_id = :personId
        """.trimIndent()
    )
        .bind("id", incomeStatementId)
        .bind("personId", personId)
        .bindIncomeStatementBody(body)
        .execute()

    return rowCount == 1
}

fun Database.Transaction.setIncomeStatementHandler(handlerId: EmployeeId?, incomeStatementId: IncomeStatementId) {
    createUpdate("UPDATE income_statement SET handler_id = :handlerId WHERE id = :id")
        .bindNullable("handlerId", handlerId)
        .bind("id", incomeStatementId)
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
    val id: UUID,
    val created: HelsinkiDateTime,
    val startDate: LocalDate,
    val type: IncomeStatementType,
    val personId: UUID,
    val personName: String,
    val primaryCareArea: String?
)

fun Database.Read.fetchIncomeStatementsAwaitingHandler(): List<IncomeStatementAwaitingHandler> =
    createQuery(
        """
WITH areas AS (
    SELECT area.name, puv.head_of_child
    FROM care_area area
        JOIN daycare daycare ON area.id = daycare.care_area_id
        JOIN primary_units_view puv ON puv.unit_id = daycare.id
)

SELECT i.id,
       i.type,
       i.created,
       i.start_date,
       p.id AS personId,
       p.first_name || ' ' || p.last_name AS personName,
       areas.name AS primaryCareArea
FROM income_statement i
    JOIN person p ON i.person_id = p.id
    LEFT JOIN areas ON areas.head_of_child = p.id
WHERE handler_id IS NULL
ORDER BY start_date DESC
LIMIT 50
        """.trimIndent()
    )
        .mapTo<IncomeStatementAwaitingHandler>()
        .list()

fun Database.Read.isOwnIncomeStatement(id: IncomeStatementId, personId: UUID): Boolean =
    createQuery("SELECT 1 FROM income_statement WHERE id = :id AND person_id = :personId")
        .bind("id", id)
        .bind("personId", personId)
        .mapTo<Int>()
        .any()
