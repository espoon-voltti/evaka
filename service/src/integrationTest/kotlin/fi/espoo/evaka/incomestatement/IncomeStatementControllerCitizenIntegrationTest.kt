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
import kotlin.test.assertNotEquals

class IncomeStatementControllerCitizenIntegrationTest : FullApplicationTest() {
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
                endDate = null,
            )
        )

        val incomeStatements = getIncomeStatements()
        assertEquals(
            listOf(
                IncomeStatement.HighestFee(
                    id = incomeStatements[0].id,
                    startDate = LocalDate.of(2021, 4, 3),
                    endDate = null,
                    created = incomeStatements[0].created,
                    updated = incomeStatements[0].updated,
                    handlerName = null,
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
                endDate = LocalDate.of(2021, 8, 9),
                gross = Gross(
                    incomeSource = IncomeSource.INCOMES_REGISTER,
                    estimatedIncome = EstimatedIncome(
                        estimatedMonthlyIncome = 500,
                        incomeStartDate = LocalDate.of(1992, 1, 2),
                        incomeEndDate = LocalDate.of(2099, 9, 9),
                    ),
                    otherIncome = setOf(OtherIncome.ALIMONY, OtherIncome.RENTAL_INCOME),
                ),
                entrepreneur = Entrepreneur(
                    fullTime = true,
                    startOfEntrepreneurship = LocalDate.of(1998, 1, 1),
                    spouseWorksInCompany = false,
                    startupGrant = true,
                    selfEmployed = SelfEmployed(
                        attachments = true,
                        estimatedIncome = EstimatedIncome(
                            estimatedMonthlyIncome = 1000,
                            incomeStartDate = LocalDate.of(2005, 6, 6),
                            incomeEndDate = LocalDate.of(2021, 7, 7),
                        )
                    ),
                    limitedCompany = LimitedCompany(IncomeSource.INCOMES_REGISTER),
                    partnership = false,
                    lightEntrepreneur = true,
                    accountant = Accountant(
                        name = "Foo",
                        address = "Bar",
                        phone = "123",
                        email = "foo.bar@example.com",
                    )
                ),
                student = false,
                alimonyPayer = true,
                otherInfo = "foo bar",
                attachmentIds = listOf(),
            )
        )

        val incomeStatements = getIncomeStatements()
        assertEquals(
            listOf(
                IncomeStatement.Income(
                    id = incomeStatements[0].id,
                    startDate = LocalDate.of(2021, 4, 3),
                    endDate = LocalDate.of(2021, 8, 9),
                    gross = Gross(
                        incomeSource = IncomeSource.INCOMES_REGISTER,
                        estimatedIncome = EstimatedIncome(
                            estimatedMonthlyIncome = 500,
                            incomeStartDate = LocalDate.of(1992, 1, 2),
                            incomeEndDate = LocalDate.of(2099, 9, 9),
                        ),
                        otherIncome = setOf(OtherIncome.ALIMONY, OtherIncome.RENTAL_INCOME),
                    ),
                    entrepreneur = Entrepreneur(
                        fullTime = true,
                        startOfEntrepreneurship = LocalDate.of(1998, 1, 1),
                        spouseWorksInCompany = false,
                        startupGrant = true,
                        selfEmployed = SelfEmployed(
                            attachments = true,
                            estimatedIncome = EstimatedIncome(
                                estimatedMonthlyIncome = 1000,
                                incomeStartDate = LocalDate.of(2005, 6, 6),
                                incomeEndDate = LocalDate.of(2021, 7, 7),
                            )
                        ),
                        limitedCompany = LimitedCompany(IncomeSource.INCOMES_REGISTER),
                        partnership = false,
                        lightEntrepreneur = true,
                        accountant = Accountant(
                            name = "Foo",
                            address = "Bar",
                            phone = "123",
                            email = "foo.bar@example.com",
                        )
                    ),
                    student = false,
                    alimonyPayer = true,
                    otherInfo = "foo bar",
                    created = incomeStatements[0].created,
                    updated = incomeStatements[0].updated,
                    handlerName = null,
                    attachments = listOf(),
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
                endDate = null,
                gross = null,
                entrepreneur = null,
                student = false,
                alimonyPayer = true,
                otherInfo = "foo bar",
                attachmentIds = listOf()
            ),
            400
        )
        createIncomeStatement(
            IncomeStatementBody.Income(
                // endDate is before startDate
                startDate = LocalDate.of(2021, 4, 3),
                endDate = LocalDate.of(2021, 4, 2),
                gross = Gross(
                    incomeSource = IncomeSource.INCOMES_REGISTER,
                    estimatedIncome = null,
                    otherIncome = setOf()
                ),
                entrepreneur = null,
                student = false,
                alimonyPayer = true,
                otherInfo = "foo bar",
                attachmentIds = listOf()
            ),
            400
        )
        createIncomeStatement(
            IncomeStatementBody.Income(
                startDate = LocalDate.of(2021, 4, 3),
                endDate = null,
                gross = null,
                // At least one company type is needed
                entrepreneur = Entrepreneur(
                    fullTime = true,
                    startOfEntrepreneurship = LocalDate.of(2000, 1, 1),
                    spouseWorksInCompany = true,
                    startupGrant = true,
                    selfEmployed = null,
                    limitedCompany = null,
                    partnership = false,
                    lightEntrepreneur = false,
                    accountant = Accountant(
                        name = "Foo",
                        address = "Bar",
                        phone = "123",
                        email = "foo.bar@example.com",
                    )
                ),
                student = false,
                alimonyPayer = false,
                otherInfo = "foo bar",
                attachmentIds = listOf()
            ),
            400
        )
        createIncomeStatement(
            IncomeStatementBody.Income(
                startDate = LocalDate.of(2021, 4, 3),
                endDate = null,
                gross = null,
                entrepreneur = Entrepreneur(
                    fullTime = true,
                    startOfEntrepreneurship = LocalDate.of(2000, 1, 1),
                    spouseWorksInCompany = true,
                    startupGrant = true,
                    selfEmployed = null,
                    limitedCompany = null,
                    partnership = true,
                    lightEntrepreneur = false,
                    // Accountant is required if limitedCompany or partnership is given
                    accountant = null,
                ),
                student = false,
                alimonyPayer = false,
                otherInfo = "foo bar",
                attachmentIds = listOf()
            ),
            400
        )
        createIncomeStatement(
            IncomeStatementBody.Income(
                startDate = LocalDate.of(2021, 4, 3),
                endDate = null,
                gross = null,
                entrepreneur = Entrepreneur(
                    fullTime = true,
                    startOfEntrepreneurship = LocalDate.of(2000, 1, 1),
                    spouseWorksInCompany = true,
                    startupGrant = true,
                    selfEmployed = null,
                    limitedCompany = null,
                    partnership = true,
                    lightEntrepreneur = false,
                    // Accountant name, phone or email cannot be empty
                    accountant = Accountant(
                        name = "",
                        address = "",
                        phone = "",
                        email = "",
                    )
                ),
                student = false,
                alimonyPayer = false,
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
                endDate = null,
                gross = Gross(
                    incomeSource = IncomeSource.ATTACHMENTS,
                    estimatedIncome = null,
                    otherIncome = setOf(),
                ),
                entrepreneur = null,
                student = false,
                alimonyPayer = true,
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
                    endDate = null,
                    gross = Gross(
                        incomeSource = IncomeSource.ATTACHMENTS,
                        estimatedIncome = null,
                        otherIncome = setOf(),
                    ),
                    entrepreneur = null,
                    student = false,
                    alimonyPayer = true,
                    otherInfo = "foo bar",
                    created = incomeStatements[0].created,
                    updated = incomeStatements[0].updated,
                    handlerName = null,
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
                endDate = null,
                gross = Gross(
                    incomeSource = IncomeSource.ATTACHMENTS,
                    estimatedIncome = null,
                    otherIncome = setOf(),
                ),
                entrepreneur = null,
                student = false,
                alimonyPayer = false,
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
                endDate = null,
                gross = Gross(
                    incomeSource = IncomeSource.ATTACHMENTS,
                    estimatedIncome = null,
                    otherIncome = setOf(),
                ),
                entrepreneur = null,
                student = false,
                alimonyPayer = false,
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
                endDate = LocalDate.of(2021, 8, 9),
                gross = Gross(
                    incomeSource = IncomeSource.INCOMES_REGISTER,
                    estimatedIncome = EstimatedIncome(
                        estimatedMonthlyIncome = 500,
                        incomeStartDate = LocalDate.of(1992, 1, 2),
                        incomeEndDate = LocalDate.of(2099, 9, 9),
                    ),
                    otherIncome = setOf(OtherIncome.ALIMONY, OtherIncome.RENTAL_INCOME),
                ),
                entrepreneur = Entrepreneur(
                    fullTime = true,
                    startOfEntrepreneurship = LocalDate.of(1998, 1, 1),
                    spouseWorksInCompany = false,
                    startupGrant = true,
                    selfEmployed = SelfEmployed(
                        attachments = true,
                        estimatedIncome = EstimatedIncome(
                            estimatedMonthlyIncome = 1000,
                            incomeStartDate = LocalDate.of(2005, 6, 6),
                            incomeEndDate = LocalDate.of(2021, 7, 7),
                        )
                    ),
                    limitedCompany = LimitedCompany(
                        incomeSource = IncomeSource.INCOMES_REGISTER,
                    ),
                    partnership = false,
                    lightEntrepreneur = false,
                    Accountant(
                        name = "Foo",
                        address = "Bar",
                        phone = "123",
                        email = "foo.bar@example.com",
                    )
                ),
                student = false,
                alimonyPayer = true,
                otherInfo = "foo bar",
                attachmentIds = listOf(attachment1)
            ),
        )

        val incomeStatement = getIncomeStatements()[0]

        updateIncomeStatement(
            incomeStatement.id,
            IncomeStatementBody.Income(
                startDate = LocalDate.of(2021, 6, 11),
                endDate = null,
                gross = null,
                entrepreneur = Entrepreneur(
                    fullTime = false,
                    startOfEntrepreneurship = LocalDate.of(2019, 1, 1),
                    spouseWorksInCompany = true,
                    startupGrant = false,
                    selfEmployed = null,
                    limitedCompany = null,
                    partnership = true,
                    lightEntrepreneur = false,
                    Accountant(
                        name = "Baz",
                        address = "Quux",
                        phone = "456",
                        email = "baz.quux@example.com",
                    )
                ),
                student = true,
                alimonyPayer = false,
                otherInfo = "",
                attachmentIds = listOf(attachment2, attachment3)
            )
        )

        val updated = getIncomeStatement(incomeStatement.id).updated
        assertNotEquals(incomeStatement.updated, updated)

        assertEquals(
            IncomeStatement.Income(
                id = incomeStatement.id,
                startDate = LocalDate.of(2021, 6, 11),
                endDate = null,
                gross = null,
                entrepreneur = Entrepreneur(
                    fullTime = false,
                    startOfEntrepreneurship = LocalDate.of(2019, 1, 1),
                    spouseWorksInCompany = true,
                    startupGrant = false,
                    selfEmployed = null,
                    limitedCompany = null,
                    partnership = true,
                    lightEntrepreneur = false,
                    Accountant(
                        name = "Baz",
                        address = "Quux",
                        phone = "456",
                        email = "baz.quux@example.com",
                    )
                ),
                student = true,
                alimonyPayer = false,
                otherInfo = "",
                created = incomeStatement.created,
                updated = updated,
                handlerName = null,
                attachments = listOf(idToAttachment(attachment2), idToAttachment(attachment3)),
            ),
            getIncomeStatement(incomeStatement.id),
        )
    }

    private fun getIncomeStatements(): List<IncomeStatement> =
        http.get("/citizen/income-statements")
            .timeout(1000000)
            .timeoutRead(1000000)
            .asUser(citizen)
            .responseObject<List<IncomeStatement>>(objectMapper)
            .let { (_, _, body) -> body.get() }

    private fun getIncomeStatement(id: IncomeStatementId): IncomeStatement =
        http.get("/citizen/income-statements/$id")
            .timeout(1000000)
            .timeoutRead(1000000)
            .asUser(citizen)
            .responseObject<IncomeStatement>(objectMapper)
            .let { (_, _, body) -> body.get() }

    private fun createIncomeStatement(body: IncomeStatementBody, expectedStatus: Int = 200) {
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
                assertEquals(200, res.statusCode)
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
