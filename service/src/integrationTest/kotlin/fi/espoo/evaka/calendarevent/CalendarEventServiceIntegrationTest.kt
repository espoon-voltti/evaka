// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.calendarevent

import fi.espoo.evaka.EmailEnv
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.backupcare.BackupCareController
import fi.espoo.evaka.backupcare.BackupCareUpdateRequest
import fi.espoo.evaka.backupcare.NewBackupCare
import fi.espoo.evaka.backupcare.getBackupCaresForChild
import fi.espoo.evaka.daycare.domain.Language
import fi.espoo.evaka.emailclient.DiscussionSurveyCreationNotificationData
import fi.espoo.evaka.emailclient.DiscussionSurveyReservationNotificationData
import fi.espoo.evaka.emailclient.DiscussionTimeReminderData
import fi.espoo.evaka.emailclient.Email
import fi.espoo.evaka.emailclient.EmailContent
import fi.espoo.evaka.emailclient.IEmailMessageProvider
import fi.espoo.evaka.emailclient.MockEmailClient
import fi.espoo.evaka.pis.PersonalDataUpdate
import fi.espoo.evaka.pis.service.insertGuardian
import fi.espoo.evaka.pis.updatePersonalDetails
import fi.espoo.evaka.placement.GroupTransferRequestBody
import fi.espoo.evaka.placement.PlacementController
import fi.espoo.evaka.placement.PlacementControllerCitizen
import fi.espoo.evaka.placement.PlacementUpdateRequestBody
import fi.espoo.evaka.placement.TerminatablePlacementType
import fi.espoo.evaka.shared.CalendarEventId
import fi.espoo.evaka.shared.CalendarEventTimeId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.GroupPlacementId
import fi.espoo.evaka.shared.HtmlSafe
import fi.espoo.evaka.shared.PlacementId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.CitizenAuthLevel
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.DevDaycareGroupPlacement
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevFosterParent
import fi.espoo.evaka.shared.dev.DevHoliday
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.Conflict
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.domain.RealEvakaClock
import fi.espoo.evaka.shared.domain.TimeRange
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testAdult_2
import fi.espoo.evaka.testAdult_3
import fi.espoo.evaka.testArea
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testChild_2
import fi.espoo.evaka.testChild_3
import fi.espoo.evaka.testChild_4
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDaycare2
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.groups.Tuple
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
    @Autowired private lateinit var asyncJobRunner: AsyncJobRunner<AsyncJob>
    @Autowired private lateinit var emailMessageProvider: IEmailMessageProvider
    @Autowired private lateinit var emailEnv: EmailEnv

    private final val adminId = EmployeeId(UUID.randomUUID())
    private val admin = AuthenticatedUser.Employee(adminId, setOf(UserRole.ADMIN))

    private val guardian = AuthenticatedUser.Citizen(testAdult_1.id, CitizenAuthLevel.STRONG)

    // a monday
    private val today = LocalDate.of(2021, 6, 5)
    private val now = HelsinkiDateTime.of(today, LocalTime.of(12, 0, 0))
    private val clock = MockEvakaClock(now)

    private final val groupId = GroupId(UUID.randomUUID())
    private final val groupId2 = GroupId(UUID.randomUUID())
    private final val group1Data =
        DevDaycareGroup(id = groupId, daycareId = testDaycare.id, name = "TestGroup1")
    private final val unit2GroupId = GroupId(UUID.randomUUID())

    private lateinit var placementId: PlacementId
    private lateinit var groupPlacementId: GroupPlacementId

    private final val placementStart = today.minusDays(100)
    private final val placementEnd = today.plusDays(100)

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insert(testArea)
            tx.insert(testDaycare)
            tx.insert(testDaycare2)
            listOf(testAdult_1, testAdult_2, testAdult_3).forEach {
                tx.insert(it, DevPersonType.ADULT)
            }
            listOf(testChild_1, testChild_2, testChild_3, testChild_4).forEach {
                tx.insert(it, DevPersonType.CHILD)
            }
            tx.insertGuardian(testAdult_1.id, testChild_1.id)
            tx.insert(DevEmployee(adminId, roles = setOf(UserRole.ADMIN)))
            tx.insert(group1Data)
            tx.insert(DevDaycareGroup(id = groupId2, daycareId = testDaycare.id))
            tx.insert(DevDaycareGroup(id = unit2GroupId, daycareId = testDaycare2.id))
            tx.insert(DevHoliday(today.plusDays(3)))

            placementId =
                tx.insert(
                    DevPlacement(
                        childId = testChild_1.id,
                        unitId = testDaycare.id,
                        startDate = placementStart,
                        endDate = placementEnd,
                    )
                )
            groupPlacementId =
                tx.insert(
                    DevDaycareGroupPlacement(
                        daycarePlacementId = placementId,
                        daycareGroupId = groupId,
                        startDate = placementStart,
                        endDate = placementEnd,
                    )
                )
            tx.insert(
                DevDaycareGroupPlacement(
                    daycarePlacementId =
                        tx.insert(
                            DevPlacement(
                                childId = testChild_2.id,
                                unitId = testDaycare.id,
                                startDate = placementStart,
                                endDate = placementEnd,
                            )
                        ),
                    daycareGroupId = groupId,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
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
                period = FiniteDateRange(today.plusDays(3), today.plusDays(3)),
                eventType = CalendarEventType.DAYCARE_EVENT,
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
                    today.plusDays(4),
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
                period = FiniteDateRange(today.plusDays(3), today.plusDays(4)),
                eventType = CalendarEventType.DAYCARE_EVENT,
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
                    today.plusDays(5),
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
                period = FiniteDateRange(today.plusDays(3), today.plusDays(4)),
                eventType = CalendarEventType.DAYCARE_EVENT,
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
                    today.plusDays(5),
                )[0]

        assertEquals(form.period, event.period)
        assertEquals(form.title, event.title)
        assertEquals(form.description, event.description)
        assertEquals(form.unitId, event.unitId)
        assertEquals(groupId, event.groups.first().id)
        assertEquals(testChild_1.id, event.individualChildren.first().id)
    }

    @Test
    fun `admin can create unit-wide calendar event with time reservation`() {
        val event =
            createCalendarEvent(
                CalendarEventForm(
                    unitId = testDaycare.id,
                    tree = null,
                    title = "Unit-wide event",
                    description = "uwe",
                    period = FiniteDateRange(today.plusDays(3), today.plusDays(3)),
                    times =
                        listOf(
                            CalendarEventTimeForm(
                                date = today.plusDays(3),
                                timeRange = TimeRange(LocalTime.of(8, 20), LocalTime.of(8, 40)),
                            )
                        ),
                    eventType = CalendarEventType.DISCUSSION_SURVEY,
                )
            )

        calendarEventController.setCalendarEventTimeReservation(
            dbInstance(),
            admin,
            clock,
            CalendarEventTimeEmployeeReservationForm(
                calendarEventTimeId = event.times.first().id,
                childId = testChild_1.id,
            ),
        )

        val calendarEventTimeId =
            calendarEventController.addCalendarEventTime(
                dbInstance(),
                admin,
                clock,
                event.id,
                CalendarEventTimeForm(
                    date = today.plusDays(3),
                    timeRange = TimeRange(LocalTime.of(8, 40), LocalTime.of(9, 0)),
                ),
            )
        calendarEventController.setCalendarEventTimeReservation(
            dbInstance(),
            admin,
            clock,
            CalendarEventTimeEmployeeReservationForm(
                calendarEventTimeId = calendarEventTimeId,
                childId = testChild_1.id,
            ),
        )
        calendarEventController.deleteCalendarEventTime(
            dbInstance(),
            admin,
            clock,
            calendarEventTimeId,
        )

        deleteCalendarEvent(event.id)
    }

    @Test
    fun `admin can create group-wide calendar event with time reservation`() {
        val event =
            createCalendarEvent(
                CalendarEventForm(
                    unitId = testDaycare.id,
                    tree = mapOf(groupId to null),
                    title = "Group-wide event",
                    description = "gwe",
                    period = FiniteDateRange(today.plusDays(3), today.plusDays(3)),
                    times =
                        listOf(
                            CalendarEventTimeForm(
                                date = today.plusDays(3),
                                timeRange = TimeRange(LocalTime.of(8, 20), LocalTime.of(8, 40)),
                            )
                        ),
                    eventType = CalendarEventType.DISCUSSION_SURVEY,
                )
            )

        calendarEventController.setCalendarEventTimeReservation(
            dbInstance(),
            admin,
            clock,
            CalendarEventTimeEmployeeReservationForm(
                calendarEventTimeId = event.times.first().id,
                childId = testChild_1.id,
            ),
        )

        val calendarEventTimeId =
            calendarEventController.addCalendarEventTime(
                dbInstance(),
                admin,
                clock,
                event.id,
                CalendarEventTimeForm(
                    date = today.plusDays(3),
                    timeRange = TimeRange(LocalTime.of(8, 40), LocalTime.of(9, 0)),
                ),
            )
        calendarEventController.setCalendarEventTimeReservation(
            dbInstance(),
            admin,
            clock,
            CalendarEventTimeEmployeeReservationForm(
                calendarEventTimeId = calendarEventTimeId,
                childId = testChild_1.id,
            ),
        )
        calendarEventController.deleteCalendarEventTime(
            dbInstance(),
            admin,
            clock,
            calendarEventTimeId,
        )

        deleteCalendarEvent(event.id)
    }

    @Test
    fun `admin can create child-specific calendar event with time reservation`() {
        val event =
            createCalendarEvent(
                CalendarEventForm(
                    unitId = testDaycare.id,
                    tree = mapOf(groupId to setOf(testChild_1.id)),
                    title = "Child-specific event",
                    description = "cse",
                    period = FiniteDateRange(today.plusDays(3), today.plusDays(3)),
                    times =
                        listOf(
                            CalendarEventTimeForm(
                                date = today.plusDays(3),
                                timeRange = TimeRange(LocalTime.of(8, 20), LocalTime.of(8, 40)),
                            )
                        ),
                    eventType = CalendarEventType.DISCUSSION_SURVEY,
                )
            )

        calendarEventController.setCalendarEventTimeReservation(
            dbInstance(),
            admin,
            clock,
            CalendarEventTimeEmployeeReservationForm(
                calendarEventTimeId = event.times.first().id,
                childId = testChild_1.id,
            ),
        )

        val calendarEventTimeId =
            calendarEventController.addCalendarEventTime(
                dbInstance(),
                admin,
                clock,
                event.id,
                CalendarEventTimeForm(
                    date = today.plusDays(3),
                    timeRange = TimeRange(LocalTime.of(8, 40), LocalTime.of(9, 0)),
                ),
            )
        calendarEventController.setCalendarEventTimeReservation(
            dbInstance(),
            admin,
            clock,
            CalendarEventTimeEmployeeReservationForm(
                calendarEventTimeId = calendarEventTimeId,
                childId = testChild_1.id,
            ),
        )
        calendarEventController.deleteCalendarEventTime(
            dbInstance(),
            admin,
            clock,
            calendarEventTimeId,
        )

        deleteCalendarEvent(event.id)
    }

    @Test
    fun `admin can change group-wide calendar event to child-specific`() {
        val created =
            createCalendarEvent(
                CalendarEventForm(
                    unitId = testDaycare.id,
                    tree = mapOf(groupId to null),
                    title = "Group-wide event",
                    description = "gwe",
                    period = FiniteDateRange(today.plusDays(3), today.plusDays(3)),
                    times =
                        listOf(
                            CalendarEventTimeForm(
                                date = today.plusDays(3),
                                timeRange = TimeRange(LocalTime.of(8, 20), LocalTime.of(8, 40)),
                            )
                        ),
                    eventType = CalendarEventType.DISCUSSION_SURVEY,
                )
            )

        val updateForm =
            CalendarEventUpdateForm(
                title = "Child-specific event",
                description = "cse",
                tree = mapOf(groupId to setOf(testChild_1.id)),
            )
        val updated = updateCalendarEvent(created.id, updateForm)

        assertThat(updated)
            .isEqualTo(
                created.copy(
                    title = updateForm.title,
                    description = updateForm.description,
                    individualChildren =
                        setOf(
                            IndividualChild(
                                testChild_1.id,
                                testChild_1.firstName,
                                testChild_1.lastName,
                                groupId,
                            )
                        ),
                )
            )
    }

    @Test
    fun `admin can remove child with time reservation from attendees`() {
        db.transaction { tx -> tx.insertGuardian(testAdult_1.id, testChild_2.id) }

        val created =
            createCalendarEvent(
                CalendarEventForm(
                    unitId = testDaycare.id,
                    tree = mapOf(groupId to setOf(testChild_1.id, testChild_2.id)),
                    title = "Child-specific event",
                    description = "cse",
                    period = FiniteDateRange(today.plusDays(3), today.plusDays(3)),
                    times =
                        listOf(
                            CalendarEventTimeForm(
                                date = today.plusDays(3),
                                timeRange = TimeRange(LocalTime.of(8, 20), LocalTime.of(8, 40)),
                            )
                        ),
                    eventType = CalendarEventType.DISCUSSION_SURVEY,
                )
            )
        calendarEventController.setCalendarEventTimeReservation(
            dbInstance(),
            admin,
            clock,
            CalendarEventTimeEmployeeReservationForm(
                calendarEventTimeId = created.times.first().id,
                childId = testChild_1.id,
            ),
        )
        assertThat(getReservableCalendarEventTimes(created.id, testChild_2.id)).isEmpty()

        val updateForm =
            CalendarEventUpdateForm(
                title = "Child-specific event",
                description = "cse",
                tree = mapOf(groupId to setOf(testChild_2.id)),
            )
        val updated = updateCalendarEvent(created.id, updateForm)
        assertThat(updated)
            .isEqualTo(
                created.copy(
                    title = updateForm.title,
                    description = updateForm.description,
                    individualChildren =
                        setOf(
                            IndividualChild(
                                testChild_2.id,
                                testChild_2.firstName,
                                testChild_2.lastName,
                                groupId,
                            )
                        ),
                )
            )
        assertThat(getReservableCalendarEventTimes(created.id, testChild_2.id))
            .extracting({ it.date }, { it.startTime }, { it.endTime })
            .containsExactly(Tuple(today.plusDays(3), LocalTime.of(8, 20), LocalTime.of(8, 40)))
    }

    @Test
    fun `citizen can add calendar event time reservation`() {
        db.transaction { tx -> tx.insertGuardian(testAdult_1.id, testChild_2.id) }

        val event =
            createCalendarEvent(
                CalendarEventForm(
                    unitId = testDaycare.id,
                    tree = mapOf(groupId to setOf(testChild_1.id, testChild_2.id)),
                    title = "Child-specific event",
                    description = "cse",
                    period = FiniteDateRange(today.plusDays(3), today.plusDays(3)),
                    times =
                        listOf(
                            CalendarEventTimeForm(
                                date = today.plusDays(3),
                                timeRange = TimeRange(LocalTime.of(8, 20), LocalTime.of(8, 40)),
                            )
                        ),
                    eventType = CalendarEventType.DISCUSSION_SURVEY,
                )
            )
        val calendarEventTimeId = event.times.first().id

        assertThat(getReservableCalendarEventTimes(event.id, testChild_1.id))
            .extracting({ it.date }, { it.startTime }, { it.endTime })
            .containsExactly(Tuple(today.plusDays(3), LocalTime.of(8, 20), LocalTime.of(8, 40)))
        assertThat(getReservableCalendarEventTimes(event.id, testChild_2.id))
            .extracting({ it.date }, { it.startTime }, { it.endTime })
            .containsExactly(Tuple(today.plusDays(3), LocalTime.of(8, 20), LocalTime.of(8, 40)))

        val reservationForm =
            CalendarEventTimeCitizenReservationForm(
                calendarEventTimeId = calendarEventTimeId,
                childId = testChild_1.id,
            )
        calendarEventController.addCalendarEventTimeReservation(
            dbInstance(),
            guardian,
            clock,
            reservationForm,
        )
        assertThat(getReservableCalendarEventTimes(event.id, testChild_1.id))
            .extracting({ it.date }, { it.startTime }, { it.endTime })
            .containsExactly(Tuple(today.plusDays(3), LocalTime.of(8, 20), LocalTime.of(8, 40)))
        assertThat(getReservableCalendarEventTimes(event.id, testChild_2.id)).isEmpty()
        assertThrows<Conflict> {
            calendarEventController.addCalendarEventTimeReservation(
                dbInstance(),
                guardian,
                clock,
                CalendarEventTimeCitizenReservationForm(
                    calendarEventTimeId = calendarEventTimeId,
                    childId = testChild_2.id,
                ),
            )
        }

        // adding same reservation again is nop
        calendarEventController.addCalendarEventTimeReservation(
            dbInstance(),
            guardian,
            clock,
            reservationForm,
        )
        assertThat(getReservableCalendarEventTimes(event.id, testChild_1.id))
            .extracting({ it.date }, { it.startTime }, { it.endTime })
            .containsExactly(Tuple(today.plusDays(3), LocalTime.of(8, 20), LocalTime.of(8, 40)))
        assertThat(getReservableCalendarEventTimes(event.id, testChild_2.id)).isEmpty()

        calendarEventController.deleteCalendarEventTimeReservation(
            dbInstance(),
            guardian,
            clock,
            reservationForm.calendarEventTimeId,
            reservationForm.childId,
        )
        assertThat(getReservableCalendarEventTimes(event.id, testChild_1.id))
            .extracting({ it.date }, { it.startTime }, { it.endTime })
            .containsExactly(Tuple(today.plusDays(3), LocalTime.of(8, 20), LocalTime.of(8, 40)))
        assertThat(getReservableCalendarEventTimes(event.id, testChild_2.id))
            .extracting({ it.date }, { it.startTime }, { it.endTime })
            .containsExactly(Tuple(today.plusDays(3), LocalTime.of(8, 20), LocalTime.of(8, 40)))
    }

    @Test
    fun `employee can edit a calendar event's title and description`() {
        val form =
            CalendarEventForm(
                unitId = testDaycare.id,
                tree = mapOf(groupId to setOf(testChild_1.id)),
                title = "Child-specific event",
                description = "cse",
                period = FiniteDateRange(today.plusDays(3), today.plusDays(4)),
                eventType = CalendarEventType.DAYCARE_EVENT,
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
                    today.plusDays(4),
                )[0]
                .id,
            CalendarEventUpdateForm(title = "Updated title", description = "desc, updated"),
        )

        val event =
            this.calendarEventController
                .getUnitCalendarEvents(
                    dbInstance(),
                    admin,
                    clock,
                    testDaycare.id,
                    today,
                    today.plusDays(4),
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
                period = FiniteDateRange(today.plusDays(3), today.plusDays(4)),
                eventType = CalendarEventType.DAYCARE_EVENT,
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
                    today.plusDays(4),
                )[0]
                .id,
        )

        assert(
            this.calendarEventController
                .getUnitCalendarEvents(
                    dbInstance(),
                    admin,
                    clock,
                    testDaycare.id,
                    today,
                    today.plusDays(4),
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
                period = FiniteDateRange(today.plusDays(3), today.plusDays(4)),
                eventType = CalendarEventType.DAYCARE_EVENT,
            )
        this.calendarEventController.createCalendarEvent(dbInstance(), admin, clock, form)

        val form2 =
            CalendarEventForm(
                unitId = testDaycare.id,
                tree = null,
                title = "Unit-wide event",
                description = "uw",
                period = FiniteDateRange(today.plusDays(1), today.plusDays(1)),
                eventType = CalendarEventType.DAYCARE_EVENT,
            )
        this.calendarEventController.createCalendarEvent(dbInstance(), admin, clock, form2)

        val form3 =
            CalendarEventForm(
                unitId = testDaycare.id,
                tree = mapOf(groupId to null),
                title = "Group event",
                description = "gw",
                period = FiniteDateRange(today.plusDays(1), today.plusDays(1)),
                eventType = CalendarEventType.DAYCARE_EVENT,
            )
        this.calendarEventController.createCalendarEvent(dbInstance(), admin, clock, form3)

        val guardianEvents =
            this.calendarEventController.getCitizenCalendarEvents(
                dbInstance(),
                guardian,
                clock,
                today,
                today.plusDays(10),
            )

        val individualEvent = guardianEvents.find { it.title == "Child-specific event" }
        assertNotNull(individualEvent)
        assertEquals(form.title, individualEvent.title)
        assertEquals(form.description, individualEvent.description)
        assertEquals(1, individualEvent.attendingChildren.size)

        individualEvent.attendingChildren[testChild_1.id]?.first().let { attendee ->
            assertNotNull(attendee)
            assertEquals("TestGroup1", attendee.groupName)
            assertEquals(AttendanceType.INDIVIDUAL, attendee.type)
            assertEquals(
                listOf(FiniteDateRange(today.plusDays(3), today.plusDays(4))),
                attendee.periods,
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
            assertEquals(AttendanceType.UNIT, attendee.type)
            assertEquals(
                listOf(FiniteDateRange(today.plusDays(1), today.plusDays(1))),
                attendee.periods,
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
            assertEquals(AttendanceType.GROUP, attendee.type)
            assertEquals(
                listOf(FiniteDateRange(today.plusDays(1), today.plusDays(1))),
                attendee.periods,
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
                period = FiniteDateRange(today.plusDays(3), today.plusDays(4)),
                eventType = CalendarEventType.DAYCARE_EVENT,
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
            assertEquals(AttendanceType.GROUP, attendee.type)
            assertEquals(
                listOf(FiniteDateRange(today.plusDays(3), today.plusDays(4))),
                attendee.periods,
            )
        }

        event.attendingChildren[testChild_2.id]?.first().let { attendee ->
            assertNotNull(attendee)
            assertEquals("TestGroup1", attendee.groupName)
            assertEquals(AttendanceType.GROUP, attendee.type)
            assertEquals(
                listOf(FiniteDateRange(today.plusDays(3), today.plusDays(4))),
                attendee.periods,
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
                period = FiniteDateRange(today.minusDays(1), today.plusDays(2)),
                eventType = CalendarEventType.DAYCARE_EVENT,
            )
        this.calendarEventController.createCalendarEvent(dbInstance(), admin, clock, form)

        assert(
            this.calendarEventController
                .getCitizenCalendarEvents(
                    dbInstance(),
                    guardian,
                    clock,
                    today.minusDays(10),
                    today.plusDays(10),
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
                period = FiniteDateRange(today.minusDays(1), today.plusDays(2)),
                eventType = CalendarEventType.DAYCARE_EVENT,
            )
        this.calendarEventController.createCalendarEvent(dbInstance(), admin, clock, form)

        assert(
            this.calendarEventController
                .getCitizenCalendarEvents(
                    dbInstance(),
                    guardian,
                    clock,
                    today.minusDays(10),
                    today.plusDays(10),
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
                period = FiniteDateRange(today.minusDays(1), today.plusDays(2)),
                eventType = CalendarEventType.DAYCARE_EVENT,
            )
        this.calendarEventController.createCalendarEvent(dbInstance(), admin, clock, form)

        assert(
            this.calendarEventController
                .getCitizenCalendarEvents(
                    dbInstance(),
                    guardian,
                    clock,
                    today.minusDays(10),
                    today.plusDays(10),
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
                period = FiniteDateRange(placementStart.minusDays(10), placementStart.minusDays(8)),
                eventType = CalendarEventType.DAYCARE_EVENT,
            )
        this.calendarEventController.createCalendarEvent(dbInstance(), admin, clock, form)

        val form2 =
            CalendarEventForm(
                unitId = testDaycare.id,
                tree = null,
                title = "Unit-wide event",
                description = "uw",
                period =
                    FiniteDateRange(placementStart.minusDays(20), placementStart.minusDays(20)),
                eventType = CalendarEventType.DAYCARE_EVENT,
            )
        this.calendarEventController.createCalendarEvent(dbInstance(), admin, clock, form2)

        assert(
            this.calendarEventController
                .getCitizenCalendarEvents(
                    dbInstance(),
                    guardian,
                    clock,
                    placementStart.minusDays(50),
                    placementStart.plusDays(50),
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
                tx.insert(
                    DevPlacement(
                        childId = testChild_3.id,
                        unitId = testDaycare.id,
                        startDate = placementStart,
                        endDate = placementEnd,
                    )
                )
            tx.insert(
                DevDaycareGroupPlacement(
                    daycarePlacementId = placementId,
                    daycareGroupId = groupId,
                    startDate = placementStart,
                    endDate = backupCareStart.minusDays(1),
                )
            )
            tx.insert(
                DevDaycareGroupPlacement(
                    daycarePlacementId = placementId,
                    daycareGroupId = groupId,
                    startDate = backupCareEnd.plusDays(1),
                    endDate = placementEnd,
                )
            )
        }
        this.backupCareController.createBackupCare(
            dbInstance(),
            admin,
            clock,
            testChild_3.id,
            NewBackupCare(testDaycare2.id, null, FiniteDateRange(backupCareStart, backupCareEnd)),
        )

        val form =
            CalendarEventForm(
                unitId = testDaycare.id,
                tree = mapOf(groupId to null),
                title = "Group event (unit 1)",
                description = "u1_g",
                period = FiniteDateRange(placementStart, placementStart.plusDays(30)),
                eventType = CalendarEventType.DAYCARE_EVENT,
            )
        this.calendarEventController.createCalendarEvent(dbInstance(), admin, clock, form)

        val form2 =
            CalendarEventForm(
                unitId = testDaycare2.id,
                tree = null,
                title = "Unit event (unit 2)",
                description = "u1_u",
                period = FiniteDateRange(placementStart, placementStart.plusDays(30)),
                eventType = CalendarEventType.DAYCARE_EVENT,
            )
        this.calendarEventController.createCalendarEvent(dbInstance(), admin, clock, form2)

        val guardianEvents =
            this.calendarEventController.getCitizenCalendarEvents(
                dbInstance(),
                AuthenticatedUser.Citizen(testAdult_3.id, CitizenAuthLevel.STRONG),
                clock,
                placementStart,
                placementStart.plusDays(50),
            )

        val mainUnitEvent = guardianEvents.find { it.title == "Group event (unit 1)" }
        assertNotNull(mainUnitEvent)
        assertEquals(form.title, mainUnitEvent.title)
        assertEquals(form.description, mainUnitEvent.description)
        assertEquals(1, mainUnitEvent.attendingChildren.size)

        mainUnitEvent.attendingChildren[testChild_3.id]?.first().let { attendee ->
            assertNotNull(attendee)
            assertEquals("TestGroup1", attendee.groupName)
            assertEquals(AttendanceType.GROUP, attendee.type)
            assertEquals(
                listOf(
                    FiniteDateRange(placementStart, placementStart.plusDays(9)),
                    FiniteDateRange(placementStart.plusDays(21), placementStart.plusDays(30)),
                ),
                attendee.periods.sortedBy { it.start },
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
            assertEquals(AttendanceType.UNIT, attendee.type)
            assertEquals(
                listOf(FiniteDateRange(placementStart.plusDays(10), placementStart.plusDays(20))),
                attendee.periods,
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
                period = FiniteDateRange(today.plusDays(3), today.plusDays(4)),
                eventType = CalendarEventType.DAYCARE_EVENT,
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
                    today.plusDays(5),
                )
                .isEmpty()
        )
        assertEquals(
            0,
            db.transaction { tx -> tx.devCalendarEventUnitAttendeeCount(testDaycare.id) },
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
                period = FiniteDateRange(today.plusDays(3), today.plusDays(4)),
                eventType = CalendarEventType.DAYCARE_EVENT,
            )
        this.calendarEventController.createCalendarEvent(dbInstance(), admin, clock, form)
        this.placementController.updatePlacementById(
            dbInstance(),
            admin,
            clock,
            placementId,
            PlacementUpdateRequestBody(startDate = today.plusDays(10), endDate = placementEnd),
        )
        assert(
            this.calendarEventController
                .getUnitCalendarEvents(
                    dbInstance(),
                    admin,
                    clock,
                    testDaycare.id,
                    today,
                    today.plusDays(5),
                )
                .isEmpty()
        )
        assertEquals(
            0,
            db.transaction { tx -> tx.devCalendarEventUnitAttendeeCount(testDaycare.id) },
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
                period = FiniteDateRange(today.plusDays(3), today.plusDays(4)),
                eventType = CalendarEventType.DAYCARE_EVENT,
            )
        this.calendarEventController.createCalendarEvent(dbInstance(), admin, clock, form)
        this.placementController.updatePlacementById(
            dbInstance(),
            admin,
            clock,
            placementId,
            PlacementUpdateRequestBody(startDate = placementStart, endDate = today.minusDays(1)),
        )
        assert(
            this.calendarEventController
                .getUnitCalendarEvents(
                    dbInstance(),
                    admin,
                    clock,
                    testDaycare.id,
                    today,
                    today.plusDays(5),
                )
                .isEmpty()
        )
        assertEquals(
            0,
            db.transaction { tx -> tx.devCalendarEventUnitAttendeeCount(testDaycare.id) },
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
                period = FiniteDateRange(today.plusDays(3), today.plusDays(4)),
                eventType = CalendarEventType.DAYCARE_EVENT,
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
                    today.plusDays(5),
                )
                .isEmpty()
        )
        assertEquals(
            0,
            db.transaction { tx -> tx.devCalendarEventUnitAttendeeCount(testDaycare.id) },
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
                period = FiniteDateRange(today.plusDays(3), today.plusDays(4)),
                eventType = CalendarEventType.DAYCARE_EVENT,
            )
        this.calendarEventController.createCalendarEvent(dbInstance(), admin, clock, form)
        this.placementController.transferGroupPlacement(
            dbInstance(),
            admin,
            clock,
            groupPlacementId,
            GroupTransferRequestBody(groupId = groupId2, startDate = today),
        )
        assert(
            this.calendarEventController
                .getUnitCalendarEvents(
                    dbInstance(),
                    admin,
                    clock,
                    testDaycare.id,
                    today,
                    today.plusDays(5),
                )
                .isEmpty()
        )
        assertEquals(
            0,
            db.transaction { tx -> tx.devCalendarEventUnitAttendeeCount(testDaycare.id) },
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
                period = FiniteDateRange(today.plusDays(3), today.plusDays(4)),
                eventType = CalendarEventType.DAYCARE_EVENT,
            )
        this.calendarEventController.createCalendarEvent(dbInstance(), admin, clock, form)
        this.placementController.transferGroupPlacement(
            dbInstance(),
            admin,
            clock,
            groupPlacementId,
            GroupTransferRequestBody(groupId = groupId2, startDate = today.plusDays(5)),
        )
        assert(
            this.calendarEventController
                .getUnitCalendarEvents(
                    dbInstance(),
                    admin,
                    clock,
                    testDaycare.id,
                    today,
                    today.plusDays(5),
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
                period = FiniteDateRange(today.plusDays(3), today.plusDays(4)),
                eventType = CalendarEventType.DAYCARE_EVENT,
            )
        this.calendarEventController.createCalendarEvent(dbInstance(), admin, clock, form)
        this.backupCareController.createBackupCare(
            dbInstance(),
            admin,
            clock,
            testChild_1.id,
            NewBackupCare(
                unitId = testDaycare2.id,
                groupId = null,
                period = FiniteDateRange(today, today.plusDays(10)),
            ),
        )
        assert(
            this.calendarEventController
                .getUnitCalendarEvents(
                    dbInstance(),
                    admin,
                    clock,
                    testDaycare.id,
                    today,
                    today.plusDays(5),
                )
                .isEmpty()
        )
        assertEquals(
            0,
            db.transaction { tx -> tx.devCalendarEventUnitAttendeeCount(testDaycare.id) },
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
                period = FiniteDateRange(today.plusDays(3), today.plusDays(4)),
                eventType = CalendarEventType.DAYCARE_EVENT,
            )
        this.calendarEventController.createCalendarEvent(dbInstance(), admin, clock, form)
        this.backupCareController.createBackupCare(
            dbInstance(),
            admin,
            clock,
            testChild_1.id,
            NewBackupCare(
                unitId = testDaycare2.id,
                groupId = null,
                period = FiniteDateRange(today.minusDays(10), today),
            ),
        )
        assert(
            this.calendarEventController
                .getUnitCalendarEvents(
                    dbInstance(),
                    admin,
                    clock,
                    testDaycare.id,
                    today,
                    today.plusDays(5),
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
                period = FiniteDateRange(today.plusDays(3), today.plusDays(4)),
                eventType = CalendarEventType.DAYCARE_EVENT,
            )
        this.calendarEventController.createCalendarEvent(dbInstance(), admin, clock, form)
        this.backupCareController.createBackupCare(
            dbInstance(),
            admin,
            clock,
            testChild_1.id,
            NewBackupCare(
                unitId = testDaycare2.id,
                groupId = null,
                period = FiniteDateRange(today.plusDays(10), today.plusDays(20)),
            ),
        )
        this.backupCareController.updateBackupCare(
            dbInstance(),
            admin,
            clock,
            db.transaction { tx -> tx.getBackupCaresForChild(testChild_1.id).first().id },
            BackupCareUpdateRequest(FiniteDateRange(today, today.plusDays(20)), null),
        )
        assert(
            this.calendarEventController
                .getUnitCalendarEvents(
                    dbInstance(),
                    admin,
                    clock,
                    testDaycare.id,
                    today,
                    today.plusDays(5),
                )
                .isEmpty()
        )
        assertEquals(
            0,
            db.transaction { tx -> tx.devCalendarEventUnitAttendeeCount(testDaycare.id) },
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
                period = FiniteDateRange(today.plusDays(3), today.plusDays(4)),
                eventType = CalendarEventType.DAYCARE_EVENT,
            )
        this.calendarEventController.createCalendarEvent(dbInstance(), admin, clock, form)
        this.backupCareController.createBackupCare(
            dbInstance(),
            admin,
            clock,
            testChild_1.id,
            NewBackupCare(
                unitId = testDaycare2.id,
                groupId = null,
                period = FiniteDateRange(today.minusDays(10), today),
            ),
        )
        this.backupCareController.updateBackupCare(
            dbInstance(),
            admin,
            clock,
            db.transaction { tx -> tx.getBackupCaresForChild(testChild_1.id).first().id },
            BackupCareUpdateRequest(FiniteDateRange(today.minusDays(10), today.plusDays(20)), null),
        )
        assert(
            this.calendarEventController
                .getUnitCalendarEvents(
                    dbInstance(),
                    admin,
                    clock,
                    testDaycare.id,
                    today,
                    today.plusDays(5),
                )
                .isEmpty()
        )
        assertEquals(
            0,
            db.transaction { tx -> tx.devCalendarEventUnitAttendeeCount(testDaycare.id) },
        )
    }

    @Test
    fun `shrinking backup care start date removes child-specific event attendance from backup unit`() {
        this.backupCareController.createBackupCare(
            dbInstance(),
            admin,
            clock,
            testChild_1.id,
            NewBackupCare(
                unitId = testDaycare2.id,
                groupId = unit2GroupId,
                period = FiniteDateRange(today, today.plusDays(10)),
            ),
        )
        val form =
            CalendarEventForm(
                unitId = testDaycare2.id,
                tree = mapOf(unit2GroupId to setOf(testChild_1.id)),
                title = "Child-specific event",
                description = "cse",
                period = FiniteDateRange(today.plusDays(3), today.plusDays(4)),
                eventType = CalendarEventType.DAYCARE_EVENT,
            )
        this.calendarEventController.createCalendarEvent(dbInstance(), admin, clock, form)
        this.backupCareController.updateBackupCare(
            dbInstance(),
            admin,
            clock,
            db.transaction { tx -> tx.getBackupCaresForChild(testChild_1.id).first().id },
            BackupCareUpdateRequest(FiniteDateRange(today.plusDays(8), today.plusDays(10)), null),
        )
        assert(
            this.calendarEventController
                .getUnitCalendarEvents(
                    dbInstance(),
                    admin,
                    clock,
                    testDaycare2.id,
                    today,
                    today.plusDays(5),
                )
                .isEmpty()
        )
        assertEquals(
            0,
            db.transaction { tx -> tx.devCalendarEventUnitAttendeeCount(testDaycare2.id) },
        )
    }

    @Test
    fun `shrinking backup care end date removes child-specific event attendance from backup unit`() {
        this.backupCareController.createBackupCare(
            dbInstance(),
            admin,
            clock,
            testChild_1.id,
            NewBackupCare(
                unitId = testDaycare2.id,
                groupId = unit2GroupId,
                period = FiniteDateRange(today, today.plusDays(10)),
            ),
        )
        val form =
            CalendarEventForm(
                unitId = testDaycare2.id,
                tree = mapOf(unit2GroupId to setOf(testChild_1.id)),
                title = "Child-specific event",
                description = "cse",
                period = FiniteDateRange(today.plusDays(3), today.plusDays(4)),
                eventType = CalendarEventType.DAYCARE_EVENT,
            )
        this.calendarEventController.createCalendarEvent(dbInstance(), admin, clock, form)
        this.backupCareController.updateBackupCare(
            dbInstance(),
            admin,
            clock,
            db.transaction { tx -> tx.getBackupCaresForChild(testChild_1.id).first().id },
            BackupCareUpdateRequest(FiniteDateRange(today, today.plusDays(1)), null),
        )
        assert(
            this.calendarEventController
                .getUnitCalendarEvents(
                    dbInstance(),
                    admin,
                    clock,
                    testDaycare2.id,
                    today,
                    today.plusDays(5),
                )
                .isEmpty()
        )
        assertEquals(
            0,
            db.transaction { tx -> tx.devCalendarEventUnitAttendeeCount(testDaycare2.id) },
        )
    }

    @Test
    fun `removing backup care removes child-specific event attendance from backup unit`() {
        this.backupCareController.createBackupCare(
            dbInstance(),
            admin,
            clock,
            testChild_1.id,
            NewBackupCare(
                unitId = testDaycare2.id,
                groupId = unit2GroupId,
                period = FiniteDateRange(today, today.plusDays(10)),
            ),
        )
        val form =
            CalendarEventForm(
                unitId = testDaycare2.id,
                tree = mapOf(unit2GroupId to setOf(testChild_1.id)),
                title = "Child-specific event",
                description = "cse",
                period = FiniteDateRange(today.plusDays(3), today.plusDays(4)),
                eventType = CalendarEventType.DAYCARE_EVENT,
            )
        this.calendarEventController.createCalendarEvent(dbInstance(), admin, clock, form)
        this.backupCareController.deleteBackupCare(
            dbInstance(),
            admin,
            clock,
            db.transaction { tx -> tx.getBackupCaresForChild(testChild_1.id).first().id },
        )
        assert(
            this.calendarEventController
                .getUnitCalendarEvents(
                    dbInstance(),
                    admin,
                    clock,
                    testDaycare2.id,
                    today,
                    today.plusDays(5),
                )
                .isEmpty()
        )
        assertEquals(
            0,
            db.transaction { tx -> tx.devCalendarEventUnitAttendeeCount(testDaycare2.id) },
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
                period = FiniteDateRange(today.plusDays(3), today.plusDays(4)),
                eventType = CalendarEventType.DAYCARE_EVENT,
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
                terminateDaycareOnly = null,
            ),
        )
        assert(
            this.calendarEventController
                .getUnitCalendarEvents(
                    dbInstance(),
                    admin,
                    clock,
                    testDaycare.id,
                    today,
                    today.plusDays(5),
                )
                .isEmpty()
        )
        assertEquals(
            0,
            db.transaction { tx -> tx.devCalendarEventUnitAttendeeCount(testDaycare.id) },
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
                period = FiniteDateRange(today.plusDays(3), today.plusDays(4)),
                eventType = CalendarEventType.DAYCARE_EVENT,
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
                    endDate = placementEnd,
                )
            )
        }

        val form =
            CalendarEventForm(
                unitId = testDaycare.id,
                tree = mapOf(groupId to setOf(testChild_3.id)),
                title = "Child-specific event",
                description = "cse",
                period = FiniteDateRange(today.plusDays(3), today.plusDays(4)),
                eventType = CalendarEventType.DAYCARE_EVENT,
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
                ),
            )
        }

        val form =
            CalendarEventForm(
                unitId = testDaycare.id,
                tree = null,
                title = "Unit-wide event",
                description = "uwe",
                period = FiniteDateRange(today.plusDays(3), today.plusDays(3)),
                eventType = CalendarEventType.DAYCARE_EVENT,
            )
        calendarEventController.createCalendarEvent(dbInstance(), admin, clock, form)

        calendarEventNotificationService.scheduleCalendarEventDigestEmails(db, now)
        asyncJobRunner.runPendingJobsSync(clock)

        assertEquals(1, MockEmailClient.emails.size)
        MockEmailClient.emails.first().let { email ->
            assertEquals("example@example.com", email.toAddress)
            assertEquals(
                "Uusia kalenteritapahtumia eVakassa / Nya kalenderhändelser i eVaka / New calendar events in eVaka",
                email.content.subject,
            )
        }
    }

    @Test
    fun `notifications are sent only to appropriate guardians for new discussion surveys`() {
        val email = "example@example.com"
        db.transaction { tx ->
            // Email address is needed
            tx.updatePersonalDetails(
                testAdult_1.id,
                PersonalDataUpdate(
                    preferredName = "",
                    phone = "",
                    backupPhone = "",
                    email = "example@example.com",
                ),
            )
            tx.insertGuardian(testAdult_3.id, testChild_3.id)
            val placementId =
                tx.insert(
                    DevPlacement(
                        childId = testChild_3.id,
                        unitId = testDaycare.id,
                        startDate = placementStart,
                        endDate = placementEnd,
                    )
                )
            tx.insert(
                DevDaycareGroupPlacement(
                    daycarePlacementId = placementId,
                    daycareGroupId = groupId2,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )
        }

        val groupSurveyForm =
            CalendarEventForm(
                unitId = testDaycare.id,
                tree = mapOf(groupId to null),
                title = "Group survey",
                description = "gsu",
                period = FiniteDateRange(today.plusDays(3), today.plusDays(3)),
                eventType = CalendarEventType.DISCUSSION_SURVEY,
                times =
                    listOf(
                        CalendarEventTimeForm(
                            date = today.plusDays(1),
                            timeRange = TimeRange(LocalTime.of(8, 0), LocalTime.of(9, 0)),
                        )
                    ),
            )

        val individualChildSurveyForm =
            CalendarEventForm(
                unitId = testDaycare.id,
                tree = mapOf(groupId to setOf(testChild_1.id)),
                title = "Individual child survey",
                description = "ids",
                period = FiniteDateRange(today.plusDays(3), today.plusDays(3)),
                eventType = CalendarEventType.DISCUSSION_SURVEY,
                times =
                    listOf(
                        CalendarEventTimeForm(
                            date = today.plusDays(1),
                            timeRange = TimeRange(LocalTime.of(8, 0), LocalTime.of(9, 0)),
                        )
                    ),
            )

        val event = createCalendarEvent(groupSurveyForm)
        val event2 = createCalendarEvent(individualChildSurveyForm)

        calendarEventNotificationService.scheduleDiscussionSurveyDigests(db, now)

        asyncJobRunner.runPendingJobsSync(RealEvakaClock())

        val emailDetails =
            DiscussionSurveyCreationNotificationData(
                eventId = event.id,
                eventTitle = HtmlSafe(event.title),
                eventDescription = HtmlSafe(event.description),
            )

        val emailDetails2 =
            DiscussionSurveyCreationNotificationData(
                eventId = event2.id,
                eventTitle = HtmlSafe(event2.title),
                eventDescription = HtmlSafe(event2.description),
            )

        val notificationEmailContent =
            emailMessageProvider.discussionSurveyCreationNotification(
                language = Language.fi,
                notificationDetails = emailDetails,
            )

        val notificationEmailContent2 =
            emailMessageProvider.discussionSurveyCreationNotification(
                language = Language.fi,
                notificationDetails = emailDetails2,
            )

        val expectedFromAddress = "${emailEnv.senderNameFi} <${emailEnv.senderAddress}>"
        val mails = MockEmailClient.emails
        assertEquals(2, MockEmailClient.emails.size)
        assertAllEmailsFor(
            testAdult_1.copy(email = email),
            listOf(notificationEmailContent, notificationEmailContent2),
            expectedFromAddress,
        )
    }

    @Test
    fun `notifications are sent for new child specific discussion survey`() {
        val email = "example@example.com"
        db.transaction { tx ->
            // Email address is needed
            tx.updatePersonalDetails(
                testAdult_1.id,
                PersonalDataUpdate(
                    preferredName = "",
                    phone = "",
                    backupPhone = "",
                    email = "example@example.com",
                ),
            )
        }

        val form =
            CalendarEventForm(
                unitId = testDaycare.id,
                tree = mapOf(groupId to setOf(testChild_1.id)),
                title = "Group survey",
                description = "gsu",
                period = FiniteDateRange(today.plusDays(3), today.plusDays(3)),
                eventType = CalendarEventType.DISCUSSION_SURVEY,
                times =
                    listOf(
                        CalendarEventTimeForm(
                            date = today.plusDays(1),
                            timeRange = TimeRange(LocalTime.of(8, 0), LocalTime.of(9, 0)),
                        )
                    ),
            )

        val event = createCalendarEvent(form)

        calendarEventNotificationService.scheduleDiscussionSurveyDigests(db, now)

        asyncJobRunner.runPendingJobsSync(RealEvakaClock())

        val expectedRecipients = listOf(testAdult_1.copy(email = email))

        val emailDetails =
            DiscussionSurveyCreationNotificationData(
                eventId = event.id,
                eventTitle = HtmlSafe(event.title),
                eventDescription = HtmlSafe(event.description),
            )

        val notificationEmailContent =
            emailMessageProvider.discussionSurveyCreationNotification(
                language = Language.fi,
                notificationDetails = emailDetails,
            )
        val expectedFromAddress = "${emailEnv.senderNameFi} <${emailEnv.senderAddress}>"
        assertEmails(expectedRecipients, notificationEmailContent, expectedFromAddress)
    }

    @Test
    fun `reminder sent for impending discussion time reservation`() {
        val email = "example@example.com"
        db.transaction { tx ->
            // Email address is needed
            tx.updatePersonalDetails(
                testAdult_1.id,
                PersonalDataUpdate(
                    preferredName = "",
                    phone = "",
                    backupPhone = "",
                    email = "example@example.com",
                ),
            )
        }

        val eventTimeForm =
            CalendarEventTimeForm(
                date = today.plusDays(2),
                timeRange = TimeRange(LocalTime.of(8, 0), LocalTime.of(9, 0)),
            )
        val form =
            CalendarEventForm(
                unitId = testDaycare.id,
                tree = mapOf(groupId to null),
                title = "Group survey",
                description = "gsu",
                period = FiniteDateRange(today.plusDays(3), today.plusDays(3)),
                eventType = CalendarEventType.DISCUSSION_SURVEY,
                times = listOf(eventTimeForm),
            )

        val event = createCalendarEvent(form)

        val reservationForm =
            CalendarEventTimeEmployeeReservationForm(event.times.first().id, testChild_1.id)

        calendarEventController.setCalendarEventTimeReservation(
            dbInstance(),
            admin,
            clock,
            reservationForm,
        )

        // flush reservation email
        asyncJobRunner.runPendingJobsSync(RealEvakaClock())
        MockEmailClient.clear()

        calendarEventNotificationService.scheduleDiscussionTimeReminders(db, now)

        asyncJobRunner.runPendingJobsSync(RealEvakaClock())

        val expectedRecipients = listOf(testAdult_1.copy(email = email))

        val emailDetails =
            DiscussionTimeReminderData(
                startTime = eventTimeForm.timeRange.start.inner,
                endTime = eventTimeForm.timeRange.end.inner,
                date = eventTimeForm.date,
            )

        val reminderEmailContent =
            emailMessageProvider.discussionTimeReservationReminder(
                language = Language.fi,
                reminderData = emailDetails,
            )
        val expectedFromAddress = "${emailEnv.senderNameFi} <${emailEnv.senderAddress}>"
        assertEmails(expectedRecipients, reminderEmailContent, expectedFromAddress)
    }

    @Test
    fun `reminders not sent outside notification window for discussion time reservations`() {
        db.transaction { tx ->
            // Email address is needed
            tx.updatePersonalDetails(
                testAdult_1.id,
                PersonalDataUpdate(
                    preferredName = "",
                    phone = "",
                    backupPhone = "",
                    email = "example@example.com",
                ),
            )
        }

        val eventTimeForm =
            CalendarEventTimeForm(
                date = today.plusDays(1),
                timeRange = TimeRange(LocalTime.of(8, 0), LocalTime.of(9, 0)),
            )

        val eventTimeForm2 =
            CalendarEventTimeForm(
                date = today.plusDays(3),
                timeRange = TimeRange(LocalTime.of(8, 0), LocalTime.of(9, 0)),
            )
        val form =
            CalendarEventForm(
                unitId = testDaycare.id,
                tree = mapOf(groupId to null),
                title = "Group survey",
                description = "gsu",
                period = FiniteDateRange(today.plusDays(3), today.plusDays(3)),
                eventType = CalendarEventType.DISCUSSION_SURVEY,
                times = listOf(eventTimeForm, eventTimeForm2),
            )

        val event = createCalendarEvent(form)

        calendarEventController.setCalendarEventTimeReservation(
            dbInstance(),
            admin,
            clock,
            CalendarEventTimeEmployeeReservationForm(event.times.first().id, testChild_1.id),
        )
        calendarEventController.setCalendarEventTimeReservation(
            dbInstance(),
            admin,
            clock,
            CalendarEventTimeEmployeeReservationForm(event.times.last().id, testChild_1.id),
        )

        // flush reservation email
        asyncJobRunner.runPendingJobsSync(RealEvakaClock())
        MockEmailClient.clear()

        calendarEventNotificationService.scheduleDiscussionTimeReminders(db, now)

        asyncJobRunner.runPendingJobsSync(RealEvakaClock())

        assertEquals(0, MockEmailClient.emails.size)
    }

    @Test
    fun `content timestamp updated on all edits except reservation changes`() {
        val tickingClock = MockEvakaClock(now)

        val form =
            CalendarEventForm(
                unitId = testDaycare.id,
                tree = null,
                title = "Unit-wide event",
                description = "uwe",
                period = FiniteDateRange(today, today.plusDays(3)),
                eventType = CalendarEventType.DISCUSSION_SURVEY,
            )
        val newEventData = createCalendarEvent(form = form, clock = tickingClock)

        tickingClock.tick()

        val updateForm =
            CalendarEventUpdateForm(
                title = "Updated title",
                description = form.description,
                tree = form.tree,
            )

        val modifiedEventData =
            modifyCalendarEvent(form = updateForm, clock = tickingClock, id = newEventData.id)

        assertThat(newEventData.contentModifiedAt < modifiedEventData.contentModifiedAt)
        tickingClock.tick()

        val updatedEventData =
            updateCalendarEvent(
                clock = tickingClock,
                id = newEventData.id,
                form = updateForm.copy(tree = mapOf(groupId to setOf(testChild_1.id))),
            )

        assertThat(modifiedEventData.contentModifiedAt < updatedEventData.contentModifiedAt)
        tickingClock.tick()

        val eventTimeForm =
            CalendarEventTimeForm(
                date = today.plusDays(1),
                timeRange = TimeRange(LocalTime.of(9, 0), LocalTime.of(9, 30)),
            )

        val eventTimeId =
            addEventTimeAsEmployee(
                form = eventTimeForm,
                eventId = newEventData.id,
                clock = tickingClock,
            )
        val timeAddedEventData = readCalendarEvent(newEventData.id)

        assertThat(updatedEventData.contentModifiedAt < timeAddedEventData.contentModifiedAt)
        tickingClock.tick()

        val citizenReservationForm =
            CalendarEventTimeCitizenReservationForm(
                calendarEventTimeId = eventTimeId,
                childId = testChild_1.id,
            )

        addEventTimeReservationAsCitizen(form = citizenReservationForm, clock = tickingClock)
        val citizenReservedEventData = readCalendarEvent(newEventData.id)

        assertEquals(
            timeAddedEventData.contentModifiedAt,
            citizenReservedEventData.contentModifiedAt,
        )
        tickingClock.tick()

        deleteEventTimeReservationAsCitizen(
            eventTimeId = eventTimeId,
            clock = tickingClock,
            childId = testChild_1.id,
        )
        val citizenCancelledEventData = readCalendarEvent(newEventData.id)

        assertEquals(
            timeAddedEventData.contentModifiedAt,
            citizenCancelledEventData.contentModifiedAt,
        )
        tickingClock.tick()

        val employeeReservationForm =
            CalendarEventTimeEmployeeReservationForm(
                calendarEventTimeId = eventTimeId,
                childId = testChild_1.id,
            )

        setCalendarEventTimeReservationAsEmployee(
            clock = tickingClock,
            form = employeeReservationForm,
        )
        val employeeReservedEventData = readCalendarEvent(newEventData.id)

        assertEquals(
            timeAddedEventData.contentModifiedAt,
            employeeReservedEventData.contentModifiedAt,
        )
        tickingClock.tick()

        deleteEventTimeAsEmployee(clock = tickingClock, eventTimeId = eventTimeId)
        val deletedTimeEventData = readCalendarEvent(newEventData.id)

        assertThat(
            employeeReservedEventData.contentModifiedAt < deletedTimeEventData.contentModifiedAt
        )
    }

    @Test
    fun `group discussion surveys are returned`() {
        val daycareEventForm =
            CalendarEventForm(
                unitId = testDaycare.id,
                tree = mapOf(groupId to null),
                title = "This SHOULD NOT be returned",
                description = "Red herring event that should not be returned",
                period = FiniteDateRange(today.plusDays(3), today.plusDays(4)),
                eventType = CalendarEventType.DAYCARE_EVENT,
            )
        this.calendarEventController.createCalendarEvent(
            dbInstance(),
            admin,
            clock,
            daycareEventForm,
        )

        val groupDiscussionSurveyForm =
            CalendarEventForm(
                unitId = testDaycare.id,
                tree = mapOf(groupId to null),
                title = "This SHOULD BE returned",
                description = "Discussion survey",
                period = FiniteDateRange(today.plusDays(1), today.plusDays(1)),
                eventType = CalendarEventType.DISCUSSION_SURVEY,
                times =
                    listOf(
                        CalendarEventTimeForm(
                            date = today.plusDays(1),
                            timeRange = TimeRange(LocalTime.of(8, 0), LocalTime.of(9, 0)),
                        )
                    ),
            )
        val id =
            this.calendarEventController.createCalendarEvent(
                dbInstance(),
                admin,
                clock,
                groupDiscussionSurveyForm,
            )
        val groupDiscussionSurvey = this.readCalendarEvent(id)

        val otherGroupDiscussionSurveyForm =
            CalendarEventForm(
                unitId = testDaycare.id,
                tree = mapOf(groupId2 to null),
                title = "This SHOULD NOT BE returned",
                description = "Discussion survey for another group",
                period = FiniteDateRange(today.plusDays(1), today.plusDays(1)),
                eventType = CalendarEventType.DISCUSSION_SURVEY,
                times =
                    listOf(
                        CalendarEventTimeForm(
                            today.plusDays(1),
                            timeRange = TimeRange(LocalTime.of(8, 0), LocalTime.of(9, 0)),
                        )
                    ),
            )
        this.calendarEventController.createCalendarEvent(
            dbInstance(),
            admin,
            clock,
            otherGroupDiscussionSurveyForm,
        )

        val returnedSurveys = getGroupDiscussionSurveys(unitId = testDaycare.id, groupId = groupId)

        assertEquals(listOf(groupDiscussionSurvey), returnedSurveys)
    }

    @Test
    fun `group discussion survey reservation days contain correct events and day information`() {

        val daycareEventForm =
            CalendarEventForm(
                unitId = testDaycare.id,
                tree = mapOf(groupId to null),
                title = "Background event",
                description = "Daycare event in the background",
                period = FiniteDateRange(today.plusDays(3), today.plusDays(4)),
                eventType = CalendarEventType.DAYCARE_EVENT,
            )
        val dcEventId =
            this.calendarEventController.createCalendarEvent(
                dbInstance(),
                admin,
                clock,
                daycareEventForm,
            )

        val dcEvent = readCalendarEvent(dcEventId)

        val groupDiscussionSurveyForm =
            CalendarEventForm(
                unitId = testDaycare.id,
                tree = mapOf(groupId to null),
                title = "Background survey",
                description = "Discussion survey in the background",
                period = FiniteDateRange(today.plusDays(2), today.plusDays(4)),
                eventType = CalendarEventType.DISCUSSION_SURVEY,
                times =
                    listOf(
                        CalendarEventTimeForm(
                            date = today.plusDays(2),
                            timeRange = TimeRange(LocalTime.of(8, 0), LocalTime.of(9, 0)),
                        ),
                        CalendarEventTimeForm(
                            date = today.plusDays(4),
                            timeRange = TimeRange(LocalTime.of(14, 0), LocalTime.of(15, 0)),
                        ),
                    ),
            )
        val discussionSurveyId =
            this.calendarEventController.createCalendarEvent(
                dbInstance(),
                admin,
                clock,
                groupDiscussionSurveyForm,
            )

        val discussionSurvey = readCalendarEvent(discussionSurveyId)

        val reservationDays =
            getDiscussionSurveyReservationDays(
                unitId = testDaycare.id,
                groupId = groupId,
                start = today.plusDays(2),
                end = today.plusDays(4),
            )

        assertThat(reservationDays)
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .isEqualTo(
                listOf(
                    DiscussionReservationDay(
                        date = today.plusDays(2),
                        events = setOf(discussionSurvey),
                        isHoliday = false,
                        isOperationalDay = true,
                    ),
                    DiscussionReservationDay(
                        date = today.plusDays(3),
                        events = setOf(dcEvent),
                        isHoliday = true,
                        isOperationalDay = true,
                    ),
                    DiscussionReservationDay(
                        date = today.plusDays(4),
                        events = setOf(discussionSurvey, dcEvent),
                        isHoliday = false,
                        isOperationalDay = true,
                    ),
                )
            )
    }

    @Test
    fun `guardian and foster parent receive email for citizen reserving and cancelling discussion times`() {
        db.transaction { tx ->
            tx.insertGuardian(testAdult_2.id, testChild_3.id)
            tx.insert(
                DevFosterParent(
                    childId = testChild_3.id,
                    parentId = testAdult_3.id,
                    validDuring = DateRange(today.minusYears(10), today.plusYears(10)),
                    modifiedAt = now,
                    modifiedBy = admin.evakaUserId,
                )
            )
            val placementId3 =
                tx.insert(
                    DevPlacement(
                        childId = testChild_3.id,
                        unitId = testDaycare.id,
                        startDate = placementStart,
                        endDate = placementEnd,
                    )
                )
            tx.insert(
                DevDaycareGroupPlacement(
                    daycarePlacementId = placementId3,
                    daycareGroupId = groupId,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )
        }
        val expectedRecipients = listOf(testAdult_2, testAdult_3)
        val guardian2 = AuthenticatedUser.Citizen(testAdult_2.id, CitizenAuthLevel.STRONG)
        val event =
            createCalendarEvent(
                CalendarEventForm(
                    unitId = testDaycare.id,
                    tree = mapOf(groupId to setOf(testChild_3.id)),
                    title = "Child-specific event",
                    description = "cse",
                    period = FiniteDateRange(today.plusDays(3), today.plusDays(3)),
                    times =
                        listOf(
                            CalendarEventTimeForm(
                                date = today.plusDays(3),
                                timeRange = TimeRange(LocalTime.of(8, 20), LocalTime.of(8, 40)),
                            )
                        ),
                    eventType = CalendarEventType.DISCUSSION_SURVEY,
                )
            )
        val calendarEventTimeForm = event.times.first()

        val reservationForm =
            CalendarEventTimeCitizenReservationForm(
                calendarEventTimeId = calendarEventTimeForm.id,
                childId = testChild_3.id,
            )
        calendarEventController.addCalendarEventTimeReservation(
            dbInstance(),
            guardian2,
            clock,
            reservationForm,
        )

        asyncJobRunner.runPendingJobsSync(RealEvakaClock())

        val emailDetails =
            DiscussionSurveyReservationNotificationData(
                calendarEventTime =
                    CalendarEventTime(
                        id = calendarEventTimeForm.id,
                        date = calendarEventTimeForm.date,
                        startTime = calendarEventTimeForm.startTime,
                        endTime = calendarEventTimeForm.endTime,
                        childId = reservationForm.childId,
                    )
            )

        val reservationEmailContent =
            emailMessageProvider.discussionSurveyReservationNotification(
                language = Language.fi,
                notificationDetails = emailDetails,
            )
        val expectedFromAddress = "${emailEnv.senderNameFi} <${emailEnv.senderAddress}>"
        assertEmails(expectedRecipients, reservationEmailContent, expectedFromAddress)

        MockEmailClient.clear()

        // adding same reservation again should not result in another mail
        calendarEventController.addCalendarEventTimeReservation(
            dbInstance(),
            guardian2,
            clock,
            reservationForm,
        )

        asyncJobRunner.runPendingJobsSync(RealEvakaClock())
        assertEquals(emptySet(), MockEmailClient.emails.map { it.toAddress }.toSet())

        // cancelling reservation should lead to a message
        calendarEventController.deleteCalendarEventTimeReservation(
            dbInstance(),
            guardian2,
            clock,
            reservationForm.calendarEventTimeId,
            reservationForm.childId,
        )

        asyncJobRunner.runPendingJobsSync(RealEvakaClock())
        assertEquals(
            expectedRecipients.mapNotNull { it.email }.toSet(),
            MockEmailClient.emails.map { it.toAddress }.toSet(),
        )

        val cancellationEmailContent =
            emailMessageProvider.discussionSurveyReservationCancellationNotification(
                language = Language.fi,
                notificationDetails = emailDetails,
            )

        assertEquals(
            expectedRecipients.mapNotNull { it.email }.toSet(),
            MockEmailClient.emails.map { it.toAddress }.toSet(),
        )
        assertEquals(cancellationEmailContent.subject, getEmailFor(testAdult_2).content.subject)
        assertEquals(
            "${emailEnv.senderNameFi} <${emailEnv.senderAddress}>",
            getEmailFor(testAdult_2).fromAddress,
        )
    }

    @Test
    fun `guardian and foster parent receive email for employee reserving and cancelling discussion times`() {
        db.transaction { tx ->
            tx.insertGuardian(testAdult_2.id, testChild_3.id)
            tx.insert(
                DevFosterParent(
                    childId = testChild_3.id,
                    parentId = testAdult_3.id,
                    validDuring = DateRange(today.minusYears(10), today.plusYears(10)),
                    modifiedAt = now,
                    modifiedBy = admin.evakaUserId,
                )
            )
            val placementId3 =
                tx.insert(
                    DevPlacement(
                        childId = testChild_3.id,
                        unitId = testDaycare.id,
                        startDate = placementStart,
                        endDate = placementEnd,
                    )
                )
            tx.insert(
                DevDaycareGroupPlacement(
                    daycarePlacementId = placementId3,
                    daycareGroupId = groupId,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )
        }
        val expectedRecipients = listOf(testAdult_2, testAdult_3)
        val event =
            createCalendarEvent(
                CalendarEventForm(
                    unitId = testDaycare.id,
                    tree = mapOf(groupId to setOf(testChild_3.id)),
                    title = "Child-specific event",
                    description = "cse",
                    period = FiniteDateRange(today.plusDays(3), today.plusDays(3)),
                    times =
                        listOf(
                            CalendarEventTimeForm(
                                date = today.plusDays(3),
                                timeRange = TimeRange(LocalTime.of(8, 20), LocalTime.of(8, 40)),
                            )
                        ),
                    eventType = CalendarEventType.DISCUSSION_SURVEY,
                )
            )
        val calendarEventTimeForm = event.times.first()

        val reservationForm =
            CalendarEventTimeEmployeeReservationForm(
                calendarEventTimeId = calendarEventTimeForm.id,
                childId = testChild_3.id,
            )
        calendarEventController.setCalendarEventTimeReservation(
            dbInstance(),
            admin,
            clock,
            reservationForm,
        )

        asyncJobRunner.runPendingJobsSync(RealEvakaClock())

        val emailDetails =
            DiscussionSurveyReservationNotificationData(
                calendarEventTime =
                    CalendarEventTime(
                        id = calendarEventTimeForm.id,
                        date = calendarEventTimeForm.date,
                        startTime = calendarEventTimeForm.startTime,
                        endTime = calendarEventTimeForm.endTime,
                        childId = reservationForm.childId,
                    )
            )

        val reservationEmailContent =
            emailMessageProvider.discussionSurveyReservationNotification(
                language = Language.fi,
                notificationDetails = emailDetails,
            )

        val expectedFromAddress = "${emailEnv.senderNameFi} <${emailEnv.senderAddress}>"
        assertEmails(expectedRecipients, reservationEmailContent, expectedFromAddress)

        MockEmailClient.clear()

        // adding same reservation again should not result in another mail
        calendarEventController.setCalendarEventTimeReservation(
            dbInstance(),
            admin,
            clock,
            reservationForm,
        )

        asyncJobRunner.runPendingJobsSync(RealEvakaClock())
        assertEmails(emptyList(), reservationEmailContent, expectedFromAddress)

        // cancelling reservation should lead to a message
        calendarEventController.setCalendarEventTimeReservation(
            dbInstance(),
            admin,
            clock,
            reservationForm.copy(childId = null),
        )

        asyncJobRunner.runPendingJobsSync(RealEvakaClock())

        val cancellationEmailContent =
            emailMessageProvider.discussionSurveyReservationCancellationNotification(
                language = Language.fi,
                notificationDetails = emailDetails,
            )

        assertEmails(expectedRecipients, cancellationEmailContent, expectedFromAddress)
    }

    @Test
    fun `guardian and foster parent receive cancellation email for employee deleting discussion times`() {
        db.transaction { tx ->
            tx.insertGuardian(testAdult_2.id, testChild_3.id)
            tx.insert(
                DevFosterParent(
                    childId = testChild_3.id,
                    parentId = testAdult_3.id,
                    validDuring = DateRange(today.minusYears(10), today.plusYears(10)),
                    modifiedAt = now,
                    modifiedBy = admin.evakaUserId,
                )
            )
            val placementId3 =
                tx.insert(
                    DevPlacement(
                        childId = testChild_3.id,
                        unitId = testDaycare.id,
                        startDate = placementStart,
                        endDate = placementEnd,
                    )
                )
            tx.insert(
                DevDaycareGroupPlacement(
                    daycarePlacementId = placementId3,
                    daycareGroupId = groupId,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )
        }
        val expectedRecipients = listOf(testAdult_2, testAdult_3)
        val event =
            createCalendarEvent(
                CalendarEventForm(
                    unitId = testDaycare.id,
                    tree = mapOf(groupId to setOf(testChild_3.id)),
                    title = "Child-specific event",
                    description = "cse",
                    period = FiniteDateRange(today.plusDays(3), today.plusDays(3)),
                    times =
                        listOf(
                            CalendarEventTimeForm(
                                date = today.plusDays(3),
                                timeRange = TimeRange(LocalTime.of(8, 20), LocalTime.of(8, 40)),
                            )
                        ),
                    eventType = CalendarEventType.DISCUSSION_SURVEY,
                )
            )
        val calendarEventTimeForm = event.times.first()

        val reservationForm =
            CalendarEventTimeEmployeeReservationForm(
                calendarEventTimeId = calendarEventTimeForm.id,
                childId = testChild_3.id,
            )
        calendarEventController.setCalendarEventTimeReservation(
            dbInstance(),
            admin,
            clock,
            reservationForm,
        )

        // clear the reservation emails
        asyncJobRunner.runPendingJobsSync(RealEvakaClock())
        MockEmailClient.clear()

        val emailDetails =
            DiscussionSurveyReservationNotificationData(
                calendarEventTime =
                    CalendarEventTime(
                        id = calendarEventTimeForm.id,
                        date = calendarEventTimeForm.date,
                        startTime = calendarEventTimeForm.startTime,
                        endTime = calendarEventTimeForm.endTime,
                        childId = reservationForm.childId,
                    )
            )

        val cancellationEmailContent =
            emailMessageProvider.discussionSurveyReservationCancellationNotification(
                language = Language.fi,
                notificationDetails = emailDetails,
            )

        // deleting the reserved time should lead to a cancellation message
        calendarEventController.deleteCalendarEventTime(
            dbInstance(),
            admin,
            clock,
            calendarEventTimeForm.id,
        )

        asyncJobRunner.runPendingJobsSync(RealEvakaClock())

        val expectedFromAddress = "${emailEnv.senderNameFi} <${emailEnv.senderAddress}>"
        assertEmails(expectedRecipients, cancellationEmailContent, expectedFromAddress)
    }

    @Test
    fun `guardian and foster parent get event times relevant to their children`() {
        db.transaction { tx ->
            tx.insertGuardian(testAdult_2.id, testChild_3.id)
            tx.insert(
                DevFosterParent(
                    childId = testChild_4.id,
                    parentId = testAdult_3.id,
                    validDuring = DateRange(today.minusYears(10), today.plusYears(10)),
                    modifiedAt = now,
                    modifiedBy = admin.evakaUserId,
                )
            )
            val placementId4 =
                tx.insert(
                    DevPlacement(
                        childId = testChild_4.id,
                        unitId = testDaycare.id,
                        startDate = placementStart,
                        endDate = placementEnd,
                    )
                )
            tx.insert(
                DevDaycareGroupPlacement(
                    daycarePlacementId = placementId4,
                    daycareGroupId = groupId,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )

            val placementId3 =
                tx.insert(
                    DevPlacement(
                        childId = testChild_3.id,
                        unitId = testDaycare.id,
                        startDate = placementStart,
                        endDate = placementEnd,
                    )
                )
            tx.insert(
                DevDaycareGroupPlacement(
                    daycarePlacementId = placementId3,
                    daycareGroupId = groupId,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )
        }

        val guardianUser = AuthenticatedUser.Citizen(testAdult_2.id, CitizenAuthLevel.STRONG)
        val fosterParentUser = AuthenticatedUser.Citizen(testAdult_3.id, CitizenAuthLevel.STRONG)
        val event =
            createCalendarEvent(
                CalendarEventForm(
                    unitId = testDaycare.id,
                    tree = mapOf(groupId to setOf(testChild_3.id, testChild_4.id)),
                    title = "Child-specific event",
                    description = "cse",
                    period = FiniteDateRange(today.plusDays(3), today.plusDays(3)),
                    times =
                        listOf(
                            CalendarEventTimeForm(
                                date = today.plusDays(3),
                                timeRange = TimeRange(LocalTime.of(8, 20), LocalTime.of(8, 40)),
                            ),
                            CalendarEventTimeForm(
                                date = today.plusDays(3),
                                timeRange = TimeRange(LocalTime.of(9, 20), LocalTime.of(9, 40)),
                            ),
                            CalendarEventTimeForm(
                                date = today.plusDays(3),
                                timeRange = TimeRange(LocalTime.of(10, 20), LocalTime.of(10, 40)),
                            ),
                        ),
                    eventType = CalendarEventType.DISCUSSION_SURVEY,
                )
            )

        val child4EventTime = event.times.last()
        val child3EventTime = event.times.first()

        reserveEventTime(child4EventTime.id, testChild_4.id)
        reserveEventTime(child3EventTime.id, testChild_3.id)

        val updatedEvent =
            calendarEventController.getCalendarEvent(dbInstance(), admin, clock, event.id)

        val expectedGuardianEventTimes =
            updatedEvent.times
                .filter { it.id != child4EventTime.id }
                .map {
                    CitizenCalendarEventTime(
                        id = it.id,
                        date = it.date,
                        startTime = it.startTime,
                        endTime = it.endTime,
                        childId = it.childId,
                        isEditable = true,
                    )
                }
        val expectedFosterParentEventTimes =
            updatedEvent.times
                .filter { it.id != child3EventTime.id }
                .map {
                    CitizenCalendarEventTime(
                        id = it.id,
                        date = it.date,
                        startTime = it.startTime,
                        endTime = it.endTime,
                        childId = it.childId,
                        isEditable = true,
                    )
                }

        val guardianEventResult =
            calendarEventController.getCitizenCalendarEvents(
                dbInstance(),
                guardianUser,
                clock,
                placementStart,
                placementEnd,
            )

        assertEquals(1, guardianEventResult.size)
        assertThat(guardianEventResult[0].timesByChild[testChild_3.id])
            .containsExactlyInAnyOrderElementsOf(expectedGuardianEventTimes)

        val fosterParentEventResult =
            calendarEventController.getCitizenCalendarEvents(
                dbInstance(),
                fosterParentUser,
                clock,
                placementStart,
                placementEnd,
            )

        assertEquals(1, fosterParentEventResult.size)
        assertThat(fosterParentEventResult[0].timesByChild[testChild_4.id])
            .containsExactlyInAnyOrderElementsOf(expectedFosterParentEventTimes)
    }

    private fun reserveEventTime(id: CalendarEventTimeId, childId: ChildId) {
        calendarEventController.setCalendarEventTimeReservation(
            dbInstance(),
            admin,
            clock,
            CalendarEventTimeEmployeeReservationForm(calendarEventTimeId = id, childId = childId),
        )
    }

    private fun getDiscussionSurveyReservationDays(
        user: AuthenticatedUser.Employee = admin,
        clock: EvakaClock = this.clock,
        unitId: DaycareId,
        groupId: GroupId,
        start: LocalDate,
        end: LocalDate,
    ): Set<DiscussionReservationDay> =
        calendarEventController.getGroupDiscussionReservationDays(
            dbInstance(),
            user,
            clock,
            unitId,
            groupId,
            start,
            end,
        )

    private fun getGroupDiscussionSurveys(
        user: AuthenticatedUser.Employee = admin,
        clock: EvakaClock = this.clock,
        unitId: DaycareId,
        groupId: GroupId,
    ) =
        calendarEventController.getGroupDiscussionSurveys(
            dbInstance(),
            user,
            clock,
            unitId,
            groupId,
        )

    private fun createCalendarEvent(
        form: CalendarEventForm,
        user: AuthenticatedUser.Employee = admin,
        clock: EvakaClock = this.clock,
    ): CalendarEvent {
        val id = calendarEventController.createCalendarEvent(dbInstance(), user, clock, form)
        return readCalendarEvent(id, user = user, clock = clock)
    }

    private fun readCalendarEvent(
        id: CalendarEventId,
        user: AuthenticatedUser.Employee = admin,
        clock: EvakaClock = this.clock,
    ) = calendarEventController.getCalendarEvent(dbInstance(), user, clock, id)

    private fun updateCalendarEvent(
        id: CalendarEventId,
        form: CalendarEventUpdateForm,
        user: AuthenticatedUser.Employee = admin,
        clock: EvakaClock = this.clock,
    ): CalendarEvent {
        calendarEventController.updateCalendarEvent(dbInstance(), user, clock, id, form)
        return readCalendarEvent(id, user = user, clock = clock)
    }

    private fun modifyCalendarEvent(
        id: CalendarEventId,
        form: CalendarEventUpdateForm,
        user: AuthenticatedUser.Employee = admin,
        clock: EvakaClock = this.clock,
    ): CalendarEvent {
        calendarEventController.modifyCalendarEvent(dbInstance(), user, clock, id, form)
        return readCalendarEvent(id, user = user, clock = clock)
    }

    private fun deleteCalendarEvent(
        id: CalendarEventId,
        user: AuthenticatedUser.Employee = admin,
        clock: EvakaClock = this.clock,
    ) {
        calendarEventController.deleteCalendarEvent(dbInstance(), user, clock, id)
        assertThrows<NotFound> { readCalendarEvent(id, user = admin, clock = clock) }
        assertThrows<NotFound> {
            calendarEventController.deleteCalendarEvent(dbInstance(), user, clock, id)
        }
    }

    private fun getReservableCalendarEventTimes(
        calendarEventId: CalendarEventId,
        childId: ChildId,
        user: AuthenticatedUser.Citizen = guardian,
        clock: EvakaClock = this.clock,
    ) =
        calendarEventController.getReservableCalendarEventTimes(
            dbInstance(),
            user,
            clock,
            calendarEventId,
            childId,
        )

    private fun addEventTimeAsEmployee(
        form: CalendarEventTimeForm,
        eventId: CalendarEventId,
        user: AuthenticatedUser.Employee = admin,
        clock: EvakaClock = this.clock,
    ): CalendarEventTimeId =
        calendarEventController.addCalendarEventTime(dbInstance(), user, clock, eventId, form)

    private fun deleteEventTimeAsEmployee(
        user: AuthenticatedUser.Employee = admin,
        clock: EvakaClock = this.clock,
        eventTimeId: CalendarEventTimeId,
    ) = calendarEventController.deleteCalendarEventTime(dbInstance(), user, clock, eventTimeId)

    private fun addEventTimeReservationAsCitizen(
        form: CalendarEventTimeCitizenReservationForm,
        user: AuthenticatedUser.Citizen = guardian,
        clock: EvakaClock = this.clock,
    ) = calendarEventController.addCalendarEventTimeReservation(dbInstance(), user, clock, form)

    private fun deleteEventTimeReservationAsCitizen(
        user: AuthenticatedUser.Citizen = guardian,
        clock: EvakaClock = this.clock,
        eventTimeId: CalendarEventTimeId,
        childId: ChildId,
    ) =
        calendarEventController.deleteCalendarEventTimeReservation(
            dbInstance(),
            user,
            clock,
            eventTimeId,
            childId,
        )

    private fun setCalendarEventTimeReservationAsEmployee(
        user: AuthenticatedUser.Employee = admin,
        clock: EvakaClock = this.clock,
        form: CalendarEventTimeEmployeeReservationForm,
    ) = calendarEventController.setCalendarEventTimeReservation(dbInstance(), user, clock, form)
}

private fun getEmailFor(person: DevPerson): Email {
    val address = person.email ?: throw Error("$person has no email")
    return MockEmailClient.getEmail(address) ?: throw Error("No emails sent to $address")
}

private fun assertAllEmailsFor(
    recipient: DevPerson,
    expectedContents: List<EmailContent>,
    expectedFromAddress: String,
) {
    val recipientEmails = MockEmailClient.emails.filter { e -> e.toAddress == recipient.email }
    recipientEmails.forEach { e -> assertEquals(expectedFromAddress, e.fromAddress) }
    assertThat(recipientEmails.map { e -> e.content })
        .containsExactlyInAnyOrderElementsOf(expectedContents)
}

private fun assertEmails(
    recipients: List<DevPerson>,
    expectedContent: EmailContent,
    expectedFromAddress: String,
) {
    assertEquals(
        recipients.mapNotNull { it.email }.toSet(),
        MockEmailClient.emails.map { it.toAddress }.toSet(),
    )
    recipients.forEach {
        val mail = getEmailFor(it)
        assertEquals(expectedContent.subject, mail.content.subject)
        assertEquals(expectedFromAddress, mail.fromAddress)
    }
}
