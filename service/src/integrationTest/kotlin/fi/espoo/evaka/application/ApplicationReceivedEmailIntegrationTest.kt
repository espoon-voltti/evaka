// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.application

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.application.persistence.club.ClubFormV0
import fi.espoo.evaka.application.persistence.daycare.Address
import fi.espoo.evaka.application.persistence.daycare.Adult
import fi.espoo.evaka.application.persistence.daycare.DaycareFormV0
import fi.espoo.evaka.daycare.domain.Language
import fi.espoo.evaka.emailclient.MockEmail
import fi.espoo.evaka.emailclient.MockEmailClient
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.asUser
import fi.espoo.evaka.shared.db.handle
import fi.espoo.evaka.shared.dev.insertTestApplication
import fi.espoo.evaka.shared.dev.insertTestApplicationForm
import fi.espoo.evaka.shared.dev.insertTestClubApplicationForm
import fi.espoo.evaka.test.validClubApplication
import fi.espoo.evaka.test.validDaycareApplication
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testSvebiDaycare
import fi.espoo.evaka.testVoucherDaycare
import org.jdbi.v3.core.Handle
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.util.UUID

class ApplicationReceivedEmailIntegrationTest : FullApplicationTest() {
    @Autowired
    lateinit var asyncJobRunner: AsyncJobRunner

    @Autowired
    lateinit var applicationReceivedEmailService: ApplicationReceivedEmailService

    private val validDaycareForm = DaycareFormV0.fromApplication2(validDaycareApplication)
    private val validClubForm = ClubFormV0.fromForm2(validClubApplication.form, false, false)

