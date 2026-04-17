// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.decision

import evaka.core.FullApplicationTest
import evaka.core.application.ApplicationStateService
import evaka.core.application.ApplicationStatus
import evaka.core.application.ApplicationType
import evaka.core.application.DaycarePlacementPlan
import evaka.core.application.persistence.daycare.Adult
import evaka.core.application.persistence.daycare.Apply
import evaka.core.application.persistence.daycare.Child
import evaka.core.application.persistence.daycare.DaycareFormV0
import evaka.core.shared.ChildId
import evaka.core.shared.DaycareId
import evaka.core.shared.DecisionId
import evaka.core.shared.EmployeeId
import evaka.core.shared.PersonId
import evaka.core.shared.async.AsyncJob
import evaka.core.shared.async.AsyncJobRunner
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.auth.UserRole
import evaka.core.shared.db.Database
import evaka.core.shared.dev.DevCareArea
import evaka.core.shared.dev.DevDaycare
import evaka.core.shared.dev.DevEmployee
import evaka.core.shared.dev.DevPerson
import evaka.core.shared.dev.DevPersonType
import evaka.core.shared.dev.insert
import evaka.core.shared.dev.insertTestApplication
import evaka.core.shared.domain.FiniteDateRange
import evaka.core.shared.domain.Forbidden
import evaka.core.shared.domain.MockEvakaClock
import evaka.core.shared.security.actionrule.AccessControlFilter
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
        applicationStateService.setVerified(
            tx = tx,
            user = AuthenticatedUser.Employee(serviceWorker, setOf(UserRole.SERVICE_WORKER)),
            clock = clock,
            applicationId = applicationId,
            confidential = false,
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
