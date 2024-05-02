// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.jamix

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.absence.AbsenceCategory
import fi.espoo.evaka.mealintegration.DefaultMealTypeMapper
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.dev.DevAbsence
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevChild
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevReservation
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.dev.insertTestDaycareGroupPlacement
import fi.espoo.evaka.shared.dev.insertTestPlacement
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.shared.domain.TimeRange
import fi.espoo.evaka.specialdiet.SpecialDiet
import fi.espoo.evaka.specialdiet.setSpecialDiets
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class JamixIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var jamixService: JamixService

    @Test
    fun `meal order jobs for the next week are planned`() {
        val area = DevCareArea()
        val daycare =
            DevDaycare(
                areaId = area.id,
                mealtimeBreakfast = TimeRange(LocalTime.of(8, 0), LocalTime.of(8, 20)),
                mealtimeLunch = TimeRange(LocalTime.of(11, 15), LocalTime.of(11, 45)),
                mealtimeSnack = TimeRange(LocalTime.of(13, 30), LocalTime.of(13, 50)),
            )
        val group1 = DevDaycareGroup(daycareId = daycare.id, jamixCustomerId = 88)
        val group2 = DevDaycareGroup(daycareId = daycare.id, jamixCustomerId = 99)

        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(group1)
            tx.insert(group2)
        }

        // Tuesday
        val now = HelsinkiDateTime.of(LocalDate.of(2024, 4, 2), LocalTime.of(2, 25))

        jamixService.planOrders(db, MockEvakaClock(now))

        val jobs =
            db.read { tx ->
                tx.createQuery {
                        sql("SELECT payload FROM async_job WHERE type = 'SendJamixOrder'")
                    }
                    .map { jsonColumn<AsyncJob.SendJamixOrder>("payload") }
                    .toList()
            }

        val mondayNextWeek = LocalDate.of(2024, 4, 8)
        val sundayNextWeek = LocalDate.of(2024, 4, 14)
        val dates = FiniteDateRange(mondayNextWeek, sundayNextWeek).dates().toList()
        val customerIds = listOf(88, 99)
        assertEquals(
            dates.flatMap { date ->
                customerIds.map { customerId -> AsyncJob.SendJamixOrder(customerId, date) }
            },
            jobs.sortedWith(compareBy({ it.date }, { it.customerId }))
        )
    }

    @Test
    fun `orders are sent correctly`() {
        val monday = LocalDate.of(2024, 4, 8)
        val tuesday = LocalDate.of(2024, 4, 9)
        val wednesday = LocalDate.of(2024, 4, 10)

        val area = DevCareArea()
        val daycare =
            DevDaycare(
                areaId = area.id,
                mealtimeBreakfast = TimeRange(LocalTime.of(8, 0), LocalTime.of(8, 20)),
                mealtimeLunch = TimeRange(LocalTime.of(11, 15), LocalTime.of(11, 45)),
                mealtimeSnack = TimeRange(LocalTime.of(13, 30), LocalTime.of(13, 50)),
            )
        val group1 = DevDaycareGroup(daycareId = daycare.id, jamixCustomerId = 88)
        val group2 = DevDaycareGroup(daycareId = daycare.id, jamixCustomerId = 99)
        val employee = DevEmployee()

        val child = DevPerson()
        val childWithSpecialDiet = DevPerson(firstName = "Diet", lastName = "Johnson")

        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(group1)
            tx.insert(group2)
            tx.insert(employee)

            tx.insert(child, DevPersonType.CHILD)
            tx.insertTestPlacement(
                    unitId = daycare.id,
                    childId = child.id,
                    startDate = monday,
                    endDate = tuesday,
                )
                .also { placementId ->
                    tx.insertTestDaycareGroupPlacement(
                        daycarePlacementId = placementId,
                        groupId = group1.id,
                        startDate = monday,
                        endDate = tuesday
                    )
                }
            listOf(
                    // All three meals on Monday
                    DevReservation(
                        childId = child.id,
                        date = monday,
                        startTime = LocalTime.of(8, 0),
                        endTime = LocalTime.of(16, 0),
                        createdBy = employee.evakaUserId
                    ),
                    // Breakfast only on Tuesday
                    DevReservation(
                        childId = child.id,
                        date = tuesday,
                        startTime = LocalTime.of(8, 0),
                        endTime = LocalTime.of(9, 0),
                        createdBy = employee.evakaUserId
                    )
                )
                .forEach { tx.insert(it) }

            tx.setSpecialDiets(listOf(SpecialDiet(1, "diet name", "diet abbreviation")))

            tx.insert(childWithSpecialDiet, DevPersonType.RAW_ROW)
            tx.insert(DevChild(id = childWithSpecialDiet.id, dietId = 1))
            tx.insertTestPlacement(
                    unitId = daycare.id,
                    childId = childWithSpecialDiet.id,
                    startDate = monday,
                    endDate = tuesday
                )
                .also { placementId ->
                    tx.insertTestDaycareGroupPlacement(
                        daycarePlacementId = placementId,
                        groupId = group2.id,
                        startDate = monday,
                        endDate = tuesday
                    )
                }
            // Lunch only on Monday
            tx.insert(
                DevReservation(
                    childId = childWithSpecialDiet.id,
                    date = monday,
                    startTime = LocalTime.of(11, 0),
                    endTime = LocalTime.of(12, 0),
                    createdBy = employee.evakaUserId
                )
            )
            // Absent on Tuesday
            tx.insert(
                DevAbsence(
                    childId = childWithSpecialDiet.id,
                    date = tuesday,
                    absenceCategory = AbsenceCategory.BILLABLE
                )
            )
        }

        val client = TestJamixClient()
        sendOrders(client, 88, monday)
        sendOrders(client, 88, tuesday)
        sendOrders(client, 99, monday)
        sendOrders(client, 99, tuesday)

        // Empty orders should not be sent
        sendOrders(client, 123, wednesday) // No groups with customer id 123
        sendOrders(client, 88, wednesday) // No placements on Wednesday

        assertEquals(
            listOf(
                JamixClient.MealOrder(
                    deliveryDate = monday,
                    customerID = 88,
                    mealOrderRows =
                        listOf(
                            JamixClient.MealOrderRow(
                                orderAmount = 1,
                                mealTypeID = 162,
                                dietID = null,
                                additionalInfo = null
                            ),
                            JamixClient.MealOrderRow(
                                orderAmount = 1,
                                mealTypeID = 175,
                                dietID = null,
                                additionalInfo = null
                            ),
                            JamixClient.MealOrderRow(
                                orderAmount = 1,
                                mealTypeID = 152,
                                dietID = null,
                                additionalInfo = null
                            )
                        )
                ),
                JamixClient.MealOrder(
                    deliveryDate = tuesday,
                    customerID = 88,
                    mealOrderRows =
                        listOf(
                            JamixClient.MealOrderRow(
                                orderAmount = 1,
                                mealTypeID = 162,
                                dietID = null,
                                additionalInfo = null
                            ),
                        )
                ),
                JamixClient.MealOrder(
                    deliveryDate = monday,
                    customerID = 99,
                    mealOrderRows =
                        listOf(
                            JamixClient.MealOrderRow(
                                orderAmount = 1,
                                mealTypeID = 145,
                                dietID = 1,
                                additionalInfo = "Johnson Diet"
                            )
                        )
                )
            ),
            client.orders
        )
    }

    private fun sendOrders(client: JamixClient, customerId: Int, date: LocalDate) {
        createAndSendJamixOrder(client, db, DefaultMealTypeMapper, customerId, date)
    }
}

class TestJamixClient : JamixClient {
    val orders = mutableListOf<JamixClient.MealOrder>()

    override fun createMealOrder(order: JamixClient.MealOrder) {
        orders.add(order)
    }
}
