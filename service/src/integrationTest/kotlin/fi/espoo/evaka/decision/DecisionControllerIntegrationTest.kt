// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.decision

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.application.ApplicationStateService
import fi.espoo.evaka.application.ApplicationStatus
import fi.espoo.evaka.application.ApplicationType
import fi.espoo.evaka.application.DaycarePlacementPlan
import fi.espoo.evaka.application.persistence.daycare.Adult
import fi.espoo.evaka.application.persistence.daycare.Apply
import fi.espoo.evaka.application.persistence.daycare.Child
import fi.espoo.evaka.application.persistence.daycare.DaycareFormV0
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.DecisionId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.dev.insertTestApplication
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.Forbidden
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.shared.security.actionrule.AccessControlFilter
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired

class DecisionControllerIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    private val clock = MockEvakaClock(2024, 3, 1, 12, 0)

    @Autowired lateinit var decisionController: DecisionController

    @Autowired lateinit var applicationStateService: ApplicationStateService

    @Autowired lateinit var asyncJobRunner: AsyncJobRunner<AsyncJob>

    val admin = DevEmployee(roles = setOf(UserRole.ADMIN))
    val serviceWorker = DevEmployee(roles = setOf(UserRole.SERVICE_WORKER))

    @BeforeEach
    fun init() {
        db.transaction { it.insert(admin) }
        db.transaction { it.insert(serviceWorker) }
    }

    @Test
    fun `PDF without contact info can be downloaded by service worker`() {
        val decisionId = db.transaction { tx -> createDecisionWithPeople(tx, serviceWorker.id) }
        asyncJobRunner.runPendingJobsSync(clock)

        downloadPdf(serviceWorker.user, decisionId)
    }

    @Test
    fun `Legacy PDF with contact info can be downloaded by service worker`() {
        val decisionId =
            db.transaction { tx ->
                createDecisionWithPeople(tx, serviceWorker.id, legacyPdfWithContactInfo = true)
            }
        asyncJobRunner.runPendingJobsSync(clock)

        downloadPdf(serviceWorker.user, decisionId)
    }

    @Test
    fun `PDF without contact info where child has restricted details can be downloaded by service worker`() {
        val decisionId =
            db.transaction { tx ->
                createDecisionWithPeople(tx, serviceWorker.id, childRestricted = true)
            }
        asyncJobRunner.runPendingJobsSync(clock)

        downloadPdf(serviceWorker.user, decisionId)
    }

    @Test
    fun `PDF without contact info where guardian has restricted details can be downloaded by service worker`() {
        val decisionId =
            db.transaction { tx ->
                createDecisionWithPeople(tx, serviceWorker.id, guardianRestricted = true)
            }
        asyncJobRunner.runPendingJobsSync(clock)

        downloadPdf(serviceWorker.user, decisionId)
    }

    @Test
    fun `Legacy PDF with contact info where child has restricted details can NOT be downloaded by service worker`() {
        val decisionId =
            db.transaction { tx ->
                createDecisionWithPeople(
                    tx,
                    serviceWorker.id,
                    childRestricted = true,
                    legacyPdfWithContactInfo = true,
                )
            }
        asyncJobRunner.runPendingJobsSync(clock)

        assertThrows<Forbidden> { downloadPdf(serviceWorker.user, decisionId) }
    }

    @Test
    fun `Legacy PDF with contact info where guardian has restricted details can NOT be downloaded by service worker`() {
        val decisionId =
            db.transaction { tx ->
                createDecisionWithPeople(
                    tx,
                    serviceWorker.id,
                    guardianRestricted = true,
                    legacyPdfWithContactInfo = true,
                )
            }
        asyncJobRunner.runPendingJobsSync(clock)

        assertThrows<Forbidden> { downloadPdf(serviceWorker.user, decisionId) }
    }

    @Test
    fun `Legacy PDF with contact info where child has restricted details can be downloaded by admin`() {
        val decisionId =
            db.transaction { tx ->
                createDecisionWithPeople(
                    tx,
                    serviceWorker.id,
                    childRestricted = true,
                    legacyPdfWithContactInfo = true,
                )
            }
        asyncJobRunner.runPendingJobsSync(clock)

        downloadPdf(admin.user, decisionId)
    }

    @Test
    fun `Legacy PDF with contact info where guardian has restricted details can be downloaded by admin`() {
        val decisionId =
            db.transaction { tx ->
                createDecisionWithPeople(
                    tx,
                    serviceWorker.id,
                    guardianRestricted = true,
                    legacyPdfWithContactInfo = true,
                )
            }
        asyncJobRunner.runPendingJobsSync(clock)

        downloadPdf(admin.user, decisionId)
    }

    private fun downloadPdf(user: AuthenticatedUser.Employee, decisionId: DecisionId) =
        decisionController.downloadDecisionPdf(dbInstance(), user, clock, decisionId)

    private fun createDecisionWithPeople(
        tx: Database.Transaction,
        serviceWorker: EmployeeId,
        guardianRestricted: Boolean = false,
        childRestricted: Boolean = false,
        legacyPdfWithContactInfo: Boolean = false,
    ): DecisionId {
        val guardianId =
            tx.insert(DevPerson(restrictedDetailsEnabled = guardianRestricted), DevPersonType.ADULT)
        val childId =
            tx.insert(
                DevPerson(
                    dateOfBirth = clock.today().minusYears(3),
                    restrictedDetailsEnabled = childRestricted,
                ),
                DevPersonType.CHILD,
            )
        val areaId = tx.insert(DevCareArea())
        val unitId = tx.insert(DevDaycare(areaId = areaId))
        val decisionId =
            createDecision(
                tx = tx,
                guardianId = guardianId,
                childId = childId,
                unitId = unitId,
                serviceWorker = serviceWorker,
            )
        if (legacyPdfWithContactInfo) {
            tx.execute {
                sql(
                    """
                    UPDATE decision SET document_contains_contact_info = TRUE
                    WHERE id = ${bind(decisionId)}
                """
                )
            }
        }
        return decisionId
    }

    private fun createDecision(
        tx: Database.Transaction,
        guardianId: PersonId,
        childId: ChildId,
        unitId: DaycareId,
        serviceWorker: EmployeeId,
    ): DecisionId {
        val applicationId =
            tx.insertTestApplication(
                status = ApplicationStatus.WAITING_PLACEMENT,
                guardianId = guardianId,
                childId = childId,
                type = ApplicationType.DAYCARE,
                document =
                    DaycareFormV0(
                        type = ApplicationType.DAYCARE,
                        serviceStart = "08:00",
                        serviceEnd = "16:00",
                        child = Child(dateOfBirth = clock.today().minusYears(3)),
                        guardian = Adult(),
                        apply = Apply(preferredUnits = listOf(unitId)),
                        preferredStartDate = clock.today().plusMonths(5),
                    ),
            )
        applicationStateService.createPlacementPlan(
            tx = tx,
            user = AuthenticatedUser.Employee(serviceWorker, setOf(UserRole.SERVICE_WORKER)),
            clock = clock,
            applicationId = applicationId,
            placementPlan =
                DaycarePlacementPlan(
                    unitId = unitId,
                    period =
                        FiniteDateRange(
                            clock.today().plusMonths(5),
                            clock.today().plusMonths(5).plusYears(3),
                        ),
                ),
        )
        applicationStateService.sendDecisionsWithoutProposal(
            tx = tx,
            user = AuthenticatedUser.Employee(serviceWorker, setOf(UserRole.SERVICE_WORKER)),
            clock = clock,
            applicationId = applicationId,
        )

        return tx.getDecisionsByApplication(applicationId, AccessControlFilter.PermitAll).first().id
    }
}
