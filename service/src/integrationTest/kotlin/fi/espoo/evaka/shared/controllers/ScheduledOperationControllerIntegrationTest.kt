// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.controllers

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.application.ApplicationStateService
import fi.espoo.evaka.application.ApplicationStatus
import fi.espoo.evaka.application.ApplicationType
import fi.espoo.evaka.application.DaycarePlacementPlan
import fi.espoo.evaka.application.fetchApplicationDetails
import fi.espoo.evaka.application.persistence.daycare.DaycareFormV0
import fi.espoo.evaka.attachment.getApplicationAttachments
import fi.espoo.evaka.insertApplication
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.dev.insertTestApplication
import fi.espoo.evaka.shared.dev.insertTestApplicationForm
import fi.espoo.evaka.shared.dev.insertTestPlacement
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.test.validDaycareApplication
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testAdult_5
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDecisionMaker_1
import org.jdbi.v3.core.kotlin.mapTo
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.util.UUID

class ScheduledOperationControllerIntegrationTest : FullApplicationTest() {
    @Autowired
    private lateinit var scheduledOperationController: ScheduledOperationController

    @Autowired
    private lateinit var applicationStateService: ApplicationStateService

    private val serviceWorker = AuthenticatedUser(id = testDecisionMaker_1.id, roles = setOf(UserRole.SERVICE_WORKER))

    @BeforeEach
    private fun beforeEach() {
        db.transaction { tx ->
            tx.resetDatabase()
            insertGeneralTestFixtures(tx.handle)
        }
    }

    @Test
    fun `Draft application and attachments older than 30 days is cleaned up`() {
        val id_to_be_deleted = UUID.randomUUID()
        val id_not_to_be_deleted = UUID.randomUUID()
        val user = AuthenticatedUser(testAdult_5.id, setOf(UserRole.END_USER))

        db.transaction { tx ->
            insertApplication(tx.handle, guardian = testAdult_5, applicationId = id_to_be_deleted)
            setApplicationCreatedDate(tx, id_to_be_deleted, LocalDate.now().minusDays(32))

            insertApplication(tx.handle, guardian = testAdult_5, applicationId = id_not_to_be_deleted)
            setApplicationCreatedDate(tx, id_not_to_be_deleted, LocalDate.now().minusDays(31))
        }

        db.transaction {
            uploadAttachment(id_to_be_deleted, user)
            uploadAttachment(id_not_to_be_deleted, user)
        }

        db.read {
            Assertions.assertEquals(1, it.getApplicationAttachments(id_to_be_deleted).size)
            Assertions.assertEquals(1, it.getApplicationAttachments(id_not_to_be_deleted).size)
        }

        scheduledOperationController.removeOldDraftApplications(db)

        db.read {
            Assertions.assertNull(fetchApplicationDetails(it.handle, id_to_be_deleted))
            Assertions.assertEquals(0, it.getApplicationAttachments(id_to_be_deleted).size)

            Assertions.assertNotNull(fetchApplicationDetails(it.handle, id_not_to_be_deleted)!!)
            Assertions.assertEquals(1, it.getApplicationAttachments(id_not_to_be_deleted).size)
        }
    }

    private fun setApplicationCreatedDate(db: Database.Transaction, applicationId: UUID, created: LocalDate) {
        db.handle.createUpdate("""UPDATE application SET created = :created WHERE id = :id""")
            .bind("created", created)
            .bind("id", applicationId)
            .execute()
    }

    @Test
    fun `a transfer application for a child without any placements is cancelled`() {
        val preferredStartDate = LocalDate.now().plusMonths(1)
        val applicationId = createTransferApplication(ApplicationType.DAYCARE, preferredStartDate)

        scheduledOperationController.cancelOutdatedTransferApplications(db)

        val applicationStatus = getApplicationStatus(applicationId)
        assertEquals(ApplicationStatus.CANCELLED, applicationStatus)
    }

    @Test
    fun `a normal application for a child without any placements is not cancelled`() {
        val preferredStartDate = LocalDate.now().plusMonths(1)
        val applicationId = createNormalApplication(ApplicationType.DAYCARE, preferredStartDate)

        scheduledOperationController.cancelOutdatedTransferApplications(db)

        val applicationStatus = getApplicationStatus(applicationId)
        assertEquals(ApplicationStatus.SENT, applicationStatus)
    }

    @Test
    fun `a daycare transfer application for a child with a daycare placement is not cancelled`() {
        val preferredStartDate = LocalDate.now().plusMonths(1)
        val applicationId = createTransferApplication(ApplicationType.DAYCARE, preferredStartDate)
        val dateRange = FiniteDateRange(preferredStartDate, preferredStartDate.plusMonths(1))
        createPlacement(PlacementType.DAYCARE, dateRange)

        scheduledOperationController.cancelOutdatedTransferApplications(db)

        val applicationStatus = getApplicationStatus(applicationId)
        assertEquals(ApplicationStatus.SENT, applicationStatus)
    }

