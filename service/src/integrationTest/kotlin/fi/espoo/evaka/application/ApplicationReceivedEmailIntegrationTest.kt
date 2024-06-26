// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.application

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.application.persistence.club.ClubFormV0
import fi.espoo.evaka.application.persistence.daycare.Address
import fi.espoo.evaka.application.persistence.daycare.Adult
import fi.espoo.evaka.application.persistence.daycare.DaycareFormV0
import fi.espoo.evaka.clubTerm2021
import fi.espoo.evaka.daycare.domain.Language
import fi.espoo.evaka.emailclient.Email
import fi.espoo.evaka.emailclient.MockEmailClient
import fi.espoo.evaka.insertApplication
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.preschoolTerm2021
import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.CitizenAuthLevel
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.asUser
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.dev.insertTestApplication
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.RealEvakaClock
import fi.espoo.evaka.test.validClubApplication
import fi.espoo.evaka.test.validDaycareApplication
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testChild_6
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testSvebiDaycare
import fi.espoo.evaka.vtjclient.service.persondetails.MockPersonDetailsService
import fi.espoo.evaka.vtjclient.service.persondetails.legacyMockVtjDataset
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
        db.transaction { tx ->
            tx.insertGeneralTestFixtures()
            listOf(testChild_1, testChild_6).forEach { tx.insert(it, DevPersonType.CHILD) }
            tx.insert(clubTerm2021)
        }
        MockPersonDetailsService.add(legacyMockVtjDataset())
    }

    @Test
    fun `email is sent after end user sends application`() {
        val applicationId =
            db.transaction { tx ->
                tx.insertTestApplication(
                    childId = testChild_1.id,
                    guardianId = guardian.id,
                    status = ApplicationStatus.CREATED,
                    type = ApplicationType.DAYCARE,
                    document =
                        validDaycareForm.copy(
                            guardian = guardianAsDaycareAdult,
                            apply =
                                validDaycareForm.apply.copy(preferredUnits = listOf(testDaycare.id))
                        )
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

        asyncJobRunner.runPendingJobsSync(RealEvakaClock())

        val sentMails = MockEmailClient.emails
        assertEquals(1, sentMails.size)

        val sentMail = sentMails[0]
        assertEquals(guardian.email, sentMail.toAddress)
        assertEquals(guardian.id.toString(), sentMail.traceId)
        assertEquals(
            "Olemme vastaanottaneet hakemuksenne / Vi har tagit emot din ansökan / We have received your application",
            sentMail.content.subject
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
                    type = ApplicationType.DAYCARE,
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

        val (_, res, _) =
            http
                .post("/citizen/applications/$applicationId/actions/send-application")
                .withMockedTime(mockedTime)
                .asUser(endUser)
                .response()

        assertEquals(200, res.statusCode)

        assertApplicationIsSent(applicationId)
        asyncJobRunner.runPendingJobsSync(RealEvakaClock())

        val sentMails = MockEmailClient.emails
        assertEquals(1, sentMails.size)

        val sentMail = sentMails[0]
        assertEquals("Test email sender sv <testemail_sv@test.com>", sentMail.fromAddress)
        assertEquals(guardian.email, sentMail.toAddress)
        assertEquals(guardian.id.toString(), sentMail.traceId)
        assertEquals(
            "Olemme vastaanottaneet hakemuksenne / Vi har tagit emot din ansökan / We have received your application",
            sentMail.content.subject
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
                    type = ApplicationType.DAYCARE,
                    document =
                        validDaycareForm.copy(
                            guardian = guardianAsDaycareAdult,
                            apply =
                                validDaycareForm.apply.copy(preferredUnits = listOf(testDaycare.id))
                        )
                )
            }
        val (_, res, _) =
            http
                .post("/v2/applications/$applicationId/actions/send-application")
                .withMockedTime(mockedTime)
                .asUser(serviceWorker)
                .response()

        assertEquals(200, res.statusCode)
        assertApplicationIsSent(applicationId)

        asyncJobRunner.runPendingJobsSync(RealEvakaClock())

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
                    type = ApplicationType.CLUB,
                    document = validClubForm
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

        asyncJobRunner.runPendingJobsSync(RealEvakaClock())

        val sentMails = MockEmailClient.emails

        assertEquals(1, sentMails.size)

        val sentMail = sentMails.first()
        assertEquals(guardian.id.toString(), sentMail.traceId)
        assertEquals(
            "Olemme vastaanottaneet hakemuksenne / Vi har tagit emot din ansökan / We have received your application",
            sentMail.content.subject
        )
    }

    @Test
    fun `email is sent after sending preschool application`() {
        val applicationId = ApplicationId(UUID.randomUUID())
        db.transaction { tx ->
            tx.insert(preschoolTerm2021)
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

        asyncJobRunner.runPendingJobsSync(RealEvakaClock())

        val sentMails = MockEmailClient.emails

        assertEquals(1, sentMails.size)

        val sentMail = sentMails.first()
        assertEquals(guardian.id.toString(), sentMail.traceId)
        assertEquals(
            "Olemme vastaanottaneet hakemuksenne / Vi har tagit emot din ansökan / We have received your application",
            sentMail.content.subject
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
                    type = ApplicationType.DAYCARE,
                    document =
                        validDaycareForm.copy(
                            guardian = guardianAsDaycareAdult,
                            apply =
                                validDaycareForm.apply.copy(preferredUnits = listOf(testDaycare.id))
                        )
                )
            }
        val (_, res, _) =
            http
                .post("/v2/applications/$applicationId/actions/send-application")
                .withMockedTime(mockedTime)
                .asUser(serviceWorker)
                .response()

        assertEquals(200, res.statusCode)
        assertApplicationIsSent(applicationId)
        asyncJobRunner.runPendingJobsSync(RealEvakaClock())

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
                    type = ApplicationType.DAYCARE,
                    document =
                        validDaycareForm.copy(
                            guardian = guardianAsDaycareAdult.copy(email = "not@valid"),
                            apply =
                                validDaycareForm.apply.copy(preferredUnits = listOf(testDaycare.id))
                        )
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
                    type = ApplicationType.DAYCARE,
                    document = validDaycareForm
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

        asyncJobRunner.runPendingJobsSync(RealEvakaClock())

        val result = db.read { r -> r.fetchApplicationDetails(applicationId) }
        assertEquals(manuallySetSentDate, result?.sentDate)
    }

    @Test
    fun `valid email is sent`() {
        setPersonEmail(testAdult_1.id, "working@test.fi")
        applicationReceivedEmailService.sendApplicationEmail(
            db,
            testAdult_1.id,
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

        setPersonEmail(testAdult_1.id, "Working.Email@Test.Com")
        applicationReceivedEmailService.sendApplicationEmail(
            db,
            testAdult_1.id,
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

    private fun setPersonEmail(personId: PersonId, email: String) {
        db.transaction { tx ->
            tx.createUpdate {
                    sql("UPDATE person SET email = ${bind(email)} where id = ${bind(personId)}")
                }
                .execute()
        }
    }

    private fun assertEmail(
        email: Email?,
        expectedToAddress: String,
        expectedFromAddress: String,
        expectedSubject: String,
        expectedHtmlPart: String,
        expectedTextPart: String
    ) {
        assertNotNull(email)
        assertEquals(expectedToAddress, email.toAddress)
        assertEquals(expectedFromAddress, email.fromAddress)
        assertEquals(expectedSubject, email.content.subject)
        assert(email.content.html.contains(expectedHtmlPart, true))
        assert(email.content.text.contains(expectedTextPart, true))
    }

    private fun assertApplicationIsSent(applicationId: ApplicationId) {
        db.read {
            assertEquals(ApplicationStatus.SENT, it.fetchApplicationDetails(applicationId)!!.status)
        }
    }
}
