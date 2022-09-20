// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare.controllers

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.daycare.createChild
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.CitizenAuthLevel
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.Forbidden
import fi.espoo.evaka.shared.domain.RealEvakaClock
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired

class ChildrenControllerIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired lateinit var childController: ChildController

    private val childId = ChildId(UUID.randomUUID())

    private lateinit var child: Child

    @BeforeEach
    internal fun setUp() {
        db.transaction { tx ->
            tx.execute(
                "INSERT INTO person (id, date_of_birth) VALUES ('$childId', '${LocalDate.now().minusYears(1)}')"
            )
            child =
                tx.createChild(
                    Child(
                        id = childId,
                        additionalInformation =
                            AdditionalInformation(
                                allergies = "dghsfhed",
                                diet = "bcvxnvgmn",
                                additionalInfo = "fjmhj"
                            )
                    )
                )
        }
    }

    @Test
    fun `get additional info returns correct reply with service worker`() {
        getAdditionalInfo(
            AuthenticatedUser.Employee(
                EmployeeId(UUID.randomUUID()),
                setOf(UserRole.SERVICE_WORKER)
            )
        )
    }

    @Test
    fun `get additional info throws forbidden with enduser`() {
        assertThrows<Forbidden> {
            getAdditionalInfo(
                AuthenticatedUser.Citizen(PersonId(UUID.randomUUID()), CitizenAuthLevel.STRONG)
            )
        }
    }

    fun getAdditionalInfo(user: AuthenticatedUser) {
        val body =
            childController.getAdditionalInfo(Database(jdbi), user, RealEvakaClock(), childId)

        assertEquals(child.additionalInformation.diet, body.diet)
        assertEquals(child.additionalInformation.additionalInfo, body.additionalInfo)
        assertEquals(child.additionalInformation.allergies, body.allergies)
    }
}
