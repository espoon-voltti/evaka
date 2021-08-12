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

enum class SelfEmployedType {
    ATTACHMENTS,
    ESTIMATION,
}

fun Database.Read.readIncomeStatementsForPerson(
    personId: UUID,
): List<IncomeStatement> =
    createQuery(
        """
SELECT
    id,
    start_date,
    type,
    gross_income_source,
    gross_other_income,
    student,
    alimony,
    entrepreneur_full_time,
    start_of_entrepreneurship,
    spouse_works_in_company,
    self_employed_type,
    self_employed_estimated_monthly_income,
    self_employed_income_start_date,
    self_employed_income_end_date,
    limited_company_income_source,
    partnership,
    startup_grant,
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
        when (row.mapColumn<IncomeStatementType>("type")) {
            IncomeStatementType.HIGHEST_FEE ->
                IncomeStatement.HighestFee(
                    id = id,
                    startDate = startDate,
                )

            IncomeStatementType.INCOME -> {
                val grossIncomeSource = row.mapColumn<IncomeSource?>("gross_income_source")
                val gross = if (grossIncomeSource != null) Gross(
                    incomeSource = grossIncomeSource,
                    otherIncome = row.mapColumn<Array<OtherGrossIncome>>("gross_other_income").toSet(),
                    student = row.mapColumn("student"),
                    alimony = row.mapColumn("alimony"),
                ) else null

                val selfEmployed =
                    when (row.mapColumn<SelfEmployedType?>("self_employed_type")) {
                        SelfEmployedType.ATTACHMENTS ->
                            SelfEmployed.Attachments
                        SelfEmployedType.ESTIMATION ->
                            SelfEmployed.Estimation(
                                estimatedMonthlyIncome = row.mapColumn("self_employed_estimated_monthly_income"),
                                incomeStartDate = row.mapColumn("self_employed_income_start_date"),
                                incomeEndDate = row.mapColumn("self_employed_income_end_date"),
                            )
                        null -> null
                    }

                val limitedCompanyIncomeSource = row.mapColumn<IncomeSource?>("limited_company_income_source")
                val limitedCompany =
                    if (limitedCompanyIncomeSource != null) LimitedCompany(limitedCompanyIncomeSource) else null

                // If one of the entrepreneur columns is non-NULL, assume the entrepreneurship info has been filled
                val fullTime = row.mapColumn<Boolean?>("entrepreneur_full_time")
                val entrepreneur = if (fullTime != null) Entrepreneur(
                    fullTime = fullTime,
                    startOfEntrepreneurship = row.mapColumn<LocalDate>("start_of_entrepreneurship"),
                    spouseWorksInCompany = row.mapColumn<Boolean>("spouse_works_in_company"),
                    startupGrant = row.mapColumn<Boolean>("startup_grant"),
                    selfEmployed = selfEmployed,
                    limitedCompany = limitedCompany,
                    partnership = row.mapColumn<Boolean>("partnership"),
                ) else null

                IncomeStatement.Income(
                    id = id,
                    startDate = startDate,
                    gross = gross,
                    entrepreneur = entrepreneur,
                    otherInfo = row.mapColumn("other_info"),
                    attachments = row.mapJsonColumn<List<Attachment>>("attachments")
                )
            }
        }
    }.list()

private fun <This : SqlStatement<This>> SqlStatement<This>.bindIncomeStatementBody(body: IncomeStatementBody): This =
    bind("startDate", body.startDate)
        .bindNullable("grossIncomeSource", null as IncomeSource?)
        .bindNullable("grossOtherIncome", null as Array<OtherGrossIncome>?)
        .bindNullable("student", null as Boolean?)
        .bindNullable("alimony", null as Boolean?)
        .bindNullable("fullTime", null as Boolean?)
        .bindNullable("startOfEntrepreneurship", null as LocalDate?)
        .bindNullable("spouseWorksInCompany", null as Boolean?)
        .bindNullable("startupGrant", null as Boolean?)
        .bindNullable("selfEmployedType", null as SelfEmployedType?)
        .bindNullable("selfEmployedEstimatedMonthlyIncome", null as Int?)
        .bindNullable("selfEmployedIncomeStartDate", null as LocalDate?)
        .bindNullable("selfEmployedIncomeEndDate", null as LocalDate?)
        .bindNullable("limitedCompanyIncomeSource", null as IncomeSource?)
        .bindNullable("partnership", null as Boolean?)
        .bindNullable("otherInfo", null as String?)
        .run {
            when (body) {
                is IncomeStatementBody.HighestFee ->
                    bind("type", IncomeStatementType.HIGHEST_FEE)
                is IncomeStatementBody.Income -> {
                    bind("type", IncomeStatementType.INCOME)
                        .run { if (body.gross != null) bindGross(body.gross) else this }
                        .run { if (body.entrepreneur != null) bindEntrepreneur(body.entrepreneur) else this }
                        .bind("otherInfo", body.otherInfo)
                }
            }
        }

private fun <This : SqlStatement<This>> SqlStatement<This>.bindGross(gross: Gross): This =
    bind("grossIncomeSource", gross.incomeSource)
        .bind("grossOtherIncome", gross.otherIncome.toTypedArray())
        .bind("student", gross.student)
        .bind("alimony", gross.alimony)

private fun <This : SqlStatement<This>> SqlStatement<This>.bindEntrepreneur(entrepreneur: Entrepreneur): This =
    this.run { if (entrepreneur.selfEmployed != null) bindSelfEmployed(entrepreneur.selfEmployed) else this }
        .run { if (entrepreneur.limitedCompany != null) bindLimitedCompany(entrepreneur.limitedCompany) else this }
        .bind("fullTime", entrepreneur.fullTime)
        .bind("startOfEntrepreneurship", entrepreneur.startOfEntrepreneurship)
        .bind("spouseWorksInCompany", entrepreneur.spouseWorksInCompany)
        .bind("startupGrant", entrepreneur.startupGrant)
        .bind("partnership", entrepreneur.partnership)

private fun <This : SqlStatement<This>> SqlStatement<This>.bindSelfEmployed(selfEmployed: SelfEmployed): This =
    when (selfEmployed) {
        is SelfEmployed.Attachments ->
            bind("selfEmployedType", SelfEmployedType.ATTACHMENTS)
        is SelfEmployed.Estimation -> {
            bind("selfEmployedType", SelfEmployedType.ESTIMATION)
                .bind("selfEmployedEstimatedMonthlyIncome", selfEmployed.estimatedMonthlyIncome)
                .bind("selfEmployedIncomeStartDate", selfEmployed.incomeStartDate)
                .bindNullable("selfEmployedIncomeEndDate", selfEmployed.incomeEndDate)
        }
    }

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
    type, 
    gross_income_source, 
    gross_other_income, 
    student,
    alimony,
    entrepreneur_full_time,
    start_of_entrepreneurship,
    spouse_works_in_company,
    startup_grant,
    self_employed_type,
    self_employed_estimated_monthly_income,
    self_employed_income_start_date, 
    self_employed_income_end_date,
    limited_company_income_source,
    partnership,
    other_info
) VALUES (
    :personId,
    :startDate,
    :type,
    :grossIncomeSource,
    :grossOtherIncome :: other_income_type[],
    :student,
    :alimony,
    :fullTime,
    :startOfEntrepreneurship,
    :spouseWorksInCompany,
    :startupGrant,
    :selfEmployedType,
    :selfEmployedEstimatedMonthlyIncome,
    :selfEmployedIncomeStartDate,
    :selfEmployedIncomeEndDate,
    :limitedCompanyIncomeSource,
    :partnership,
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
    type = :type,
    gross_income_source = :grossIncomeSource,
    gross_other_income = :grossOtherIncome :: other_income_type[],
    student = :student,
    alimony = :alimony,
    entrepreneur_full_time = :fullTime,
    start_of_entrepreneurship = :startOfEntrepreneurship,
    spouse_works_in_company = :spouseWorksInCompany,
    startup_grant = :startupGrant,
    self_employed_type = :selfEmployedType,
    self_employed_estimated_monthly_income = :selfEmployedEstimatedMonthlyIncome,
    self_employed_income_start_date = :selfEmployedIncomeStartDate,
    self_employed_income_end_date = :selfEmployedIncomeEndDate,
    limited_company_income_source = :limitedCompanyIncomeSource,
    partnership = :partnership,
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
