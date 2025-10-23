// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.nekku

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.calendarevent.CalendarEventType
import fi.espoo.evaka.serviceneed.ShiftCareType
import fi.espoo.evaka.shared.dev.DevCalendarEvent
import fi.espoo.evaka.shared.dev.DevCalendarEventAttendee
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.DevDaycareGroupPlacement
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.DevServiceNeed
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.dev.insertServiceNeedOptions
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.TimeRange
import fi.espoo.evaka.shared.domain.toFiniteDateRange
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.springframework.beans.factory.annotation.Autowired

class NekkuQueryIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {

    @Autowired private lateinit var nekkuController: NekkuController

    @Test
    fun `Meal types should contain the null value`() {
        val mealTypes =
            nekkuController.getNekkuMealTypes(
                dbInstance(),
                getAuthenticatedEmployee(db),
                getClock(),
            )

        assert(mealTypes.map { it.type }.contains(null))
    }

    @Test
    fun `Meal types should contain all NekkuProductMealTime values`() {
        val mealTypes =
            nekkuController.getNekkuMealTypes(
                dbInstance(),
                getAuthenticatedEmployee(db),
                getClock(),
            )

        NekkuProductMealType.entries.forEach { entry ->
            assert(mealTypes.map { it.type }.contains(entry))
        }
    }

    @Test
    fun `Fetching customer numbers should work`() {
        insertCustomerNumbers(db)

        val customerNumbers =
            nekkuController.getNekkuUnitNumbers(
                dbInstance(),
                getAuthenticatedEmployee(db),
                getClock(),
            )
        assertEquals(2, customerNumbers.size)
        assertEquals(
            listOf(
                NekkuUnitNumber("1234", "Lönnrotinkadun päiväkoti"),
                NekkuUnitNumber("5678", "Rubeberginkadun päiväkoti"),
            ),
            customerNumbers,
        )
    }

    @Test
    fun `Fetching special diets should work`() {
        insertSpecialDiets(db)

        val specialDiets =
            nekkuController.getNekkuSpecialDiets(
                dbInstance(),
                getAuthenticatedEmployee(db),
                getClock(),
            )
        assertEquals(1, specialDiets.size)
        assertEquals(listOf(NekkuSpecialDietWithoutFields("2", "Päiväkodit ER")), specialDiets)
    }

    @Test
    fun `Fetching special diet fields should work`() {
        insertSpecialDiets(db)

        val specialDietFields =
            nekkuController.getNekkuSpecialDietFields(
                dbInstance(),
                getAuthenticatedEmployee(db),
                getClock(),
            )
        assertEquals(2, specialDietFields.size)
        assertEquals(
            listOf(
                NekkuSpecialDietsFieldWithoutOptions(
                    "12345678-9ABC-DEF0-1234-56789ABCDEF0",
                    "Erityisruokavaliot",
                    NekkuSpecialDietType.CHECKBOXLIST,
                    "2",
                ),
                NekkuSpecialDietsFieldWithoutOptions(
                    "56789ABC-DEF0-1234-5678-9ABCDEF01234",
                    "Muu erityisruokavalio",
                    NekkuSpecialDietType.TEXT,
                    "2",
                ),
            ),
            specialDietFields,
        )
    }

    @Test
    fun `Fetching special diet options should work`() {
        insertSpecialDiets(db)

        val specialDietOptions =
            nekkuController.getNekkuSpecialDietOptions(
                dbInstance(),
                getAuthenticatedEmployee(db),
                getClock(),
            )
        assertEquals(2, specialDietOptions.size)
        assertEquals(
            listOf(
                NekkuSpecialDietOptionWithFieldId(
                    1,
                    "Kananmunaton",
                    "Kananmunaton",
                    "12345678-9ABC-DEF0-1234-56789ABCDEF0",
                ),
                NekkuSpecialDietOptionWithFieldId(
                    2,
                    "Sianlihaton",
                    "Sianlihaton",
                    "12345678-9ABC-DEF0-1234-56789ABCDEF0",
                ),
            ),
            specialDietOptions,
        )
    }

