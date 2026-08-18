// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.pis.controller

import com.google.common.hash.Hashing
import evaka.core.FullApplicationTest
import evaka.core.emailclient.MockEmailClient
import evaka.core.pis.CitizenUserIdentity
import evaka.core.pis.SystemController
import evaka.core.pis.controllers.MAX_PASSKEYS_PER_CITIZEN
import evaka.core.pis.controllers.PasskeyControllerCitizen
import evaka.core.shared.async.AsyncJob
import evaka.core.shared.async.AsyncJobRunner
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.auth.CitizenAuthLevel
import evaka.core.shared.dev.DevPerson
import evaka.core.shared.dev.DevPersonType
import evaka.core.shared.dev.insert
import evaka.core.shared.domain.BadRequest
import evaka.core.shared.domain.Conflict
import evaka.core.shared.domain.Forbidden
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.shared.domain.MockEvakaClock
import evaka.core.shared.domain.NotFound
import evaka.core.shared.domain.RealEvakaClock
import evaka.core.user.CitizenPasskey
import evaka.core.user.DeviceClass
import evaka.core.user.getCitizenPasskeys
import evaka.core.vtjclient.service.persondetails.MockPersonDetailsService
import java.nio.charset.StandardCharsets
import java.time.Duration
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired

class PasskeyIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var controller: PasskeyControllerCitizen
    @Autowired private lateinit var systemController: SystemController
    @Autowired private lateinit var asyncJobRunner: AsyncJobRunner<AsyncJob>

    private val clock = MockEvakaClock(2024, 1, 1, 12, 0)

    private val userAgent =
        "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 " +
            "(KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1"

    private val email = "verified@example.com"
    private val person = DevPerson(email = email, verifiedEmail = email, ssn = "010107A995B")
    private val strongUser = person.user(CitizenAuthLevel.STRONG)
    private val weakUser = person.user(CitizenAuthLevel.WEAK)

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx -> tx.insert(person, DevPersonType.ADULT) }
        MockPersonDetailsService.addPersons(person)
    }

    @Test
    fun `a citizen can register a passkey and log in with it`() {
        val authenticator = SoftwareAuthenticator()
        val passkey = registerPasskey(authenticator, name = "My phone")

        asyncJobRunner.runPendingJobsSync(RealEvakaClock())
        assertEquals(1, MockEmailClient.emails.size)
        assertContains(
            MockEmailClient.emails.first().content.subject,
            "eVaka-tilillesi on lisätty pääsyavain",
        )

        val identity = passkeyLogin(authenticator)
        assertEquals(person.id, identity.id)

        val passkeys = controller.getPasskeys(dbInstance(), weakUser, clock)
        assertEquals(listOf(passkey.id), passkeys.map { it.id })
        assertEquals("My phone", passkeys.single().name)
        assertEquals(clock.now(), passkeys.single().lastUsedAt)
        assertEquals(DeviceClass.PHONE, passkeys.single().deviceClass)
        assertEquals("iOS", passkeys.single().operatingSystemName)
        assertEquals("Safari", passkeys.single().agentName)
    }

    @Test
    fun `a passkey-only citizen without weak credentials can log in`() {
        assertNull(getCitizenUserRow())

        val authenticator = SoftwareAuthenticator()
        registerPasskey(authenticator)

        val identity = passkeyLogin(authenticator)
        assertEquals(person.id, identity.id)
        assertEquals(clock.now(), getCitizenUserRow()?.lastWeakLogin)
        assertNull(getCitizenUserRow()?.username)
    }

    @Test
    fun `registration requires a strong session`() {
        assertThrows<Forbidden> {
            controller.startPasskeyRegistration(dbInstance(), weakUser, clock)
        }
    }

    @Test
    fun `a citizen can rename a passkey`() {
        val passkey = registerPasskey(SoftwareAuthenticator(), name = "Pääsyavain")

        controller.updatePasskeyName(
            dbInstance(),
            strongUser,
            clock,
            passkey.id,
            PasskeyControllerCitizen.UpdatePasskeyNameRequest("  My phone  "),
        )

        assertEquals("My phone", db.read { it.getCitizenPasskeys(person.id) }.single().name)
    }

    @Test
    fun `renaming an unknown passkey fails`() {
        val other = registerPasskey(SoftwareAuthenticator())
        controller.deletePasskey(dbInstance(), strongUser, clock, other.id)
        assertThrows<NotFound> {
            controller.updatePasskeyName(
                dbInstance(),
                strongUser,
                clock,
                other.id,
                PasskeyControllerCitizen.UpdatePasskeyNameRequest("New name"),
            )
        }
    }

    @Test
    fun `renaming requires a strong session`() {
        val passkey = registerPasskey(SoftwareAuthenticator())
        assertThrows<Forbidden> {
            controller.updatePasskeyName(
                dbInstance(),
                weakUser,
                clock,
                passkey.id,
                PasskeyControllerCitizen.UpdatePasskeyNameRequest("New name"),
            )
        }
    }

    @Test
    fun `a blank passkey name is rejected`() {
        assertThrows<BadRequest> { PasskeyControllerCitizen.UpdatePasskeyNameRequest("   ") }
    }

    @Test
    fun `deletion requires a strong session`() {
        val passkey = registerPasskey(SoftwareAuthenticator())
        assertThrows<Forbidden> {
            controller.deletePasskey(dbInstance(), weakUser, clock, passkey.id)
        }
    }

    @Test
    fun `listing passkeys is allowed for a weak session`() {
        registerPasskey(SoftwareAuthenticator())
        assertEquals(1, controller.getPasskeys(dbInstance(), weakUser, clock).size)
    }

    @Test
    fun `registration without user verification is rejected`() {
        val authenticator = SoftwareAuthenticator(userVerified = false)
        val options = controller.startPasskeyRegistration(dbInstance(), strongUser, clock)
        val credential = authenticator.create(options.credentialsCreate)
        assertThrows<BadRequest> {
            controller.finishPasskeyRegistration(
                dbInstance(),
                strongUser,
                clock,
                userAgent,
                PasskeyControllerCitizen.FinishPasskeyRegistrationRequest("Test", credential),
            )
        }
    }

    @Test
    fun `login without user verification is rejected`() {
        val authenticator = SoftwareAuthenticator()
        registerPasskey(authenticator)

        authenticator.userVerified = false
        assertThrows<Forbidden> { passkeyLogin(authenticator) }
    }

    @Test
    fun `login with an unknown credential is rejected`() {
        registerPasskey(SoftwareAuthenticator())
        assertThrows<Forbidden> { passkeyLogin(SoftwareAuthenticator()) }
    }

    @Test
    fun `login with a deleted credential is rejected and deletion sends an email`() {
        val authenticator = SoftwareAuthenticator()
        val passkey = registerPasskey(authenticator)
        assertEquals(person.id, passkeyLogin(authenticator).id)

        controller.deletePasskey(dbInstance(), strongUser, clock, passkey.id)

        asyncJobRunner.runPendingJobsSync(RealEvakaClock())
        assertContains(
            MockEmailClient.emails.map { it.content.subject }.last(),
            "eVaka-tililtäsi on poistettu pääsyavain",
        )

        assertThrows<Forbidden> { passkeyLogin(authenticator) }
        assertEquals(0, db.read { it.getCitizenPasskeys(person.id) }.size)
    }

    @Test
    fun `deleting an unknown passkey fails`() {
        val other = registerPasskey(SoftwareAuthenticator())
        controller.deletePasskey(dbInstance(), strongUser, clock, other.id)
        assertThrows<NotFound> {
            controller.deletePasskey(dbInstance(), strongUser, clock, other.id)
        }
    }

    @Test
    fun `the registration challenge is single-use`() {
        val authenticator = SoftwareAuthenticator()
        val options = controller.startPasskeyRegistration(dbInstance(), strongUser, clock)
        val credential = authenticator.create(options.credentialsCreate)

        val request = PasskeyControllerCitizen.FinishPasskeyRegistrationRequest("Test", credential)
        controller.finishPasskeyRegistration(dbInstance(), strongUser, clock, userAgent, request)
        assertThrows<NotFound> {
            controller.finishPasskeyRegistration(
                dbInstance(),
                strongUser,
                clock,
                userAgent,
                request,
            )
        }
    }

    @Test
    fun `the registration challenge expires`() {
        // Avoid mutating the clock instance shared between tests
        val clock2 = clock.copy()

        val authenticator = SoftwareAuthenticator()
        val options = controller.startPasskeyRegistration(dbInstance(), strongUser, clock2)
        val credential = authenticator.create(options.credentialsCreate)

        clock2.tick(Duration.ofMinutes(6))
        assertThrows<NotFound> {
            controller.finishPasskeyRegistration(
                dbInstance(),
                strongUser,
                clock2,
                userAgent,
                PasskeyControllerCitizen.FinishPasskeyRegistrationRequest("Test", credential),
            )
        }
    }

    @Test
    fun `the same authenticator cannot be registered twice`() {
        val authenticator = SoftwareAuthenticator()
        registerPasskey(authenticator)

        val options = controller.startPasskeyRegistration(dbInstance(), strongUser, clock)
        val credential = authenticator.create(options.credentialsCreate)
        assertThrows<BadRequest> {
            controller.finishPasskeyRegistration(
                dbInstance(),
                strongUser,
                clock,
                userAgent,
                PasskeyControllerCitizen.FinishPasskeyRegistrationRequest("Test", credential),
            )
        }
    }

    @Test
    fun `at most 10 passkeys can be registered`() {
        repeat(MAX_PASSKEYS_PER_CITIZEN) { registerPasskey(SoftwareAuthenticator()) }
        val error =
            assertThrows<Conflict> {
                controller.startPasskeyRegistration(dbInstance(), strongUser, clock)
            }
        assertEquals("PASSKEY_LIMIT", error.errorCode)
    }

    @Test
    fun `a non-incrementing signature counter does not block login`() {
        val authenticator = SoftwareAuthenticator(signCountStep = 0)
        registerPasskey(authenticator)
        assertEquals(person.id, passkeyLogin(authenticator).id)
        assertEquals(person.id, passkeyLogin(authenticator).id)
    }

    @Test
    fun `login from a new device sends an email`() {
        whenever(evakaEnv.newBrowserLoginEmailEnabled).thenReturn(true)
        val authenticator = SoftwareAuthenticator()
        registerPasskey(authenticator)
        asyncJobRunner.runPendingJobsSync(RealEvakaClock())
        MockEmailClient.clear()

        passkeyLogin(authenticator)
        asyncJobRunner.runPendingJobsSync(RealEvakaClock())
        assertContains(
            MockEmailClient.emails.map { it.content.subject }.single(),
            "Kirjautuminen uudella laitteella eVakaan",
        )
    }

    @Test
    fun `login from a known device does not send an email`() {
        whenever(evakaEnv.newBrowserLoginEmailEnabled).thenReturn(true)
        val authenticator = SoftwareAuthenticator()
        registerPasskey(authenticator)
        asyncJobRunner.runPendingJobsSync(RealEvakaClock())
        MockEmailClient.clear()

        val userIdHash =
            Hashing.sha256().hashString(person.id.toString(), StandardCharsets.UTF_8).toString()
        passkeyLogin(authenticator, deviceAuthHistory = listOf(userIdHash))
        asyncJobRunner.runPendingJobsSync(RealEvakaClock())
        assertEquals(0, MockEmailClient.emails.size)
    }

    private fun registerPasskey(
        authenticator: SoftwareAuthenticator,
        name: String = "Test passkey",
    ): CitizenPasskey {
        val options = controller.startPasskeyRegistration(dbInstance(), strongUser, clock)
        val credential = authenticator.create(options.credentialsCreate)
        return controller.finishPasskeyRegistration(
            dbInstance(),
            strongUser,
            clock,
            userAgent,
            PasskeyControllerCitizen.FinishPasskeyRegistrationRequest(name, credential),
        )
    }

    private fun passkeyLogin(
        authenticator: SoftwareAuthenticator,
        deviceAuthHistory: List<String> = emptyList(),
    ): CitizenUserIdentity {
        val options =
            systemController.citizenPasskeyLoginOptions(
                dbInstance(),
                AuthenticatedUser.SystemInternalUser,
                clock,
            )
        val credential = authenticator.get(options.credentialsGet)
        return systemController.citizenPasskeyLogin(
            dbInstance(),
            AuthenticatedUser.SystemInternalUser,
            clock,
            SystemController.CitizenPasskeyLoginRequest(
                assertionRequest = options.assertionRequest,
                credential = credential,
                deviceAuthHistory = deviceAuthHistory,
            ),
        )
    }

    private data class CitizenUserRow(val username: String?, val lastWeakLogin: HelsinkiDateTime?)

    private fun getCitizenUserRow(): CitizenUserRow? = db.read {
        it.createQuery {
                sql(
                    "SELECT username, last_weak_login FROM citizen_user WHERE id = ${bind(person.id)}"
                )
            }
            .exactlyOneOrNull<CitizenUserRow>()
    }
}
