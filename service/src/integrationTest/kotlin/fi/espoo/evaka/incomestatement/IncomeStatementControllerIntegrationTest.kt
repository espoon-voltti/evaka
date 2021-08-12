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
    fun `create an income statement`() {
        createIncomeStatement(
            IncomeStatementBody.Income(
                startDate = LocalDate.of(2021, 4, 3),
                gross = Gross(
                    incomeSource = IncomeSource.INCOMES_REGISTER,
                    otherIncome = setOf(OtherGrossIncome.ALIMONY, OtherGrossIncome.PERKS),
                    student = false,
                    alimony = true,
                ),
                entrepreneur = Entrepreneur(
                    fullTime = true,
                    startOfEntrepreneurship = LocalDate.of(1998, 1, 1),
                    spouseWorksInCompany = false,
                    startupGrant = true,
                    selfEmployed = SelfEmployed.Attachments,
                    limitedCompany = LimitedCompany(
                        incomeSource = IncomeSource.INCOMES_REGISTER,
                    ),
                    partnership = false,
                ),
                otherInfo = "foo bar",
                attachmentIds = listOf()
            )
        )

        val incomeStatements = getIncomeStatements()
        assertEquals(
            listOf(
                IncomeStatement.Income(
                    id = incomeStatements[0].id,
                    startDate = LocalDate.of(2021, 4, 3),
                    gross = Gross(
                        incomeSource = IncomeSource.INCOMES_REGISTER,
                        otherIncome = setOf(OtherGrossIncome.ALIMONY, OtherGrossIncome.PERKS),
                        student = false,
                        alimony = true,
                    ),
                    entrepreneur = Entrepreneur(
                        fullTime = true,
                        startOfEntrepreneurship = LocalDate.of(1998, 1, 1),
                        spouseWorksInCompany = false,
                        startupGrant = true,
                        selfEmployed = SelfEmployed.Attachments,
                        limitedCompany = LimitedCompany(
                            incomeSource = IncomeSource.INCOMES_REGISTER,
                        ),
                        partnership = false,
                    ),
                    otherInfo = "foo bar",
                    attachments = listOf()
                )
            ),
            incomeStatements,
        )
    }

    @Test
    fun `create an invalid income statement`() {
        createIncomeStatement(
            // Either gross or entrepreneur is needed
            IncomeStatementBody.Income(
                startDate = LocalDate.of(2021, 4, 3),
                gross = null,
                entrepreneur = null,
                otherInfo = "foo bar",
                attachmentIds = listOf()
            ),
            400
        )
        createIncomeStatement(
            // Either gross or entrepreneur is needed
            IncomeStatementBody.Income(
                startDate = LocalDate.of(2021, 4, 3),
                gross = null,
                entrepreneur = Entrepreneur(
                    fullTime = true,
                    startOfEntrepreneurship = LocalDate.of(2000, 1, 1),
                    spouseWorksInCompany = true,
                    startupGrant = true,
                    selfEmployed = null,
                    limitedCompany = null,
                    partnership = false
                ),
                otherInfo = "foo bar",
                attachmentIds = listOf()
            ),
            400
        )
    }

    @Test
    fun `create an income statement with an attachment`() {
        val attachmentId = uploadAttachment()

        createIncomeStatement(
            IncomeStatementBody.Income(
                startDate = LocalDate.of(2021, 4, 3),
                gross = Gross(
                    incomeSource = IncomeSource.ATTACHMENTS,
                    otherIncome = setOf(),
                    student = false,
                    alimony = true,
                ),
                entrepreneur = null,
                otherInfo = "foo bar",
                attachmentIds = listOf(attachmentId)
            )
        )

        val incomeStatements = getIncomeStatements()
        assertEquals(
            listOf(
                IncomeStatement.Income(
                    id = incomeStatements[0].id,
                    startDate = LocalDate.of(2021, 4, 3),
                    gross = Gross(
                        incomeSource = IncomeSource.ATTACHMENTS,
                        otherIncome = setOf(),
                        student = false,
                        alimony = true,
                    ),
                    entrepreneur = null,
                    otherInfo = "foo bar",
                    attachments = listOf(idToAttachment(attachmentId)),
                )
            ),
            incomeStatements,
        )
    }

    @Test
    fun `create an income statement with an attachment that does not exist`() {
        val nonExistingAttachmentId = AttachmentId(UUID.randomUUID())

        createIncomeStatement(
            IncomeStatementBody.Income(
                startDate = LocalDate.of(2021, 4, 3),
                gross = Gross(
                    incomeSource = IncomeSource.ATTACHMENTS,
                    otherIncome = setOf(),
                    student = false,
                    alimony = true,
                ),
                entrepreneur = null,
                otherInfo = "foo bar",
                attachmentIds = listOf(nonExistingAttachmentId)
            ),
            400
        )
    }

    @Test
    fun `create an income statement with someone else's attachment`() {
        val someoneElse = AuthenticatedUser.Citizen(testAdult_2.id)
        val attachmentId = uploadAttachment(someoneElse)

        createIncomeStatement(
            IncomeStatementBody.Income(
                startDate = LocalDate.of(2021, 4, 3),
                gross = Gross(
                    incomeSource = IncomeSource.ATTACHMENTS,
                    otherIncome = setOf(),
                    student = false,
                    alimony = true,
                ),
                entrepreneur = null,
                otherInfo = "foo bar",
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
            IncomeStatementBody.Income(
                startDate = LocalDate.of(2021, 4, 3),
                gross = Gross(
                    incomeSource = IncomeSource.INCOMES_REGISTER,
                    otherIncome = setOf(OtherGrossIncome.ALIMONY, OtherGrossIncome.PERKS),
                    student = false,
                    alimony = true,
                ),
                entrepreneur = Entrepreneur(
                    fullTime = true,
                    startOfEntrepreneurship = LocalDate.of(1998, 1, 1),
                    spouseWorksInCompany = false,
                    startupGrant = true,
                    selfEmployed = SelfEmployed.Attachments,
                    limitedCompany = LimitedCompany(
                        incomeSource = IncomeSource.INCOMES_REGISTER,
                    ),
                    partnership = false,
                ),
                otherInfo = "foo bar",
                attachmentIds = listOf(attachment1)
            ),
        )

        val incomeStatementId = getIncomeStatements()[0].id

        updateIncomeStatement(
            incomeStatementId,
            IncomeStatementBody.Income(
                startDate = LocalDate.of(2021, 6, 11),
                gross = null,
                entrepreneur = Entrepreneur(
                    fullTime = false,
                    startOfEntrepreneurship = LocalDate.of(2019, 1, 1),
                    spouseWorksInCompany = true,
                    startupGrant = false,
                    selfEmployed = null,
                    limitedCompany = null,
                    partnership = true,
                ),
                otherInfo = "",
                attachmentIds = listOf(attachment2, attachment3)
            )
        )

        assertEquals(
            listOf(
                IncomeStatement.Income(
                    id = incomeStatementId,
                    startDate = LocalDate.of(2021, 6, 11),
                    gross = null,
                    entrepreneur = Entrepreneur(
                        fullTime = false,
                        startOfEntrepreneurship = LocalDate.of(2019, 1, 1),
                        spouseWorksInCompany = true,
                        startupGrant = false,
                        selfEmployed = null,
                        limitedCompany = null,
                        partnership = true,
                    ),
                    otherInfo = "",
                    attachments = listOf(idToAttachment(attachment2), idToAttachment(attachment3)),
                ),
            ),
            getIncomeStatements(),
        )
    }

    private fun getIncomeStatements(): List<IncomeStatement> =
        http.get("/citizen/income-statements")
            .timeout(1000000)
            .timeoutRead(1000000)
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
