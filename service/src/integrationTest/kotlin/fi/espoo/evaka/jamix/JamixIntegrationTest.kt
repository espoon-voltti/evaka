// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.jamix

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.absence.AbsenceCategory
import fi.espoo.evaka.daycare.getChild
import fi.espoo.evaka.emailclient.MockEmailClient
import fi.espoo.evaka.mealintegration.DefaultMealTypeMapper
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.dev.*
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.shared.domain.TimeRange
import fi.espoo.evaka.specialdiet.*
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired

class JamixIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var asyncJobRunner: AsyncJobRunner<AsyncJob>

    private val customerNumberToIdMapping = mapOf(77 to 777, 88 to 888, 99 to 999)
    private val now = HelsinkiDateTime.of(LocalDate.of(2024, 4, 8), LocalTime.of(2, 25))

    @Test
    fun `meal order jobs for daycare groups without customer number are not planned`() {
        val area = DevCareArea()
        val daycare =
            DevDaycare(
                areaId = area.id,
                mealtimeBreakfast = TimeRange(LocalTime.of(8, 0), LocalTime.of(8, 20)),
                mealtimeLunch = TimeRange(LocalTime.of(11, 15), LocalTime.of(11, 45)),
                mealtimeSnack = TimeRange(LocalTime.of(13, 30), LocalTime.of(13, 50)),
            )
        val group = DevDaycareGroup(daycareId = daycare.id, jamixCustomerNumber = null)

        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(group)
        }

        // Tuesday
        val now = HelsinkiDateTime.of(LocalDate.of(2024, 4, 2), LocalTime.of(2, 25))

        planJamixOrderJobs(db, asyncJobRunner, TestJamixClient(customerNumberToIdMapping), now)

        assertEquals(emptyList(), getJobs())
    }

    @Test
    fun `meal order jobs for closed units are not planned`() {
        // Tuesday
        val now = HelsinkiDateTime.of(LocalDate.of(2024, 4, 2), LocalTime.of(2, 25))
        val area = DevCareArea()
        val daycare =
            DevDaycare(
                areaId = area.id,
                mealtimeBreakfast = TimeRange(LocalTime.of(8, 0), LocalTime.of(8, 20)),
                mealtimeLunch = TimeRange(LocalTime.of(11, 15), LocalTime.of(11, 45)),
                mealtimeSnack = TimeRange(LocalTime.of(13, 30), LocalTime.of(13, 50)),
                closingDate = now.toLocalDate().minusDays(1),
            )
        val group1 = DevDaycareGroup(daycareId = daycare.id, jamixCustomerNumber = 88)

        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(group1)
        }

        planJamixOrderJobs(db, asyncJobRunner, TestJamixClient(customerNumberToIdMapping), now)

        assertEquals(emptyList(), getJobs())
    }

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
        val group1 = DevDaycareGroup(daycareId = daycare.id, jamixCustomerNumber = 88)
        val group2 = DevDaycareGroup(daycareId = daycare.id, jamixCustomerNumber = 99)

        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(group1)
            tx.insert(group2)
        }

        // Tuesday
        val now = HelsinkiDateTime.of(LocalDate.of(2024, 4, 2), LocalTime.of(2, 25))

        planJamixOrderJobs(db, asyncJobRunner, TestJamixClient(customerNumberToIdMapping), now)

        val jobs = getJobs()

        val mondayNextWeek = LocalDate.of(2024, 4, 8)
        val sundayNextWeek = LocalDate.of(2024, 4, 14)
        val dates = FiniteDateRange(mondayNextWeek, sundayNextWeek).dates().toList()
        assertEquals(
            dates.flatMap { date ->
                listOf(
                    AsyncJob.SendJamixOrder(customerNumber = 88, customerId = 888, date),
                    AsyncJob.SendJamixOrder(customerNumber = 99, customerId = 999, date),
                )
            },
            jobs.sortedWith(compareBy({ it.date }, { it.customerId })),
        )
    }

    @Test
    fun `meal order jobs can be manually requested for a unit and date`() {
        val area = DevCareArea()
        val daycare = DevDaycare(areaId = area.id)
        val daycare2 = DevDaycare(areaId = area.id)
        val group1 = DevDaycareGroup(daycareId = daycare.id, jamixCustomerNumber = 77)
        val group2 = DevDaycareGroup(daycareId = daycare.id, jamixCustomerNumber = 88)
        val group3 = DevDaycareGroup(daycareId = daycare2.id, jamixCustomerNumber = 99)

        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(daycare2)
            tx.insert(group1)
            tx.insert(group2)
            tx.insert(group3)
        }

        // Tuesday
        val now = HelsinkiDateTime.of(LocalDate.of(2024, 4, 2), LocalTime.of(2, 25))

        // Date must be at least 2 days in future
        assertThrows<IllegalArgumentException> {
            planJamixOrderJobsForUnitAndDate(
                dbc = db,
                asyncJobRunner = asyncJobRunner,
                client = TestJamixClient(customerNumberToIdMapping),
                now = now,
                unitId = daycare.id,
                date = now.toLocalDate().plusDays(1),
            )
        }

        val date = now.toLocalDate().plusDays(2)
        planJamixOrderJobsForUnitAndDate(
            dbc = db,
            asyncJobRunner = asyncJobRunner,
            client = TestJamixClient(customerNumberToIdMapping),
            now = now,
            unitId = daycare.id,
            date = date,
        )

        val jobs = getJobs()

        assertEquals(
            listOf(
                AsyncJob.SendJamixOrder(customerNumber = 77, customerId = 777, date),
                AsyncJob.SendJamixOrder(customerNumber = 88, customerId = 888, date),
            ),
            jobs.sortedWith(compareBy({ it.date }, { it.customerId })),
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
        val group1 = DevDaycareGroup(daycareId = daycare.id, jamixCustomerNumber = 88)
        val group2 = DevDaycareGroup(daycareId = daycare.id, jamixCustomerNumber = 99)
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
            tx.insert(
                    DevPlacement(
                        childId = child.id,
                        unitId = daycare.id,
                        startDate = monday,
                        endDate = tuesday,
                    )
                )
                .also { placementId ->
                    tx.insert(
                        DevDaycareGroupPlacement(
                            daycarePlacementId = placementId,
                            daycareGroupId = group1.id,
                            startDate = monday,
                            endDate = tuesday,
                        )
                    )
                }
            listOf(
                    // All three meals on Monday
                    DevReservation(
                        childId = child.id,
                        date = monday,
                        startTime = LocalTime.of(8, 0),
                        endTime = LocalTime.of(16, 0),
                        createdBy = employee.evakaUserId,
                    ),
                    // Breakfast only on Tuesday
                    DevReservation(
                        childId = child.id,
                        date = tuesday,
                        startTime = LocalTime.of(8, 0),
                        endTime = LocalTime.of(9, 0),
                        createdBy = employee.evakaUserId,
                    ),
                )
                .forEach { tx.insert(it) }

            tx.setSpecialDiets(listOf(SpecialDiet(1, "diet abbreviation")))
            tx.setMealTextures(listOf(MealTexture(42, "Sosemainen")))

            tx.insert(childWithSpecialDiet, DevPersonType.RAW_ROW)
            tx.insert(DevChild(id = childWithSpecialDiet.id, dietId = 1, mealTextureId = 42))
            tx.insert(
                    DevPlacement(
                        childId = childWithSpecialDiet.id,
                        unitId = daycare.id,
                        startDate = monday,
                        endDate = tuesday,
                    )
                )
                .also { placementId ->
                    tx.insert(
                        DevDaycareGroupPlacement(
                            daycarePlacementId = placementId,
                            daycareGroupId = group2.id,
                            startDate = monday,
                            endDate = tuesday,
                        )
                    )
                }
            // Lunch only on Monday
            tx.insert(
                DevReservation(
                    childId = childWithSpecialDiet.id,
                    date = monday,
                    startTime = LocalTime.of(11, 0),
                    endTime = LocalTime.of(12, 0),
                    createdBy = employee.evakaUserId,
                )
            )
            // Absent on Tuesday
            tx.insert(
                DevAbsence(
                    childId = childWithSpecialDiet.id,
                    date = tuesday,
                    absenceCategory = AbsenceCategory.BILLABLE,
                )
            )
        }

        val client = TestJamixClient()
        sendOrders(client, 88, 888, monday)
        sendOrders(client, 88, 888, tuesday)
        sendOrders(client, 99, 999, monday)
        sendOrders(client, 99, 999, tuesday)

        // Empty orders should not be sent
        sendOrders(client, 123, 1234, wednesday) // No groups with customer id 123
        sendOrders(client, 88, 888, wednesday) // No placements on Wednesday

        assertEquals(
            listOf(
                JamixClient.MealOrder(
                    deliveryDate = monday,
                    customerID = 888,
                    mealOrderRows =
                        listOf(
                            JamixClient.MealOrderRow(
                                orderAmount = 1,
                                mealTypeID = 162,
                                dietID = null,
                                additionalInfo = null,
                                textureID = null,
                            ),
                            JamixClient.MealOrderRow(
                                orderAmount = 1,
                                mealTypeID = 175,
                                dietID = null,
                                additionalInfo = null,
                                textureID = null,
                            ),
                            JamixClient.MealOrderRow(
                                orderAmount = 1,
                                mealTypeID = 152,
                                dietID = null,
                                additionalInfo = null,
                                textureID = null,
                            ),
                        ),
                ),
                JamixClient.MealOrder(
                    deliveryDate = tuesday,
                    customerID = 888,
                    mealOrderRows =
                        listOf(
                            JamixClient.MealOrderRow(
                                orderAmount = 1,
                                mealTypeID = 162,
                                dietID = null,
                                additionalInfo = null,
                                textureID = null,
                            )
                        ),
                ),
                JamixClient.MealOrder(
                    deliveryDate = monday,
                    customerID = 999,
                    mealOrderRows =
                        listOf(
                            JamixClient.MealOrderRow(
                                orderAmount = 1,
                                mealTypeID = 145,
                                dietID = 1,
                                additionalInfo = "Johnson Diet",
                                textureID = 42,
                            )
                        ),
                ),
            ),
            client.orders,
        )
    }

    @Test
    fun `diet sync does not sync empty data`() {
        val client = TestJamixClient()
        assertThrows<Exception> { fetchAndUpdateJamixDiets(client, db, asyncJobRunner, now) }
    }

    @Test
    fun `diet sync adds new diets`() {
        val client =
            TestJamixClient(
                specialDiets =
                    listOf(
                        JamixSpecialDiet(1, JamixSpecialDietFields("Foobar", "Foo")),
                        JamixSpecialDiet(2, JamixSpecialDietFields("Hello World", "Hello")),
                    )
            )
        fetchAndUpdateJamixDiets(client, db, asyncJobRunner, now)
        db.transaction { tx ->
            val diets = tx.getSpecialDiets().toSet()
            assertEquals(setOf(SpecialDiet(1, "Foo"), SpecialDiet(2, "Hello")), diets)
        }
    }

    @Test
    fun `diet sync nullifies removed diets from child`() {
        val childWithSpecialDiet = DevPerson(firstName = "Diet", lastName = "Johnson")
        db.transaction { tx ->
            tx.setSpecialDiets(listOf(SpecialDiet(555, "diet abbreviation")))
            tx.insert(childWithSpecialDiet, DevPersonType.RAW_ROW)
            tx.insert(DevChild(id = childWithSpecialDiet.id, dietId = 555))
        }
        val client =
            TestJamixClient(
                specialDiets = listOf(JamixSpecialDiet(1, JamixSpecialDietFields("Foobar", "Foo")))
            )
        fetchAndUpdateJamixDiets(client, db, asyncJobRunner, now)

        db.transaction { tx ->
            val childAfterSync = tx.getChild(childWithSpecialDiet.id)
            assertNotNull(childAfterSync)
            assertNull(childAfterSync.additionalInformation.specialDiet)
        }
    }

    @Test
    fun `diet sync generates warning emails`() {
        val area = DevCareArea()
        val daycare = DevDaycare(areaId = area.id, name = "Daycare 1")
        val daycare2 = DevDaycare(areaId = area.id, name = "Daycare 2")
        val employee1 = DevEmployee(email = "supervisor_the_first@city.fi")
        val employee2 = DevEmployee(email = "supervisor_the_second@city.fi")
        val employee3 = DevEmployee(email = "supervisor_the_third@city.fi")

        val child1 = DevPerson()
        val child2 = DevPerson()
        val child3 = DevPerson()

        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(daycare2)
            tx.insert(employee1, mapOf(daycare.id to UserRole.UNIT_SUPERVISOR))
            tx.insert(employee2, mapOf(daycare.id to UserRole.UNIT_SUPERVISOR))
            tx.insert(employee3, mapOf(daycare2.id to UserRole.UNIT_SUPERVISOR))

            tx.setSpecialDiets(listOf(SpecialDiet(1, "diet 1"), SpecialDiet(2, "diet 2")))
            tx.insert(child1, DevPersonType.RAW_ROW)
            tx.insert(DevChild(id = child1.id, dietId = 1))
            tx.insert(
                DevPlacement(
                    childId = child1.id,
                    unitId = daycare.id,
                    startDate = now.toLocalDate().minusYears(1),
                    endDate = now.toLocalDate(),
                )
            )
            // Placement today and another in the future -> email is generated only for the current
            // placement
            tx.insert(
                DevPlacement(
                    childId = child1.id,
                    unitId = daycare2.id,
                    startDate = now.toLocalDate().plusDays(1),
                    endDate = now.toLocalDate().plusYears(1),
                )
            )

            tx.insert(child2, DevPersonType.RAW_ROW)
            tx.insert(DevChild(id = child2.id, dietId = 2))
            // No placement today, placement starts in the future -> email is generated
            tx.insert(
                DevPlacement(
                    childId = child2.id,
                    unitId = daycare.id,
                    startDate = now.toLocalDate().plusDays(1),
                    endDate = now.toLocalDate().plusYears(1),
                )
            )

            tx.insert(child3, DevPersonType.RAW_ROW)
            tx.insert(DevChild(id = child3.id, dietId = 2))
            // Placement ended in the past -> no email
            tx.insert(
                DevPlacement(
                    childId = child3.id,
                    unitId = daycare.id,
                    startDate = now.toLocalDate().minusYears(1),
                    endDate = now.toLocalDate().minusDays(1),
                )
            )
        }

        val client =
            TestJamixClient(
                specialDiets =
                    listOf(
                        JamixSpecialDiet(3, JamixSpecialDietFields("diet 3", "diet abbreviation 3"))
                    )
            )
        fetchAndUpdateJamixDiets(client, db, asyncJobRunner, now)
        asyncJobRunner.runPendingJobsSync(MockEvakaClock(now))

        val expectedTextContent =
            """
Seuraavien lasten erityisruokavaliot on poistettu johtuen erityisruokavalioiden poistumisesta Jamixista:

- Lapsen tunniste: '${child1.id}', Alkuperäinen erityisruokavalio: 'diet 1' ERV tunniste: 1

- Lapsen tunniste: '${child2.id}', Alkuperäinen erityisruokavalio: 'diet 2' ERV tunniste: 2
"""
                .trim()

        val employees =
            listOf(employee1.id to employee1.email, employee2.id to employee2.email).sortedBy {
                it.first
            }
        assertEquals(
            employees.map { it.second to expectedTextContent },
            MockEmailClient.emails.map { it.toAddress to it.content.text },
        )
    }

    @Test
    fun `diet sync keeps diet selection when diet abbreviation changes`() {
        val childWithSpecialDiet = DevPerson(firstName = "Diet", lastName = "Johnson")
        db.transaction { tx ->
            tx.setSpecialDiets(listOf(SpecialDiet(1, "diet abbreviation with a typo")))
            tx.insert(childWithSpecialDiet, DevPersonType.RAW_ROW)
            tx.insert(DevChild(id = childWithSpecialDiet.id, dietId = 1))
        }
        val client =
            TestJamixClient(
                specialDiets =
                    listOf(
                        JamixSpecialDiet(
                            1,
                            JamixSpecialDietFields("diet name fixed", "diet abbreviation fixed"),
                        )
                    )
            )
        fetchAndUpdateJamixDiets(client, db, asyncJobRunner, now)
        db.transaction { tx ->
            val childAfterSync = tx.getChild(childWithSpecialDiet.id)
            assertNotNull(childAfterSync)
            assertEquals(
                SpecialDiet(1, "diet abbreviation fixed"),
                childAfterSync.additionalInformation.specialDiet,
            )
        }
    }

    private fun getJobs() =
        db.read { tx ->
            tx.createQuery { sql("SELECT payload FROM async_job WHERE type = 'SendJamixOrder'") }
                .map { jsonColumn<AsyncJob.SendJamixOrder>("payload") }
                .toList()
        }

    private fun sendOrders(
        client: JamixClient,
        customerNumber: Int,
        customerId: Int,
        date: LocalDate,
    ) {
        createAndSendJamixOrder(client, db, DefaultMealTypeMapper, customerNumber, customerId, date)
    }
}

class TestJamixClient(
    val customers: Map<Int, Int> = mapOf(),
    val specialDiets: List<JamixSpecialDiet> = emptyList(),
    val mealTextures: List<JamixTexture> = emptyList(),
) : JamixClient {
    val orders = mutableListOf<JamixClient.MealOrder>()

    override fun getCustomers(): List<JamixClient.Customer> {
        return customers.map { (customerNumber, customerId) ->
            JamixClient.Customer(customerId, customerNumber)
        }
    }

    override fun createMealOrder(order: JamixClient.MealOrder) {
        orders.add(order)
    }

    override fun getDiets(): List<JamixSpecialDiet> {
        return specialDiets
    }

    override fun getTextures(): List<JamixTexture> {
        return mealTextures
    }
}
