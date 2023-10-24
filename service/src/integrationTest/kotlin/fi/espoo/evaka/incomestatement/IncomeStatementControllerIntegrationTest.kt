// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.incomestatement

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.attachment.AttachmentsController
import fi.espoo.evaka.daycare.domain.ProviderType
import fi.espoo.evaka.insertApplication
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.invoicing.controller.SortDirection
import fi.espoo.evaka.pis.service.insertGuardian
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.AttachmentId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.IncomeStatementId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.PlacementId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevGuardian
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.dev.insertTestParentship
import fi.espoo.evaka.shared.dev.insertTestPartnership
import fi.espoo.evaka.shared.dev.insertTestPlacement
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.shared.domain.RealEvakaClock
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testAdult_2
import fi.espoo.evaka.testAdult_3
import fi.espoo.evaka.testAdult_4
import fi.espoo.evaka.testAdult_5
import fi.espoo.evaka.testAdult_6
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testChild_2
import fi.espoo.evaka.testChild_3
import fi.espoo.evaka.testChild_4
import fi.espoo.evaka.testChild_5
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDaycare2
import fi.espoo.evaka.testPurchasedDaycare
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.mock.web.MockMultipartFile

class IncomeStatementControllerIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var incomeStatementController: IncomeStatementController
    @Autowired private lateinit var attachmentsController: AttachmentsController

    private val employeeId = EmployeeId(UUID.randomUUID())
    private val citizenId = testAdult_1.id
    private val employee = AuthenticatedUser.Employee(employeeId, setOf(UserRole.FINANCE_ADMIN))

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insertGeneralTestFixtures()
            tx.insert(DevEmployee(id = employeeId, roles = setOf(UserRole.FINANCE_ADMIN)))
        }
    }

    @Test
    fun `set as handled`() {
        val startDate = LocalDate.now()
        val endDate = LocalDate.now().plusDays(30)

        val id =
            db.transaction { tx ->
                tx.createIncomeStatement(
                    citizenId,
                    IncomeStatementBody.HighestFee(startDate, endDate)
                )
            }

        val incomeStatement1 = getIncomeStatement(id)
        assertEquals(
            IncomeStatement.HighestFee(
                id = id,
                personId = testAdult_1.id,
                firstName = testAdult_1.firstName,
                lastName = testAdult_1.lastName,
                startDate = startDate,
                endDate = endDate,
                created = incomeStatement1.created,
                updated = incomeStatement1.updated,
                handled = false,
                handlerNote = ""
            ),
            incomeStatement1
        )

        setIncomeStatementHandled(
            id,
            IncomeStatementController.SetIncomeStatementHandledBody(true, "is cool")
        )

        val incomeStatement2 = getIncomeStatement(id)
        assertEquals(
            IncomeStatement.HighestFee(
                id = id,
                personId = testAdult_1.id,
                firstName = testAdult_1.firstName,
                lastName = testAdult_1.lastName,
                startDate = startDate,
                endDate = endDate,
                created = incomeStatement1.created,
                updated = incomeStatement2.updated,
                handled = true,
                handlerNote = "is cool"
            ),
            incomeStatement2
        )

        setIncomeStatementHandled(
            id,
            IncomeStatementController.SetIncomeStatementHandledBody(false, "is not cool")
        )

        val incomeStatement3 = getIncomeStatement(id)
        assertEquals(
            IncomeStatement.HighestFee(
                id = id,
                personId = testAdult_1.id,
                firstName = testAdult_1.firstName,
                lastName = testAdult_1.lastName,
                startDate = startDate,
                endDate = endDate,
                created = incomeStatement1.created,
                updated = incomeStatement3.updated,
                handled = false,
                handlerNote = "is not cool"
            ),
            incomeStatement3
        )

        assertEquals(listOf(false), getIncomeStatements(citizenId).data.map { it.handled })
    }

    @Test
    fun `add an attachment`() {
        val id =
            db.transaction { tx ->
                tx.createIncomeStatement(
                    citizenId,
                    IncomeStatementBody.Income(
                        startDate = LocalDate.now(),
                        endDate = null,
                        gross =
                            Gross(
                                incomeSource = IncomeSource.ATTACHMENTS,
                                estimatedMonthlyIncome = 2000,
                                otherIncome = setOf(),
                                otherIncomeInfo = ""
                            ),
                        entrepreneur = null,
                        student = false,
                        alimonyPayer = false,
                        otherInfo = "",
                        attachmentIds = listOf()
                    )
                )
            }

        val attachmentId = uploadAttachment(id)

        val incomeStatement = getIncomeStatement(id)
        assertEquals(
            IncomeStatement.Income(
                id = id,
                personId = testAdult_1.id,
                firstName = testAdult_1.firstName,
                lastName = testAdult_1.lastName,
                startDate = LocalDate.now(),
                endDate = null,
                gross =
                    Gross(
                        incomeSource = IncomeSource.ATTACHMENTS,
                        estimatedMonthlyIncome = 2000,
                        otherIncome = setOf(),
                        otherIncomeInfo = ""
                    ),
                entrepreneur = null,
                student = false,
                alimonyPayer = false,
                otherInfo = "",
                attachments =
                    listOf(
                        Attachment(
                            id = attachmentId,
                            name = "evaka-logo.png",
                            contentType = "image/png",
                            uploadedByEmployee = true
                        )
                    ),
                created = incomeStatement.created,
                updated = incomeStatement.updated,
                handled = false,
                handlerNote = ""
            ),
            getIncomeStatement(id)
        )
    }

    private fun createTestIncomeStatement(
        personId: PersonId,
        startDate: LocalDate? = null
    ): IncomeStatement {
        val id =
            db.transaction { tx ->
                tx.createIncomeStatement(
                    personId,
                    IncomeStatementBody.HighestFee(
                        startDate = startDate ?: LocalDate.now(),
                        endDate = null
                    )
                )
            }
        return db.read { it.readIncomeStatementForPerson(personId, id, true)!! }
    }

    private fun createChildTestIncomeStatement(personId: PersonId): IncomeStatement {
        val id =
            db.transaction { tx ->
                tx.createIncomeStatement(
                    personId,
                    IncomeStatementBody.ChildIncome(
                        startDate = LocalDate.now(),
                        endDate = null,
                        attachmentIds = listOf(),
                        otherInfo = ""
                    )
                )
            }
        return db.read { it.readIncomeStatementForPerson(personId, id, true)!! }
    }

    @Test
    fun `list income statements awaiting handler - no income statements`() {
        assertEquals(
            PagedIncomeStatementsAwaitingHandler(listOf(), 0, 1),
            getIncomeStatementsAwaitingHandler()
        )
    }

    @Test
    fun `list income statements awaiting handler`() {
        val placementId1 = PlacementId(UUID.randomUUID())
        val placementId2 = PlacementId(UUID.randomUUID())
        val placementId3 = PlacementId(UUID.randomUUID())
        val placementId4 = PlacementId(UUID.randomUUID())
        val placementStart = LocalDate.now().minusDays(30)
        val placementEnd = LocalDate.now().plusDays(30)
        db.transaction { tx ->
            tx.insertTestParentship(
                citizenId,
                testChild_1.id,
                startDate = placementStart,
                endDate = placementEnd
            )
            tx.insertTestPlacement(
                id = placementId1,
                childId = testChild_1.id,
                unitId = testDaycare.id,
                startDate = placementStart,
                endDate = placementEnd,
                type = PlacementType.PRESCHOOL_DAYCARE
            )

            tx.insertTestParentship(
                testAdult_2.id,
                testChild_2.id,
                startDate = placementStart,
                endDate = placementEnd
            )
            tx.insertTestParentship(
                testAdult_2.id,
                testChild_3.id,
                startDate = placementStart,
                endDate = placementEnd
            )
            tx.insertTestPartnership(
                testAdult_2.id,
                testAdult_3.id,
                startDate = placementStart,
                endDate = placementEnd
            )
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

            tx.insertTestParentship(
                testAdult_4.id,
                testChild_4.id,
                startDate = placementStart,
                endDate = placementEnd
            )
            tx.insertApplication(testAdult_4, testChild_4)

            tx.insertGuardian(testAdult_5.id, testChild_5.id)
            tx.insertTestPlacement(
                id = placementId4,
                childId = testChild_5.id,
                unitId = testDaycare2.id,
                startDate = placementStart,
                endDate = placementEnd,
                type = PlacementType.PRESCHOOL_DAYCARE
            )
        }

        val incomeStatement1 = createTestIncomeStatement(citizenId)
        val incomeStatement2 = createTestIncomeStatement(testAdult_2.id)
        val incomeStatement3 = createTestIncomeStatement(testAdult_3.id)
        val incomeStatement4 = createTestIncomeStatement(testAdult_4.id)
        val incomeStatement5 = createTestIncomeStatement(testAdult_5.id)
        val incomeStatement6 = createTestIncomeStatement(testAdult_6.id)
        val incomeStatement7 = createChildTestIncomeStatement(testChild_1.id)

        assertEquals(
            PagedIncomeStatementsAwaitingHandler(
                listOf(
                    IncomeStatementAwaitingHandler(
                        id = incomeStatement1.id,
                        created = incomeStatement1.created,
                        startDate = incomeStatement1.startDate,
                        handlerNote = "",
                        type = IncomeStatementType.HIGHEST_FEE,
                        personId = citizenId,
                        personName = "Doe John",
                        primaryCareArea = "Test Area",
                    ),
                    IncomeStatementAwaitingHandler(
                        id = incomeStatement2.id,
                        created = incomeStatement2.created,
                        startDate = incomeStatement2.startDate,
                        handlerNote = "",
                        type = IncomeStatementType.HIGHEST_FEE,
                        personId = testAdult_2.id,
                        personName = "Doe Joan",
                        primaryCareArea = "Lwiz Foo",
                    ),
                    IncomeStatementAwaitingHandler(
                        id = incomeStatement3.id,
                        created = incomeStatement3.created,
                        startDate = incomeStatement3.startDate,
                        handlerNote = "",
                        type = IncomeStatementType.HIGHEST_FEE,
                        personId = testAdult_3.id,
                        personName = "Foo Mark",
                        primaryCareArea = "Lwiz Foo",
                    ),
                    IncomeStatementAwaitingHandler(
                        id = incomeStatement4.id,
                        created = incomeStatement4.created,
                        startDate = incomeStatement4.startDate,
                        handlerNote = "",
                        type = IncomeStatementType.HIGHEST_FEE,
                        personId = testAdult_4.id,
                        personName = "Aman Dork",
                        primaryCareArea = "Test Area",
                    ),
                    IncomeStatementAwaitingHandler(
                        id = incomeStatement5.id,
                        created = incomeStatement5.created,
                        startDate = incomeStatement5.startDate,
                        handlerNote = "",
                        type = IncomeStatementType.HIGHEST_FEE,
                        personId = testAdult_5.id,
                        personName = "Karhula Johannes Olavi Antero Tapio",
                        primaryCareArea = null,
                    ),
                    IncomeStatementAwaitingHandler(
                        id = incomeStatement6.id,
                        created = incomeStatement6.created,
                        startDate = incomeStatement6.startDate,
                        handlerNote = "",
                        type = IncomeStatementType.HIGHEST_FEE,
                        personId = testAdult_6.id,
                        personName = "Vilkas Ville",
                        primaryCareArea = null
                    ),
                    IncomeStatementAwaitingHandler(
                        id = incomeStatement7.id,
                        created = incomeStatement7.created,
                        startDate = incomeStatement7.startDate,
                        handlerNote = "",
                        type = IncomeStatementType.CHILD_INCOME,
                        personId = testChild_1.id,
                        personName = "Doe Ricky",
                        primaryCareArea = "Test Area"
                    )
                ),
                7,
                1
            ),
            getIncomeStatementsAwaitingHandler()
        )
    }

    @Test
    fun `list income statements awaiting handler - area filter`() {
        val placementId1 = PlacementId(UUID.randomUUID())
        val placementId2 = PlacementId(UUID.randomUUID())
        val placementStart = LocalDate.now().minusDays(30)
        val placementEnd = LocalDate.now().plusDays(30)
        db.transaction { tx ->
            tx.insertTestParentship(
                citizenId,
                testChild_1.id,
                startDate = placementStart,
                endDate = placementEnd
            )
            tx.insertTestPlacement(
                id = placementId1,
                childId = testChild_1.id,
                unitId = testDaycare.id,
                startDate = placementStart,
                endDate = placementEnd,
                type = PlacementType.PRESCHOOL_DAYCARE
            )

            tx.insertTestParentship(
                testAdult_2.id,
                testChild_2.id,
                startDate = placementStart,
                endDate = placementEnd
            )
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
            PagedIncomeStatementsAwaitingHandler(
                listOf(
                    IncomeStatementAwaitingHandler(
                        id = incomeStatement2.id,
                        created = incomeStatement2.created,
                        startDate = incomeStatement2.startDate,
                        handlerNote = "",
                        type = IncomeStatementType.HIGHEST_FEE,
                        personId = testAdult_2.id,
                        personName = "Doe Joan",
                        primaryCareArea = "Lwiz Foo"
                    )
                ),
                1,
                1
            ),
            // short name for the "Lwiz Foo" care area is "short name 2"
            getIncomeStatementsAwaitingHandler(
                SearchIncomeStatementsRequest(
                    areas = listOf("short name 2"),
                    providerTypes = listOf(ProviderType.MUNICIPAL)
                )
            )
        )
    }

    @Test
    fun `list income statements awaiting handler - provider type filter`() {
        val placementId1 = PlacementId(UUID.randomUUID())
        val placementId2 = PlacementId(UUID.randomUUID())
        val placementStart = LocalDate.now().minusDays(30)
        val placementEnd = LocalDate.now().plusDays(30)
        db.transaction { tx ->
            tx.insertTestParentship(
                citizenId,
                testChild_1.id,
                startDate = placementStart,
                endDate = placementEnd
            )
            tx.insertTestPlacement(
                id = placementId1,
                childId = testChild_1.id,
                unitId = testPurchasedDaycare.id,
                startDate = placementStart,
                endDate = placementEnd,
                type = PlacementType.PRESCHOOL_DAYCARE
            )

            tx.insertTestParentship(
                testAdult_2.id,
                testChild_2.id,
                startDate = placementStart,
                endDate = placementEnd
            )
            tx.insertTestPlacement(
                id = placementId2,
                childId = testChild_2.id,
                unitId = testDaycare.id,
                startDate = placementStart,
                endDate = placementEnd,
                type = PlacementType.PRESCHOOL_DAYCARE
            )
        }

        createTestIncomeStatement(citizenId)
        val incomeStatement2 = createTestIncomeStatement(testAdult_2.id)

        assertEquals(
            PagedIncomeStatementsAwaitingHandler(
                listOf(
                    IncomeStatementAwaitingHandler(
                        id = incomeStatement2.id,
                        created = incomeStatement2.created,
                        startDate = incomeStatement2.startDate,
                        handlerNote = "",
                        type = IncomeStatementType.HIGHEST_FEE,
                        personId = testAdult_2.id,
                        personName = "Doe Joan",
                        primaryCareArea = "Test Area"
                    )
                ),
                1,
                1
            ),
            getIncomeStatementsAwaitingHandler(
                SearchIncomeStatementsRequest(providerTypes = listOf(ProviderType.MUNICIPAL))
            )
        )
    }

    @Test
    fun `list income statements awaiting handler - sent date filter`() {
        val placementId1 = PlacementId(UUID.randomUUID())
        val placementId2 = PlacementId(UUID.randomUUID())
        val placementStart = LocalDate.now().minusDays(30)
        val placementEnd = LocalDate.now().plusDays(30)
        db.transaction { tx ->
            tx.insertTestParentship(
                citizenId,
                testChild_1.id,
                startDate = placementStart,
                endDate = placementEnd
            )
            tx.insertTestPlacement(
                id = placementId1,
                childId = testChild_1.id,
                unitId = testDaycare.id,
                startDate = placementStart,
                endDate = placementEnd,
                type = PlacementType.PRESCHOOL_DAYCARE
            )

            tx.insertTestParentship(
                testAdult_2.id,
                testChild_2.id,
                startDate = placementStart,
                endDate = placementEnd
            )
            tx.insertTestPlacement(
                id = placementId2,
                childId = testChild_2.id,
                unitId = testDaycare.id,
                startDate = placementStart,
                endDate = placementEnd,
                type = PlacementType.PRESCHOOL_DAYCARE
            )
        }

        val incomeStatement1 = createTestIncomeStatement(citizenId)
        val incomeStatement2 = createTestIncomeStatement(testAdult_2.id)

        val newCreated = HelsinkiDateTime.now().minusDays(2)

        db.transaction {
            it.createUpdate("UPDATE income_statement SET created = :newCreated WHERE id = :id")
                .bind("newCreated", newCreated)
                .bind("id", incomeStatement1.id)
                .execute()
        }

        assertEquals(
            PagedIncomeStatementsAwaitingHandler(
                listOf(
                    IncomeStatementAwaitingHandler(
                        id = incomeStatement1.id,
                        created = newCreated,
                        startDate = incomeStatement1.startDate,
                        handlerNote = "",
                        type = IncomeStatementType.HIGHEST_FEE,
                        personId = citizenId,
                        personName = "Doe John",
                        primaryCareArea = "Test Area"
                    )
                ),
                1,
                1
            ),
            getIncomeStatementsAwaitingHandler(
                SearchIncomeStatementsRequest(
                    sentStartDate = LocalDate.now().minusDays(3),
                    sentEndDate = LocalDate.now().minusDays(1)
                )
            )
        )

        assertEquals(
            PagedIncomeStatementsAwaitingHandler(
                listOf(
                    IncomeStatementAwaitingHandler(
                        id = incomeStatement2.id,
                        created = incomeStatement2.created,
                        startDate = incomeStatement2.startDate,
                        handlerNote = "",
                        type = IncomeStatementType.HIGHEST_FEE,
                        personId = testAdult_2.id,
                        personName = "Doe Joan",
                        primaryCareArea = "Test Area"
                    )
                ),
                1,
                1
            ),
            getIncomeStatementsAwaitingHandler(
                SearchIncomeStatementsRequest(sentStartDate = LocalDate.now())
            )
        )
    }

    @Test
    fun `list income statements awaiting handler - placement valid date filter`() {
        val placementId1 = PlacementId(UUID.randomUUID())
        val placementId2 = PlacementId(UUID.randomUUID())
        val placementId3 = PlacementId(UUID.randomUUID())
        val placement1Start = LocalDate.of(2022, 9, 19)
        val placement1End = LocalDate.of(2022, 11, 19)
        val placement2Start = LocalDate.of(2022, 10, 19)
        val placement2End = LocalDate.of(2023, 1, 17)
        db.transaction { tx ->
            tx.insertTestParentship(
                citizenId,
                testChild_1.id,
                startDate = placement1Start,
                endDate = placement1End
            )
            tx.insertTestPlacement(
                id = placementId1,
                childId = testChild_1.id,
                unitId = testDaycare.id,
                startDate = placement1Start,
                endDate = placement1End,
                type = PlacementType.PRESCHOOL_DAYCARE
            )

            tx.insertTestParentship(
                testAdult_2.id,
                testChild_2.id,
                startDate = placement2Start,
                endDate = placement2End
            )
            tx.insertTestPlacement(
                id = placementId2,
                childId = testChild_2.id,
                unitId = testDaycare.id,
                startDate = placement2Start,
                endDate = placement2End,
                type = PlacementType.PRESCHOOL_DAYCARE
            )

            tx.insert(DevGuardian(guardianId = testAdult_3.id, childId = testChild_3.id))
            tx.insertTestPlacement(
                id = placementId3,
                childId = testChild_3.id,
                unitId = testDaycare.id,
                startDate = placement2Start,
                endDate = placement2End,
                type = PlacementType.PRESCHOOL_DAYCARE
            )
        }

        val incomeStatement1 = createTestIncomeStatement(citizenId)
        val incomeStatement2 = createTestIncomeStatement(testAdult_2.id)
        val incomeStatement3 = createTestIncomeStatement(testAdult_3.id)

        val newCreated = HelsinkiDateTime.of(LocalDate.of(2022, 10, 17), LocalTime.of(11, 4))

        db.transaction {
            it.createUpdate("UPDATE income_statement SET created = :newCreated WHERE id = :id")
                .bind("newCreated", newCreated)
                .bind("id", incomeStatement1.id)
                .execute()
        }

        assertEquals(
            PagedIncomeStatementsAwaitingHandler(
                listOf(
                    IncomeStatementAwaitingHandler(
                        id = incomeStatement1.id,
                        created = newCreated,
                        startDate = incomeStatement1.startDate,
                        handlerNote = "",
                        type = IncomeStatementType.HIGHEST_FEE,
                        personId = citizenId,
                        personName = "Doe John",
                        primaryCareArea = "Test Area"
                    ),
                    IncomeStatementAwaitingHandler(
                        id = incomeStatement2.id,
                        created = incomeStatement2.created,
                        startDate = incomeStatement2.startDate,
                        handlerNote = "",
                        type = IncomeStatementType.HIGHEST_FEE,
                        personId = testAdult_2.id,
                        personName = "Doe Joan",
                        primaryCareArea = "Test Area"
                    ),
                    IncomeStatementAwaitingHandler(
                        id = incomeStatement3.id,
                        created = incomeStatement3.created,
                        startDate = incomeStatement3.startDate,
                        handlerNote = "",
                        type = IncomeStatementType.HIGHEST_FEE,
                        personId = testAdult_3.id,
                        personName = "Foo Mark",
                        primaryCareArea = null
                    )
                ),
                3,
                1
            ),
            getIncomeStatementsAwaitingHandler(
                SearchIncomeStatementsRequest(),
                MockEvakaClock(HelsinkiDateTime.of(LocalDate.of(2022, 10, 19), LocalTime.MAX))
            )
        )
        assertEquals(
            PagedIncomeStatementsAwaitingHandler(
                listOf(
                    IncomeStatementAwaitingHandler(
                        id = incomeStatement2.id,
                        created = incomeStatement2.created,
                        startDate = incomeStatement2.startDate,
                        handlerNote = "",
                        type = IncomeStatementType.HIGHEST_FEE,
                        personId = testAdult_2.id,
                        personName = "Doe Joan",
                        primaryCareArea = "Test Area"
                    )
                ),
                1,
                1
            ),
            getIncomeStatementsAwaitingHandler(
                SearchIncomeStatementsRequest(placementValidDate = placement2End),
                MockEvakaClock(HelsinkiDateTime.of(LocalDate.of(2022, 10, 19), LocalTime.MAX))
            )
        )

        assertEquals(
            PagedIncomeStatementsAwaitingHandler(
                listOf(
                    IncomeStatementAwaitingHandler(
                        id = incomeStatement1.id,
                        created = newCreated,
                        startDate = incomeStatement1.startDate,
                        handlerNote = "",
                        type = IncomeStatementType.HIGHEST_FEE,
                        personId = citizenId,
                        personName = "Doe John",
                        primaryCareArea = "Test Area"
                    ),
                    IncomeStatementAwaitingHandler(
                        id = incomeStatement2.id,
                        created = incomeStatement2.created,
                        startDate = incomeStatement2.startDate,
                        handlerNote = "",
                        type = IncomeStatementType.HIGHEST_FEE,
                        personId = testAdult_2.id,
                        personName = "Doe Joan",
                        primaryCareArea = "Test Area"
                    )
                ),
                2,
                1
            ),
            getIncomeStatementsAwaitingHandler(
                SearchIncomeStatementsRequest(placementValidDate = placement2Start),
                MockEvakaClock(HelsinkiDateTime.of(LocalDate.of(2022, 10, 19), LocalTime.MAX))
            )
        )
    }

    @Test
    fun `list income statements awaiting handler - sent date sort`() {
        val placementId1 = PlacementId(UUID.randomUUID())
        val placementId2 = PlacementId(UUID.randomUUID())
        val placementStart = LocalDate.now().minusDays(30)
        val placementEnd = LocalDate.now().plusDays(30)
        db.transaction { tx ->
            tx.insertTestParentship(
                citizenId,
                testChild_1.id,
                startDate = placementStart,
                endDate = placementEnd
            )
            tx.insertTestPlacement(
                id = placementId1,
                childId = testChild_1.id,
                unitId = testDaycare.id,
                startDate = placementStart,
                endDate = placementEnd,
                type = PlacementType.PRESCHOOL_DAYCARE
            )

            tx.insertTestParentship(
                testAdult_2.id,
                testChild_2.id,
                startDate = placementStart,
                endDate = placementEnd
            )
            tx.insertTestPlacement(
                id = placementId2,
                childId = testChild_2.id,
                unitId = testDaycare.id,
                startDate = placementStart,
                endDate = placementEnd,
                type = PlacementType.PRESCHOOL_DAYCARE
            )
        }

        val incomeStatement1 =
            createTestIncomeStatement(citizenId, startDate = LocalDate.of(2022, 10, 12))
        val incomeStatement2 =
            createTestIncomeStatement(testAdult_2.id, startDate = LocalDate.of(2022, 10, 13))

        val newCreated = HelsinkiDateTime.now().minusDays(2)

        db.transaction {
            it.createUpdate("UPDATE income_statement SET created = :newCreated")
                .bind("newCreated", newCreated)
                .execute()
        }

        val expected1 =
            IncomeStatementAwaitingHandler(
                id = incomeStatement1.id,
                created = newCreated,
                startDate = incomeStatement1.startDate,
                handlerNote = "",
                type = IncomeStatementType.HIGHEST_FEE,
                personId = citizenId,
                personName = "Doe John",
                primaryCareArea = "Test Area"
            )
        val expected2 =
            IncomeStatementAwaitingHandler(
                id = incomeStatement2.id,
                created = newCreated,
                startDate = incomeStatement2.startDate,
                handlerNote = "",
                type = IncomeStatementType.HIGHEST_FEE,
                personId = testAdult_2.id,
                personName = "Doe Joan",
                primaryCareArea = "Test Area"
            )
        assertEquals(
            PagedIncomeStatementsAwaitingHandler(listOf(expected1, expected2), 2, 1),
            getIncomeStatementsAwaitingHandler(
                SearchIncomeStatementsRequest(
                    sortBy = IncomeStatementSortParam.START_DATE,
                    sortDirection = SortDirection.ASC
                )
            )
        )
        assertEquals(
            PagedIncomeStatementsAwaitingHandler(listOf(expected2, expected1), 2, 1),
            getIncomeStatementsAwaitingHandler(
                SearchIncomeStatementsRequest(
                    sortBy = IncomeStatementSortParam.START_DATE,
                    sortDirection = SortDirection.DESC
                )
            )
        )
    }

    private fun getIncomeStatement(id: IncomeStatementId): IncomeStatement {
        return incomeStatementController.getIncomeStatement(
            dbInstance(),
            employee,
            RealEvakaClock(),
            citizenId,
            id
        )
    }

    private fun getIncomeStatements(personId: PersonId): PagedIncomeStatements {
        return incomeStatementController.getIncomeStatements(
            dbInstance(),
            employee,
            RealEvakaClock(),
            personId,
            page = 1,
            pageSize = 10
        )
    }

    private fun setIncomeStatementHandled(
        id: IncomeStatementId,
        body: IncomeStatementController.SetIncomeStatementHandledBody
    ) {
        incomeStatementController.setHandled(dbInstance(), employee, RealEvakaClock(), id, body)
    }

    private fun getIncomeStatementsAwaitingHandler(
        body: SearchIncomeStatementsRequest =
            SearchIncomeStatementsRequest(1, 50, null, null, emptyList(), emptyList(), null, null),
        clock: EvakaClock = RealEvakaClock()
    ): PagedIncomeStatementsAwaitingHandler {
        return incomeStatementController.getIncomeStatementsAwaitingHandler(
            dbInstance(),
            employee,
            clock,
            body
        )
    }

    private fun uploadAttachment(id: IncomeStatementId): AttachmentId {
        return attachmentsController.uploadIncomeStatementAttachmentEmployee(
            dbInstance(),
            employee,
            RealEvakaClock(),
            id,
            MockMultipartFile("file", "evaka-logo.png", "image/png", pngFile.readBytes())
        )
    }
}
