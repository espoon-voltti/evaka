// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.pis.controller

import evaka.core.FullApplicationTest
import evaka.core.Sensitive
import evaka.core.emailclient.MockEmailClient
import evaka.core.pis.PersonalDataUpdate
import evaka.core.pis.SystemController
import evaka.core.pis.controllers.CONFIRMATION_CODE_LENGTH
import evaka.core.pis.controllers.PersonalDataControllerCitizen
import evaka.core.pis.updatePersonalDetails
import evaka.core.shared.PersonId
import evaka.core.shared.async.AsyncJob
import evaka.core.shared.async.AsyncJobRunner
import evaka.core.shared.auth.*
import evaka.core.shared.dev.DevCitizenUser
import evaka.core.shared.dev.DevPerson
import evaka.core.shared.dev.DevPersonType
import evaka.core.shared.dev.insert
import evaka.core.shared.domain.BadRequest
import evaka.core.shared.domain.Conflict
import evaka.core.shared.domain.Forbidden
import evaka.core.shared.domain.MockEvakaClock
import evaka.core.shared.domain.RealEvakaClock
import evaka.core.vtjclient.service.persondetails.MockPersonDetailsService
import kotlin.test.assertContains
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired

class WeakCredentialsIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var controller: PersonalDataControllerCitizen
    @Autowired private lateinit var systemController: SystemController
    @Autowired private lateinit var passwordService: PasswordService
    @Autowired private lateinit var asyncJobRunner: AsyncJobRunner<AsyncJob>

    private val clock = MockEvakaClock(2024, 1, 1, 12, 0)

    private val ssn = "010107A995B"
    private val email = "verified@example.com"
    private val person = DevPerson(email = email, verifiedEmail = email, ssn = ssn)
    private val user = person.user(CitizenAuthLevel.STRONG)

    @Test
    fun `a person without a verified email cannot activate weak credentials`() {
        db.transaction { tx -> tx.insert(person.copy(verifiedEmail = null), DevPersonType.ADULT) }
        MockPersonDetailsService.addPersons(person)

        citizenStrongLogin()
        val error =
            assertThrows<BadRequest> {
                updateWeakLoginCredentials(password = Sensitive("test123123"))
            }
        assertEquals("Verified email is required", error.message)
    }

    @Test
    fun `a person with a verified email can activate weak credentials and log in`() {
        db.transaction { tx -> tx.insert(person, DevPersonType.ADULT) }
        MockPersonDetailsService.addPersons(person)

        citizenStrongLogin()
        val password = Sensitive("test123123")
        updateWeakLoginCredentials(password = password)

        asyncJobRunner.runPendingJobsSync(RealEvakaClock())
        assertEquals(0, MockEmailClient.emails.size)

        val identity = citizenWeakLogin(username = email, password = password)
        assertEquals(person.id, identity.id)
    }

    @Test
    fun `username is derived from verified email and converted to lowercase automatically`() {
        val mixedCaseEmail = "Verified@Example.Com"
        db.transaction { tx ->
            tx.insert(
                person.copy(email = mixedCaseEmail, verifiedEmail = mixedCaseEmail),
                DevPersonType.ADULT,
            )
        }
        MockPersonDetailsService.addPersons(person)

        citizenStrongLogin()
        val password = Sensitive("test123123")
        updateWeakLoginCredentials(password = password)

        asyncJobRunner.runPendingJobsSync(RealEvakaClock())
        assertEquals(0, MockEmailClient.emails.size)

        val identity = citizenWeakLogin(username = mixedCaseEmail.lowercase(), password = password)
        assertEquals(person.id, identity.id)
    }

    @Test
    fun `a person can update their password and log in with the new password`() {
        val password = Sensitive("test123123")
        db.transaction { tx ->
            tx.insert(person, DevPersonType.ADULT)
            tx.insert(citizenUser(person.id, email, password))
        }
        MockPersonDetailsService.addPersons(person)

        val newPassword = Sensitive("test256256")
        updateWeakLoginCredentials(password = newPassword)

        asyncJobRunner.runPendingJobsSync(RealEvakaClock())
        assertEquals(1, MockEmailClient.emails.size)
        assertContains(
            MockEmailClient.emails.first().content.subject,
            "eVaka-salasanasi on vaihdettu",
        )

        val identity = citizenWeakLogin(username = email, password = newPassword)
        assertEquals(person.id, identity.id)
    }

    @Test
    fun `a person can't use a password that does not match constraints`() {
        db.transaction { tx -> tx.insert(person, DevPersonType.ADULT) }
        MockPersonDetailsService.addPersons(person)

        citizenStrongLogin()
        val password = Sensitive("nope")
        val error = assertThrows<BadRequest> { updateWeakLoginCredentials(password = password) }
        assertEquals("PASSWORD_FORMAT", error.errorCode)

        asyncJobRunner.runPendingJobsSync(RealEvakaClock())
        assertEquals(0, MockEmailClient.emails.size)
    }

    @Test
    fun `a person can't use a password that is blacklisted`() {
        val password = Sensitive("ValidButBlacklisted123!!")
        db.transaction { tx ->
            tx.insert(person, DevPersonType.ADULT)
            tx.upsertPasswordBlacklist(
                PasswordBlacklistSource("test", clock.now()),
                sequenceOf(password.value),
            )
        }
        MockPersonDetailsService.addPersons(person)

        citizenStrongLogin()
        val error = assertThrows<BadRequest> { updateWeakLoginCredentials(password = password) }
        assertEquals("PASSWORD_UNACCEPTABLE", error.errorCode)

        asyncJobRunner.runPendingJobsSync(RealEvakaClock())
        assertEquals(0, MockEmailClient.emails.size)
    }

    @Test
    fun `a person cannot login with wrong password`() {
        val correctPassword = Sensitive("correct123")
        val wrongPassword = Sensitive("wrong456")

        db.transaction { tx ->
            tx.insert(person, DevPersonType.ADULT)
            tx.insert(citizenUser(person.id, email, correctPassword))
        }
        MockPersonDetailsService.addPersons(person)

        assertThrows<Forbidden> { citizenWeakLogin(username = email, password = wrongPassword) }
    }

    @Test
    fun `notification email is sent when logging in from a new device`() {
        whenever(evakaEnv.newBrowserLoginEmailEnabled).thenReturn(true)

        val password = Sensitive("test123123")
        db.transaction { tx ->
            tx.insert(person, DevPersonType.ADULT)
            tx.insert(citizenUser(person.id, email, password))
        }
        MockPersonDetailsService.addPersons(person)

        // Login with empty deviceAuthHistory (simulating new device)
        val identity =
            citizenWeakLogin(
                username = email,
                password = password,
                previouslyAuthenticatedUserHashes = emptyList(),
            )
        assertEquals(person.id, identity.id)

        asyncJobRunner.runPendingJobsSync(RealEvakaClock())
        assertEquals(1, MockEmailClient.emails.size)
        val notificationEmail = MockEmailClient.emails.first()
        assertEquals(email, notificationEmail.toAddress)
        assertContains(
            notificationEmail.content.subject,
            "Kirjautuminen uudella laitteella eVakaan",
        )
    }

    @Test
    fun `no notification email is sent when logging in from a previously used device`() {
        whenever(evakaEnv.newBrowserLoginEmailEnabled).thenReturn(true)

        val password = Sensitive("test123123")
        db.transaction { tx ->
            tx.insert(person, DevPersonType.ADULT)
            tx.insert(citizenUser(person.id, email, password))
        }
        MockPersonDetailsService.addPersons(person)

        // Generate the expected user hash for this person
        val userIdHash =
            com.google.common.hash.Hashing.sha256()
                .hashString(person.id.toString(), java.nio.charset.StandardCharsets.UTF_8)
                .toString()

        // Login with the user hash in deviceAuthHistory (simulating known
        // device)
        val identity =
            citizenWeakLogin(
                username = email,
                password = password,
                previouslyAuthenticatedUserHashes = listOf(userIdHash),
            )
        assertEquals(person.id, identity.id)

        asyncJobRunner.runPendingJobsSync(RealEvakaClock())
        assertEquals(0, MockEmailClient.emails.size)
    }

    @Test
    fun `username is synced when a person with weak credentials verifies a new email`() {
        val password = Sensitive("test123123")
        db.transaction { tx ->
            tx.insert(person, DevPersonType.ADULT)
            tx.insert(citizenUser(person.id, email, password))
        }
        MockPersonDetailsService.addPersons(person)

        val newEmail = "newemail@example.com"
        db.transaction { tx ->
            tx.updatePersonalDetails(
                person.id,
                PersonalDataUpdate(
                    preferredName = person.firstName,
                    phone = person.phone,
                    backupPhone = person.backupPhone,
                    email = newEmail,
                ),
            )
        }
        sendEmailVerificationCode()
        val verificationCode = getVerificationCodeFromEmail(newEmail)
        val verification = getEmailVerificationStatus().latestVerification!!
        verifyEmail(
            PersonalDataControllerCitizen.EmailVerificationRequest(
                verification.id,
                verificationCode,
            )
        )

        val identity = citizenWeakLogin(username = newEmail, password = password)
        assertEquals(person.id, identity.id)

        assertThrows<Forbidden> { citizenWeakLogin(username = email, password = password) }
    }

    @Test
    fun `email verification fails with a conflict if the new email is already another person's weak login username`() {
        val password = Sensitive("test123123")
        val otherPassword = Sensitive("test12341234")
        val otherPersonEmail = "other@example.com"
        val otherPerson =
            DevPerson(
                email = otherPersonEmail,
                verifiedEmail = otherPersonEmail,
                firstName = "Other",
                lastName = "Person",
            )
        val otherUser = otherPerson.user(CitizenAuthLevel.STRONG)

        db.transaction { tx ->
            tx.insert(person, DevPersonType.ADULT)
            tx.insert(citizenUser(person.id, email, password))

            tx.insert(otherPerson, DevPersonType.ADULT)
            tx.insert(citizenUser(otherPerson.id, otherPersonEmail, otherPassword))

            tx.updatePersonalDetails(
                otherPerson.id,
                PersonalDataUpdate(
                    preferredName = otherPerson.firstName,
                    phone = otherPerson.phone,
                    backupPhone = otherPerson.backupPhone,
                    email = email,
                ),
            )
        }
        MockPersonDetailsService.addPersons(person)

        sendEmailVerificationCode(otherUser)
        val verificationCode = getVerificationCodeFromEmail(email)
        val verification = getEmailVerificationStatus(otherUser).latestVerification!!

        assertThrows<Conflict> {
            verifyEmail(
                PersonalDataControllerCitizen.EmailVerificationRequest(
                    verification.id,
                    verificationCode,
                ),
                otherUser,
            )
        }

        val aIdentity = citizenWeakLogin(username = email, password = password)
        assertEquals(person.id, aIdentity.id)

        assertThrows<Forbidden> { citizenWeakLogin(username = email, password = otherPassword) }
        val bIdentity = citizenWeakLogin(username = otherPersonEmail, password = otherPassword)
        assertEquals(otherPerson.id, bIdentity.id)
    }

    private fun updateWeakLoginCredentials(password: Sensitive<String>) =
        controller.updateWeakLoginCredentials(
            dbInstance(),
            user,
            clock,
            PersonalDataControllerCitizen.UpdateWeakLoginCredentialsRequest(password),
        )

    private fun citizenStrongLogin() =
        systemController.citizenLogin(
            dbInstance(),
            AuthenticatedUser.SystemInternalUser,
            clock,
            SystemController.CitizenLoginRequest(
                socialSecurityNumber = ssn,
                firstName = person.firstName,
                lastName = person.lastName,
            ),
        )

    private fun citizenWeakLogin(
        username: String,
        password: Sensitive<String>,
        previouslyAuthenticatedUserHashes: List<String> = emptyList(),
    ) =
        systemController.citizenWeakLogin(
            dbInstance(),
            AuthenticatedUser.SystemInternalUser,
            clock,
            SystemController.CitizenWeakLoginRequest(
                username,
                password,
                previouslyAuthenticatedUserHashes,
            ),
        )

    private fun citizenUser(id: PersonId, email: String, password: Sensitive<String>) =
        DevCitizenUser(
            id,
            username = email,
            usernameUpdatedAt = clock.now(),
            password = passwordService.encode(password),
            passwordUpdatedAt = clock.now(),
        )

    private fun sendEmailVerificationCode(citizen: AuthenticatedUser.Citizen = user) {
        controller.sendEmailVerificationCode(dbInstance(), citizen, clock)
        asyncJobRunner.runPendingJobsSync(clock)
    }

    private fun getEmailVerificationStatus(citizen: AuthenticatedUser.Citizen = user) =
        controller.getEmailVerificationStatus(dbInstance(), citizen, clock)

    private fun getVerificationCodeFromEmail(emailAddress: String): String =
        MockEmailClient.getEmail(emailAddress)?.let { email ->
            Regex("[0-9]{$CONFIRMATION_CODE_LENGTH}").find(email.content.text)?.value
        } ?: error("No verification code found for $emailAddress")

    private fun verifyEmail(
        request: PersonalDataControllerCitizen.EmailVerificationRequest,
        citizen: AuthenticatedUser.Citizen = user,
    ) = controller.verifyEmail(dbInstance(), citizen, clock, request)
}
