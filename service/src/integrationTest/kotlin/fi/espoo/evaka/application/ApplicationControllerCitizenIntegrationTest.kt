// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.application

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.insertApplication as insertTestApplication
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.CitizenAuthLevel
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.test.getApplicationStatus
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testArea
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDecisionMaker_1
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired

class ApplicationControllerCitizenIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var applicationControllerCitizen: ApplicationControllerCitizen

    @Autowired private lateinit var stateService: ApplicationStateService

    private val clock = MockEvakaClock(2020, 1, 1, 12, 0)

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insert(testAdult_1)
            tx.insert(testChild_1)
            tx.insert(testArea)
            tx.insert(testDaycare)
            tx.insert(testDecisionMaker_1)
        }
    }

    @Test
    fun `user can delete a draft application`() {
        val applicationDetails =
            db.transaction { tx ->
                tx.insertTestApplication(
                    guardian = testAdult_1,
                    child = testChild_1,
                    appliedType = PlacementType.DAYCARE
                )
            }

        applicationControllerCitizen.deleteOrCancelUnprocessedApplication(
            db = dbInstance(),
            user = AuthenticatedUser.Citizen(testAdult_1.id, CitizenAuthLevel.STRONG),
            clock = clock,
            applicationId = applicationDetails.id
        )

        db.transaction { tx -> assertNull(tx.fetchApplicationDetails(applicationDetails.id)) }
    }

    @Test
    fun `user can cancel a sent unprocessed application`() {
        val (id) =
            db.transaction { tx ->
                val application =
                    tx.insertTestApplication(
                        guardian = testAdult_1,
                        child = testChild_1,
                        appliedType = PlacementType.DAYCARE
                    )
                stateService.sendApplication(
                    tx = tx,
                    user = AuthenticatedUser.Citizen(testAdult_1.id, CitizenAuthLevel.STRONG),
                    clock = clock,
                    applicationId = application.id
                )
                application
            }

        applicationControllerCitizen.deleteOrCancelUnprocessedApplication(
            db = dbInstance(),
            user = AuthenticatedUser.Citizen(testAdult_1.id, CitizenAuthLevel.STRONG),
            clock = clock,
            applicationId = id
        )

        db.transaction { tx ->
            assertEquals(ApplicationStatus.CANCELLED, tx.getApplicationStatus(id))
        }
    }

    @Test
    fun `user can not cancel a processed application`() {
        val (id) =
            db.transaction { tx ->
                val application =
                    tx.insertTestApplication(
                        guardian = testAdult_1,
                        child = testChild_1,
                        appliedType = PlacementType.DAYCARE
                    )
                stateService.sendApplication(
                    tx = tx,
                    user = AuthenticatedUser.Citizen(testAdult_1.id, CitizenAuthLevel.STRONG),
                    clock = clock,
                    applicationId = application.id
                )
                stateService.moveToWaitingPlacement(
                    tx = tx,
                    user =
                        AuthenticatedUser.Employee(
                            testDecisionMaker_1.id,
                            setOf(UserRole.SERVICE_WORKER)
                        ),
                    clock = clock,
                    applicationId = application.id
                )
                application
            }

        assertThrows<BadRequest> {
            applicationControllerCitizen.deleteOrCancelUnprocessedApplication(
                db = dbInstance(),
                user = AuthenticatedUser.Citizen(testAdult_1.id, CitizenAuthLevel.STRONG),
                clock = clock,
                applicationId = id
            )
        }
    }
}
