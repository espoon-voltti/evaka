// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.incomestatement

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.attachment.AttachmentsController
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
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.Forbidden
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testAdult_2
import fi.espoo.evaka.testArea
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testDaycare
import java.time.LocalDate
import java.time.LocalTime
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

    private val clock =
        MockEvakaClock(HelsinkiDateTime.of(LocalDate.of(2024, 11, 18), LocalTime.of(15, 30)))

    private val citizen = AuthenticatedUser.Citizen(testAdult_1.id, CitizenAuthLevel.STRONG)

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insert(testArea)
            tx.insert(testDaycare)
            listOf(testAdult_1, testAdult_2).forEach { tx.insert(it, DevPersonType.ADULT) }
            tx.insert(testChild_1, DevPersonType.CHILD)
        }
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
                    createdAt = incomeStatements[0].createdAt,
                    modifiedAt = incomeStatements[0].modifiedAt,
                    sentAt = incomeStatements[0].sentAt,
                    status = IncomeStatementStatus.SENT,
                    handledAt = null,
                    handlerNote = "",
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
                gross =
                    Gross(
                        incomeSource = IncomeSource.INCOMES_REGISTER,
                        estimatedMonthlyIncome = 500,
                        otherIncome = setOf(OtherIncome.ALIMONY, OtherIncome.RENTAL_INCOME),
                        otherIncomeInfo = "Elatusmaksut 100, vuoratulot 150",
                    ),
                entrepreneur =
                    Entrepreneur(
                        fullTime = true,
                        startOfEntrepreneurship = LocalDate.of(1998, 1, 1),
                        companyName = "Acme Inc",
                        businessId = "1234567-8",
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
                                        incomeEndDate = LocalDate.of(2021, 7, 7),
                                    ),
                            ),
                        limitedCompany = LimitedCompany(IncomeSource.INCOMES_REGISTER),
                        partnership = false,
                        lightEntrepreneur = true,
                        accountant =
                            Accountant(
                                name = "Foo",
                                address = "Bar",
                                phone = "123",
                                email = "foo.bar@example.com",
                            ),
                    ),
                student = false,
                alimonyPayer = true,
                otherInfo = "foo bar",
                attachmentIds = listOf(),
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
                            otherIncomeInfo = "Elatusmaksut 100, vuoratulot 150",
                        ),
                    entrepreneur =
                        Entrepreneur(
                            fullTime = true,
                            startOfEntrepreneurship = LocalDate.of(1998, 1, 1),
                            companyName = "Acme Inc",
                            businessId = "1234567-8",
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
                                            incomeEndDate = LocalDate.of(2021, 7, 7),
                                        ),
                                ),
                            limitedCompany = LimitedCompany(IncomeSource.INCOMES_REGISTER),
                            partnership = false,
                            lightEntrepreneur = true,
                            accountant =
                                Accountant(
                                    name = "Foo",
                                    address = "Bar",
                                    phone = "123",
                                    email = "foo.bar@example.com",
                                ),
                        ),
                    student = false,
                    alimonyPayer = true,
                    otherInfo = "foo bar",
                    createdAt = incomeStatements[0].createdAt,
                    modifiedAt = incomeStatements[0].modifiedAt,
                    sentAt = incomeStatements[0].sentAt,
                    status = IncomeStatementStatus.SENT,
                    handlerNote = "",
                    handledAt = null,
                    attachments = listOf(),
                )
            ),
            incomeStatements,
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
                    attachmentIds = listOf(),
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
                attachmentIds = listOf(),
            ),
            testChild_1.id,
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
                    createdAt = incomeStatements[0].createdAt,
                    modifiedAt = incomeStatements[0].modifiedAt,
                    sentAt = incomeStatements[0].sentAt,
                    status = IncomeStatementStatus.SENT,
                    handlerNote = "",
                    handledAt = null,
                    attachments = listOf(),
                )
            ),
            incomeStatements,
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
                    attachmentIds = listOf(),
                )
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
                            otherIncomeInfo = "",
                        ),
                    entrepreneur = null,
                    student = false,
                    alimonyPayer = true,
                    otherInfo = "foo bar",
                    attachmentIds = listOf(),
                )
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
                            companyName = "Acme Inc",
                            businessId = "1234567-8",
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
                                    email = "foo.bar@example.com",
                                ),
                        ),
                    student = false,
                    alimonyPayer = false,
                    otherInfo = "foo bar",
                    attachmentIds = listOf(),
                )
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
                            companyName = "Acme Inc",
                            businessId = "1234567-8",
                            spouseWorksInCompany = true,
                            startupGrant = true,
                            checkupConsent = true,
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
                    attachmentIds = listOf(),
                )
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
                            companyName = "Acme Inc",
                            businessId = "1234567-8",
                            spouseWorksInCompany = true,
                            startupGrant = true,
                            checkupConsent = true,
                            selfEmployed = null,
                            limitedCompany = null,
                            partnership = true,
                            lightEntrepreneur = false,
                            // Accountant name, phone or email cannot be empty
                            accountant = Accountant(name = "", address = "", phone = "", email = ""),
                        ),
                    student = false,
                    alimonyPayer = false,
                    otherInfo = "foo bar",
                    attachmentIds = listOf(),
                )
            )
        }
    }

    @Test
    fun `create an income statement with attachments`() {
        val attachmentId1 =
            uploadAttachment(attachmentType = IncomeStatementAttachmentType.PAYSLIP_GROSS)
        val attachmentId2 = uploadAttachment(attachmentType = IncomeStatementAttachmentType.OTHER)

        createIncomeStatement(
            IncomeStatementBody.Income(
                startDate = LocalDate.of(2021, 4, 3),
                endDate = LocalDate.of(2022, 4, 1),
                gross =
                    Gross(
                        incomeSource = IncomeSource.ATTACHMENTS,
                        estimatedMonthlyIncome = 1500,
                        otherIncome = setOf(),
                        otherIncomeInfo = "",
                    ),
                entrepreneur = null,
                student = false,
                alimonyPayer = true,
                otherInfo = "foo bar",
                attachmentIds = listOf(attachmentId1, attachmentId2),
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
                    endDate = LocalDate.of(2022, 4, 1),
                    gross =
                        Gross(
                            incomeSource = IncomeSource.ATTACHMENTS,
                            estimatedMonthlyIncome = 1500,
                            otherIncome = setOf(),
                            otherIncomeInfo = "",
                        ),
                    entrepreneur = null,
                    student = false,
                    alimonyPayer = true,
                    otherInfo = "foo bar",
                    createdAt = incomeStatements[0].createdAt,
                    modifiedAt = incomeStatements[0].modifiedAt,
                    sentAt = incomeStatements[0].sentAt,
                    status = IncomeStatementStatus.SENT,
                    handlerNote = "",
                    handledAt = null,
                    attachments =
                        listOf(
                            idToAttachment(
                                attachmentId1,
                                IncomeStatementAttachmentType.PAYSLIP_GROSS,
                            ),
                            idToAttachment(attachmentId2, IncomeStatementAttachmentType.OTHER),
                        ),
                )
            ),
            incomeStatements,
        )
    }

    @Test
    fun `create an income statement with an untyped attachment`() {
        val attachmentId = uploadAttachment(attachmentType = null)

        createIncomeStatement(
            IncomeStatementBody.Income(
                startDate = LocalDate.of(2021, 4, 3),
                endDate = LocalDate.of(2022, 4, 1),
                gross =
                    Gross(
                        incomeSource = IncomeSource.ATTACHMENTS,
                        estimatedMonthlyIncome = 1500,
                        otherIncome = setOf(),
                        otherIncomeInfo = "",
                    ),
                entrepreneur = null,
                student = false,
                alimonyPayer = true,
                otherInfo = "foo bar",
                attachmentIds = listOf(attachmentId),
            )
        )

        val incomeStatements = getIncomeStatements().data
        assertEquals(1, incomeStatements.size)
        incomeStatements.first().also {
            val incomeStatement = it as IncomeStatement.Income
            assertEquals(
                listOf(idToAttachment(attachmentId, attachmentType = null)),
                incomeStatement.attachments,
            )
        }
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
                attachmentIds = listOf(attachmentId),
            ),
            testChild_1.id,
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
                    createdAt = incomeStatements[0].createdAt,
                    modifiedAt = incomeStatements[0].modifiedAt,
                    sentAt = incomeStatements[0].sentAt,
                    status = IncomeStatementStatus.SENT,
                    handlerNote = "",
                    handledAt = null,
                    attachments = listOf(idToAttachment(attachmentId)),
                )
            ),
            incomeStatements,
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
                            otherIncomeInfo = "",
                        ),
                    entrepreneur = null,
                    student = false,
                    alimonyPayer = false,
                    otherInfo = "foo bar",
                    attachmentIds = listOf(nonExistingAttachmentId),
                )
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
                            otherIncomeInfo = "",
                        ),
                    entrepreneur = null,
                    student = false,
                    alimonyPayer = false,
                    otherInfo = "foo bar",
                    attachmentIds = listOf(attachmentId),
                )
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
                        otherIncomeInfo = "Elatusmaksut 100, vuokratulot 150",
                    ),
                entrepreneur =
                    Entrepreneur(
                        fullTime = true,
                        startOfEntrepreneurship = LocalDate.of(1998, 1, 1),
                        companyName = "Acme Inc",
                        businessId = "1234567-8",
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
                                        incomeEndDate = LocalDate.of(2021, 7, 7),
                                    ),
                            ),
                        limitedCompany =
                            LimitedCompany(incomeSource = IncomeSource.INCOMES_REGISTER),
                        partnership = false,
                        lightEntrepreneur = false,
                        Accountant(
                            name = "Foo",
                            address = "Bar",
                            phone = "123",
                            email = "foo.bar@example.com",
                        ),
                    ),
                student = false,
                alimonyPayer = true,
                otherInfo = "foo bar",
                attachmentIds = listOf(attachment1),
            ),
            draft = true,
        )

        val original = getIncomeStatements().data[0]

        clock.tick()

        val update1 =
            IncomeStatementBody.Income(
                startDate = LocalDate.of(2021, 6, 11),
                endDate = LocalDate.of(2022, 6, 1),
                gross = null,
                entrepreneur =
                    Entrepreneur(
                        fullTime = false,
                        startOfEntrepreneurship = LocalDate.of(2019, 1, 1),
                        companyName = "Acme Inc",
                        businessId = "1234567-8",
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
                            email = "baz.quux@example.com",
                        ),
                    ),
                student = true,
                alimonyPayer = false,
                otherInfo = "",
                attachmentIds = listOf(attachment2, attachment3),
            )

        // update and send
        updateIncomeStatement(id = original.id, body = update1, draft = false)

        val modifiedAt = getIncomeStatement(original.id).modifiedAt
        assertNotEquals(original.modifiedAt, modifiedAt)

        assertEquals(
            IncomeStatement.Income(
                id = original.id,
                personId = testAdult_1.id,
                firstName = testAdult_1.firstName,
                lastName = testAdult_1.lastName,
                startDate = LocalDate.of(2021, 6, 11),
                endDate = LocalDate.of(2022, 6, 1),
                gross = null,
                entrepreneur =
                    Entrepreneur(
                        fullTime = false,
                        startOfEntrepreneurship = LocalDate.of(2019, 1, 1),
                        companyName = "Acme Inc",
                        businessId = "1234567-8",
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
                            email = "baz.quux@example.com",
                        ),
                    ),
                student = true,
                alimonyPayer = false,
                otherInfo = "",
                createdAt = original.createdAt,
                modifiedAt = modifiedAt,
                sentAt = clock.now(),
                status = IncomeStatementStatus.SENT,
                handlerNote = "",
                handledAt = null,
                attachments = listOf(idToAttachment(attachment2), idToAttachment(attachment3)),
            ),
            getIncomeStatement(original.id),
        )

        // attachments and otherInfo can be still updated after sending
        updateSentIncomeStatement(
            id = original.id,
            body =
                IncomeStatementControllerCitizen.UpdateSentIncomeStatementBody(
                    otherInfo = "hello",
                    attachmentIds = listOf(attachment1, attachment3),
                ),
        )
        getIncomeStatement(original.id).let {
            assertEquals("hello", (it as IncomeStatement.Income).otherInfo)
            assertEquals(
                listOf(idToAttachment(attachment1), idToAttachment(attachment3)),
                it.attachments,
            )
        }

        // full update is not allowed after sending
        val update3 = update1.copy(alimonyPayer = true)
        assertThrows<Forbidden> {
            updateIncomeStatement(id = original.id, body = update3, draft = false)
        }
    }

    @Test
    fun `cannot update a handled income statement`() {
        val employee = DevEmployee()
        db.transaction { it.insert(employee) }

        createIncomeStatement(
            IncomeStatementBody.HighestFee(startDate = LocalDate.of(2021, 4, 3), endDate = null)
        )
        val id = getIncomeStatements().data.first().id

        markIncomeStatementHandled(id, employee.id, "foooooo")

        assertThrows<Forbidden> {
            updateSentIncomeStatement(
                id,
                IncomeStatementControllerCitizen.UpdateSentIncomeStatementBody(
                    otherInfo = "hello",
                    attachmentIds = listOf(),
                ),
            )
        }

        assertThrows<Forbidden> {
            updateIncomeStatement(
                id,
                IncomeStatementBody.HighestFee(startDate = LocalDate.of(2030, 4, 3), endDate = null),
            )
        }
    }

    @Test
    fun `cannot see handler note or remove a handled income statement`() {
        val employee = DevEmployee()
        db.transaction { it.insert(employee) }

        createIncomeStatement(
            IncomeStatementBody.HighestFee(startDate = LocalDate.of(2021, 4, 3), endDate = null)
        )
        val incomeStatement = getIncomeStatements().data.first()
        assertEquals("", incomeStatement.handlerNote)

        markIncomeStatementHandled(incomeStatement.id, employee.id, "foo bar")

        val handled = getIncomeStatements().data.first()
        assertEquals(IncomeStatementStatus.HANDLED, handled.status)
        assertEquals("", handled.handlerNote)

        assertThrows<Forbidden> { deleteIncomeStatement(incomeStatement.id) }
    }

    @Test
    fun `paging works`() {
        createIncomeStatement(
            IncomeStatementBody.HighestFee(
                startDate = LocalDate.of(2020, 4, 3),
                endDate = LocalDate.of(2020, 12, 31),
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
    }

    @Test
    fun `cannot create two income statements with the same startDate`() {
        createIncomeStatement(
            IncomeStatementBody.HighestFee(startDate = LocalDate.of(2021, 4, 3), endDate = null)
        )
        assertThrows<BadRequest> {
            createIncomeStatement(
                IncomeStatementBody.HighestFee(startDate = LocalDate.of(2021, 4, 3), endDate = null)
            )
        }
    }

    private fun markIncomeStatementHandled(
        id: IncomeStatementId,
        handlerId: EmployeeId,
        note: String,
    ) =
        db.transaction { tx ->
            tx.execute {
                sql(
                    """
UPDATE income_statement
SET handler_id = ${bind(handlerId)}, 
    handler_note = ${bind(note)}, 
    status = 'HANDLED'::income_statement_status,
    handled_at = ${bind(clock.now())}
WHERE id = ${bind(id)}
"""
                )
            }
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
                        otherIncomeInfo = "Elatusmaksut 100, vuokratulot 150",
                    ),
                entrepreneur =
                    Entrepreneur(
                        fullTime = true,
                        startOfEntrepreneurship = LocalDate.of(1998, 1, 1),
                        companyName = "Acme Inc",
                        businessId = "1234567-8",
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
                                        incomeEndDate = LocalDate.of(2021, 7, 7),
                                    ),
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
                                email = "foo.bar@example.com",
                            ),
                    ),
                student = false,
                alimonyPayer = true,
                otherInfo = "foo bar",
                attachmentIds = listOf(attachment1),
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
            },
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
                    startDate = clock.today(),
                    endDate = clock.today(),
                    type = PlacementType.DAYCARE,
                    unitId = testDaycare.id,
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
                    startDate = clock.today().minusWeeks(1),
                    endDate = clock.today().minusWeeks(1),
                    type = PlacementType.DAYCARE,
                    unitId = testDaycare.id,
                )
            )
        }

        assertEquals(getIncomeStatementChildren().size, 0)
    }

    private fun getIncomeStatements(page: Int = 1): PagedIncomeStatements {
        return incomeStatementControllerCitizen.getIncomeStatements(
            dbInstance(),
            citizen,
            clock,
            page = page,
        )
    }

    private fun getIncomeStatementsForChild(
        childId: ChildId,
        page: Int = 1,
    ): PagedIncomeStatements {
        return incomeStatementControllerCitizen.getChildIncomeStatements(
            dbInstance(),
            citizen,
            clock,
            childId,
            page = page,
        )
    }

    private fun getIncomeStatement(id: IncomeStatementId): IncomeStatement {
        return incomeStatementControllerCitizen.getIncomeStatement(dbInstance(), citizen, clock, id)
    }

    private fun getIncomeStatementChildren(): List<ChildBasicInfo> {
        return incomeStatementControllerCitizen.getIncomeStatementChildren(
            dbInstance(),
            citizen,
            clock,
        )
    }

    private fun createIncomeStatement(body: IncomeStatementBody, draft: Boolean = false) {
        incomeStatementControllerCitizen.createIncomeStatement(
            dbInstance(),
            citizen,
            clock,
            body,
            draft,
        )
    }

    private fun createIncomeStatementForChild(
        body: IncomeStatementBody.ChildIncome,
        childId: ChildId,
        draft: Boolean = false,
    ) {
        incomeStatementControllerCitizen.createChildIncomeStatement(
            dbInstance(),
            citizen,
            clock,
            childId,
            body,
            draft,
        )
    }

    private fun updateIncomeStatement(
        id: IncomeStatementId,
        body: IncomeStatementBody,
        draft: Boolean = false,
    ) {
        incomeStatementControllerCitizen.updateIncomeStatement(
            dbInstance(),
            citizen,
            clock,
            id,
            body,
            draft,
        )
    }

    private fun updateSentIncomeStatement(
        id: IncomeStatementId,
        body: IncomeStatementControllerCitizen.UpdateSentIncomeStatementBody,
    ) {
        incomeStatementControllerCitizen.updateSentIncomeStatement(
            dbInstance(),
            citizen,
            clock,
            id,
            body,
        )
    }

    private fun deleteIncomeStatement(id: IncomeStatementId) {
        incomeStatementControllerCitizen.deleteIncomeStatement(dbInstance(), citizen, clock, id)
    }

    private fun uploadAttachment(
        user: AuthenticatedUser.Citizen = citizen,
        attachmentType: IncomeStatementAttachmentType? = IncomeStatementAttachmentType.OTHER,
    ): AttachmentId {
        return attachmentsController.uploadIncomeStatementAttachmentCitizen(
            dbInstance(),
            user,
            clock,
            incomeStatementId = null,
            attachmentType = attachmentType,
            file = MockMultipartFile("file", "evaka-logo.png", "image/png", pngFile.readBytes()),
        )
    }

    private fun idToAttachment(
        id: AttachmentId,
        attachmentType: IncomeStatementAttachmentType? = IncomeStatementAttachmentType.OTHER,
    ) = IncomeStatementAttachment(id, "evaka-logo.png", "image/png", attachmentType, false)

    private fun uploadAttachmentAsEmployee(
        user: AuthenticatedUser.Employee,
        incomeStatementId: IncomeStatementId,
    ): AttachmentId {
        return attachmentsController.uploadIncomeStatementAttachmentEmployee(
            dbInstance(),
            user,
            clock,
            incomeStatementId,
            IncomeStatementAttachmentType.OTHER,
            MockMultipartFile("file", "evaka-logo.png", "image/png", pngFile.readBytes()),
        )
    }
}
