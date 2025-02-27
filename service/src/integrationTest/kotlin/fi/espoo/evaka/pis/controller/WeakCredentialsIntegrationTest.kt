// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis.controller

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.Sensitive
import fi.espoo.evaka.pis.SystemController
import fi.espoo.evaka.pis.controllers.PersonalDataControllerCitizen
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.auth.*
import fi.espoo.evaka.shared.dev.DevCitizenUser
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.vtjclient.service.persondetails.MockPersonDetailsService
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired

class WeakCredentialsIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var controller: PersonalDataControllerCitizen
    @Autowired private lateinit var systemController: SystemController
    @Autowired private lateinit var passwordService: PasswordService

    private val clock = MockEvakaClock(2024, 1, 1, 12, 0)

    private val ssn = "010107A995B"
    private val email = "verified@example.com"
    private val person = DevPerson(email = email, verifiedEmail = email, ssn = ssn)
    private val user = person.user(CitizenAuthLevel.STRONG)

    @Test
    fun `a person with a verified email can activate weak credentials and log in`() {
        db.transaction { tx -> tx.insert(person, DevPersonType.ADULT) }
        MockPersonDetailsService.addPersons(person)

        citizenStrongLogin()
        val password = Sensitive("test123123")
        updateWeakLoginCredentials(username = email, password = password)

        val identity = citizenWeakLogin(username = email, password = password)
        assertEquals(person.id, identity.id)
    }

    @Test
    fun `a person can update their username to match their new email, and log in with it`() {
        val newEmail = "new@example.com"
        val password = Sensitive("test123123")
        db.transaction { tx ->
            tx.insert(person.copy(email = newEmail, verifiedEmail = newEmail), DevPersonType.ADULT)
            tx.insert(citizenUser(person.id, email, password))
        }
        MockPersonDetailsService.addPersons(person)

        updateWeakLoginCredentials(username = newEmail, password = null)

        val identity = citizenWeakLogin(username = newEmail, password = password)
        assertEquals(person.id, identity.id)
    }

    @Test
    fun `username is converted to lowercase automatically`() {
        val newEmail = "nEW@eXample.Com"
        val password = Sensitive("test123123")
        db.transaction { tx ->
            tx.insert(person.copy(email = newEmail, verifiedEmail = newEmail), DevPersonType.ADULT)
            tx.insert(citizenUser(person.id, email, password))
        }
        MockPersonDetailsService.addPersons(person)

        updateWeakLoginCredentials(username = newEmail, password = null)

        // In a real environment apigw converts the username to lowercase when logging in.
        // This conversion must be in the apigw so the login rate limit is applied correctly to the
        // lowercase username, and it can't be bypassed by just changing some letter(s) to
        // uppercase. The service API endpoint expects a lowercase username, so in this test we have
        // to convert it manually
        val actualUsername = newEmail.lowercase()
        val identity = citizenWeakLogin(username = actualUsername, password = password)
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
        updateWeakLoginCredentials(username = null, password = newPassword)

        val identity = citizenWeakLogin(username = email, password = newPassword)
        assertEquals(person.id, identity.id)
    }

    @Test
    fun `a person can't use a password that does not match constraints`() {
        db.transaction { tx -> tx.insert(person, DevPersonType.ADULT) }
        MockPersonDetailsService.addPersons(person)

        citizenStrongLogin()
        val password = Sensitive("nope")
        val error =
            assertThrows<BadRequest> {
                updateWeakLoginCredentials(username = email, password = password)
            }
        assertEquals("PASSWORD_FORMAT", error.errorCode)
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
        val error =
            assertThrows<BadRequest> {
                updateWeakLoginCredentials(username = email, password = password)
            }
        assertEquals("PASSWORD_UNACCEPTABLE", error.errorCode)
    }

    private fun updateWeakLoginCredentials(username: String?, password: Sensitive<String>?) =
        controller.updateWeakLoginCredentials(
            dbInstance(),
            user,
            clock,
            PersonalDataControllerCitizen.UpdateWeakLoginCredentialsRequest(username, password),
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

    private fun citizenWeakLogin(username: String, password: Sensitive<String>) =
        systemController.citizenWeakLogin(
            dbInstance(),
            AuthenticatedUser.SystemInternalUser,
            clock,
            SystemController.CitizenWeakLoginRequest(username, password),
        )

    private fun citizenUser(id: PersonId, email: String, password: Sensitive<String>) =
        DevCitizenUser(
            id,
            username = email,
            usernameUpdatedAt = clock.now(),
            password = passwordService.encode(password),
            passwordUpdatedAt = clock.now(),
        )
}
