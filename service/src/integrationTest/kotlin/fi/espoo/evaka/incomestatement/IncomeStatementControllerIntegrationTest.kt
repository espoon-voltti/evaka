package fi.espoo.evaka.incomestatement

import com.github.kittinunf.fuel.core.FileDataPart
import com.github.kittinunf.fuel.jackson.objectBody
import com.github.kittinunf.fuel.jackson.responseObject
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.daycare.controllers.utils.Wrapper
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.shared.AttachmentId
import fi.espoo.evaka.shared.IncomeStatementId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.asUser
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.insertTestEmployee
import fi.espoo.evaka.testAdult_1
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.net.URL
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals

class IncomeStatementControllerIntegrationTest : FullApplicationTest() {
    private val employeeId = UUID.randomUUID()
    private val employee = AuthenticatedUser.Employee(employeeId, setOf(UserRole.FINANCE_ADMIN))

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.resetDatabase()
            tx.insertGeneralTestFixtures()
            tx.insertTestEmployee(DevEmployee(id = employeeId, roles = setOf(UserRole.FINANCE_ADMIN)))
        }
    }

    @Test
    fun `set as handled`() {
        val startDate = LocalDate.now()
        val endDate = LocalDate.now().plusDays(30)

        val id = db.transaction { tx ->
            tx.createIncomeStatement(
                testAdult_1.id, IncomeStatementBody.HighestFee(startDate, endDate)
            )
        }

        val incomeStatement1 = getIncomeStatement(id)
        assertEquals(
            IncomeStatement.HighestFee(
                id = id,
                startDate = startDate,
                endDate = endDate,
                created = incomeStatement1.created,
                updated = incomeStatement1.updated,
                handlerName = null,
            ),
            incomeStatement1
        )

        setIncomeStatementHandled(id, true)

        val incomeStatement2 = getIncomeStatement(id)
        assertEquals(
            IncomeStatement.HighestFee(
                id = id,
                startDate = startDate,
                endDate = endDate,
                created = incomeStatement1.created,
                updated = incomeStatement2.updated,
                handlerName = "Test Person",
            ),
            incomeStatement2
        )

        setIncomeStatementHandled(id, false)

        val incomeStatement3 = getIncomeStatement(id)
        assertEquals(
            IncomeStatement.HighestFee(
                id = id,
                startDate = startDate,
                endDate = endDate,
                created = incomeStatement1.created,
                updated = incomeStatement3.updated,
                handlerName = null,
            ),
            incomeStatement3
        )
    }

    @Test
    fun `add an attachment`() {
        val id = db.transaction { tx ->
            tx.createIncomeStatement(
                testAdult_1.id, IncomeStatementBody.Income(
                    startDate = LocalDate.now(),
                    endDate = null,
                    gross = Gross(
                        incomeSource = IncomeSource.ATTACHMENTS,
                        estimatedIncome = null,
                        otherIncome = setOf(),
                    ),
                    entrepreneur = null,
                    student = false,
                    alimonyPayer = false,
                    otherInfo = "",
                    attachmentIds = listOf(),
                )
            )
        }

        val attachmentId = uploadAttachment(id)

        val incomeStatement = getIncomeStatement(id)
        assertEquals(
            IncomeStatement.Income(
                id = id,
                startDate = LocalDate.now(),
                endDate = null,
                gross = Gross(
                    incomeSource = IncomeSource.ATTACHMENTS,
                    estimatedIncome = null,
                    otherIncome = setOf(),
                ),
                entrepreneur = null,
                student = false,
                alimonyPayer = false,
                otherInfo = "",
                attachments = listOf(Attachment(
                    id = attachmentId,
                    name = "espoo-logo.png",
                    contentType = "image/png",
                )),
                created = incomeStatement.created,
                updated = incomeStatement.updated,
                handlerName = null,
            ),
            getIncomeStatement(id)
        )
    }

    private fun getIncomeStatement(id: IncomeStatementId): IncomeStatement =
        http.get("/income-statements/person/${testAdult_1.id}/$id")
            .asUser(employee)
            .responseObject<IncomeStatement>(objectMapper)
            .let { (_, _, body) -> body.get() }

    private fun setIncomeStatementHandled(id: IncomeStatementId, handled: Boolean) {
        http.post("/income-statements/$id/handled")
            .asUser(employee)
            .objectBody(Wrapper(handled), mapper = objectMapper)
            .response()
            .also { (_, res, _) ->
                assertEquals(200, res.statusCode)
            }
    }

    private val pngFile: URL = this::class.java.getResource("/attachments-fixtures/espoo-logo.png")!!

    private fun uploadAttachment(id: IncomeStatementId): AttachmentId {
        val (_, _, result) = http.upload("/attachments/income-statements/$id")
            .add(FileDataPart(File(pngFile.toURI()), name = "file"))
            .asUser(employee)
            .responseObject<AttachmentId>(objectMapper)

        return result.get()
    }

    private fun idToAttachment(id: AttachmentId) = Attachment(id, "espoo-logo.png", "image/png")
}
