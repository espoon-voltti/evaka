package fi.espoo.evaka.incomestatement

import com.github.kittinunf.fuel.jackson.objectBody
import com.github.kittinunf.fuel.jackson.responseObject
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.shared.IncomeStatementId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.asUser
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.testAdult_1
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertEquals

class IncomeStatementIntegrationTest : FullApplicationTest() {
    private val citizen = AuthenticatedUser.Employee(testAdult_1.id, setOf(UserRole.END_USER))

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.resetDatabase()
            tx.insertGeneralTestFixtures()
        }
    }

    @Test
    fun `create a highest fee income statement`() {
        createIncomeStatement(
            IncomeStatementBody.HighestFee(
                startDate = LocalDate.of(2021, 4, 3),
            )
        )

        val incomeStatements = getIncomeStatements()
        assertEquals(
            listOf(
                IncomeStatement.HighestFee(
                    incomeStatements[0].id,
                    LocalDate.of(2021, 4, 3),
                )
            ),
            incomeStatements,
        )
    }

    @Test
    fun `create a gross income statement`() {
        createIncomeStatement(
            IncomeStatementBody.Gross(
                startDate = LocalDate.of(2021, 4, 3),
                incomeSource = IncomeSource.INCOMES_REGISTER,
                otherIncome = setOf(OtherGrossIncome.ALIMONY, OtherGrossIncome.PERKS),
            )
        )

        val incomeStatements = getIncomeStatements()
        assertEquals(
            listOf(
                IncomeStatement.Gross(
                    id = incomeStatements[0].id,
                    startDate = LocalDate.of(2021, 4, 3),
                    incomeSource = IncomeSource.INCOMES_REGISTER,
                    otherIncome = setOf(OtherGrossIncome.ALIMONY, OtherGrossIncome.PERKS)
                )
            ),
            incomeStatements,
        )
    }

    @Test
    fun `create a self-employed income statement`() {
        createIncomeStatement(
            IncomeStatementBody.EntrepreneurSelfEmployedEstimation(
                startDate = LocalDate.of(2021, 4, 3),
                estimatedMonthlyIncome = 1234,
                incomeStartDate = LocalDate.of(2020, 1, 1),
                incomeEndDate = null,
            )
        )

        val incomeStatements = getIncomeStatements()
        assertEquals(
            listOf(
                IncomeStatement.EntrepreneurSelfEmployedEstimation(
                    id = incomeStatements[0].id,
                    startDate = LocalDate.of(2021, 4, 3),
                    estimatedMonthlyIncome = 1234,
                    incomeDateRange = DateRange(LocalDate.of(2020, 1, 1), null),
                )
            ),
            incomeStatements,
        )
    }

    @Test
    fun `create a limited company income statement`() {
        createIncomeStatement(
            IncomeStatementBody.EntrepreneurLimitedCompany(
                startDate = LocalDate.of(2021, 4, 3),
                incomeSource = IncomeSource.INCOMES_REGISTER,
            )
        )

        val incomeStatements = getIncomeStatements()
        assertEquals(
            listOf(
                IncomeStatement.EntrepreneurLimitedCompany(
                    id = incomeStatements[0].id,
                    startDate = LocalDate.of(2021, 4, 3),
                    incomeSource = IncomeSource.INCOMES_REGISTER
                )
            ),
            incomeStatements,
        )
    }

    @Test
    fun `create a partnership income statement`() {
        createIncomeStatement(
            IncomeStatementBody.EntrepreneurPartnership(
                startDate = LocalDate.of(2021, 4, 3),
            )
        )

        val incomeStatements = getIncomeStatements()
        assertEquals(
            listOf(
                IncomeStatement.EntrepreneurPartnership(
                    id = incomeStatements[0].id,
                    startDate = LocalDate.of(2021, 4, 3),
                )
            ),
            incomeStatements,
        )
    }

    @Test
    fun `update an income statement`() {
        createIncomeStatement(
            IncomeStatementBody.EntrepreneurLimitedCompany(
                startDate = LocalDate.of(2021, 4, 3),
                incomeSource = IncomeSource.INCOMES_REGISTER
            )
        )

        val incomeStatementId = getIncomeStatements()[0].id

        updateIncomeStatement(
            incomeStatementId,
            IncomeStatementBody.HighestFee(
                startDate = LocalDate.of(2021, 6, 11),
            )
        )

        assertEquals(
            listOf(
                IncomeStatement.HighestFee(
                    id = incomeStatementId,
                    startDate = LocalDate.of(2021, 6, 11),
                )
            ),
            getIncomeStatements(),
        )
    }

    private fun getIncomeStatements(): List<IncomeStatement> =
        http.get("/citizen/income-statements")
            .asUser(citizen)
            .responseObject<List<IncomeStatement>>(objectMapper)
            .let { (_, _, body) -> body.get() }

    private fun createIncomeStatement(body: IncomeStatementBody, expectedStatus: Int = 204) {
        http.post("/citizen/income-statements")
            .asUser(citizen)
            .objectBody(body, mapper = objectMapper)
            .response()
            .also { (_, res, _) ->
                assertEquals(expectedStatus, res.statusCode)
            }
    }

    private fun updateIncomeStatement(id: IncomeStatementId, body: IncomeStatementBody) {
        http.put("/citizen/income-statements/$id")
            .asUser(citizen)
            .objectBody(body, mapper = objectMapper)
            .response()
            .also { (_, res, _) ->
                assertEquals(204, res.statusCode)
            }
    }
}
