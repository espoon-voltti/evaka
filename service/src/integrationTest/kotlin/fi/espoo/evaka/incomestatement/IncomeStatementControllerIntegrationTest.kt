package fi.espoo.evaka.incomestatement

import com.github.kittinunf.fuel.core.FileDataPart
import com.github.kittinunf.fuel.jackson.objectBody
import com.github.kittinunf.fuel.jackson.responseObject
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.shared.AttachmentId
import fi.espoo.evaka.shared.IncomeStatementId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.asUser
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testAdult_2
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.net.URL
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals

class IncomeStatementControllerIntegrationTest : FullApplicationTest() {
    private val citizen = AuthenticatedUser.Citizen(testAdult_1.id)

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
                attachmentIds = listOf()
            )
        )

        val incomeStatements = getIncomeStatements()
        assertEquals(
            listOf(
                IncomeStatement.HighestFee(
                    incomeStatements[0].id,
                    LocalDate.of(2021, 4, 3),
                    attachments = listOf(),
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
                attachmentIds = listOf()
            )
        )

        val incomeStatements = getIncomeStatements()
        assertEquals(
            listOf(
                IncomeStatement.Gross(
                    id = incomeStatements[0].id,
                    startDate = LocalDate.of(2021, 4, 3),
                    incomeSource = IncomeSource.INCOMES_REGISTER,
                    otherIncome = setOf(OtherGrossIncome.ALIMONY, OtherGrossIncome.PERKS),
                    attachments = listOf(),
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
                attachmentIds = listOf()
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
                    attachments = listOf(),
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
                attachmentIds = listOf()
            )
        )

        val incomeStatements = getIncomeStatements()
        assertEquals(
            listOf(
                IncomeStatement.EntrepreneurLimitedCompany(
                    id = incomeStatements[0].id,
                    startDate = LocalDate.of(2021, 4, 3),
                    incomeSource = IncomeSource.INCOMES_REGISTER,
                    attachments = listOf(),
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
                attachmentIds = listOf()
            )
        )

        val incomeStatements = getIncomeStatements()
        assertEquals(
            listOf(
                IncomeStatement.EntrepreneurPartnership(
                    id = incomeStatements[0].id,
                    startDate = LocalDate.of(2021, 4, 3),
                    attachments = listOf(),
                )
            ),
            incomeStatements,
        )
    }

    @Test
    fun `create an income statement with an attachment`() {
        val attachmentId = uploadAttachment()

        createIncomeStatement(
            IncomeStatementBody.EntrepreneurPartnership(
                startDate = LocalDate.of(2021, 4, 3),
                attachmentIds = listOf(attachmentId)
            )
        )

        val incomeStatements = getIncomeStatements()
        assertEquals(
            listOf(
                IncomeStatement.EntrepreneurPartnership(
                    id = incomeStatements[0].id,
                    startDate = LocalDate.of(2021, 4, 3),
                    attachments = listOf(idToAttachment(attachmentId)),
                )
            ),
            incomeStatements,
        )
    }

    @Test
    fun `create an income statement with an attachment that does not exist`() {
        val attachmentId = AttachmentId(UUID.randomUUID())

        createIncomeStatement(
            IncomeStatementBody.EntrepreneurPartnership(
                startDate = LocalDate.of(2021, 4, 3),
                attachmentIds = listOf(attachmentId)
            ),
            400
        )
    }

    @Test
    fun `create an income statement with someone else's attachment`() {
        val someoneElse = AuthenticatedUser.Citizen(testAdult_2.id)
        val attachmentId = uploadAttachment(someoneElse)

        createIncomeStatement(
            IncomeStatementBody.EntrepreneurPartnership(
                startDate = LocalDate.of(2021, 4, 3),
                attachmentIds = listOf(attachmentId)
            ),
            400
        )
    }

    @Test
    fun `update an income statement`() {
        val attachment1 = uploadAttachment()
        val attachment2 = uploadAttachment()
        val attachment3 = uploadAttachment()

        createIncomeStatement(
            IncomeStatementBody.EntrepreneurLimitedCompany(
                startDate = LocalDate.of(2021, 4, 3),
                incomeSource = IncomeSource.INCOMES_REGISTER,
                attachmentIds = listOf(attachment1)
            )
        )

        val incomeStatementId = getIncomeStatements()[0].id

        updateIncomeStatement(
            incomeStatementId,
            IncomeStatementBody.HighestFee(
                startDate = LocalDate.of(2021, 6, 11),
                attachmentIds = listOf(attachment2, attachment3)
            )
        )

        assertEquals(
            listOf(
                IncomeStatement.HighestFee(
                    id = incomeStatementId,
                    startDate = LocalDate.of(2021, 6, 11),
                    attachments = listOf(idToAttachment(attachment2), idToAttachment(attachment3)),
                ),
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

    private val pngFile: URL = this::class.java.getResource("/attachments-fixtures/espoo-logo.png")!!

    private fun uploadAttachment(user: AuthenticatedUser = citizen): AttachmentId {
        val (_, _, result) = http.upload("/attachments/citizen")
            .add(FileDataPart(File(pngFile.toURI()), name = "file"))
            .asUser(user)
            .responseObject<AttachmentId>(objectMapper)

        return result.get()
    }

    private fun idToAttachment(id: AttachmentId) = Attachment(id, "espoo-logo.png", "image/png")
}
