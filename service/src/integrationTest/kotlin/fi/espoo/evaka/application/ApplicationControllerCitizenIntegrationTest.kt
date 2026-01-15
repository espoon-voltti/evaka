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
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.test.getApplicationStatus
import fi.espoo.evaka.vtjclient.service.persondetails.MockPersonDetailsService
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

    private val area = DevCareArea()
    private val daycare = DevDaycare(areaId = area.id)
    private val adult = DevPerson(ssn = "010180-1232")
    private val child = DevPerson(ssn = "010617A123U")
    private val decisionMaker = DevEmployee()

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(adult, DevPersonType.ADULT)
            tx.insert(child, DevPersonType.RAW_ROW)
            tx.insert(decisionMaker)
        }
        MockPersonDetailsService.addPersons(adult, child)
        MockPersonDetailsService.addDependants(adult, child)
    }

    @Test
    fun `user can delete a draft application`() {
        val applicationDetails =
            db.transaction { tx ->
                tx.insertTestApplication(
                    guardian = adult,
                    child = child,
                    appliedType = PlacementType.DAYCARE,
                    preferredUnit = daycare,
                )
            }

        applicationControllerCitizen.deleteOrCancelUnprocessedApplication(
            db = dbInstance(),
            user = AuthenticatedUser.Citizen(adult.id, CitizenAuthLevel.STRONG),
            clock = clock,
            applicationId = applicationDetails.id,
        )

        db.transaction { tx -> assertNull(tx.fetchApplicationDetails(applicationDetails.id)) }
    }

    @Test
    fun `user can cancel a sent unprocessed application`() {
        val application =
            db.transaction { tx ->
                tx.insertTestApplication(
                        guardian = adult,
                        child = child,
                        appliedType = PlacementType.DAYCARE,
                        preferredUnit = daycare,
                    )
                    .also {
                        stateService.sendApplication(
                            tx = tx,
                            user = AuthenticatedUser.Citizen(adult.id, CitizenAuthLevel.STRONG),
                            clock = clock,
                            applicationId = it.id,
                        )
                    }
            }

        applicationControllerCitizen.deleteOrCancelUnprocessedApplication(
            db = dbInstance(),
            user = AuthenticatedUser.Citizen(adult.id, CitizenAuthLevel.STRONG),
            clock = clock,
            applicationId = application.id,
        )

        db.transaction { tx ->
            assertEquals(ApplicationStatus.CANCELLED, tx.getApplicationStatus(application.id))
        }
    }

    @Test
    fun `user can not cancel a processed application`() {
        val application =
            db.transaction { tx ->
                tx.insertTestApplication(
                        guardian = adult,
                        child = child,
                        appliedType = PlacementType.DAYCARE,
                        preferredUnit = daycare,
                    )
                    .also {
                        stateService.sendApplication(
                            tx = tx,
                            user = AuthenticatedUser.Citizen(adult.id, CitizenAuthLevel.STRONG),
                            clock = clock,
                            applicationId = it.id,
                        )
                        stateService.moveToWaitingPlacement(
                            tx = tx,
                            user =
                                AuthenticatedUser.Employee(
                                    decisionMaker.id,
                                    setOf(UserRole.SERVICE_WORKER),
                                ),
                            clock = clock,
                            applicationId = it.id,
                        )
                    }
            }

        assertThrows<BadRequest> {
            applicationControllerCitizen.deleteOrCancelUnprocessedApplication(
                db = dbInstance(),
                user = AuthenticatedUser.Citizen(adult.id, CitizenAuthLevel.STRONG),
                clock = clock,
                applicationId = application.id,
            )
        }
    }
}