    @Test
    fun `Fetching the order reduction percentage by group should work`() {

        val area = DevCareArea()
        val daycare1 = DevDaycare(areaId = area.id, nekkuOrderReductionPercentage = 10)
        val dc1group1 = DevDaycareGroup(daycareId = daycare1.id)
        val dc1group2 = DevDaycareGroup(daycareId = daycare1.id)
        val daycare2 = DevDaycare(areaId = area.id, nekkuOrderReductionPercentage = 20)
        val dc2group = DevDaycareGroup(daycareId = daycare2.id)
        val daycare3 = DevDaycare(areaId = area.id, nekkuOrderReductionPercentage = 15)
        val dc3group = DevDaycareGroup(daycareId = daycare3.id)

        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare1)
            tx.insert(daycare2)
            tx.insert(daycare3)
            tx.insert(dc1group1)
            tx.insert(dc1group2)
            tx.insert(dc2group)
            tx.insert(dc3group)

            assertEquals(10, tx.getNekkuOrderReductionForDaycareByGroup(dc1group1.id))
            assertEquals(10, tx.getNekkuOrderReductionForDaycareByGroup(dc1group2.id))
            assertEquals(20, tx.getNekkuOrderReductionForDaycareByGroup(dc2group.id))
        }
    }

    @Test
    fun `fetching open daycare group IDs should work`() {

        insertCustomerNumbers(db)

        val area = DevCareArea()
        val daycare1 =
            DevDaycare(
                areaId = area.id,
                openingDate = LocalDate.of(2025, 2, 1),
                closingDate = LocalDate.of(2025, 12, 23),
            )
        val daycare2 =
            DevDaycare(
                areaId = area.id,
                openingDate = LocalDate.of(2025, 6, 1),
                closingDate = LocalDate.of(2025, 8, 31),
            )
        val group1 =
            DevDaycareGroup(
                daycareId = daycare1.id,
                startDate = LocalDate.of(2025, 3, 1),
                endDate = LocalDate.of(2025, 12, 31),
                nekkuCustomerNumber = "1234",
            )
        val group2 =
            DevDaycareGroup(
                daycareId = daycare2.id,
                startDate = LocalDate.of(2025, 5, 1),
                endDate = LocalDate.of(2025, 7, 30),
                nekkuCustomerNumber = "5678",
            )

        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare1)
            tx.insert(daycare2)
            tx.insert(group1)
            tx.insert(group2)

            val result = tx.getNekkuOpenDaycareGroupDates(LocalDate.of(2025, 6, 15))

            assertEquals(
                listOf(
                        GroupDates(group1.id, group1.startDate, daycare1.closingDate),
                        GroupDates(group2.id, daycare2.openingDate, group2.endDate),
                    )
                    .sortedBy { it.id },
                result.sortedBy { it.id },
            )
        }
    }

    @Test
    fun `fetching groups opening during the next week should work`() {

        insertCustomerNumbers(db)

        val today = LocalDate.of(2025, 5, 5)

        val area = DevCareArea()
        val daycare = DevDaycare(areaId = area.id)
        val group1 =
            DevDaycareGroup(
                daycareId = daycare.id,
                startDate = today.minusMonths(1),
                nekkuCustomerNumber = "1234",
            )
        val group2 =
            DevDaycareGroup(
                daycareId = daycare.id,
                startDate = today.plusDays(2),
                nekkuCustomerNumber = "5678",
            )
        val group3 =
            DevDaycareGroup(
                daycareId = daycare.id,
                startDate = today.plusDays(6),
                nekkuCustomerNumber = "5678",
            )
        val group4 =
            DevDaycareGroup(
                daycareId = daycare.id,
                startDate = today.plusWeeks(2),
                nekkuCustomerNumber = "5678",
            )

        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(group1)
            tx.insert(group2)
            tx.insert(group3)
            tx.insert(group4)

            val result = tx.findNekkuGroupsOpeningInNextWeek(today)

            assertEquals(
                listOf(group2.id, group3.id).sortedBy { it },
                result.sortedBy { it.id }.map { it.id },
            )
        }
    }

    @Test
    fun `fetching group operating days should work`() {

        val area = DevCareArea()

        val daycare =
            DevDaycare(
                areaId = area.id,
                operationTimes =
                    listOf(
                        TimeRange(LocalTime.parse("00:00"), LocalTime.parse("23:59")),
                        TimeRange(LocalTime.parse("00:00"), LocalTime.parse("23:59")),
                        TimeRange(LocalTime.parse("00:00"), LocalTime.parse("23:59")),
                        TimeRange(LocalTime.parse("00:00"), LocalTime.parse("23:59")),
                        TimeRange(LocalTime.parse("00:00"), LocalTime.parse("23:59")),
                        TimeRange(LocalTime.parse("00:00"), LocalTime.parse("23:59")),
                        TimeRange(LocalTime.parse("00:00"), LocalTime.parse("23:59")),
                    ),
                shiftCareOperationTimes =
                    listOf(
                        TimeRange(LocalTime.parse("00:00"), LocalTime.parse("23:59")),
                        TimeRange(LocalTime.parse("00:00"), LocalTime.parse("23:59")),
                        TimeRange(LocalTime.parse("00:00"), LocalTime.parse("23:59")),
                        TimeRange(LocalTime.parse("00:00"), LocalTime.parse("23:59")),
                        TimeRange(LocalTime.parse("00:00"), LocalTime.parse("23:59")),
                        TimeRange(LocalTime.parse("00:00"), LocalTime.parse("23:59")),
                        TimeRange(LocalTime.parse("00:00"), LocalTime.parse("23:59")),
                    ),
                shiftCareOpenOnHolidays = true,
                nekkuNoWeekendMealOrders = true,
            )

        val group = DevDaycareGroup(daycareId = daycare.id)

        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(group)

            val result = tx.getGroupOperationDays(group.id)
            assertNotNull(result)

            assertEquals(7, result.combinedDays.size)
            assertEquals(true, result.noWeekendMealOrders)
            assertEquals(true, result.shiftCareOpenOnHolidays)
        }
    }

    @Test
    fun `checking for shift care children in group should work correctly`() {

        val placementStartDay = LocalDate.of(2025, 5, 12)
        val placementEndDay = LocalDate.of(2025, 5, 18)

        val area = DevCareArea()
        val daycare = DevDaycare(areaId = area.id)
        val group = DevDaycareGroup(daycareId = daycare.id)
        val employee = DevEmployee()

        val noShiftCareChild1 = DevPerson()
        val noShiftCareChild2 = DevPerson()
        val shiftCareBeforeCheckedDayChild = DevPerson()

        db.transaction { tx -> tx.insertServiceNeedOptions() }

        val serviceNeedOptionId = getServiceNeedOptionId(db, "Kokopäiväinen")

        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(group)
            tx.insert(employee)

            listOf(noShiftCareChild1, noShiftCareChild2).map {
                tx.insert(it, DevPersonType.CHILD)
                tx.insert(
                        DevPlacement(
                            childId = it.id,
                            unitId = daycare.id,
                            startDate = placementStartDay,
                            endDate = placementEndDay,
                        )
                    )
                    .also { placementId ->
                        tx.insert(
                            DevDaycareGroupPlacement(
                                daycarePlacementId = placementId,
                                daycareGroupId = group.id,
                                startDate = placementStartDay,
                                endDate = placementEndDay,
                            )
                        )
                        tx.insert(
                            DevServiceNeed(
                                placementId = placementId,
                                startDate = placementStartDay,
                                endDate = placementEndDay,
                                confirmedBy = employee.evakaUserId,
                                optionId = serviceNeedOptionId,
                                shiftCare = ShiftCareType.NONE,
                            )
                        )
                    }
            }

            tx.insert(shiftCareBeforeCheckedDayChild, DevPersonType.CHILD)
            tx.insert(
                    DevPlacement(
                        childId = shiftCareBeforeCheckedDayChild.id,
                        unitId = daycare.id,
                        startDate = placementStartDay.minusWeeks(2),
                        endDate = placementEndDay.minusWeeks(2),
                    )
                )
                .also { placementId ->
                    tx.insert(
                        DevDaycareGroupPlacement(
                            daycarePlacementId = placementId,
                            daycareGroupId = group.id,
                            startDate = placementStartDay.minusWeeks(2),
                            endDate = placementEndDay.minusWeeks(2),
                        )
                    )
                    tx.insert(
                        DevServiceNeed(
                            placementId = placementId,
                            startDate = placementStartDay.minusWeeks(2),
                            endDate = placementEndDay.minusWeeks(2),
                            confirmedBy = employee.evakaUserId,
                            optionId = serviceNeedOptionId,
                            shiftCare = ShiftCareType.FULL,
                        )
                    )
                }

            assertEquals(
                false,
                tx.groupHasShiftCareChildren(group.id, placementStartDay.plusDays(3)),
            )
        }

        val shiftCareOnCheckedDayChild = DevPerson()

        db.transaction { tx ->
            tx.insert(shiftCareOnCheckedDayChild, DevPersonType.CHILD)
            tx.insert(
                    DevPlacement(
                        childId = shiftCareOnCheckedDayChild.id,
                        unitId = daycare.id,
                        startDate = placementStartDay,
                        endDate = placementEndDay,
                    )
                )
                .also { placementId ->
                    tx.insert(
                        DevDaycareGroupPlacement(
                            daycarePlacementId = placementId,
                            daycareGroupId = group.id,
                            startDate = placementStartDay,
                            endDate = placementEndDay,
                        )
                    )
                    tx.insert(
                        DevServiceNeed(
                            placementId = placementId,
                            startDate = placementStartDay,
                            endDate = placementEndDay,
                            confirmedBy = employee.evakaUserId,
                            optionId = serviceNeedOptionId,
                            shiftCare = ShiftCareType.FULL,
                        )
                    )
                }

            assertEquals(
                true,
                tx.groupHasShiftCareChildren(group.id, placementStartDay.plusDays(3)),
            )
        }
    }

    @Test
    fun `finding calendar event meal reductions for group should work`() {
        val area = DevCareArea()
        val daycare = DevDaycare(areaId = area.id)
        val group = DevDaycareGroup(daycareId = daycare.id)
        val employee = DevEmployee()
        val event =
            DevCalendarEvent(
                title = "Testitapaus",
                description = "Testausta",
                period = LocalDate.of(2025, 5, 14).toFiniteDateRange(),
                modifiedAt =
                    HelsinkiDateTime.of(LocalDate.of(2025, 5, 2), LocalTime.of(12, 34, 56)),
                modifiedBy = employee.evakaUserId,
                eventType = CalendarEventType.DAYCARE_EVENT,
                nekkuUnorderedMeals = listOf(NekkuProductMealTime.LUNCH, NekkuProductMealTime.SNACK),
            )
        val eventAttendee =
            DevCalendarEventAttendee(
                calendarEventId = event.id,
                unitId = daycare.id,
                groupId = null,
                childId = null,
            )

        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(group)
            tx.insert(employee)
            tx.insert(event)
            tx.insert(eventAttendee)

            val result = tx.getGroupWideEventOrderReductions(group.id, LocalDate.of(2025, 5, 14))
            assertEquals(setOf(NekkuProductMealTime.LUNCH, NekkuProductMealTime.SNACK), result)
        }
    }

    @Test
    fun `finding calendar event meal reductions for children should work`() {
        val area = DevCareArea()
        val daycare = DevDaycare(areaId = area.id)
        val group = DevDaycareGroup(daycareId = daycare.id)
        val child1 = DevPerson()
        val child2 = DevPerson()
        val employee = DevEmployee()
        val event1 =
            DevCalendarEvent(
                title = "Testitapaus",
                description = "Testausta",
                period = LocalDate.of(2025, 5, 14).toFiniteDateRange(),
                modifiedAt =
                    HelsinkiDateTime.of(LocalDate.of(2025, 5, 2), LocalTime.of(12, 34, 56)),
                modifiedBy = employee.evakaUserId,
                eventType = CalendarEventType.DAYCARE_EVENT,
                nekkuUnorderedMeals = listOf(NekkuProductMealTime.LUNCH, NekkuProductMealTime.SNACK),
            )
        val event2 =
            DevCalendarEvent(
                title = " Muu tapaus",
                description = "Testausta",
                period = LocalDate.of(2025, 5, 14).toFiniteDateRange(),
                modifiedAt =
                    HelsinkiDateTime.of(LocalDate.of(2025, 5, 2), LocalTime.of(12, 34, 56)),
                modifiedBy = employee.evakaUserId,
                eventType = CalendarEventType.DAYCARE_EVENT,
                nekkuUnorderedMeals = listOf(NekkuProductMealTime.LUNCH),
            )
        val event3 =
            DevCalendarEvent(
                title = "Outo tapaus",
                description = "Testausta",
                period = LocalDate.of(2025, 5, 14).toFiniteDateRange(),
                modifiedAt =
                    HelsinkiDateTime.of(LocalDate.of(2025, 5, 2), LocalTime.of(12, 34, 56)),
                modifiedBy = employee.evakaUserId,
                eventType = CalendarEventType.DAYCARE_EVENT,
                nekkuUnorderedMeals = listOf(NekkuProductMealTime.BREAKFAST),
            )
        val event4 =
            DevCalendarEvent(
                title = "Aiempi tapaus",
                description = "Tätä ei mukaan vähennyksiin",
                period = LocalDate.of(2025, 5, 12).toFiniteDateRange(),
                modifiedAt =
                    HelsinkiDateTime.of(LocalDate.of(2025, 5, 2), LocalTime.of(12, 34, 56)),
                modifiedBy = employee.evakaUserId,
                eventType = CalendarEventType.DAYCARE_EVENT,
                nekkuUnorderedMeals = listOf(NekkuProductMealTime.BREAKFAST),
            )

        val eventAttendee1 =
            DevCalendarEventAttendee(
                calendarEventId = event1.id,
                unitId = daycare.id,
                groupId = group.id,
                childId = child1.id,
            )
        val eventAttendee2 =
            DevCalendarEventAttendee(
                calendarEventId = event2.id,
                unitId = daycare.id,
                groupId = group.id,
                childId = child2.id,
            )
        val eventAttendee3 =
            DevCalendarEventAttendee(
                calendarEventId = event3.id,
                unitId = daycare.id,
                groupId = group.id,
                childId = child2.id,
            )
        val eventAttendee4 =
            DevCalendarEventAttendee(
                calendarEventId = event4.id,
                unitId = daycare.id,
                groupId = group.id,
                childId = child1.id,
            )

        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(group)
            tx.insert(child1, DevPersonType.CHILD)
            tx.insert(child2, DevPersonType.CHILD)
            tx.insert(employee)
            tx.insert(event1)
            tx.insert(event2)
            tx.insert(event3)
            tx.insert(event4)
            tx.insert(eventAttendee1)
            tx.insert(eventAttendee2)
            tx.insert(eventAttendee3)
            tx.insert(eventAttendee4)

            val result =
                tx.getChildSpecificEventOrderReductions(group.id, LocalDate.of(2025, 5, 14))
            assertEquals(
                mapOf(
                    child1.id to setOf(NekkuProductMealTime.LUNCH, NekkuProductMealTime.SNACK),
                    child2.id to setOf(NekkuProductMealTime.LUNCH, NekkuProductMealTime.BREAKFAST),
                ),
                result,
            )
        }
    }
}
