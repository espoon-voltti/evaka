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
import fi.espoo.evaka.note.child.daily.ChildDailyNoteBody
import fi.espoo.evaka.note.child.daily.createChildDailyNote
import fi.espoo.evaka.note.child.daily.getChildDailyNoteForChild
import fi.espoo.evaka.note.child.sticky.ChildStickyNoteBody
import fi.espoo.evaka.note.child.sticky.createChildStickyNote
import fi.espoo.evaka.note.child.sticky.getChildStickyNotesForChild
import fi.espoo.evaka.pis.service.getGuardianChildIds
import fi.espoo.evaka.pis.service.insertGuardian
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.CitizenAuthLevel
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.getDaycareAclRows
import fi.espoo.evaka.shared.auth.insertDaycareAclRow
import fi.espoo.evaka.shared.auth.insertScheduledDaycareAclRow
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.dev.DevBackupCare
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.dev.insertTestApplication
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.shared.domain.RealEvakaClock
import fi.espoo.evaka.test.validDaycareApplication
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testAdult_5
import fi.espoo.evaka.testArea
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testChild_2
import fi.espoo.evaka.testChild_6
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDecisionMaker_1
import fi.espoo.evaka.vtjclient.service.persondetails.MockPersonDetailsService
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class ScheduledJobsTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var scheduledJobs: ScheduledJobs

    @Autowired private lateinit var applicationStateService: ApplicationStateService

    private val serviceWorker =
        AuthenticatedUser.Employee(
            id = testDecisionMaker_1.id,
            roles = setOf(UserRole.SERVICE_WORKER),
        )

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insert(testDecisionMaker_1)
            tx.insert(testArea)
            tx.insert(testDaycare)
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
        tx: Database.Transaction,
        applicationId: ApplicationId,
        created: LocalDate,
    ) =
        tx.execute {
            sql(
                "UPDATE application SET created_at = ${bind(created)} WHERE id = ${bind(applicationId)}"
            )
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
                preschoolDaycare = true,
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
                status = ApplicationStatus.WAITING_PLACEMENT,
            )
        db.transaction {
            applicationStateService.setVerified(
                it,
                serviceWorker,
                RealEvakaClock(),
                applicationId,
                false,
            )
            applicationStateService.createPlacementPlan(
                it,
                serviceWorker,
                RealEvakaClock(),
                applicationId,
                DaycarePlacementPlan(
                    testDaycare.id,
                    FiniteDateRange(preferredStartDate, preferredStartDate.plusMonths(1)),
                ),
            )
            applicationStateService.sendDecisionsWithoutProposal(
                it,
                serviceWorker,
                RealEvakaClock(),
                applicationId,
            )
        }

        scheduledJobs.cancelOutdatedTransferApplications(db, RealEvakaClock())

        val applicationStatus = getApplicationStatus(applicationId)
        assertEquals(ApplicationStatus.WAITING_CONFIRMATION, applicationStatus)
    }

    @Test
    fun `RemoveGuardiansFromAdults removes guardianships where child is already 18`() {
        val today = LocalDate.of(2024, 8, 15)
        val clock = MockEvakaClock(HelsinkiDateTime.of(today, LocalTime.of(2, 0)))

        val adult = DevPerson()
        val child1 = DevPerson(dateOfBirth = today.minusYears(18).minusDays(1))
        val child2 = DevPerson(dateOfBirth = today.minusYears(18))
        val child3 = DevPerson(dateOfBirth = today.minusYears(18).plusDays(1))
        db.transaction { tx ->
            tx.insert(adult, DevPersonType.ADULT)
            tx.insert(child1, DevPersonType.CHILD)
            tx.insert(child2, DevPersonType.CHILD)
            tx.insert(child3, DevPersonType.CHILD)
            tx.insertGuardian(adult.id, child1.id)
            tx.insertGuardian(adult.id, child2.id)
            tx.insertGuardian(adult.id, child3.id)
        }

        db.read { tx ->
            assertEquals(
                setOf(child1.id, child2.id, child3.id),
                tx.getGuardianChildIds(adult.id).toSet(),
            )
        }

        scheduledJobs.removeGuardiansFromAdults(db, clock)

        db.read { tx -> assertEquals(setOf(child3.id), tx.getGuardianChildIds(adult.id).toSet()) }
    }

    @Test
    fun removeExpiredStickyNotes() {
        // expired note
        db.transaction { tx ->
            tx.createChildStickyNote(
                childId = testChild_1.id,
                note = ChildStickyNoteBody(note = "", expires = LocalDate.now().minusDays(1)),
            )
        }

        val validNoteId =
            db.transaction { tx ->
                tx.createChildStickyNote(
                    childId = testChild_1.id,
                    note = ChildStickyNoteBody(note = "", expires = LocalDate.now()),
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
                it.insert(
                    DevPlacement(
                        childId = testChild_2.id,
                        unitId = testDaycare.id,
                        startDate = LocalDate.now().minusDays(100),
                        endDate = LocalDate.now().plusDays(100),
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
                        sleepingNote = null,
                    ),
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

    @Test
    fun `syncAclRows removes rows where end date has passed`() {
        val now = HelsinkiDateTime.of(LocalDate.of(2024, 5, 1), LocalTime.of(0, 5))
        val today = now.toLocalDate()

        val daycare1 = DevDaycare(areaId = testArea.id)
        val daycare2 = DevDaycare(areaId = testArea.id)
        val daycare3 = DevDaycare(areaId = testArea.id)
        val daycare4 = DevDaycare(areaId = testArea.id)
        val staff = DevEmployee()
        db.transaction { tx ->
            tx.insert(daycare1)
            tx.insert(daycare2)
            tx.insert(daycare3)
            tx.insert(daycare4)
            tx.insert(staff)
            tx.insertDaycareAclRow(daycare1.id, staff.id, UserRole.STAFF, endDate = null)
            tx.insertDaycareAclRow(
                daycare2.id,
                staff.id,
                UserRole.STAFF,
                endDate = today.minusDays(1),
            )
            tx.insertDaycareAclRow(daycare3.id, staff.id, UserRole.STAFF, endDate = today)
            tx.insertDaycareAclRow(
                daycare4.id,
                staff.id,
                UserRole.STAFF,
                endDate = today.plusDays(1),
            )
        }
        db.read { tx ->
            assertEquals(
                1,
                tx.getDaycareAclRows(daycare1.id, false, includeStaffEmployeeNumber = false).size,
            )
            assertEquals(
                1,
                tx.getDaycareAclRows(daycare2.id, false, includeStaffEmployeeNumber = false).size,
            )
            assertEquals(
                1,
                tx.getDaycareAclRows(daycare3.id, false, includeStaffEmployeeNumber = false).size,
            )
            assertEquals(
                1,
                tx.getDaycareAclRows(daycare4.id, false, includeStaffEmployeeNumber = false).size,
            )
        }

        scheduledJobs.syncAclRows(db, MockEvakaClock(now))

        db.read { tx ->
            assertEquals(
                1,
                tx.getDaycareAclRows(daycare1.id, false, includeStaffEmployeeNumber = false).size,
            )
            assertEquals(
                0,
                tx.getDaycareAclRows(daycare2.id, false, includeStaffEmployeeNumber = false).size,
            )
            assertEquals(
                1,
                tx.getDaycareAclRows(daycare3.id, false, includeStaffEmployeeNumber = false).size,
            )
            assertEquals(
                1,
                tx.getDaycareAclRows(daycare4.id, false, includeStaffEmployeeNumber = false).size,
            )
        }
    }

    @Test
    fun `syncAclRows upserts scheduled rows where start date has come`() {
        val now = HelsinkiDateTime.of(LocalDate.of(2024, 5, 1), LocalTime.of(0, 5))
        val today = now.toLocalDate()

        val daycare1 = DevDaycare(areaId = testArea.id)
        val daycare2 = DevDaycare(areaId = testArea.id)
        val daycare3 = DevDaycare(areaId = testArea.id)
        val employee = DevEmployee()
        db.transaction { tx ->
            tx.insert(daycare1)
            tx.insert(daycare2)
            tx.insert(daycare3)
            tx.insert(employee)
            tx.insertDaycareAclRow(daycare1.id, employee.id, UserRole.STAFF, endDate = null)
            tx.insertScheduledDaycareAclRow(
                employeeId = employee.id,
                daycareIds = listOf(daycare1.id),
                role = UserRole.SPECIAL_EDUCATION_TEACHER,
                startDate = today,
                endDate = null,
            )
            tx.insertScheduledDaycareAclRow(
                employeeId = employee.id,
                daycareIds = listOf(daycare2.id),
                role = UserRole.SPECIAL_EDUCATION_TEACHER,
                startDate = today.minusDays(1),
                endDate = null,
            )
            tx.insertScheduledDaycareAclRow(
                employeeId = employee.id,
                daycareIds = listOf(daycare3.id),
                role = UserRole.SPECIAL_EDUCATION_TEACHER,
                startDate = today.plusDays(1),
                endDate = null,
            )
        }

        scheduledJobs.syncAclRows(db, MockEvakaClock(now))

        db.read { tx ->
            assertEquals(
                listOf(UserRole.SPECIAL_EDUCATION_TEACHER),
                tx.getDaycareAclRows(daycare1.id, false, includeStaffEmployeeNumber = false).map {
                    it.role
                },
            )
            assertEquals(
                listOf(UserRole.SPECIAL_EDUCATION_TEACHER),
                tx.getDaycareAclRows(daycare2.id, false, includeStaffEmployeeNumber = false).map {
                    it.role
                },
            )
            assertTrue(
                tx.getDaycareAclRows(daycare3.id, false, includeStaffEmployeeNumber = false)
                    .isEmpty()
            )
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
                        sleepingNote = null,
                    ),
                )
                .let { id ->
                    it.createUpdate {
                            sql(
                                "UPDATE child_daily_note SET modified_at = ${bind(sixteenHoursAgo)} WHERE id = ${bind(id)}"
                            )
                        }
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
                        endDate = LocalDate.now().plusDays(150),
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
                                LocalDate.now().plusDays(100),
                            ),
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
                        sleepingNote = null,
                    ),
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
        status: ApplicationStatus = ApplicationStatus.SENT,
    ): ApplicationId {
        return db.transaction { tx ->
            tx.insertTestApplication(
                status = status,
                confidential =
                    if (
                        status in
                            listOf(
                                ApplicationStatus.CREATED,
                                ApplicationStatus.SENT,
                                ApplicationStatus.WAITING_PLACEMENT,
                            )
                    )
                        null
                    else true,
                childId = childId,
                guardianId = testAdult_1.id,
                transferApplication = true,
                type = type,
                document =
                    DaycareFormV0.fromApplication2(validDaycareApplication)
                        .copy(
                            type = type,
                            preferredStartDate = preferredStartDate,
                            connectedDaycare = preschoolDaycare,
                        ),
            )
        }
    }

    private fun createNormalApplication(
        type: ApplicationType,
        preferredStartDate: LocalDate,
        preparatory: Boolean = false,
        childId: ChildId = testChild_1.id,
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
                            careDetails = form.careDetails.copy(preparatory = preparatory),
                        )
                    },
            )
        }
    }

    private fun createPlacement(
        type: PlacementType,
        dateRange: FiniteDateRange,
        childId: ChildId = testChild_1.id,
    ) {
        db.transaction {
            it.insert(
                DevPlacement(
                    type = type,
                    childId = childId,
                    unitId = testDaycare.id,
                    startDate = dateRange.start,
                    endDate = dateRange.end,
                )
            )
        }
    }

    private fun getApplicationStatus(applicationId: ApplicationId): ApplicationStatus {
        return db.read {
            it.createQuery {
                    sql("SELECT status FROM application WHERE id = ${bind(applicationId)}")
                }
                .exactlyOne<ApplicationStatus>()
        }
    }
}
