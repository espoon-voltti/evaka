// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.nekku

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.TimeRange
import java.time.LocalDate
import java.time.LocalTime
import java.util.*
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired

class NekkuManualOrderIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var asyncJobRunner: AsyncJobRunner<AsyncJob>

    @Test
    fun `should throw if a manual order is attempted for today, in the past or after current automatic orders`() {

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
                                    "100-lasta",
                                )
                            ),
                        )
                    ),
                nekkuProducts = nekkuProductsForOrder,
                specialDiets = listOf(getNekkuSpecialDiet()),
            )

        fetchAndUpdateNekkuCustomers(client, db, asyncJobRunner, now)

        val area = DevCareArea()
        val daycare =
            DevDaycare(
                areaId = area.id,
                mealtimeBreakfast = TimeRange(LocalTime.of(8, 0), LocalTime.of(8, 20)),
                mealtimeLunch = TimeRange(LocalTime.of(11, 15), LocalTime.of(11, 45)),
                mealtimeSnack = TimeRange(LocalTime.of(13, 30), LocalTime.of(13, 50)),
            )
        val group = DevDaycareGroup(daycareId = daycare.id, nekkuCustomerNumber = "2501K6090")

        val now = HelsinkiDateTime.of(LocalDate.of(2025, 5, 7), LocalTime.of(12, 34, 52))
        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(group)

            assertThrows<BadRequest> {
                planNekkuManualOrderJob(
                    tx,
                    asyncJobRunner,
                    now,
                    group.id,
                    now.toLocalDate().minusDays(1),
                )
            }

            assertThrows<BadRequest> {
                planNekkuManualOrderJob(tx, asyncJobRunner, now, group.id, now.toLocalDate())
            }

            assertThrows<BadRequest> {
                planNekkuManualOrderJob(tx, asyncJobRunner, now, group.id, LocalDate.of(2025, 6, 2))
            }
        }
    }

    @Test
    fun `should throw when manual order is attempted if group ID is invalid`() {

        val now = HelsinkiDateTime.of(LocalDate.of(2025, 5, 7), LocalTime.of(12, 34, 52))

        db.transaction { tx ->
            assertThrows<BadRequest> {
                planNekkuManualOrderJob(
                    tx,
                    asyncJobRunner,
                    now,
                    GroupId(UUID.randomUUID()),
                    now.toLocalDate().plusDays(1),
                )
            }
        }
    }

    @Test
    fun `should throw when manual order is attempted if the requested date is not an operation day for the group`() {

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
                                    "100-lasta",
                                )
                            ),
                        )
                    ),
                nekkuProducts = nekkuProductsForOrder,
                specialDiets = listOf(getNekkuSpecialDiet()),
            )

        fetchAndUpdateNekkuCustomers(client, db, asyncJobRunner, now)

        val area = DevCareArea()
        val daycare =
            DevDaycare(
                areaId = area.id,
                mealtimeBreakfast = TimeRange(LocalTime.of(8, 0), LocalTime.of(8, 20)),
                mealtimeLunch = TimeRange(LocalTime.of(11, 15), LocalTime.of(11, 45)),
                mealtimeSnack = TimeRange(LocalTime.of(13, 30), LocalTime.of(13, 50)),
            )
        val group = DevDaycareGroup(daycareId = daycare.id, nekkuCustomerNumber = "2501K6090")

        val now = HelsinkiDateTime.of(LocalDate.of(2025, 5, 7), LocalTime.of(12, 34, 52))

        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(group)

            assertThrows<BadRequest> {
                planNekkuManualOrderJob(
                    tx,
                    asyncJobRunner,
                    now,
                    group.id,
                    LocalDate.of(2025, 5, 11),
                )
            }
        }
    }

    @Test
    fun `should throw when manual order is attempted if group has no customer number`() {

        val area = DevCareArea()
        val daycare =
            DevDaycare(
                areaId = area.id,
                mealtimeBreakfast = TimeRange(LocalTime.of(8, 0), LocalTime.of(8, 20)),
                mealtimeLunch = TimeRange(LocalTime.of(11, 15), LocalTime.of(11, 45)),
                mealtimeSnack = TimeRange(LocalTime.of(13, 30), LocalTime.of(13, 50)),
            )
        val group = DevDaycareGroup(daycareId = daycare.id, nekkuCustomerNumber = null)

        val now = HelsinkiDateTime.of(LocalDate.of(2025, 5, 7), LocalTime.of(12, 34, 52))

        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(group)

            assertThrows<BadRequest> {
                planNekkuManualOrderJob(
                    tx,
                    asyncJobRunner,
                    now,
                    group.id,
                    now.toLocalDate().plusDays(1),
                )
            }
        }
    }

    @Test
    fun `should throw when manual order is attempted if group or unit has been closed`() {

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
                                    "100-lasta",
                                )
                            ),
                        ),
                        NekkuApiCustomer(
                            "2501K6091",
                            "Haukipuron päiväkoti",
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
                                    "100-lasta",
                                )
                            ),
                        ),
                    ),
                nekkuProducts = nekkuProductsForOrder,
                specialDiets = listOf(getNekkuSpecialDiet()),
            )

        fetchAndUpdateNekkuCustomers(client, db, asyncJobRunner, now)

        val area = DevCareArea()
        val closedDaycare =
            DevDaycare(
                areaId = area.id,
                mealtimeBreakfast = TimeRange(LocalTime.of(8, 0), LocalTime.of(8, 20)),
                mealtimeLunch = TimeRange(LocalTime.of(11, 15), LocalTime.of(11, 45)),
                mealtimeSnack = TimeRange(LocalTime.of(13, 30), LocalTime.of(13, 50)),
                openingDate = LocalDate.of(2024, 1, 2),
                closingDate = LocalDate.of(2025, 5, 1),
            )
        val groupInClosedDaycare =
            DevDaycareGroup(daycareId = closedDaycare.id, nekkuCustomerNumber = "2501K6090")
        val openDaycare =
            DevDaycare(
                areaId = area.id,
                mealtimeBreakfast = TimeRange(LocalTime.of(8, 0), LocalTime.of(8, 20)),
                mealtimeLunch = TimeRange(LocalTime.of(11, 15), LocalTime.of(11, 45)),
                mealtimeSnack = TimeRange(LocalTime.of(13, 30), LocalTime.of(13, 50)),
            )
        val closedGroupInOpenDaycare =
            DevDaycareGroup(
                daycareId = openDaycare.id,
                nekkuCustomerNumber = "2501K6091",
                endDate = LocalDate.of(2025, 5, 1),
            )

        val now = HelsinkiDateTime.of(LocalDate.of(2025, 5, 7), LocalTime.of(12, 34, 52))

        db.transaction { tx ->
            tx.insert(area)
            tx.insert(closedDaycare)
            tx.insert(openDaycare)
            tx.insert(groupInClosedDaycare)
            tx.insert(closedGroupInOpenDaycare)

            assertThrows<BadRequest> {
                planNekkuManualOrderJob(
                    tx,
                    asyncJobRunner,
                    now,
                    groupInClosedDaycare.id,
                    now.toLocalDate().plusDays(1),
                )
            }

            assertThrows<BadRequest> {
                planNekkuManualOrderJob(
                    tx,
                    asyncJobRunner,
                    now,
                    closedGroupInOpenDaycare.id,
                    now.toLocalDate().plusDays(1),
                )
            }
        }
    }

    @Test
    fun `manual order should plan weekly order if daycare times have not been locked for the order date`() {

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
                                    "100-lasta",
                                )
                            ),
                        )
                    ),
                nekkuProducts = nekkuProductsForOrder,
                specialDiets = listOf(getNekkuSpecialDiet()),
            )

        fetchAndUpdateNekkuCustomers(client, db, asyncJobRunner, now)

        val area = DevCareArea()
        val daycare =
            DevDaycare(
                areaId = area.id,
                mealtimeBreakfast = TimeRange(LocalTime.of(8, 0), LocalTime.of(8, 20)),
                mealtimeLunch = TimeRange(LocalTime.of(11, 15), LocalTime.of(11, 45)),
                mealtimeSnack = TimeRange(LocalTime.of(13, 30), LocalTime.of(13, 50)),
            )
        val group = DevDaycareGroup(daycareId = daycare.id, nekkuCustomerNumber = "2501K6090")

        val now = HelsinkiDateTime.of(LocalDate.of(2025, 5, 7), LocalTime.of(12, 34, 52))

        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(group)

            planNekkuManualOrderJob(tx, asyncJobRunner, now, group.id, LocalDate.of(2025, 5, 20))
        }

        assertEquals(1, getNekkuJobs(db).size)
    }

    @Test
    fun `manual order should plan daily order if daycare times have been locked for the order date`() {

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
                                    "100-lasta",
                                )
                            ),
                        )
                    ),
                nekkuProducts = nekkuProductsForOrder,
                specialDiets = listOf(getNekkuSpecialDiet()),
            )

        fetchAndUpdateNekkuCustomers(client, db, asyncJobRunner, now)

        val area = DevCareArea()
        val daycare =
            DevDaycare(
                areaId = area.id,
                mealtimeBreakfast = TimeRange(LocalTime.of(8, 0), LocalTime.of(8, 20)),
                mealtimeLunch = TimeRange(LocalTime.of(11, 15), LocalTime.of(11, 45)),
                mealtimeSnack = TimeRange(LocalTime.of(13, 30), LocalTime.of(13, 50)),
            )
        val group = DevDaycareGroup(daycareId = daycare.id, nekkuCustomerNumber = "2501K6090")

        val now = HelsinkiDateTime.of(LocalDate.of(2025, 5, 7), LocalTime.of(12, 34, 52))

        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(group)

            planNekkuManualOrderJob(tx, asyncJobRunner, now, group.id, LocalDate.of(2025, 5, 14))
        }

        assertEquals(1, getNekkuJobs(db).size)
    }
}
