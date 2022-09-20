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
import fi.espoo.evaka.insertApplication
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.CitizenAuthLevel
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.asUser
import fi.espoo.evaka.shared.dev.insertTestApplication
import fi.espoo.evaka.shared.dev.insertTestApplicationForm
import fi.espoo.evaka.shared.dev.insertTestClubApplicationForm
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.RealEvakaClock
import fi.espoo.evaka.test.validClubApplication
import fi.espoo.evaka.test.validDaycareApplication
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testSvebiDaycare
import fi.espoo.evaka.withMockedTime
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class ApplicationReceivedEmailIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired lateinit var asyncJobRunner: AsyncJobRunner<AsyncJob>

    @Autowired lateinit var applicationReceivedEmailService: ApplicationReceivedEmailService

    private val validDaycareForm = DaycareFormV0.fromApplication2(validDaycareApplication)
    private val validClubForm = ClubFormV0.fromForm2(validClubApplication.form, false, false)

    private val serviceWorker =
        AuthenticatedUser.Employee(EmployeeId(testAdult_1.id.raw), setOf(UserRole.SERVICE_WORKER))
    private val endUser = AuthenticatedUser.Citizen(testAdult_1.id, CitizenAuthLevel.STRONG)
    private val guardian = testAdult_1.copy(email = "john.doe@espootest.com")
    private val guardianAsDaycareAdult =
        Adult(
            firstName = guardian.firstName,
            lastName = guardian.lastName,
            phoneNumber = guardian.phone,
            email = guardian.email,
            socialSecurityNumber = guardian.ssn!!,
            address =
                Address(
                    street = guardian.streetAddress,
                    city = guardian.postOffice,
                    postalCode = guardian.postalCode,
                    editable = false
                )
        )

    private val mockedTime = HelsinkiDateTime.of(LocalDate.of(2021, 1, 15), LocalTime.of(12, 0))

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx -> tx.insertGeneralTestFixtures() }
        MockEmailClient.emails.clear()
    }

    @Test
    fun `email is sent after end user sends application`() {
        val applicationId =
            db.transaction { tx ->
                tx.insertTestApplication(
                        childId = testChild_1.id,
                        guardianId = guardian.id,
                        status = ApplicationStatus.CREATED,
                        type = ApplicationType.DAYCARE
                    )
                    .also { id ->
                        tx.insertTestApplicationForm(
                            applicationId = id,
                            document =
                                validDaycareForm.copy(
                                    guardian = guardianAsDaycareAdult,
                                    apply =
                                        validDaycareForm.apply.copy(
                                            preferredUnits = listOf(testDaycare.id)
                                        )
                                )
                        )
                    }
            }

        val (_, res, _) =
            http
                .post("/citizen/applications/$applicationId/actions/send-application")
                .withMockedTime(mockedTime)
                .asUser(endUser)
                .response()

        assertEquals(200, res.statusCode)
        assertApplicationIsSent(applicationId)

        assertEquals(1, asyncJobRunner.getPendingJobCount())
        asyncJobRunner.runPendingJobsSync(RealEvakaClock())

        val sentMails = MockEmailClient.emails
        assertEquals(1, sentMails.size)

        val sentMail = sentMails[0]
        assertEquals(guardian.email, sentMail.toAddress)
        assertEquals(guardian.id.toString(), sentMail.traceId)
        assertEquals(
            "Olemme vastaanottaneet hakemuksenne / Vi har tagit emot din ansökan / We have received your application",
            sentMail.subject
        )
    }

    @Test
    fun `swedish email from address is used after end user sends application to svebi daycare`() {
        val applicationId =
            db.transaction { tx ->
                tx.insertTestApplication(
                        childId = testChild_1.id,
                        guardianId = guardian.id,
                        status = ApplicationStatus.CREATED,
                        type = ApplicationType.DAYCARE
                    )
                    .also { id ->
                        tx.insertTestApplicationForm(
                            applicationId = id,
                            document =
                                validDaycareForm.copy(
                                    guardian = guardianAsDaycareAdult,
                                    apply =
                                        validDaycareForm.apply.copy(
                                            preferredUnits = listOf(testSvebiDaycare.id)
                                        )
                                )
                        )
                    }
            }

        val (_, res, _) =
            http
                .post("/citizen/applications/$applicationId/actions/send-application")
                .withMockedTime(mockedTime)
                .asUser(endUser)
                .response()

        assertEquals(200, res.statusCode)

        assertApplicationIsSent(applicationId)
        asyncJobRunner.runPendingJobsSync(RealEvakaClock(), 1)

        val sentMails = MockEmailClient.emails
        assertEquals(1, sentMails.size)

        val sentMail = sentMails[0]
        assertEquals("Test email sender sv <testemail_sv@test.com>", sentMail.fromAddress)
        assertEquals(guardian.email, sentMail.toAddress)
        assertEquals(guardian.id.toString(), sentMail.traceId)
        assertEquals(
            "Olemme vastaanottaneet hakemuksenne / Vi har tagit emot din ansökan / We have received your application",
            sentMail.subject
        )
    }

    @Test
    fun `email is sent after service worker sends application`() {
        val applicationId =
            db.transaction { tx ->
                tx.insertTestApplication(
                        childId = testChild_1.id,
                        guardianId = guardian.id,
                        status = ApplicationStatus.CREATED,
                        hideFromGuardian = false,
                        type = ApplicationType.DAYCARE
                    )
                    .also { id ->
                        tx.insertTestApplicationForm(
                            applicationId = id,
                            document =
                                validDaycareForm.copy(
                                    guardian = guardianAsDaycareAdult,
                                    apply =
                                        validDaycareForm.apply.copy(
                                            preferredUnits = listOf(testDaycare.id)
                                        )
                                )
                        )
                    }
            }
        val (_, res, _) =
            http
                .post("/v2/applications/$applicationId/actions/send-application")
                .withMockedTime(mockedTime)
                .asUser(serviceWorker)
                .response()

        assertEquals(200, res.statusCode)
        assertApplicationIsSent(applicationId)
        assertEquals(1, asyncJobRunner.getPendingJobCount())

        asyncJobRunner.runPendingJobsSync(RealEvakaClock(), 1)
        assertEquals(0, asyncJobRunner.getPendingJobCount())

        val sentMails = MockEmailClient.emails
        assertEquals(1, sentMails.size)

        val sentMail = sentMails[0]
        assertEquals(guardian.email, sentMail.toAddress)
        assertEquals(guardian.id.toString(), sentMail.traceId)
    }

    @Test
    fun `email is sent after sending club application`() {
        val applicationId =
            db.transaction { tx ->
                tx.insertTestApplication(
                        childId = testChild_1.id,
                        guardianId = guardian.id,
                        status = ApplicationStatus.CREATED,
                        type = ApplicationType.CLUB
                    )
                    .also { id ->
                        tx.insertTestClubApplicationForm(
                            applicationId = id,
                            document = validClubForm
                        )
                    }
            }

        val (_, res, _) =
            http
                .post("/citizen/applications/$applicationId/actions/send-application")
                .withMockedTime(mockedTime)
                .asUser(endUser)
                .response()

        assertEquals(200, res.statusCode)
        assertApplicationIsSent(applicationId)

        asyncJobRunner.runPendingJobsSync(RealEvakaClock(), 1)
        assertEquals(0, asyncJobRunner.getPendingJobCount())

        val sentMails = MockEmailClient.emails

        assertEquals(1, sentMails.size)

        val sentMail = sentMails.first()
        assertEquals(guardian.id.toString(), sentMail.traceId)
        assertEquals(
            "Olemme vastaanottaneet hakemuksenne / Vi har tagit emot din ansökan / We have received your application",
            sentMail.subject
        )
    }

    @Test
    fun `email is sent after sending preschool application`() {
        val applicationId = ApplicationId(UUID.randomUUID())
        db.transaction { tx ->
            tx.insertApplication(
                guardian = guardian,
                appliedType = PlacementType.PRESCHOOL,
                applicationId = applicationId,
                preferredStartDate = LocalDate.of(2021, 8, 11)
            )
        }

        val (_, res, _) =
            http
                .post("/citizen/applications/$applicationId/actions/send-application")
                .withMockedTime(mockedTime)
                .asUser(endUser)
                .response()

        assertEquals(200, res.statusCode)
        assertApplicationIsSent(applicationId)

        asyncJobRunner.runPendingJobsSync(RealEvakaClock(), 1)
        assertEquals(0, asyncJobRunner.getPendingJobCount())

        val sentMails = MockEmailClient.emails

        assertEquals(1, sentMails.size)

        val sentMail = sentMails.first()
        assertEquals(guardian.id.toString(), sentMail.traceId)
        assertEquals(
            "Olemme vastaanottaneet hakemuksenne / Vi har tagit emot din ansökan / We have received your application",
            sentMail.subject
        )
    }

    @Test
    fun `email is not sent when service workers sends hidden application`() {
        val applicationId =
            db.transaction { tx ->
                tx.insertTestApplication(
                        childId = testChild_1.id,
                        guardianId = guardian.id,
                        status = ApplicationStatus.CREATED,
                        hideFromGuardian = true,
                        type = ApplicationType.DAYCARE
                    )
                    .also { id ->
                        tx.insertTestApplicationForm(
                            applicationId = id,
                            document =
                                validDaycareForm.copy(
                                    guardian = guardianAsDaycareAdult,
                                    apply =
                                        validDaycareForm.apply.copy(
                                            preferredUnits = listOf(testDaycare.id)
                                        )
                                )
                        )
                    }
            }
        val (_, res, _) =
            http
                .post("/v2/applications/$applicationId/actions/send-application")
                .withMockedTime(mockedTime)
                .asUser(serviceWorker)
                .response()

        assertEquals(200, res.statusCode)
        assertApplicationIsSent(applicationId)
        assertEquals(0, asyncJobRunner.getPendingJobCount())

        val sentMails = MockEmailClient.emails
        assertEquals(0, sentMails.size)
    }

    @Test
    fun `email is not sent to invalid email`() {
        val applicationId =
            db.transaction { tx ->
                tx.insertTestApplication(
                        childId = testChild_1.id,
                        guardianId = testAdult_1.id,
                        status = ApplicationStatus.CREATED,
                        type = ApplicationType.DAYCARE
                    )
                    .also { id ->
                        tx.insertTestApplicationForm(
                            applicationId = id,
                            document =
                                validDaycareForm.copy(
                                    guardian = guardianAsDaycareAdult.copy(email = "not@valid"),
                                    apply =
                                        validDaycareForm.apply.copy(
                                            preferredUnits = listOf(testDaycare.id)
                                        )
                                )
                        )
                    }
            }

        val (_, res, _) =
            http
                .post("/citizen/applications/$applicationId/actions/send-application")
                .withMockedTime(mockedTime)
                .asUser(endUser)
                .response()

        assertEquals(200, res.statusCode)
        assertApplicationIsSent(applicationId)
        assertEquals(1, asyncJobRunner.getPendingJobCount())

        asyncJobRunner.runPendingJobsSync(RealEvakaClock())

        val sentMails = MockEmailClient.emails
        assertEquals(0, sentMails.size)
    }

    @Test
    fun `application keeps its manually set sent date after it is sent`() {
        val manuallySetSentDate = LocalDate.of(2020, 1, 1)
        val applicationId =
            db.transaction { tx ->
                tx.insertTestApplication(
                        childId = testChild_1.id,
                        guardianId = testAdult_1.id,
                        status = ApplicationStatus.CREATED,
                        sentDate = manuallySetSentDate,
                        type = ApplicationType.DAYCARE
                    )
                    .also { id ->
                        tx.insertTestApplicationForm(
                            applicationId = id,
                            document = validDaycareForm
                        )
                    }
            }

        val (_, res, _) =
            http
                .post("/citizen/applications/$applicationId/actions/send-application")
                .withMockedTime(mockedTime)
                .asUser(endUser)
                .response()

        assertEquals(200, res.statusCode)
        assertApplicationIsSent(applicationId)
        assertEquals(1, asyncJobRunner.getPendingJobCount())

        asyncJobRunner.runPendingJobsSync(RealEvakaClock())

        val result = db.read { r -> r.fetchApplicationDetails(applicationId) }
        assertEquals(manuallySetSentDate, result?.sentDate)
    }

    @Test
    fun `valid email is sent`() {
        applicationReceivedEmailService.sendApplicationEmail(
            testAdult_1.id,
            "working@test.fi",
            Language.fi,
            ApplicationType.DAYCARE
        )
        assertEmail(
            MockEmailClient.getEmail("working@test.fi"),
            "working@test.fi",
            "Test email sender fi <testemail_fi@test.com>",
            "Olemme vastaanottaneet hakemuksenne / Vi har tagit emot din ansökan / We have received your application",
            "Varhaiskasvatushakemuksella on <strong>neljän (4) kuukauden hakuaika",
            "Varhaiskasvatushakemuksella on neljän (4) kuukauden hakuaika"
        )

        applicationReceivedEmailService.sendApplicationEmail(
            testAdult_1.id,
            "Working.Email@Test.Com",
            Language.sv,
            ApplicationType.DAYCARE
        )
        assertEmail(
            MockEmailClient.getEmail("Working.Email@Test.Com"),
            "Working.Email@Test.Com",
            "Test email sender sv <testemail_sv@test.com>",
            "Olemme vastaanottaneet hakemuksenne / Vi har tagit emot din ansökan / We have received your application",
            "Ansökan om småbarnspedagogik har en <strong>ansökningstid på fyra (4) månader",
            "Ansökan om småbarnspedagogik har en ansökningstid på fyra (4) månader"
        )
    }

    @Test
    fun `email with invalid toAddress is not sent`() {
        applicationReceivedEmailService.sendApplicationEmail(
            testAdult_1.id,
            "not.working.com",
            Language.fi,
            ApplicationType.DAYCARE
        )
        applicationReceivedEmailService.sendApplicationEmail(
            testAdult_1.id,
            "@test.fi",
            Language.fi,
            ApplicationType.DAYCARE
        )

        assertEquals(0, MockEmailClient.emails.size)
    }

    private fun assertEmail(
        email: MockEmail?,
        expectedToAddress: String,
        expectedFromAddress: String,
        expectedSubject: String,
        expectedHtmlPart: String,
        expectedTextPart: String
    ) {
        assertNotNull(email)
        assertEquals(expectedToAddress, email.toAddress)
        assertEquals(expectedFromAddress, email.fromAddress)
        assertEquals(expectedSubject, email.subject)
        assert(email.htmlBody.contains(expectedHtmlPart, true))
        assert(email.textBody.contains(expectedTextPart, true))
    }

    private fun assertApplicationIsSent(applicationId: ApplicationId) {
        db.read {
            assertEquals(ApplicationStatus.SENT, it.fetchApplicationDetails(applicationId)!!.status)
        }
    }
}