    private val serviceWorker = AuthenticatedUser.Employee(testAdult_1.id, setOf(UserRole.SERVICE_WORKER))
    private val endUser = AuthenticatedUser.Citizen(testAdult_1.id)
    private val guardian = testAdult_1.copy(email = "john.doe@espootest.com")
    private val guardianAsDaycareAdult = Adult(
        firstName = guardian.firstName,
        lastName = guardian.lastName,
        phoneNumber = guardian.phone,
        email = guardian.email,
        socialSecurityNumber = guardian.ssn!!,
        address = Address(
            street = guardian.streetAddress!!,
            city = guardian.postOffice!!,
            postalCode = guardian.postalCode!!,
            editable = false
        )
    )

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.resetDatabase()
            insertGeneralTestFixtures(tx.handle)
        }
        MockEmailClient.emails.clear()
    }

    @Test
    fun `email is sent after end user sends application`() {
        val applicationId = db.transaction { tx ->
            insertTestApplication(
                h = tx.handle,
                childId = testChild_1.id,
                guardianId = guardian.id,
                status = ApplicationStatus.CREATED
            ).also { id ->
                insertTestApplicationForm(
                    h = tx.handle,
                    applicationId = id,
                    document = validDaycareForm.copy(
                        guardian = guardianAsDaycareAdult,
                        apply = validDaycareForm.apply.copy(
                            preferredUnits = listOf(testDaycare.id)
                        )
                    )
                )
            }
        }

        val (_, res, _) = http.post("/citizen/applications/$applicationId/actions/send-application")
            .asUser(endUser)
            .response()

        assertEquals(204, res.statusCode)
        db.read { tx ->
            assertApplicationIsSent(tx.handle, applicationId)
        }

        assertEquals(1, asyncJobRunner.getPendingJobCount())
        asyncJobRunner.runPendingJobsSync()

        val sentMails = MockEmailClient.emails
        assertEquals(1, sentMails.size)

        val sentMail = sentMails[0]
        assertEquals(guardian.email, sentMail.toAddress)
        assertEquals(guardian.id.toString(), sentMail.traceId)
        assertEquals("Olemme vastaanottaneet hakemuksenne", sentMail.subject)
    }

    @Test
    fun `swedish email is sent after end user sends application to svebi daycare`() {
        val applicationId = db.transaction { tx ->
            insertTestApplication(
                h = tx.handle,
                childId = testChild_1.id,
                guardianId = guardian.id,
                status = ApplicationStatus.CREATED
            ).also { id ->
                insertTestApplicationForm(
                    h = tx.handle,
                    applicationId = id,
                    document = validDaycareForm.copy(
                        guardian = guardianAsDaycareAdult,
                        apply = validDaycareForm.apply.copy(
                            preferredUnits = listOf(testSvebiDaycare.id)
                        )
                    )
                )
            }
        }

        val (_, res, _) = http.post("/citizen/applications/$applicationId/actions/send-application")
            .asUser(endUser)
            .response()

        assertEquals(204, res.statusCode)

        db.read { tx -> assertApplicationIsSent(tx.handle, applicationId) }
        asyncJobRunner.runPendingJobsSync(1)

        val sentMails = MockEmailClient.emails
        assertEquals(1, sentMails.size)

        val sentMail = sentMails[0]
        assertEquals(guardian.email, sentMail.toAddress)
        assertEquals(guardian.id.toString(), sentMail.traceId)
        assertEquals("Vi har tagit emot din ansökan", sentMail.subject)
    }

    @Test
    fun `email is sent after service worker sends application`() {
        val applicationId = db.transaction { tx ->
            insertTestApplication(
                h = tx.handle,
                childId = testChild_1.id,
                guardianId = guardian.id,
                status = ApplicationStatus.CREATED,
                hideFromGuardian = false
            ).also { id ->
                insertTestApplicationForm(
                    h = tx.handle,
                    applicationId = id,
                    document = validDaycareForm.copy(
                        guardian = guardianAsDaycareAdult,
                        apply = validDaycareForm.apply.copy(
                            preferredUnits = listOf(testDaycare.id)
                        )
                    )
                )
            }
        }
        val (_, res, _) = http.post("/v2/applications/$applicationId/actions/send-application")
            .asUser(serviceWorker)
            .response()

        assertEquals(204, res.statusCode)
        db.read { tx -> assertApplicationIsSent(tx.handle, applicationId) }
        assertEquals(1, asyncJobRunner.getPendingJobCount())

        asyncJobRunner.runPendingJobsSync(1)
        assertEquals(0, asyncJobRunner.getPendingJobCount())

        val sentMails = MockEmailClient.emails
        assertEquals(1, sentMails.size)

        val sentMail = sentMails[0]
        assertEquals(guardian.email, sentMail.toAddress)
        assertEquals(guardian.id.toString(), sentMail.traceId)
    }

    @Test
    fun `email is sent after sending club application`() {
        val applicationId = db.transaction { tx ->
            insertTestApplication(
                h = tx.handle,
                childId = testChild_1.id,
                guardianId = guardian.id,
                status = ApplicationStatus.CREATED
            ).also { id ->
                insertTestClubApplicationForm(
                    h = tx.handle,
                    applicationId = id,
                    document = validClubForm
                )
            }
        }

        val (_, res, _) = http.post("/citizen/applications/$applicationId/actions/send-application")
            .asUser(endUser)
            .response()

        assertEquals(204, res.statusCode)
        db.read { tx -> assertApplicationIsSent(tx.handle, applicationId) }

        asyncJobRunner.runPendingJobsSync(1)
        assertEquals(0, asyncJobRunner.getPendingJobCount())

        val sentMails = MockEmailClient.emails

        assertEquals(1, sentMails.size)

        val sentMail = sentMails.first()
        assertEquals(guardian.id.toString(), sentMail.traceId)
        assertEquals("Olemme vastaanottaneet hakemuksenne", sentMail.subject)
    }

    @Test
    fun `email is not sent when provider type of preferred unit is private voucher`() {
        val applicationId = db.transaction { tx ->
            insertTestApplication(
                h = tx.handle,
                childId = testChild_1.id,
                guardianId = guardian.id,
                status = ApplicationStatus.CREATED
            ).also { id ->
                insertTestApplicationForm(
                    h = tx.handle,
                    applicationId = id,
                    document = validDaycareForm.copy(
                        guardian = guardianAsDaycareAdult,
                        apply = validDaycareForm.apply.copy(
                            preferredUnits = listOf(testVoucherDaycare.id)
                        )
                    )
                )
            }
        }

        val (_, res, _) = http.post("/citizen/applications/$applicationId/actions/send-application")
            .asUser(endUser)
            .response()

        assertEquals(204, res.statusCode)
        db.read { tx -> assertApplicationIsSent(tx.handle, applicationId) }
        assertEquals(0, asyncJobRunner.getPendingJobCount())
        assertEquals(0, MockEmailClient.emails.size)
    }

    @Test
    fun `email is not sent when service workers sends hidden application`() {
        val applicationId = db.transaction { tx ->
            insertTestApplication(
                h = tx.handle,
                childId = testChild_1.id,
                guardianId = guardian.id,
                status = ApplicationStatus.CREATED,
                hideFromGuardian = true
            ).also { id ->
                insertTestApplicationForm(
                    h = tx.handle,
                    applicationId = id,
                    document = validDaycareForm.copy(
                        guardian = guardianAsDaycareAdult,
                        apply = validDaycareForm.apply.copy(
                            preferredUnits = listOf(testDaycare.id)
                        )
                    )
                )
            }
        }
        val (_, res, _) = http.post("/v2/applications/$applicationId/actions/send-application")
            .asUser(serviceWorker)
            .response()

        assertEquals(204, res.statusCode)
        db.read { tx -> assertApplicationIsSent(tx.handle, applicationId) }
        assertEquals(0, asyncJobRunner.getPendingJobCount())

        val sentMails = MockEmailClient.emails
        assertEquals(0, sentMails.size)
    }

    @Test
    fun `email is not sent to invalid email`() {
        val applicationId = db.transaction { tx ->
            insertTestApplication(
                h = tx.handle,
                childId = testChild_1.id,
                guardianId = testAdult_1.id,
                status = ApplicationStatus.CREATED
            ).also { id ->
                insertTestApplicationForm(
                    h = tx.handle,
                    applicationId = id,
                    document = validDaycareForm.copy(
                        guardian = guardianAsDaycareAdult.copy(email = "not@valid"),
                        apply = validDaycareForm.apply.copy(
                            preferredUnits = listOf(testDaycare.id)
                        )
                    )
                )
            }
        }

        val (_, res, _) = http.post("/citizen/applications/$applicationId/actions/send-application")
            .asUser(endUser)
            .response()

        assertEquals(204, res.statusCode)
        db.read { tx -> assertApplicationIsSent(tx.handle, applicationId) }
        assertEquals(1, asyncJobRunner.getPendingJobCount())

        asyncJobRunner.runPendingJobsSync()

        val sentMails = MockEmailClient.emails
        assertEquals(0, sentMails.size)
    }

    @Test
    fun `application keeps its manually set sent date after it is sent`() {
        val manuallySetSentDate = LocalDate.of(2020, 1, 1)
        val applicationId = db.transaction { tx ->
            insertTestApplication(
                h = tx.handle,
                childId = testChild_1.id,
                guardianId = testAdult_1.id,
                status = ApplicationStatus.CREATED,
                sentDate = manuallySetSentDate
            ).also { id ->
                insertTestApplicationForm(
                    h = tx.handle,
                    applicationId = id,
                    document = validDaycareForm
                )
            }
        }

        val (_, res, _) = http.post("/citizen/applications/$applicationId/actions/send-application")
            .asUser(endUser)
            .response()

        assertEquals(204, res.statusCode)
        db.read { tx -> assertApplicationIsSent(tx.handle, applicationId) }
        assertEquals(1, asyncJobRunner.getPendingJobCount())

        asyncJobRunner.runPendingJobsSync()

        val result = db.read { tx -> fetchApplicationDetails(tx.handle, applicationId) }
        assertEquals(manuallySetSentDate, result?.sentDate)
    }

    @Test
    fun `valid email is sent`() {
        applicationReceivedEmailService.sendApplicationEmail(testAdult_1.id, "working@test.fi", Language.fi, ApplicationType.DAYCARE)
        assertEmail(
            MockEmailClient.getEmail("working@test.fi"),
            "working@test.fi",
            "Test email sender fi <testemail_fi@test.com>",
            "Olemme vastaanottaneet hakemuksenne",
            "Varhaiskasvatushakemuksella on <strong>neljän (4) kuukauden hakuaika",
            "Varhaiskasvatushakemuksella on neljän (4) kuukauden hakuaika"
        )

        applicationReceivedEmailService.sendApplicationEmail(testAdult_1.id, "Working.Email@Test.Com", Language.sv, ApplicationType.DAYCARE)
        assertEmail(
            MockEmailClient.getEmail("Working.Email@Test.Com"),
            "Working.Email@Test.Com",
            "Test email sender sv <testemail_sv@test.com>",
            "Vi har tagit emot din ansökan",
            "Ansökan om småbarnspedagogik har en <strong>ansökningstid på fyra (4) månader",
            "Ansökan om småbarnspedagogik har en ansökningstid på fyra (4) månader"
        )
    }

    @Test
    fun `email with invalid toAddress is not sent`() {
        applicationReceivedEmailService.sendApplicationEmail(testAdult_1.id, "not.working.com", Language.fi, ApplicationType.DAYCARE)
        applicationReceivedEmailService.sendApplicationEmail(testAdult_1.id, "@test.fi", Language.fi, ApplicationType.DAYCARE)

        assertEquals(0, MockEmailClient.emails.size)
    }

    private fun assertEmail(email: MockEmail?, expectedToAddress: String, expectedFromAddress: String, expectedSubject: String, expectedHtmlPart: String, expectedTextPart: String) {
        Assertions.assertNotNull(email)
        assertEquals(expectedToAddress, email?.toAddress)
        assertEquals(expectedFromAddress, email?.fromAddress)
        assertEquals(expectedSubject, email?.subject)
        assert(email!!.htmlBody.contains(expectedHtmlPart, true))
        assert(email.textBody.contains(expectedTextPart, true))
    }

    private fun assertApplicationIsSent(h: Handle, applicationId: UUID) {
        assertEquals(ApplicationStatus.SENT, fetchApplicationDetails(h, applicationId)!!.status)
    }
}
