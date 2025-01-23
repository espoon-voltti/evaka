// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis.controller

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.Sensitive
import fi.espoo.evaka.pis.SystemController
import fi.espoo.evaka.pis.controllers.PersonalDataControllerCitizen
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.CitizenAuthLevel
import fi.espoo.evaka.shared.auth.PasswordService
import fi.espoo.evaka.shared.dev.DevCitizenUser
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.vtjclient.service.persondetails.MockPersonDetailsService
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
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
        updateWeakLoginCredentials(
            PersonalDataControllerCitizen.UpdateWeakLoginCredentialsRequest(
                username = email,
                password = password,
            )
        )

        val identity =
            citizenWeakLogin(
                SystemController.CitizenWeakLoginRequest(username = email, password = password)
            )
        assertEquals(person.id, identity.id)
    }

    @Test
    fun `a person can update their username to match their new email, and log in with it`() {
        val newEmail = "new@example.com"
        val password = Sensitive("test123123")
        db.transaction { tx ->
            tx.insert(person.copy(email = newEmail, verifiedEmail = newEmail), DevPersonType.ADULT)
            MockPersonDetailsService.addPersons(person)
            tx.insert(
                DevCitizenUser(
                    person.id,
                    username = email,
                    usernameUpdatedAt = clock.now(),
                    password = passwordService.encode(password),
                    passwordUpdatedAt = clock.now(),
                )
            )
        }

        updateWeakLoginCredentials(
            PersonalDataControllerCitizen.UpdateWeakLoginCredentialsRequest(
                username = newEmail,
                password = null,
            )
        )

        val identity =
            citizenWeakLogin(
                SystemController.CitizenWeakLoginRequest(username = newEmail, password = password)
            )
        assertEquals(person.id, identity.id)
    }

    @Test
    fun `a person can update their password and log in with the new password`() {
        val password = Sensitive("test123123")
        db.transaction { tx ->
            tx.insert(person, DevPersonType.ADULT)
            MockPersonDetailsService.addPersons(person)
            tx.insert(
                DevCitizenUser(
                    person.id,
                    username = email,
                    usernameUpdatedAt = clock.now(),
                    password = passwordService.encode(password),
                    passwordUpdatedAt = clock.now(),
                )
            )
        }

        val newPassword = Sensitive("test256256")
        updateWeakLoginCredentials(
            PersonalDataControllerCitizen.UpdateWeakLoginCredentialsRequest(
                username = null,
                password = newPassword,
            )
        )

        val identity =
            citizenWeakLogin(
                SystemController.CitizenWeakLoginRequest(username = email, password = newPassword)
            )
        assertEquals(person.id, identity.id)
    }

    private fun updateWeakLoginCredentials(
        request: PersonalDataControllerCitizen.UpdateWeakLoginCredentialsRequest
    ) = controller.updateWeakLoginCredentials(dbInstance(), user, clock, request)

    private fun citizenStrongLogin() =
        systemController.citizenLogin(
            dbInstance(),
            AuthenticatedUser.SystemInternalUser,
            clock,
            SystemController.CitizenLoginRequest(
                socialSecurityNumber = ssn,
                firstName = person.firstName,
                lastName = person.lastName,
                keycloakEmail = null,
            ),
        )

    private fun citizenWeakLogin(request: SystemController.CitizenWeakLoginRequest) =
        systemController.citizenWeakLogin(
            dbInstance(),
            AuthenticatedUser.SystemInternalUser,
            clock,
            request,
        )
}
