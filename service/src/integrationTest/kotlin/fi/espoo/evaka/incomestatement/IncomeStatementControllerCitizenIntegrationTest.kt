// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.incomestatement

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.attachment.AttachmentsController
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.pis.service.insertGuardian
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.AttachmentId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.IncomeStatementId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.CitizenAuthLevel
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.Forbidden
import fi.espoo.evaka.shared.domain.RealEvakaClock
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testAdult_2
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testDaycare
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.mock.web.MockMultipartFile

class IncomeStatementControllerCitizenIntegrationTest :
    FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired
    private lateinit var incomeStatementControllerCitizen: IncomeStatementControllerCitizen
    @Autowired private lateinit var attachmentsController: AttachmentsController

    private val citizen = AuthenticatedUser.Citizen(testAdult_1.id, CitizenAuthLevel.STRONG)

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx -> tx.insertGeneralTestFixtures() }
    }

    @Test
    fun `create a highest fee income statement`() {
        createIncomeStatement(
            IncomeStatementBody.HighestFee(startDate = LocalDate.of(2021, 4, 3), endDate = null)
        )

        val incomeStatements = getIncomeStatements().data
        assertEquals(
            listOf(
                IncomeStatement.HighestFee(
                    id = incomeStatements[0].id,
                    personId = testAdult_1.id,
                    firstName = testAdult_1.firstName,
                    lastName = testAdult_1.lastName,
                    startDate = LocalDate.of(2021, 4, 3),
                    endDate = null,
                    created = incomeStatements[0].created,
                    updated = incomeStatements[0].updated,
                    handled = false,
                    handlerNote = ""
                )
            ),
            incomeStatements
        )
    }

    @Test
    fun `create an income statement`() {
        createIncomeStatement(
            IncomeStatementBody.Income(
                startDate = LocalDate.of(2021, 4, 3),
                endDate = LocalDate.of(2021, 8, 9),
                gross =
                    Gross(
                        incomeSource = IncomeSource.INCOMES_REGISTER,
                        estimatedMonthlyIncome = 500,
                        otherIncome = setOf(OtherIncome.ALIMONY, OtherIncome.RENTAL_INCOME),
                        otherIncomeInfo = "Elatusmaksut 100, vuoratulot 150"
                    ),
                entrepreneur =
                    Entrepreneur(
                        fullTime = true,
                        startOfEntrepreneurship = LocalDate.of(1998, 1, 1),
                        spouseWorksInCompany = false,
                        startupGrant = true,
                        checkupConsent = true,
                        selfEmployed =
                            SelfEmployed(
                                attachments = true,
                                estimatedIncome =
                                    EstimatedIncome(
                                        estimatedMonthlyIncome = 1000,
                                        incomeStartDate = LocalDate.of(2005, 6, 6),
                                        incomeEndDate = LocalDate.of(2021, 7, 7)
                                    )
                            ),
                        limitedCompany = LimitedCompany(IncomeSource.INCOMES_REGISTER),
                        partnership = false,
                        lightEntrepreneur = true,
                        accountant =
                            Accountant(
                                name = "Foo",
                                address = "Bar",
                                phone = "123",
                                email = "foo.bar@example.com"
                            )
                    ),
                student = false,
                alimonyPayer = true,
                otherInfo = "foo bar",
                attachmentIds = listOf()
            )
        )

        val incomeStatements = getIncomeStatements().data
        assertEquals(
            listOf(
                IncomeStatement.Income(
                    id = incomeStatements[0].id,
                    personId = testAdult_1.id,
                    firstName = testAdult_1.firstName,
                    lastName = testAdult_1.lastName,
                    startDate = LocalDate.of(2021, 4, 3),
                    endDate = LocalDate.of(2021, 8, 9),
                    gross =
                        Gross(
                            incomeSource = IncomeSource.INCOMES_REGISTER,
                            estimatedMonthlyIncome = 500,
                            otherIncome = setOf(OtherIncome.ALIMONY, OtherIncome.RENTAL_INCOME),
                            otherIncomeInfo = "Elatusmaksut 100, vuoratulot 150"
                        ),
                    entrepreneur =
                        Entrepreneur(
                            fullTime = true,
                            startOfEntrepreneurship = LocalDate.of(1998, 1, 1),
                            spouseWorksInCompany = false,
                            startupGrant = true,
                            checkupConsent = true,
                            selfEmployed =
                                SelfEmployed(
                                    attachments = true,
                                    estimatedIncome =
                                        EstimatedIncome(
                                            estimatedMonthlyIncome = 1000,
                                            incomeStartDate = LocalDate.of(2005, 6, 6),
                                            incomeEndDate = LocalDate.of(2021, 7, 7)
                                        )
                                ),
                            limitedCompany = LimitedCompany(IncomeSource.INCOMES_REGISTER),
                            partnership = false,
                            lightEntrepreneur = true,
                            accountant =
                                Accountant(
                                    name = "Foo",
                                    address = "Bar",
                                    phone = "123",
                                    email = "foo.bar@example.com"
                                )
                        ),
                    student = false,
                    alimonyPayer = true,
                    otherInfo = "foo bar",
                    created = incomeStatements[0].created,
                    updated = incomeStatements[0].updated,
                    handled = false,
                    handlerNote = "",
                    attachments = listOf()
                )
            ),
            incomeStatements
        )
    }

    @Test
    fun `create an income statement for non custodian child fails`() {
        assertThrows<Forbidden> {
            createIncomeStatementForChild(
                IncomeStatementBody.ChildIncome(
                    startDate = LocalDate.of(2021, 4, 3),
                    endDate = LocalDate.of(2021, 8, 9),
                    otherInfo = "foo bar",
                    attachmentIds = listOf()
                ),
                testChild_1.id,
            )
        }
    }

    @Test
    fun `create an income statement for child`() {
        db.transaction { it.insertGuardian(testAdult_1.id, testChild_1.id) }

        createIncomeStatementForChild(
            IncomeStatementBody.ChildIncome(
                startDate = LocalDate.of(2021, 4, 3),
                endDate = LocalDate.of(2021, 8, 9),
                otherInfo = "foo bar",
                attachmentIds = listOf()
            ),
            testChild_1.id
        )

        val incomeStatements = getIncomeStatementsForChild(testChild_1.id).data
        assertEquals(
            listOf(
                IncomeStatement.ChildIncome(
                    id = incomeStatements[0].id,
                    personId = testChild_1.id,
                    firstName = testChild_1.firstName,
                    lastName = testChild_1.lastName,
                    startDate = LocalDate.of(2021, 4, 3),
                    endDate = LocalDate.of(2021, 8, 9),
                    otherInfo = "foo bar",
                    created = incomeStatements[0].created,
                    updated = incomeStatements[0].updated,
                    handled = false,
                    handlerNote = "",
                    attachments = listOf()
                )
            ),
            incomeStatements
        )
    }

    @Test
    fun `create an invalid income statement`() {
        assertThrows<BadRequest> {
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
            )
        }
        assertThrows<BadRequest> {
            createIncomeStatement(
                IncomeStatementBody.Income(
                    // endDate is before startDate
                    startDate = LocalDate.of(2021, 4, 3),
                    endDate = LocalDate.of(2021, 4, 2),
                    gross =
                        Gross(
                            incomeSource = IncomeSource.INCOMES_REGISTER,
                            estimatedMonthlyIncome = 1500,
                            otherIncome = setOf(),
                            otherIncomeInfo = ""
                        ),
                    entrepreneur = null,
                    student = false,
                    alimonyPayer = true,
                    otherInfo = "foo bar",
                    attachmentIds = listOf()
                ),
            )
        }
        assertThrows<BadRequest> {
            createIncomeStatement(
                IncomeStatementBody.Income(
                    startDate = LocalDate.of(2021, 4, 3),
                    endDate = null,
                    gross = null,
                    // At least one company type is needed
                    entrepreneur =
                        Entrepreneur(
                            fullTime = true,
                            startOfEntrepreneurship = LocalDate.of(2000, 1, 1),
                            spouseWorksInCompany = true,
                            startupGrant = true,
                            checkupConsent = true,
                            selfEmployed = null,
                            limitedCompany = null,
                            partnership = false,
                            lightEntrepreneur = false,
                            accountant =
                                Accountant(
                                    name = "Foo",
                                    address = "Bar",
                                    phone = "123",
                                    email = "foo.bar@example.com"
                                )
                        ),
                    student = false,
                    alimonyPayer = false,
                    otherInfo = "foo bar",
                    attachmentIds = listOf()
                ),
            )
        }
        assertThrows<BadRequest> {
            createIncomeStatement(
                IncomeStatementBody.Income(
                    startDate = LocalDate.of(2021, 4, 3),
                    endDate = null,
                    gross = null,
                    entrepreneur =
                        Entrepreneur(
                            fullTime = true,
                            startOfEntrepreneurship = LocalDate.of(2000, 1, 1),
                            spouseWorksInCompany = true,
                            startupGrant = true,
                            checkupConsent = true,
                            selfEmployed = null,
                            limitedCompany = null,
                            partnership = true,
                            lightEntrepreneur = false,
                            // Accountant is required if limitedCompany or partnership is given
                            accountant = null
                        ),
                    student = false,
                    alimonyPayer = false,
                    otherInfo = "foo bar",
                    attachmentIds = listOf()
                ),
            )
        }
        assertThrows<BadRequest> {
            createIncomeStatement(
                IncomeStatementBody.Income(
                    startDate = LocalDate.of(2021, 4, 3),
                    endDate = null,
                    gross = null,
                    entrepreneur =
                        Entrepreneur(
                            fullTime = true,
                            startOfEntrepreneurship = LocalDate.of(2000, 1, 1),
                            spouseWorksInCompany = true,
                            startupGrant = true,
                            checkupConsent = true,
                            selfEmployed = null,
                            limitedCompany = null,
                            partnership = true,
                            lightEntrepreneur = false,
                            // Accountant name, phone or email cannot be empty
                            accountant = Accountant(name = "", address = "", phone = "", email = "")
                        ),
                    student = false,
                    alimonyPayer = false,
                    otherInfo = "foo bar",
                    attachmentIds = listOf()
                ),
            )
        }
    }

    @Test
    fun `create an income statement with an attachment`() {
        val attachmentId = uploadAttachment()

        createIncomeStatement(
            IncomeStatementBody.Income(
                startDate = LocalDate.of(2021, 4, 3),
                endDate = null,
                gross =
                    Gross(
                        incomeSource = IncomeSource.ATTACHMENTS,
                        estimatedMonthlyIncome = 1500,
                        otherIncome = setOf(),
                        otherIncomeInfo = ""
                    ),
                entrepreneur = null,
                student = false,
                alimonyPayer = true,
                otherInfo = "foo bar",
                attachmentIds = listOf(attachmentId)
            )
        )

        val incomeStatements = getIncomeStatements().data
        assertEquals(
            listOf(
                IncomeStatement.Income(
                    id = incomeStatements[0].id,
                    personId = testAdult_1.id,
                    firstName = testAdult_1.firstName,
                    lastName = testAdult_1.lastName,
                    startDate = LocalDate.of(2021, 4, 3),
                    endDate = null,
                    gross =
                        Gross(
                            incomeSource = IncomeSource.ATTACHMENTS,
                            estimatedMonthlyIncome = 1500,
                            otherIncome = setOf(),
                            otherIncomeInfo = ""
                        ),
                    entrepreneur = null,
                    student = false,
                    alimonyPayer = true,
                    otherInfo = "foo bar",
                    created = incomeStatements[0].created,
                    updated = incomeStatements[0].updated,
                    handled = false,
                    handlerNote = "",
                    attachments = listOf(idToAttachment(attachmentId))
                )
            ),
            incomeStatements
        )
    }

    @Test
    fun `create an income statement for a child with an attachment`() {
        db.transaction { it.insertGuardian(testAdult_1.id, testChild_1.id) }
        val attachmentId = uploadAttachment()

        createIncomeStatementForChild(
            IncomeStatementBody.ChildIncome(
                startDate = LocalDate.of(2021, 4, 3),
                endDate = null,
                otherInfo = "foo bar",
                attachmentIds = listOf(attachmentId)
            ),
            testChild_1.id
        )

        val incomeStatements = getIncomeStatementsForChild(testChild_1.id).data
        assertEquals(
            listOf(
                IncomeStatement.ChildIncome(
                    id = incomeStatements[0].id,
                    personId = testChild_1.id,
                    firstName = testChild_1.firstName,
                    lastName = testChild_1.lastName,
                    startDate = LocalDate.of(2021, 4, 3),
                    endDate = null,
                    otherInfo = "foo bar",
                    created = incomeStatements[0].created,
                    updated = incomeStatements[0].updated,
                    handled = false,
                    handlerNote = "",
                    attachments = listOf(idToAttachment(attachmentId))
                )
            ),
            incomeStatements
        )
    }

    @Test
    fun `create an income statement with an attachment that does not exist`() {
        val nonExistingAttachmentId = AttachmentId(UUID.randomUUID())

        assertThrows<BadRequest> {
            createIncomeStatement(
                IncomeStatementBody.Income(
                    startDate = LocalDate.of(2021, 4, 3),
                    endDate = null,
                    gross =
                        Gross(
                            incomeSource = IncomeSource.ATTACHMENTS,
                            estimatedMonthlyIncome = 1500,
                            otherIncome = setOf(),
                            otherIncomeInfo = ""
                        ),
                    entrepreneur = null,
                    student = false,
                    alimonyPayer = false,
                    otherInfo = "foo bar",
                    attachmentIds = listOf(nonExistingAttachmentId)
                ),
            )
        }
    }

    @Test
    fun `create an income statement with someone else's attachment`() {
        val someoneElse = AuthenticatedUser.Citizen(testAdult_2.id, CitizenAuthLevel.STRONG)
        val attachmentId = uploadAttachment(someoneElse)

        assertThrows<BadRequest> {
            createIncomeStatement(
                IncomeStatementBody.Income(
                    startDate = LocalDate.of(2021, 4, 3),
                    endDate = null,
                    gross =
                        Gross(
                            incomeSource = IncomeSource.ATTACHMENTS,
                            estimatedMonthlyIncome = 1500,
                            otherIncome = setOf(),
                            otherIncomeInfo = ""
                        ),
                    entrepreneur = null,
                    student = false,
                    alimonyPayer = false,
                    otherInfo = "foo bar",
                    attachmentIds = listOf(attachmentId)
                ),
            )
        }
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
                gross =
                    Gross(
                        incomeSource = IncomeSource.INCOMES_REGISTER,
                        estimatedMonthlyIncome = 500,
                        otherIncome = setOf(OtherIncome.ALIMONY, OtherIncome.RENTAL_INCOME),
                        otherIncomeInfo = "Elatusmaksut 100, vuokratulot 150"
                    ),
                entrepreneur =
                    Entrepreneur(
                        fullTime = true,
                        startOfEntrepreneurship = LocalDate.of(1998, 1, 1),
                        spouseWorksInCompany = false,
                        startupGrant = true,
                        checkupConsent = true,
                        selfEmployed =
                            SelfEmployed(
                                attachments = true,
                                estimatedIncome =
                                    EstimatedIncome(
                                        estimatedMonthlyIncome = 1000,
                                        incomeStartDate = LocalDate.of(2005, 6, 6),
                                        incomeEndDate = LocalDate.of(2021, 7, 7)
                                    )
                            ),
                        limitedCompany =
                            LimitedCompany(incomeSource = IncomeSource.INCOMES_REGISTER),
                        partnership = false,
                        lightEntrepreneur = false,
                        Accountant(
                            name = "Foo",
                            address = "Bar",
                            phone = "123",
                            email = "foo.bar@example.com"
                        )
                    ),
                student = false,
                alimonyPayer = true,
                otherInfo = "foo bar",
                attachmentIds = listOf(attachment1)
            )
        )

        val incomeStatement = getIncomeStatements().data[0]

        updateIncomeStatement(
            incomeStatement.id,
            IncomeStatementBody.Income(
                startDate = LocalDate.of(2021, 6, 11),
                endDate = null,
                gross = null,
                entrepreneur =
                    Entrepreneur(
                        fullTime = false,
                        startOfEntrepreneurship = LocalDate.of(2019, 1, 1),
                        spouseWorksInCompany = true,
                        startupGrant = false,
                        checkupConsent = false,
                        selfEmployed = null,
                        limitedCompany = null,
                        partnership = true,
                        lightEntrepreneur = false,
                        Accountant(
                            name = "Baz",
                            address = "Quux",
                            phone = "456",
                            email = "baz.quux@example.com"
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
                personId = testAdult_1.id,
                firstName = testAdult_1.firstName,
                lastName = testAdult_1.lastName,
                startDate = LocalDate.of(2021, 6, 11),
                endDate = null,
                gross = null,
                entrepreneur =
                    Entrepreneur(
                        fullTime = false,
                        startOfEntrepreneurship = LocalDate.of(2019, 1, 1),
                        spouseWorksInCompany = true,
                        startupGrant = false,
                        checkupConsent = false,
                        selfEmployed = null,
                        limitedCompany = null,
                        partnership = true,
                        lightEntrepreneur = false,
                        Accountant(
                            name = "Baz",
                            address = "Quux",
                            phone = "456",
                            email = "baz.quux@example.com"
                        )
                    ),
                student = true,
                alimonyPayer = false,
                otherInfo = "",
                created = incomeStatement.created,
                updated = updated,
                handled = false,
                handlerNote = "",
                attachments = listOf(idToAttachment(attachment2), idToAttachment(attachment3))
            ),
            getIncomeStatement(incomeStatement.id)
        )
    }

    @Test
    fun `cannot update a handled income statement`() {
        createIncomeStatement(
            IncomeStatementBody.HighestFee(startDate = LocalDate.of(2021, 4, 3), endDate = null)
        )
        val id = getIncomeStatements().data.first().id

        markIncomeStatementHandled(id, "foooooo")

        assertThrows<Forbidden> {
            updateIncomeStatement(
                id,
                IncomeStatementBody.HighestFee(
                    startDate = LocalDate.of(2030, 4, 3),
                    endDate = null
                ),
            )
        }
    }

    @Test
    fun `cannot see handler note or remove a handled income statement`() {
        createIncomeStatement(
            IncomeStatementBody.HighestFee(startDate = LocalDate.of(2021, 4, 3), endDate = null)
        )
        val incomeStatement = getIncomeStatements().data.first()
        assertEquals("", incomeStatement.handlerNote)

        markIncomeStatementHandled(incomeStatement.id, "foo bar")

        val handled = getIncomeStatements().data.first()
        assertEquals(true, handled.handled)
        assertEquals("", handled.handlerNote)

        assertThrows<Forbidden> { deleteIncomeStatement(incomeStatement.id) }
    }

    @Test
    fun `paging works`() {
        createIncomeStatement(
            IncomeStatementBody.HighestFee(
                startDate = LocalDate.of(2020, 4, 3),
                endDate = LocalDate.of(2020, 12, 31)
            )
        )
        createIncomeStatement(
            IncomeStatementBody.HighestFee(startDate = LocalDate.of(2021, 1, 1), endDate = null)
        )

        val result = getIncomeStatements()
        assertEquals(2, result.total)
        assertEquals(1, result.pages)
        assertNull(result.data.first().endDate)
        assertEquals(LocalDate.of(2020, 12, 31), result.data.last().endDate)

        val paged = getIncomeStatements(pageSize = 1)
        assertEquals(2, paged.total)
        assertEquals(2, paged.pages)
        assertEquals(1, paged.data.size)
        assertNull(paged.data.first().endDate)
    }

    @Test
    fun `cannot create two income statements with the same startDate`() {
        createIncomeStatement(
            IncomeStatementBody.HighestFee(startDate = LocalDate.of(2021, 4, 3), endDate = null)
        )
        assertThrows<BadRequest> {
            createIncomeStatement(
                IncomeStatementBody.HighestFee(
                    startDate = LocalDate.of(2021, 4, 3),
                    endDate = null
                ),
            )
        }
    }

    private fun markIncomeStatementHandled(id: IncomeStatementId, note: String) =
        db.transaction { tx ->
            @Suppress("DEPRECATION")
            tx.createUpdate(
                    """
            UPDATE income_statement
            SET handler_id = (SELECT id FROM employee LIMIT 1), handler_note = :note
            WHERE id = :id
            """
                        .trimIndent()
                )
                .bind("id", id)
                .bind("note", note)
                .execute()
        }

    @Test
    fun `employee attachments are not visible to citizen`() {
        val attachment1 = uploadAttachment()

        val employeeId = EmployeeId(UUID.randomUUID())
        val employee = AuthenticatedUser.Employee(employeeId, setOf(UserRole.FINANCE_ADMIN))
        db.transaction {
            it.insert(DevEmployee(id = employeeId, roles = setOf(UserRole.FINANCE_ADMIN)))
        }

        createIncomeStatement(
            IncomeStatementBody.Income(
                startDate = LocalDate.of(2021, 4, 3),
                endDate = LocalDate.of(2021, 8, 9),
                gross =
                    Gross(
                        incomeSource = IncomeSource.INCOMES_REGISTER,
                        estimatedMonthlyIncome = 500,
                        otherIncome = setOf(OtherIncome.ALIMONY, OtherIncome.RENTAL_INCOME),
                        otherIncomeInfo = "Elatusmaksut 100, vuokratulot 150"
                    ),
                entrepreneur =
                    Entrepreneur(
                        fullTime = true,
                        startOfEntrepreneurship = LocalDate.of(1998, 1, 1),
                        spouseWorksInCompany = false,
                        startupGrant = true,
                        checkupConsent = true,
                        selfEmployed =
                            SelfEmployed(
                                attachments = true,
                                estimatedIncome =
                                    EstimatedIncome(
                                        estimatedMonthlyIncome = 1000,
                                        incomeStartDate = LocalDate.of(2005, 6, 6),
                                        incomeEndDate = LocalDate.of(2021, 7, 7)
                                    )
                            ),
                        limitedCompany =
                            LimitedCompany(incomeSource = IncomeSource.INCOMES_REGISTER),
                        partnership = false,
                        lightEntrepreneur = false,
                        accountant =
                            Accountant(
                                name = "Foo",
                                address = "Bar",
                                phone = "123",
                                email = "foo.bar@example.com"
                            )
                    ),
                student = false,
                alimonyPayer = true,
                otherInfo = "foo bar",
                attachmentIds = listOf(attachment1)
            )
        )

        val incomeStatementId = getIncomeStatements().data[0].id

        uploadAttachmentAsEmployee(employee, incomeStatementId)

        val incomeStatement = getIncomeStatement(incomeStatementId)
        assertEquals(
            listOf(idToAttachment(attachment1)),
            when (incomeStatement) {
                is IncomeStatement.Income -> incomeStatement.attachments
                else -> throw Error("No attachments")
            }
        )
    }

    @Test
    fun `guardian does not see children without an active billable placement`() {
        assertEquals(getIncomeStatementChildren().size, 0)
    }

    @Test
    fun `guardian sees children with an active billable placement`() {
        db.transaction {
            it.insertGuardian(testAdult_1.id, testChild_1.id)

            it.insert(
                DevPlacement(
                    childId = testChild_1.id,
                    startDate = LocalDate.now(),
                    endDate = LocalDate.now(),
                    type = PlacementType.DAYCARE,
                    unitId = testDaycare.id
                )
            )
        }

        assertEquals(getIncomeStatementChildren().size, 1)
    }

    @Test
    fun `guardian does not see children with an inactive billable placement`() {
        db.transaction {
            it.insertGuardian(testAdult_1.id, testChild_1.id)

            it.insert(
                DevPlacement(
                    childId = testChild_1.id,
                    startDate = LocalDate.now().minusWeeks(1),
                    endDate = LocalDate.now().minusWeeks(1),
                    type = PlacementType.DAYCARE,
                    unitId = testDaycare.id
                )
            )
        }

        assertEquals(getIncomeStatementChildren().size, 0)
    }

    private fun getIncomeStatements(pageSize: Int = 10, page: Int = 1): PagedIncomeStatements {
        return incomeStatementControllerCitizen.getIncomeStatements(
            dbInstance(),
            citizen,
            RealEvakaClock(),
            page = page,
            pageSize = pageSize
        )
    }

    private fun getIncomeStatementsForChild(
        childId: ChildId,
        pageSize: Int = 10,
        page: Int = 1
    ): PagedIncomeStatements {
        return incomeStatementControllerCitizen.getChildIncomeStatements(
            dbInstance(),
            citizen,
            RealEvakaClock(),
            childId,
            page = page,
            pageSize = pageSize
        )
    }

    private fun getIncomeStatement(id: IncomeStatementId): IncomeStatement {
        return incomeStatementControllerCitizen.getIncomeStatement(
            dbInstance(),
            citizen,
            RealEvakaClock(),
            id
        )
    }

    private fun getIncomeStatementChildren(): List<ChildBasicInfo> {
        return incomeStatementControllerCitizen.getIncomeStatementChildren(
            dbInstance(),
            citizen,
            RealEvakaClock(),
        )
    }

    private fun createIncomeStatement(body: IncomeStatementBody) {
        incomeStatementControllerCitizen.createIncomeStatement(
            dbInstance(),
            citizen,
            RealEvakaClock(),
            body,
        )
    }

    private fun createIncomeStatementForChild(
        body: IncomeStatementBody.ChildIncome,
        childId: ChildId,
    ) {
        incomeStatementControllerCitizen.createChildIncomeStatement(
            dbInstance(),
            citizen,
            RealEvakaClock(),
            childId,
            body
        )
    }

    private fun updateIncomeStatement(
        id: IncomeStatementId,
        body: IncomeStatementBody,
    ) {
        incomeStatementControllerCitizen.updateIncomeStatement(
            dbInstance(),
            citizen,
            RealEvakaClock(),
            id,
            body
        )
    }

    private fun deleteIncomeStatement(id: IncomeStatementId) {
        incomeStatementControllerCitizen.deleteIncomeStatement(
            dbInstance(),
            citizen,
            RealEvakaClock(),
            id,
        )
    }

    private fun uploadAttachment(user: AuthenticatedUser.Citizen = citizen): AttachmentId {
        return attachmentsController.uploadIncomeStatementAttachmentCitizen(
            dbInstance(),
            user,
            RealEvakaClock(),
            incomeStatementId = null,
            file = MockMultipartFile("file", "evaka-logo.png", "image/png", pngFile.readBytes())
        )
    }

    private fun idToAttachment(id: AttachmentId) =
        Attachment(id, "evaka-logo.png", "image/png", false)

    private fun uploadAttachmentAsEmployee(
        user: AuthenticatedUser.Employee,
        incomeStatementId: IncomeStatementId
    ): AttachmentId {
        return attachmentsController.uploadIncomeStatementAttachmentEmployee(
            dbInstance(),
            user,
            RealEvakaClock(),
            incomeStatementId,
            MockMultipartFile("file", "evaka-logo.png", "image/png", pngFile.readBytes())
        )
    }
}
