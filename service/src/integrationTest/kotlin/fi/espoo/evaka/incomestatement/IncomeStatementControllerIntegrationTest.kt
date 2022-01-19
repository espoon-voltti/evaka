// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.incomestatement

import com.github.kittinunf.fuel.core.FileDataPart
import com.github.kittinunf.fuel.jackson.objectBody
import com.github.kittinunf.fuel.jackson.responseObject
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.AttachmentId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.IncomeStatementId
import fi.espoo.evaka.shared.Paged
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.PlacementId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.asUser
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.insertTestEmployee
import fi.espoo.evaka.shared.dev.insertTestParentship
import fi.espoo.evaka.shared.dev.insertTestPartnership
import fi.espoo.evaka.shared.dev.insertTestPlacement
import fi.espoo.evaka.shared.dev.resetDatabase
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testAdult_2
import fi.espoo.evaka.testAdult_3
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testChild_2
import fi.espoo.evaka.testChild_3
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDaycare2
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals

class IncomeStatementControllerIntegrationTest : FullApplicationTest() {
    private val employeeId = EmployeeId(UUID.randomUUID())
    private val citizenId = testAdult_1.id
    private val employee = AuthenticatedUser.Employee(employeeId.raw, setOf(UserRole.FINANCE_ADMIN))

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
                citizenId, IncomeStatementBody.HighestFee(startDate, endDate)
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
                handlerNote = "",
            ),
            incomeStatement1
        )

        setIncomeStatementHandled(id, IncomeStatementController.SetIncomeStatementHandledBody(true, "is cool"))

        val incomeStatement2 = getIncomeStatement(id)
        assertEquals(
            IncomeStatement.HighestFee(
                id = id,
                startDate = startDate,
                endDate = endDate,
                created = incomeStatement1.created,
                updated = incomeStatement2.updated,
                handled = true,
                handlerNote = "is cool"
            ),
            incomeStatement2
        )

        setIncomeStatementHandled(id, IncomeStatementController.SetIncomeStatementHandledBody(false, "is not cool"))

        val incomeStatement3 = getIncomeStatement(id)
        assertEquals(
            IncomeStatement.HighestFee(
                id = id,
                startDate = startDate,
                endDate = endDate,
                created = incomeStatement1.created,
                updated = incomeStatement3.updated,
                handled = false,
                handlerNote = "is not cool",
            ),
            incomeStatement3
        )

        assertEquals(listOf(false), getIncomeStatements(citizenId).data.map { it.handled })
    }

    @Test
    fun `add an attachment`() {
        val id = db.transaction { tx ->
            tx.createIncomeStatement(
                citizenId,
                IncomeStatementBody.Income(
                    startDate = LocalDate.now(),
                    endDate = null,
                    gross = Gross(
                        incomeSource = IncomeSource.ATTACHMENTS,
                        estimatedMonthlyIncome = 2000,
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
                    estimatedMonthlyIncome = 2000,
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
                handlerNote = "",
            ),
            getIncomeStatement(id)
        )
    }

    private fun createTestIncomeStatement(personId: PersonId): IncomeStatement {
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
        val incomeStatement = createTestIncomeStatement(citizenId)
        assertEquals(
            Paged(
                listOf(
                    IncomeStatementAwaitingHandler(
                        id = incomeStatement.id,
                        created = incomeStatement.created,
                        startDate = incomeStatement.startDate,
                        type = IncomeStatementType.HIGHEST_FEE,
                        personId = citizenId,
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
        val placementId3 = PlacementId(UUID.randomUUID())
        val placementStart = LocalDate.now().minusDays(30)
        val placementEnd = LocalDate.now().plusDays(30)
        db.transaction { tx ->
            tx.insertTestParentship(citizenId, testChild_1.id, startDate = placementStart, endDate = placementEnd)
            tx.insertTestPlacement(
                id = placementId1,
                childId = testChild_1.id,
                unitId = testDaycare.id,
                startDate = placementStart,
                endDate = placementEnd,
                type = PlacementType.PRESCHOOL_DAYCARE
            )

            tx.insertTestParentship(testAdult_2.id, testChild_2.id, startDate = placementStart, endDate = placementEnd)
            tx.insertTestParentship(testAdult_2.id, testChild_3.id, startDate = placementStart, endDate = placementEnd)
            tx.insertTestPartnership(testAdult_2.id, testAdult_3.id, startDate = placementStart, endDate = placementEnd)
            tx.insertTestPlacement(
                id = placementId2,
                childId = testChild_2.id,
                unitId = testDaycare2.id,
                startDate = placementStart,
                endDate = placementEnd,
                type = PlacementType.PRESCHOOL_DAYCARE
            )
            tx.insertTestPlacement(
                id = placementId3,
                childId = testChild_3.id,
                unitId = testDaycare2.id,
                startDate = placementStart,
                endDate = placementEnd,
                type = PlacementType.PRESCHOOL_DAYCARE
            )
        }

        val incomeStatement1 = createTestIncomeStatement(citizenId)
        val incomeStatement2 = createTestIncomeStatement(testAdult_2.id)
        val incomeStatement3 = createTestIncomeStatement(testAdult_3.id)

        assertEquals(
            Paged(
                listOf(
                    IncomeStatementAwaitingHandler(
                        id = incomeStatement1.id,
                        created = incomeStatement1.created,
                        startDate = incomeStatement1.startDate,
                        type = IncomeStatementType.HIGHEST_FEE,
                        personId = citizenId,
                        personName = "John Doe",
                        primaryCareArea = "Test Area",
                    ),
                    IncomeStatementAwaitingHandler(
                        id = incomeStatement2.id,
                        created = incomeStatement2.created,
                        startDate = incomeStatement2.startDate,
                        type = IncomeStatementType.HIGHEST_FEE,
                        personId = testAdult_2.id,
                        personName = "Joan Doe",
                        primaryCareArea = "Lwiz Foo",
                    ),
                    IncomeStatementAwaitingHandler(
                        id = incomeStatement3.id,
                        created = incomeStatement3.created,
                        startDate = incomeStatement3.startDate,
                        type = IncomeStatementType.HIGHEST_FEE,
                        personId = testAdult_3.id,
                        personName = "Mark Foo",
                        primaryCareArea = "Lwiz Foo",
                    )
                ),
                3, 1
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
            tx.insertTestParentship(citizenId, testChild_1.id, startDate = placementStart, endDate = placementEnd)
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

        createTestIncomeStatement(citizenId)
        val incomeStatement2 = createTestIncomeStatement(testAdult_2.id)

        assertEquals(
            Paged(
                listOf(
                    IncomeStatementAwaitingHandler(
                        id = incomeStatement2.id,
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
        http.get("/income-statements/person/$citizenId/$id")
            .asUser(employee)
            .responseObject<IncomeStatement>(jsonMapper)
            .let { (_, _, body) -> body.get() }

    private fun getIncomeStatements(personId: PersonId): Paged<IncomeStatement> =
        http.get("/income-statements/person/$personId?page=1&pageSize=10")
            .asUser(employee)
            .responseObject<Paged<IncomeStatement>>(jsonMapper)
            .let { (_, _, body) -> body.get() }

    private fun setIncomeStatementHandled(
        id: IncomeStatementId,
        body: IncomeStatementController.SetIncomeStatementHandledBody
    ) {
        http.post("/income-statements/$id/handled")
            .asUser(employee)
            .objectBody(body, mapper = jsonMapper)
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
            .responseObject<Paged<IncomeStatementAwaitingHandler>>(jsonMapper)
            .let { (_, _, body) -> body.get() }

    private fun uploadAttachment(id: IncomeStatementId): AttachmentId {
        val (_, _, result) = http.upload("/attachments/income-statements/$id")
            .add(FileDataPart(File(pngFile.toURI()), name = "file"))
            .asUser(employee)
            .responseObject<AttachmentId>(jsonMapper)

        return result.get()
    }
}
