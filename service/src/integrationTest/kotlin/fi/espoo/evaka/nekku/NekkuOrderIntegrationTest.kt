// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.nekku

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.absence.AbsenceCategory
import fi.espoo.evaka.absence.AbsenceType
import fi.espoo.evaka.reports.getNekkuReportRows
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.dev.DevAbsence
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevChild
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.DevDaycareGroupPlacement
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.DevReservation
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.TimeRange
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class NekkuOrderIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var asyncJobRunner: AsyncJobRunner<AsyncJob>

    @Test
    fun `meal order jobs for daycare groups without customer number are not planned`() {

        val client = TestNekkuClient(customers = basicTestClientCustomers)
        fetchAndUpdateNekkuCustomers(client, db, asyncJobRunner, now)

        val area = DevCareArea()
        val daycare =
            DevDaycare(
                areaId = area.id,
                mealtimeBreakfast = TimeRange(LocalTime.of(8, 0), LocalTime.of(8, 20)),
                mealtimeLunch = TimeRange(LocalTime.of(11, 15), LocalTime.of(11, 45)),
                mealtimeSnack = TimeRange(LocalTime.of(13, 30), LocalTime.of(13, 50)),
            )
        val group = DevDaycareGroup(daycareId = daycare.id, nekkuCustomerNumber = null)

        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(group)
        }

        // Tuesday
        val now = HelsinkiDateTime.of(LocalDate.of(2025, 4, 1), LocalTime.of(2, 25))

        planNekkuOrderJobs(db, asyncJobRunner, now)

        assertEquals(emptyList(), getNekkuJobs(db))
    }

    @Test
    fun `meal order jobs for daycare groups with customer number are planned for three week time`() {

        val client = TestNekkuClient(customers = basicTestClientCustomers)
        fetchAndUpdateNekkuCustomers(client, db, asyncJobRunner, now)

        val area = DevCareArea()

        val daycare =
            DevDaycare(
                areaId = area.id,
                mealtimeBreakfast = TimeRange(LocalTime.of(8, 0), LocalTime.of(8, 20)),
                mealtimeLunch = TimeRange(LocalTime.of(11, 15), LocalTime.of(11, 45)),
                mealtimeSnack = TimeRange(LocalTime.of(13, 30), LocalTime.of(13, 50)),
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
            )
        val group = DevDaycareGroup(daycareId = daycare.id, nekkuCustomerNumber = "2501K6089")

        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(group)
        }

        // Friday
        val now = HelsinkiDateTime.of(LocalDate.of(2025, 3, 21), LocalTime.of(2, 25))

        planNekkuOrderJobs(db, asyncJobRunner, now)

        assertEquals(7, getNekkuJobs(db).count())
    }

    @Test
    fun `meal order jobs for daycare groups are planned for three week time and it does not create job for daycare that is not open`() {

        val client = TestNekkuClient(customers = basicTestClientCustomers)
        fetchAndUpdateNekkuCustomers(client, db, asyncJobRunner, now)

        val area = DevCareArea()

        val daycare =
            DevDaycare(
                areaId = area.id,
                mealtimeBreakfast = TimeRange(LocalTime.of(8, 0), LocalTime.of(8, 20)),
                mealtimeLunch = TimeRange(LocalTime.of(11, 15), LocalTime.of(11, 45)),
                mealtimeSnack = TimeRange(LocalTime.of(13, 30), LocalTime.of(13, 50)),
                shiftCareOperationTimes =
                    listOf(
                        TimeRange(LocalTime.parse("00:00"), LocalTime.parse("23:59")),
                        TimeRange(LocalTime.parse("00:00"), LocalTime.parse("23:59")),
                        TimeRange(LocalTime.parse("00:00"), LocalTime.parse("23:59")),
                        TimeRange(LocalTime.parse("00:00"), LocalTime.parse("23:59")),
                        TimeRange(LocalTime.parse("00:00"), LocalTime.parse("23:59")),
                        TimeRange(LocalTime.parse("00:00"), LocalTime.parse("23:59")),
                        null,
                    ),
            )
        val group = DevDaycareGroup(daycareId = daycare.id, nekkuCustomerNumber = "2501K6089")

        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(group)
        }

        // monday
        val now = HelsinkiDateTime.of(LocalDate.of(2025, 3, 17), LocalTime.of(2, 25))

        val nowPlusThreeWeeks = HelsinkiDateTime.of(LocalDate.of(2025, 4, 7), LocalTime.of(2, 25))

        planNekkuOrderJobs(db, asyncJobRunner, now)

        assertEquals(
            nowPlusThreeWeeks.toLocalDate().toString(),
            getNekkuJobs(db).first().date.toString(),
        )

        assertEquals(6, getNekkuJobs(db).count())
    }

    @Test
    fun `meal order jobs for daycare groups are planned for three week time and it checks holidays`() {

        val client = TestNekkuClient(customers = basicTestClientCustomers)
        fetchAndUpdateNekkuCustomers(client, db, asyncJobRunner, now)

        val area = DevCareArea()

        val daycare =
            DevDaycare(
                areaId = area.id,
                mealtimeBreakfast = TimeRange(LocalTime.of(8, 0), LocalTime.of(8, 20)),
                mealtimeLunch = TimeRange(LocalTime.of(11, 15), LocalTime.of(11, 45)),
                mealtimeSnack = TimeRange(LocalTime.of(13, 30), LocalTime.of(13, 50)),
            )
        val group = DevDaycareGroup(daycareId = daycare.id, nekkuCustomerNumber = "2501K6089")

        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(group)
        }

        // friday
        val now = HelsinkiDateTime.of(LocalDate.of(2025, 3, 28), LocalTime.of(2, 25))

        val nowPlusThreeWeeks = HelsinkiDateTime.of(LocalDate.of(2025, 4, 14), LocalTime.of(2, 25))

        planNekkuOrderJobs(db, asyncJobRunner, now)

        assertEquals(
            nowPlusThreeWeeks.toLocalDate().toString(),
            getNekkuJobs(db).first().date.toString(),
        )

        assertEquals(4, getNekkuJobs(db).count())
    }

    @Test
    fun `meal order jobs for daycare groups are planned if daycare has shift care open on holiday`() {

        val client = TestNekkuClient(customers = basicTestClientCustomers)
        fetchAndUpdateNekkuCustomers(client, db, asyncJobRunner, now)

        val area = DevCareArea()

        val daycare =
            DevDaycare(
                areaId = area.id,
                mealtimeBreakfast = TimeRange(LocalTime.of(8, 0), LocalTime.of(8, 20)),
                mealtimeLunch = TimeRange(LocalTime.of(11, 15), LocalTime.of(11, 45)),
                mealtimeSnack = TimeRange(LocalTime.of(13, 30), LocalTime.of(13, 50)),
                shiftCareOperationTimes =
                    listOf(
                        TimeRange(LocalTime.parse("00:00"), LocalTime.parse("23:59")),
                        TimeRange(LocalTime.parse("00:00"), LocalTime.parse("23:59")),
                        TimeRange(LocalTime.parse("00:00"), LocalTime.parse("23:59")),
                        TimeRange(LocalTime.parse("00:00"), LocalTime.parse("23:59")),
                        TimeRange(LocalTime.parse("00:00"), LocalTime.parse("23:59")),
                        TimeRange(LocalTime.parse("00:00"), LocalTime.parse("23:59")),
                        null,
                    ),
                shiftCareOpenOnHolidays = true,
            )
        val group = DevDaycareGroup(daycareId = daycare.id, nekkuCustomerNumber = "2501K6089")

        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(group)
        }

        // friday
        val now = HelsinkiDateTime.of(LocalDate.of(2025, 3, 28), LocalTime.of(2, 25))

        val nowPlusThreeWeeks = HelsinkiDateTime.of(LocalDate.of(2025, 4, 14), LocalTime.of(2, 25))

        planNekkuOrderJobs(db, asyncJobRunner, now)

        assertEquals(
            nowPlusThreeWeeks.toLocalDate().toString(),
            getNekkuJobs(db).first().date.toString(),
        )

        assertEquals(6, getNekkuJobs(db).count())
    }

    @Test
    fun `meal order jobs for daycare groups are re-planned for tomorrow`() {

        val client = TestNekkuClient(customers = basicTestClientCustomers)
        fetchAndUpdateNekkuCustomers(client, db, asyncJobRunner, now)

        val area = DevCareArea()

        val daycare =
            DevDaycare(
                areaId = area.id,
                mealtimeBreakfast = TimeRange(LocalTime.of(8, 0), LocalTime.of(8, 20)),
                mealtimeLunch = TimeRange(LocalTime.of(11, 15), LocalTime.of(11, 45)),
                mealtimeSnack = TimeRange(LocalTime.of(13, 30), LocalTime.of(13, 50)),
            )
        val group = DevDaycareGroup(daycareId = daycare.id, nekkuCustomerNumber = "2501K6089")

        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(group)
        }

        // Tuesday
        val now = HelsinkiDateTime.of(LocalDate.of(2025, 4, 1), LocalTime.of(2, 25))

        val tomorrow = HelsinkiDateTime.of(LocalDate.of(2025, 4, 2), LocalTime.of(2, 25))

        planNekkuDailyOrderJobs(db, asyncJobRunner, now)

        assertEquals(tomorrow.toLocalDate().toString(), getNekkuJobs(db).single().date.toString())
    }

    @Test
    fun `meal order jobs for daycare groups are not re-planned for tomorrow if daycare is not open`() {

        val client =
            TestNekkuClient(
                customers = basicTestClientCustomers,
                nekkuProducts = nekkuProductsForOrder,
                specialDiets = listOf(getNekkuSpecialDiet()),
            )

        fetchAndUpdateNekkuCustomers(client, db, asyncJobRunner, now)
        // products
        fetchAndUpdateNekkuProducts(client, db)
        fetchAndUpdateNekkuSpecialDiets(client, db, asyncJobRunner, now)

        val area = DevCareArea()

        val daycare =
            DevDaycare(
                areaId = area.id,
                mealtimeBreakfast = TimeRange(LocalTime.of(8, 0), LocalTime.of(8, 20)),
                mealtimeLunch = TimeRange(LocalTime.of(11, 15), LocalTime.of(11, 45)),
                mealtimeSnack = TimeRange(LocalTime.of(13, 30), LocalTime.of(13, 50)),
            )

        val group = DevDaycareGroup(daycareId = daycare.id, nekkuCustomerNumber = "2501K6089")

        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(group)
        }

        // friday
        val friday = HelsinkiDateTime.of(LocalDate.of(2025, 4, 4), LocalTime.of(2, 25))

        val nextMonday = HelsinkiDateTime.of(LocalDate.of(2025, 4, 7), LocalTime.of(2, 25))

        planNekkuDailyOrderJobs(db, asyncJobRunner, friday)

        assertEquals(nextMonday.toLocalDate().toString(), getNekkuJobs(db).single().date.toString())
    }

    @Test
    fun `meal order jobs for daycare groups are not planned if daycare has no operation days`() {

        val client =
            TestNekkuClient(
                customers = basicTestClientCustomers,
                nekkuProducts = nekkuProductsForOrder,
                specialDiets = listOf(getNekkuSpecialDiet()),
            )

        fetchAndUpdateNekkuCustomers(client, db, asyncJobRunner, now)
        // products
        fetchAndUpdateNekkuProducts(client, db)
        fetchAndUpdateNekkuSpecialDiets(client, db, asyncJobRunner, now)

        val area = DevCareArea()

        val daycare =
            DevDaycare(
                areaId = area.id,
                mealtimeBreakfast = TimeRange(LocalTime.of(8, 0), LocalTime.of(8, 20)),
                mealtimeLunch = TimeRange(LocalTime.of(11, 15), LocalTime.of(11, 45)),
                mealtimeSnack = TimeRange(LocalTime.of(13, 30), LocalTime.of(13, 50)),
                operationTimes = listOf(null, null, null, null, null, null, null),
            )

        val group = DevDaycareGroup(daycareId = daycare.id, nekkuCustomerNumber = "2501K6089")

        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(group)
        }

        // tuesday
        val tuesday = HelsinkiDateTime.of(LocalDate.of(2025, 4, 1), LocalTime.of(2, 25))

        planNekkuDailyOrderJobs(db, asyncJobRunner, tuesday)

        assertEquals(0, getNekkuJobs(db).count())
    }

    @Test
    fun `meal order jobs for daycare groups are re-planned four days before actual date`() {

        val client = TestNekkuClient(customers = basicTestClientCustomers)
        fetchAndUpdateNekkuCustomers(client, db, asyncJobRunner, now)

        val area = DevCareArea()

        val daycare =
            DevDaycare(
                areaId = area.id,
                mealtimeBreakfast = TimeRange(LocalTime.of(8, 0), LocalTime.of(8, 20)),
                mealtimeLunch = TimeRange(LocalTime.of(11, 15), LocalTime.of(11, 45)),
                mealtimeSnack = TimeRange(LocalTime.of(13, 30), LocalTime.of(13, 50)),
            )
        val group = DevDaycareGroup(daycareId = daycare.id, nekkuCustomerNumber = "2501K6089")

        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(group)
        }

        // Monday
        val now = HelsinkiDateTime.of(LocalDate.of(2025, 3, 24), LocalTime.of(2, 25))
        // Friday
        val toFourDays = HelsinkiDateTime.of(LocalDate.of(2025, 3, 28), LocalTime.of(2, 25))

        planNekkuSpecifyOrderJobs(db, asyncJobRunner, now)

        assertEquals(toFourDays.toLocalDate().toString(), getNekkuJobs(db).single().date.toString())
    }

    @Test
    fun `meal order jobs for daycare groups are not re-planned four days before if daycare is not open at weekends`() {

        val client = TestNekkuClient(customers = basicTestClientCustomers)
        fetchAndUpdateNekkuCustomers(client, db, asyncJobRunner, now)

        val area = DevCareArea()

        val daycare =
            DevDaycare(
                areaId = area.id,
                mealtimeBreakfast = TimeRange(LocalTime.of(8, 0), LocalTime.of(8, 20)),
                mealtimeLunch = TimeRange(LocalTime.of(11, 15), LocalTime.of(11, 45)),
                mealtimeSnack = TimeRange(LocalTime.of(13, 30), LocalTime.of(13, 50)),
            )
        val group = DevDaycareGroup(daycareId = daycare.id, nekkuCustomerNumber = "2501K6089")

        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(group)
        }

        // Wednesday
        val now = HelsinkiDateTime.of(LocalDate.of(2025, 3, 26), LocalTime.of(2, 25))

        planNekkuSpecifyOrderJobs(db, asyncJobRunner, now)

        val jobs = getNekkuJobs(db)

        assertEquals(0, jobs.count())
    }

    @Test
    fun `meal order jobs for daycare groups are re-planned four days before actual date also on weekend`() {

        val client = TestNekkuClient(customers = basicTestClientCustomers)
        fetchAndUpdateNekkuCustomers(client, db, asyncJobRunner, now)

        val area = DevCareArea()

        val daycare =
            DevDaycare(
                areaId = area.id,
                mealtimeBreakfast = TimeRange(LocalTime.of(8, 0), LocalTime.of(8, 20)),
                mealtimeLunch = TimeRange(LocalTime.of(11, 15), LocalTime.of(11, 45)),
                mealtimeSnack = TimeRange(LocalTime.of(13, 30), LocalTime.of(13, 50)),
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
            )
        val group = DevDaycareGroup(daycareId = daycare.id, nekkuCustomerNumber = "2501K6089")

        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(group)
        }

        // Wednesday
        val now = HelsinkiDateTime.of(LocalDate.of(2025, 3, 26), LocalTime.of(2, 25))

        // Sunday
        val toFourDays = HelsinkiDateTime.of(LocalDate.of(2025, 3, 30), LocalTime.of(2, 25))

        planNekkuSpecifyOrderJobs(db, asyncJobRunner, now)

        assertEquals(toFourDays.toLocalDate().toString(), getNekkuJobs(db).single().date.toString())
    }

    @Test
    fun `Send Nekku orders with known reservations`() {
        val monday = LocalDate.of(2025, 4, 14)
        val tuesday = LocalDate.of(2025, 4, 15)

        // First create all of the basic backgrounds like
        // Customer numbers
        val client =
            TestNekkuClient(
                customers = basicTestClientCustomers,
                nekkuProducts = nekkuProductsForOrder,
                specialDiets = listOf(getNekkuSpecialDiet()),
            )
        fetchAndUpdateNekkuCustomers(client, db, asyncJobRunner, now)
        // products
        fetchAndUpdateNekkuProducts(client, db)
        fetchAndUpdateNekkuSpecialDiets(client, db, asyncJobRunner, now)
        // Daycare with groups
        val area = DevCareArea()
        val daycare =
            DevDaycare(
                areaId = area.id,
                mealtimeBreakfast = TimeRange(LocalTime.of(8, 0), LocalTime.of(8, 20)),
                mealtimeLunch = TimeRange(LocalTime.of(11, 15), LocalTime.of(11, 45)),
                mealtimeSnack = TimeRange(LocalTime.of(13, 30), LocalTime.of(13, 50)),
            )
        val group = DevDaycareGroup(daycareId = daycare.id, nekkuCustomerNumber = "2501K6089")
        val employee = DevEmployee()

        // Children with placements in the group and they are not absent
        val child = DevPerson()

        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(group)
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
                            daycareGroupId = group.id,
                            startDate = monday,
                            endDate = tuesday,
                        )
                    )
                }
            listOf(
                    // Two meals on Monday
                    DevReservation(
                        childId = child.id,
                        date = monday,
                        startTime = LocalTime.of(8, 0),
                        endTime = LocalTime.of(12, 0),
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
        }

        createAndSendNekkuOrder(client, db, group.id, monday)
        createAndSendNekkuOrder(client, db, group.id, tuesday)

        assertOrdersListEquals(
            listOf(
                NekkuClient.NekkuOrders(
                    listOf(
                        NekkuClient.NekkuOrder(
                            monday.toString(),
                            "2501K6089",
                            group.id.toString(),
                            listOf(
                                NekkuClient.Item("31000010", 1, null),
                                NekkuClient.Item("31000011", 1, null),
                            ),
                            group.name,
                        )
                    ),
                    dryRun = false,
                ),
                NekkuClient.NekkuOrders(
                    listOf(
                        NekkuClient.NekkuOrder(
                            tuesday.toString(),
                            "2501K6089",
                            group.id.toString(),
                            listOf(NekkuClient.Item("31000010", 1, null)),
                            group.name,
                        )
                    ),
                    dryRun = false,
                ),
            ),
            client.orders,
        )
    }

    @Test
    fun `Add Nekku order info to Nekku report`() {
        val monday = LocalDate.of(2025, 4, 14)
        val tuesday = LocalDate.of(2025, 4, 15)

        // First create all of the basic backgrounds like
        // Customer numbers
        val client =
            TestNekkuClient(
                customers = basicTestClientCustomers,
                nekkuProducts = nekkuProductsForOrder,
                specialDiets = listOf(getNekkuSpecialDiet()),
            )
        fetchAndUpdateNekkuCustomers(client, db, asyncJobRunner, now)
        // products
        fetchAndUpdateNekkuProducts(client, db)
        fetchAndUpdateNekkuSpecialDiets(client, db, asyncJobRunner, now)
        // Daycare with groups
        val area = DevCareArea()
        val daycare =
            DevDaycare(
                areaId = area.id,
                mealtimeBreakfast = TimeRange(LocalTime.of(8, 0), LocalTime.of(8, 20)),
                mealtimeLunch = TimeRange(LocalTime.of(11, 15), LocalTime.of(11, 45)),
                mealtimeSnack = TimeRange(LocalTime.of(13, 30), LocalTime.of(13, 50)),
            )
        val group = DevDaycareGroup(daycareId = daycare.id, nekkuCustomerNumber = "2501K6089")
        val employee = DevEmployee()

        // Children with placements in the group and they are not absent
        val child = DevPerson()

        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(group)
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
                            daycareGroupId = group.id,
                            startDate = monday,
                            endDate = tuesday,
                        )
                    )
                }
            listOf(
                    // Two meals on Monday
                    DevReservation(
                        childId = child.id,
                        date = monday,
                        startTime = LocalTime.of(8, 0),
                        endTime = LocalTime.of(16, 0),
                        createdBy = employee.evakaUserId,
                    )
                )
                .forEach { tx.insert(it) }
        }

        createAndSendNekkuOrder(client, db, group.id, monday)

        db.transaction { tx ->
            val nekkuOrderReportResult = tx.getNekkuOrderReport(daycare.id, group.id, monday)

            assertEquals(
                setOf(
                    NekkuOrdersReport(
                        monday,
                        daycare.id,
                        group.id,
                        "31000010",
                        1,
                        listOf(NekkuProductMealTime.BREAKFAST),
                        null,
                        null,
                        "Luotu: [12345], Peruttu: [65432]",
                    ),
                    NekkuOrdersReport(
                        monday,
                        daycare.id,
                        group.id,
                        "31000011",
                        1,
                        listOf(NekkuProductMealTime.LUNCH),
                        null,
                        null,
                        "Luotu: [12345], Peruttu: [65432]",
                    ),
                    NekkuOrdersReport(
                        monday,
                        daycare.id,
                        group.id,
                        "31000012",
                        1,
                        listOf(NekkuProductMealTime.SNACK),
                        null,
                        null,
                        "Luotu: [12345], Peruttu: [65432]",
                    ),
                ),
                nekkuOrderReportResult.toSet(),
            )
        }
    }

    @Test
    fun `Add Nekku order info to Nekku report if there is an error in the order`() {
        val monday = LocalDate.of(2025, 4, 14)
        val tuesday = LocalDate.of(2025, 4, 15)

        // First create all of the basic backgrounds like
        // Customer numbers
        val client =
            TestNekkuClient(
                customers =
                    listOf(
                        NekkuApiCustomer(
                            "2501K6089",
                            "Ahvenojan päiväkoti",
                            "Varhaiskasvatus",
                            listOf(
                                CustomerApiType(
                                    listOf(
                                        NekkuCustomerApiWeekday.WEDNESDAY,
                                        NekkuCustomerApiWeekday.THURSDAY,
                                        NekkuCustomerApiWeekday.FRIDAY,
                                        NekkuCustomerApiWeekday.SATURDAY,
                                        NekkuCustomerApiWeekday.SUNDAY,
                                        NekkuCustomerApiWeekday.WEEKDAYHOLIDAY,
                                    ),
                                    "100-lasta",
                                )
                            ),
                        )
                    ),
                nekkuProducts = nekkuProductsForErrorOrder,
                specialDiets = listOf(getNekkuSpecialDiet()),
            )
        fetchAndUpdateNekkuCustomers(client, db, asyncJobRunner, now)
        // products
        fetchAndUpdateNekkuProducts(client, db)
        fetchAndUpdateNekkuSpecialDiets(client, db, asyncJobRunner, now)
        // Daycare with groups
        val area = DevCareArea()
        val daycare =
            DevDaycare(
                areaId = area.id,
                mealtimeBreakfast = TimeRange(LocalTime.of(8, 0), LocalTime.of(8, 20)),
                mealtimeLunch = TimeRange(LocalTime.of(11, 15), LocalTime.of(11, 45)),
                mealtimeSnack = TimeRange(LocalTime.of(13, 30), LocalTime.of(13, 50)),
            )
        val group = DevDaycareGroup(daycareId = daycare.id, nekkuCustomerNumber = "2501K6089")
        val employee = DevEmployee()

        // Children with placements in the group and they are not absent
        val child = DevPerson()

        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(group)
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
                            daycareGroupId = group.id,
                            startDate = monday,
                            endDate = tuesday,
                        )
                    )
                }
            listOf(
                    // Two meals on Monday
                    DevReservation(
                        childId = child.id,
                        date = monday,
                        startTime = LocalTime.of(8, 0),
                        endTime = LocalTime.of(16, 0),
                        createdBy = employee.evakaUserId,
                    )
                )
                .forEach { tx.insert(it) }
        }

        createAndSendNekkuOrder(client, db, group.id, monday)

        db.transaction { tx ->
            val nekkuOrderReportResult = tx.getNekkuOrderReport(daycare.id, group.id, monday)

            assertEquals(
                setOf(
                    NekkuOrdersReport(
                        monday,
                        daycare.id,
                        group.id,
                        "",
                        0,
                        null,
                        null,
                        null,
                        "Could not find any customer with given date: MONDAY groupId=${group.id}",
                    )
                ),
                nekkuOrderReportResult.toSet(),
            )
        }
    }

    @Test
    fun `Send Nekku orders with known reservations and remove 10prcent of normal orders`() {
        val monday = LocalDate.of(2025, 4, 14)
        val tuesday = LocalDate.of(2025, 4, 15)

        // First create all of the basic backgrounds like
        // Customer numbers
        val client =
            TestNekkuClient(
                customers = basicTestClientCustomers,
                nekkuProducts = nekkuProductsForOrder,
                specialDiets = listOf(getNekkuSpecialDiet()),
            )
        fetchAndUpdateNekkuCustomers(client, db, asyncJobRunner, now)
        // products
        fetchAndUpdateNekkuProducts(client, db)
        fetchAndUpdateNekkuSpecialDiets(client, db, asyncJobRunner, now)
        // Daycare with groups
        val area = DevCareArea()
        val daycare =
            DevDaycare(
                areaId = area.id,
                mealtimeBreakfast = TimeRange(LocalTime.of(8, 0), LocalTime.of(8, 20)),
                mealtimeLunch = TimeRange(LocalTime.of(11, 15), LocalTime.of(11, 45)),
                mealtimeSnack = TimeRange(LocalTime.of(13, 30), LocalTime.of(13, 50)),
                nekkuOrderReductionPercentage = 10,
            )
        val group = DevDaycareGroup(daycareId = daycare.id, nekkuCustomerNumber = "2501K6089")
        val employee = DevEmployee()

        // Children with placements in the group and they are not absent

        val children =
            listOf(
                DevPerson(),
                DevPerson(),
                DevPerson(),
                DevPerson(),
                DevPerson(),
                DevPerson(),
                DevPerson(),
                DevPerson(),
                DevPerson(),
                DevPerson(),
            )

        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(group)
            tx.insert(employee)
            children.map { child ->
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
                                daycareGroupId = group.id,
                                startDate = monday,
                                endDate = tuesday,
                            )
                        )
                    }
                listOf(
                        // Two meals on Monday
                        DevReservation(
                            childId = child.id,
                            date = monday,
                            startTime = LocalTime.of(8, 0),
                            endTime = LocalTime.of(12, 0),
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
            }
        }

        createAndSendNekkuOrder(client, db, group.id, monday)
        createAndSendNekkuOrder(client, db, group.id, tuesday)

        assertOrdersListEquals(
            listOf(
                NekkuClient.NekkuOrders(
                    listOf(
                        NekkuClient.NekkuOrder(
                            monday.toString(),
                            "2501K6089",
                            group.id.toString(),
                            listOf(
                                NekkuClient.Item("31000010", 9, null),
                                NekkuClient.Item("31000011", 9, null),
                            ),
                            group.name,
                        )
                    ),
                    dryRun = false,
                ),
                NekkuClient.NekkuOrders(
                    listOf(
                        NekkuClient.NekkuOrder(
                            tuesday.toString(),
                            "2501K6089",
                            group.id.toString(),
                            listOf(NekkuClient.Item("31000010", 9, null)),
                            group.name,
                        )
                    ),
                    dryRun = false,
                ),
            ),
            client.orders,
        )
    }

    @Test
    fun `Send Nekku orders with known reservations and remove unit specific percentage of normal orders`() {
        val monday = LocalDate.of(2025, 4, 14)
        val tuesday = LocalDate.of(2025, 4, 15)

        // First create all of the basic backgrounds like
        // Customer numbers
        val client =
            TestNekkuClient(
                customers = basicTestClientCustomers,
                nekkuProducts = nekkuProductsForOrder,
                specialDiets = listOf(getNekkuSpecialDiet()),
            )
        fetchAndUpdateNekkuCustomers(client, db, asyncJobRunner, now)
        // products
        fetchAndUpdateNekkuProducts(client, db)
        fetchAndUpdateNekkuSpecialDiets(client, db, asyncJobRunner, now)
        // Daycare with groups
        val area = DevCareArea()
        val daycare1 =
            DevDaycare(
                areaId = area.id,
                mealtimeBreakfast = TimeRange(LocalTime.of(8, 0), LocalTime.of(8, 20)),
                mealtimeLunch = TimeRange(LocalTime.of(11, 15), LocalTime.of(11, 45)),
                mealtimeSnack = TimeRange(LocalTime.of(13, 30), LocalTime.of(13, 50)),
                nekkuOrderReductionPercentage = 10,
            )
        val daycare2 =
            DevDaycare(
                areaId = area.id,
                mealtimeBreakfast = TimeRange(LocalTime.of(8, 0), LocalTime.of(8, 20)),
                mealtimeLunch = TimeRange(LocalTime.of(11, 15), LocalTime.of(11, 45)),
                mealtimeSnack = TimeRange(LocalTime.of(13, 30), LocalTime.of(13, 50)),
                nekkuOrderReductionPercentage = 20,
            )
        val group1 = DevDaycareGroup(daycareId = daycare1.id, nekkuCustomerNumber = "2501K6089")
        val group2 = DevDaycareGroup(daycareId = daycare2.id, nekkuCustomerNumber = "2501K6089")
        val employee = DevEmployee()

        // Children with placements in the group and they are not absent

        val children1 =
            listOf(
                DevPerson(),
                DevPerson(),
                DevPerson(),
                DevPerson(),
                DevPerson(),
                DevPerson(),
                DevPerson(),
                DevPerson(),
                DevPerson(),
                DevPerson(),
            )
        val children2 =
            listOf(
                DevPerson(),
                DevPerson(),
                DevPerson(),
                DevPerson(),
                DevPerson(),
                DevPerson(),
                DevPerson(),
                DevPerson(),
                DevPerson(),
                DevPerson(),
            )

        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare1)
            tx.insert(daycare2)
            tx.insert(group1)
            tx.insert(group2)
            tx.insert(employee)
            children1.map { child ->
                tx.insert(child, DevPersonType.CHILD)
                tx.insert(
                        DevPlacement(
                            childId = child.id,
                            unitId = daycare1.id,
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
                // Two meals on Monday
                tx.insert(
                    DevReservation(
                        childId = child.id,
                        date = monday,
                        startTime = LocalTime.of(8, 0),
                        endTime = LocalTime.of(12, 0),
                        createdBy = employee.evakaUserId,
                    )
                )
            }

            children2.map { child ->
                tx.insert(child, DevPersonType.CHILD)
                tx.insert(
                        DevPlacement(
                            childId = child.id,
                            unitId = daycare2.id,
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
                // Two meals on Monday
                tx.insert(
                    DevReservation(
                        childId = child.id,
                        date = monday,
                        startTime = LocalTime.of(8, 0),
                        endTime = LocalTime.of(12, 0),
                        createdBy = employee.evakaUserId,
                    )
                )
            }
        }

        createAndSendNekkuOrder(client, db, group1.id, monday)
        createAndSendNekkuOrder(client, db, group2.id, monday)

        assertOrdersListEquals(
            listOf(
                NekkuClient.NekkuOrders(
                    listOf(
                        NekkuClient.NekkuOrder(
                            monday.toString(),
                            "2501K6089",
                            group1.id.toString(),
                            listOf(
                                NekkuClient.Item("31000010", 9, null),
                                NekkuClient.Item("31000011", 9, null),
                            ),
                            group1.name,
                        )
                    ),
                    dryRun = false,
                ),
                NekkuClient.NekkuOrders(
                    listOf(
                        NekkuClient.NekkuOrder(
                            monday.toString(),
                            "2501K6089",
                            group2.id.toString(),
                            listOf(
                                NekkuClient.Item("31000010", 8, null),
                                NekkuClient.Item("31000011", 8, null),
                            ),
                            group2.name,
                        )
                    ),
                    dryRun = false,
                ),
            ),
            client.orders,
        )
    }

    @Test
    fun `10percnt deduction does not remove any vegan or vegetable diets`() {
        val monday = LocalDate.of(2025, 4, 14)
        val tuesday = LocalDate.of(2025, 4, 15)

        // First create all of the basic backgrounds like
        // Customer numbers
        val client =
            TestNekkuClient(
                customers = basicTestClientCustomers,
                nekkuProducts = nekkuProductsForOrder,
                specialDiets = listOf(getNekkuSpecialDiet()),
            )
        fetchAndUpdateNekkuCustomers(client, db, asyncJobRunner, now)
        // products
        fetchAndUpdateNekkuProducts(client, db)
        fetchAndUpdateNekkuSpecialDiets(client, db, asyncJobRunner, now)
        // Daycare with groups
        val area = DevCareArea()
        val daycare =
            DevDaycare(
                areaId = area.id,
                mealtimeBreakfast = TimeRange(LocalTime.of(8, 0), LocalTime.of(8, 20)),
                mealtimeLunch = TimeRange(LocalTime.of(11, 15), LocalTime.of(11, 45)),
                mealtimeSnack = TimeRange(LocalTime.of(13, 30), LocalTime.of(13, 50)),
                nekkuOrderReductionPercentage = 10,
            )
        val group = DevDaycareGroup(daycareId = daycare.id, nekkuCustomerNumber = "2501K6089")
        val employee = DevEmployee()

        // Children with placements in the group and they are not absent

        val children =
            listOf(
                DevPerson(),
                DevPerson(),
                DevPerson(),
                DevPerson(),
                DevPerson(),
                DevPerson(),
                DevPerson(),
                DevPerson(),
                DevPerson(),
                DevPerson(),
            )

        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(group)
            tx.insert(employee)
            children.map { child ->
                tx.insert(child, DevPersonType.CHILD)
                tx.insert(DevChild(id = child.id, nekkuDiet = NekkuProductMealType.VEGETABLE))
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
                                daycareGroupId = group.id,
                                startDate = monday,
                                endDate = tuesday,
                            )
                        )
                    }
                listOf(
                        // Two meals on Monday
                        DevReservation(
                            childId = child.id,
                            date = monday,
                            startTime = LocalTime.of(8, 0),
                            endTime = LocalTime.of(12, 0),
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
            }
        }

        createAndSendNekkuOrder(client, db, group.id, monday)
        createAndSendNekkuOrder(client, db, group.id, tuesday)

        assertOrdersListEquals(
            listOf(
                NekkuClient.NekkuOrders(
                    listOf(
                        NekkuClient.NekkuOrder(
                            monday.toString(),
                            "2501K6089",
                            group.id.toString(),
                            listOf(
                                NekkuClient.Item("31000014", 10, null),
                                NekkuClient.Item("31000015", 10, null),
                            ),
                            group.name,
                        )
                    ),
                    dryRun = false,
                ),
                NekkuClient.NekkuOrders(
                    listOf(
                        NekkuClient.NekkuOrder(
                            tuesday.toString(),
                            "2501K6089",
                            group.id.toString(),
                            listOf(NekkuClient.Item("31000014", 10, null)),
                            group.name,
                        )
                    ),
                    dryRun = false,
                ),
            ),
            client.orders,
        )
    }

    @Test
    fun `Combined breakfast and lunch is ordered even if child is not eating breakfast`() {

        val monday = LocalDate.of(2025, 4, 14)
        val tuesday = LocalDate.of(2025, 4, 15)

        // First create all of the basic backgrounds like
        // Customer numbers
        val client =
            TestNekkuClient(
                customers =
                    listOf(
                        NekkuApiCustomer(
                            "2501K6090",
                            "Apporan päiväkoti",
                            "Varhaiskasvatus",
                            listOf(
                                CustomerApiType(
                                    listOf(
                                        NekkuCustomerApiWeekday.MONDAY,
                                        NekkuCustomerApiWeekday.TUESDAY,
                                        NekkuCustomerApiWeekday.WEDNESDAY,
                                        NekkuCustomerApiWeekday.THURSDAY,
                                        NekkuCustomerApiWeekday.FRIDAY,
                                        NekkuCustomerApiWeekday.SATURDAY,
                                        NekkuCustomerApiWeekday.SUNDAY,
                                        NekkuCustomerApiWeekday.WEEKDAYHOLIDAY,
                                    ),
                                    "50-99-lasta",
                                )
                            ),
                        )
                    ),
                nekkuProducts = nekkuProductsForOrder,
                specialDiets = listOf(getNekkuSpecialDiet()),
            )
        fetchAndUpdateNekkuCustomers(client, db, asyncJobRunner, now)
        // products
        fetchAndUpdateNekkuProducts(client, db)
        fetchAndUpdateNekkuSpecialDiets(client, db, asyncJobRunner, now)
        // Daycare with groups
        val area = DevCareArea()
        val daycare =
            DevDaycare(
                areaId = area.id,
                mealtimeBreakfast = TimeRange(LocalTime.of(8, 0), LocalTime.of(8, 20)),
                mealtimeLunch = TimeRange(LocalTime.of(11, 15), LocalTime.of(11, 45)),
                mealtimeSnack = TimeRange(LocalTime.of(13, 30), LocalTime.of(13, 50)),
            )
        val group = DevDaycareGroup(daycareId = daycare.id, nekkuCustomerNumber = "2501K6090")
        val employee = DevEmployee()

        // Children with placements in the group and they are not absent
        val child = DevPerson()

        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(group)
            tx.insert(employee)
            tx.insert(child, DevPersonType.CHILD)
            tx.insert(
                DevChild(id = child.id, participatesInBreakfast = false)
            ) // child does not eat breakfast
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
                            daycareGroupId = group.id,
                            startDate = monday,
                            endDate = tuesday,
                        )
                    )
                }
            listOf(
                    // Two meals on Monday
                    DevReservation(
                        childId = child.id,
                        date = monday,
                        startTime = LocalTime.of(8, 0),
                        endTime = LocalTime.of(12, 0),
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
        }

        createAndSendNekkuOrder(client, db, group.id, monday)

        assertOrdersListEquals(
            listOf(
                NekkuClient.NekkuOrders(
                    listOf(
                        NekkuClient.NekkuOrder(
                            monday.toString(),
                            "2501K6090",
                            group.id.toString(),
                            listOf(NekkuClient.Item("31000018", 1, null)),
                            group.name,
                        )
                    ),
                    dryRun = false,
                )
            ),
            client.orders,
        )
    }

    @Test
    fun `Breakfast is deducted from meals if child is not eating breakfast according to child details`() {

        val monday = LocalDate.of(2025, 4, 14)
        val tuesday = LocalDate.of(2025, 4, 15)

        // First create all of the basic backgrounds like
        // Customer numbers
        val client =
            TestNekkuClient(
                customers = basicTestClientCustomers,
                nekkuProducts = nekkuProductsForOrder,
                specialDiets = listOf(getNekkuSpecialDiet()),
            )
        fetchAndUpdateNekkuCustomers(client, db, asyncJobRunner, now)
        // products
        fetchAndUpdateNekkuProducts(client, db)
        fetchAndUpdateNekkuSpecialDiets(client, db, asyncJobRunner, now)
        // Daycare with groups
        val area = DevCareArea()
        val daycare =
            DevDaycare(
                areaId = area.id,
                mealtimeBreakfast = TimeRange(LocalTime.of(8, 0), LocalTime.of(8, 20)),
                mealtimeLunch = TimeRange(LocalTime.of(11, 15), LocalTime.of(11, 45)),
                mealtimeSnack = TimeRange(LocalTime.of(13, 30), LocalTime.of(13, 50)),
            )
        val group = DevDaycareGroup(daycareId = daycare.id, nekkuCustomerNumber = "2501K6089")
        val employee = DevEmployee()

        // Children with placements in the group and they are not absent
        val child = DevPerson()

        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(group)
            tx.insert(employee)
            tx.insert(child, DevPersonType.CHILD)
            tx.insert(
                DevChild(id = child.id, participatesInBreakfast = false)
            ) // child does not eat breakfast
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
                            daycareGroupId = group.id,
                            startDate = monday,
                            endDate = tuesday,
                        )
                    )
                }
            listOf(
                    // Two meals on Monday
                    DevReservation(
                        childId = child.id,
                        date = monday,
                        startTime = LocalTime.of(8, 0),
                        endTime = LocalTime.of(12, 0),
                        createdBy = employee.evakaUserId,
                    )
                )
                .forEach { tx.insert(it) }
        }

        createAndSendNekkuOrder(client, db, group.id, monday)

        assertOrdersListEquals(
            listOf(
                NekkuClient.NekkuOrders(
                    listOf(
                        NekkuClient.NekkuOrder(
                            monday.toString(),
                            "2501K6089",
                            group.id.toString(),
                            listOf(NekkuClient.Item("31000011", 1, null)),
                            group.name,
                        )
                    ),
                    dryRun = false,
                )
            ),
            client.orders,
        )
    }

    @Test
    fun `Send Nekku orders with different meal types`() {
        val monday = LocalDate.of(2025, 4, 14)
        val tuesday = LocalDate.of(2025, 4, 15)

        // First create all of the basic backgrounds like
        // Customer numbers
        val client =
            TestNekkuClient(
                customers = basicTestClientCustomers,
                nekkuProducts = nekkuProductsForOrder,
                specialDiets = listOf(getNekkuSpecialDiet()),
            )
        fetchAndUpdateNekkuCustomers(client, db, asyncJobRunner, now)
        // products
        fetchAndUpdateNekkuProducts(client, db)
        fetchAndUpdateNekkuSpecialDiets(client, db, asyncJobRunner, now)
        // Daycare with groups
        val area = DevCareArea()
        val daycare =
            DevDaycare(
                areaId = area.id,
                mealtimeBreakfast = TimeRange(LocalTime.of(8, 0), LocalTime.of(8, 20)),
                mealtimeLunch = TimeRange(LocalTime.of(11, 15), LocalTime.of(11, 45)),
                mealtimeSnack = TimeRange(LocalTime.of(13, 30), LocalTime.of(13, 50)),
            )
        val group = DevDaycareGroup(daycareId = daycare.id, nekkuCustomerNumber = "2501K6089")
        val employee = DevEmployee()

        // Children with placements in the group and they are not absent
        val child = DevPerson()

        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(group)
            tx.insert(employee)
            tx.insert(child, DevPersonType.CHILD)
            tx.insert(DevChild(id = child.id, nekkuDiet = NekkuProductMealType.VEGETABLE))
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
                            daycareGroupId = group.id,
                            startDate = monday,
                            endDate = tuesday,
                        )
                    )
                }
            listOf(
                    // Three meals on Monday
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
        }

        createAndSendNekkuOrder(client, db, group.id, monday)
        createAndSendNekkuOrder(client, db, group.id, tuesday)

        assertOrdersListEquals(
            listOf(
                NekkuClient.NekkuOrders(
                    listOf(
                        NekkuClient.NekkuOrder(
                            monday.toString(),
                            "2501K6089",
                            group.id.toString(),
                            listOf(
                                NekkuClient.Item("31000014", 1, null),
                                NekkuClient.Item("31000015", 1, null),
                                NekkuClient.Item("31000016", 1, null),
                            ),
                            group.name,
                        )
                    ),
                    dryRun = false,
                ),
                NekkuClient.NekkuOrders(
                    listOf(
                        NekkuClient.NekkuOrder(
                            tuesday.toString(),
                            "2501K6089",
                            group.id.toString(),
                            listOf(NekkuClient.Item("31000014", 1, null)),
                            group.name,
                        )
                    ),
                    dryRun = false,
                ),
            ),
            client.orders,
        )
    }

    @Test
    fun `Send Nekku orders with 2 children without reservations uses default meal amounts`() {
        val monday = LocalDate.of(2025, 4, 14)
        val tuesday = LocalDate.of(2025, 4, 15)

        // First create all of the basic backgrounds like
        // Customer numbers
        val client =
            TestNekkuClient(
                customers = basicTestClientCustomers,
                nekkuProducts = nekkuProductsForOrder,
                specialDiets = listOf(getNekkuSpecialDiet()),
            )
        fetchAndUpdateNekkuCustomers(client, db, asyncJobRunner, now)
        // products
        fetchAndUpdateNekkuProducts(client, db)
        fetchAndUpdateNekkuSpecialDiets(client, db, asyncJobRunner, now)
        // Daycare with groups
        val area = DevCareArea()
        val daycare =
            DevDaycare(
                areaId = area.id,
                mealtimeBreakfast = TimeRange(LocalTime.of(8, 0), LocalTime.of(8, 20)),
                mealtimeLunch = TimeRange(LocalTime.of(11, 15), LocalTime.of(11, 45)),
                mealtimeSnack = TimeRange(LocalTime.of(13, 30), LocalTime.of(13, 50)),
            )
        val group = DevDaycareGroup(daycareId = daycare.id, nekkuCustomerNumber = "2501K6089")
        val employee = DevEmployee()

        // Children with placements in the group and they are not absent
        val child = DevPerson()
        val child2 = DevPerson()

        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(group)
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
                            daycareGroupId = group.id,
                            startDate = monday,
                            endDate = tuesday,
                        )
                    )
                }
            tx.insert(child2, DevPersonType.CHILD)
            tx.insert(
                    DevPlacement(
                        childId = child2.id,
                        unitId = daycare.id,
                        startDate = monday,
                        endDate = tuesday,
                    )
                )
                .also { placementId ->
                    tx.insert(
                        DevDaycareGroupPlacement(
                            daycarePlacementId = placementId,
                            daycareGroupId = group.id,
                            startDate = monday,
                            endDate = tuesday,
                        )
                    )
                }
        }

        createAndSendNekkuOrder(client, db, group.id, monday)
        createAndSendNekkuOrder(client, db, group.id, tuesday)

        assertOrdersListEquals(
            listOf(
                NekkuClient.NekkuOrders(
                    listOf(
                        NekkuClient.NekkuOrder(
                            monday.toString(),
                            "2501K6089",
                            group.id.toString(),
                            listOf(
                                NekkuClient.Item("31000010", 2, null),
                                NekkuClient.Item("31000011", 2, null),
                                NekkuClient.Item("31000012", 2, null),
                            ),
                            group.name,
                        )
                    ),
                    dryRun = false,
                ),
                NekkuClient.NekkuOrders(
                    listOf(
                        NekkuClient.NekkuOrder(
                            tuesday.toString(),
                            "2501K6089",
                            group.id.toString(),
                            listOf(
                                NekkuClient.Item("31000010", 2, null),
                                NekkuClient.Item("31000011", 2, null),
                                NekkuClient.Item("31000012", 2, null),
                            ),
                            group.name,
                        )
                    ),
                    dryRun = false,
                ),
            ),
            client.orders,
        )
    }

    @Test
    fun `Nekku order items are not generated if child is absent`() {
        val monday = LocalDate.of(2025, 4, 14)
        val tuesday = LocalDate.of(2025, 4, 15)

        // First create all of the basic backgrounds like
        // Customer numbers
        val client =
            TestNekkuClient(
                customers = basicTestClientCustomers,
                nekkuProducts = nekkuProductsForOrder,
                specialDiets = listOf(getNekkuSpecialDiet()),
            )
        fetchAndUpdateNekkuCustomers(client, db, asyncJobRunner, now)
        // products
        fetchAndUpdateNekkuProducts(client, db)
        fetchAndUpdateNekkuSpecialDiets(client, db, asyncJobRunner, now)
        // Daycare with groups
        val area = DevCareArea()
        val daycare =
            DevDaycare(
                areaId = area.id,
                mealtimeBreakfast = TimeRange(LocalTime.of(8, 0), LocalTime.of(8, 20)),
                mealtimeLunch = TimeRange(LocalTime.of(11, 15), LocalTime.of(11, 45)),
                mealtimeSnack = TimeRange(LocalTime.of(13, 30), LocalTime.of(13, 50)),
            )
        val group = DevDaycareGroup(daycareId = daycare.id, nekkuCustomerNumber = "2501K6089")
        val employee = DevEmployee()

        // Children with placements in the group and they are not absent
        val child = DevPerson()

        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(group)
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
                            daycareGroupId = group.id,
                            startDate = monday,
                            endDate = tuesday,
                        )
                    )
                }
            listOf(
                    // Child is absent, so no meals on Monday
                    DevAbsence(
                        childId = child.id,
                        date = monday,
                        absenceType = AbsenceType.PLANNED_ABSENCE,
                        absenceCategory = AbsenceCategory.BILLABLE,
                        modifiedBy = employee.evakaUserId,
                        modifiedAt = HelsinkiDateTime.now(),
                    ),
                    // Child is absent, so no meals on Tuesday
                    DevAbsence(
                        childId = child.id,
                        date = tuesday,
                        absenceType = AbsenceType.PLANNED_ABSENCE,
                        absenceCategory = AbsenceCategory.BILLABLE,
                        modifiedBy = employee.evakaUserId,
                        modifiedAt = HelsinkiDateTime.now(),
                    ),
                )
                .forEach { tx.insert(it) }
        }

        createAndSendNekkuOrder(client, db, group.id, monday)
        createAndSendNekkuOrder(client, db, group.id, tuesday)

        assertOrdersListEquals(
            listOf(
                NekkuClient.NekkuOrders(
                    listOf(
                        NekkuClient.NekkuOrder(
                            monday.toString(),
                            "2501K6089",
                            group.id.toString(),
                            emptyList(),
                            group.name,
                        )
                    ),
                    dryRun = false,
                ),
                NekkuClient.NekkuOrders(
                    listOf(
                        NekkuClient.NekkuOrder(
                            tuesday.toString(),
                            "2501K6089",
                            group.id.toString(),
                            emptyList(),
                            group.name,
                        )
                    ),
                    dryRun = false,
                ),
            ),
            client.orders,
        )
    }

    @Test
    fun `Send Nekku orders with different size customers`() {
        val monday = LocalDate.of(2025, 4, 14)
        val tuesday = LocalDate.of(2025, 4, 15)

        // First create all of the basic backgrounds like
        // Customer numbers
        val client =
            TestNekkuClient(
                customers =
                    listOf(
                        NekkuApiCustomer(
                            "2501K6090",
                            "Apporan päiväkoti",
                            "Varhaiskasvatus",
                            listOf(
                                CustomerApiType(
                                    listOf(
                                        NekkuCustomerApiWeekday.MONDAY,
                                        NekkuCustomerApiWeekday.TUESDAY,
                                        NekkuCustomerApiWeekday.WEDNESDAY,
                                        NekkuCustomerApiWeekday.THURSDAY,
                                        NekkuCustomerApiWeekday.FRIDAY,
                                        NekkuCustomerApiWeekday.SATURDAY,
                                        NekkuCustomerApiWeekday.SUNDAY,
                                        NekkuCustomerApiWeekday.WEEKDAYHOLIDAY,
                                    ),
                                    "50-99-lasta",
                                )
                            ),
                        )
                    ),
                nekkuProducts = nekkuProductsForOrder,
                specialDiets = listOf(getNekkuSpecialDiet()),
            )
        fetchAndUpdateNekkuCustomers(client, db, asyncJobRunner, now)
        // products
        fetchAndUpdateNekkuProducts(client, db)
        fetchAndUpdateNekkuSpecialDiets(client, db, asyncJobRunner, now)
        // Daycare with groups
        val area = DevCareArea()
        val daycare =
            DevDaycare(
                areaId = area.id,
                mealtimeBreakfast = TimeRange(LocalTime.of(8, 0), LocalTime.of(8, 20)),
                mealtimeLunch = TimeRange(LocalTime.of(11, 15), LocalTime.of(11, 45)),
                mealtimeSnack = TimeRange(LocalTime.of(13, 30), LocalTime.of(13, 50)),
            )
        val group = DevDaycareGroup(daycareId = daycare.id, nekkuCustomerNumber = "2501K6090")
        val employee = DevEmployee()

        // Children with placements in the group and they are not absent
        val child = DevPerson()

        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(group)
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
                            daycareGroupId = group.id,
                            startDate = monday,
                            endDate = tuesday,
                        )
                    )
                }
            listOf(
                    // Two meals on Monday
                    DevReservation(
                        childId = child.id,
                        date = monday,
                        startTime = LocalTime.of(8, 0),
                        endTime = LocalTime.of(12, 0),
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
        }

        createAndSendNekkuOrder(client, db, group.id, monday)
        createAndSendNekkuOrder(client, db, group.id, tuesday)

        assertOrdersListEquals(
            listOf(
                NekkuClient.NekkuOrders(
                    listOf(
                        NekkuClient.NekkuOrder(
                            monday.toString(),
                            "2501K6090",
                            group.id.toString(),
                            listOf(NekkuClient.Item("31000018", 1, null)),
                            group.name,
                        )
                    ),
                    dryRun = false,
                ),
                NekkuClient.NekkuOrders(
                    listOf(
                        NekkuClient.NekkuOrder(
                            tuesday.toString(),
                            "2501K6090",
                            group.id.toString(),
                            listOf(NekkuClient.Item("31000018", 1, null)),
                            group.name,
                        )
                    ),
                    dryRun = false,
                ),
            ),
            client.orders,
        )
    }

    @Test
    fun `Order is not done if customer has not set a weekday in Nekku`() {
        val monday = LocalDate.of(2025, 4, 14)
        val tuesday = LocalDate.of(2025, 4, 15)

        // First create all of the basic backgrounds like
        // Customer numbers
        val client =
            TestNekkuClient(
                customers =
                    listOf(
                        NekkuApiCustomer(
                            "2501K6089",
                            "Ahvenojan päiväkoti",
                            "Varhaiskasvatus",
                            listOf(
                                CustomerApiType(
                                    listOf(NekkuCustomerApiWeekday.TUESDAY),
                                    "100-lasta",
                                )
                            ),
                        )
                    ),
                nekkuProducts = nekkuProductsForOrder,
                specialDiets = listOf(getNekkuSpecialDiet()),
            )
        fetchAndUpdateNekkuCustomers(client, db, asyncJobRunner, now)
        // products
        fetchAndUpdateNekkuProducts(client, db)
        fetchAndUpdateNekkuSpecialDiets(client, db, asyncJobRunner, now)
        // Daycare with groups
        val area = DevCareArea()
        val daycare =
            DevDaycare(
                areaId = area.id,
                mealtimeBreakfast = TimeRange(LocalTime.of(8, 0), LocalTime.of(8, 20)),
                mealtimeLunch = TimeRange(LocalTime.of(11, 15), LocalTime.of(11, 45)),
                mealtimeSnack = TimeRange(LocalTime.of(13, 30), LocalTime.of(13, 50)),
            )
        val group = DevDaycareGroup(daycareId = daycare.id, nekkuCustomerNumber = "2501K6089")
        val employee = DevEmployee()

        // Children with placements in the group and they are not absent
        val child = DevPerson()

        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(group)
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
                            daycareGroupId = group.id,
                            startDate = monday,
                            endDate = tuesday,
                        )
                    )
                }
            listOf(
                    // Two meals on Monday
                    DevReservation(
                        childId = child.id,
                        date = monday,
                        startTime = LocalTime.of(8, 0),
                        endTime = LocalTime.of(12, 0),
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
        }

        createAndSendNekkuOrder(client, db, group.id, monday)
        createAndSendNekkuOrder(client, db, group.id, tuesday)

        assertOrdersListEquals(
            listOf(
                NekkuClient.NekkuOrders(
                    listOf(
                        NekkuClient.NekkuOrder(
                            tuesday.toString(),
                            "2501K6089",
                            group.id.toString(),
                            listOf(NekkuClient.Item("31000010", 1, null)),
                            group.name,
                        )
                    ),
                    dryRun = false,
                )
            ),
            client.orders,
        )
    }

    @Test
    fun `Should order meals with special diets`() {
        val client =
            TestNekkuClient(
                customers = basicTestClientWithAnotherCustomerNumber,
                nekkuProducts = nekkuProductsForOrder,
                specialDiets = listOf(getNekkuSpecialDiet()),
            )

        fetchAndUpdateNekkuCustomers(client, db, asyncJobRunner, now)
        // products
        fetchAndUpdateNekkuProducts(client, db)
        fetchAndUpdateNekkuSpecialDiets(client, db, asyncJobRunner, now)

        val monday = LocalDate.of(2025, 4, 14)
        val tuesday = LocalDate.of(2025, 4, 15)

        // Daycare with groups
        val area = DevCareArea()
        val daycare =
            DevDaycare(
                areaId = area.id,
                mealtimeBreakfast = TimeRange(LocalTime.of(8, 0), LocalTime.of(8, 20)),
                mealtimeLunch = TimeRange(LocalTime.of(11, 15), LocalTime.of(11, 45)),
                mealtimeSnack = TimeRange(LocalTime.of(13, 30), LocalTime.of(13, 50)),
            )
        val group = DevDaycareGroup(daycareId = daycare.id, nekkuCustomerNumber = "2501K6090")
        val employee = DevEmployee()

        // Children with placements in the group and they are not absent
        // two children with no special diets
        // two children with mixed diet and special diets
        // one child with mixed diet and another special diet
        // two children with vegetable diet and special diets

        val mixedChild1 = DevPerson()
        val mixedChild2 = DevPerson()
        val mixedSpecial1Child1 = DevPerson()
        val mixedSpecial1Child2 = DevPerson()
        val mixedSpecial2Child1 = DevPerson()
        val vegetableSpecialChild1 = DevPerson()
        val vegetableSpecialChild2 = DevPerson()
        val allChildren =
            listOf(
                mixedChild1,
                mixedChild2,
                mixedSpecial1Child1,
                mixedSpecial1Child2,
                mixedSpecial2Child1,
                vegetableSpecialChild1,
                vegetableSpecialChild2,
            )

        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(group)
            tx.insert(employee)
            allChildren.forEach {
                tx.insert(it, DevPersonType.CHILD)
                tx.insert(
                        DevPlacement(
                            childId = it.id,
                            unitId = daycare.id,
                            startDate = monday,
                            endDate = tuesday,
                        )
                    )
                    .also { placementId ->
                        tx.insert(
                            DevDaycareGroupPlacement(
                                daycarePlacementId = placementId,
                                daycareGroupId = group.id,
                                startDate = monday,
                                endDate = tuesday,
                            )
                        )
                    }
                tx.insert(
                    // Three meals on Monday
                    DevReservation(
                        childId = it.id,
                        date = monday,
                        startTime = LocalTime.of(8, 0),
                        endTime = LocalTime.of(16, 0),
                        createdBy = employee.evakaUserId,
                    )
                )
            }

            tx.insert(
                DevChild(id = vegetableSpecialChild1.id, nekkuDiet = NekkuProductMealType.VEGETABLE)
            )
            tx.insert(
                DevChild(id = vegetableSpecialChild2.id, nekkuDiet = NekkuProductMealType.VEGETABLE)
            )
        }

        insertNekkuSpecialDietChoice(
            db,
            mixedSpecial1Child1.id,
            "2",
            "AE1FE5FE-9619-4D7A-9043-A6B0C615156B",
            "Laktoositon ruokavalio",
        )
        insertNekkuSpecialDietChoice(
            db,
            mixedSpecial1Child1.id,
            "2",
            "AE1FE5FE-9619-4D7A-9043-A6B0C615156B",
            "Luontaisesti gluteeniton ruokavalio",
        )
        insertNekkuSpecialDietChoice(
            db,
            mixedSpecial1Child2.id,
            "2",
            "AE1FE5FE-9619-4D7A-9043-A6B0C615156B",
            "Luontaisesti gluteeniton ruokavalio",
        )
        insertNekkuSpecialDietChoice(
            db,
            mixedSpecial1Child2.id,
            "2",
            "AE1FE5FE-9619-4D7A-9043-A6B0C615156B",
            "Laktoositon ruokavalio",
        )
        insertNekkuSpecialDietChoice(
            db,
            mixedSpecial2Child1.id,
            "2",
            "AE1FE5FE-9619-4D7A-9043-A6B0C615156B",
            "Kananmunaton ruokavalio",
        )
        insertNekkuSpecialDietChoice(
            db,
            vegetableSpecialChild1.id,
            "2",
            "17A9ACF0-DE9E-4C07-882E-C8C47351D009",
            "Pähkinätön",
        )
        insertNekkuSpecialDietChoice(
            db,
            vegetableSpecialChild2.id,
            "2",
            "17A9ACF0-DE9E-4C07-882E-C8C47351D009",
            "Pähkinätön",
        )

        createAndSendNekkuOrder(client, db, group.id, monday)

        assertOrdersListEquals(
            listOf(
                NekkuClient.NekkuOrders(
                    listOf(
                        NekkuClient.NekkuOrder(
                            monday.toString(),
                            "2501K6090",
                            group.id.toString(),
                            listOf(
                                NekkuClient.Item("31000010", 2, null),
                                NekkuClient.Item("31000011", 2, null),
                                NekkuClient.Item("31000012", 2, null),
                                NekkuClient.Item(
                                    "31000020",
                                    2,
                                    setOf(
                                        NekkuClient.ProductOption(
                                            "AE1FE5FE-9619-4D7A-9043-A6B0C615156B",
                                            "Luontaisesti gluteeniton ruokavalio",
                                        ),
                                        NekkuClient.ProductOption(
                                            "AE1FE5FE-9619-4D7A-9043-A6B0C615156B",
                                            "Laktoositon ruokavalio",
                                        ),
                                    ),
                                ),
                                NekkuClient.Item(
                                    "31000021",
                                    2,
                                    setOf(
                                        NekkuClient.ProductOption(
                                            "AE1FE5FE-9619-4D7A-9043-A6B0C615156B",
                                            "Luontaisesti gluteeniton ruokavalio",
                                        ),
                                        NekkuClient.ProductOption(
                                            "AE1FE5FE-9619-4D7A-9043-A6B0C615156B",
                                            "Laktoositon ruokavalio",
                                        ),
                                    ),
                                ),
                                NekkuClient.Item(
                                    "31000022",
                                    2,
                                    setOf(
                                        NekkuClient.ProductOption(
                                            "AE1FE5FE-9619-4D7A-9043-A6B0C615156B",
                                            "Luontaisesti gluteeniton ruokavalio",
                                        ),
                                        NekkuClient.ProductOption(
                                            "AE1FE5FE-9619-4D7A-9043-A6B0C615156B",
                                            "Laktoositon ruokavalio",
                                        ),
                                    ),
                                ),
                                NekkuClient.Item(
                                    "31000020",
                                    1,
                                    setOf(
                                        NekkuClient.ProductOption(
                                            "AE1FE5FE-9619-4D7A-9043-A6B0C615156B",
                                            "Kananmunaton ruokavalio",
                                        )
                                    ),
                                ),
                                NekkuClient.Item(
                                    "31000021",
                                    1,
                                    setOf(
                                        NekkuClient.ProductOption(
                                            "AE1FE5FE-9619-4D7A-9043-A6B0C615156B",
                                            "Kananmunaton ruokavalio",
                                        )
                                    ),
                                ),
                                NekkuClient.Item(
                                    "31000022",
                                    1,
                                    setOf(
                                        NekkuClient.ProductOption(
                                            "AE1FE5FE-9619-4D7A-9043-A6B0C615156B",
                                            "Kananmunaton ruokavalio",
                                        )
                                    ),
                                ),
                                NekkuClient.Item(
                                    "31000023",
                                    2,
                                    setOf(
                                        NekkuClient.ProductOption(
                                            "17A9ACF0-DE9E-4C07-882E-C8C47351D009",
                                            "Pähkinätön",
                                        )
                                    ),
                                ),
                                NekkuClient.Item(
                                    "31000024",
                                    2,
                                    setOf(
                                        NekkuClient.ProductOption(
                                            "17A9ACF0-DE9E-4C07-882E-C8C47351D009",
                                            "Pähkinätön",
                                        )
                                    ),
                                ),
                                NekkuClient.Item(
                                    "31000025",
                                    2,
                                    setOf(
                                        NekkuClient.ProductOption(
                                            "17A9ACF0-DE9E-4C07-882E-C8C47351D009",
                                            "Pähkinätön",
                                        )
                                    ),
                                ),
                            ),
                            group.name,
                        )
                    ),
                    dryRun = false,
                )
            ),
            client.orders,
        )
    }

    @Test
    fun `should have additional under one year old diet ordered for under one year olds`() {
        val client =
            TestNekkuClient(
                customers = basicTestClientWithAnotherCustomerNumber,
                nekkuProducts = nekkuProductsForOrder,
                specialDiets = listOf(getNekkuSpecialDiet()),
            )

        fetchAndUpdateNekkuCustomers(client, db, asyncJobRunner, now)
        // products
        fetchAndUpdateNekkuProducts(client, db)
        fetchAndUpdateNekkuSpecialDiets(client, db, asyncJobRunner, now)

        val monday = LocalDate.of(2025, 4, 14)
        val tuesday = LocalDate.of(2025, 4, 15)

        // Daycare with groups
        val area = DevCareArea()
        val daycare =
            DevDaycare(
                areaId = area.id,
                mealtimeBreakfast = TimeRange(LocalTime.of(8, 0), LocalTime.of(8, 20)),
                mealtimeLunch = TimeRange(LocalTime.of(11, 15), LocalTime.of(11, 45)),
                mealtimeSnack = TimeRange(LocalTime.of(13, 30), LocalTime.of(13, 50)),
            )
        val group = DevDaycareGroup(daycareId = daycare.id, nekkuCustomerNumber = "2501K6090")
        val employee = DevEmployee()

        val mixedBaby = DevPerson(dateOfBirth = monday.minusMonths(6))
        val mixedBabyWithoutLactose = DevPerson(dateOfBirth = monday.minusMonths(6))
        val mixedBabyWithOtherSpecialDiet = DevPerson(dateOfBirth = monday.minusMonths(6))
        val mixedBabyWithoutLactoseAndAnotherSpecialDiet =
            DevPerson(dateOfBirth = monday.minusMonths(6))
        val allChildren =
            listOf(
                mixedBaby,
                mixedBabyWithoutLactose,
                mixedBabyWithOtherSpecialDiet,
                mixedBabyWithoutLactoseAndAnotherSpecialDiet,
            )

        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(group)
            tx.insert(employee)
            allChildren.forEach {
                tx.insert(it, DevPersonType.CHILD)
                tx.insert(
                        DevPlacement(
                            childId = it.id,
                            unitId = daycare.id,
                            startDate = monday,
                            endDate = tuesday,
                        )
                    )
                    .also { placementId ->
                        tx.insert(
                            DevDaycareGroupPlacement(
                                daycarePlacementId = placementId,
                                daycareGroupId = group.id,
                                startDate = monday,
                                endDate = tuesday,
                            )
                        )
                    }
                tx.insert(
                    // Three meals on Monday
                    DevReservation(
                        childId = it.id,
                        date = monday,
                        startTime = LocalTime.of(8, 0),
                        endTime = LocalTime.of(16, 0),
                        createdBy = employee.evakaUserId,
                    )
                )
            }
        }

        insertNekkuSpecialDietChoice(
            db,
            mixedBabyWithoutLactose.id,
            "2",
            "AE1FE5FE-9619-4D7A-9043-A6B0C615156B",
            "Laktoositon ruokavalio",
        )
        insertNekkuSpecialDietChoice(
            db,
            mixedBabyWithOtherSpecialDiet.id,
            "2",
            "17A9ACF0-DE9E-4C07-882E-C8C47351D009",
            "Pähkinätön",
        )
        insertNekkuSpecialDietChoice(
            db,
            mixedBabyWithoutLactoseAndAnotherSpecialDiet.id,
            "2",
            "AE1FE5FE-9619-4D7A-9043-A6B0C615156B",
            "Laktoositon ruokavalio",
        )
        insertNekkuSpecialDietChoice(
            db,
            mixedBabyWithoutLactoseAndAnotherSpecialDiet.id,
            "2",
            "17A9ACF0-DE9E-4C07-882E-C8C47351D009",
            "Pähkinätön",
        )

        createAndSendNekkuOrder(client, db, group.id, monday)

        assertOrdersListEquals(
            listOf(
                NekkuClient.NekkuOrders(
                    listOf(
                        NekkuClient.NekkuOrder(
                            monday.toString(),
                            "2501K6090",
                            group.id.toString(),
                            listOf(
                                NekkuClient.Item(
                                    "31000020",
                                    1,
                                    setOf(
                                        NekkuClient.ProductOption(
                                            "17A9ACF0-DE9E-4C07-882E-C8C47351D009",
                                            "Alle 1-vuotiaan ruokavalio",
                                        )
                                    ),
                                ),
                                NekkuClient.Item(
                                    "31000021",
                                    1,
                                    setOf(
                                        NekkuClient.ProductOption(
                                            "17A9ACF0-DE9E-4C07-882E-C8C47351D009",
                                            "Alle 1-vuotiaan ruokavalio",
                                        )
                                    ),
                                ),
                                NekkuClient.Item(
                                    "31000022",
                                    1,
                                    setOf(
                                        NekkuClient.ProductOption(
                                            "17A9ACF0-DE9E-4C07-882E-C8C47351D009",
                                            "Alle 1-vuotiaan ruokavalio",
                                        )
                                    ),
                                ),
                                NekkuClient.Item(
                                    "31000020",
                                    1,
                                    setOf(
                                        NekkuClient.ProductOption(
                                            "17A9ACF0-DE9E-4C07-882E-C8C47351D009",
                                            "Alle 1-vuotiaan ruokavalio",
                                        ),
                                        NekkuClient.ProductOption(
                                            "AE1FE5FE-9619-4D7A-9043-A6B0C615156B",
                                            "Laktoositon ruokavalio",
                                        ),
                                    ),
                                ),
                                NekkuClient.Item(
                                    "31000021",
                                    1,
                                    setOf(
                                        NekkuClient.ProductOption(
                                            "17A9ACF0-DE9E-4C07-882E-C8C47351D009",
                                            "Alle 1-vuotiaan ruokavalio",
                                        ),
                                        NekkuClient.ProductOption(
                                            "AE1FE5FE-9619-4D7A-9043-A6B0C615156B",
                                            "Laktoositon ruokavalio",
                                        ),
                                    ),
                                ),
                                NekkuClient.Item(
                                    "31000022",
                                    1,
                                    setOf(
                                        NekkuClient.ProductOption(
                                            "17A9ACF0-DE9E-4C07-882E-C8C47351D009",
                                            "Alle 1-vuotiaan ruokavalio",
                                        ),
                                        NekkuClient.ProductOption(
                                            "AE1FE5FE-9619-4D7A-9043-A6B0C615156B",
                                            "Laktoositon ruokavalio",
                                        ),
                                    ),
                                ),
                                NekkuClient.Item(
                                    "31000020",
                                    1,
                                    setOf(
                                        NekkuClient.ProductOption(
                                            "17A9ACF0-DE9E-4C07-882E-C8C47351D009",
                                            "Pähkinätön, Alle 1-vuotiaan ruokavalio",
                                        )
                                    ),
                                ),
                                NekkuClient.Item(
                                    "31000021",
                                    1,
                                    setOf(
                                        NekkuClient.ProductOption(
                                            "17A9ACF0-DE9E-4C07-882E-C8C47351D009",
                                            "Pähkinätön, Alle 1-vuotiaan ruokavalio",
                                        )
                                    ),
                                ),
                                NekkuClient.Item(
                                    "31000022",
                                    1,
                                    setOf(
                                        NekkuClient.ProductOption(
                                            "17A9ACF0-DE9E-4C07-882E-C8C47351D009",
                                            "Pähkinätön, Alle 1-vuotiaan ruokavalio",
                                        )
                                    ),
                                ),
                                NekkuClient.Item(
                                    "31000020",
                                    1,
                                    setOf(
                                        NekkuClient.ProductOption(
                                            "17A9ACF0-DE9E-4C07-882E-C8C47351D009",
                                            "Pähkinätön, Alle 1-vuotiaan ruokavalio",
                                        ),
                                        NekkuClient.ProductOption(
                                            "AE1FE5FE-9619-4D7A-9043-A6B0C615156B",
                                            "Laktoositon ruokavalio",
                                        ),
                                    ),
                                ),
                                NekkuClient.Item(
                                    "31000021",
                                    1,
                                    setOf(
                                        NekkuClient.ProductOption(
                                            "17A9ACF0-DE9E-4C07-882E-C8C47351D009",
                                            "Pähkinätön, Alle 1-vuotiaan ruokavalio",
                                        ),
                                        NekkuClient.ProductOption(
                                            "AE1FE5FE-9619-4D7A-9043-A6B0C615156B",
                                            "Laktoositon ruokavalio",
                                        ),
                                    ),
                                ),
                                NekkuClient.Item(
                                    "31000022",
                                    1,
                                    setOf(
                                        NekkuClient.ProductOption(
                                            "17A9ACF0-DE9E-4C07-882E-C8C47351D009",
                                            "Pähkinätön, Alle 1-vuotiaan ruokavalio",
                                        ),
                                        NekkuClient.ProductOption(
                                            "AE1FE5FE-9619-4D7A-9043-A6B0C615156B",
                                            "Laktoositon ruokavalio",
                                        ),
                                    ),
                                ),
                            ),
                            group.name,
                        )
                    ),
                    dryRun = false,
                )
            ),
            client.orders,
        )
    }

    @Test
    fun `should skip meals for which no product is found`() {
        val client =
            TestNekkuClient(
                customers =
                    listOf(
                        NekkuApiCustomer(
                            "2501K6090",
                            "Ahvenojan päiväkoti",
                            "Varhaiskasvatus",
                            listOf(
                                CustomerApiType(
                                    listOf(
                                        NekkuCustomerApiWeekday.MONDAY,
                                        NekkuCustomerApiWeekday.TUESDAY,
                                        NekkuCustomerApiWeekday.WEDNESDAY,
                                        NekkuCustomerApiWeekday.THURSDAY,
                                        NekkuCustomerApiWeekday.FRIDAY,
                                        NekkuCustomerApiWeekday.SATURDAY,
                                        NekkuCustomerApiWeekday.SUNDAY,
                                        NekkuCustomerApiWeekday.WEEKDAYHOLIDAY,
                                    ),
                                    "palveluovelle",
                                )
                            ),
                        )
                    ),
                nekkuProducts =
                    listOf(
                        NekkuApiProduct(
                            "Palvelu ovelle lounas 1",
                            "310000030",
                            "",
                            listOf("palveluovelle"),
                            listOf(NekkuProductMealTime.LUNCH),
                        ),
                        NekkuApiProduct(
                            "Palvelu ovelle päivällinen 1",
                            "310000031",
                            "",
                            listOf("palveluovelle"),
                            listOf(NekkuProductMealTime.DINNER),
                        ),
                    ),
                specialDiets = listOf(getNekkuSpecialDiet()),
            )

        fetchAndUpdateNekkuCustomers(client, db, asyncJobRunner, now)
        // products
        fetchAndUpdateNekkuProducts(client, db)
        fetchAndUpdateNekkuSpecialDiets(client, db, asyncJobRunner, now)

        val monday = LocalDate.of(2025, 4, 14)

        // Daycare with groups
        val area = DevCareArea()
        val daycare =
            DevDaycare(
                areaId = area.id,
                mealtimeBreakfast = TimeRange(LocalTime.of(8, 0), LocalTime.of(8, 20)),
                mealtimeLunch = TimeRange(LocalTime.of(11, 15), LocalTime.of(11, 45)),
                mealtimeSnack = TimeRange(LocalTime.of(13, 30), LocalTime.of(13, 50)),
            )
        val group = DevDaycareGroup(daycareId = daycare.id, nekkuCustomerNumber = "2501K6090")
        val employee = DevEmployee()

        val child = DevPerson()

        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(group)
            tx.insert(employee)
            tx.insert(child, DevPersonType.CHILD)
            tx.insert(
                    DevPlacement(
                        childId = child.id,
                        unitId = daycare.id,
                        startDate = monday,
                        endDate = monday,
                    )
                )
                .also { placementId ->
                    tx.insert(
                        DevDaycareGroupPlacement(
                            daycarePlacementId = placementId,
                            daycareGroupId = group.id,
                            startDate = monday,
                            endDate = monday,
                        )
                    )
                }
            tx.insert(
                // Three meals on Monday
                DevReservation(
                    childId = child.id,
                    date = monday,
                    startTime = LocalTime.of(8, 0),
                    endTime = LocalTime.of(16, 0),
                    createdBy = employee.evakaUserId,
                )
            )
        }

        createAndSendNekkuOrder(client, db, group.id, monday)

        assertOrdersListEquals(
            listOf(
                NekkuClient.NekkuOrders(
                    listOf(
                        NekkuClient.NekkuOrder(
                            monday.toString(),
                            "2501K6090",
                            group.id.toString(),
                            listOf(NekkuClient.Item("310000030", 1, null)),
                            group.name,
                        )
                    ),
                    dryRun = false,
                )
            ),
            client.orders,
        )
    }

    @Test
    fun `Make sure that Nekku order is stored in database`() {

        val client =
            TestNekkuClient(
                customers = basicTestClientWithAnotherCustomerNumber,
                nekkuProducts = nekkuProductsForOrder,
                specialDiets = listOf(getNekkuSpecialDiet()),
            )

        fetchAndUpdateNekkuCustomers(client, db, asyncJobRunner, now)
        // products
        fetchAndUpdateNekkuProducts(client, db)
        fetchAndUpdateNekkuSpecialDiets(client, db, asyncJobRunner, now)

        val monday = LocalDate.of(2025, 4, 14)
        val tuesday = LocalDate.of(2025, 4, 15)

        // Daycare with groups
        val area = DevCareArea()
        val daycare =
            DevDaycare(
                areaId = area.id,
                mealtimeBreakfast = TimeRange(LocalTime.of(8, 0), LocalTime.of(8, 20)),
                mealtimeLunch = TimeRange(LocalTime.of(11, 15), LocalTime.of(11, 45)),
                mealtimeSnack = TimeRange(LocalTime.of(13, 30), LocalTime.of(13, 50)),
            )
        val group = DevDaycareGroup(daycareId = daycare.id, nekkuCustomerNumber = "2501K6090")
        val employee = DevEmployee()

        val mixedBaby = DevPerson(dateOfBirth = monday.minusMonths(6))
        val mixedBabyWithoutLactose = DevPerson(dateOfBirth = monday.minusMonths(6))
        val mixedBabyWithOtherSpecialDiet = DevPerson(dateOfBirth = monday.minusMonths(6))
        val mixedBabyWithoutLactoseAndAnotherSpecialDiet =
            DevPerson(dateOfBirth = monday.minusMonths(6))
        val allChildren =
            listOf(
                mixedBaby,
                mixedBabyWithoutLactose,
                mixedBabyWithOtherSpecialDiet,
                mixedBabyWithoutLactoseAndAnotherSpecialDiet,
            )

        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(group)
            tx.insert(employee)
            allChildren.forEach {
                tx.insert(it, DevPersonType.CHILD)
                tx.insert(
                        DevPlacement(
                            childId = it.id,
                            unitId = daycare.id,
                            startDate = monday,
                            endDate = tuesday,
                        )
                    )
                    .also { placementId ->
                        tx.insert(
                            DevDaycareGroupPlacement(
                                daycarePlacementId = placementId,
                                daycareGroupId = group.id,
                                startDate = monday,
                                endDate = tuesday,
                            )
                        )
                    }
                tx.insert(
                    // Three meals on Monday
                    DevReservation(
                        childId = it.id,
                        date = monday,
                        startTime = LocalTime.of(8, 0),
                        endTime = LocalTime.of(16, 0),
                        createdBy = employee.evakaUserId,
                    )
                )
            }
        }

        insertNekkuSpecialDietChoice(
            db,
            mixedBabyWithoutLactose.id,
            "2",
            "AE1FE5FE-9619-4D7A-9043-A6B0C615156B",
            "Laktoositon ruokavalio",
        )
        insertNekkuSpecialDietChoice(
            db,
            mixedBabyWithOtherSpecialDiet.id,
            "2",
            "17A9ACF0-DE9E-4C07-882E-C8C47351D009",
            "Pähkinätön",
        )
        insertNekkuSpecialDietChoice(
            db,
            mixedBabyWithoutLactoseAndAnotherSpecialDiet.id,
            "2",
            "AE1FE5FE-9619-4D7A-9043-A6B0C615156B",
            "Laktoositon ruokavalio",
        )
        insertNekkuSpecialDietChoice(
            db,
            mixedBabyWithoutLactoseAndAnotherSpecialDiet.id,
            "2",
            "17A9ACF0-DE9E-4C07-882E-C8C47351D009",
            "Pähkinätön",
        )

        createAndSendNekkuOrder(client, db, group.id, monday)

        db.transaction { tx ->
            val nekkuOrderReportResult = tx.getNekkuOrderReport(daycare.id, group.id, monday)

            assertEquals(
                setOf(
                    NekkuOrdersReport(
                        monday,
                        daycare.id,
                        group.id,
                        "31000020",
                        1,
                        listOf(NekkuProductMealTime.BREAKFAST),
                        null,
                        listOf("Alle 1-vuotiaan ruokavalio"),
                        "Luotu: [12345], Peruttu: [65432]",
                    ),
                    NekkuOrdersReport(
                        monday,
                        daycare.id,
                        group.id,
                        "31000021",
                        1,
                        listOf(NekkuProductMealTime.LUNCH),
                        null,
                        listOf("Alle 1-vuotiaan ruokavalio"),
                        "Luotu: [12345], Peruttu: [65432]",
                    ),
                    NekkuOrdersReport(
                        monday,
                        daycare.id,
                        group.id,
                        "31000022",
                        1,
                        listOf(NekkuProductMealTime.SNACK),
                        null,
                        listOf("Alle 1-vuotiaan ruokavalio"),
                        "Luotu: [12345], Peruttu: [65432]",
                    ),
                    NekkuOrdersReport(
                        monday,
                        daycare.id,
                        group.id,
                        "31000020",
                        1,
                        listOf(NekkuProductMealTime.BREAKFAST),
                        null,
                        listOf("Laktoositon ruokavalio", "Alle 1-vuotiaan ruokavalio"),
                        "Luotu: [12345], Peruttu: [65432]",
                    ),
                    NekkuOrdersReport(
                        monday,
                        daycare.id,
                        group.id,
                        "31000021",
                        1,
                        listOf(NekkuProductMealTime.LUNCH),
                        null,
                        listOf("Laktoositon ruokavalio", "Alle 1-vuotiaan ruokavalio"),
                        "Luotu: [12345], Peruttu: [65432]",
                    ),
                    NekkuOrdersReport(
                        monday,
                        daycare.id,
                        group.id,
                        "31000022",
                        1,
                        listOf(NekkuProductMealTime.SNACK),
                        null,
                        listOf("Laktoositon ruokavalio", "Alle 1-vuotiaan ruokavalio"),
                        "Luotu: [12345], Peruttu: [65432]",
                    ),
                    NekkuOrdersReport(
                        monday,
                        daycare.id,
                        group.id,
                        "31000020",
                        1,
                        listOf(NekkuProductMealTime.BREAKFAST),
                        null,
                        listOf("Pähkinätön, Alle 1-vuotiaan ruokavalio"),
                        "Luotu: [12345], Peruttu: [65432]",
                    ),
                    NekkuOrdersReport(
                        monday,
                        daycare.id,
                        group.id,
                        "31000021",
                        1,
                        listOf(NekkuProductMealTime.LUNCH),
                        null,
                        listOf("Pähkinätön, Alle 1-vuotiaan ruokavalio"),
                        "Luotu: [12345], Peruttu: [65432]",
                    ),
                    NekkuOrdersReport(
                        monday,
                        daycare.id,
                        group.id,
                        "31000022",
                        1,
                        listOf(NekkuProductMealTime.SNACK),
                        null,
                        listOf("Pähkinätön, Alle 1-vuotiaan ruokavalio"),
                        "Luotu: [12345], Peruttu: [65432]",
                    ),
                    NekkuOrdersReport(
                        monday,
                        daycare.id,
                        group.id,
                        "31000020",
                        1,
                        listOf(NekkuProductMealTime.BREAKFAST),
                        null,
                        listOf("Laktoositon ruokavalio", "Pähkinätön, Alle 1-vuotiaan ruokavalio"),
                        "Luotu: [12345], Peruttu: [65432]",
                    ),
                    NekkuOrdersReport(
                        monday,
                        daycare.id,
                        group.id,
                        "31000021",
                        1,
                        listOf(NekkuProductMealTime.LUNCH),
                        null,
                        listOf("Laktoositon ruokavalio", "Pähkinätön, Alle 1-vuotiaan ruokavalio"),
                        "Luotu: [12345], Peruttu: [65432]",
                    ),
                    NekkuOrdersReport(
                        monday,
                        daycare.id,
                        group.id,
                        "31000022",
                        1,
                        listOf(NekkuProductMealTime.SNACK),
                        null,
                        listOf("Laktoositon ruokavalio", "Pähkinätön, Alle 1-vuotiaan ruokavalio"),
                        "Luotu: [12345], Peruttu: [65432]",
                    ),
                ),
                nekkuOrderReportResult.toSet(),
            )
        }
    }

    @Test
    fun `Specifying order for next day removes old order and adds new order to database`() {

        val client =
            TestNekkuClient(
                customers = basicTestClientWithAnotherCustomerNumber,
                nekkuProducts = nekkuProductsForOrder,
                specialDiets = listOf(getNekkuSpecialDiet()),
            )

        fetchAndUpdateNekkuCustomers(client, db, asyncJobRunner, now)
        // products
        fetchAndUpdateNekkuProducts(client, db)
        fetchAndUpdateNekkuSpecialDiets(client, db, asyncJobRunner, now)

        val monday = LocalDate.of(2025, 4, 14)
        val tuesday = LocalDate.of(2025, 4, 15)

        // Daycare with groups
        val area = DevCareArea()
        val daycare =
            DevDaycare(
                areaId = area.id,
                mealtimeBreakfast = TimeRange(LocalTime.of(8, 0), LocalTime.of(8, 20)),
                mealtimeLunch = TimeRange(LocalTime.of(11, 15), LocalTime.of(11, 45)),
                mealtimeSnack = TimeRange(LocalTime.of(13, 30), LocalTime.of(13, 50)),
            )
        val group = DevDaycareGroup(daycareId = daycare.id, nekkuCustomerNumber = "2501K6090")
        val employee = DevEmployee()

        val mixedBaby = DevPerson(dateOfBirth = monday.minusMonths(6))
        val mixedBabyWithoutLactose = DevPerson(dateOfBirth = monday.minusMonths(6))
        val mixedBabyWithOtherSpecialDiet = DevPerson(dateOfBirth = monday.minusMonths(6))
        val mixedBabyWithoutLactoseAndAnotherSpecialDiet =
            DevPerson(dateOfBirth = monday.minusMonths(6))
        val allChildren =
            listOf(
                mixedBaby,
                mixedBabyWithoutLactose,
                mixedBabyWithOtherSpecialDiet,
                mixedBabyWithoutLactoseAndAnotherSpecialDiet,
            )

        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(group)
            tx.insert(employee)
            allChildren.forEach {
                tx.insert(it, DevPersonType.CHILD)
                tx.insert(
                        DevPlacement(
                            childId = it.id,
                            unitId = daycare.id,
                            startDate = monday,
                            endDate = tuesday,
                        )
                    )
                    .also { placementId ->
                        tx.insert(
                            DevDaycareGroupPlacement(
                                daycarePlacementId = placementId,
                                daycareGroupId = group.id,
                                startDate = monday,
                                endDate = tuesday,
                            )
                        )
                    }
                tx.insert(
                    // Three meals on Monday
                    DevReservation(
                        childId = it.id,
                        date = monday,
                        startTime = LocalTime.of(8, 0),
                        endTime = LocalTime.of(16, 0),
                        createdBy = employee.evakaUserId,
                    )
                )
            }
        }

        insertNekkuSpecialDietChoice(
            db,
            mixedBabyWithoutLactose.id,
            "2",
            "AE1FE5FE-9619-4D7A-9043-A6B0C615156B",
            "Laktoositon ruokavalio",
        )
        insertNekkuSpecialDietChoice(
            db,
            mixedBabyWithOtherSpecialDiet.id,
            "2",
            "17A9ACF0-DE9E-4C07-882E-C8C47351D009",
            "Pähkinätön",
        )
        insertNekkuSpecialDietChoice(
            db,
            mixedBabyWithoutLactoseAndAnotherSpecialDiet.id,
            "2",
            "AE1FE5FE-9619-4D7A-9043-A6B0C615156B",
            "Laktoositon ruokavalio",
        )
        insertNekkuSpecialDietChoice(
            db,
            mixedBabyWithoutLactoseAndAnotherSpecialDiet.id,
            "2",
            "17A9ACF0-DE9E-4C07-882E-C8C47351D009",
            "Pähkinätön",
        )

        createAndSendNekkuOrder(client, db, group.id, monday)

        db.transaction { tx ->
            val nekkuOrderReportResult = tx.getNekkuOrderReport(daycare.id, group.id, monday)

            assertEquals(
                setOf(
                    NekkuOrdersReport(
                        monday,
                        daycare.id,
                        group.id,
                        "31000020",
                        1,
                        listOf(NekkuProductMealTime.BREAKFAST),
                        null,
                        listOf("Alle 1-vuotiaan ruokavalio"),
                        "Luotu: [12345], Peruttu: [65432]",
                    ),
                    NekkuOrdersReport(
                        monday,
                        daycare.id,
                        group.id,
                        "31000021",
                        1,
                        listOf(NekkuProductMealTime.LUNCH),
                        null,
                        listOf("Alle 1-vuotiaan ruokavalio"),
                        "Luotu: [12345], Peruttu: [65432]",
                    ),
                    NekkuOrdersReport(
                        monday,
                        daycare.id,
                        group.id,
                        "31000022",
                        1,
                        listOf(NekkuProductMealTime.SNACK),
                        null,
                        listOf("Alle 1-vuotiaan ruokavalio"),
                        "Luotu: [12345], Peruttu: [65432]",
                    ),
                    NekkuOrdersReport(
                        monday,
                        daycare.id,
                        group.id,
                        "31000020",
                        1,
                        listOf(NekkuProductMealTime.BREAKFAST),
                        null,
                        listOf("Laktoositon ruokavalio", "Alle 1-vuotiaan ruokavalio"),
                        "Luotu: [12345], Peruttu: [65432]",
                    ),
                    NekkuOrdersReport(
                        monday,
                        daycare.id,
                        group.id,
                        "31000021",
                        1,
                        listOf(NekkuProductMealTime.LUNCH),
                        null,
                        listOf("Laktoositon ruokavalio", "Alle 1-vuotiaan ruokavalio"),
                        "Luotu: [12345], Peruttu: [65432]",
                    ),
                    NekkuOrdersReport(
                        monday,
                        daycare.id,
                        group.id,
                        "31000022",
                        1,
                        listOf(NekkuProductMealTime.SNACK),
                        null,
                        listOf("Laktoositon ruokavalio", "Alle 1-vuotiaan ruokavalio"),
                        "Luotu: [12345], Peruttu: [65432]",
                    ),
                    NekkuOrdersReport(
                        monday,
                        daycare.id,
                        group.id,
                        "31000020",
                        1,
                        listOf(NekkuProductMealTime.BREAKFAST),
                        null,
                        listOf("Pähkinätön, Alle 1-vuotiaan ruokavalio"),
                        "Luotu: [12345], Peruttu: [65432]",
                    ),
                    NekkuOrdersReport(
                        monday,
                        daycare.id,
                        group.id,
                        "31000021",
                        1,
                        listOf(NekkuProductMealTime.LUNCH),
                        null,
                        listOf("Pähkinätön, Alle 1-vuotiaan ruokavalio"),
                        "Luotu: [12345], Peruttu: [65432]",
                    ),
                    NekkuOrdersReport(
                        monday,
                        daycare.id,
                        group.id,
                        "31000022",
                        1,
                        listOf(NekkuProductMealTime.SNACK),
                        null,
                        listOf("Pähkinätön, Alle 1-vuotiaan ruokavalio"),
                        "Luotu: [12345], Peruttu: [65432]",
                    ),
                    NekkuOrdersReport(
                        monday,
                        daycare.id,
                        group.id,
                        "31000020",
                        1,
                        listOf(NekkuProductMealTime.BREAKFAST),
                        null,
                        listOf("Laktoositon ruokavalio", "Pähkinätön, Alle 1-vuotiaan ruokavalio"),
                        "Luotu: [12345], Peruttu: [65432]",
                    ),
                    NekkuOrdersReport(
                        monday,
                        daycare.id,
                        group.id,
                        "31000021",
                        1,
                        listOf(NekkuProductMealTime.LUNCH),
                        null,
                        listOf("Laktoositon ruokavalio", "Pähkinätön, Alle 1-vuotiaan ruokavalio"),
                        "Luotu: [12345], Peruttu: [65432]",
                    ),
                    NekkuOrdersReport(
                        monday,
                        daycare.id,
                        group.id,
                        "31000022",
                        1,
                        listOf(NekkuProductMealTime.SNACK),
                        null,
                        listOf("Laktoositon ruokavalio", "Pähkinätön, Alle 1-vuotiaan ruokavalio"),
                        "Luotu: [12345], Peruttu: [65432]",
                    ),
                ),
                nekkuOrderReportResult.toSet(),
            )
        }

        // create specifying order
        // Daycare with groups

        val removedChildren =
            listOf(mixedBabyWithOtherSpecialDiet, mixedBabyWithoutLactoseAndAnotherSpecialDiet)

        db.transaction { tx ->
            removedChildren
                .flatMap {
                    listOf(
                        // Child is absent, so no meals on Monday
                        DevAbsence(
                            childId = it.id,
                            date = monday,
                            absenceType = AbsenceType.PLANNED_ABSENCE,
                            absenceCategory = AbsenceCategory.BILLABLE,
                            modifiedBy = employee.evakaUserId,
                            modifiedAt = HelsinkiDateTime.now(),
                        ),
                        // Child is absent, so no meals on Tuesday
                        DevAbsence(
                            childId = it.id,
                            date = tuesday,
                            absenceType = AbsenceType.PLANNED_ABSENCE,
                            absenceCategory = AbsenceCategory.BILLABLE,
                            modifiedBy = employee.evakaUserId,
                            modifiedAt = HelsinkiDateTime.now(),
                        ),
                    )
                }
                .forEach { tx.insert(it) }
        }

        insertNekkuSpecialDietChoice(
            db,
            mixedBabyWithoutLactose.id,
            "2",
            "AE1FE5FE-9619-4D7A-9043-A6B0C615156B",
            "Laktoositon ruokavalio",
        )

        createAndSendNekkuOrder(client, db, group.id, monday)

        db.transaction { tx ->
            val nekkuOrderReportResult = tx.getNekkuOrderReport(daycare.id, group.id, monday)

            assertEquals(
                setOf(
                    NekkuOrdersReport(
                        monday,
                        daycare.id,
                        group.id,
                        "31000020",
                        1,
                        listOf(NekkuProductMealTime.BREAKFAST),
                        null,
                        listOf("Alle 1-vuotiaan ruokavalio"),
                        "Luotu: [12345], Peruttu: [65432]",
                    ),
                    NekkuOrdersReport(
                        monday,
                        daycare.id,
                        group.id,
                        "31000021",
                        1,
                        listOf(NekkuProductMealTime.LUNCH),
                        null,
                        listOf("Alle 1-vuotiaan ruokavalio"),
                        "Luotu: [12345], Peruttu: [65432]",
                    ),
                    NekkuOrdersReport(
                        monday,
                        daycare.id,
                        group.id,
                        "31000022",
                        1,
                        listOf(NekkuProductMealTime.SNACK),
                        null,
                        listOf("Alle 1-vuotiaan ruokavalio"),
                        "Luotu: [12345], Peruttu: [65432]",
                    ),
                    NekkuOrdersReport(
                        monday,
                        daycare.id,
                        group.id,
                        "31000020",
                        1,
                        listOf(NekkuProductMealTime.BREAKFAST),
                        null,
                        listOf("Laktoositon ruokavalio", "Alle 1-vuotiaan ruokavalio"),
                        "Luotu: [12345], Peruttu: [65432]",
                    ),
                    NekkuOrdersReport(
                        monday,
                        daycare.id,
                        group.id,
                        "31000021",
                        1,
                        listOf(NekkuProductMealTime.LUNCH),
                        null,
                        listOf("Laktoositon ruokavalio", "Alle 1-vuotiaan ruokavalio"),
                        "Luotu: [12345], Peruttu: [65432]",
                    ),
                    NekkuOrdersReport(
                        monday,
                        daycare.id,
                        group.id,
                        "31000022",
                        1,
                        listOf(NekkuProductMealTime.SNACK),
                        null,
                        listOf("Laktoositon ruokavalio", "Alle 1-vuotiaan ruokavalio"),
                        "Luotu: [12345], Peruttu: [65432]",
                    ),
                ),
                nekkuOrderReportResult.toSet(),
            )
        }
    }

    @Test
    fun `should be able to read report rows when an error has been reported`() {

        val area = DevCareArea()
        val daycare = DevDaycare(areaId = area.id)
        val group = DevDaycareGroup(daycareId = daycare.id)

        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(group)
            tx.setNekkuReportOrderErrorReport(group.id, LocalDate.now(), "Test error")

            tx.getNekkuReportRows(daycare.id, listOf(group.id), listOf(LocalDate.now()))
        }
    }
}
