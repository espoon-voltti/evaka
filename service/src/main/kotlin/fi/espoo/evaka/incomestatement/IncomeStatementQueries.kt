package fi.espoo.evaka.incomestatement

import fi.espoo.evaka.shared.IncomeStatementId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.bindNullable
import fi.espoo.evaka.shared.db.mapColumn
import fi.espoo.evaka.shared.db.mapJsonColumn
import org.jdbi.v3.core.kotlin.mapTo
import org.jdbi.v3.core.statement.SqlStatement
import java.time.LocalDate
import java.util.UUID

enum class IncomeStatementType {
    HIGHEST_FEE,
    INCOME,
}

fun Database.Read.readIncomeStatementsForPerson(
    personId: UUID,
): List<IncomeStatement> =
    createQuery(
        """
SELECT
    id,
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
    self_employed_attachments,
    self_employed_estimated_monthly_income,
    self_employed_income_start_date,
    self_employed_income_end_date,
    limited_company_income_source,
    partnership,
    light_entrepreneur,
    startup_grant,
    student,
    alimony_payer,
    other_info,
    (SELECT coalesce(jsonb_agg(json_build_object(
        'id', id, 
        'name', name,
        'contentType', content_type
      )), '[]'::jsonb) FROM (
        SELECT id, name, content_type FROM attachment a
        WHERE a.income_statement_id = ist.id 
        ORDER BY a.created
    ) s) AS attachments
FROM income_statement ist
WHERE person_id = :personId
ORDER BY start_date DESC
        """.trimIndent()
    ).bind("personId", personId).map { row ->
        val id = row.mapColumn<IncomeStatementId>("id")
        val startDate = row.mapColumn<LocalDate>("start_date")
        val endDate = row.mapColumn<LocalDate?>("end_date")
        when (row.mapColumn<IncomeStatementType>("type")) {
            IncomeStatementType.HIGHEST_FEE ->
                IncomeStatement.HighestFee(
                    id = id,
                    startDate = startDate,
                    endDate = endDate,
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
                    otherIncome = row.mapColumn<Array<OtherGrossIncome>>("gross_other_income").toSet(),
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

                // If one of the entrepreneur columns is non-NULL, assume the entrepreneurship info has been filled
                val fullTime = row.mapColumn<Boolean?>("entrepreneur_full_time")
                val entrepreneur = if (fullTime != null) Entrepreneur(
                    fullTime = fullTime,
                    startOfEntrepreneurship = row.mapColumn("start_of_entrepreneurship"),
                    spouseWorksInCompany = row.mapColumn("spouse_works_in_company"),
                    startupGrant = row.mapColumn("startup_grant"),
                    selfEmployed = selfEmployed,
                    limitedCompany = limitedCompany,
                    partnership = row.mapColumn("partnership"),
                    lightEntrepreneur = row.mapColumn("light_entrepreneur")
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
                    attachments = row.mapJsonColumn("attachments")
                )
            }
        }
    }.list()

private fun <This : SqlStatement<This>> SqlStatement<This>.bindIncomeStatementBody(body: IncomeStatementBody): This =
    bind("startDate", body.startDate)
        .bindNullable("endDate", body.endDate)
        .bindNullable("grossIncomeSource", null as IncomeSource?)
        .bindNullable("grossEstimatedMonthlyIncome", null as Int?)
        .bindNullable("grossIncomeStartDate", null as LocalDate?)
        .bindNullable("grossIncomeEndDate", null as LocalDate?)
        .bindNullable("grossOtherIncome", null as Array<OtherGrossIncome>?)
        .bindNullable("fullTime", null as Boolean?)
        .bindNullable("startOfEntrepreneurship", null as LocalDate?)
        .bindNullable("spouseWorksInCompany", null as Boolean?)
        .bindNullable("startupGrant", null as Boolean?)
        .bindNullable("selfEmployedAttachments", null as Boolean?)
        .bindNullable("selfEmployedEstimatedMonthlyIncome", null as Int?)
        .bindNullable("selfEmployedIncomeStartDate", null as LocalDate?)
        .bindNullable("selfEmployedIncomeEndDate", null as LocalDate?)
        .bindNullable("limitedCompanyIncomeSource", null as IncomeSource?)
        .bindNullable("partnership", null as Boolean?)
        .bindNullable("lightEntrepreneur", null as Boolean?)
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
        .bind("partnership", entrepreneur.partnership)
        .bind("lightEntrepreneur", entrepreneur.lightEntrepreneur)

private fun <This : SqlStatement<This>> SqlStatement<This>.bindSelfEmployed(selfEmployed: SelfEmployed): This =
    bind("selfEmployedAttachments", selfEmployed.attachments)
        .run { if (selfEmployed.estimatedIncome != null) bindSelfEmployedEstimation(selfEmployed.estimatedIncome) else this }

private fun <This : SqlStatement<This>> SqlStatement<This>.bindSelfEmployedEstimation(estimation: EstimatedIncome): This =
    bind("selfEmployedEstimatedMonthlyIncome", estimation.estimatedMonthlyIncome)
        .bind("selfEmployedIncomeStartDate", estimation.incomeStartDate)
        .bindNullable("selfEmployedIncomeEndDate", estimation.incomeEndDate)

private fun <This : SqlStatement<This>> SqlStatement<This>.bindLimitedCompany(limitedCompany: LimitedCompany): This =
    bind("limitedCompanyIncomeSource", limitedCompany.incomeSource)

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
    self_employed_attachments,
    self_employed_estimated_monthly_income,
    self_employed_income_start_date, 
    self_employed_income_end_date,
    limited_company_income_source,
    partnership,
    light_entrepreneur,
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
    :selfEmployedAttachments,
    :selfEmployedEstimatedMonthlyIncome,
    :selfEmployedIncomeStartDate,
    :selfEmployedIncomeEndDate,
    :limitedCompanyIncomeSource,
    :partnership,
    :lightEntrepreneur,
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
    self_employed_attachments = :selfEmployedAttachments,
    self_employed_estimated_monthly_income = :selfEmployedEstimatedMonthlyIncome,
    self_employed_income_start_date = :selfEmployedIncomeStartDate,
    self_employed_income_end_date = :selfEmployedIncomeEndDate,
    limited_company_income_source = :limitedCompanyIncomeSource,
    partnership = :partnership,
    light_entrepreneur = :lightEntrepreneur,
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
