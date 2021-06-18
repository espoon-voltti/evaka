// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare.controllers

import fi.espoo.evaka.daycare.AbstractIntegrationTest
import fi.espoo.evaka.daycare.createChild
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.domain.Forbidden
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import java.time.LocalDate
import java.util.UUID

class ChildrenControllerIntegrationTest : AbstractIntegrationTest() {

    @Autowired
    lateinit var childController: ChildController

    private val childId = UUID.randomUUID()

    private lateinit var child: Child

    @BeforeEach
    internal fun setUp() {
        db.transaction { tx ->
            tx.execute("INSERT INTO person (id, date_of_birth) VALUES ('$childId', '${LocalDate.now().minusYears(1)}')")
            child = tx.createChild(
                Child(
                    id = childId,
                    additionalInformation = AdditionalInformation(
                        allergies = "dghsfhed",
                        diet = "bcvxnvgmn",
                        additionalInfo = "fjmhj"
                    )
                )
            )
        }
    }

    @AfterEach
    internal fun tearDown() {
        db.transaction {
            it.execute("DELETE FROM person WHERE id = '$childId'")
        }
    }

    @Test
    fun `get additional info returns correct reply with service worker`() {
        getAdditionalInfo(AuthenticatedUser.Employee(UUID.randomUUID(), setOf(UserRole.SERVICE_WORKER)))
    }

    @Test
    fun `get additional info returns correct reply with finance admin`() {
        getAdditionalInfo(AuthenticatedUser.Employee(UUID.randomUUID(), setOf(UserRole.FINANCE_ADMIN)))
    }

    @Test
    fun `get additional info throws forbidden with enduser`() {
        assertThrows<Forbidden> { getAdditionalInfo(AuthenticatedUser.Citizen(UUID.randomUUID())) }
    }

    fun getAdditionalInfo(user: AuthenticatedUser) {
        val response = childController.getAdditionalInfo(db, user, childId)
        val body = response.body!!

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(child.additionalInformation.diet, body.diet)
        assertEquals(child.additionalInformation.additionalInfo, body.additionalInfo)
        assertEquals(child.additionalInformation.allergies, body.allergies)
    }
}
