// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.calendarevent

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.backupcare.BackupCareController
import fi.espoo.evaka.backupcare.BackupCareUpdateRequest
import fi.espoo.evaka.backupcare.NewBackupCare
import fi.espoo.evaka.backupcare.getBackupCaresForChild
import fi.espoo.evaka.emailclient.MockEmailClient
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.pis.PersonalDataUpdate
import fi.espoo.evaka.pis.service.insertGuardian
import fi.espoo.evaka.pis.updatePersonalDetails
import fi.espoo.evaka.placement.GroupTransferRequestBody
import fi.espoo.evaka.placement.PlacementController
import fi.espoo.evaka.placement.PlacementControllerCitizen
import fi.espoo.evaka.placement.PlacementUpdateRequestBody
import fi.espoo.evaka.placement.TerminatablePlacementType
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.GroupPlacementId
import fi.espoo.evaka.shared.PlacementId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.CitizenAuthLevel
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.dev.insertTestDaycareGroupPlacement
import fi.espoo.evaka.shared.dev.insertTestPlacement
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testAdult_3
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testChild_2
import fi.espoo.evaka.testChild_3
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDaycare2
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired

class CalendarEventServiceIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired lateinit var calendarEventController: CalendarEventController
    @Autowired lateinit var placementController: PlacementController
    @Autowired lateinit var backupCareController: BackupCareController
    @Autowired lateinit var placementControllerCitizen: PlacementControllerCitizen
    @Autowired lateinit var calendarEventNotificationService: CalendarEventNotificationService

    private final val adminId = EmployeeId(UUID.randomUUID())
    private val admin = AuthenticatedUser.Employee(adminId, setOf(UserRole.ADMIN))

    private val guardian = AuthenticatedUser.Citizen(testAdult_1.id, CitizenAuthLevel.STRONG)

    // a monday
    private val today = LocalDate.of(2021, 6, 5)
    private val now = HelsinkiDateTime.of(today, LocalTime.of(12, 0, 0))
    private val clock = MockEvakaClock(now)

    private final val groupId = GroupId(UUID.randomUUID())
    private final val groupId2 = GroupId(UUID.randomUUID())
    private final val unit2GroupId = GroupId(UUID.randomUUID())

    private lateinit var placementId: PlacementId
    private lateinit var groupPlacementId: GroupPlacementId

    private final val placementStart = today.minusDays(100)
    private final val placementEnd = today.plusDays(100)

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insertGeneralTestFixtures()
            tx.insertGuardian(testAdult_1.id, testChild_1.id)
            tx.insert(DevEmployee(adminId, roles = setOf(UserRole.ADMIN)))
            tx.insert(
                DevDaycareGroup(id = groupId, daycareId = testDaycare.id, name = "TestGroup1")
            )
            tx.insert(DevDaycareGroup(id = groupId2, daycareId = testDaycare.id))
            tx.insert(DevDaycareGroup(id = unit2GroupId, daycareId = testDaycare2.id))
            placementId =
                tx.insertTestPlacement(
                    childId = testChild_1.id,
                    unitId = testDaycare.id,
                    startDate = placementStart,
                    endDate = placementEnd
                )
            groupPlacementId =
                tx.insertTestDaycareGroupPlacement(
                    daycarePlacementId = placementId,
                    groupId = groupId,
                    startDate = placementStart,
                    endDate = placementEnd
                )
            tx.insertTestDaycareGroupPlacement(
                daycarePlacementId =
                    tx.insertTestPlacement(
                        childId = testChild_2.id,
                        unitId = testDaycare.id,
                        startDate = placementStart,
                        endDate = placementEnd
                    ),
                groupId = groupId,
                startDate = placementStart,
                endDate = placementEnd
            )
        }
    }

    @Test
    fun `employee can create a calendar event for a unit`() {
        val form =
            CalendarEventForm(
                unitId = testDaycare.id,
                tree = null,
                title = "Unit-wide event",
                description = "uwe",
                period = FiniteDateRange(today.plusDays(3), today.plusDays(3))
            )
        this.calendarEventController.createCalendarEvent(dbInstance(), admin, clock, form)

        val event =
            this.calendarEventController
                .getUnitCalendarEvents(
                    dbInstance(),
                    admin,
                    clock,
                    testDaycare.id,
                    today,
                    today.plusDays(4)
                )[0]

        assertEquals(form.period, event.period)
        assertEquals(form.title, event.title)
        assertEquals(form.description, event.description)
        assertEquals(form.unitId, event.unitId)
        assert(event.groups.isEmpty())
        assert(event.individualChildren.isEmpty())
    }

    @Test
    fun `employee can create a calendar event for a group`() {
        val form =
            CalendarEventForm(
                unitId = testDaycare.id,
                tree = mapOf(groupId to null),
                title = "Group-wide event",
                description = "gwe",
                period = FiniteDateRange(today.plusDays(3), today.plusDays(4))
            )
        this.calendarEventController.createCalendarEvent(dbInstance(), admin, clock, form)

        val event =
            this.calendarEventController
                .getUnitCalendarEvents(
                    dbInstance(),
                    admin,
                    clock,
                    testDaycare.id,
                    today,
                    today.plusDays(5)
                )[0]

        assertEquals(form.period, event.period)
        assertEquals(form.title, event.title)
        assertEquals(form.description, event.description)
        assertEquals(form.unitId, event.unitId)
        assertEquals(groupId, event.groups.first().id)
        assert(event.individualChildren.isEmpty())
    }

    @Test
    fun `employee can create a calendar event for a child`() {
        val form =
            CalendarEventForm(
                unitId = testDaycare.id,
                tree = mapOf(groupId to setOf(testChild_1.id)),
                title = "Child-specific event",
                description = "cse",
                period = FiniteDateRange(today.plusDays(3), today.plusDays(4))
            )
        this.calendarEventController.createCalendarEvent(dbInstance(), admin, clock, form)

        val event =
            this.calendarEventController
                .getUnitCalendarEvents(
                    dbInstance(),
                    admin,
                    clock,
                    testDaycare.id,
                    today,
                    today.plusDays(5)
                )[0]

        assertEquals(form.period, event.period)
        assertEquals(form.title, event.title)
        assertEquals(form.description, event.description)
        assertEquals(form.unitId, event.unitId)
        assertEquals(groupId, event.groups.first().id)
        assertEquals(testChild_1.id, event.individualChildren.first().id)
    }

    @Test
    fun `employee can edit a calendar event's title and description`() {
        val form =
            CalendarEventForm(
                unitId = testDaycare.id,
                tree = mapOf(groupId to setOf(testChild_1.id)),
                title = "Child-specific event",
                description = "cse",
                period = FiniteDateRange(today.plusDays(3), today.plusDays(4))
            )
        this.calendarEventController.createCalendarEvent(dbInstance(), admin, clock, form)

        this.calendarEventController.modifyCalendarEvent(
            dbInstance(),
            admin,
            clock,
            this.calendarEventController
                .getUnitCalendarEvents(
                    dbInstance(),
                    admin,
                    clock,
                    testDaycare.id,
                    today,
                    today.plusDays(4)
                )[0]
                .id,
            CalendarEventUpdateForm(title = "Updated title", description = "desc, updated")
        )

        val event =
            this.calendarEventController
                .getUnitCalendarEvents(
                    dbInstance(),
                    admin,
                    clock,
                    testDaycare.id,
                    today,
                    today.plusDays(4)
                )[0]

        assertEquals("Updated title", event.title)
        assertEquals("desc, updated", event.description)
    }

    @Test
    fun `employee can delete a calendar event`() {
        val form =
            CalendarEventForm(
                unitId = testDaycare.id,
                tree = mapOf(groupId to setOf(testChild_1.id)),
                title = "Child-specific event",
                description = "cse",
                period = FiniteDateRange(today.plusDays(3), today.plusDays(4))
            )
        this.calendarEventController.createCalendarEvent(dbInstance(), admin, clock, form)

        this.calendarEventController.deleteCalendarEvent(
            dbInstance(),
            admin,
            clock,
            this.calendarEventController
                .getUnitCalendarEvents(
                    dbInstance(),
                    admin,
                    clock,
                    testDaycare.id,
                    today,
                    today.plusDays(4)
                )[0]
                .id
        )

        assert(
            this.calendarEventController
                .getUnitCalendarEvents(
                    dbInstance(),
                    admin,
                    clock,
                    testDaycare.id,
                    today,
                    today.plusDays(4)
                )
                .isEmpty()
        )
    }

    @Test
    fun `citizen sees basic calendar events`() {
        val form =
            CalendarEventForm(
                unitId = testDaycare.id,
                tree = mapOf(groupId to setOf(testChild_1.id)),
                title = "Child-specific event",
                description = "cse",
                period = FiniteDateRange(today.plusDays(3), today.plusDays(4))
            )
        this.calendarEventController.createCalendarEvent(dbInstance(), admin, clock, form)

        val form2 =
            CalendarEventForm(
                unitId = testDaycare.id,
                tree = null,
                title = "Unit-wide event",
                description = "uw",
                period = FiniteDateRange(today.plusDays(1), today.plusDays(1))
            )
        this.calendarEventController.createCalendarEvent(dbInstance(), admin, clock, form2)

        val form3 =
            CalendarEventForm(
                unitId = testDaycare.id,
                tree = mapOf(groupId to null),
                title = "Group event",
                description = "gw",
                period = FiniteDateRange(today.plusDays(1), today.plusDays(1))
            )
        this.calendarEventController.createCalendarEvent(dbInstance(), admin, clock, form3)

        val guardianEvents =
            this.calendarEventController.getCitizenCalendarEvents(
                dbInstance(),
                guardian,
                clock,
                today,
                today.plusDays(10)
            )

        val individualEvent = guardianEvents.find { it.title == "Child-specific event" }
        assertNotNull(individualEvent)
        assertEquals(form.title, individualEvent.title)
        assertEquals(form.description, individualEvent.description)
        assertEquals(1, individualEvent.attendingChildren.size)

        individualEvent.attendingChildren[testChild_1.id]?.first().let { attendee ->
            assertNotNull(attendee)
            assertEquals("TestGroup1", attendee.groupName)
            assertEquals("individual", attendee.type)
            assertEquals(
                listOf(FiniteDateRange(today.plusDays(3), today.plusDays(4))),
                attendee.periods
            )
        }

        val unitEvent = guardianEvents.find { it.title == "Unit-wide event" }
        assertNotNull(unitEvent)
        assertEquals(form2.title, unitEvent.title)
        assertEquals(form2.description, unitEvent.description)
        assertEquals(1, unitEvent.attendingChildren.size)

        unitEvent.attendingChildren[testChild_1.id]?.first().let { attendee ->
            assertNotNull(attendee)
            assertNull(attendee.groupName)
            assertEquals(testDaycare.name, attendee.unitName)
            assertEquals("unit", attendee.type)
            assertEquals(
                listOf(FiniteDateRange(today.plusDays(1), today.plusDays(1))),
                attendee.periods
            )
        }

        val groupEvent = guardianEvents.find { it.title == "Group event" }
        assertNotNull(groupEvent)
        assertEquals(form3.title, groupEvent.title)
        assertEquals(form3.description, groupEvent.description)
        assertEquals(1, groupEvent.attendingChildren.size)

        groupEvent.attendingChildren[testChild_1.id]?.first().let { attendee ->
            assertNotNull(attendee)
            assertEquals("TestGroup1", attendee.groupName)
            assertEquals("group", attendee.type)
            assertEquals(
                listOf(FiniteDateRange(today.plusDays(1), today.plusDays(1))),
                attendee.periods
            )
        }
    }

    @Test
    fun `citizen with multiple children sees children as attendees`() {
        db.transaction { tx -> tx.insertGuardian(testAdult_1.id, testChild_2.id) }

        val form =
            CalendarEventForm(
                unitId = testDaycare.id,
                tree = mapOf(groupId to null),
                title = "Group event",
                description = "ge",
                period = FiniteDateRange(today.plusDays(3), today.plusDays(4))
            )
        this.calendarEventController.createCalendarEvent(dbInstance(), admin, clock, form)

        val event =
            this.calendarEventController
                .getCitizenCalendarEvents(dbInstance(), guardian, clock, today, today.plusDays(10))
                .first()

        assertEquals(form.title, event.title)
        assertEquals(form.description, event.description)
        assertEquals(2, event.attendingChildren.size)

        event.attendingChildren[testChild_1.id]?.first().let { attendee ->
            assertNotNull(attendee)
            assertEquals("TestGroup1", attendee.groupName)
            assertEquals("group", attendee.type)
            assertEquals(
                listOf(FiniteDateRange(today.plusDays(3), today.plusDays(4))),
                attendee.periods
            )
        }

        event.attendingChildren[testChild_2.id]?.first().let { attendee ->
            assertNotNull(attendee)
            assertEquals("TestGroup1", attendee.groupName)
            assertEquals("group", attendee.type)
            assertEquals(
                listOf(FiniteDateRange(today.plusDays(3), today.plusDays(4))),
                attendee.periods
            )
        }
    }

    @Test
    fun `citizen does not see other child's events`() {
        val form =
            CalendarEventForm(
                unitId = testDaycare.id,
                tree = mapOf(groupId to setOf(testChild_2.id)),
                title = "Individual child event (other child)",
                description = "ie",
                period = FiniteDateRange(today.minusDays(1), today.plusDays(2))
            )
        this.calendarEventController.createCalendarEvent(dbInstance(), admin, clock, form)

        assert(
            this.calendarEventController
                .getCitizenCalendarEvents(
                    dbInstance(),
                    guardian,
                    clock,
                    today.minusDays(10),
                    today.plusDays(10)
                )
                .isEmpty()
        )
    }

    @Test
    fun `citizen does not see other group's events`() {
        val form =
            CalendarEventForm(
                unitId = testDaycare.id,
                tree = mapOf(groupId2 to null),
                title = "Group event",
                description = "ge",
                period = FiniteDateRange(today.minusDays(1), today.plusDays(2))
            )
        this.calendarEventController.createCalendarEvent(dbInstance(), admin, clock, form)

        assert(
            this.calendarEventController
                .getCitizenCalendarEvents(
                    dbInstance(),
                    guardian,
                    clock,
                    today.minusDays(10),
                    today.plusDays(10)
                )
                .isEmpty()
        )
    }

    @Test
    fun `citizen does not see other unit's events`() {
        val form =
            CalendarEventForm(
                unitId = testDaycare2.id,
                tree = null,
                title = "Unit-wide event in another unit",
                description = "uw",
                period = FiniteDateRange(today.minusDays(1), today.plusDays(2))
            )
        this.calendarEventController.createCalendarEvent(dbInstance(), admin, clock, form)

        assert(
            this.calendarEventController
                .getCitizenCalendarEvents(
                    dbInstance(),
                    guardian,
                    clock,
                    today.minusDays(10),
                    today.plusDays(10)
                )
                .isEmpty()
        )
    }

    @Test
    fun `citizen only sees unit and group events during active placement`() {
        val form =
            CalendarEventForm(
                unitId = testDaycare.id,
                tree = mapOf(groupId to null),
                title = "Group event",
                description = "ge",
                period = FiniteDateRange(placementStart.minusDays(10), placementStart.minusDays(8))
            )
        this.calendarEventController.createCalendarEvent(dbInstance(), admin, clock, form)

        val form2 =
            CalendarEventForm(
                unitId = testDaycare.id,
                tree = null,
                title = "Unit-wide event",
                description = "uw",
                period = FiniteDateRange(placementStart.minusDays(20), placementStart.minusDays(20))
            )
        this.calendarEventController.createCalendarEvent(dbInstance(), admin, clock, form2)

        assert(
            this.calendarEventController
                .getCitizenCalendarEvents(
                    dbInstance(),
                    guardian,
                    clock,
                    placementStart.minusDays(50),
                    placementStart.plusDays(50)
                )
                .isEmpty()
        )
    }

    @Test
    fun `citizen sees backup care and main unit events`() {
        val backupCareStart = placementStart.plusDays(10)
        val backupCareEnd = placementStart.plusDays(20)
        db.transaction { tx ->
            tx.insertGuardian(testAdult_3.id, testChild_3.id)
            val placementId =
                tx.insertTestPlacement(
                    childId = testChild_3.id,
                    unitId = testDaycare.id,
                    startDate = placementStart,
                    endDate = placementEnd
                )
            tx.insertTestDaycareGroupPlacement(
                daycarePlacementId = placementId,
                groupId = groupId,
                startDate = placementStart,
                endDate = backupCareStart.minusDays(1)
            )
            tx.insertTestDaycareGroupPlacement(
                daycarePlacementId = placementId,
                groupId = groupId,
                startDate = backupCareEnd.plusDays(1),
                endDate = placementEnd
            )
        }
        this.backupCareController.createForChild(
            dbInstance(),
            admin,
            clock,
            testChild_3.id,
            NewBackupCare(testDaycare2.id, null, FiniteDateRange(backupCareStart, backupCareEnd))
        )

        val form =
            CalendarEventForm(
                unitId = testDaycare.id,
                tree = mapOf(groupId to null),
                title = "Group event (unit 1)",
                description = "u1_g",
                period = FiniteDateRange(placementStart, placementStart.plusDays(30))
            )
        this.calendarEventController.createCalendarEvent(dbInstance(), admin, clock, form)

        val form2 =
            CalendarEventForm(
                unitId = testDaycare2.id,
                tree = null,
                title = "Unit event (unit 2)",
                description = "u1_u",
                period = FiniteDateRange(placementStart, placementStart.plusDays(30))
            )
        this.calendarEventController.createCalendarEvent(dbInstance(), admin, clock, form2)

        val guardianEvents =
            this.calendarEventController.getCitizenCalendarEvents(
                dbInstance(),
                AuthenticatedUser.Citizen(testAdult_3.id, CitizenAuthLevel.STRONG),
                clock,
                placementStart,
                placementStart.plusDays(50)
            )

        val mainUnitEvent = guardianEvents.find { it.title == "Group event (unit 1)" }
        assertNotNull(mainUnitEvent)
        assertEquals(form.title, mainUnitEvent.title)
        assertEquals(form.description, mainUnitEvent.description)
        assertEquals(1, mainUnitEvent.attendingChildren.size)

        mainUnitEvent.attendingChildren[testChild_3.id]?.first().let { attendee ->
            assertNotNull(attendee)
            assertEquals("TestGroup1", attendee.groupName)
            assertEquals("group", attendee.type)
            assertEquals(
                listOf(
                    FiniteDateRange(placementStart, placementStart.plusDays(9)),
                    FiniteDateRange(placementStart.plusDays(21), placementStart.plusDays(30))
                ),
                attendee.periods.sortedBy { it.start }
            )
        }

        val backupCareEvent = guardianEvents.find { it.title == "Unit event (unit 2)" }
        assertNotNull(backupCareEvent)
        assertEquals(form2.title, backupCareEvent.title)
        assertEquals(form2.description, backupCareEvent.description)
        assertEquals(1, backupCareEvent.attendingChildren.size)

        backupCareEvent.attendingChildren[testChild_3.id]?.first().let { attendee ->
            assertNotNull(attendee)
            assertNull(attendee.groupName)
            assertEquals(testDaycare2.name, attendee.unitName)
            assertEquals("unit", attendee.type)
            assertEquals(
                listOf(FiniteDateRange(placementStart.plusDays(10), placementStart.plusDays(20))),
                attendee.periods
            )
        }
    }

    @Test
    fun `removing placement removes child-specific event attendance`() {
        val form =
            CalendarEventForm(
                unitId = testDaycare.id,
                tree = mapOf(groupId to setOf(testChild_1.id)),
                title = "Child-specific event",
                description = "cse",
                period = FiniteDateRange(today.plusDays(3), today.plusDays(4))
            )
        this.calendarEventController.createCalendarEvent(dbInstance(), admin, clock, form)
        this.placementController.deletePlacement(dbInstance(), admin, clock, placementId)
        assert(
            this.calendarEventController
                .getUnitCalendarEvents(
                    dbInstance(),
                    admin,
                    clock,
                    testDaycare.id,
                    today,
                    today.plusDays(5)
                )
                .isEmpty()
        )
        assertEquals(
            0,
            db.transaction { tx -> tx.devCalendarEventUnitAttendeeCount(testDaycare.id) }
        )
    }

    @Test
    fun `shrinking start date of placement removes child-specific event attendance`() {
        val form =
            CalendarEventForm(
                unitId = testDaycare.id,
                tree = mapOf(groupId to setOf(testChild_1.id)),
                title = "Child-specific event",
                description = "cse",
                period = FiniteDateRange(today.plusDays(3), today.plusDays(4))
            )
        this.calendarEventController.createCalendarEvent(dbInstance(), admin, clock, form)
        this.placementController.updatePlacementById(
            dbInstance(),
            admin,
            clock,
            placementId,
            PlacementUpdateRequestBody(startDate = today.plusDays(10), endDate = placementEnd)
        )
        assert(
            this.calendarEventController
                .getUnitCalendarEvents(
                    dbInstance(),
                    admin,
                    clock,
                    testDaycare.id,
                    today,
                    today.plusDays(5)
                )
                .isEmpty()
        )
        assertEquals(
            0,
            db.transaction { tx -> tx.devCalendarEventUnitAttendeeCount(testDaycare.id) }
        )
    }

    @Test
    fun `shrinking end date of placement removes child-specific event attendance`() {
        val form =
            CalendarEventForm(
                unitId = testDaycare.id,
                tree = mapOf(groupId to setOf(testChild_1.id)),
                title = "Child-specific event",
                description = "cse",
                period = FiniteDateRange(today.plusDays(3), today.plusDays(4))
            )
        this.calendarEventController.createCalendarEvent(dbInstance(), admin, clock, form)
        this.placementController.updatePlacementById(
            dbInstance(),
            admin,
            clock,
            placementId,
            PlacementUpdateRequestBody(startDate = placementStart, endDate = today.minusDays(1))
        )
        assert(
            this.calendarEventController
                .getUnitCalendarEvents(
                    dbInstance(),
                    admin,
                    clock,
                    testDaycare.id,
                    today,
                    today.plusDays(5)
                )
                .isEmpty()
        )
        assertEquals(
            0,
            db.transaction { tx -> tx.devCalendarEventUnitAttendeeCount(testDaycare.id) }
        )
    }

    @Test
    fun `removing group placement removes child-specific event attendance`() {
        val form =
            CalendarEventForm(
                unitId = testDaycare.id,
                tree = mapOf(groupId to setOf(testChild_1.id)),
                title = "Child-specific event",
                description = "cse",
                period = FiniteDateRange(today.plusDays(3), today.plusDays(4))
            )
        this.calendarEventController.createCalendarEvent(dbInstance(), admin, clock, form)
        this.placementController.deleteGroupPlacement(dbInstance(), admin, clock, groupPlacementId)
        assert(
            this.calendarEventController
                .getUnitCalendarEvents(
                    dbInstance(),
                    admin,
                    clock,
                    testDaycare.id,
                    today,
                    today.plusDays(5)
                )
                .isEmpty()
        )
        assertEquals(
            0,
            db.transaction { tx -> tx.devCalendarEventUnitAttendeeCount(testDaycare.id) }
        )
    }

    @Test
    fun `transferring group placement removes child-specific event attendance`() {
        val form =
            CalendarEventForm(
                unitId = testDaycare.id,
                tree = mapOf(groupId to setOf(testChild_1.id)),
                title = "Child-specific event",
                description = "cse",
                period = FiniteDateRange(today.plusDays(3), today.plusDays(4))
            )
        this.calendarEventController.createCalendarEvent(dbInstance(), admin, clock, form)
        this.placementController.transferGroupPlacement(
            dbInstance(),
            admin,
            clock,
            groupPlacementId,
            GroupTransferRequestBody(groupId = groupId2, startDate = today)
        )
        assert(
            this.calendarEventController
                .getUnitCalendarEvents(
                    dbInstance(),
                    admin,
                    clock,
                    testDaycare.id,
                    today,
                    today.plusDays(5)
                )
                .isEmpty()
        )
        assertEquals(
            0,
            db.transaction { tx -> tx.devCalendarEventUnitAttendeeCount(testDaycare.id) }
        )
    }

    @Test
    fun `transferring group placement does not remove child-specific event attendance when transferred after`() {
        val form =
            CalendarEventForm(
                unitId = testDaycare.id,
                tree = mapOf(groupId to setOf(testChild_1.id)),
                title = "Child-specific event",
                description = "cse",
                period = FiniteDateRange(today.plusDays(3), today.plusDays(4))
            )
        this.calendarEventController.createCalendarEvent(dbInstance(), admin, clock, form)
        this.placementController.transferGroupPlacement(
            dbInstance(),
            admin,
            clock,
            groupPlacementId,
            GroupTransferRequestBody(groupId = groupId2, startDate = today.plusDays(5))
        )
        assert(
            this.calendarEventController
                .getUnitCalendarEvents(
                    dbInstance(),
                    admin,
                    clock,
                    testDaycare.id,
                    today,
                    today.plusDays(5)
                )
                .isNotEmpty()
        )
    }

    @Test
    fun `creating backup care removes child-specific event attendance from main unit`() {
        val form =
            CalendarEventForm(
                unitId = testDaycare.id,
                tree = mapOf(groupId to setOf(testChild_1.id)),
                title = "Child-specific event",
                description = "cse",
                period = FiniteDateRange(today.plusDays(3), today.plusDays(4))
            )
        this.calendarEventController.createCalendarEvent(dbInstance(), admin, clock, form)
        this.backupCareController.createForChild(
            dbInstance(),
            admin,
            clock,
            testChild_1.id,
            NewBackupCare(
                unitId = testDaycare2.id,
                groupId = null,
                period = FiniteDateRange(today, today.plusDays(10))
            )
        )
        assert(
            this.calendarEventController
                .getUnitCalendarEvents(
                    dbInstance(),
                    admin,
                    clock,
                    testDaycare.id,
                    today,
                    today.plusDays(5)
                )
                .isEmpty()
        )
        assertEquals(
            0,
            db.transaction { tx -> tx.devCalendarEventUnitAttendeeCount(testDaycare.id) }
        )
    }

    @Test
    fun `creating backup care outside event period does not remove child-specific event attendance from main unit`() {
        val form =
            CalendarEventForm(
                unitId = testDaycare.id,
                tree = mapOf(groupId to setOf(testChild_1.id)),
                title = "Child-specific event",
                description = "cse",
                period = FiniteDateRange(today.plusDays(3), today.plusDays(4))
            )
        this.calendarEventController.createCalendarEvent(dbInstance(), admin, clock, form)
        this.backupCareController.createForChild(
            dbInstance(),
            admin,
            clock,
            testChild_1.id,
            NewBackupCare(
                unitId = testDaycare2.id,
                groupId = null,
                period = FiniteDateRange(today.minusDays(10), today)
            )
        )
        assert(
            this.calendarEventController
                .getUnitCalendarEvents(
                    dbInstance(),
                    admin,
                    clock,
                    testDaycare.id,
                    today,
                    today.plusDays(5)
                )
                .isNotEmpty()
        )
    }

    @Test
    fun `extending backup care start date removes child-specific event attendance from main unit`() {
        val form =
            CalendarEventForm(
                unitId = testDaycare.id,
                tree = mapOf(groupId to setOf(testChild_1.id)),
                title = "Child-specific event",
                description = "cse",
                period = FiniteDateRange(today.plusDays(3), today.plusDays(4))
            )
        this.calendarEventController.createCalendarEvent(dbInstance(), admin, clock, form)
        this.backupCareController.createForChild(
            dbInstance(),
            admin,
            clock,
            testChild_1.id,
            NewBackupCare(
                unitId = testDaycare2.id,
                groupId = null,
                period = FiniteDateRange(today.plusDays(10), today.plusDays(20))
            )
        )
        this.backupCareController.update(
            dbInstance(),
            admin,
            clock,
            db.transaction { tx -> tx.getBackupCaresForChild(testChild_1.id).first().id },
            BackupCareUpdateRequest(FiniteDateRange(today, today.plusDays(20)), null)
        )
        assert(
            this.calendarEventController
                .getUnitCalendarEvents(
                    dbInstance(),
                    admin,
                    clock,
                    testDaycare.id,
                    today,
                    today.plusDays(5)
                )
                .isEmpty()
        )
        assertEquals(
            0,
            db.transaction { tx -> tx.devCalendarEventUnitAttendeeCount(testDaycare.id) }
        )
    }

    @Test
    fun `extending backup care end date removes child-specific event attendance from main unit`() {
        val form =
            CalendarEventForm(
                unitId = testDaycare.id,
                tree = mapOf(groupId to setOf(testChild_1.id)),
                title = "Child-specific event",
                description = "cse",
                period = FiniteDateRange(today.plusDays(3), today.plusDays(4))
            )
        this.calendarEventController.createCalendarEvent(dbInstance(), admin, clock, form)
        this.backupCareController.createForChild(
            dbInstance(),
            admin,
            clock,
            testChild_1.id,
            NewBackupCare(
                unitId = testDaycare2.id,
                groupId = null,
                period = FiniteDateRange(today.minusDays(10), today)
            )
        )
        this.backupCareController.update(
            dbInstance(),
            admin,
            clock,
            db.transaction { tx -> tx.getBackupCaresForChild(testChild_1.id).first().id },
            BackupCareUpdateRequest(FiniteDateRange(today.minusDays(10), today.plusDays(20)), null)
        )
        assert(
            this.calendarEventController
                .getUnitCalendarEvents(
                    dbInstance(),
                    admin,
                    clock,
                    testDaycare.id,
                    today,
                    today.plusDays(5)
                )
                .isEmpty()
        )
        assertEquals(
            0,
            db.transaction { tx -> tx.devCalendarEventUnitAttendeeCount(testDaycare.id) }
        )
    }

    @Test
    fun `shrinking backup care start date removes child-specific event attendance from backup unit`() {
        this.backupCareController.createForChild(
            dbInstance(),
            admin,
            clock,
            testChild_1.id,
            NewBackupCare(
                unitId = testDaycare2.id,
                groupId = unit2GroupId,
                period = FiniteDateRange(today, today.plusDays(10))
            )
        )
        val form =
            CalendarEventForm(
                unitId = testDaycare2.id,
                tree = mapOf(unit2GroupId to setOf(testChild_1.id)),
                title = "Child-specific event",
                description = "cse",
                period = FiniteDateRange(today.plusDays(3), today.plusDays(4))
            )
        this.calendarEventController.createCalendarEvent(dbInstance(), admin, clock, form)
        this.backupCareController.update(
            dbInstance(),
            admin,
            clock,
            db.transaction { tx -> tx.getBackupCaresForChild(testChild_1.id).first().id },
            BackupCareUpdateRequest(FiniteDateRange(today.plusDays(8), today.plusDays(10)), null)
        )
        assert(
            this.calendarEventController
                .getUnitCalendarEvents(
                    dbInstance(),
                    admin,
                    clock,
                    testDaycare2.id,
                    today,
                    today.plusDays(5)
                )
                .isEmpty()
        )
        assertEquals(
            0,
            db.transaction { tx -> tx.devCalendarEventUnitAttendeeCount(testDaycare2.id) }
        )
    }

    @Test
    fun `shrinking backup care end date removes child-specific event attendance from backup unit`() {
        this.backupCareController.createForChild(
            dbInstance(),
            admin,
            clock,
            testChild_1.id,
            NewBackupCare(
                unitId = testDaycare2.id,
                groupId = unit2GroupId,
                period = FiniteDateRange(today, today.plusDays(10))
            )
        )
        val form =
            CalendarEventForm(
                unitId = testDaycare2.id,
                tree = mapOf(unit2GroupId to setOf(testChild_1.id)),
                title = "Child-specific event",
                description = "cse",
                period = FiniteDateRange(today.plusDays(3), today.plusDays(4))
            )
        this.calendarEventController.createCalendarEvent(dbInstance(), admin, clock, form)
        this.backupCareController.update(
            dbInstance(),
            admin,
            clock,
            db.transaction { tx -> tx.getBackupCaresForChild(testChild_1.id).first().id },
            BackupCareUpdateRequest(FiniteDateRange(today, today.plusDays(1)), null)
        )
        assert(
            this.calendarEventController
                .getUnitCalendarEvents(
                    dbInstance(),
                    admin,
                    clock,
                    testDaycare2.id,
                    today,
                    today.plusDays(5)
                )
                .isEmpty()
        )
        assertEquals(
            0,
            db.transaction { tx -> tx.devCalendarEventUnitAttendeeCount(testDaycare2.id) }
        )
    }

    @Test
    fun `removing backup care removes child-specific event attendance from backup unit`() {
        this.backupCareController.createForChild(
            dbInstance(),
            admin,
            clock,
            testChild_1.id,
            NewBackupCare(
                unitId = testDaycare2.id,
                groupId = unit2GroupId,
                period = FiniteDateRange(today, today.plusDays(10))
            )
        )
        val form =
            CalendarEventForm(
                unitId = testDaycare2.id,
                tree = mapOf(unit2GroupId to setOf(testChild_1.id)),
                title = "Child-specific event",
                description = "cse",
                period = FiniteDateRange(today.plusDays(3), today.plusDays(4))
            )
        this.calendarEventController.createCalendarEvent(dbInstance(), admin, clock, form)
        this.backupCareController.delete(
            dbInstance(),
            admin,
            clock,
            db.transaction { tx -> tx.getBackupCaresForChild(testChild_1.id).first().id }
        )
        assert(
            this.calendarEventController
                .getUnitCalendarEvents(
                    dbInstance(),
                    admin,
                    clock,
                    testDaycare2.id,
                    today,
                    today.plusDays(5)
                )
                .isEmpty()
        )
        assertEquals(
            0,
            db.transaction { tx -> tx.devCalendarEventUnitAttendeeCount(testDaycare2.id) }
        )
    }

    @Test
    fun `guardian terminating placement removes child-specific event attendance`() {
        val form =
            CalendarEventForm(
                unitId = testDaycare.id,
                tree = mapOf(groupId to setOf(testChild_1.id)),
                title = "Child-specific event",
                description = "cse",
                period = FiniteDateRange(today.plusDays(3), today.plusDays(4))
            )
        this.calendarEventController.createCalendarEvent(dbInstance(), admin, clock, form)
        this.placementControllerCitizen.postPlacementTermination(
            dbInstance(),
            guardian,
            clock,
            testChild_1.id,
            PlacementControllerCitizen.PlacementTerminationRequestBody(
                type = TerminatablePlacementType.DAYCARE,
                unitId = testDaycare.id,
                terminationDate = today,
                terminateDaycareOnly = null
            )
        )
        assert(
            this.calendarEventController
                .getUnitCalendarEvents(
                    dbInstance(),
                    admin,
                    clock,
                    testDaycare.id,
                    today,
                    today.plusDays(5)
                )
                .isEmpty()
        )
        assertEquals(
            0,
            db.transaction { tx -> tx.devCalendarEventUnitAttendeeCount(testDaycare.id) }
        )
    }

    @Test
    fun `events cannot be created for children that do not have an active placement in the unit`() {
        val form =
            CalendarEventForm(
                unitId = testDaycare2.id,
                tree = mapOf(groupId to setOf(testChild_1.id)),
                title = "Child-specific event",
                description = "cse",
                period = FiniteDateRange(today.plusDays(3), today.plusDays(4))
            )

        assertThrows<BadRequest> {
            this.calendarEventController.createCalendarEvent(dbInstance(), admin, clock, form)
        }
    }

    @Test
    fun `events cannot be created for children that don't have any group placement in the group`() {
        db.transaction { tx ->
            tx.insert(
                DevPlacement(
                    childId = testChild_3.id,
                    unitId = testDaycare.id,
                    startDate = placementStart,
                    endDate = placementEnd
                )
            )
        }

        val form =
            CalendarEventForm(
                unitId = testDaycare.id,
                tree = mapOf(groupId to setOf(testChild_3.id)),
                title = "Child-specific event",
                description = "cse",
                period = FiniteDateRange(today.plusDays(3), today.plusDays(4))
            )

        assertThrows<BadRequest> {
            this.calendarEventController.createCalendarEvent(dbInstance(), admin, clock, form)
        }
    }

    @Test
    fun `notifications are sent`() {
        db.transaction { tx ->
            // Email address is needed
            tx.updatePersonalDetails(
                testAdult_1.id,
                PersonalDataUpdate(
                    preferredName = "",
                    phone = "",
                    backupPhone = "",
                    email = "example@example.com",
                )
            )
        }

        val form =
            CalendarEventForm(
                unitId = testDaycare.id,
                tree = null,
                title = "Unit-wide event",
                description = "uwe",
                period = FiniteDateRange(today.plusDays(3), today.plusDays(3))
            )
        calendarEventController.createCalendarEvent(dbInstance(), admin, clock, form)

        calendarEventNotificationService.sendCalendarEventDigests(db, now)

        assertEquals(1, MockEmailClient.emails.size)
        MockEmailClient.emails.first().let { email ->
            assertEquals("example@example.com", email.toAddress)
            assertEquals(
                "Uusia kalenteritapahtumia eVakassa / Nya kalenderhändelser i eVaka / New calendar events in eVaka",
                email.content.subject
            )
        }
    }
}
