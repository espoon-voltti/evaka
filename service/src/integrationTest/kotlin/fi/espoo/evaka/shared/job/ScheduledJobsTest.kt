// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.job

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.application.ApplicationStateService
import fi.espoo.evaka.application.ApplicationStatus
import fi.espoo.evaka.application.ApplicationType
import fi.espoo.evaka.application.DaycarePlacementPlan
import fi.espoo.evaka.application.fetchApplicationDetails
import fi.espoo.evaka.application.getApplicationAttachments
import fi.espoo.evaka.application.persistence.daycare.DaycareFormV0
import fi.espoo.evaka.insertApplication
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.messaging.daycarydailynote.DaycareDailyNote
import fi.espoo.evaka.messaging.daycarydailynote.createDaycareDailyNote
import fi.espoo.evaka.messaging.daycarydailynote.getChildDaycareDailyNotes
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.placement.insertPlacement
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.dev.DevBackupCare
import fi.espoo.evaka.shared.dev.insertTestApplication
import fi.espoo.evaka.shared.dev.insertTestApplicationForm
import fi.espoo.evaka.shared.dev.insertTestBackupCare
import fi.espoo.evaka.shared.dev.insertTestPlacement
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.test.validDaycareApplication
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testAdult_5
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDecisionMaker_1
import org.jdbi.v3.core.kotlin.mapTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ScheduledJobsTest : FullApplicationTest() {
    @Autowired
    private lateinit var scheduledJobs: ScheduledJobs

    @Autowired
    private lateinit var applicationStateService: ApplicationStateService

    private val serviceWorker = AuthenticatedUser.Employee(id = testDecisionMaker_1.id, roles = setOf(UserRole.SERVICE_WORKER))

    @BeforeEach
    private fun beforeEach() {
        db.transaction { tx ->
            tx.resetDatabase()
            tx.insertGeneralTestFixtures()
        }
    }

    @Test
    fun `Draft application and attachments older than 30 days is cleaned up`() {
        val id_to_be_deleted = ApplicationId(UUID.randomUUID())
        val id_not_to_be_deleted = ApplicationId(UUID.randomUUID())
        val user = AuthenticatedUser.Citizen(testAdult_5.id)

        db.transaction { tx ->
            tx.insertApplication(guardian = testAdult_5, applicationId = id_to_be_deleted)
            setApplicationCreatedDate(tx, id_to_be_deleted, LocalDate.now().minusDays(32))

            tx.insertApplication(guardian = testAdult_5, applicationId = id_not_to_be_deleted)
            setApplicationCreatedDate(tx, id_not_to_be_deleted, LocalDate.now().minusDays(31))
        }

        db.transaction {
            uploadAttachment(id_to_be_deleted, user)
            uploadAttachment(id_not_to_be_deleted, user)
        }

        db.read {
            assertEquals(1, it.getApplicationAttachments(id_to_be_deleted).size)
            assertEquals(1, it.getApplicationAttachments(id_not_to_be_deleted).size)
        }

        scheduledJobs.removeOldDraftApplications(db)

        db.read {
            assertNull(it.fetchApplicationDetails(id_to_be_deleted))
            assertEquals(0, it.getApplicationAttachments(id_to_be_deleted).size)

            assertNotNull(it.fetchApplicationDetails(id_not_to_be_deleted)!!)
            assertEquals(1, it.getApplicationAttachments(id_not_to_be_deleted).size)
        }
    }

    private fun setApplicationCreatedDate(db: Database.Transaction, applicationId: ApplicationId, created: LocalDate) {
        db.handle.createUpdate("""UPDATE application SET created = :created WHERE id = :id""")
            .bind("created", created)
            .bind("id", applicationId)
            .execute()
    }

    @Test
    fun `a transfer application for a child without any placements is cancelled`() {
        val preferredStartDate = LocalDate.now().plusMonths(1)
        val applicationId = createTransferApplication(ApplicationType.DAYCARE, preferredStartDate)

        scheduledJobs.cancelOutdatedTransferApplications(db)

        val applicationStatus = getApplicationStatus(applicationId)
        assertEquals(ApplicationStatus.CANCELLED, applicationStatus)
    }

    @Test
    fun `a normal application for a child without any placements is not cancelled`() {
        val preferredStartDate = LocalDate.now().plusMonths(1)
        val applicationId = createNormalApplication(ApplicationType.DAYCARE, preferredStartDate)

        scheduledJobs.cancelOutdatedTransferApplications(db)

        val applicationStatus = getApplicationStatus(applicationId)
        assertEquals(ApplicationStatus.SENT, applicationStatus)
    }

    @Test
    fun `a daycare transfer application for a child with a daycare placement is not cancelled`() {
        val preferredStartDate = LocalDate.now().plusMonths(1)
        val applicationId = createTransferApplication(ApplicationType.DAYCARE, preferredStartDate)
        val dateRange = FiniteDateRange(preferredStartDate, preferredStartDate.plusMonths(1))
        createPlacement(PlacementType.DAYCARE, dateRange)

        scheduledJobs.cancelOutdatedTransferApplications(db)

        val applicationStatus = getApplicationStatus(applicationId)
        assertEquals(ApplicationStatus.SENT, applicationStatus)
    }

    @Test
    fun `a daycare transfer application for a child with a future daycare placement is not cancelled`() {
        val preferredStartDate = LocalDate.now().plusMonths(1)
        val applicationId = createTransferApplication(ApplicationType.DAYCARE, preferredStartDate)
        val dateRange = FiniteDateRange(preferredStartDate.plusMonths(1), preferredStartDate.plusMonths(2))
        createPlacement(PlacementType.DAYCARE, dateRange)

        scheduledJobs.cancelOutdatedTransferApplications(db)

        val applicationStatus = getApplicationStatus(applicationId)
        assertEquals(ApplicationStatus.SENT, applicationStatus)
    }

    @Test
    fun `a daycare transfer application for a child with a preschool placement is cancelled`() {
        val preferredStartDate = LocalDate.now().plusMonths(1)
        val applicationId = createTransferApplication(ApplicationType.DAYCARE, preferredStartDate)
        val dateRange = FiniteDateRange(preferredStartDate, preferredStartDate.plusMonths(1))
        createPlacement(PlacementType.PRESCHOOL, dateRange)

        scheduledJobs.cancelOutdatedTransferApplications(db)

        val applicationStatus = getApplicationStatus(applicationId)
        assertEquals(ApplicationStatus.CANCELLED, applicationStatus)
    }

    @Test
    fun `a daycare transfer application for a child with a past daycare placement is cancelled`() {
        val preferredStartDate = LocalDate.now().minusMonths(2)
        val applicationId = createTransferApplication(ApplicationType.DAYCARE, preferredStartDate)
        val dateRange = FiniteDateRange(preferredStartDate.minusMonths(1), LocalDate.now().minusMonths(1))
        createPlacement(PlacementType.DAYCARE, dateRange)

        scheduledJobs.cancelOutdatedTransferApplications(db)

        val applicationStatus = getApplicationStatus(applicationId)
        assertEquals(ApplicationStatus.CANCELLED, applicationStatus)
    }

    @Test
    fun `a daycare transfer application for a child with a daycare placement that ends today is not cancelled`() {
        val preferredStartDate = LocalDate.now().minusMonths(2)
        val applicationId = createTransferApplication(ApplicationType.DAYCARE, preferredStartDate)
        val dateRange = FiniteDateRange(preferredStartDate.minusMonths(1), LocalDate.now())
        createPlacement(PlacementType.DAYCARE, dateRange)

        scheduledJobs.cancelOutdatedTransferApplications(db)

        val applicationStatus = getApplicationStatus(applicationId)
        assertEquals(ApplicationStatus.SENT, applicationStatus)
    }

    @Test
    fun `a daycare transfer application for a child with a past daycare placement that ended yesterday is cancelled`() {
        val preferredStartDate = LocalDate.now().minusMonths(2)
        val applicationId = createTransferApplication(ApplicationType.DAYCARE, preferredStartDate)
        val dateRange = FiniteDateRange(preferredStartDate.minusMonths(1), LocalDate.now().minusDays(1))
        createPlacement(PlacementType.DAYCARE, dateRange)

        scheduledJobs.cancelOutdatedTransferApplications(db)

        val applicationStatus = getApplicationStatus(applicationId)
        assertEquals(ApplicationStatus.CANCELLED, applicationStatus)
    }

    @Test
    fun `a preschool transfer application for a child with a preschool placement is not cancelled`() {
        val preferredStartDate = LocalDate.now().minusMonths(2)
        val applicationId = createTransferApplication(ApplicationType.PRESCHOOL, preferredStartDate)
        val dateRange = FiniteDateRange(preferredStartDate.minusMonths(1), LocalDate.now().plusMonths(1))
        createPlacement(PlacementType.PRESCHOOL, dateRange)

        scheduledJobs.cancelOutdatedTransferApplications(db)

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

        scheduledJobs.cancelOutdatedTransferApplications(db)

        val applicationStatus = getApplicationStatus(applicationId)
        assertEquals(ApplicationStatus.CANCELLED, applicationStatus)
    }

    @Test
    fun `a preschool transfer application for a child with a preparatory placement is not cancelled`() {
        val preferredStartDate = LocalDate.now().minusMonths(2)
        val applicationId = createTransferApplication(ApplicationType.PRESCHOOL, preferredStartDate)
        val dateRange = FiniteDateRange(preferredStartDate.minusMonths(1), LocalDate.now().plusMonths(1))
        createPlacement(PlacementType.PREPARATORY, dateRange)

        scheduledJobs.cancelOutdatedTransferApplications(db)

        val applicationStatus = getApplicationStatus(applicationId)
        assertEquals(ApplicationStatus.SENT, applicationStatus)
    }

    @Test
    fun `a preschool transfer application for a child with a preschool daycare placement is not cancelled`() {
        val preferredStartDate = LocalDate.now().minusMonths(2)
        val applicationId = createTransferApplication(ApplicationType.PRESCHOOL, preferredStartDate)
        val dateRange = FiniteDateRange(preferredStartDate.minusMonths(1), LocalDate.now().plusMonths(1))
        createPlacement(PlacementType.PRESCHOOL_DAYCARE, dateRange)

        scheduledJobs.cancelOutdatedTransferApplications(db)

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

        scheduledJobs.cancelOutdatedTransferApplications(db)

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

        scheduledJobs.cancelOutdatedTransferApplications(db)

        val applicationStatus = getApplicationStatus(applicationId)
        assertEquals(ApplicationStatus.WAITING_CONFIRMATION, applicationStatus)
    }

    @Test
    fun removeDaycareDailyNotes() {
        val now = Instant.now()
        val twelveHoursAgo = now.minusSeconds(60 * 60 * 12)
        val expiredNoteId = UUID.randomUUID()
        val validNoteId = UUID.randomUUID()
        db.transaction {
            it.createDaycareDailyNote(
                DaycareDailyNote(
                    id = expiredNoteId,
                    childId = testChild_1.id,
                    date = LocalDate.ofInstant(twelveHoursAgo, ZoneOffset.UTC),
                    modifiedAt = twelveHoursAgo,
                    feedingNote = null,
                    note = null,
                    reminderNote = null,
                    sleepingMinutes = null,
                    reminders = emptyList(),
                    modifiedBy = "integrationTest",
                    groupId = null,
                    sleepingNote = null
                )
            )

            it.createDaycareDailyNote(
                DaycareDailyNote(
                    id = validNoteId,
                    childId = testChild_1.id,
                    date = LocalDate.ofInstant(now, ZoneOffset.UTC),
                    modifiedAt = now,
                    feedingNote = null,
                    note = null,
                    reminderNote = null,
                    sleepingMinutes = null,
                    reminders = emptyList(),
                    modifiedBy = "integrationTest",
                    groupId = null,
                    sleepingNote = null
                )
            )
            it.insertPlacement(
                type = PlacementType.DAYCARE,
                childId = testChild_1.id,
                unitId = testDaycare.id,
                startDate = LocalDate.now().minusDays(100),
                endDate = LocalDate.now().plusDays(100),
            )
        }

        scheduledJobs.removeOldDaycareDailyNotes(db)

        db.read {
            val notesAfterCleanup = it.getChildDaycareDailyNotes(testChild_1.id)
            assertEquals(1, notesAfterCleanup.size)
            assertEquals(validNoteId, notesAfterCleanup.get(0).id)
        }
    }

    @Test
    fun removeOldBackupCareDaycareDailyNotes() {
        val now = Instant.now()
        val twelveHoursAgo = now.minusSeconds(60 * 60 * 12)
        val expiredNoteId = UUID.randomUUID()
        val validNoteId = UUID.randomUUID()
        db.transaction {
            it.createDaycareDailyNote(
                DaycareDailyNote(
                    id = expiredNoteId,
                    childId = testChild_1.id,
                    date = LocalDate.ofInstant(twelveHoursAgo, ZoneOffset.UTC),
                    modifiedAt = twelveHoursAgo,
                    feedingNote = null,
                    note = null,
                    reminderNote = null,
                    sleepingMinutes = null,
                    reminders = emptyList(),
                    modifiedBy = "integrationTest",
                    groupId = null,
                    sleepingNote = null
                )
            )

            it.createDaycareDailyNote(
                DaycareDailyNote(
                    id = validNoteId,
                    childId = testChild_1.id,
                    date = LocalDate.ofInstant(now, ZoneOffset.UTC),
                    modifiedAt = now,
                    feedingNote = null,
                    note = null,
                    reminderNote = null,
                    sleepingMinutes = null,
                    reminders = emptyList(),
                    modifiedBy = "integrationTest",
                    groupId = null,
                    sleepingNote = null
                )
            )
            it.insertTestBackupCare(
                DevBackupCare(
                    childId = testChild_1.id,
                    groupId = null,
                    unitId = testDaycare.id,
                    period = FiniteDateRange(
                        LocalDate.now().minusDays(100),
                        LocalDate.now().plusDays(100)
                    )
                )
            )
        }

        scheduledJobs.removeOldDaycareDailyNotes(db)

        db.read {
            val notesAfterCleanup = it.getChildDaycareDailyNotes(testChild_1.id)
            assertEquals(1, notesAfterCleanup.size)
            assertEquals(validNoteId, notesAfterCleanup.get(0).id)
        }
    }

    private fun createTransferApplication(
        type: ApplicationType,
        preferredStartDate: LocalDate,
        preschoolDaycare: Boolean = false,
        childId: UUID = testChild_1.id,
        status: ApplicationStatus = ApplicationStatus.SENT
    ): ApplicationId {
        return db.transaction { tx ->
            val applicationId = tx.insertTestApplication(
                status = status,
                childId = childId,
                guardianId = testAdult_1.id,
                transferApplication = true
            )
            tx.insertTestApplicationForm(
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
    ): ApplicationId {
        return db.transaction { tx ->
            val applicationId = tx.insertTestApplication(
                status = ApplicationStatus.SENT,
                childId = childId,
                guardianId = testAdult_1.id
            )
            tx.insertTestApplicationForm(
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
            it.insertTestPlacement(
                childId = childId,
                type = type,
                startDate = dateRange.start,
                endDate = dateRange.end,
                unitId = testDaycare.id
            )
        }
    }

    private fun getApplicationStatus(applicationId: ApplicationId): ApplicationStatus {
        return db.read {
            it.createQuery("SELECT status FROM application WHERE id = :id")
                .bind("id", applicationId)
                .mapTo<ApplicationStatus>()
                .first()
        }
    }
}
