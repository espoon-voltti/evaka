// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.incomestatement

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.attachment.AttachmentsController
import fi.espoo.evaka.daycare.domain.ProviderType
import fi.espoo.evaka.insertApplication
import fi.espoo.evaka.invoicing.controller.SortDirection
import fi.espoo.evaka.invoicing.domain.IncomeEffect
import fi.espoo.evaka.pis.service.insertGuardian
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.AttachmentId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.IncomeStatementId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.PlacementId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevGuardian
import fi.espoo.evaka.shared.dev.DevIncome
import fi.espoo.evaka.shared.dev.DevParentship
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.dev.insertTestPartnership
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
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

    private val area1 = DevCareArea(name = "Area 1", shortName = "area1")
    private val daycare1 = DevDaycare(areaId = area1.id)
    private val daycarePurchased =
        DevDaycare(areaId = area1.id, providerType = ProviderType.PURCHASED)
    private val area2 = DevCareArea(name = "Area 2", shortName = "area2")
    private val daycare2 = DevDaycare(areaId = area2.id)

    private val employeeId = EmployeeId(UUID.randomUUID())
    private val citizenId = testAdult_1.id
    private val employee = AuthenticatedUser.Employee(employeeId, setOf(UserRole.FINANCE_ADMIN))

    private val today = LocalDate.of(2024, 8, 30)
    private val now = HelsinkiDateTime.of(today, LocalTime.of(12, 0))

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insert(area1)
            tx.insert(area2)
            tx.insert(daycare1)
            tx.insert(daycare2)
            tx.insert(daycarePurchased)
            listOf(testAdult_1, testAdult_2, testAdult_3, testAdult_4, testAdult_5, testAdult_6)
                .forEach { tx.insert(it, DevPersonType.ADULT) }
            listOf(testChild_1, testChild_2, testChild_3, testChild_4, testChild_5).forEach {
                tx.insert(it, DevPersonType.CHILD)
            }
            tx.insert(DevEmployee(id = employeeId, roles = setOf(UserRole.FINANCE_ADMIN)))
        }
    }

    @Test
    fun `set as handled`() {
        val startDate = today
        val endDate = today.plusDays(30)

        val id =
            db.transaction { tx ->
                tx.createIncomeStatement(
                    citizenId,
                    IncomeStatementBody.HighestFee(startDate, endDate),
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
                handlerNote = "",
            ),
            incomeStatement1,
        )

        setIncomeStatementHandled(
            id,
            IncomeStatementController.SetIncomeStatementHandledBody(true, "is cool"),
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
                handlerNote = "is cool",
            ),
            incomeStatement2,
        )

        setIncomeStatementHandled(
            id,
            IncomeStatementController.SetIncomeStatementHandledBody(false, "is not cool"),
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
                handlerNote = "is not cool",
            ),
            incomeStatement3,
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
                        startDate = today,
                        endDate = null,
                        gross =
                            Gross(
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
                    ),
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
                startDate = today,
                endDate = null,
                gross =
                    Gross(
                        incomeSource = IncomeSource.ATTACHMENTS,
                        estimatedMonthlyIncome = 2000,
                        otherIncome = setOf(),
                        otherIncomeInfo = "",
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
                            uploadedByEmployee = true,
                        )
                    ),
                created = incomeStatement.created,
                updated = incomeStatement.updated,
                handled = false,
                handlerNote = "",
            ),
            getIncomeStatement(id),
        )
    }

    private fun createTestIncomeStatement(
        personId: PersonId,
        startDate: LocalDate? = null,
    ): IncomeStatement {
        val id =
            db.transaction { tx ->
                tx.createIncomeStatement(
                    personId,
                    IncomeStatementBody.HighestFee(startDate = startDate ?: today, endDate = null),
                )
            }
        return db.read { it.readIncomeStatementForPerson(personId, id, true)!! }
    }

    private fun createChildTestIncomeStatement(
        personId: PersonId,
        startDate: LocalDate? = null,
    ): IncomeStatement {
        val id =
            db.transaction { tx ->
                tx.createIncomeStatement(
                    personId,
                    IncomeStatementBody.ChildIncome(
                        startDate = startDate ?: today,
                        endDate = null,
                        attachmentIds = listOf(),
                        otherInfo = "",
                    ),
                )
            }
        return db.read { it.readIncomeStatementForPerson(personId, id, true)!! }
    }

    @Test
    fun `list income statements awaiting handler - no income statements`() {
        assertEquals(
            PagedIncomeStatementsAwaitingHandler(listOf(), 0, 1),
            getIncomeStatementsAwaitingHandler(),
        )
    }

    @Test
    fun `list income statements awaiting handler`() {
        val incomeDate1 = LocalDate.now().minusDays(2)
        val incomeDate2 = LocalDate.now().plusMonths(1)
        val placementId1 = PlacementId(UUID.randomUUID())
        val placementId2 = PlacementId(UUID.randomUUID())
        val placementId3 = PlacementId(UUID.randomUUID())
        val placementId4 = PlacementId(UUID.randomUUID())
        val placementStart = today.minusDays(30)
        val placementEnd = today.plusDays(30)
        val createdAt = HelsinkiDateTime.of(placementStart, LocalTime.of(12, 0, 0))

        db.transaction { tx ->
            tx.insert(
                DevParentship(
                    childId = testChild_1.id,
                    headOfChildId = citizenId,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )
            tx.insert(
                DevPlacement(
                    id = placementId1,
                    type = PlacementType.PRESCHOOL_DAYCARE,
                    childId = testChild_1.id,
                    unitId = daycare1.id,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )

            // `validTo` of the newest income statement is returned as `incomeEndDate`
            tx.insert(
                DevIncome(
                    personId = citizenId,
                    updatedBy = AuthenticatedUser.SystemInternalUser.evakaUserId,
                    validFrom = incomeDate1.minusYears(1),
                    validTo = incomeDate1.minusMonths(1).minusDays(1),
                )
            )
            tx.insert(
                DevIncome(
                    personId = citizenId,
                    updatedBy = AuthenticatedUser.SystemInternalUser.evakaUserId,
                    validFrom = incomeDate1.minusMonths(1),
                    validTo = incomeDate1,
                )
            )

            tx.insert(
                DevParentship(
                    childId = testChild_2.id,
                    headOfChildId = testAdult_2.id,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )
            tx.insert(
                DevParentship(
                    childId = testChild_3.id,
                    headOfChildId = testAdult_2.id,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )
            tx.insertTestPartnership(
                testAdult_2.id,
                testAdult_3.id,
                startDate = placementStart,
                endDate = placementEnd,
                createdAt = createdAt,
            )
            tx.insert(
                DevPlacement(
                    id = placementId2,
                    type = PlacementType.PRESCHOOL_DAYCARE,
                    childId = testChild_2.id,
                    unitId = daycare2.id,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )
            tx.insert(
                DevPlacement(
                    id = placementId3,
                    type = PlacementType.PRESCHOOL_DAYCARE,
                    childId = testChild_3.id,
                    unitId = daycare2.id,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )

            // Latest income has no end date -> incomeEndDate is null
            tx.insert(
                DevIncome(
                    personId = testAdult_2.id,
                    updatedBy = AuthenticatedUser.SystemInternalUser.evakaUserId,
                    validFrom = incomeDate2.minusMonths(1),
                    validTo = incomeDate2.minusDays(1),
                )
            )
            tx.insert(
                DevIncome(
                    personId = testAdult_2.id,
                    updatedBy = AuthenticatedUser.SystemInternalUser.evakaUserId,
                    effect = IncomeEffect.MAX_FEE_ACCEPTED,
                    validFrom = incomeDate2,
                    validTo = null,
                )
            )

            tx.insert(
                DevParentship(
                    childId = testChild_4.id,
                    headOfChildId = testAdult_4.id,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )
            tx.insertApplication(testAdult_4, testChild_4, preferredUnit = daycare1)

            tx.insertGuardian(testAdult_5.id, testChild_5.id)
            tx.insert(
                DevPlacement(
                    id = placementId4,
                    type = PlacementType.PRESCHOOL_DAYCARE,
                    childId = testChild_5.id,
                    unitId = daycare2.id,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
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
                        incomeEndDate = incomeDate1,
                        handlerNote = "",
                        type = IncomeStatementType.HIGHEST_FEE,
                        personId = citizenId,
                        personLastName = "Doe",
                        personFirstName = "John",
                        primaryCareArea = area1.name,
                    ),
                    IncomeStatementAwaitingHandler(
                        id = incomeStatement2.id,
                        created = incomeStatement2.created,
                        startDate = incomeStatement2.startDate,
                        incomeEndDate = null,
                        handlerNote = "",
                        type = IncomeStatementType.HIGHEST_FEE,
                        personId = testAdult_2.id,
                        personLastName = "Doe",
                        personFirstName = "Joan",
                        primaryCareArea = area2.name,
                    ),
                    IncomeStatementAwaitingHandler(
                        id = incomeStatement3.id,
                        created = incomeStatement3.created,
                        startDate = incomeStatement3.startDate,
                        incomeEndDate = null,
                        handlerNote = "",
                        type = IncomeStatementType.HIGHEST_FEE,
                        personId = testAdult_3.id,
                        personLastName = "Foo",
                        personFirstName = "Mark",
                        primaryCareArea = area2.name,
                    ),
                    IncomeStatementAwaitingHandler(
                        id = incomeStatement4.id,
                        created = incomeStatement4.created,
                        startDate = incomeStatement4.startDate,
                        incomeEndDate = null,
                        handlerNote = "",
                        type = IncomeStatementType.HIGHEST_FEE,
                        personId = testAdult_4.id,
                        personLastName = "Aman",
                        personFirstName = "Dork",
                        primaryCareArea = area1.name,
                    ),
                    IncomeStatementAwaitingHandler(
                        id = incomeStatement5.id,
                        created = incomeStatement5.created,
                        startDate = incomeStatement5.startDate,
                        incomeEndDate = null,
                        handlerNote = "",
                        type = IncomeStatementType.HIGHEST_FEE,
                        personId = testAdult_5.id,
                        personLastName = "Karhula",
                        personFirstName = "Johannes Olavi Antero Tapio",
                        primaryCareArea = null,
                    ),
                    IncomeStatementAwaitingHandler(
                        id = incomeStatement6.id,
                        created = incomeStatement6.created,
                        startDate = incomeStatement6.startDate,
                        incomeEndDate = null,
                        handlerNote = "",
                        type = IncomeStatementType.HIGHEST_FEE,
                        personId = testAdult_6.id,
                        personLastName = "Vilkas",
                        personFirstName = "Ville",
                        primaryCareArea = null,
                    ),
                    IncomeStatementAwaitingHandler(
                        id = incomeStatement7.id,
                        created = incomeStatement7.created,
                        startDate = incomeStatement7.startDate,
                        incomeEndDate = null,
                        handlerNote = "",
                        type = IncomeStatementType.CHILD_INCOME,
                        personId = testChild_1.id,
                        personLastName = "Doe",
                        personFirstName = "Ricky",
                        primaryCareArea = area1.name,
                    ),
                ),
                7,
                1,
            ),
            getIncomeStatementsAwaitingHandler(),
        )
    }

    @Test
    fun `list income statements awaiting handler - area filter`() {
        val placementId1 = PlacementId(UUID.randomUUID())
        val placementId2 = PlacementId(UUID.randomUUID())
        val placementStart = today.minusDays(30)
        val placementEnd = today.plusDays(30)
        db.transaction { tx ->
            tx.insert(
                DevParentship(
                    childId = testChild_1.id,
                    headOfChildId = citizenId,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )
            tx.insert(
                DevPlacement(
                    id = placementId1,
                    type = PlacementType.PRESCHOOL_DAYCARE,
                    childId = testChild_1.id,
                    unitId = daycare1.id,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )

            tx.insert(
                DevParentship(
                    childId = testChild_2.id,
                    headOfChildId = testAdult_2.id,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )
            tx.insert(
                DevPlacement(
                    id = placementId2,
                    type = PlacementType.PRESCHOOL_DAYCARE,
                    childId = testChild_2.id,
                    unitId = daycare2.id,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
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
                        incomeEndDate = null,
                        handlerNote = "",
                        type = IncomeStatementType.HIGHEST_FEE,
                        personId = testAdult_2.id,
                        personLastName = "Doe",
                        personFirstName = "Joan",
                        primaryCareArea = area2.name,
                    )
                ),
                1,
                1,
            ),
            getIncomeStatementsAwaitingHandler(
                SearchIncomeStatementsRequest(
                    areas = listOf(area2.shortName),
                    providerTypes = listOf(ProviderType.MUNICIPAL),
                )
            ),
        )
    }

    @Test
    fun `list income statements awaiting handler - unit filter`() {
        val placementId1 = PlacementId(UUID.randomUUID())
        val placementId2 = PlacementId(UUID.randomUUID())
        val placementStart = today.minusDays(30)
        val placementEnd = today.plusDays(30)
        db.transaction { tx ->
            tx.insert(
                DevParentship(
                    childId = testChild_1.id,
                    headOfChildId = citizenId,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )
            tx.insert(
                DevPlacement(
                    id = placementId1,
                    type = PlacementType.PRESCHOOL_DAYCARE,
                    childId = testChild_1.id,
                    unitId = daycare1.id,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )

            tx.insert(
                DevParentship(
                    childId = testChild_2.id,
                    headOfChildId = testAdult_2.id,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )
            tx.insert(
                DevPlacement(
                    id = placementId2,
                    type = PlacementType.PRESCHOOL_DAYCARE,
                    childId = testChild_2.id,
                    unitId = daycare2.id,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
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
                        incomeEndDate = null,
                        handlerNote = "",
                        type = IncomeStatementType.HIGHEST_FEE,
                        personId = testAdult_2.id,
                        personLastName = "Doe",
                        personFirstName = "Joan",
                        primaryCareArea = area2.name,
                    )
                ),
                1,
                1,
            ),
            getIncomeStatementsAwaitingHandler(SearchIncomeStatementsRequest(unit = daycare2.id)),
        )
    }

    @Test
    fun `list income statements awaiting handler - provider type filter`() {
        val placementId1 = PlacementId(UUID.randomUUID())
        val placementId2 = PlacementId(UUID.randomUUID())
        val placementStart = today.minusDays(30)
        val placementEnd = today.plusDays(30)
        db.transaction { tx ->
            tx.insert(
                DevParentship(
                    childId = testChild_1.id,
                    headOfChildId = citizenId,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )
            tx.insert(
                DevPlacement(
                    id = placementId1,
                    type = PlacementType.PRESCHOOL_DAYCARE,
                    childId = testChild_1.id,
                    unitId = daycarePurchased.id,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )

            tx.insert(
                DevParentship(
                    childId = testChild_2.id,
                    headOfChildId = testAdult_2.id,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )
            tx.insert(
                DevPlacement(
                    id = placementId2,
                    type = PlacementType.PRESCHOOL_DAYCARE,
                    childId = testChild_2.id,
                    unitId = daycare1.id,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
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
                        incomeEndDate = null,
                        handlerNote = "",
                        type = IncomeStatementType.HIGHEST_FEE,
                        personId = testAdult_2.id,
                        personLastName = "Doe",
                        personFirstName = "Joan",
                        primaryCareArea = area1.name,
                    )
                ),
                1,
                1,
            ),
            getIncomeStatementsAwaitingHandler(
                SearchIncomeStatementsRequest(providerTypes = listOf(ProviderType.MUNICIPAL))
            ),
        )
    }

    @Test
    fun `list income statements awaiting handler - sent date filter`() {
        val placementId1 = PlacementId(UUID.randomUUID())
        val placementId2 = PlacementId(UUID.randomUUID())
        val placementStart = today.minusDays(30)
        val placementEnd = today.plusDays(30)
        db.transaction { tx ->
            tx.insert(
                DevParentship(
                    childId = testChild_1.id,
                    headOfChildId = citizenId,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )
            tx.insert(
                DevPlacement(
                    id = placementId1,
                    type = PlacementType.PRESCHOOL_DAYCARE,
                    childId = testChild_1.id,
                    unitId = daycare1.id,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )

            tx.insert(
                DevParentship(
                    childId = testChild_2.id,
                    headOfChildId = testAdult_2.id,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )
            tx.insert(
                DevPlacement(
                    id = placementId2,
                    type = PlacementType.PRESCHOOL_DAYCARE,
                    childId = testChild_2.id,
                    unitId = daycare1.id,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )
        }

        val incomeStatement1 = createTestIncomeStatement(citizenId)
        val incomeStatement2 = createTestIncomeStatement(testAdult_2.id)

        val newCreated = HelsinkiDateTime.of(today.minusDays(2), LocalTime.of(12, 0))

        db.transaction {
            it.execute {
                sql(
                    "UPDATE income_statement SET created = ${bind(newCreated)} WHERE id = ${bind(incomeStatement1.id)}"
                )
            }
        }

        assertEquals(
            PagedIncomeStatementsAwaitingHandler(
                listOf(
                    IncomeStatementAwaitingHandler(
                        id = incomeStatement1.id,
                        created = newCreated,
                        startDate = incomeStatement1.startDate,
                        incomeEndDate = null,
                        handlerNote = "",
                        type = IncomeStatementType.HIGHEST_FEE,
                        personId = citizenId,
                        personLastName = "Doe",
                        personFirstName = "John",
                        primaryCareArea = area1.name,
                    )
                ),
                1,
                1,
            ),
            getIncomeStatementsAwaitingHandler(
                SearchIncomeStatementsRequest(
                    sentStartDate = today.minusDays(3),
                    sentEndDate = today.minusDays(1),
                )
            ),
        )

        assertEquals(
            PagedIncomeStatementsAwaitingHandler(
                listOf(
                    IncomeStatementAwaitingHandler(
                        id = incomeStatement2.id,
                        created = incomeStatement2.created,
                        startDate = incomeStatement2.startDate,
                        incomeEndDate = null,
                        handlerNote = "",
                        type = IncomeStatementType.HIGHEST_FEE,
                        personId = testAdult_2.id,
                        personLastName = "Doe",
                        personFirstName = "Joan",
                        primaryCareArea = area1.name,
                    )
                ),
                1,
                1,
            ),
            getIncomeStatementsAwaitingHandler(SearchIncomeStatementsRequest(sentStartDate = today)),
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
            tx.insert(
                DevParentship(
                    childId = testChild_1.id,
                    headOfChildId = citizenId,
                    startDate = placement1Start,
                    endDate = placement1End,
                )
            )
            tx.insert(
                DevPlacement(
                    id = placementId1,
                    type = PlacementType.PRESCHOOL_DAYCARE,
                    childId = testChild_1.id,
                    unitId = daycare1.id,
                    startDate = placement1Start,
                    endDate = placement1End,
                )
            )

            tx.insert(
                DevParentship(
                    childId = testChild_2.id,
                    headOfChildId = testAdult_2.id,
                    startDate = placement2Start,
                    endDate = placement2End,
                )
            )
            tx.insert(
                DevPlacement(
                    id = placementId2,
                    type = PlacementType.PRESCHOOL_DAYCARE,
                    childId = testChild_2.id,
                    unitId = daycare1.id,
                    startDate = placement2Start,
                    endDate = placement2End,
                )
            )

            tx.insert(DevGuardian(guardianId = testAdult_3.id, childId = testChild_3.id))
            tx.insert(
                DevPlacement(
                    id = placementId3,
                    type = PlacementType.PRESCHOOL_DAYCARE,
                    childId = testChild_3.id,
                    unitId = daycare1.id,
                    startDate = placement2Start,
                    endDate = placement2End,
                )
            )
        }

        val incomeStatement1 = createTestIncomeStatement(citizenId)
        val incomeStatement2 = createTestIncomeStatement(testAdult_2.id)
        val incomeStatement3 = createTestIncomeStatement(testAdult_3.id)

        val newCreated = HelsinkiDateTime.of(LocalDate.of(2022, 10, 17), LocalTime.of(11, 4))

        db.transaction {
            @Suppress("DEPRECATION")
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
                        incomeEndDate = null,
                        handlerNote = "",
                        type = IncomeStatementType.HIGHEST_FEE,
                        personId = citizenId,
                        personLastName = "Doe",
                        personFirstName = "John",
                        primaryCareArea = area1.name,
                    ),
                    IncomeStatementAwaitingHandler(
                        id = incomeStatement2.id,
                        created = incomeStatement2.created,
                        startDate = incomeStatement2.startDate,
                        incomeEndDate = null,
                        handlerNote = "",
                        type = IncomeStatementType.HIGHEST_FEE,
                        personId = testAdult_2.id,
                        personLastName = "Doe",
                        personFirstName = "Joan",
                        primaryCareArea = area1.name,
                    ),
                    IncomeStatementAwaitingHandler(
                        id = incomeStatement3.id,
                        created = incomeStatement3.created,
                        startDate = incomeStatement3.startDate,
                        incomeEndDate = null,
                        handlerNote = "",
                        type = IncomeStatementType.HIGHEST_FEE,
                        personId = testAdult_3.id,
                        personLastName = "Foo",
                        personFirstName = "Mark",
                        primaryCareArea = null,
                    ),
                ),
                3,
                1,
            ),
            getIncomeStatementsAwaitingHandler(
                SearchIncomeStatementsRequest(),
                MockEvakaClock(HelsinkiDateTime.of(LocalDate.of(2022, 10, 19), LocalTime.MAX)),
            ),
        )
        assertEquals(
            PagedIncomeStatementsAwaitingHandler(
                listOf(
                    IncomeStatementAwaitingHandler(
                        id = incomeStatement2.id,
                        created = incomeStatement2.created,
                        startDate = incomeStatement2.startDate,
                        incomeEndDate = null,
                        handlerNote = "",
                        type = IncomeStatementType.HIGHEST_FEE,
                        personId = testAdult_2.id,
                        personLastName = "Doe",
                        personFirstName = "Joan",
                        primaryCareArea = area1.name,
                    )
                ),
                1,
                1,
            ),
            getIncomeStatementsAwaitingHandler(
                SearchIncomeStatementsRequest(placementValidDate = placement2End),
                MockEvakaClock(HelsinkiDateTime.of(LocalDate.of(2022, 10, 19), LocalTime.MAX)),
            ),
        )

        assertEquals(
            PagedIncomeStatementsAwaitingHandler(
                listOf(
                    IncomeStatementAwaitingHandler(
                        id = incomeStatement1.id,
                        created = newCreated,
                        startDate = incomeStatement1.startDate,
                        incomeEndDate = null,
                        handlerNote = "",
                        type = IncomeStatementType.HIGHEST_FEE,
                        personId = citizenId,
                        personLastName = "Doe",
                        personFirstName = "John",
                        primaryCareArea = area1.name,
                    ),
                    IncomeStatementAwaitingHandler(
                        id = incomeStatement2.id,
                        created = incomeStatement2.created,
                        startDate = incomeStatement2.startDate,
                        incomeEndDate = null,
                        handlerNote = "",
                        type = IncomeStatementType.HIGHEST_FEE,
                        personId = testAdult_2.id,
                        personLastName = "Doe",
                        personFirstName = "Joan",
                        primaryCareArea = area1.name,
                    ),
                ),
                2,
                1,
            ),
            getIncomeStatementsAwaitingHandler(
                SearchIncomeStatementsRequest(placementValidDate = placement2Start),
                MockEvakaClock(HelsinkiDateTime.of(LocalDate.of(2022, 10, 19), LocalTime.MAX)),
            ),
        )
    }

    @Test
    fun `list income statements awaiting handler - start date sort`() {
        val placementId1 = PlacementId(UUID.randomUUID())
        val placementId2 = PlacementId(UUID.randomUUID())
        val placementStart = today.minusDays(30)
        val placementEnd = today.plusDays(30)
        db.transaction { tx ->
            tx.insert(
                DevParentship(
                    childId = testChild_1.id,
                    headOfChildId = citizenId,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )
            tx.insert(
                DevPlacement(
                    id = placementId1,
                    type = PlacementType.PRESCHOOL_DAYCARE,
                    childId = testChild_1.id,
                    unitId = daycare1.id,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )

            tx.insert(
                DevParentship(
                    childId = testChild_2.id,
                    headOfChildId = testAdult_2.id,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )
            tx.insert(
                DevPlacement(
                    id = placementId2,
                    type = PlacementType.PRESCHOOL_DAYCARE,
                    childId = testChild_2.id,
                    unitId = daycare1.id,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )
        }

        val incomeStatement1 =
            createTestIncomeStatement(citizenId, startDate = LocalDate.of(2022, 10, 12))
        val incomeStatement2 =
            createTestIncomeStatement(testAdult_2.id, startDate = LocalDate.of(2022, 10, 13))

        val newCreated = HelsinkiDateTime.of(today.minusDays(2), LocalTime.of(12, 0))

        db.transaction {
            @Suppress("DEPRECATION")
            it.createUpdate("UPDATE income_statement SET created = :newCreated")
                .bind("newCreated", newCreated)
                .execute()
        }

        val expected1 =
            IncomeStatementAwaitingHandler(
                id = incomeStatement1.id,
                created = newCreated,
                startDate = incomeStatement1.startDate,
                incomeEndDate = null,
                handlerNote = "",
                type = IncomeStatementType.HIGHEST_FEE,
                personId = citizenId,
                personLastName = "Doe",
                personFirstName = "John",
                primaryCareArea = area1.name,
            )
        val expected2 =
            IncomeStatementAwaitingHandler(
                id = incomeStatement2.id,
                created = newCreated,
                startDate = incomeStatement2.startDate,
                incomeEndDate = null,
                handlerNote = "",
                type = IncomeStatementType.HIGHEST_FEE,
                personId = testAdult_2.id,
                personLastName = "Doe",
                personFirstName = "Joan",
                primaryCareArea = area1.name,
            )
        assertEquals(
            PagedIncomeStatementsAwaitingHandler(listOf(expected1, expected2), 2, 1),
            getIncomeStatementsAwaitingHandler(
                SearchIncomeStatementsRequest(
                    sortBy = IncomeStatementSortParam.START_DATE,
                    sortDirection = SortDirection.ASC,
                )
            ),
        )
        assertEquals(
            PagedIncomeStatementsAwaitingHandler(listOf(expected2, expected1), 2, 1),
            getIncomeStatementsAwaitingHandler(
                SearchIncomeStatementsRequest(
                    sortBy = IncomeStatementSortParam.START_DATE,
                    sortDirection = SortDirection.DESC,
                )
            ),
        )
    }

    @Test
    fun `list income statements awaiting handler - income end date sort`() {
        val placementStart = today.minusDays(30)
        val placementEnd = today.plusDays(30)
        val incomeRange1 = DateRange(today.minusDays(10), today)
        val incomeRange2 = DateRange(today.minusDays(20), null)
        db.transaction { tx ->
            tx.insert(
                DevParentship(
                    childId = testChild_1.id,
                    headOfChildId = citizenId,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )
            tx.insert(
                DevPlacement(
                    type = PlacementType.PRESCHOOL_DAYCARE,
                    childId = testChild_1.id,
                    unitId = daycare1.id,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )
            tx.insert(
                DevIncome(
                    personId = citizenId,
                    updatedBy = AuthenticatedUser.SystemInternalUser.evakaUserId,
                    validFrom = incomeRange1.start,
                    validTo = incomeRange1.end,
                )
            )

            tx.insert(
                DevParentship(
                    childId = testChild_2.id,
                    headOfChildId = testAdult_2.id,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )
            tx.insert(
                DevPlacement(
                    type = PlacementType.PRESCHOOL_DAYCARE,
                    childId = testChild_2.id,
                    unitId = daycare1.id,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )
            tx.insert(
                DevIncome(
                    personId = testAdult_2.id,
                    updatedBy = AuthenticatedUser.SystemInternalUser.evakaUserId,
                    validFrom = incomeRange2.start,
                    validTo = incomeRange2.end,
                )
            )
        }

        val incomeStatement1 =
            createTestIncomeStatement(citizenId, startDate = LocalDate.of(2022, 10, 12))
        val incomeStatement2 =
            createTestIncomeStatement(testAdult_2.id, startDate = LocalDate.of(2022, 10, 13))

        val expected1 =
            IncomeStatementAwaitingHandler(
                id = incomeStatement1.id,
                created = incomeStatement1.created,
                startDate = incomeStatement1.startDate,
                incomeEndDate = incomeRange1.end,
                handlerNote = "",
                type = IncomeStatementType.HIGHEST_FEE,
                personId = citizenId,
                personLastName = "Doe",
                personFirstName = "John",
                primaryCareArea = area1.name,
            )
        val expected2 =
            IncomeStatementAwaitingHandler(
                id = incomeStatement2.id,
                created = incomeStatement2.created,
                startDate = incomeStatement2.startDate,
                incomeEndDate = incomeRange2.end,
                handlerNote = "",
                type = IncomeStatementType.HIGHEST_FEE,
                personId = testAdult_2.id,
                personLastName = "Doe",
                personFirstName = "Joan",
                primaryCareArea = area1.name,
            )
        assertEquals(
            PagedIncomeStatementsAwaitingHandler(listOf(expected1, expected2), 2, 1),
            getIncomeStatementsAwaitingHandler(
                SearchIncomeStatementsRequest(
                    sortBy = IncomeStatementSortParam.INCOME_END_DATE,
                    sortDirection = SortDirection.ASC,
                )
            ),
        )
        assertEquals(
            PagedIncomeStatementsAwaitingHandler(listOf(expected2, expected1), 2, 1),
            getIncomeStatementsAwaitingHandler(
                SearchIncomeStatementsRequest(
                    sortBy = IncomeStatementSortParam.INCOME_END_DATE,
                    sortDirection = SortDirection.DESC,
                )
            ),
        )
    }

    @Test
    fun `list income statements awaiting handler - type sort`() {
        val placementStart = today.minusDays(30)
        val placementEnd = today.plusDays(30)
        db.transaction { tx ->
            tx.insert(
                DevParentship(
                    childId = testChild_1.id,
                    headOfChildId = testAdult_1.id,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )
            tx.insert(
                DevPlacement(
                    type = PlacementType.DAYCARE,
                    childId = testChild_1.id,
                    unitId = daycare1.id,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )

            tx.insert(
                DevParentship(
                    childId = testChild_2.id,
                    headOfChildId = testAdult_2.id,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )
            tx.insert(
                DevPlacement(
                    type = PlacementType.DAYCARE,
                    childId = testChild_2.id,
                    unitId = daycare1.id,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )
        }

        val incomeStatement1 =
            createTestIncomeStatement(testAdult_1.id, startDate = LocalDate.of(2022, 10, 12))
        val incomeStatement2 =
            createChildTestIncomeStatement(testChild_2.id, startDate = LocalDate.of(2022, 10, 13))

        val expected1 =
            IncomeStatementAwaitingHandler(
                id = incomeStatement1.id,
                created = incomeStatement1.created,
                startDate = incomeStatement1.startDate,
                incomeEndDate = null,
                handlerNote = "",
                type = IncomeStatementType.HIGHEST_FEE,
                personId = testAdult_1.id,
                personLastName = "Doe",
                personFirstName = "John",
                primaryCareArea = area1.name,
            )
        val expected2 =
            IncomeStatementAwaitingHandler(
                id = incomeStatement2.id,
                created = incomeStatement2.created,
                startDate = incomeStatement2.startDate,
                incomeEndDate = null,
                handlerNote = "",
                type = IncomeStatementType.CHILD_INCOME,
                personId = testChild_2.id,
                personLastName = "Doe",
                personFirstName = "Micky",
                primaryCareArea = area1.name,
            )
        assertEquals(
            PagedIncomeStatementsAwaitingHandler(listOf(expected1, expected2), 2, 1),
            getIncomeStatementsAwaitingHandler(
                SearchIncomeStatementsRequest(
                    sortBy = IncomeStatementSortParam.TYPE,
                    sortDirection = SortDirection.ASC,
                )
            ),
        )
        assertEquals(
            PagedIncomeStatementsAwaitingHandler(listOf(expected2, expected1), 2, 1),
            getIncomeStatementsAwaitingHandler(
                SearchIncomeStatementsRequest(
                    sortBy = IncomeStatementSortParam.TYPE,
                    sortDirection = SortDirection.DESC,
                )
            ),
        )
    }

    @Test
    fun `list income statements awaiting handler - handler note sort`() {
        val placementStart = today.minusDays(30)
        val placementEnd = today.plusDays(30)
        db.transaction { tx ->
            tx.insert(
                DevParentship(
                    childId = testChild_1.id,
                    headOfChildId = testAdult_1.id,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )
            tx.insert(
                DevPlacement(
                    type = PlacementType.DAYCARE,
                    childId = testChild_1.id,
                    unitId = daycare1.id,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )

            tx.insert(
                DevParentship(
                    childId = testChild_2.id,
                    headOfChildId = testAdult_2.id,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )
            tx.insert(
                DevPlacement(
                    type = PlacementType.DAYCARE,
                    childId = testChild_2.id,
                    unitId = daycare1.id,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )
        }

        val incomeStatement1 =
            createTestIncomeStatement(testAdult_1.id, startDate = LocalDate.of(2022, 10, 12))
        setIncomeStatementHandled(
            incomeStatement1.id,
            IncomeStatementController.SetIncomeStatementHandledBody(
                handled = false,
                handlerNote = "a",
            ),
        )
        val incomeStatement2 =
            createTestIncomeStatement(testAdult_2.id, startDate = LocalDate.of(2022, 10, 13))
        setIncomeStatementHandled(
            incomeStatement2.id,
            IncomeStatementController.SetIncomeStatementHandledBody(
                handled = false,
                handlerNote = "b",
            ),
        )

        val expected1 =
            IncomeStatementAwaitingHandler(
                id = incomeStatement1.id,
                created = incomeStatement1.created,
                startDate = incomeStatement1.startDate,
                incomeEndDate = null,
                handlerNote = "a",
                type = IncomeStatementType.HIGHEST_FEE,
                personId = testAdult_1.id,
                personLastName = "Doe",
                personFirstName = "John",
                primaryCareArea = area1.name,
            )
        val expected2 =
            IncomeStatementAwaitingHandler(
                id = incomeStatement2.id,
                created = incomeStatement2.created,
                startDate = incomeStatement2.startDate,
                incomeEndDate = null,
                handlerNote = "b",
                type = IncomeStatementType.HIGHEST_FEE,
                personId = testAdult_2.id,
                personLastName = "Doe",
                personFirstName = "Joan",
                primaryCareArea = area1.name,
            )
        assertEquals(
            PagedIncomeStatementsAwaitingHandler(listOf(expected1, expected2), 2, 1),
            getIncomeStatementsAwaitingHandler(
                SearchIncomeStatementsRequest(
                    sortBy = IncomeStatementSortParam.HANDLER_NOTE,
                    sortDirection = SortDirection.ASC,
                )
            ),
        )
        assertEquals(
            PagedIncomeStatementsAwaitingHandler(listOf(expected2, expected1), 2, 1),
            getIncomeStatementsAwaitingHandler(
                SearchIncomeStatementsRequest(
                    sortBy = IncomeStatementSortParam.HANDLER_NOTE,
                    sortDirection = SortDirection.DESC,
                )
            ),
        )
    }

    @Test
    fun `list income statements awaiting handler - person name sort`() {
        val placementStart = today.minusDays(30)
        val placementEnd = today.plusDays(30)
        db.transaction { tx ->
            tx.insert(
                DevParentship(
                    childId = testChild_1.id,
                    headOfChildId = testAdult_1.id,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )
            tx.insert(
                DevPlacement(
                    type = PlacementType.DAYCARE,
                    childId = testChild_1.id,
                    unitId = daycare1.id,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )

            tx.insert(
                DevParentship(
                    childId = testChild_2.id,
                    headOfChildId = testAdult_2.id,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )
            tx.insert(
                DevPlacement(
                    type = PlacementType.DAYCARE,
                    childId = testChild_2.id,
                    unitId = daycare1.id,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )
        }

        val incomeStatement1 =
            createTestIncomeStatement(testAdult_1.id, startDate = LocalDate.of(2022, 10, 12))
        val incomeStatement2 =
            createTestIncomeStatement(testAdult_2.id, startDate = LocalDate.of(2022, 10, 13))

        val expected1 =
            IncomeStatementAwaitingHandler(
                id = incomeStatement1.id,
                created = incomeStatement1.created,
                startDate = incomeStatement1.startDate,
                incomeEndDate = null,
                handlerNote = "",
                type = IncomeStatementType.HIGHEST_FEE,
                personId = testAdult_1.id,
                personLastName = "Doe",
                personFirstName = "John",
                primaryCareArea = area1.name,
            )
        val expected2 =
            IncomeStatementAwaitingHandler(
                id = incomeStatement2.id,
                created = incomeStatement2.created,
                startDate = incomeStatement2.startDate,
                incomeEndDate = null,
                handlerNote = "",
                type = IncomeStatementType.HIGHEST_FEE,
                personId = testAdult_2.id,
                personLastName = "Doe",
                personFirstName = "Joan",
                primaryCareArea = area1.name,
            )
        assertEquals(
            PagedIncomeStatementsAwaitingHandler(listOf(expected2, expected1), 2, 1),
            getIncomeStatementsAwaitingHandler(
                SearchIncomeStatementsRequest(
                    sortBy = IncomeStatementSortParam.PERSON_NAME,
                    sortDirection = SortDirection.ASC,
                )
            ),
        )
        assertEquals(
            PagedIncomeStatementsAwaitingHandler(listOf(expected1, expected2), 2, 1),
            getIncomeStatementsAwaitingHandler(
                SearchIncomeStatementsRequest(
                    sortBy = IncomeStatementSortParam.PERSON_NAME,
                    sortDirection = SortDirection.DESC,
                )
            ),
        )
    }

    private fun getIncomeStatement(id: IncomeStatementId): IncomeStatement {
        return incomeStatementController.getIncomeStatement(
            dbInstance(),
            employee,
            MockEvakaClock(now),
            citizenId,
            id,
        )
    }

    private fun getIncomeStatements(personId: PersonId): PagedIncomeStatements {
        return incomeStatementController.getIncomeStatements(
            dbInstance(),
            employee,
            MockEvakaClock(now),
            personId,
            page = 1,
        )
    }

    private fun setIncomeStatementHandled(
        id: IncomeStatementId,
        body: IncomeStatementController.SetIncomeStatementHandledBody,
    ) {
        incomeStatementController.setIncomeStatementHandled(
            dbInstance(),
            employee,
            MockEvakaClock(now),
            id,
            body,
        )
    }

    private fun getIncomeStatementsAwaitingHandler(
        body: SearchIncomeStatementsRequest =
            SearchIncomeStatementsRequest(
                1,
                null,
                null,
                emptyList(),
                null,
                emptyList(),
                null,
                null,
            ),
        clock: EvakaClock = MockEvakaClock(now),
    ): PagedIncomeStatementsAwaitingHandler {
        return incomeStatementController.getIncomeStatementsAwaitingHandler(
            dbInstance(),
            employee,
            clock,
            body,
        )
    }

    private fun uploadAttachment(id: IncomeStatementId): AttachmentId {
        return attachmentsController.uploadIncomeStatementAttachmentEmployee(
            dbInstance(),
            employee,
            MockEvakaClock(now),
            id,
            MockMultipartFile("file", "evaka-logo.png", "image/png", pngFile.readBytes()),
        )
    }
}
