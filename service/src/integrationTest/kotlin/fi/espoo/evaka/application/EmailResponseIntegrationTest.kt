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
import fi.espoo.evaka.emailclient.MockEmailClient
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.asUser
import fi.espoo.evaka.shared.config.Roles
import fi.espoo.evaka.shared.db.handle
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.insertTestApplication
import fi.espoo.evaka.shared.dev.insertTestApplicationForm
import fi.espoo.evaka.shared.dev.insertTestClubApplicationForm
import fi.espoo.evaka.shared.dev.insertTestDaycare
import fi.espoo.evaka.test.validClubApplication
import fi.espoo.evaka.test.validDaycareApplication
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testAreaId
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testSvebiDaycare
import fi.espoo.evaka.testVoucherDaycare
import org.jdbi.v3.core.Handle
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.util.UUID

class EmailResponseIntegrationTest : FullApplicationTest() {
    @Autowired
    lateinit var asyncJobRunner: AsyncJobRunner

    private val validDaycareForm = DaycareFormV0.fromApplication2(validDaycareApplication)
    private val validClubForm = ClubFormV0.fromForm2(validClubApplication.form, false, false)

    private val serviceWorker = AuthenticatedUser(testAdult_1.id, setOf(Roles.SERVICE_WORKER))
    private val endUser = AuthenticatedUser(testAdult_1.id, setOf(Roles.END_USER))
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
        jdbi.handle { h ->
            resetDatabase(h)
            insertGeneralTestFixtures(h)
            h.insertTestDaycare(DevDaycare(areaId = testAreaId, id = testSvebiDaycare.id, name = testSvebiDaycare.name, language = Language.sv))
        }
        MockEmailClient.applicationEmails.clear()
    }

    @Test
    fun `email is send after end user sends application`() {
        jdbi.handle { h ->
            val applicationId = insertTestApplication(h = h, childId = testChild_1.id, guardianId = guardian.id, status = ApplicationStatus.CREATED)
            insertTestApplicationForm(
                h = h,
                applicationId = applicationId,
                document = validDaycareForm.copy(
                    guardian = guardianAsDaycareAdult,
                    apply = validDaycareForm.apply.copy(
                        preferredUnits = listOf(testDaycare.id)
                    )
                )
            )

            val (_, res, _) = http.post("/enduser/v2/applications/$applicationId/actions/send-application")
                .asUser(endUser)
                .response()

            assertEquals(204, res.statusCode)
            assertApplicationIsSent(h, applicationId)
            assertEquals(1, asyncJobRunner.getPendingJobCount())

            asyncJobRunner.runPendingJobsSync()

            val sentMails = MockEmailClient.applicationEmails
            assertEquals(1, sentMails.size)

            val sentMail = sentMails[0]
            assertEquals(guardian.email, sentMail.toAddress)
            assertEquals(guardian.id, sentMail.personId)
            assertEquals("fi", sentMail.language)
        }
    }

    @Test
    fun `swedish email is sent after end user sends application to svebi daycare`() {
        jdbi.handle { h ->
            val applicationId = insertTestApplication(h = h, childId = testChild_1.id, guardianId = guardian.id, status = ApplicationStatus.CREATED)
            insertTestApplicationForm(
                h = h,
                applicationId = applicationId,
                document = validDaycareForm.copy(
                    guardian = guardianAsDaycareAdult,
                    apply = validDaycareForm.apply.copy(
                        preferredUnits = listOf(testSvebiDaycare.id)
                    )
                )
            )

            val (_, res, _) = http.post("/enduser/v2/applications/$applicationId/actions/send-application")
                .asUser(endUser)
                .response()

            assertEquals(204, res.statusCode)
            assertApplicationIsSent(h, applicationId)
            asyncJobRunner.runPendingJobsSync(1)

            val sentMails = MockEmailClient.applicationEmails
            assertEquals(1, sentMails.size)

            val sentMail = sentMails[0]
            assertEquals(guardian.email, sentMail.toAddress)
            assertEquals(guardian.id, sentMail.personId)
            assertEquals("sv", sentMail.language)
        }
    }

    @Test
    fun `email is sent after service worker sends application`() {
        jdbi.handle { h ->
            val applicationId = insertTestApplication(
                h = h,
                childId = testChild_1.id,
                guardianId = guardian.id,
                status = ApplicationStatus.CREATED,
                hideFromGuardian = false
            )
            insertTestApplicationForm(
                h = h,
                applicationId = applicationId,
                document = validDaycareForm.copy(
                    guardian = guardianAsDaycareAdult,
                    apply = validDaycareForm.apply.copy(
                        preferredUnits = listOf(testDaycare.id)
                    )
                )
            )
            val (_, res, _) = http.post("/v2/applications/$applicationId/actions/send-application")
                .asUser(serviceWorker)
                .response()

            assertEquals(204, res.statusCode)
            assertApplicationIsSent(h, applicationId)
            assertEquals(1, asyncJobRunner.getPendingJobCount())

            asyncJobRunner.runPendingJobsSync(1)
            assertEquals(0, asyncJobRunner.getPendingJobCount())

            val sentMails = MockEmailClient.applicationEmails
            assertEquals(1, sentMails.size)

            val sentMail = sentMails[0]
            assertEquals(guardian.email, sentMail.toAddress)
            assertEquals(guardian.id, sentMail.personId)
        }
    }

    @Test
    fun `email is not sent after sending club application`() {
        jdbi.handle { h ->
            val applicationId = insertTestApplication(h = h, childId = testChild_1.id, guardianId = testAdult_1.id, status = ApplicationStatus.CREATED)
            insertTestClubApplicationForm(
                h = h,
                applicationId = applicationId,
                document = validClubForm
            )

            val (_, res, _) = http.post("/enduser/v2/applications/$applicationId/actions/send-application")
                .asUser(endUser)
                .response()

            assertEquals(204, res.statusCode)
            assertApplicationIsSent(h, applicationId)
            assertEquals(0, asyncJobRunner.getPendingJobCount())

            val sentMails = MockEmailClient.applicationEmails

            assertEquals(0, sentMails.size)
        }
    }

    @Test
    fun `email is not sent when provider type of preferred unit is private voucher`() {
        jdbi.handle { h ->
            val applicationId = insertTestApplication(h = h, childId = testChild_1.id, guardianId = guardian.id, status = ApplicationStatus.CREATED)
            insertTestApplicationForm(
                h = h,
                applicationId = applicationId,
                document = validDaycareForm.copy(
                    guardian = guardianAsDaycareAdult,
                    apply = validDaycareForm.apply.copy(
                        preferredUnits = listOf(testVoucherDaycare.id)
                    )
                )
            )

            val (_, res, _) = http.post("/enduser/v2/applications/$applicationId/actions/send-application")
                .asUser(endUser)
                .response()

            assertEquals(204, res.statusCode)
            assertApplicationIsSent(h, applicationId)
            assertEquals(0, asyncJobRunner.getPendingJobCount())
            assertEquals(0, MockEmailClient.applicationEmails.size)
        }
    }

    @Test
    fun `email is not sent when service workers sends hidden application`() {
        jdbi.handle { h ->
            val applicationId = insertTestApplication(
                h = h,
                childId = testChild_1.id,
                guardianId = guardian.id,
                status = ApplicationStatus.CREATED,
                hideFromGuardian = true
            )
            insertTestApplicationForm(
                h = h,
                applicationId = applicationId,
                document = validDaycareForm.copy(
                    guardian = guardianAsDaycareAdult,
                    apply = validDaycareForm.apply.copy(
                        preferredUnits = listOf(testDaycare.id)
                    )
                )
            )
            val (_, res, _) = http.post("/v2/applications/$applicationId/actions/send-application")
                .asUser(serviceWorker)
                .response()

            assertEquals(204, res.statusCode)
            assertApplicationIsSent(h, applicationId)
            assertEquals(0, asyncJobRunner.getPendingJobCount())

            val sentMails = MockEmailClient.applicationEmails
            assertEquals(0, sentMails.size)
        }
    }

    @Test
    fun `email is not sent to invalid email`() {
        jdbi.handle { h ->
            val applicationId = insertTestApplication(h = h, childId = testChild_1.id, guardianId = testAdult_1.id, status = ApplicationStatus.CREATED)
            insertTestApplicationForm(
                h = h,
                applicationId = applicationId,
                document = validDaycareForm.copy(
                    guardian = guardianAsDaycareAdult.copy(email = "not@valid"),
                    apply = validDaycareForm.apply.copy(
                        preferredUnits = listOf(testDaycare.id)
                    )
                )
            )

            val (_, res, _) = http.post("/enduser/v2/applications/$applicationId/actions/send-application")
                .asUser(endUser)
                .response()

            assertEquals(204, res.statusCode)
            assertApplicationIsSent(h, applicationId)
            assertEquals(1, asyncJobRunner.getPendingJobCount())

            asyncJobRunner.runPendingJobsSync()

            val sentMails = MockEmailClient.applicationEmails
            assertEquals(0, sentMails.size)
        }
    }

    @Test
    fun `application keeps its manually set sent date after it is sent`() {
        jdbi.handle { h ->
            val manuallySetSentDate = LocalDate.of(2020, 1, 1)
            val applicationId = insertTestApplication(
                h = h,
                childId = testChild_1.id,
                guardianId = testAdult_1.id,
                status = ApplicationStatus.CREATED,
                sentDate = manuallySetSentDate
            )
            insertTestApplicationForm(
                h = h,
                applicationId = applicationId,
                document = validDaycareForm
            )

            val (_, res, _) = http.post("/enduser/v2/applications/$applicationId/actions/send-application")
                .asUser(endUser)
                .response()

            assertEquals(204, res.statusCode)
            assertApplicationIsSent(h, applicationId)
            assertEquals(1, asyncJobRunner.getPendingJobCount())

            asyncJobRunner.runPendingJobsSync()

            val result = fetchApplicationDetails(h, applicationId)
            assertEquals(manuallySetSentDate, result?.sentDate)
        }
    }

    private fun assertApplicationIsSent(h: Handle, applicationId: UUID) {
        assertEquals(ApplicationStatus.SENT, fetchApplicationDetails(h, applicationId)!!.status)
    }
}