    @Test
    fun `a daycare transfer application for a child with a future daycare placement is not cancelled`() {
        val preferredStartDate = LocalDate.now().plusMonths(1)
        val applicationId = createTransferApplication(ApplicationType.DAYCARE, preferredStartDate)
        val dateRange = FiniteDateRange(preferredStartDate.plusMonths(1), preferredStartDate.plusMonths(2))
        createPlacement(PlacementType.DAYCARE, dateRange)

        scheduledOperationController.cancelOutdatedTransferApplications(db)

        val applicationStatus = getApplicationStatus(applicationId)
        assertEquals(ApplicationStatus.SENT, applicationStatus)
    }

    @Test
    fun `a daycare transfer application for a child with a preschool placement is cancelled`() {
        val preferredStartDate = LocalDate.now().plusMonths(1)
        val applicationId = createTransferApplication(ApplicationType.DAYCARE, preferredStartDate)
        val dateRange = FiniteDateRange(preferredStartDate, preferredStartDate.plusMonths(1))
        createPlacement(PlacementType.PRESCHOOL, dateRange)

        scheduledOperationController.cancelOutdatedTransferApplications(db)

        val applicationStatus = getApplicationStatus(applicationId)
        assertEquals(ApplicationStatus.CANCELLED, applicationStatus)
    }

    @Test
    fun `a daycare transfer application for a child with a past daycare placement is cancelled`() {
        val preferredStartDate = LocalDate.now().minusMonths(2)
        val applicationId = createTransferApplication(ApplicationType.DAYCARE, preferredStartDate)
        val dateRange = FiniteDateRange(preferredStartDate.minusMonths(1), LocalDate.now().minusMonths(1))
        createPlacement(PlacementType.DAYCARE, dateRange)

        scheduledOperationController.cancelOutdatedTransferApplications(db)

        val applicationStatus = getApplicationStatus(applicationId)
        assertEquals(ApplicationStatus.CANCELLED, applicationStatus)
    }

    @Test
    fun `a daycare transfer application for a child with a daycare placement that ends today is not cancelled`() {
        val preferredStartDate = LocalDate.now().minusMonths(2)
        val applicationId = createTransferApplication(ApplicationType.DAYCARE, preferredStartDate)
        val dateRange = FiniteDateRange(preferredStartDate.minusMonths(1), LocalDate.now())
        createPlacement(PlacementType.DAYCARE, dateRange)

        scheduledOperationController.cancelOutdatedTransferApplications(db)

        val applicationStatus = getApplicationStatus(applicationId)
        assertEquals(ApplicationStatus.SENT, applicationStatus)
    }

    @Test
    fun `a daycare transfer application for a child with a past daycare placement that ended yesterday is cancelled`() {
        val preferredStartDate = LocalDate.now().minusMonths(2)
        val applicationId = createTransferApplication(ApplicationType.DAYCARE, preferredStartDate)
        val dateRange = FiniteDateRange(preferredStartDate.minusMonths(1), LocalDate.now().minusDays(1))
        createPlacement(PlacementType.DAYCARE, dateRange)

        scheduledOperationController.cancelOutdatedTransferApplications(db)

        val applicationStatus = getApplicationStatus(applicationId)
        assertEquals(ApplicationStatus.CANCELLED, applicationStatus)
    }

    @Test
    fun `a preschool transfer application for a child with a preschool placement is not cancelled`() {
        val preferredStartDate = LocalDate.now().minusMonths(2)
        val applicationId = createTransferApplication(ApplicationType.PRESCHOOL, preferredStartDate)
        val dateRange = FiniteDateRange(preferredStartDate.minusMonths(1), LocalDate.now().plusMonths(1))
        createPlacement(PlacementType.PRESCHOOL, dateRange)

        scheduledOperationController.cancelOutdatedTransferApplications(db)

        val applicationStatus = getApplicationStatus(applicationId)
        assertEquals(ApplicationStatus.SENT, applicationStatus)
    }

    @Test
    fun `a preschool daycare transfer application for a child with a preschool placement is cancelled`() {
        val preferredStartDate = LocalDate.now().minusMonths(2)
        val applicationId =
            createTransferApplication(ApplicationType.PRESCHOOL, preferredStartDate, preschoolDaycare = true)
        val dateRange = FiniteDateRange(preferredStartDate.minusMonths(1), LocalDate.now().plusMonths(1))
        createPlacement(PlacementType.PRESCHOOL, dateRange)

        scheduledOperationController.cancelOutdatedTransferApplications(db)

        val applicationStatus = getApplicationStatus(applicationId)
        assertEquals(ApplicationStatus.CANCELLED, applicationStatus)
    }

    @Test
    fun `a preschool transfer application for a child with a preparatory placement is not cancelled`() {
        val preferredStartDate = LocalDate.now().minusMonths(2)
        val applicationId = createTransferApplication(ApplicationType.PRESCHOOL, preferredStartDate)
        val dateRange = FiniteDateRange(preferredStartDate.minusMonths(1), LocalDate.now().plusMonths(1))
        createPlacement(PlacementType.PREPARATORY, dateRange)

        scheduledOperationController.cancelOutdatedTransferApplications(db)

        val applicationStatus = getApplicationStatus(applicationId)
        assertEquals(ApplicationStatus.SENT, applicationStatus)
    }

