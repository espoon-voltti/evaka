// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.children.consent

import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.jackson.responseObject
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.pis.service.insertGuardian
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.CitizenAuthLevel
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.asUser
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.insertTestEmployee
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testChild_1
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ChildConsentControllerIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    private final val adminId = EmployeeId(UUID.randomUUID())
    private val admin = AuthenticatedUser.Employee(adminId, setOf(UserRole.ADMIN))

    private val guardian = AuthenticatedUser.Citizen(testAdult_1.id, authLevel = CitizenAuthLevel.STRONG)

    @BeforeEach
    protected fun beforeEach() {
        db.transaction {
            it.insertGeneralTestFixtures()
            it.insertTestEmployee(DevEmployee(adminId, roles = setOf(UserRole.ADMIN)))
            it.insertGuardian(testAdult_1.id, testChild_1.id)
        }
    }

    @Test
    fun `employee can give consent`() {
        this.whenPostChildConsentThenExpectSuccess(
            listOf(
                ChildConsentController.UpdateChildConsentRequest(
                    type = ChildConsentType.EVAKA_PROFILE_PICTURE,
                    given = true
                )
            )
        )

        val consent = this.whenGetChildConsentsThenExpectSuccess()[0]

        assertEquals(true, consent.given)
        assertEquals("Test Person", consent.givenByEmployee)
    }

    @Test
    fun `employee can clear consent`() {
        this.whenPostChildConsentThenExpectSuccess(
            listOf(
                ChildConsentController.UpdateChildConsentRequest(
                    type = ChildConsentType.EVAKA_PROFILE_PICTURE,
                    given = true
                )
            )
        )

        val consent = this.whenGetChildConsentsThenExpectSuccess()[0]

        assertEquals(true, consent.given)
        assertEquals("Test Person", consent.givenByEmployee)

        this.whenPostChildConsentThenExpectSuccess(
            listOf(
                ChildConsentController.UpdateChildConsentRequest(
                    type = ChildConsentType.EVAKA_PROFILE_PICTURE,
                    given = null
                )
            )
        )

        assertNull(this.whenGetChildConsentsThenExpectSuccess()[0].given)
    }

    @Test
    fun `citizen can give consent once`() {
        this.whenCitizenPostChildConsentThenExpectStatus(
            listOf(
                CitizenChildConsent(
                    type = ChildConsentType.EVAKA_PROFILE_PICTURE,
                    given = true
                )
            ),
            200
        )

        val consent = this.whenCitizenGetChildConsentsThenExpectSuccess()[testChild_1.id]

        assertEquals(true, consent?.any { it.type == ChildConsentType.EVAKA_PROFILE_PICTURE && it.given == true })

        this.whenCitizenPostChildConsentThenExpectStatus(
            listOf(
                CitizenChildConsent(
                    type = ChildConsentType.EVAKA_PROFILE_PICTURE,
                    given = true
                )
            ),
            403
        )
    }

    private fun whenPostChildConsentThenExpectSuccess(request: List<ChildConsentController.UpdateChildConsentRequest>) {
        val (_, res, _) = http.post("/children/${testChild_1.id}/consent")
            .jsonBody(jsonMapper.writeValueAsString(request))
            .asUser(admin)
            .response()

        assertEquals(200, res.statusCode)
    }

    private fun whenGetChildConsentsThenExpectSuccess(): List<ChildConsent> {
        val (_, res, response) = http.get("/children/${testChild_1.id}/consent")
            .asUser(admin)
            .responseObject<List<ChildConsent>>(jsonMapper)

        assertEquals(200, res.statusCode)
        return response.get()
    }

    private fun whenCitizenPostChildConsentThenExpectStatus(
        request: List<CitizenChildConsent>,
        status: Int
    ) {
        val (_, res, _) = http.post("/citizen/children/${testChild_1.id}/consent")
            .jsonBody(jsonMapper.writeValueAsString(request))
            .asUser(guardian)
            .response()

        assertEquals(status, res.statusCode)
    }

    private fun whenCitizenGetChildConsentsThenExpectSuccess(): Map<ChildId, List<CitizenChildConsent>> {
        val (_, res, response) = http.get("/citizen/children/consents")
            .asUser(guardian)
            .responseObject<Map<ChildId, List<CitizenChildConsent>>>(jsonMapper)

        assertEquals(200, res.statusCode)
        return response.get()
    }
}
