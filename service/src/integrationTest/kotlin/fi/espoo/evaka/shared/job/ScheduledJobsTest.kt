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
import fi.espoo.evaka.note.child.daily.ChildDailyNoteBody
import fi.espoo.evaka.note.child.daily.createChildDailyNote
import fi.espoo.evaka.note.child.daily.getChildDailyNoteForChild
import fi.espoo.evaka.note.child.sticky.ChildStickyNoteBody
import fi.espoo.evaka.note.child.sticky.createChildStickyNote
import fi.espoo.evaka.note.child.sticky.getChildStickyNotesForChild
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.placement.insertPlacement
import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.CitizenAuthLevel
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.dev.DevBackupCare
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.dev.insertTestApplication
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.RealEvakaClock
import fi.espoo.evaka.test.validDaycareApplication
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testAdult_5
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testChild_2
import fi.espoo.evaka.testChild_6
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDecisionMaker_1
import fi.espoo.evaka.vtjclient.service.persondetails.MockPersonDetailsService
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class ScheduledJobsTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var scheduledJobs: ScheduledJobs

    @Autowired private lateinit var applicationStateService: ApplicationStateService

    private val serviceWorker =
        AuthenticatedUser.Employee(
            id = testDecisionMaker_1.id,
            roles = setOf(UserRole.SERVICE_WORKER)
        )

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insertGeneralTestFixtures()
            listOf(testAdult_1, testAdult_5).forEach { tx.insert(it, DevPersonType.ADULT) }
            listOf(testChild_1, testChild_2, testChild_6).forEach {
                tx.insert(it, DevPersonType.CHILD)
            }
        }
    }

    @Test
    fun `Draft application and attachments older than 60 days is cleaned up`() {
        val idToBeDeleted = ApplicationId(UUID.randomUUID())
        val idNotToBeDeleted = ApplicationId(UUID.randomUUID())
        val user = AuthenticatedUser.Citizen(testAdult_5.id, CitizenAuthLevel.STRONG)

        db.transaction { tx ->
            tx.insertApplication(guardian = testAdult_5, applicationId = idToBeDeleted)
            setApplicationCreatedDate(tx, idToBeDeleted, LocalDate.now().minusDays(61))

            tx.insertApplication(guardian = testAdult_5, applicationId = idNotToBeDeleted)
            setApplicationCreatedDate(tx, idNotToBeDeleted, LocalDate.now().minusDays(60))
        }

        db.transaction {
            uploadAttachment(idToBeDeleted, user)
            uploadAttachment(idNotToBeDeleted, user)
        }

        db.read {
            assertEquals(1, it.getApplicationAttachments(idToBeDeleted).size)
            assertEquals(1, it.getApplicationAttachments(idNotToBeDeleted).size)
        }

        scheduledJobs.removeOldDraftApplications(db, RealEvakaClock())

        db.read {
            assertNull(it.fetchApplicationDetails(idToBeDeleted))
            assertEquals(0, it.getApplicationAttachments(idToBeDeleted).size)

            assertNotNull(it.fetchApplicationDetails(idNotToBeDeleted)!!)
            assertEquals(1, it.getApplicationAttachments(idNotToBeDeleted).size)
        }
    }

    private fun setApplicationCreatedDate(
        db: Database.Transaction,
        applicationId: ApplicationId,
        created: LocalDate
    ) {
        db.handle
            .createUpdate("""UPDATE application SET created = :created WHERE id = :id""")
            .bind("created", created)
            .bind("id", applicationId)
            .execute()
    }

    @Test
    fun `a transfer application for a child without any placements is cancelled`() {
        val preferredStartDate = LocalDate.now().plusMonths(1)
        val applicationId = createTransferApplication(ApplicationType.DAYCARE, preferredStartDate)

        scheduledJobs.cancelOutdatedTransferApplications(db, RealEvakaClock())

        val applicationStatus = getApplicationStatus(applicationId)
        assertEquals(ApplicationStatus.CANCELLED, applicationStatus)
    }

    @Test
    fun `a normal application for a child without any placements is not cancelled`() {
        val preferredStartDate = LocalDate.now().plusMonths(1)
        val applicationId = createNormalApplication(ApplicationType.DAYCARE, preferredStartDate)

        scheduledJobs.cancelOutdatedTransferApplications(db, RealEvakaClock())

        val applicationStatus = getApplicationStatus(applicationId)
        assertEquals(ApplicationStatus.SENT, applicationStatus)
    }

    @Test
    fun `a daycare transfer application for a child with a daycare placement is not cancelled`() {
        val preferredStartDate = LocalDate.now().plusMonths(1)
        val applicationId = createTransferApplication(ApplicationType.DAYCARE, preferredStartDate)
        val dateRange = FiniteDateRange(preferredStartDate, preferredStartDate.plusMonths(1))
        createPlacement(PlacementType.DAYCARE, dateRange)

        scheduledJobs.cancelOutdatedTransferApplications(db, RealEvakaClock())

        val applicationStatus = getApplicationStatus(applicationId)
        assertEquals(ApplicationStatus.SENT, applicationStatus)
    }

    @Test
    fun `a daycare transfer application for a child with a 5yo daycare placement is not cancelled`() {
        val preferredStartDate = LocalDate.now().plusMonths(1)
        val applicationId = createTransferApplication(ApplicationType.DAYCARE, preferredStartDate)
        val dateRange = FiniteDateRange(preferredStartDate, preferredStartDate.plusMonths(1))
        createPlacement(PlacementType.DAYCARE_FIVE_YEAR_OLDS, dateRange)

        scheduledJobs.cancelOutdatedTransferApplications(db, RealEvakaClock())

        val applicationStatus = getApplicationStatus(applicationId)
        assertEquals(ApplicationStatus.SENT, applicationStatus)
    }

    @Test
    fun `a daycare transfer application for a child with a future daycare placement is not cancelled`() {
        val preferredStartDate = LocalDate.now().plusMonths(1)
        val applicationId = createTransferApplication(ApplicationType.DAYCARE, preferredStartDate)
        val dateRange =
            FiniteDateRange(preferredStartDate.plusMonths(1), preferredStartDate.plusMonths(2))
        createPlacement(PlacementType.DAYCARE, dateRange)

        scheduledJobs.cancelOutdatedTransferApplications(db, RealEvakaClock())

        val applicationStatus = getApplicationStatus(applicationId)
        assertEquals(ApplicationStatus.SENT, applicationStatus)
    }

    @Test
    fun `a daycare transfer application for a child with a preschool placement is cancelled`() {
        val preferredStartDate = LocalDate.now().plusMonths(1)
        val applicationId = createTransferApplication(ApplicationType.DAYCARE, preferredStartDate)
        val dateRange = FiniteDateRange(preferredStartDate, preferredStartDate.plusMonths(1))
        createPlacement(PlacementType.PRESCHOOL, dateRange)

        scheduledJobs.cancelOutdatedTransferApplications(db, RealEvakaClock())

        val applicationStatus = getApplicationStatus(applicationId)
        assertEquals(ApplicationStatus.CANCELLED, applicationStatus)
    }

    @Test
    fun `a daycare transfer application for a child with a past daycare placement is cancelled`() {
        val preferredStartDate = LocalDate.now().minusMonths(2)
        val applicationId = createTransferApplication(ApplicationType.DAYCARE, preferredStartDate)
        val dateRange =
            FiniteDateRange(preferredStartDate.minusMonths(1), LocalDate.now().minusMonths(1))
        createPlacement(PlacementType.DAYCARE, dateRange)

        scheduledJobs.cancelOutdatedTransferApplications(db, RealEvakaClock())

        val applicationStatus = getApplicationStatus(applicationId)
        assertEquals(ApplicationStatus.CANCELLED, applicationStatus)
    }

    @Test
    fun `a daycare transfer application for a child with a daycare placement that ends today is not cancelled`() {
        val preferredStartDate = LocalDate.now().minusMonths(2)
        val applicationId = createTransferApplication(ApplicationType.DAYCARE, preferredStartDate)
        val dateRange = FiniteDateRange(preferredStartDate.minusMonths(1), LocalDate.now())
        createPlacement(PlacementType.DAYCARE, dateRange)

        scheduledJobs.cancelOutdatedTransferApplications(db, RealEvakaClock())

        val applicationStatus = getApplicationStatus(applicationId)
        assertEquals(ApplicationStatus.SENT, applicationStatus)
    }

    @Test
    fun `a daycare transfer application for a child with a past daycare placement that ended day before yesterday is cancelled`() {
        val preferredStartDate = LocalDate.now().minusMonths(2)
        val applicationId = createTransferApplication(ApplicationType.DAYCARE, preferredStartDate)
        val dateRange =
            FiniteDateRange(preferredStartDate.minusMonths(1), LocalDate.now().minusDays(2))
        createPlacement(PlacementType.DAYCARE, dateRange)

        scheduledJobs.cancelOutdatedTransferApplications(db, RealEvakaClock())

        val applicationStatus = getApplicationStatus(applicationId)
        assertEquals(ApplicationStatus.CANCELLED, applicationStatus)
    }

    @Test
    fun `a preschool transfer application for a child with a preschool placement is not cancelled`() {
        val preferredStartDate = LocalDate.now().minusMonths(2)
        val applicationId = createTransferApplication(ApplicationType.PRESCHOOL, preferredStartDate)
        val dateRange =
            FiniteDateRange(preferredStartDate.minusMonths(1), LocalDate.now().plusMonths(1))
        createPlacement(PlacementType.PRESCHOOL, dateRange)

        scheduledJobs.cancelOutdatedTransferApplications(db, RealEvakaClock())

        val applicationStatus = getApplicationStatus(applicationId)
        assertEquals(ApplicationStatus.SENT, applicationStatus)
    }

    @Test
    fun `a preschool daycare transfer application for a child with a preschool placement is not cancelled`() {
        val preferredStartDate = LocalDate.now().minusMonths(2)
        val applicationId =
            createTransferApplication(
                ApplicationType.PRESCHOOL,
                preferredStartDate,
                preschoolDaycare = true
            )
        val dateRange =
            FiniteDateRange(preferredStartDate.minusMonths(1), LocalDate.now().plusMonths(1))
        createPlacement(PlacementType.PRESCHOOL, dateRange)

        scheduledJobs.cancelOutdatedTransferApplications(db, RealEvakaClock())

        val applicationStatus = getApplicationStatus(applicationId)
        assertEquals(ApplicationStatus.SENT, applicationStatus)
    }

    @Test
    fun `a preschool transfer application for a child with a preparatory placement is not cancelled`() {
        val preferredStartDate = LocalDate.now().minusMonths(2)
        val applicationId = createTransferApplication(ApplicationType.PRESCHOOL, preferredStartDate)
        val dateRange =
            FiniteDateRange(preferredStartDate.minusMonths(1), LocalDate.now().plusMonths(1))
        createPlacement(PlacementType.PREPARATORY, dateRange)

        scheduledJobs.cancelOutdatedTransferApplications(db, RealEvakaClock())

        val applicationStatus = getApplicationStatus(applicationId)
        assertEquals(ApplicationStatus.SENT, applicationStatus)
    }

    @Test
    fun `a preschool transfer application for a child with a preschool daycare placement is not cancelled`() {
        val preferredStartDate = LocalDate.now().minusMonths(2)
        val applicationId = createTransferApplication(ApplicationType.PRESCHOOL, preferredStartDate)
        val dateRange =
            FiniteDateRange(preferredStartDate.minusMonths(1), LocalDate.now().plusMonths(1))
        createPlacement(PlacementType.PRESCHOOL_DAYCARE, dateRange)

        scheduledJobs.cancelOutdatedTransferApplications(db, RealEvakaClock())

        val applicationStatus = getApplicationStatus(applicationId)
        assertEquals(ApplicationStatus.SENT, applicationStatus)
    }

    @Test
    fun `a transfer application with a decision does not get canceled`() {
        val preferredStartDate = LocalDate.now()
        MockPersonDetailsService.addPersons(testAdult_1, testChild_1)
        MockPersonDetailsService.addDependants(testAdult_1, testChild_1)
        val applicationId =
            createTransferApplication(
                ApplicationType.PRESCHOOL,
                preferredStartDate,
                status = ApplicationStatus.WAITING_PLACEMENT
            )
        db.transaction {
            applicationStateService.createPlacementPlan(
                it,
                serviceWorker,
                RealEvakaClock(),
                applicationId,
                DaycarePlacementPlan(
                    testDaycare.id,
                    FiniteDateRange(preferredStartDate, preferredStartDate.plusMonths(1))
                )
            )
            applicationStateService.sendDecisionsWithoutProposal(
                it,
                serviceWorker,
                RealEvakaClock(),
                applicationId
            )
        }

        scheduledJobs.cancelOutdatedTransferApplications(db, RealEvakaClock())

        val applicationStatus = getApplicationStatus(applicationId)
        assertEquals(ApplicationStatus.WAITING_CONFIRMATION, applicationStatus)
    }

    @Test
    fun removeExpiredStickyNotes() {
        // expired note
        db.transaction { tx ->
            tx.createChildStickyNote(
                childId = testChild_1.id,
                note = ChildStickyNoteBody(note = "", expires = LocalDate.now().minusDays(1))
            )
        }

        val validNoteId =
            db.transaction { tx ->
                tx.createChildStickyNote(
                    childId = testChild_1.id,
                    note = ChildStickyNoteBody(note = "", expires = LocalDate.now())
                )
            }

        db.read {
            val notesBeforeCleanup = it.getChildStickyNotesForChild(testChild_1.id)
            assertEquals(2, notesBeforeCleanup.size)
        }
        scheduledJobs.removeExpiredNotes(db, RealEvakaClock())

        db.read {
            val notesAfterCleanup = it.getChildStickyNotesForChild(testChild_1.id)
            assertEquals(1, notesAfterCleanup.size)
            assertEquals(validNoteId, notesAfterCleanup.first().id)
        }
    }

    @Test
    fun removeDaycareDailyNotes() {
        val now = Instant.now()
        createExpiredDailyNote(now)

        val validNoteId =
            db.transaction {
                it.insertPlacement(
                    type = PlacementType.DAYCARE,
                    childId = testChild_2.id,
                    unitId = testDaycare.id,
                    startDate = LocalDate.now().minusDays(100),
                    endDate = LocalDate.now().plusDays(100),
                    false
                )
                it.createChildDailyNote(
                    testChild_2.id,
                    ChildDailyNoteBody(
                        feedingNote = null,
                        note = "",
                        reminderNote = "",
                        sleepingMinutes = null,
                        reminders = emptyList(),
                        sleepingNote = null
                    )
                )
            }

        scheduledJobs.removeExpiredNotes(db, RealEvakaClock())

        db.read {
            val note1AfterCleanup = it.getChildDailyNoteForChild(testChild_1.id)
            assertNull(note1AfterCleanup)
            val note2AfterCleanup = it.getChildDailyNoteForChild(testChild_2.id)
            assertNotNull(note2AfterCleanup)
            assertEquals(validNoteId, note2AfterCleanup.id)
        }
    }

    private fun createExpiredDailyNote(now: Instant) {
        db.transaction {
            val sixteenHoursAgo = now - Duration.ofHours(16)
            it.createChildDailyNote(
                    testChild_1.id,
                    ChildDailyNoteBody(
                        feedingNote = null,
                        note = "",
                        reminderNote = "",
                        sleepingMinutes = null,
                        reminders = emptyList(),
                        sleepingNote = null
                    )
                )
                .let { id ->
                    @Suppress("DEPRECATION")
                    it.createUpdate(
                            "UPDATE child_daily_note SET modified_at = :date WHERE id = :id"
                        )
                        .bind("id", id)
                        .bind("date", sixteenHoursAgo)
                        .updateExactlyOne()
                }
        }
    }

    @Test
    fun removeOldBackupCareDaycareDailyNotes() {
        val now = Instant.now()
        createExpiredDailyNote(now)

        val validNoteId =
            db.transaction {
                it.insert(
                    DevPlacement(
                        childId = testChild_2.id,
                        unitId = testDaycare.id,
                        startDate = LocalDate.now().minusDays(150),
                        endDate = LocalDate.now().plusDays(150)
                    )
                )
                it.insert(
                    DevBackupCare(
                        childId = testChild_2.id,
                        groupId = null,
                        unitId = testDaycare.id,
                        period =
                            FiniteDateRange(
                                LocalDate.now().minusDays(100),
                                LocalDate.now().plusDays(100)
                            )
                    )
                )
                it.createChildDailyNote(
                    testChild_2.id,
                    ChildDailyNoteBody(
                        feedingNote = null,
                        note = "",
                        reminderNote = "",
                        sleepingMinutes = null,
                        reminders = emptyList(),
                        sleepingNote = null
                    )
                )
            }

        scheduledJobs.removeExpiredNotes(db, RealEvakaClock())

        db.read {
            val note1AfterCleanup = it.getChildDailyNoteForChild(testChild_1.id)
            assertNull(note1AfterCleanup)
            val note2AfterCleanup = it.getChildDailyNoteForChild(testChild_2.id)
            assertNotNull(note2AfterCleanup)
            assertEquals(validNoteId, note2AfterCleanup.id)
        }
    }

    private fun createTransferApplication(
        type: ApplicationType,
        preferredStartDate: LocalDate,
        preschoolDaycare: Boolean = false,
        childId: ChildId = testChild_1.id,
        status: ApplicationStatus = ApplicationStatus.SENT
    ): ApplicationId {
        return db.transaction { tx ->
            tx.insertTestApplication(
                status = status,
                childId = childId,
                guardianId = testAdult_1.id,
                transferApplication = true,
                type = type,
                document =
                    DaycareFormV0.fromApplication2(validDaycareApplication)
                        .copy(
                            type = type,
                            preferredStartDate = preferredStartDate,
                            connectedDaycare = preschoolDaycare
                        )
            )
        }
    }

    private fun createNormalApplication(
        type: ApplicationType,
        preferredStartDate: LocalDate,
        preparatory: Boolean = false,
        childId: ChildId = testChild_1.id
    ): ApplicationId {
        return db.transaction { tx ->
            tx.insertTestApplication(
                status = ApplicationStatus.SENT,
                childId = childId,
                guardianId = testAdult_1.id,
                type = type,
                document =
                    DaycareFormV0.fromApplication2(validDaycareApplication).let { form ->
                        form.copy(
                            type = type,
                            preferredStartDate = preferredStartDate,
                            careDetails = form.careDetails.copy(preparatory = preparatory)
                        )
                    }
            )
        }
    }

    private fun createPlacement(
        type: PlacementType,
        dateRange: FiniteDateRange,
        childId: ChildId = testChild_1.id
    ) {
        db.transaction {
            it.insert(
                DevPlacement(
                    type = type,
                    childId = childId,
                    unitId = testDaycare.id,
                    startDate = dateRange.start,
                    endDate = dateRange.end
                )
            )
        }
    }

    private fun getApplicationStatus(applicationId: ApplicationId): ApplicationStatus {
        return db.read {
            @Suppress("DEPRECATION")
            it.createQuery("SELECT status FROM application WHERE id = :id")
                .bind("id", applicationId)
                .exactlyOne<ApplicationStatus>()
        }
    }
}
