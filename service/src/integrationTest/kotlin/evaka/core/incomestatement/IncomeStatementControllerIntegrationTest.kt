// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.incomestatement

import evaka.core.FullApplicationTest
import evaka.core.application.ApplicationType
import evaka.core.application.persistence.daycare.Adult
import evaka.core.application.persistence.daycare.Apply
import evaka.core.application.persistence.daycare.Child
import evaka.core.application.persistence.daycare.DaycareFormV0
import evaka.core.attachment.AttachmentsController
import evaka.core.daycare.domain.ProviderType
import evaka.core.invoicing.controller.SortDirection
import evaka.core.invoicing.domain.IncomeEffect
import evaka.core.pis.service.insertGuardian
import evaka.core.placement.PlacementType
import evaka.core.shared.AttachmentId
import evaka.core.shared.EvakaUserId
import evaka.core.shared.IncomeStatementId
import evaka.core.shared.PersonId
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.auth.UserRole
import evaka.core.shared.dev.DevCareArea
import evaka.core.shared.dev.DevDaycare
import evaka.core.shared.dev.DevEmployee
import evaka.core.shared.dev.DevGuardian
import evaka.core.shared.dev.DevIncome
import evaka.core.shared.dev.DevIncomeStatement
import evaka.core.shared.dev.DevParentship
import evaka.core.shared.dev.DevPerson
import evaka.core.shared.dev.DevPersonType
import evaka.core.shared.dev.DevPlacement
import evaka.core.shared.dev.insert
import evaka.core.shared.dev.insertTestApplication
import evaka.core.shared.dev.insertTestPartnership
import evaka.core.shared.domain.BadRequest
import evaka.core.shared.domain.DateRange
import evaka.core.shared.domain.EvakaClock
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.shared.domain.MockEvakaClock
import evaka.core.shared.domain.NotFound
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.mock.web.MockMultipartFile

class IncomeStatementControllerIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var incomeStatementController: IncomeStatementController
    @Autowired private lateinit var attachmentsController: AttachmentsController

    private val area1 = DevCareArea(name = "Area 1", shortName = "area1")
    private val daycare1 = DevDaycare(areaId = area1.id, name = "Daycare 1")
    private val daycarePurchased =
        DevDaycare(
            areaId = area1.id,
            name = "Purchased Daycare",
            providerType = ProviderType.PURCHASED,
        )
    private val area2 = DevCareArea(name = "Area 2", shortName = "area2")
    private val daycare2 = DevDaycare(areaId = area2.id, name = "Daycare 2")

    private val adult1 = DevPerson(firstName = "John", lastName = "Doe")
    private val adult2 = DevPerson(firstName = "Joan", lastName = "Doe")
    private val adult3 = DevPerson(firstName = "Mark", lastName = "Foo")
    private val adult4 = DevPerson(firstName = "Dork", lastName = "Aman")
    private val adult5 = DevPerson(firstName = "Johannes Olavi Antero Tapio", lastName = "Karhula")
    private val adult6 = DevPerson(firstName = "Ville", lastName = "Vilkas")
    private val child1 = DevPerson(firstName = "Ricky", lastName = "Doe")
    private val child2 = DevPerson(firstName = "Micky", lastName = "Doe")
    private val child3 = DevPerson(firstName = "Hillary", lastName = "Foo")
    private val child4 = DevPerson(firstName = "Maisa", lastName = "Farang")
    private val child5 = DevPerson(firstName = "Visa", lastName = "Virén")

    private val employee = DevEmployee(roles = setOf(UserRole.FINANCE_ADMIN))

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
            listOf(adult1, adult2, adult3, adult4, adult5, adult6).forEach {
                tx.insert(it, DevPersonType.ADULT)
            }
            listOf(child1, child2, child3, child4, child5).forEach {
                tx.insert(it, DevPersonType.CHILD)
            }
            tx.insert(employee)
        }
    }

    @Test
    fun `set as handled`() {
        val startDate = today
        val endDate = today.plusDays(30)

        val incomeStatement =
            DevIncomeStatement(
                personId = adult1.id,
                data = IncomeStatementBody.HighestFee(startDate, endDate),
                status = IncomeStatementStatus.SENT,
                createdAt = now.minusHours(10),
                modifiedAt = now.minusHours(7),
                sentAt = now.minusHours(5),
            )
        db.transaction { it.insert(incomeStatement) }
        val id = incomeStatement.id

        val incomeStatement1 = getIncomeStatement(id)
        assertEquals(
            IncomeStatement.HighestFee(
                id = id,
                personId = adult1.id,
                firstName = adult1.firstName,
                lastName = adult1.lastName,
                startDate = startDate,
                endDate = endDate,
                createdAt = incomeStatement.createdAt,
                modifiedAt = incomeStatement.modifiedAt,
                sentAt = incomeStatement.sentAt,
                status = IncomeStatementStatus.SENT,
                handledAt = null,
                handlerNote = "",
            ),
            incomeStatement1,
        )

        setIncomeStatementHandled(
            id,
            IncomeStatementController.SetIncomeStatementHandledBody(
                IncomeStatementStatus.HANDLING,
                "",
            ),
        )

        val incomeStatement2 = getIncomeStatement(id)
        assertEquals(
            IncomeStatement.HighestFee(
                id = id,
                personId = adult1.id,
                firstName = adult1.firstName,
                lastName = adult1.lastName,
                startDate = startDate,
                endDate = endDate,
                createdAt = incomeStatement.createdAt,
                modifiedAt = incomeStatement2.modifiedAt,
                sentAt = incomeStatement.sentAt,
                status = IncomeStatementStatus.HANDLING,
                handledAt = null,
                handlerNote = "",
            ),
            incomeStatement2,
        )

        setIncomeStatementHandled(
            id,
            IncomeStatementController.SetIncomeStatementHandledBody(
                IncomeStatementStatus.HANDLED,
                "is cool",
            ),
        )

        val incomeStatement3 = getIncomeStatement(id)
        assertEquals(
            IncomeStatement.HighestFee(
                id = id,
                personId = adult1.id,
                firstName = adult1.firstName,
                lastName = adult1.lastName,
                startDate = startDate,
                endDate = endDate,
                createdAt = incomeStatement3.createdAt,
                modifiedAt = incomeStatement3.modifiedAt,
                sentAt = incomeStatement3.sentAt,
                status = IncomeStatementStatus.HANDLED,
                handledAt = now,
                handlerNote = "is cool",
            ),
            incomeStatement3,
        )

        setIncomeStatementHandled(
            id,
            IncomeStatementController.SetIncomeStatementHandledBody(
                IncomeStatementStatus.HANDLING,
                "is not cool",
            ),
        )

        val incomeStatement4 = getIncomeStatement(id)
        assertEquals(
            IncomeStatement.HighestFee(
                id = id,
                personId = adult1.id,
                firstName = adult1.firstName,
                lastName = adult1.lastName,
                startDate = startDate,
                endDate = endDate,
                createdAt = incomeStatement3.createdAt,
                modifiedAt = incomeStatement3.modifiedAt,
                sentAt = incomeStatement3.sentAt,
                status = IncomeStatementStatus.HANDLING,
                handledAt = null,
                handlerNote = "is not cool",
            ),
            incomeStatement4,
        )

        assertEquals(
            listOf(false),
            getIncomeStatements(adult1.id).data.map { it.status == IncomeStatementStatus.HANDLED },
        )
    }

    @Test
    fun `cannot set to draft`() {
        val incomeStatement =
            DevIncomeStatement(
                personId = adult1.id,
                data = IncomeStatementBody.HighestFee(today, today.plusDays(30)),
                status = IncomeStatementStatus.SENT,
                sentAt = now,
            )
        db.transaction { it.insert(incomeStatement) }
        val id = incomeStatement.id

        assertThrows<BadRequest> {
            setIncomeStatementHandled(
                id,
                IncomeStatementController.SetIncomeStatementHandledBody(
                    IncomeStatementStatus.DRAFT,
                    "",
                ),
            )
        }
    }

    @Test
    fun `cannot read draft statements`() {
        val sentIncomeStatement =
            DevIncomeStatement(
                personId = adult1.id,
                data = IncomeStatementBody.HighestFee(today, today.plusDays(9)),
                status = IncomeStatementStatus.SENT,
                sentAt = now,
            )
        val draftIncomeStatement =
            DevIncomeStatement(
                personId = adult1.id,
                data = IncomeStatementBody.HighestFee(today.plusDays(10), today.plusDays(20)),
                status = IncomeStatementStatus.DRAFT,
                sentAt = null,
            )
        db.transaction {
            it.insert(sentIncomeStatement)
            it.insert(draftIncomeStatement)
        }

        assertEquals(
            listOf(sentIncomeStatement.id),
            getIncomeStatements(adult1.id).data.map { it.id },
        )
        assertEquals(today, getIncomeStatement(sentIncomeStatement.id).startDate)
        assertThrows<NotFound> { getIncomeStatement(draftIncomeStatement.id) }
    }

    @Test
    fun `add an attachment`() {
        val devIncomeStatement =
            DevIncomeStatement(
                personId = adult1.id,
                status = IncomeStatementStatus.SENT,
                sentAt = now,
                data =
                    IncomeStatementBody.Income(
                        startDate = today,
                        endDate = null,
                        gross =
                            Gross.Income(
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
        db.transaction { it.insert(devIncomeStatement) }
        val id = devIncomeStatement.id

        val attachmentId = uploadAttachment(id)

        val incomeStatement = getIncomeStatement(id)
        assertEquals(
            IncomeStatement.Income(
                id = id,
                personId = adult1.id,
                firstName = adult1.firstName,
                lastName = adult1.lastName,
                startDate = today,
                endDate = null,
                gross =
                    Gross.Income(
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
                        IncomeStatementAttachment(
                            id = attachmentId,
                            name = "evaka-logo.png",
                            contentType = "image/png",
                            type = IncomeStatementAttachmentType.OTHER,
                            uploadedByEmployee = true,
                        )
                    ),
                createdAt = incomeStatement.createdAt,
                modifiedAt = incomeStatement.modifiedAt,
                sentAt = now,
                status = IncomeStatementStatus.SENT,
                handledAt = null,
                handlerNote = "",
            ),
            getIncomeStatement(id),
        )
    }

    private fun createTestIncomeStatement(
        personId: PersonId,
        startDate: LocalDate? = null,
        sentAt: HelsinkiDateTime = now,
    ): IncomeStatement {
        val incomeStatement =
            DevIncomeStatement(
                personId = personId,
                data =
                    IncomeStatementBody.HighestFee(startDate = startDate ?: today, endDate = null),
                status = IncomeStatementStatus.SENT,
                sentAt = sentAt,
            )

        return db.transaction { tx ->
            tx.insert(incomeStatement)
            tx.readIncomeStatement(employee.user, incomeStatement.id)!!
        }
    }

    private fun createChildTestIncomeStatement(
        personId: PersonId,
        startDate: LocalDate? = null,
        sentAt: HelsinkiDateTime = now,
    ): IncomeStatement {
        val incomeStatement =
            DevIncomeStatement(
                personId = personId,
                data =
                    IncomeStatementBody.ChildIncome(
                        startDate = startDate ?: today,
                        endDate = null,
                        attachmentIds = listOf(),
                        otherInfo = "",
                    ),
                status = IncomeStatementStatus.SENT,
                sentAt = sentAt,
            )

        return db.transaction { tx ->
            tx.insert(incomeStatement)
            tx.readIncomeStatement(employee.user, incomeStatement.id)!!
        }
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
        val placementStart = today.minusDays(30)
        val placementEnd = today.plusDays(30)
        val createdAt = HelsinkiDateTime.of(placementStart, LocalTime.of(12, 0, 0))

        db.transaction { tx ->
            tx.insert(
                DevParentship(
                    childId = child1.id,
                    headOfChildId = adult1.id,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )
            tx.insert(
                DevPlacement(
                    type = PlacementType.PRESCHOOL_DAYCARE,
                    childId = child1.id,
                    unitId = daycare1.id,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )

            // `validTo` of the newest income statement is returned as `incomeEndDate`
            tx.insert(
                DevIncome(
                    personId = adult1.id,
                    modifiedBy = AuthenticatedUser.SystemInternalUser.evakaUserId,
                    validFrom = incomeDate1.minusYears(1),
                    validTo = incomeDate1.minusMonths(1).minusDays(1),
                )
            )
            tx.insert(
                DevIncome(
                    personId = adult1.id,
                    modifiedBy = AuthenticatedUser.SystemInternalUser.evakaUserId,
                    validFrom = incomeDate1.minusMonths(1),
                    validTo = incomeDate1,
                )
            )

            tx.insert(
                DevParentship(
                    childId = child2.id,
                    headOfChildId = adult2.id,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )
            tx.insert(
                DevParentship(
                    childId = child3.id,
                    headOfChildId = adult2.id,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )
            tx.insertTestPartnership(
                adult2.id,
                adult3.id,
                startDate = placementStart,
                endDate = null,
                createdAt = createdAt,
            )
            tx.insert(
                DevPlacement(
                    type = PlacementType.PRESCHOOL_DAYCARE,
                    childId = child2.id,
                    unitId = daycare2.id,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )
            tx.insert(
                DevPlacement(
                    type = PlacementType.PRESCHOOL_DAYCARE,
                    childId = child3.id,
                    unitId = daycare2.id,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )

            // Latest income has no end date -> incomeEndDate is null
            tx.insert(
                DevIncome(
                    personId = adult2.id,
                    modifiedBy = AuthenticatedUser.SystemInternalUser.evakaUserId,
                    validFrom = incomeDate2.minusMonths(1),
                    validTo = incomeDate2.minusDays(1),
                )
            )
            tx.insert(
                DevIncome(
                    personId = adult2.id,
                    modifiedBy = AuthenticatedUser.SystemInternalUser.evakaUserId,
                    effect = IncomeEffect.MAX_FEE_ACCEPTED,
                    validFrom = incomeDate2,
                    validTo = null,
                )
            )

            tx.insert(
                DevParentship(
                    childId = child4.id,
                    headOfChildId = adult4.id,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )
            tx.insertTestApplication(
                type = ApplicationType.PRESCHOOL,
                guardianId = adult4.id,
                childId = child4.id,
                document =
                    DaycareFormV0(
                        type = ApplicationType.PRESCHOOL,
                        connectedDaycare = true,
                        child = Child(dateOfBirth = null),
                        guardian = Adult(),
                        apply = Apply(preferredUnits = listOf(daycare1.id)),
                    ),
            )

            tx.insertGuardian(adult5.id, child5.id)
            tx.insert(
                DevPlacement(
                    type = PlacementType.PRESCHOOL_DAYCARE,
                    childId = child5.id,
                    unitId = daycare2.id,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )
        }

        val incomeStatement1 = createTestIncomeStatement(adult1.id, sentAt = now.minusHours(7))
        val incomeStatement2 = createTestIncomeStatement(adult2.id, sentAt = now.minusHours(6))
        val incomeStatement3 = createTestIncomeStatement(adult3.id, sentAt = now.minusHours(5))
        val incomeStatement4 = createTestIncomeStatement(adult4.id, sentAt = now.minusHours(4))
        val incomeStatement5 = createTestIncomeStatement(adult5.id, sentAt = now.minusHours(3))
        val incomeStatement6 = createTestIncomeStatement(adult6.id, sentAt = now.minusHours(2))
        val incomeStatement7 = createChildTestIncomeStatement(child1.id, sentAt = now.minusHours(1))

        assertEquals(
            PagedIncomeStatementsAwaitingHandler(
                listOf(
                    IncomeStatementAwaitingHandler(
                        id = incomeStatement1.id,
                        sentAt = incomeStatement1.sentAt!!,
                        citizenModifiedAt = incomeStatement1.sentAt!!,
                        startDate = incomeStatement1.startDate,
                        incomeEndDate = incomeDate1,
                        handlerNote = "",
                        type = IncomeStatementType.HIGHEST_FEE,
                        personId = adult1.id,
                        personLastName = "Doe",
                        personFirstName = "John",
                        primaryCareArea = area1.name,
                    ),
                    IncomeStatementAwaitingHandler(
                        id = incomeStatement2.id,
                        sentAt = incomeStatement2.sentAt!!,
                        citizenModifiedAt = incomeStatement2.sentAt!!,
                        startDate = incomeStatement2.startDate,
                        incomeEndDate = null,
                        handlerNote = "",
                        type = IncomeStatementType.HIGHEST_FEE,
                        personId = adult2.id,
                        personLastName = "Doe",
                        personFirstName = "Joan",
                        primaryCareArea = area2.name,
                    ),
                    IncomeStatementAwaitingHandler(
                        id = incomeStatement3.id,
                        sentAt = incomeStatement3.sentAt!!,
                        citizenModifiedAt = incomeStatement3.sentAt!!,
                        startDate = incomeStatement3.startDate,
                        incomeEndDate = null,
                        handlerNote = "",
                        type = IncomeStatementType.HIGHEST_FEE,
                        personId = adult3.id,
                        personLastName = "Foo",
                        personFirstName = "Mark",
                        primaryCareArea = area2.name,
                    ),
                    IncomeStatementAwaitingHandler(
                        id = incomeStatement4.id,
                        sentAt = incomeStatement4.sentAt!!,
                        citizenModifiedAt = incomeStatement4.sentAt!!,
                        startDate = incomeStatement4.startDate,
                        incomeEndDate = null,
                        handlerNote = "",
                        type = IncomeStatementType.HIGHEST_FEE,
                        personId = adult4.id,
                        personLastName = "Aman",
                        personFirstName = "Dork",
                        primaryCareArea = area1.name,
                    ),
                    IncomeStatementAwaitingHandler(
                        id = incomeStatement5.id,
                        sentAt = incomeStatement5.sentAt!!,
                        citizenModifiedAt = incomeStatement5.sentAt!!,
                        startDate = incomeStatement5.startDate,
                        incomeEndDate = null,
                        handlerNote = "",
                        type = IncomeStatementType.HIGHEST_FEE,
                        personId = adult5.id,
                        personLastName = "Karhula",
                        personFirstName = "Johannes Olavi Antero Tapio",
                        primaryCareArea = null,
                    ),
                    IncomeStatementAwaitingHandler(
                        id = incomeStatement6.id,
                        sentAt = incomeStatement6.sentAt!!,
                        citizenModifiedAt = incomeStatement6.sentAt!!,
                        startDate = incomeStatement6.startDate,
                        incomeEndDate = null,
                        handlerNote = "",
                        type = IncomeStatementType.HIGHEST_FEE,
                        personId = adult6.id,
                        personLastName = "Vilkas",
                        personFirstName = "Ville",
                        primaryCareArea = null,
                    ),
                    IncomeStatementAwaitingHandler(
                        id = incomeStatement7.id,
                        sentAt = incomeStatement7.sentAt!!,
                        citizenModifiedAt = incomeStatement7.sentAt!!,
                        startDate = incomeStatement7.startDate,
                        incomeEndDate = null,
                        handlerNote = "",
                        type = IncomeStatementType.CHILD_INCOME,
                        personId = child1.id,
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
        val placementStart = today.minusDays(30)
        val placementEnd = today.plusDays(30)
        db.transaction { tx ->
            tx.insert(
                DevParentship(
                    childId = child1.id,
                    headOfChildId = adult1.id,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )
            tx.insert(
                DevPlacement(
                    type = PlacementType.PRESCHOOL_DAYCARE,
                    childId = child1.id,
                    unitId = daycare1.id,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )

            tx.insert(
                DevParentship(
                    childId = child2.id,
                    headOfChildId = adult2.id,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )
            tx.insert(
                DevPlacement(
                    type = PlacementType.PRESCHOOL_DAYCARE,
                    childId = child2.id,
                    unitId = daycare2.id,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )
        }

        createTestIncomeStatement(adult1.id)
        val incomeStatement2 = createTestIncomeStatement(adult2.id)

        assertEquals(
            PagedIncomeStatementsAwaitingHandler(
                listOf(
                    IncomeStatementAwaitingHandler(
                        id = incomeStatement2.id,
                        sentAt = incomeStatement2.sentAt!!,
                        citizenModifiedAt = incomeStatement2.sentAt!!,
                        startDate = incomeStatement2.startDate,
                        incomeEndDate = null,
                        handlerNote = "",
                        type = IncomeStatementType.HIGHEST_FEE,
                        personId = adult2.id,
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
        val placementStart = today.minusDays(30)
        val placementEnd = today.plusDays(30)
        db.transaction { tx ->
            tx.insert(
                DevParentship(
                    childId = child1.id,
                    headOfChildId = adult1.id,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )
            tx.insert(
                DevPlacement(
                    type = PlacementType.PRESCHOOL_DAYCARE,
                    childId = child1.id,
                    unitId = daycare1.id,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )

            tx.insert(
                DevParentship(
                    childId = child2.id,
                    headOfChildId = adult2.id,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )
            tx.insert(
                DevPlacement(
                    type = PlacementType.PRESCHOOL_DAYCARE,
                    childId = child2.id,
                    unitId = daycare2.id,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )
        }

        createTestIncomeStatement(adult1.id)
        val incomeStatement2 = createTestIncomeStatement(adult2.id)

        assertEquals(
            PagedIncomeStatementsAwaitingHandler(
                listOf(
                    IncomeStatementAwaitingHandler(
                        id = incomeStatement2.id,
                        sentAt = incomeStatement2.sentAt!!,
                        citizenModifiedAt = incomeStatement2.sentAt!!,
                        startDate = incomeStatement2.startDate,
                        incomeEndDate = null,
                        handlerNote = "",
                        type = IncomeStatementType.HIGHEST_FEE,
                        personId = adult2.id,
                        personLastName = "Doe",
                        personFirstName = "Joan",
                        primaryCareArea = area2.name,
                    )
                ),
                1,
                1,
            ),
            getIncomeStatementsAwaitingHandler(
                SearchIncomeStatementsRequest(unitIds = listOf(daycare2.id))
            ),
        )
    }

    @Test
    fun `list income statements awaiting handler - multiple units filter`() {
        val placementStart = today.minusDays(30)
        val placementEnd = today.plusDays(30)
        db.transaction { tx ->
            tx.insert(
                DevParentship(
                    childId = child1.id,
                    headOfChildId = adult1.id,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )
            tx.insert(
                DevPlacement(
                    type = PlacementType.PRESCHOOL_DAYCARE,
                    childId = child1.id,
                    unitId = daycare1.id,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )

            tx.insert(
                DevParentship(
                    childId = child2.id,
                    headOfChildId = adult2.id,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )
            tx.insert(
                DevPlacement(
                    type = PlacementType.PRESCHOOL_DAYCARE,
                    childId = child2.id,
                    unitId = daycare2.id,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )

            tx.insert(
                DevParentship(
                    childId = child3.id,
                    headOfChildId = adult3.id,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )
            tx.insert(
                DevPlacement(
                    type = PlacementType.PRESCHOOL_DAYCARE,
                    childId = child3.id,
                    unitId = daycarePurchased.id,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )
        }

        val incomeStatement1 = createTestIncomeStatement(adult1.id)
        val incomeStatement2 = createTestIncomeStatement(adult2.id)
        createTestIncomeStatement(adult3.id)

        assertEquals(
            PagedIncomeStatementsAwaitingHandler(
                listOf(
                    IncomeStatementAwaitingHandler(
                        id = incomeStatement2.id,
                        sentAt = incomeStatement2.sentAt!!,
                        citizenModifiedAt = incomeStatement2.sentAt!!,
                        startDate = incomeStatement2.startDate,
                        incomeEndDate = null,
                        handlerNote = "",
                        type = IncomeStatementType.HIGHEST_FEE,
                        personId = adult2.id,
                        personLastName = "Doe",
                        personFirstName = "Joan",
                        primaryCareArea = area2.name,
                    ),
                    IncomeStatementAwaitingHandler(
                        id = incomeStatement1.id,
                        sentAt = incomeStatement1.sentAt!!,
                        citizenModifiedAt = incomeStatement1.sentAt!!,
                        startDate = incomeStatement1.startDate,
                        incomeEndDate = null,
                        handlerNote = "",
                        type = IncomeStatementType.HIGHEST_FEE,
                        personId = adult1.id,
                        personLastName = "Doe",
                        personFirstName = "John",
                        primaryCareArea = area1.name,
                    ),
                ),
                2,
                1,
            ),
            getIncomeStatementsAwaitingHandler(
                SearchIncomeStatementsRequest(unitIds = listOf(daycare1.id, daycare2.id))
            ),
        )
    }

    @Test
    fun `list income statements awaiting handler - empty units list returns unfiltered`() {
        val placementStart = today.minusDays(30)
        val placementEnd = today.plusDays(30)
        db.transaction { tx ->
            tx.insert(
                DevParentship(
                    childId = child1.id,
                    headOfChildId = adult1.id,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )
            tx.insert(
                DevPlacement(
                    type = PlacementType.PRESCHOOL_DAYCARE,
                    childId = child1.id,
                    unitId = daycare1.id,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )

            tx.insert(
                DevParentship(
                    childId = child2.id,
                    headOfChildId = adult2.id,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )
            tx.insert(
                DevPlacement(
                    type = PlacementType.PRESCHOOL_DAYCARE,
                    childId = child2.id,
                    unitId = daycare2.id,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )
        }

        createTestIncomeStatement(adult1.id)
        createTestIncomeStatement(adult2.id)

        val unfiltered = getIncomeStatementsAwaitingHandler(SearchIncomeStatementsRequest())
        val emptyUnits =
            getIncomeStatementsAwaitingHandler(SearchIncomeStatementsRequest(unitIds = emptyList()))

        assertEquals(2, unfiltered.total)
        assertEquals(unfiltered, emptyUnits)
    }

    @Test
    fun `list income statements awaiting handler - provider type filter`() {
        val placementStart = today.minusDays(30)
        val placementEnd = today.plusDays(30)
        db.transaction { tx ->
            tx.insert(
                DevParentship(
                    childId = child1.id,
                    headOfChildId = adult1.id,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )
            tx.insert(
                DevPlacement(
                    type = PlacementType.PRESCHOOL_DAYCARE,
                    childId = child1.id,
                    unitId = daycarePurchased.id,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )

            tx.insert(
                DevParentship(
                    childId = child2.id,
                    headOfChildId = adult2.id,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )
            tx.insert(
                DevPlacement(
                    type = PlacementType.PRESCHOOL_DAYCARE,
                    childId = child2.id,
                    unitId = daycare1.id,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )
        }

        createTestIncomeStatement(adult1.id)
        val incomeStatement2 = createTestIncomeStatement(adult2.id)

        assertEquals(
            PagedIncomeStatementsAwaitingHandler(
                listOf(
                    IncomeStatementAwaitingHandler(
                        id = incomeStatement2.id,
                        sentAt = incomeStatement2.sentAt!!,
                        citizenModifiedAt = incomeStatement2.sentAt!!,
                        startDate = incomeStatement2.startDate,
                        incomeEndDate = null,
                        handlerNote = "",
                        type = IncomeStatementType.HIGHEST_FEE,
                        personId = adult2.id,
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
        val placementStart = today.minusDays(30)
        val placementEnd = today.plusDays(30)
        db.transaction { tx ->
            tx.insert(
                DevParentship(
                    childId = child1.id,
                    headOfChildId = adult1.id,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )
            tx.insert(
                DevPlacement(
                    type = PlacementType.PRESCHOOL_DAYCARE,
                    childId = child1.id,
                    unitId = daycare1.id,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )

            tx.insert(
                DevParentship(
                    childId = child2.id,
                    headOfChildId = adult2.id,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )
            tx.insert(
                DevPlacement(
                    type = PlacementType.PRESCHOOL_DAYCARE,
                    childId = child2.id,
                    unitId = daycare1.id,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )
        }

        val incomeStatement1 = createTestIncomeStatement(adult1.id)
        val incomeStatement2 = createTestIncomeStatement(adult2.id)

        val newSentAt = HelsinkiDateTime.of(today.minusDays(2), LocalTime.of(12, 0))

        db.transaction {
            it.execute {
                sql(
                    "UPDATE income_statement SET sent_at = ${bind(newSentAt)}, citizen_modified_at = ${bind(newSentAt)} WHERE id = ${bind(incomeStatement1.id)}"
                )
            }
        }

        assertEquals(
            PagedIncomeStatementsAwaitingHandler(
                listOf(
                    IncomeStatementAwaitingHandler(
                        id = incomeStatement1.id,
                        sentAt = newSentAt,
                        citizenModifiedAt = newSentAt,
                        startDate = incomeStatement1.startDate,
                        incomeEndDate = null,
                        handlerNote = "",
                        type = IncomeStatementType.HIGHEST_FEE,
                        personId = adult1.id,
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
                        sentAt = incomeStatement2.sentAt!!,
                        citizenModifiedAt = incomeStatement2.sentAt!!,
                        startDate = incomeStatement2.startDate,
                        incomeEndDate = null,
                        handlerNote = "",
                        type = IncomeStatementType.HIGHEST_FEE,
                        personId = adult2.id,
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
        val placement1Start = LocalDate.of(2022, 9, 19)
        val placement1End = LocalDate.of(2022, 11, 19)
        val placement2Start = LocalDate.of(2022, 10, 19)
        val placement2End = LocalDate.of(2023, 1, 17)
        db.transaction { tx ->
            tx.insert(
                DevParentship(
                    childId = child1.id,
                    headOfChildId = adult1.id,
                    startDate = placement1Start,
                    endDate = placement1End,
                )
            )
            tx.insert(
                DevPlacement(
                    type = PlacementType.PRESCHOOL_DAYCARE,
                    childId = child1.id,
                    unitId = daycare1.id,
                    startDate = placement1Start,
                    endDate = placement1End,
                )
            )

            tx.insert(
                DevParentship(
                    childId = child2.id,
                    headOfChildId = adult2.id,
                    startDate = placement2Start,
                    endDate = placement2End,
                )
            )
            tx.insert(
                DevPlacement(
                    type = PlacementType.PRESCHOOL_DAYCARE,
                    childId = child2.id,
                    unitId = daycare1.id,
                    startDate = placement2Start,
                    endDate = placement2End,
                )
            )

            tx.insert(DevGuardian(guardianId = adult3.id, childId = child3.id))
            tx.insert(
                DevPlacement(
                    type = PlacementType.PRESCHOOL_DAYCARE,
                    childId = child3.id,
                    unitId = daycare1.id,
                    startDate = placement2Start,
                    endDate = placement2End,
                )
            )
        }

        val incomeStatement1 = createTestIncomeStatement(adult1.id)
        val incomeStatement2 = createTestIncomeStatement(adult2.id)
        val incomeStatement3 = createTestIncomeStatement(adult3.id)

        val newSentAt = HelsinkiDateTime.of(LocalDate.of(2022, 10, 17), LocalTime.of(11, 4))

        db.transaction {
            it.execute {
                sql(
                    "UPDATE income_statement SET sent_at = ${bind(newSentAt)}, citizen_modified_at = ${bind(newSentAt)} WHERE id = ${bind(incomeStatement1.id)}"
                )
            }
        }

        assertEquals(
            PagedIncomeStatementsAwaitingHandler(
                listOf(
                    IncomeStatementAwaitingHandler(
                        id = incomeStatement1.id,
                        sentAt = newSentAt,
                        citizenModifiedAt = newSentAt,
                        startDate = incomeStatement1.startDate,
                        incomeEndDate = null,
                        handlerNote = "",
                        type = IncomeStatementType.HIGHEST_FEE,
                        personId = adult1.id,
                        personLastName = "Doe",
                        personFirstName = "John",
                        primaryCareArea = area1.name,
                    ),
                    IncomeStatementAwaitingHandler(
                        id = incomeStatement2.id,
                        sentAt = incomeStatement2.sentAt!!,
                        citizenModifiedAt = incomeStatement2.sentAt!!,
                        startDate = incomeStatement2.startDate,
                        incomeEndDate = null,
                        handlerNote = "",
                        type = IncomeStatementType.HIGHEST_FEE,
                        personId = adult2.id,
                        personLastName = "Doe",
                        personFirstName = "Joan",
                        primaryCareArea = area1.name,
                    ),
                    IncomeStatementAwaitingHandler(
                        id = incomeStatement3.id,
                        sentAt = incomeStatement3.sentAt!!,
                        citizenModifiedAt = incomeStatement3.sentAt!!,
                        startDate = incomeStatement3.startDate,
                        incomeEndDate = null,
                        handlerNote = "",
                        type = IncomeStatementType.HIGHEST_FEE,
                        personId = adult3.id,
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
                        sentAt = incomeStatement2.sentAt!!,
                        citizenModifiedAt = incomeStatement2.sentAt!!,
                        startDate = incomeStatement2.startDate,
                        incomeEndDate = null,
                        handlerNote = "",
                        type = IncomeStatementType.HIGHEST_FEE,
                        personId = adult2.id,
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
                        sentAt = newSentAt,
                        citizenModifiedAt = newSentAt,
                        startDate = incomeStatement1.startDate,
                        incomeEndDate = null,
                        handlerNote = "",
                        type = IncomeStatementType.HIGHEST_FEE,
                        personId = adult1.id,
                        personLastName = "Doe",
                        personFirstName = "John",
                        primaryCareArea = area1.name,
                    ),
                    IncomeStatementAwaitingHandler(
                        id = incomeStatement2.id,
                        sentAt = incomeStatement2.sentAt!!,
                        citizenModifiedAt = incomeStatement2.sentAt!!,
                        startDate = incomeStatement2.startDate,
                        incomeEndDate = null,
                        handlerNote = "",
                        type = IncomeStatementType.HIGHEST_FEE,
                        personId = adult2.id,
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
    fun `list income statements awaiting handler - status filter`() {
        val placementStart = today.minusDays(30)
        val placementEnd = today.plusDays(30)
        db.transaction { tx ->
            tx.insert(
                DevParentship(
                    childId = child1.id,
                    headOfChildId = adult1.id,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )
            tx.insert(
                DevPlacement(
                    type = PlacementType.PRESCHOOL_DAYCARE,
                    childId = child1.id,
                    unitId = daycare1.id,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )
            tx.insert(
                DevParentship(
                    childId = child2.id,
                    headOfChildId = adult2.id,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )
            tx.insert(
                DevPlacement(
                    type = PlacementType.PRESCHOOL_DAYCARE,
                    childId = child2.id,
                    unitId = daycare1.id,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )
            tx.insert(
                DevParentship(
                    childId = child3.id,
                    headOfChildId = adult3.id,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )
            tx.insert(
                DevPlacement(
                    type = PlacementType.PRESCHOOL_DAYCARE,
                    childId = child3.id,
                    unitId = daycare1.id,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )
        }

        val incomeStatement1 =
            createTestIncomeStatement(adult1.id, startDate = LocalDate.of(2022, 10, 12))
        val incomeStatement2 =
            createTestIncomeStatement(adult2.id, startDate = LocalDate.of(2022, 10, 13))
        val incomeStatement3 =
            createTestIncomeStatement(adult3.id, startDate = LocalDate.of(2022, 10, 14))

        setIncomeStatementHandled(
            incomeStatement1.id,
            IncomeStatementController.SetIncomeStatementHandledBody(
                handlerNote = "Handled",
                status = IncomeStatementStatus.HANDLED,
            ),
        )
        setIncomeStatementHandled(
            incomeStatement2.id,
            IncomeStatementController.SetIncomeStatementHandledBody(
                status = IncomeStatementStatus.HANDLING,
                handlerNote = "",
            ),
        )

        assertEquals(
            PagedIncomeStatementsAwaitingHandler(
                listOf(
                    IncomeStatementAwaitingHandler(
                        id = incomeStatement2.id,
                        sentAt = incomeStatement2.sentAt!!,
                        citizenModifiedAt = incomeStatement2.sentAt!!,
                        startDate = incomeStatement2.startDate,
                        incomeEndDate = null,
                        handlerNote = "",
                        type = IncomeStatementType.HIGHEST_FEE,
                        personId = adult2.id,
                        personLastName = "Doe",
                        personFirstName = "Joan",
                        primaryCareArea = area1.name,
                    ),
                    IncomeStatementAwaitingHandler(
                        id = incomeStatement3.id,
                        sentAt = incomeStatement3.sentAt!!,
                        citizenModifiedAt = incomeStatement3.sentAt!!,
                        startDate = incomeStatement3.startDate,
                        incomeEndDate = null,
                        handlerNote = "",
                        type = IncomeStatementType.HIGHEST_FEE,
                        personId = adult3.id,
                        personLastName = "Foo",
                        personFirstName = "Mark",
                        primaryCareArea = area1.name,
                    ),
                ),
                2,
                1,
            ),
            getIncomeStatementsAwaitingHandler(SearchIncomeStatementsRequest()),
        )

        assertEquals(
            PagedIncomeStatementsAwaitingHandler(
                listOf(
                    IncomeStatementAwaitingHandler(
                        id = incomeStatement3.id,
                        sentAt = incomeStatement3.sentAt!!,
                        citizenModifiedAt = incomeStatement3.sentAt!!,
                        startDate = incomeStatement3.startDate,
                        incomeEndDate = null,
                        handlerNote = "",
                        type = IncomeStatementType.HIGHEST_FEE,
                        personId = adult3.id,
                        personLastName = "Foo",
                        personFirstName = "Mark",
                        primaryCareArea = area1.name,
                    )
                ),
                1,
                1,
            ),
            getIncomeStatementsAwaitingHandler(
                SearchIncomeStatementsRequest(status = listOf(IncomeStatementStatus.SENT))
            ),
        )

        assertEquals(
            PagedIncomeStatementsAwaitingHandler(
                listOf(
                    IncomeStatementAwaitingHandler(
                        id = incomeStatement2.id,
                        sentAt = incomeStatement2.sentAt!!,
                        citizenModifiedAt = incomeStatement2.sentAt!!,
                        startDate = incomeStatement2.startDate,
                        incomeEndDate = null,
                        handlerNote = "",
                        type = IncomeStatementType.HIGHEST_FEE,
                        personId = adult2.id,
                        personLastName = "Doe",
                        personFirstName = "Joan",
                        primaryCareArea = area1.name,
                    )
                ),
                1,
                1,
            ),
            getIncomeStatementsAwaitingHandler(
                SearchIncomeStatementsRequest(status = listOf(IncomeStatementStatus.HANDLING))
            ),
        )

        assertThrows<BadRequest> {
            getIncomeStatementsAwaitingHandler(
                SearchIncomeStatementsRequest(status = listOf(IncomeStatementStatus.DRAFT))
            )
        }

        assertThrows<BadRequest> {
            getIncomeStatementsAwaitingHandler(
                SearchIncomeStatementsRequest(status = listOf(IncomeStatementStatus.HANDLED))
            )
        }

        assertThrows<BadRequest> {
            getIncomeStatementsAwaitingHandler(
                SearchIncomeStatementsRequest(
                    status = listOf(IncomeStatementStatus.SENT, IncomeStatementStatus.DRAFT)
                )
            )
        }

        assertThrows<BadRequest> {
            getIncomeStatementsAwaitingHandler(
                SearchIncomeStatementsRequest(
                    status = listOf(IncomeStatementStatus.HANDLING, IncomeStatementStatus.HANDLED)
                )
            )
        }
    }

    @Test
    fun `list income statements awaiting handler - start date sort`() {
        val placementStart = today.minusDays(30)
        val placementEnd = today.plusDays(30)
        db.transaction { tx ->
            tx.insert(
                DevParentship(
                    childId = child1.id,
                    headOfChildId = adult1.id,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )
            tx.insert(
                DevPlacement(
                    type = PlacementType.PRESCHOOL_DAYCARE,
                    childId = child1.id,
                    unitId = daycare1.id,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )

            tx.insert(
                DevParentship(
                    childId = child2.id,
                    headOfChildId = adult2.id,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )
            tx.insert(
                DevPlacement(
                    type = PlacementType.PRESCHOOL_DAYCARE,
                    childId = child2.id,
                    unitId = daycare1.id,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )
        }

        val incomeStatement1 =
            createTestIncomeStatement(adult1.id, startDate = LocalDate.of(2022, 10, 12))
        val incomeStatement2 =
            createTestIncomeStatement(adult2.id, startDate = LocalDate.of(2022, 10, 13))

        val newSentAt = HelsinkiDateTime.of(today.minusDays(2), LocalTime.of(12, 0))

        db.transaction {
            it.execute {
                sql(
                    "UPDATE income_statement SET sent_at = ${bind(newSentAt)}, citizen_modified_at = ${bind(newSentAt)}"
                )
            }
        }

        val expected1 =
            IncomeStatementAwaitingHandler(
                id = incomeStatement1.id,
                sentAt = newSentAt,
                citizenModifiedAt = newSentAt,
                startDate = incomeStatement1.startDate,
                incomeEndDate = null,
                handlerNote = "",
                type = IncomeStatementType.HIGHEST_FEE,
                personId = adult1.id,
                personLastName = "Doe",
                personFirstName = "John",
                primaryCareArea = area1.name,
            )
        val expected2 =
            IncomeStatementAwaitingHandler(
                id = incomeStatement2.id,
                sentAt = newSentAt,
                citizenModifiedAt = newSentAt,
                startDate = incomeStatement2.startDate,
                incomeEndDate = null,
                handlerNote = "",
                type = IncomeStatementType.HIGHEST_FEE,
                personId = adult2.id,
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
                    childId = child1.id,
                    headOfChildId = adult1.id,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )
            tx.insert(
                DevPlacement(
                    type = PlacementType.PRESCHOOL_DAYCARE,
                    childId = child1.id,
                    unitId = daycare1.id,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )
            tx.insert(
                DevIncome(
                    personId = adult1.id,
                    modifiedBy = AuthenticatedUser.SystemInternalUser.evakaUserId,
                    validFrom = incomeRange1.start,
                    validTo = incomeRange1.end,
                )
            )

            tx.insert(
                DevParentship(
                    childId = child2.id,
                    headOfChildId = adult2.id,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )
            tx.insert(
                DevPlacement(
                    type = PlacementType.PRESCHOOL_DAYCARE,
                    childId = child2.id,
                    unitId = daycare1.id,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )
            tx.insert(
                DevIncome(
                    personId = adult2.id,
                    modifiedBy = AuthenticatedUser.SystemInternalUser.evakaUserId,
                    validFrom = incomeRange2.start,
                    validTo = incomeRange2.end,
                )
            )
        }

        val incomeStatement1 =
            createTestIncomeStatement(adult1.id, startDate = LocalDate.of(2022, 10, 12))
        val incomeStatement2 =
            createTestIncomeStatement(adult2.id, startDate = LocalDate.of(2022, 10, 13))

        val expected1 =
            IncomeStatementAwaitingHandler(
                id = incomeStatement1.id,
                sentAt = incomeStatement1.sentAt!!,
                citizenModifiedAt = incomeStatement1.sentAt!!,
                startDate = incomeStatement1.startDate,
                incomeEndDate = incomeRange1.end,
                handlerNote = "",
                type = IncomeStatementType.HIGHEST_FEE,
                personId = adult1.id,
                personLastName = "Doe",
                personFirstName = "John",
                primaryCareArea = area1.name,
            )
        val expected2 =
            IncomeStatementAwaitingHandler(
                id = incomeStatement2.id,
                sentAt = incomeStatement2.sentAt!!,
                citizenModifiedAt = incomeStatement2.sentAt!!,
                startDate = incomeStatement2.startDate,
                incomeEndDate = incomeRange2.end,
                handlerNote = "",
                type = IncomeStatementType.HIGHEST_FEE,
                personId = adult2.id,
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
                    childId = child1.id,
                    headOfChildId = adult1.id,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )
            tx.insert(
                DevPlacement(
                    type = PlacementType.DAYCARE,
                    childId = child1.id,
                    unitId = daycare1.id,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )

            tx.insert(
                DevParentship(
                    childId = child2.id,
                    headOfChildId = adult2.id,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )
            tx.insert(
                DevPlacement(
                    type = PlacementType.DAYCARE,
                    childId = child2.id,
                    unitId = daycare1.id,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )
        }

        val incomeStatement1 =
            createTestIncomeStatement(adult1.id, startDate = LocalDate.of(2022, 10, 12))
        val incomeStatement2 =
            createChildTestIncomeStatement(child2.id, startDate = LocalDate.of(2022, 10, 13))

        val expected1 =
            IncomeStatementAwaitingHandler(
                id = incomeStatement1.id,
                sentAt = incomeStatement1.sentAt!!,
                citizenModifiedAt = incomeStatement1.sentAt!!,
                startDate = incomeStatement1.startDate,
                incomeEndDate = null,
                handlerNote = "",
                type = IncomeStatementType.HIGHEST_FEE,
                personId = adult1.id,
                personLastName = "Doe",
                personFirstName = "John",
                primaryCareArea = area1.name,
            )
        val expected2 =
            IncomeStatementAwaitingHandler(
                id = incomeStatement2.id,
                sentAt = incomeStatement2.sentAt!!,
                citizenModifiedAt = incomeStatement2.sentAt!!,
                startDate = incomeStatement2.startDate,
                incomeEndDate = null,
                handlerNote = "",
                type = IncomeStatementType.CHILD_INCOME,
                personId = child2.id,
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
                    childId = child1.id,
                    headOfChildId = adult1.id,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )
            tx.insert(
                DevPlacement(
                    type = PlacementType.DAYCARE,
                    childId = child1.id,
                    unitId = daycare1.id,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )

            tx.insert(
                DevParentship(
                    childId = child2.id,
                    headOfChildId = adult2.id,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )
            tx.insert(
                DevPlacement(
                    type = PlacementType.DAYCARE,
                    childId = child2.id,
                    unitId = daycare1.id,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )
        }

        val incomeStatement1 =
            createTestIncomeStatement(adult1.id, startDate = LocalDate.of(2022, 10, 12))
        setIncomeStatementHandled(
            incomeStatement1.id,
            IncomeStatementController.SetIncomeStatementHandledBody(
                status = IncomeStatementStatus.SENT,
                handlerNote = "a",
            ),
        )
        val incomeStatement2 =
            createTestIncomeStatement(adult2.id, startDate = LocalDate.of(2022, 10, 13))
        setIncomeStatementHandled(
            incomeStatement2.id,
            IncomeStatementController.SetIncomeStatementHandledBody(
                status = IncomeStatementStatus.SENT,
                handlerNote = "b",
            ),
        )

        val expected1 =
            IncomeStatementAwaitingHandler(
                id = incomeStatement1.id,
                sentAt = incomeStatement1.sentAt!!,
                citizenModifiedAt = incomeStatement1.sentAt!!,
                startDate = incomeStatement1.startDate,
                incomeEndDate = null,
                handlerNote = "a",
                type = IncomeStatementType.HIGHEST_FEE,
                personId = adult1.id,
                personLastName = "Doe",
                personFirstName = "John",
                primaryCareArea = area1.name,
            )
        val expected2 =
            IncomeStatementAwaitingHandler(
                id = incomeStatement2.id,
                sentAt = incomeStatement2.sentAt!!,
                citizenModifiedAt = incomeStatement2.sentAt!!,
                startDate = incomeStatement2.startDate,
                incomeEndDate = null,
                handlerNote = "b",
                type = IncomeStatementType.HIGHEST_FEE,
                personId = adult2.id,
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
                    childId = child1.id,
                    headOfChildId = adult1.id,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )
            tx.insert(
                DevPlacement(
                    type = PlacementType.DAYCARE,
                    childId = child1.id,
                    unitId = daycare1.id,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )

            tx.insert(
                DevParentship(
                    childId = child2.id,
                    headOfChildId = adult2.id,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )
            tx.insert(
                DevPlacement(
                    type = PlacementType.DAYCARE,
                    childId = child2.id,
                    unitId = daycare1.id,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )
        }

        val incomeStatement1 =
            createTestIncomeStatement(adult1.id, startDate = LocalDate.of(2022, 10, 12))
        val incomeStatement2 =
            createTestIncomeStatement(adult2.id, startDate = LocalDate.of(2022, 10, 13))

        val expected1 =
            IncomeStatementAwaitingHandler(
                id = incomeStatement1.id,
                sentAt = incomeStatement1.sentAt!!,
                citizenModifiedAt = incomeStatement1.sentAt!!,
                startDate = incomeStatement1.startDate,
                incomeEndDate = null,
                handlerNote = "",
                type = IncomeStatementType.HIGHEST_FEE,
                personId = adult1.id,
                personLastName = "Doe",
                personFirstName = "John",
                primaryCareArea = area1.name,
            )
        val expected2 =
            IncomeStatementAwaitingHandler(
                id = incomeStatement2.id,
                sentAt = incomeStatement2.sentAt!!,
                citizenModifiedAt = incomeStatement2.sentAt!!,
                startDate = incomeStatement2.startDate,
                incomeEndDate = null,
                handlerNote = "",
                type = IncomeStatementType.HIGHEST_FEE,
                personId = adult2.id,
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

    @Test
    fun `list income statements awaiting handler - citizen modified at sort`() {
        val placementStart = today.minusDays(30)
        val placementEnd = today.plusDays(30)
        db.transaction { tx ->
            tx.insert(
                DevParentship(
                    childId = child1.id,
                    headOfChildId = adult1.id,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )
            tx.insert(
                DevPlacement(
                    type = PlacementType.DAYCARE,
                    childId = child1.id,
                    unitId = daycare1.id,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )

            tx.insert(
                DevParentship(
                    childId = child2.id,
                    headOfChildId = adult2.id,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )
            tx.insert(
                DevPlacement(
                    type = PlacementType.DAYCARE,
                    childId = child2.id,
                    unitId = daycare1.id,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )
        }

        val body = IncomeStatementBody.HighestFee(startDate = today, endDate = null)
        val userId1 = adult1.id.raw.let(::EvakaUserId)
        val userId2 = adult2.id.raw.let(::EvakaUserId)

        val sentAt = HelsinkiDateTime.of(today.minusDays(2), LocalTime.of(12, 0))
        val id1 = db.transaction { tx ->
            tx.insertIncomeStatement(userId1, sentAt.minusHours(1), adult1.id, body, draft = true)
        }
        val id2 = db.transaction { tx ->
            tx.insertIncomeStatement(userId2, sentAt.minusHours(1), adult2.id, body, draft = true)
        }

        db.transaction { tx ->
            tx.updateIncomeStatement(userId1, sentAt, id1, body, draft = false)
            tx.updateIncomeStatement(userId2, sentAt, id2, body, draft = false)
        }

        val citizenUpdateTime = HelsinkiDateTime.of(today, LocalTime.of(10, 0))
        db.transaction { tx ->
            tx.updateIncomeStatementOtherInfo(id2, userId2, citizenUpdateTime, "updated info")
        }

        val expected1 =
            IncomeStatementAwaitingHandler(
                id = id1,
                sentAt = sentAt,
                citizenModifiedAt = sentAt,
                startDate = today,
                incomeEndDate = null,
                handlerNote = "",
                type = IncomeStatementType.HIGHEST_FEE,
                personId = adult1.id,
                personLastName = "Doe",
                personFirstName = "John",
                primaryCareArea = area1.name,
            )
        val expected2 =
            IncomeStatementAwaitingHandler(
                id = id2,
                sentAt = sentAt,
                citizenModifiedAt = citizenUpdateTime,
                startDate = today,
                incomeEndDate = null,
                handlerNote = "",
                type = IncomeStatementType.HIGHEST_FEE,
                personId = adult2.id,
                personLastName = "Doe",
                personFirstName = "Joan",
                primaryCareArea = area1.name,
            )

        val result =
            getIncomeStatementsAwaitingHandler(
                SearchIncomeStatementsRequest(
                    sortBy = IncomeStatementSortParam.CITIZEN_MODIFIED_AT,
                    sortDirection = SortDirection.ASC,
                )
            )
        assertEquals(
            PagedIncomeStatementsAwaitingHandler(listOf(expected1, expected2), 2, 1),
            result,
        )

        val updatedRow = result.data[1]
        assertTrue(updatedRow.citizenModifiedAt > updatedRow.sentAt)
        val unmodifiedRow = result.data[0]
        assertEquals(unmodifiedRow.sentAt, unmodifiedRow.citizenModifiedAt)

        assertEquals(
            PagedIncomeStatementsAwaitingHandler(listOf(expected2, expected1), 2, 1),
            getIncomeStatementsAwaitingHandler(
                SearchIncomeStatementsRequest(
                    sortBy = IncomeStatementSortParam.CITIZEN_MODIFIED_AT,
                    sortDirection = SortDirection.DESC,
                )
            ),
        )
    }

    @Test
    fun `citizen modified at diverges from sent at after citizen update`() {
        val placementStart = today.minusDays(30)
        val placementEnd = today.plusDays(30)
        db.transaction { tx ->
            tx.insert(
                DevParentship(
                    childId = child1.id,
                    headOfChildId = adult1.id,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )
            tx.insert(
                DevPlacement(
                    type = PlacementType.DAYCARE,
                    childId = child1.id,
                    unitId = daycare1.id,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )
        }

        val incomeStatement = createTestIncomeStatement(adult1.id)

        val updateClock = MockEvakaClock(now.plusHours(2))
        db.transaction { tx ->
            tx.updateIncomeStatementOtherInfo(
                incomeStatement.id,
                adult1.id.raw.let(::EvakaUserId),
                updateClock.now(),
                "updated info",
            )
        }

        val result = getIncomeStatementsAwaitingHandler()
        assertEquals(1, result.data.size)
        val row = result.data[0]
        assertEquals(incomeStatement.sentAt, row.sentAt)
        assertEquals(updateClock.now(), row.citizenModifiedAt)
        assertTrue(row.citizenModifiedAt > row.sentAt)
    }

    private fun getIncomeStatement(id: IncomeStatementId): IncomeStatement {
        return incomeStatementController.getIncomeStatement(
            dbInstance(),
            employee.user,
            MockEvakaClock(now),
            id,
        )
    }

    private fun getIncomeStatements(personId: PersonId): PagedIncomeStatements {
        return incomeStatementController.getIncomeStatements(
            dbInstance(),
            employee.user,
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
            employee.user,
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
            employee.user,
            clock,
            body,
        )
    }

    private fun uploadAttachment(id: IncomeStatementId): AttachmentId {
        return attachmentsController.uploadIncomeStatementAttachmentEmployee(
            dbInstance(),
            employee.user,
            MockEvakaClock(now),
            id,
            IncomeStatementAttachmentType.OTHER,
            MockMultipartFile("file", "evaka-logo.png", "image/png", pngFile.readBytes()),
        )
    }
}
