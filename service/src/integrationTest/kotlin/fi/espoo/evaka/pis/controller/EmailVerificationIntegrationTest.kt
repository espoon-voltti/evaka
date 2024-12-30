// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis.controller

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.emailclient.MockEmailClient
import fi.espoo.evaka.pis.controllers.EMAIL_VERIFICATION_CODE_DURATION
import fi.espoo.evaka.pis.controllers.EMAIL_VERIFICATION_CODE_LENGTH
import fi.espoo.evaka.pis.controllers.PersonalDataControllerCitizen
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.CitizenAuthLevel
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.MockEvakaClock
import kotlin.test.*
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired

class EmailVerificationIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var controller: PersonalDataControllerCitizen
    @Autowired private lateinit var asyncJobRunner: AsyncJobRunner<AsyncJob>

    private val clock = MockEvakaClock(2024, 1, 1, 12, 0)
    private val email = "tobeverified@example.com"
    private val person = DevPerson(email = email)
    private val user = person.user(CitizenAuthLevel.STRONG)

    @Test
    fun `email verification status returns both email addresses correctly`() {
        val verifiedEmail = "alreadyverified@example.com"
        db.transaction { tx ->
            tx.insert(person.copy(verifiedEmail = verifiedEmail), DevPersonType.ADULT)
        }
        assertEquals(
            PersonalDataControllerCitizen.EmailVerificationStatusResponse(
                email = email,
                verifiedEmail = verifiedEmail,
                latestVerification = null,
            ),
            getStatus(),
        )
    }

    @Test
    fun `an email address can be verified`() {
        db.transaction { tx -> tx.insert(person, DevPersonType.ADULT) }

        sendEmailVerificationCode()
        val verification =
            getStatus().let { status ->
                assertEquals(email, status.email)
                assertNull(status.verifiedEmail)

                assertNotNull(status.latestVerification)
            }
        assertEquals(email, verification.email)
        assertNotNull(verification.sentAt)

        val verificationCode = assertNotNull(getVerificationCodeFromEmail(email))
        verifyEmail(
            PersonalDataControllerCitizen.EmailVerificationRequest(
                verification.id,
                verificationCode,
            )
        )
        getStatus().let { status ->
            assertEquals(email, status.email)
            assertEquals(email, status.verifiedEmail)
            assertNull(status.latestVerification)
        }
    }

    @Test
    fun `a verification code is sent only once until it expires`() {
        db.transaction { tx -> tx.insert(person, DevPersonType.ADULT) }

        sendEmailVerificationCode()
        assertNotNull(getVerificationCodeFromEmail(email))

        MockEmailClient.clear()
        sendEmailVerificationCode()
        assertNull(getVerificationCodeFromEmail(email))
    }

    @Test
    fun `a verification code can expire, and can be resent after expiration`() {
        db.transaction { tx -> tx.insert(person, DevPersonType.ADULT) }

        sendEmailVerificationCode()
        val verification = assertNotNull(getStatus().latestVerification)
        val verificationCode = assertNotNull(getVerificationCodeFromEmail(email))

        clock.tick(EMAIL_VERIFICATION_CODE_DURATION.plusMinutes(1))
        assertNull(getStatus().latestVerification)
        assertThrows<BadRequest> {
            verifyEmail(
                PersonalDataControllerCitizen.EmailVerificationRequest(
                    verification.id,
                    verificationCode,
                )
            )
        }

        MockEmailClient.clear()
        sendEmailVerificationCode()
        val resentVerification = assertNotNull(getStatus().latestVerification)
        assertTrue(verification.expiresAt < resentVerification.expiresAt)
        assertTrue(assertNotNull(verification.sentAt) < assertNotNull(resentVerification.sentAt))
    }

    private fun getVerificationCodeFromEmail(emailAddress: String) =
        MockEmailClient.getEmail(emailAddress)?.let { email ->
            Regex("[0-9]{$EMAIL_VERIFICATION_CODE_LENGTH}").find(email.content.text)?.value
        }

    private fun sendEmailVerificationCode() {
        controller.sendEmailVerificationCode(dbInstance(), user, clock)
        asyncJobRunner.runPendingJobsSync(clock)
    }

    private fun getStatus() = controller.getEmailVerificationStatus(dbInstance(), user, clock)

    private fun verifyEmail(request: PersonalDataControllerCitizen.EmailVerificationRequest) =
        controller.verifyEmail(dbInstance(), user, clock, request)
}
