package fi.espoo.evaka.incomestatement

import fi.espoo.evaka.shared.IncomeStatementId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.bindNullable
import fi.espoo.evaka.shared.db.mapColumn
import fi.espoo.evaka.shared.db.mapJsonColumn
import fi.espoo.evaka.shared.domain.DateRange
import org.jdbi.v3.core.kotlin.mapTo
import org.jdbi.v3.core.statement.SqlStatement
import java.time.LocalDate
import java.util.UUID

enum class IncomeType {
    HIGHEST_FEE,
    GROSS,
    ENTREPRENEUR_SELF_EMPLOYED_ESTIMATION,
    ENTREPRENEUR_SELF_EMPLOYED_ATTACHMENTS,
    ENTREPRENEUR_LIMITED_COMPANY,
    ENTREPRENEUR_PARTNERSHIP,
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
    income_source,
    other_income,
    self_employed_estimated_monthly_income,
    self_employed_income_start_date,
    self_employed_income_end_date,
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
        val attachments = row.mapJsonColumn<List<Attachment>>("attachments")
        when (row.mapColumn<IncomeType>("type")) {
            IncomeType.HIGHEST_FEE ->
                IncomeStatement.HighestFee(
                    id = id,
                    startDate = startDate,
                    attachments = attachments,
                )

            IncomeType.GROSS ->
                IncomeStatement.Gross(
                    id = id,
                    startDate = startDate,
                    incomeSource = row.mapColumn("income_source"),
                    otherIncome = row.mapColumn<Array<OtherGrossIncome>>("other_income").toSet(),
                    attachments = attachments,
                )

            IncomeType.ENTREPRENEUR_SELF_EMPLOYED_ESTIMATION -> {
                val incomeStartDate = row.mapColumn<LocalDate>("self_employed_income_start_date")
                val incomeEndDate = row.mapColumn<LocalDate?>("self_employed_income_end_date")
                IncomeStatement.EntrepreneurSelfEmployedEstimation(
                    id = id,
                    startDate = startDate,
                    estimatedMonthlyIncome = row.mapColumn("self_employed_estimated_monthly_income"),
                    incomeDateRange = DateRange(incomeStartDate, incomeEndDate),
                    attachments = attachments,
                )
            }

            IncomeType.ENTREPRENEUR_SELF_EMPLOYED_ATTACHMENTS ->
                IncomeStatement.EntrepreneurSelfEmployedAttachments(
                    id = id,
                    startDate = startDate,
                    attachments = attachments,
                )

            IncomeType.ENTREPRENEUR_LIMITED_COMPANY ->
                IncomeStatement.EntrepreneurLimitedCompany(
                    id = id,
                    startDate = startDate,
                    incomeSource = row.mapColumn("income_source"),
                    attachments = attachments,
                )

            IncomeType.ENTREPRENEUR_PARTNERSHIP ->
                IncomeStatement.EntrepreneurPartnership(
                    id = id,
                    startDate = startDate,
                    attachments = attachments,
                )
        }
    }.list()

private fun <This : SqlStatement<This>> SqlStatement<This>.bindIncomeStatementBody(body: IncomeStatementBody): This =
    bind("startDate", body.startDate)
        .bindNullable("incomeSource", null as IncomeSource?)
        .bindNullable("otherIncome", null as Array<OtherGrossIncome>?)
        .bindNullable("estimatedMonthlyIncome", null as Int?)
        .bindNullable("incomeStartDate", null as LocalDate?)
        .bindNullable("incomeEndDate", null as LocalDate?)
        .let {
            when (body) {
                is IncomeStatementBody.HighestFee ->
                    it.bind("type", IncomeType.HIGHEST_FEE)
                is IncomeStatementBody.Gross ->
                    it.bind("type", IncomeType.GROSS)
                        .bind("incomeSource", body.incomeSource)
                        .bind("otherIncome", body.otherIncome.toTypedArray())
                is IncomeStatementBody.EntrepreneurSelfEmployedEstimation ->
                    it.bind("type", IncomeType.ENTREPRENEUR_SELF_EMPLOYED_ESTIMATION)
                        .bind("estimatedMonthlyIncome", body.estimatedMonthlyIncome)
                        .bind("incomeStartDate", body.incomeStartDate)
                        .bind("incomeEndDate", body.incomeEndDate)
                is IncomeStatementBody.EntrepreneurSelfEmployedAttachments ->
                    it.bind("type", IncomeType.ENTREPRENEUR_SELF_EMPLOYED_ATTACHMENTS)
                is IncomeStatementBody.EntrepreneurLimitedCompany ->
                    it.bind("type", IncomeType.ENTREPRENEUR_LIMITED_COMPANY)
                        .bind("incomeSource", body.incomeSource)
                is IncomeStatementBody.EntrepreneurPartnership ->
                    it.bind("type", IncomeType.ENTREPRENEUR_PARTNERSHIP)
            }
        }

fun Database.Transaction.createIncomeStatement(personId: UUID, body: IncomeStatementBody): IncomeStatementId {
    return createQuery(
        """
INSERT INTO income_statement (
    person_id,
    start_date, 
    type, 
    income_source, 
    other_income, 
    self_employed_estimated_monthly_income,
    self_employed_income_start_date, 
    self_employed_income_end_date
) VALUES (
    :personId,
    :startDate,
    :type,
    :incomeSource,
    :otherIncome :: other_income_type[],
    :estimatedMonthlyIncome,
    :incomeStartDate,
    :incomeEndDate
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
    income_source = :incomeSource,
    other_income = :otherIncome :: other_income_type[],
    self_employed_estimated_monthly_income = :estimatedMonthlyIncome,
    self_employed_income_start_date = :incomeStartDate,
    self_employed_income_end_date = :incomeEndDate
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
