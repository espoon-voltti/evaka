// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare.controllers

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.daycare.createChild
import fi.espoo.evaka.daycare.getChild
import fi.espoo.evaka.espoo.EspooActionRuleMapping
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.FeatureConfig
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.config.testFeatureConfig
import fi.espoo.evaka.shared.domain.RealEvakaClock
import fi.espoo.evaka.shared.noopTracer
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class ChildrenControllerIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired lateinit var childController: ChildController

    private val childId = ChildId(UUID.randomUUID())

    private lateinit var child: Child

    @BeforeEach
    fun setUp() {
        db.transaction { tx ->
            tx.execute {
                sql(
                    "INSERT INTO person (id, date_of_birth) VALUES ('$childId', '${LocalDate.now().minusYears(1)}')"
                )
            }
            tx.createChild(
                Child(
                    id = childId,
                    additionalInformation =
                        AdditionalInformation(
                            allergies = "dghsfhed",
                            diet = "bcvxnvgmn",
                            additionalInfo = "fjmhj",
                        ),
                )
            )
            child = tx.getChild(childId)!!
        }
    }

    @Test
    fun `get additional info returns correct reply with service worker`() {
        getAdditionalInfo(
            AuthenticatedUser.Employee(
                EmployeeId(UUID.randomUUID()),
                setOf(UserRole.SERVICE_WORKER),
            )
        )
    }

    fun getAdditionalInfo(user: AuthenticatedUser.Employee) {
        val body = childController.getAdditionalInfo(dbInstance(), user, RealEvakaClock(), childId)

        assertEquals(child.additionalInformation.diet, body.diet)
        assertEquals(child.additionalInformation.additionalInfo, body.additionalInfo)
        assertEquals(child.additionalInformation.allergies, body.allergies)
    }

    @Test
    fun `getChild permitted actions should not contain READ_DAILY_SERVICE_TIMES with default AccessControl`() {
        val user =
            AuthenticatedUser.Employee(
                EmployeeId(UUID.randomUUID()),
                setOf(UserRole.SERVICE_WORKER),
            )
        val result = childController.getChild(dbInstance(), user, RealEvakaClock(), childId)

        Assertions.assertThat(result.permittedActions)
            .doesNotContain(Action.Child.READ_DAILY_SERVICE_TIMES)
    }

    @Test
    fun `getChild permitted actions contains READ_DAILY_SERVICE_TIMES in Espoo`() {
        val featureConfig: FeatureConfig = testFeatureConfig

        val espooChildController =
            ChildController(AccessControl(EspooActionRuleMapping(), noopTracer()), featureConfig)
        val user =
            AuthenticatedUser.Employee(
                EmployeeId(UUID.randomUUID()),
                setOf(UserRole.SERVICE_WORKER),
            )
        val result = espooChildController.getChild(dbInstance(), user, RealEvakaClock(), childId)

        Assertions.assertThat(result.permittedActions)
            .contains(Action.Child.READ_DAILY_SERVICE_TIMES)
    }
}
