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
import fi.espoo.evaka.application.persistence.daycare.Adult
import fi.espoo.evaka.application.persistence.daycare.Apply
import fi.espoo.evaka.application.persistence.daycare.Child
import fi.espoo.evaka.application.persistence.daycare.DaycareFormV0
import fi.espoo.evaka.defaultMunicipalOrganizerOid
import fi.espoo.evaka.note.child.daily.ChildDailyNoteBody
import fi.espoo.evaka.note.child.daily.createChildDailyNote
import fi.espoo.evaka.note.child.daily.getChildDailyNoteForChild
import fi.espoo.evaka.note.child.sticky.ChildStickyNoteBody
import fi.espoo.evaka.note.child.sticky.createChildStickyNote
import fi.espoo.evaka.note.child.sticky.getChildStickyNotesForChild
import fi.espoo.evaka.pis.service.getGuardianChildIds
import fi.espoo.evaka.pis.service.insertGuardian
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.reservations.getReservationsCitizen
import fi.espoo.evaka.serviceneed.ShiftCareType
import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.AttendanceReservationId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.CitizenAuthLevel
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.getDaycareAclRows
import fi.espoo.evaka.shared.auth.insertDaycareAclRow
import fi.espoo.evaka.shared.auth.insertScheduledDaycareAclRow
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.dev.DevBackupCare
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.DevReservation
import fi.espoo.evaka.shared.dev.DevServiceNeed
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.dev.insertTestApplication
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.shared.domain.RealEvakaClock
import fi.espoo.evaka.shared.domain.TimeRange
import fi.espoo.evaka.snDefaultDaycare
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
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class ScheduledJobsTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var scheduledJobs: ScheduledJobs

    @Autowired private lateinit var applicationStateService: ApplicationStateService

    private val area = DevCareArea()
    private val daycare =
        DevDaycare(areaId = area.id, ophOrganizerOid = defaultMunicipalOrganizerOid)
    private val employee = DevEmployee()
    private val serviceWorker =
        AuthenticatedUser.Employee(id = employee.id, roles = setOf(UserRole.SERVICE_WORKER))
    private val adult1 = DevPerson(ssn = "010180-1232")
    private val adult2 = DevPerson()
    private val child1 = DevPerson(dateOfBirth = LocalDate.of(2017, 6, 1), ssn = "010617A123U")
    private val child2 = DevPerson(dateOfBirth = LocalDate.of(2016, 3, 1))

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insert(snDefaultDaycare)
            tx.insert(employee)
            tx.insert(area)
            tx.insert(daycare)
            listOf(adult1, adult2).forEach { tx.insert(it, DevPersonType.ADULT) }
            listOf(child1, child2).forEach { tx.insert(it, DevPersonType.CHILD) }
        }
    }

    @Test
    fun `Draft application and attachments older than 60 days is cleaned up`() {
        val idToBeDeleted = ApplicationId(UUID.randomUUID())
        val idNotToBeDeleted = ApplicationId(UUID.randomUUID())
        val user = AuthenticatedUser.Citizen(adult2.id, CitizenAuthLevel.STRONG)

        db.transaction { tx ->
            tx.insertTestApplication(
                id = idToBeDeleted,
                status = ApplicationStatus.CREATED,
                guardianId = adult2.id,
                childId = child1.id,
                type = ApplicationType.DAYCARE,
                document =
                    DaycareFormV0(
                        type = ApplicationType.DAYCARE,
                        child = Child(dateOfBirth = null),
                        guardian = Adult(),
                        apply = Apply(preferredUnits = listOf(daycare.id)),
                    ),
            )
            setApplicationCreatedDate(tx, idToBeDeleted, LocalDate.now().minusDays(61))

            tx.insertTestApplication(
                id = idNotToBeDeleted,
                status = ApplicationStatus.CREATED,
                guardianId = adult2.id,
                childId = child1.id,
                type = ApplicationType.DAYCARE,
                document =
                    DaycareFormV0(
                        type = ApplicationType.DAYCARE,
                        child = Child(dateOfBirth = null),
                        guardian = Adult(),
                        apply = Apply(preferredUnits = listOf(daycare.id)),
                    ),
            )
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
        MockPersonDetailsService.addPersons(adult1, child1)
        MockPersonDetailsService.addDependants(adult1, child1)
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
                    daycare.id,
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
                childId = child1.id,
                note = ChildStickyNoteBody(note = "", expires = LocalDate.now().minusDays(1)),
            )
        }

        val validNoteId =
            db.transaction { tx ->
                tx.createChildStickyNote(
                    childId = child1.id,
                    note = ChildStickyNoteBody(note = "", expires = LocalDate.now()),
                )
            }

        db.read {
            val notesBeforeCleanup = it.getChildStickyNotesForChild(child1.id)
            assertEquals(2, notesBeforeCleanup.size)
        }
        scheduledJobs.removeExpiredNotes(db, RealEvakaClock())

        db.read {
            val notesAfterCleanup = it.getChildStickyNotesForChild(child1.id)
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
                        childId = child2.id,
                        unitId = daycare.id,
                        startDate = LocalDate.now().minusDays(100),
                        endDate = LocalDate.now().plusDays(100),
                    )
                )
                it.createChildDailyNote(
                    child2.id,
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
            val note1AfterCleanup = it.getChildDailyNoteForChild(child1.id)
            assertNull(note1AfterCleanup)
            val note2AfterCleanup = it.getChildDailyNoteForChild(child2.id)
            assertNotNull(note2AfterCleanup)
            assertEquals(validNoteId, note2AfterCleanup.id)
        }
    }

    @Test
    fun `syncAclRows removes rows where end date has passed`() {
        val now = HelsinkiDateTime.of(LocalDate.of(2024, 5, 1), LocalTime.of(0, 5))
        val today = now.toLocalDate()

        val daycare1 = DevDaycare(areaId = area.id)
        val daycare2 = DevDaycare(areaId = area.id)
        val daycare3 = DevDaycare(areaId = area.id)
        val daycare4 = DevDaycare(areaId = area.id)
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

        val daycare1 = DevDaycare(areaId = area.id)
        val daycare2 = DevDaycare(areaId = area.id)
        val daycare3 = DevDaycare(areaId = area.id)
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
                    child1.id,
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
                        childId = child2.id,
                        unitId = daycare.id,
                        startDate = LocalDate.now().minusDays(150),
                        endDate = LocalDate.now().plusDays(150),
                    )
                )
                it.insert(
                    DevBackupCare(
                        childId = child2.id,
                        groupId = null,
                        unitId = daycare.id,
                        period =
                            FiniteDateRange(
                                LocalDate.now().minusDays(100),
                                LocalDate.now().plusDays(100),
                            ),
                    )
                )
                it.createChildDailyNote(
                    child2.id,
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
            val note1AfterCleanup = it.getChildDailyNoteForChild(child1.id)
            assertNull(note1AfterCleanup)
            val note2AfterCleanup = it.getChildDailyNoteForChild(child2.id)
            assertNotNull(note2AfterCleanup)
            assertEquals(validNoteId, note2AfterCleanup.id)
        }
    }

    @Test
    fun removeInvalidatedShiftCareReservations() {
        val mockClock = MockEvakaClock(2024, 12, 2, 0, 15)
        val monday = mockClock.today()

        val standardDay = TimeRange(start = LocalTime.of(8, 0), LocalTime.of(17, 0))
        val shiftCareDay = TimeRange(start = LocalTime.MIN, LocalTime.MAX)

        val unit =
            DevDaycare(
                areaId = area.id,
                operationTimes = List(5) { standardDay } + List(2) { null },
                shiftCareOperationTimes = List(7) { shiftCareDay },
                shiftCareOpenOnHolidays = true,
            )
        val placement1 =
            DevPlacement(
                childId = child1.id,
                unitId = unit.id,
                startDate = monday.minusDays(7),
                endDate = monday.plusDays(7),
            )

        val placement2 =
            DevPlacement(
                childId = child2.id,
                unitId = unit.id,
                startDate = monday.minusDays(7),
                endDate = monday.plusDays(7),
            )

        val shiftCareReservation =
            DevReservation(
                childId = child2.id,
                date = monday.plusDays(2),
                startTime = shiftCareDay.start.inner,
                endTime = shiftCareDay.end.inner,
                createdBy = serviceWorker.evakaUserId,
            )

        db.transaction { tx ->
            tx.insertGuardian(adult1.id, child1.id)
            tx.insertGuardian(adult1.id, child2.id)
            tx.insert(unit)
            tx.insert(placement1)
            tx.insert(placement2)

            // child 1 has full shift care
            tx.insert(
                DevServiceNeed(
                    placementId = placement1.id,
                    startDate = placement1.startDate,
                    endDate = placement1.endDate,
                    shiftCare = ShiftCareType.FULL,
                    optionId = snDefaultDaycare.id,
                    confirmedBy = serviceWorker.evakaUserId,
                )
            )

            // child 2 has no shift care and only mon-sat service need coverage
            tx.insert(
                DevServiceNeed(
                    placementId = placement2.id,
                    startDate = monday,
                    endDate = monday.plusDays(5),
                    shiftCare = ShiftCareType.NONE,
                    optionId = snDefaultDaycare.id,
                    confirmedBy = serviceWorker.evakaUserId,
                )
            )

            // add same reservations for both children
            listOf(child1, child2).forEach {
                // wednesday a shift care reservation
                tx.insert(
                    shiftCareReservation.copy(
                        id = AttendanceReservationId(UUID.randomUUID()),
                        childId = it.id,
                    )
                )

                // thursday an empty reservation
                tx.insert(
                    shiftCareReservation.copy(
                        id = AttendanceReservationId(UUID.randomUUID()),
                        date = monday.plusDays(3),
                        startTime = null,
                        endTime = null,
                        childId = it.id,
                    )
                )

                // friday is a holiday, shift care reservation
                tx.insert(
                    shiftCareReservation.copy(
                        id = AttendanceReservationId(UUID.randomUUID()),
                        date = monday.plusDays(4),
                        childId = it.id,
                    )
                )

                // saturday not a standard operation day, shift care reservation
                tx.insert(
                    shiftCareReservation.copy(
                        id = AttendanceReservationId(UUID.randomUUID()),
                        date = monday.plusDays(5),
                        childId = it.id,
                    )
                )

                // sunday not a standard operation day, shift care reservation
                tx.insert(
                    shiftCareReservation.copy(
                        id = AttendanceReservationId(UUID.randomUUID()),
                        date = monday.plusDays(6),
                        childId = it.id,
                    )
                )
            }
        }

        val reservationsBefore =
            db.read {
                    it.getReservationsCitizen(
                        guardianId = adult1.id,
                        today = mockClock.today(),
                        range = FiniteDateRange(monday, monday.plusWeeks(1)),
                    )
                }
                .map { Triple(it.childId, it.date, it.reservation.asTimeRange()) }

        assertThat(reservationsBefore)
            .containsExactlyInAnyOrderElementsOf(
                listOf(child1, child2)
                    .map {
                        listOf(
                            Triple(it.id, monday.plusDays(2), shiftCareDay),
                            Triple(it.id, monday.plusDays(3), null),
                            Triple(it.id, monday.plusDays(4), shiftCareDay),
                            Triple(it.id, monday.plusDays(5), shiftCareDay),
                            Triple(it.id, monday.plusDays(6), shiftCareDay),
                        )
                    }
                    .flatten()
            )

        scheduledJobs.removeInvalidatedShiftCareReservations(db, mockClock)

        val reservationsAfter =
            db.read {
                    it.getReservationsCitizen(
                        guardianId = adult1.id,
                        today = mockClock.today(),
                        range = FiniteDateRange(monday, monday.plusWeeks(1)),
                    )
                }
                .map { Triple(it.childId, it.date, it.reservation.asTimeRange()) }

        // assert that only child 2's non-normal operating days have been removed
        assertThat(reservationsAfter)
            .containsExactlyInAnyOrderElementsOf(
                listOf(
                    Triple(child1.id, monday.plusDays(2), shiftCareDay),
                    Triple(child1.id, monday.plusDays(3), null),
                    Triple(child1.id, monday.plusDays(4), shiftCareDay),
                    Triple(child1.id, monday.plusDays(5), shiftCareDay),
                    Triple(child1.id, monday.plusDays(6), shiftCareDay),
                    Triple(child2.id, monday.plusDays(2), shiftCareDay),
                    Triple(child2.id, monday.plusDays(3), null),
                )
            )
    }

    private fun createTransferApplication(
        type: ApplicationType,
        preferredStartDate: LocalDate,
        preschoolDaycare: Boolean = false,
        childId: ChildId = child1.id,
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
                guardianId = adult1.id,
                transferApplication = true,
                type = type,
                document =
                    DaycareFormV0(
                        type = type,
                        child = Child(dateOfBirth = null),
                        guardian = Adult(),
                        apply = Apply(preferredUnits = listOf(daycare.id)),
                        preferredStartDate = preferredStartDate,
                        connectedDaycare =
                            if (type == ApplicationType.PRESCHOOL) preschoolDaycare else null,
                    ),
            )
        }
    }

    private fun createNormalApplication(
        type: ApplicationType,
        preferredStartDate: LocalDate,
        childId: ChildId = child1.id,
    ): ApplicationId {
        return db.transaction { tx ->
            tx.insertTestApplication(
                status = ApplicationStatus.SENT,
                childId = childId,
                guardianId = adult1.id,
                type = type,
                document =
                    DaycareFormV0(
                        type = type,
                        child = Child(dateOfBirth = null),
                        guardian = Adult(),
                        apply = Apply(preferredUnits = listOf(daycare.id)),
                        preferredStartDate = preferredStartDate,
                    ),
            )
        }
    }

    private fun createPlacement(
        type: PlacementType,
        dateRange: FiniteDateRange,
        childId: ChildId = child1.id,
    ) {
        db.transaction {
            it.insert(
                DevPlacement(
                    type = type,
                    childId = childId,
                    unitId = daycare.id,
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