    @Test
    fun `a preschool transfer application for a child with a preschool daycare placement is not cancelled`() {
        val preferredStartDate = LocalDate.now().minusMonths(2)
        val applicationId = createTransferApplication(ApplicationType.PRESCHOOL, preferredStartDate)
        val dateRange = FiniteDateRange(preferredStartDate.minusMonths(1), LocalDate.now().plusMonths(1))
        createPlacement(PlacementType.PRESCHOOL_DAYCARE, dateRange)

        scheduledOperationController.cancelOutdatedTransferApplications(db)

        val applicationStatus = getApplicationStatus(applicationId)
        assertEquals(ApplicationStatus.SENT, applicationStatus)
    }

    @Test
    fun `transfer application cancelling cleans up placement plans and decision drafts`() {
        val preferredStartDate = LocalDate.now()
        val applicationId = createTransferApplication(
            ApplicationType.PRESCHOOL,
            preferredStartDate,
            status = ApplicationStatus.WAITING_PLACEMENT
        )
        db.transaction {
            applicationStateService.createPlacementPlan(
                it,
                serviceWorker,
                applicationId,
                DaycarePlacementPlan(
                    testDaycare.id,
                    FiniteDateRange(preferredStartDate, preferredStartDate.plusMonths(1))
                )
            )
        }

        scheduledOperationController.cancelOutdatedTransferApplications(db)

        val applicationStatus = getApplicationStatus(applicationId)
        assertEquals(ApplicationStatus.CANCELLED, applicationStatus)
        val placementPlans = db.read { it.createQuery("SELECT COUNT(*) FROM placement_plan").mapTo<Int>().first() }
        assertEquals(0, placementPlans)
        val decisions = db.read { it.createQuery("SELECT COUNT(*) FROM decision").mapTo<Int>().first() }
        assertEquals(0, decisions)
    }

    @Test
    fun `a transfer application with a decision does not get canceled`() {
        val preferredStartDate = LocalDate.now()
        val applicationId = createTransferApplication(
            ApplicationType.PRESCHOOL,
            preferredStartDate,
            status = ApplicationStatus.WAITING_PLACEMENT
        )
        db.transaction {
            applicationStateService.createPlacementPlan(
                it,
                serviceWorker,
                applicationId,
                DaycarePlacementPlan(
                    testDaycare.id,
                    FiniteDateRange(preferredStartDate, preferredStartDate.plusMonths(1))
                )
            )
            applicationStateService.sendDecisionsWithoutProposal(it, serviceWorker, applicationId)
        }

        scheduledOperationController.cancelOutdatedTransferApplications(db)

        val applicationStatus = getApplicationStatus(applicationId)
        assertEquals(ApplicationStatus.WAITING_CONFIRMATION, applicationStatus)
    }

    private fun createTransferApplication(
        type: ApplicationType,
        preferredStartDate: LocalDate,
        preschoolDaycare: Boolean = false,
        childId: UUID = testChild_1.id,
        status: ApplicationStatus = ApplicationStatus.SENT
    ): UUID {
        return db.transaction { tx ->
            val applicationId = insertTestApplication(
                h = tx.handle,
                status = status,
                childId = childId,
                guardianId = testAdult_1.id,
                transferApplication = true
            )
            insertTestApplicationForm(
                h = tx.handle,
                applicationId = applicationId,
                document = DaycareFormV0.fromApplication2(validDaycareApplication).copy(
                    type = type,
                    preferredStartDate = preferredStartDate,
                    connectedDaycare = preschoolDaycare
                )
            )
            applicationId
        }
    }

    private fun createNormalApplication(
        type: ApplicationType,
        preferredStartDate: LocalDate,
        preparatory: Boolean = false,
        childId: UUID = testChild_1.id
    ): UUID {
        return db.transaction { tx ->
            val applicationId = insertTestApplication(
                h = tx.handle,
                status = ApplicationStatus.SENT,
                childId = childId,
                guardianId = testAdult_1.id
            )
            insertTestApplicationForm(
                h = tx.handle,
                applicationId = applicationId,
                document = DaycareFormV0.fromApplication2(validDaycareApplication).let { form ->
                    form.copy(
                        type = type,
                        preferredStartDate = preferredStartDate,
                        careDetails = form.careDetails.copy(preparatory = preparatory)
                    )
                }
            )
            applicationId
        }
    }

    private fun createPlacement(type: PlacementType, dateRange: FiniteDateRange, childId: UUID = testChild_1.id) {
        db.transaction {
            insertTestPlacement(
                it.handle,
                childId = childId,
                type = type,
                startDate = dateRange.start,
                endDate = dateRange.end,
                unitId = testDaycare.id
            )
        }
    }

    private fun getApplicationStatus(applicationId: UUID): ApplicationStatus {
        return db.read {
            it.createQuery("SELECT status FROM application WHERE id = :id")
                .bind("id", applicationId)
                .mapTo<ApplicationStatus>()
                .first()
        }
    }
}
