// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare.controllers

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.daycare.createChild
import fi.espoo.evaka.espoo.EspooActionRuleMapping
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.FeatureConfig
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.CitizenAuthLevel
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.config.testFeatureConfig
import fi.espoo.evaka.shared.domain.Forbidden
import fi.espoo.evaka.shared.domain.RealEvakaClock
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import io.opentracing.noop.NoopTracerFactory
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired

class ChildrenControllerIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired lateinit var childController: ChildController

    private val childId = ChildId(UUID.randomUUID())

    private lateinit var child: Child

    @BeforeEach
    fun setUp() {
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
        val body = childController.getAdditionalInfo(dbInstance(), user, RealEvakaClock(), childId)

        assertEquals(child.additionalInformation.diet, body.diet)
        assertEquals(child.additionalInformation.additionalInfo, body.additionalInfo)
        assertEquals(child.additionalInformation.allergies, body.allergies)
    }

    @Test
    fun `getChild returns permitted actions`() {
        val user =
            AuthenticatedUser.Employee(
                EmployeeId(UUID.randomUUID()),
                setOf(UserRole.SERVICE_WORKER)
            )
        val result = childController.getChild(dbInstance(), user, RealEvakaClock(), childId)

        Assertions.assertThat(result.permittedActions)
            .isEqualTo(
                setOf(
                    Action.Child.READ,
                    Action.Child.READ_ADDITIONAL_INFO,
                    Action.Child.UPDATE_ADDITIONAL_INFO,
                    Action.Child.READ_DECISIONS,
                    Action.Child.READ_APPLICATION,
                    Action.Child.READ_ASSISTANCE,
                    Action.Child.CREATE_ASSISTANCE_FACTOR,
                    Action.Child.READ_ASSISTANCE_FACTORS,
                    Action.Child.CREATE_DAYCARE_ASSISTANCE,
                    Action.Child.READ_DAYCARE_ASSISTANCES,
                    Action.Child.CREATE_PRESCHOOL_ASSISTANCE,
                    Action.Child.READ_PRESCHOOL_ASSISTANCES,
                    Action.Child.CREATE_ASSISTANCE_ACTION,
                    Action.Child.READ_ASSISTANCE_ACTION,
                    Action.Child.CREATE_OTHER_ASSISTANCE_MEASURE,
                    Action.Child.READ_OTHER_ASSISTANCE_MEASURES,
                    Action.Child.CREATE_ASSISTANCE_NEED_DECISION,
                    Action.Child.READ_ASSISTANCE_NEED_DECISIONS,
                    Action.Child.CREATE_ASSISTANCE_NEED_PRESCHOOL_DECISION,
                    Action.Child.READ_ASSISTANCE_NEED_PRESCHOOL_DECISIONS,
                    Action.Child.CREATE_BACKUP_CARE,
                    Action.Child.READ_BACKUP_CARE,
                    Action.Child.CREATE_DAILY_SERVICE_TIME,
                    Action.Child.READ_PLACEMENT,
                    Action.Child.READ_GUARDIANS,
                    Action.Child.READ_BLOCKED_GUARDIANS,
                    Action.Child.READ_CHILD_RECIPIENTS,
                    Action.Child.UPDATE_CHILD_RECIPIENT,
                    Action.Child.READ_CHILD_CONSENTS
                )
            )
    }

    @Test
    fun `getChild permitted actions contains READ_DAILY_SERVICE_TIMES in Espoo`() {
        val featureConfig: FeatureConfig = testFeatureConfig

        val espooChildController =
            ChildController(
                AccessControl(EspooActionRuleMapping(), NoopTracerFactory.create()),
                featureConfig
            )
        val user =
            AuthenticatedUser.Employee(
                EmployeeId(UUID.randomUUID()),
                setOf(UserRole.SERVICE_WORKER)
            )
        val result = espooChildController.getChild(dbInstance(), user, RealEvakaClock(), childId)

        Assertions.assertThat(result.permittedActions)
            .contains(Action.Child.READ_DAILY_SERVICE_TIMES)
    }
}
