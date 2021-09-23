// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.incomestatement

import com.github.kittinunf.fuel.core.FileDataPart
import com.github.kittinunf.fuel.jackson.objectBody
import com.github.kittinunf.fuel.jackson.responseObject
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.daycare.controllers.utils.Wrapper
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.shared.AttachmentId
import fi.espoo.evaka.shared.IncomeStatementId
import fi.espoo.evaka.shared.Paged
import fi.espoo.evaka.shared.PlacementId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.asUser
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.insertTestEmployee
import fi.espoo.evaka.shared.dev.insertTestParentship
import fi.espoo.evaka.shared.dev.insertTestPlacement
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testAdult_2
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testChild_2
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDaycare2
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
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
                handled = false,
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
                handled = true,
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
                handled = false,
            ),
            incomeStatement3
        )
    }

    @Test
    fun `add an attachment`() {
        val id = db.transaction { tx ->
            tx.createIncomeStatement(
                testAdult_1.id,
                IncomeStatementBody.Income(
                    startDate = LocalDate.now(),
                    endDate = null,
                    gross = Gross(
                        incomeSource = IncomeSource.ATTACHMENTS,
                        estimatedMonthlyIncome = null,
                        otherIncome = setOf(),
                        otherIncomeInfo = "",
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
                    estimatedMonthlyIncome = null,
                    otherIncome = setOf(),
                    otherIncomeInfo = "",
                ),
                entrepreneur = null,
                student = false,
                alimonyPayer = false,
                otherInfo = "",
                attachments = listOf(
                    Attachment(
                        id = attachmentId,
                        name = "evaka-logo.png",
                        contentType = "image/png",
                        uploadedByEmployee = true,
                    )
                ),
                created = incomeStatement.created,
                updated = incomeStatement.updated,
                handled = false,
            ),
            getIncomeStatement(id)
        )
    }

    private fun createTestIncomeStatement(personId: UUID): IncomeStatement {
        val id = db.transaction { tx ->
            tx.createIncomeStatement(
                personId,
                IncomeStatementBody.HighestFee(
                    startDate = LocalDate.now(),
                    endDate = null,
                )
            )
        }
        return db.read { it.readIncomeStatementForPerson(personId, id, true)!! }
    }

    @Test
    fun `list income statements awaiting handler - no income statements`() {
        assertEquals(
            Paged(listOf(), 0, 1),
            getIncomeStatementsAwaitingHandler(listOf())
        )
    }

    @Test
    fun `list income statements awaiting handler - no area`() {
        val incomeStatement = createTestIncomeStatement(testAdult_1.id)
        assertEquals(
            Paged(
                listOf(
                    IncomeStatementAwaitingHandler(
                        id = incomeStatement.id.raw,
                        created = incomeStatement.created,
                        startDate = incomeStatement.startDate,
                        type = IncomeStatementType.HIGHEST_FEE,
                        personId = testAdult_1.id,
                        personName = "John Doe",
                        primaryCareArea = null,
                    )
                ),
                1, 1
            ),
            getIncomeStatementsAwaitingHandler(listOf())
        )
    }

    @Test
    fun `list income statements awaiting handler - has area`() {
        val placementId1 = PlacementId(UUID.randomUUID())
        val placementId2 = PlacementId(UUID.randomUUID())
        val placementStart = LocalDate.now().minusDays(30)
        val placementEnd = LocalDate.now().plusDays(30)
        db.transaction { tx ->
            tx.insertTestParentship(testAdult_1.id, testChild_1.id, startDate = placementStart, endDate = placementEnd)
            tx.insertTestPlacement(
                id = placementId1,
                childId = testChild_1.id,
                unitId = testDaycare.id,
                startDate = placementStart,
                endDate = placementEnd,
                type = PlacementType.PRESCHOOL_DAYCARE
            )

            tx.insertTestParentship(testAdult_2.id, testChild_2.id, startDate = placementStart, endDate = placementEnd)
            tx.insertTestPlacement(
                id = placementId2,
                childId = testChild_2.id,
                unitId = testDaycare2.id,
                startDate = placementStart,
                endDate = placementEnd,
                type = PlacementType.PRESCHOOL_DAYCARE
            )
        }

        val incomeStatement1 = createTestIncomeStatement(testAdult_1.id)
        val incomeStatement2 = createTestIncomeStatement(testAdult_2.id)

        assertEquals(
            Paged(
                listOf(
                    IncomeStatementAwaitingHandler(
                        id = incomeStatement1.id.raw,
                        created = incomeStatement1.created,
                        startDate = incomeStatement1.startDate,
                        type = IncomeStatementType.HIGHEST_FEE,
                        personId = testAdult_1.id,
                        personName = "John Doe",
                        primaryCareArea = "Test Area",
                    ),
                    IncomeStatementAwaitingHandler(
                        id = incomeStatement2.id.raw,
                        created = incomeStatement2.created,
                        startDate = incomeStatement2.startDate,
                        type = IncomeStatementType.HIGHEST_FEE,
                        personId = testAdult_2.id,
                        personName = "Joan Doe",
                        primaryCareArea = "Lwiz Foo",
                    )
                ),
                2, 1
            ),
            getIncomeStatementsAwaitingHandler(listOf())
        )
    }

    @Test
    fun `list income statements awaiting handler - area filter`() {
        val placementId1 = PlacementId(UUID.randomUUID())
        val placementId2 = PlacementId(UUID.randomUUID())
        val placementStart = LocalDate.now().minusDays(30)
        val placementEnd = LocalDate.now().plusDays(30)
        db.transaction { tx ->
            tx.insertTestParentship(testAdult_1.id, testChild_1.id, startDate = placementStart, endDate = placementEnd)
            tx.insertTestPlacement(
                id = placementId1,
                childId = testChild_1.id,
                unitId = testDaycare.id,
                startDate = placementStart,
                endDate = placementEnd,
                type = PlacementType.PRESCHOOL_DAYCARE
            )

            tx.insertTestParentship(testAdult_2.id, testChild_2.id, startDate = placementStart, endDate = placementEnd)
            tx.insertTestPlacement(
                id = placementId2,
                childId = testChild_2.id,
                unitId = testDaycare2.id,
                startDate = placementStart,
                endDate = placementEnd,
                type = PlacementType.PRESCHOOL_DAYCARE
            )
        }

        createTestIncomeStatement(testAdult_1.id)
        val incomeStatement2 = createTestIncomeStatement(testAdult_2.id)

        assertEquals(
            Paged(
                listOf(
                    IncomeStatementAwaitingHandler(
                        id = incomeStatement2.id.raw,
                        created = incomeStatement2.created,
                        startDate = incomeStatement2.startDate,
                        type = IncomeStatementType.HIGHEST_FEE,
                        personId = testAdult_2.id,
                        personName = "Joan Doe",
                        primaryCareArea = "Lwiz Foo",
                    )
                ),
                1, 1
            ),
            // short name for the "Lwiz Foo" care area is "short name 2"
            getIncomeStatementsAwaitingHandler(listOf("short name 2"))
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

    private fun getIncomeStatementsAwaitingHandler(
        areas: List<String>,
        page: Int = 1,
        pageSize: Int = 50
    ): Paged<IncomeStatementAwaitingHandler> =
        http.get("/income-statements/awaiting-handler?areas=${areas.joinToString(",")}&page=$page&pageSize=$pageSize")
            .asUser(employee)
            .responseObject<Paged<IncomeStatementAwaitingHandler>>(objectMapper)
            .let { (_, _, body) -> body.get() }

    private fun uploadAttachment(id: IncomeStatementId): AttachmentId {
        val (_, _, result) = http.upload("/attachments/income-statements/$id")
            .add(FileDataPart(File(pngFile.toURI()), name = "file"))
            .asUser(employee)
            .responseObject<AttachmentId>(objectMapper)

        return result.get()
    }
}
