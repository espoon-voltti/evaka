// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.nekku

import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.kotlin.readValue
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.absence.AbsenceCategory
import fi.espoo.evaka.absence.AbsenceType
import fi.espoo.evaka.daycare.getDaycareGroup
import fi.espoo.evaka.emailclient.MockEmailClient
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
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
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.shared.domain.TimeRange
import java.time.LocalDate
import java.time.LocalTime
import java.util.*
import kotlin.collections.List
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired

class NekkuIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var asyncJobRunner: AsyncJobRunner<AsyncJob>

    @Autowired private lateinit var nekkuController: NekkuController

    private val now = HelsinkiDateTime.of(LocalDate.of(2025, 5, 12), LocalTime.of(9, 50))

    @Test
    fun `Nekku customer sync does not sync empty data`() {
        val client = TestNekkuClient()
        assertThrows<Exception> { fetchAndUpdateNekkuCustomers(client, db, asyncJobRunner, now) }
    }

    @Test
    fun `Nekku customer sync does sync non-empty data`() {
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
                    )
            )
        fetchAndUpdateNekkuCustomers(client, db, asyncJobRunner, now)
        db.transaction { tx ->
            val customers = tx.getNekkuCustomers()
            assertEquals(1, customers.size)
        }
    }

    @Test
    fun `Nekku customer sync with multiple customer types adds every type to database`() {
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
                                        NekkuCustomerApiWeekday.MONDAY,
                                        NekkuCustomerApiWeekday.TUESDAY,
                                        NekkuCustomerApiWeekday.WEDNESDAY,
                                        NekkuCustomerApiWeekday.THURSDAY,
                                        NekkuCustomerApiWeekday.FRIDAY,
                                    ),
                                    "100-lasta",
                                ),
                                CustomerApiType(
                                    listOf(
                                        NekkuCustomerApiWeekday.SATURDAY,
                                        NekkuCustomerApiWeekday.SUNDAY,
                                        NekkuCustomerApiWeekday.WEEKDAYHOLIDAY,
                                    ),
                                    "Palvelu-Ovelle-viikonloppu",
                                ),
                            ),
                        ),
                        NekkuApiCustomer(
                            "2501K0000",
                            "Ylikylän päiväkoti",
                            "Varhaiskasvatus",
                            listOf(
                                CustomerApiType(
                                    listOf(
                                        NekkuCustomerApiWeekday.MONDAY,
                                        NekkuCustomerApiWeekday.TUESDAY,
                                        NekkuCustomerApiWeekday.WEDNESDAY,
                                        NekkuCustomerApiWeekday.THURSDAY,
                                        NekkuCustomerApiWeekday.FRIDAY,
                                    ),
                                    "100-lasta",
                                ),
                                CustomerApiType(
                                    listOf(
                                        NekkuCustomerApiWeekday.SATURDAY,
                                        NekkuCustomerApiWeekday.SUNDAY,
                                        NekkuCustomerApiWeekday.WEEKDAYHOLIDAY,
                                    ),
                                    "Palvelu-Ovelle-viikonloppu",
                                ),
                            ),
                        ),
                    )
            )
        fetchAndUpdateNekkuCustomers(client, db, asyncJobRunner, now)
        db.transaction { tx ->
            val customers = tx.getNekkuCustomers()
            assertEquals(
                listOf(
                    NekkuCustomer(
                        number = "2501K0000",
                        name = "Ylikylän päiväkoti",
                        group = "Varhaiskasvatus",
                        listOf(
                            CustomerType(
                                listOf(
                                    NekkuCustomerWeekday.MONDAY,
                                    NekkuCustomerWeekday.TUESDAY,
                                    NekkuCustomerWeekday.WEDNESDAY,
                                    NekkuCustomerWeekday.THURSDAY,
                                    NekkuCustomerWeekday.FRIDAY,
                                ),
                                "100-lasta",
                            ),
                            CustomerType(
                                listOf(
                                    NekkuCustomerWeekday.SATURDAY,
                                    NekkuCustomerWeekday.SUNDAY,
                                    NekkuCustomerWeekday.WEEKDAYHOLIDAY,
                                ),
                                "Palvelu-Ovelle-viikonloppu",
                            ),
                        ),
                    ),
                    NekkuCustomer(
                        number = "2501K6089",
                        name = "Ahvenojan päiväkoti",
                        group = "Varhaiskasvatus",
                        listOf(
                            CustomerType(
                                listOf(
                                    NekkuCustomerWeekday.MONDAY,
                                    NekkuCustomerWeekday.TUESDAY,
                                    NekkuCustomerWeekday.WEDNESDAY,
                                    NekkuCustomerWeekday.THURSDAY,
                                    NekkuCustomerWeekday.FRIDAY,
                                ),
                                "100-lasta",
                            ),
                            CustomerType(
                                listOf(
                                    NekkuCustomerWeekday.SATURDAY,
                                    NekkuCustomerWeekday.SUNDAY,
                                    NekkuCustomerWeekday.WEEKDAYHOLIDAY,
                                ),
                                "Palvelu-Ovelle-viikonloppu",
                            ),
                        ),
                    ),
                ),
                customers,
            )
        }
    }

    @Test
    fun `Nekku customer sync with different customer types and same weekdays does sync`() {
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
                                        NekkuCustomerApiWeekday.MONDAY,
                                        NekkuCustomerApiWeekday.TUESDAY,
                                        NekkuCustomerApiWeekday.WEDNESDAY,
                                        NekkuCustomerApiWeekday.THURSDAY,
                                        NekkuCustomerApiWeekday.FRIDAY,
                                    ),
                                    "100-lasta",
                                ),
                                CustomerApiType(
                                    listOf(
                                        NekkuCustomerApiWeekday.MONDAY,
                                        NekkuCustomerApiWeekday.TUESDAY,
                                        NekkuCustomerApiWeekday.WEDNESDAY,
                                        NekkuCustomerApiWeekday.THURSDAY,
                                        NekkuCustomerApiWeekday.FRIDAY,
                                    ),
                                    "palvelu-ovelle-arki",
                                ),
                                CustomerApiType(
                                    listOf(
                                        NekkuCustomerApiWeekday.SATURDAY,
                                        NekkuCustomerApiWeekday.SUNDAY,
                                        NekkuCustomerApiWeekday.WEEKDAYHOLIDAY,
                                    ),
                                    "Palvelu-Ovelle-viikonloppu",
                                ),
                            ),
                        )
                    )
            )
        fetchAndUpdateNekkuCustomers(client, db, asyncJobRunner, now)
        db.transaction { tx ->
            val customers = tx.getNekkuCustomers()
            assertEquals(
                listOf(
                    NekkuCustomer(
                        number = "2501K6089",
                        name = "Ahvenojan päiväkoti",
                        group = "Varhaiskasvatus",
                        listOf(
                            CustomerType(
                                listOf(
                                    NekkuCustomerWeekday.MONDAY,
                                    NekkuCustomerWeekday.TUESDAY,
                                    NekkuCustomerWeekday.WEDNESDAY,
                                    NekkuCustomerWeekday.THURSDAY,
                                    NekkuCustomerWeekday.FRIDAY,
                                ),
                                "100-lasta",
                            ),
                            CustomerType(
                                listOf(
                                    NekkuCustomerWeekday.MONDAY,
                                    NekkuCustomerWeekday.TUESDAY,
                                    NekkuCustomerWeekday.WEDNESDAY,
                                    NekkuCustomerWeekday.THURSDAY,
                                    NekkuCustomerWeekday.FRIDAY,
                                ),
                                "palvelu-ovelle-arki",
                            ),
                            CustomerType(
                                listOf(
                                    NekkuCustomerWeekday.SATURDAY,
                                    NekkuCustomerWeekday.SUNDAY,
                                    NekkuCustomerWeekday.WEEKDAYHOLIDAY,
                                ),
                                "Palvelu-Ovelle-viikonloppu",
                            ),
                        ),
                    )
                ),
                customers,
            )
        }
    }

    @Test
    fun `Nekku customer lists only 'Varhaiskasvatus'-data`() {
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
                            "4282K9253",
                            "Haukiputaan lukio lipa",
                            "Liikunta",
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
                                    "palvelu-ovelle-viikonloppu-ja-arkipyhat",
                                )
                            ),
                        ),
                    )
            )
        fetchAndUpdateNekkuCustomers(client, db, asyncJobRunner, now)
        db.transaction { tx ->
            val customers = tx.getNekkuCustomers().toSet()
            assertEquals(1, customers.size)
        }
    }

    @Test
    fun `Nekku customer updates name and unit_size`() {
        var client =
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
                    )
            )
        fetchAndUpdateNekkuCustomers(client, db, asyncJobRunner, now)

        db.transaction { tx ->
            val customers = tx.getNekkuCustomers().toSet()

            assertEquals(1, customers.size)
            assertEquals("Ahvenojan päiväkoti", customers.first().name)
            assertEquals("100-lasta", customers.first().customerType.first().type)
            assertEquals("Varhaiskasvatus", customers.first().group)
        }

        client =
            TestNekkuClient(
                customers =
                    listOf(
                        NekkuApiCustomer(
                            "2501K6089",
                            "Ahvenojan päiväkoti MUUTETTU",
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
                                    "alle 50-lasta",
                                )
                            ),
                        )
                    )
            )
        fetchAndUpdateNekkuCustomers(client, db, asyncJobRunner, now)
        db.transaction { tx ->
            val customers = tx.getNekkuCustomers().toSet()

            assertEquals(1, customers.size)
            assertEquals("Ahvenojan päiväkoti MUUTETTU", customers.first().name)
            assertEquals("alle 50-lasta", customers.first().customerType.first().type)
        }
    }

    @Test
    fun `Nekku customer syncs nullifies removed customer numbers from groups`() {

        var client =
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
                                        NekkuCustomerApiWeekday.MONDAY,
                                        NekkuCustomerApiWeekday.TUESDAY,
                                        NekkuCustomerApiWeekday.WEDNESDAY,
                                        NekkuCustomerApiWeekday.THURSDAY,
                                        NekkuCustomerApiWeekday.FRIDAY,
                                        NekkuCustomerApiWeekday.SATURDAY,
                                        NekkuCustomerApiWeekday.SUNDAY,
                                        NekkuCustomerApiWeekday.WEEKDAYHOLIDAY,
                                    ),
                                    "alle 50-lasta",
                                )
                            ),
                        ),
                        NekkuApiCustomer(
                            "2501K6090",
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
                                    "alle 50-lasta",
                                )
                            ),
                        ),
                        NekkuApiCustomer(
                            "2501K6091",
                            "Käteisvirran päiväkoti",
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
                                    "alle 50-lasta",
                                )
                            ),
                        ),
                    )
            )
        fetchAndUpdateNekkuCustomers(client, db, asyncJobRunner, now)

        val area = DevCareArea()
        val daycare1 = DevDaycare(areaId = area.id)
        val daycare2 = DevDaycare(areaId = area.id)
        val daycare3 = DevDaycare(areaId = area.id)
        val group1 = DevDaycareGroup(daycareId = daycare1.id, nekkuCustomerNumber = "2501K6089")
        val group2 = DevDaycareGroup(daycareId = daycare2.id, nekkuCustomerNumber = "2501K6090")
        val group3 = DevDaycareGroup(daycareId = daycare2.id, nekkuCustomerNumber = "2501K6090")
        val group4 = DevDaycareGroup(daycareId = daycare3.id, nekkuCustomerNumber = "2501K6091")

        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare1)
            tx.insert(daycare2)
            tx.insert(daycare3)
            tx.insert(group1)
            tx.insert(group2)
            tx.insert(group3)
            tx.insert(group4)
        }

        client =
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
                                        NekkuCustomerApiWeekday.MONDAY,
                                        NekkuCustomerApiWeekday.TUESDAY,
                                        NekkuCustomerApiWeekday.WEDNESDAY,
                                        NekkuCustomerApiWeekday.THURSDAY,
                                        NekkuCustomerApiWeekday.FRIDAY,
                                        NekkuCustomerApiWeekday.SATURDAY,
                                        NekkuCustomerApiWeekday.SUNDAY,
                                        NekkuCustomerApiWeekday.WEEKDAYHOLIDAY,
                                    ),
                                    "alle 50-lasta",
                                )
                            ),
                        )
                    )
            )
        fetchAndUpdateNekkuCustomers(client, db, asyncJobRunner, now)

        db.read { tx ->
            assertEquals("2501K6089", tx.getDaycareGroup(group1.id)!!.nekkuCustomerNumber)
            assertNull((tx.getDaycareGroup(group2.id))!!.nekkuCustomerNumber)
            assertNull((tx.getDaycareGroup(group3.id))!!.nekkuCustomerNumber)
            assertNull((tx.getDaycareGroup(group4.id))!!.nekkuCustomerNumber)
        }
    }

    @Test
    fun `nullifying customer numbers from groups generates warning emails`() {
        var client =
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
                                        NekkuCustomerApiWeekday.MONDAY,
                                        NekkuCustomerApiWeekday.TUESDAY,
                                        NekkuCustomerApiWeekday.WEDNESDAY,
                                        NekkuCustomerApiWeekday.THURSDAY,
                                        NekkuCustomerApiWeekday.FRIDAY,
                                        NekkuCustomerApiWeekday.SATURDAY,
                                        NekkuCustomerApiWeekday.SUNDAY,
                                        NekkuCustomerApiWeekday.WEEKDAYHOLIDAY,
                                    ),
                                    "alle 50-lasta",
                                )
                            ),
                        ),
                        NekkuApiCustomer(
                            "2501K6090",
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
                                    "alle 50-lasta",
                                )
                            ),
                        ),
                        NekkuApiCustomer(
                            "2501K6091",
                            "Käteisvirran päiväkoti",
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
                                    "alle 50-lasta",
                                )
                            ),
                        ),
                    )
            )
        fetchAndUpdateNekkuCustomers(client, db, asyncJobRunner, now)

        val area = DevCareArea()
        val daycare1 = DevDaycare(areaId = area.id)
        val daycare2 = DevDaycare(areaId = area.id)
        val daycare3 = DevDaycare(areaId = area.id)
        val group1 =
            DevDaycareGroup(
                daycareId = daycare1.id,
                name = "Toukat",
                nekkuCustomerNumber = "2501K6089",
            )
        val group2 =
            DevDaycareGroup(
                daycareId = daycare2.id,
                name = "Kotelot",
                nekkuCustomerNumber = "2501K6090",
            )
        val group3 =
            DevDaycareGroup(
                daycareId = daycare2.id,
                name = "Perhoset",
                nekkuCustomerNumber = "2501K6090",
            )
        val group4 =
            DevDaycareGroup(
                daycareId = daycare3.id,
                name = "Torjunta-aineet",
                nekkuCustomerNumber = "2501K6091",
            )
        val employee1 = DevEmployee(email = "supervisor1@city.fi")
        val employee2 = DevEmployee(email = "supervisor2@city.fi")
        val employee3 = DevEmployee(email = "supervisor3@city.fi")

        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare1)
            tx.insert(daycare2)
            tx.insert(daycare3)
            tx.insert(group1)
            tx.insert(group2)
            tx.insert(group3)
            tx.insert(group4)
            tx.insert(employee1, mapOf(daycare1.id to UserRole.UNIT_SUPERVISOR))
            tx.insert(employee2, mapOf(daycare2.id to UserRole.UNIT_SUPERVISOR))
            tx.insert(employee3, mapOf(daycare3.id to UserRole.UNIT_SUPERVISOR))
        }

        client =
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
                                        NekkuCustomerApiWeekday.MONDAY,
                                        NekkuCustomerApiWeekday.TUESDAY,
                                        NekkuCustomerApiWeekday.WEDNESDAY,
                                        NekkuCustomerApiWeekday.THURSDAY,
                                        NekkuCustomerApiWeekday.FRIDAY,
                                        NekkuCustomerApiWeekday.SATURDAY,
                                        NekkuCustomerApiWeekday.SUNDAY,
                                        NekkuCustomerApiWeekday.WEEKDAYHOLIDAY,
                                    ),
                                    "alle 50-lasta",
                                )
                            ),
                        )
                    )
            )
        fetchAndUpdateNekkuCustomers(client, db, asyncJobRunner, now)

        asyncJobRunner.runPendingJobsSync(MockEvakaClock(now))

        val expectedTestContent1 =
            """
Seuraavien ryhmien asiakasnumerot on poistettu johtuen asiakasnumeron poistumisesta Nekusta:

- Kotelot

- Perhoset
        """
                .trim()

        val expectedTestContent2 =
            """
Seuraavien ryhmien asiakasnumerot on poistettu johtuen asiakasnumeron poistumisesta Nekusta:

- Torjunta-aineet
        """
                .trim()

        assertEquals(
            listOf(
                "supervisor2@city.fi" to expectedTestContent1,
                "supervisor3@city.fi" to expectedTestContent2,
            ),
            MockEmailClient.emails.map { it.toAddress to it.content.text },
        )
    }

    fun getNekkuSpecialDiet(): NekkuApiSpecialDiet =
        NekkuApiSpecialDiet(
            "2",
            "Päiväkodit er.",
            listOf(
                NekkuApiSpecialDietsField(
                    "17A9ACF0-DE9E-4C07-882E-C8C47351D009",
                    "Muu erityisruokavalio, mikä?",
                    NekkuApiSpecialDietType.Text,
                ),
                NekkuApiSpecialDietsField(
                    "AE1FE5FE-9619-4D7A-9043-A6B0C615156B",
                    "Erityisruokavaliot",
                    NekkuApiSpecialDietType.CheckBoxLst,
                    listOf(
                        NekkuSpecialDietOption(
                            1,
                            "Kananmunaton ruokavalio",
                            "Kananmunaton ruokavalio",
                        ),
                        NekkuSpecialDietOption(
                            2,
                            "Sianlihaton ruokavalio",
                            "Sianlihaton ruokavalio",
                        ),
                        NekkuSpecialDietOption(
                            3,
                            "Luontaisesti gluteeniton ruokavalio",
                            "Luontaisesti gluteeniton ruokavalio",
                        ),
                        NekkuSpecialDietOption(
                            4,
                            "Maitoallergisen ruokavalio",
                            "Maitoallergisen ruokavalio",
                        ),
                        NekkuSpecialDietOption(
                            5,
                            "Laktoositon ruokavalio",
                            "Laktoositon ruokavalio",
                        ),
                    ),
                ),
            ),
        )

    @Test
    fun `Nekku special diets sync does not sync empty data`() {
        val client = TestNekkuClient()
        assertThrows<Exception> { fetchAndUpdateNekkuSpecialDiets(client, db) }
    }

    @Test
    fun `Nekku special diets sync does sync non-empty data`() {
        val client = TestNekkuClient(specialDiets = listOf(getNekkuSpecialDiet()))

        fetchAndUpdateNekkuSpecialDiets(client, db)
        db.transaction { tx ->
            val specialDiets = tx.getNekkuSpecialOptions().toSet()
            assertEquals(5, specialDiets.size)
        }
    }

    @Test
    fun `Nekku special diets sync does update data`() {
        var client = TestNekkuClient(specialDiets = listOf(getNekkuSpecialDiet()))
        fetchAndUpdateNekkuSpecialDiets(client, db)
        db.transaction { tx ->
            val specialDiets = tx.getNekkuSpecialOptions().toSet()
            assertEquals(5, specialDiets.size)
        }

        client =
            TestNekkuClient(
                specialDiets =
                    listOf(
                        NekkuApiSpecialDiet(
                            "2",
                            "Päiväkodit erikois",
                            listOf(
                                NekkuApiSpecialDietsField(
                                    "17A9ACF0-DE9E-4C07-882E-C8C47351D009",
                                    "Muu erityisruokavalio, mikä?",
                                    NekkuApiSpecialDietType.Text,
                                ),
                                NekkuApiSpecialDietsField(
                                    "AE1FE5FE-9619-4D7A-9043-A6B0C615156B",
                                    "Erityisruokavaliot",
                                    NekkuApiSpecialDietType.CheckBoxLst,
                                    listOf(
                                        NekkuSpecialDietOption(
                                            1,
                                            "Kananmunaton ruokavalio",
                                            "Kananmunaton ruokavalio",
                                        ),
                                        NekkuSpecialDietOption(
                                            2,
                                            "Sianlihaton ruokavalio",
                                            "Sianlihaton ruokavalio",
                                        ),
                                        NekkuSpecialDietOption(
                                            3,
                                            "Luontaisesti gluteeniton ruokavalio",
                                            "Luontaisesti gluteeniton ruokavalio",
                                        ),
                                        NekkuSpecialDietOption(
                                            4,
                                            "Maitoallergisen ruokavalio",
                                            "Maitoallergisen ruokavalio",
                                        ),
                                        NekkuSpecialDietOption(
                                            5,
                                            "Laktoositon ruokavalio",
                                            "Laktoositon ruokavalio",
                                        ),
                                    ),
                                ),
                            ),
                        )
                    )
            )
        fetchAndUpdateNekkuSpecialDiets(client, db)
        db.transaction { tx ->
            val specialDiets = tx.getNekkuSpecialOptions()
            assertEquals(5, specialDiets.size)
        }
    }

    @Test
    fun `Nekku special diets sync removes old data and creates new data`() {
        var client = TestNekkuClient(specialDiets = listOf(getNekkuSpecialDiet()))
        fetchAndUpdateNekkuSpecialDiets(client, db)

        db.transaction { tx ->
            val specialDiets = tx.getNekkuSpecialOptions().toSet()
            assertEquals(5, specialDiets.size)
        }

        client =
            TestNekkuClient(
                specialDiets =
                    listOf(
                        NekkuApiSpecialDiet(
                            "2",
                            "Päiväkodit er.",
                            listOf(
                                NekkuApiSpecialDietsField(
                                    "17A9ACF0-DE9E-4C07-882E-C8C47351D009",
                                    "Muu erityisruokavalio, mikä?",
                                    NekkuApiSpecialDietType.Text,
                                ),
                                NekkuApiSpecialDietsField(
                                    "AE1FE5FE-9619-4D7A-9043-A6B0C615156B",
                                    "Erityisruokavaliot",
                                    NekkuApiSpecialDietType.CheckBoxLst,
                                    listOf(
                                        NekkuSpecialDietOption(
                                            1,
                                            "Kananmunaton ruokavalio",
                                            "Kananmunaton ruokavalio",
                                        ),
                                        NekkuSpecialDietOption(
                                            2,
                                            "Sianlihaton ruokavalio",
                                            "Sianlihaton ruokavalio",
                                        ),
                                    ),
                                ),
                            ),
                        )
                    )
            )

        fetchAndUpdateNekkuSpecialDiets(client, db)

        db.transaction { tx ->
            val specialDiets = tx.getNekkuSpecialOptions()
            assertEquals(2, specialDiets.size)
        }
    }

    @Test
    fun `Nekku special diets sync adds new special diet objects`() {
        var client = TestNekkuClient(specialDiets = listOf(getNekkuSpecialDiet()))
        fetchAndUpdateNekkuSpecialDiets(client, db)

        db.transaction { tx ->
            val nekkuSpecialDietOptions = tx.getNekkuSpecialOptions()
            assertEquals(5, nekkuSpecialDietOptions.size)
        }

        client =
            TestNekkuClient(
                specialDiets =
                    listOf(
                        getNekkuSpecialDiet(),
                        NekkuApiSpecialDiet(
                            "3",
                            "Päiväkodit erikoiset",
                            listOf(
                                NekkuApiSpecialDietsField(
                                    "17A9ACF0-DE9E-4C07-882E-C8C47351D008",
                                    "Muu erityisruokavalio, mikä?",
                                    NekkuApiSpecialDietType.Text,
                                ),
                                NekkuApiSpecialDietsField(
                                    "AE1FE5FE-9619-4D7A-9043-A6B0C6151566",
                                    "Erityisruokavaliot",
                                    NekkuApiSpecialDietType.CheckBoxLst,
                                    listOf(
                                        NekkuSpecialDietOption(
                                            1,
                                            "Kananmunaton ruokavalio",
                                            "Kananmunaton ruokavalio",
                                        ),
                                        NekkuSpecialDietOption(
                                            2,
                                            "Sianlihaton ruokavalio",
                                            "Sianlihaton ruokavalio",
                                        ),
                                    ),
                                ),
                            ),
                        ),
                    )
            )

        fetchAndUpdateNekkuSpecialDiets(client, db)

        db.transaction { tx ->
            val nekkuSpecialDietOptions = tx.getNekkuSpecialOptions()
            assertEquals(7, nekkuSpecialDietOptions.size)
        }
    }

    @Test
    fun `Nekku special diet sync can deserialize all field types`() {
        val specialDiet =
            """
            [
                {
                    "id" : "42",
                    "name" : "Kevyt kenttätesti",
                    "fields" : [
                        {
                            "id" : "A",
                            "type" : "text",
                            "name" : "A"
                        },
                        {
                            "id" : "B",
                            "type" : "checkboxlst",
                            "name" : "B",
                            "options" : [
                                {
                                    "key" : "BA",
                                    "value" : "BA",
                                    "weight" : 1
                                },
                                {
                                    "key" : "BB",
                                    "value" : "BB",
                                    "weight" : 2
                                },
                                {
                                    "key" : "BC",
                                    "value" : "BC",
                                    "weight" : 3
                                }
                            ]
                        },
                        {
                            "id" : "C",
                            "type" : "checkbox",
                            "name" : "C"
                        },
                        {
                            "id" : "D",
                            "type" : "radio",
                            "name" : "D",
                            "options": [
                                {
                                    "key" : "DA",
                                    "value": "DA",
                                    "weight" : 1
                                },
                                {
                                    "key" : "DB",
                                    "value": "DB",
                                    "weight" : 2
                                },
                                {
                                    "key" : "DC",
                                    "value": "DC",
                                    "weight" : 3
                                }
                            ]
                        },
                        {
                            "id" : "E",
                            "type" : "textarea",
                            "name" : "E"
                        },
                        {
                            "id" : "F",
                            "type" : "email",
                            "name" : "F"
                        }
                    ]
                }
            ]
        """

        val client = DeserializingTestNekkuClient(jsonMapper, specialDiets = specialDiet)
        client.getSpecialDiets()
    }

    @Test
    fun `Nekku special diet sync can save all field types to database`() {
        val client =
            TestNekkuClient(
                specialDiets =
                    listOf(
                        NekkuApiSpecialDiet(
                            "42",
                            "Kanta-enumin testi",
                            listOf(
                                NekkuApiSpecialDietsField("A", "A", NekkuApiSpecialDietType.Text),
                                NekkuApiSpecialDietsField(
                                    "B",
                                    "B",
                                    NekkuApiSpecialDietType.CheckBoxLst,
                                    listOf(
                                        NekkuSpecialDietOption(1, "BA", "BB"),
                                        NekkuSpecialDietOption(2, "BB", "BB"),
                                    ),
                                ),
                                NekkuApiSpecialDietsField(
                                    "C",
                                    "C",
                                    NekkuApiSpecialDietType.Checkbox,
                                ),
                                NekkuApiSpecialDietsField(
                                    "D",
                                    "D",
                                    NekkuApiSpecialDietType.Radio,
                                    listOf(
                                        NekkuSpecialDietOption(1, "DA", "DA"),
                                        NekkuSpecialDietOption(2, "DB", "DB"),
                                    ),
                                ),
                                NekkuApiSpecialDietsField(
                                    "E",
                                    "E",
                                    NekkuApiSpecialDietType.Textarea,
                                ),
                                NekkuApiSpecialDietsField("F", "F", NekkuApiSpecialDietType.Email),
                            ),
                        )
                    )
            )

        fetchAndUpdateNekkuSpecialDiets(client, db)

        db.read { tx -> assertEquals(6, tx.getNekkuSpecialDietFields().size) }
    }

    @Test
    fun `Nekku special diet sync removes removed special diets from children`() {
        var client = TestNekkuClient(specialDiets = listOf(getNekkuSpecialDiet()))
        fetchAndUpdateNekkuSpecialDiets(client, db)

        val childWithNoSpecialDiet = DevPerson()
        val childWithFreeTextField = DevPerson()
        val childWithRemainingCheckbox = DevPerson()
        val childWithRemovedCheckbox = DevPerson()
        val childWithFreeTextFieldAndRemainingCheckbox = DevPerson()
        val childWithFreeTextFieldAndRemovedCheckbox = DevPerson()

        db.transaction { tx ->
            tx.insert(childWithNoSpecialDiet, DevPersonType.CHILD)
            tx.insert(childWithFreeTextField, DevPersonType.CHILD)
            tx.insert(childWithRemainingCheckbox, DevPersonType.CHILD)
            tx.insert(childWithRemovedCheckbox, DevPersonType.CHILD)
            tx.insert(childWithFreeTextFieldAndRemainingCheckbox, DevPersonType.CHILD)
            tx.insert(childWithFreeTextFieldAndRemovedCheckbox, DevPersonType.CHILD)
        }

        insertNekkuSpecialDietChoice(
            childWithFreeTextField.id,
            "2",
            "17A9ACF0-DE9E-4C07-882E-C8C47351D009",
            "Random value",
        )
        insertNekkuSpecialDietChoice(
            childWithRemainingCheckbox.id,
            "2",
            "AE1FE5FE-9619-4D7A-9043-A6B0C615156B",
            "Kananmunaton ruokavalio",
        )
        insertNekkuSpecialDietChoice(
            childWithRemovedCheckbox.id,
            "2",
            "AE1FE5FE-9619-4D7A-9043-A6B0C615156B",
            "Laktoositon ruokavalio",
        )
        insertNekkuSpecialDietChoice(
            childWithFreeTextFieldAndRemainingCheckbox.id,
            "2",
            "17A9ACF0-DE9E-4C07-882E-C8C47351D009",
            "Random value",
        )
        insertNekkuSpecialDietChoice(
            childWithFreeTextFieldAndRemainingCheckbox.id,
            "2",
            "AE1FE5FE-9619-4D7A-9043-A6B0C615156B",
            "Kananmunaton ruokavalio",
        )
        insertNekkuSpecialDietChoice(
            childWithFreeTextFieldAndRemovedCheckbox.id,
            "2",
            "17A9ACF0-DE9E-4C07-882E-C8C47351D009",
            "Random value",
        )
        insertNekkuSpecialDietChoice(
            childWithFreeTextFieldAndRemovedCheckbox.id,
            "2",
            "AE1FE5FE-9619-4D7A-9043-A6B0C615156B",
            "Laktoositon ruokavalio",
        )

        client =
            TestNekkuClient(
                specialDiets =
                    listOf(
                        NekkuApiSpecialDiet(
                            "2",
                            "Päiväkodit er.",
                            listOf(
                                NekkuApiSpecialDietsField(
                                    "AE1FE5FE-9619-4D7A-9043-A6B0C615156B",
                                    "Erityisruokavaliot",
                                    NekkuApiSpecialDietType.CheckBoxLst,
                                    listOf(
                                        NekkuSpecialDietOption(
                                            1,
                                            "Kananmunaton ruokavalio",
                                            "Kananmunaton ruokavalio",
                                        ),
                                        NekkuSpecialDietOption(
                                            2,
                                            "Sianlihaton ruokavalio",
                                            "Sianlihaton ruokavalio",
                                        ),
                                    ),
                                )
                            ),
                        )
                    )
            )

        fetchAndUpdateNekkuSpecialDiets(client, db)

        db.read { tx ->
            assertEquals(listOf(), tx.getNekkuSpecialDietChoices(childWithNoSpecialDiet.id))
            assertEquals(listOf(), tx.getNekkuSpecialDietChoices(childWithFreeTextField.id))
            assertEquals(listOf(), tx.getNekkuSpecialDietChoices(childWithRemovedCheckbox.id))
            assertEquals(
                listOf(
                    NekkuSpecialDietChoices(
                        "2",
                        "AE1FE5FE-9619-4D7A-9043-A6B0C615156B",
                        "Kananmunaton ruokavalio",
                    )
                ),
                tx.getNekkuSpecialDietChoices(childWithRemainingCheckbox.id),
            )
            assertEquals(
                listOf(),
                tx.getNekkuSpecialDietChoices(childWithFreeTextFieldAndRemovedCheckbox.id),
            )
            assertEquals(
                listOf(
                    NekkuSpecialDietChoices(
                        "2",
                        "AE1FE5FE-9619-4D7A-9043-A6B0C615156B",
                        "Kananmunaton ruokavalio",
                    )
                ),
                tx.getNekkuSpecialDietChoices(childWithFreeTextFieldAndRemainingCheckbox.id),
            )
        }
    }

    val nekkuProducts =
        listOf(
            NekkuApiProduct(
                "Ateriapalvelu 1 kasvis",
                "31000010",
                "",
                listOf("alle-50-lasta", "alle-100-lasta"),
                listOf(
                    NekkuProductMealTime.BREAKFAST,
                    NekkuProductMealTime.LUNCH,
                    NekkuProductMealTime.SNACK,
                ),
                NekkuApiProductMealType.Kasvis,
            ),
            NekkuApiProduct(
                "Ateriapalvelu 1 kasvis er",
                "31000011",
                "2",
                listOf("alle-50-lasta"),
                listOf(
                    NekkuProductMealTime.BREAKFAST,
                    NekkuProductMealTime.LUNCH,
                    NekkuProductMealTime.SNACK,
                ),
                NekkuApiProductMealType.Kasvis,
            ),
            NekkuApiProduct(
                "Päivällinen vegaani päiväkoti",
                "31000008",
                "",
                listOf("100-lasta"),
                listOf(NekkuProductMealTime.DINNER),
                NekkuApiProductMealType.Vegaani,
            ),
            NekkuApiProduct("Lounas kasvis er", "31001011", "2", listOf("50-99-lasta"), null, null),
        )

    val nekkuProductsForOrder =
        listOf(
            NekkuApiProduct(
                "Ateriapalvelu 1 aamupala",
                "31000010",
                "",
                listOf("100-lasta"),
                listOf(NekkuProductMealTime.BREAKFAST),
                null,
            ),
            NekkuApiProduct(
                "Ateriapalvelu 1 lounas",
                "31000011",
                "",
                listOf("100-lasta"),
                listOf(NekkuProductMealTime.LUNCH),
                null,
            ),
            NekkuApiProduct(
                "Ateriapalvelu 1 välipala",
                "31000012",
                "",
                listOf("100-lasta"),
                listOf(NekkuProductMealTime.SNACK),
                null,
            ),
            NekkuApiProduct(
                "Ateriapalvelu 1 iltapala",
                "31000013",
                "",
                listOf("100-lasta"),
                listOf(NekkuProductMealTime.SUPPER),
                null,
            ),
            NekkuApiProduct(
                "Ateriapalvelu kasvis 1 aamupala",
                "31000014",
                "",
                listOf("100-lasta"),
                listOf(NekkuProductMealTime.BREAKFAST),
                NekkuApiProductMealType.Kasvis,
            ),
            NekkuApiProduct(
                "Ateriapalvelu kasvis 1 lounas",
                "31000015",
                "",
                listOf("100-lasta"),
                listOf(NekkuProductMealTime.LUNCH),
                NekkuApiProductMealType.Kasvis,
            ),
            NekkuApiProduct(
                "Ateriapalvelu kasvis 1 välipala",
                "31000016",
                "",
                listOf("100-lasta"),
                listOf(NekkuProductMealTime.SNACK),
                NekkuApiProductMealType.Kasvis,
            ),
            NekkuApiProduct(
                "Ateriapalvelu kasvis 1 iltapala",
                "31000017",
                "",
                listOf("100-lasta"),
                listOf(NekkuProductMealTime.SUPPER),
                NekkuApiProductMealType.Kasvis,
            ),
            NekkuApiProduct(
                "Ateriapalvelu 2 aamupala ja lounas",
                "31000018",
                "",
                listOf("50-99-lasta"),
                listOf(NekkuProductMealTime.BREAKFAST, NekkuProductMealTime.LUNCH),
                null,
            ),
            NekkuApiProduct(
                "Ateriapalvelu 2 välipala",
                "31000019",
                "",
                listOf("50-99-lasta"),
                listOf(NekkuProductMealTime.SNACK),
                null,
            ),
            NekkuApiProduct(
                "Ateriapalvelu 1 aamupala er",
                "31000020",
                "2",
                listOf("100-lasta"),
                listOf(NekkuProductMealTime.BREAKFAST),
                null,
            ),
            NekkuApiProduct(
                "Ateriapalvelu 1 lounas er",
                "31000021",
                "2",
                listOf("100-lasta"),
                listOf(NekkuProductMealTime.LUNCH),
                null,
            ),
            NekkuApiProduct(
                "Ateriapalvelu 1 välipala er",
                "31000022",
                "2",
                listOf("100-lasta"),
                listOf(NekkuProductMealTime.SNACK),
                null,
            ),
            NekkuApiProduct(
                "Ateriapalvelu 1 aamupala kasvis er",
                "31000023",
                "2",
                listOf("100-lasta"),
                listOf(NekkuProductMealTime.BREAKFAST),
                NekkuApiProductMealType.Kasvis,
            ),
            NekkuApiProduct(
                "Ateriapalvelu 1 lounas kasvis er",
                "31000024",
                "2",
                listOf("100-lasta"),
                listOf(NekkuProductMealTime.LUNCH),
                NekkuApiProductMealType.Kasvis,
            ),
            NekkuApiProduct(
                "Ateriapalvelu 1 välipala kasvis er",
                "31000025",
                "2",
                listOf("100-lasta"),
                listOf(NekkuProductMealTime.SNACK),
                NekkuApiProductMealType.Kasvis,
            ),
        )

    @Test
    fun `Nekku product sync does not sync empty data`() {
        val client = TestNekkuClient()
        assertThrows<Exception> { fetchAndUpdateNekkuProducts(client, db) }
    }

    @Test
    fun `Nekku product sync does sync non-empty data`() {
        val client = TestNekkuClient(nekkuProducts = nekkuProducts)
        fetchAndUpdateNekkuProducts(client, db)
        db.read { tx ->
            val products = tx.getNekkuProducts()
            assertEquals(4, products.size)
        }
    }

    @Test
    fun `Nekku product deletes old products`() {
        var client = TestNekkuClient(nekkuProducts = nekkuProducts)
        fetchAndUpdateNekkuProducts(client, db)
        db.read { tx ->
            val products = tx.getNekkuProducts()
            assertEquals(4, products.size)
        }

        client =
            TestNekkuClient(
                nekkuProducts =
                    listOf(
                        NekkuApiProduct(
                            "Ateriapalvelu 1 kasvis",
                            "31000010",
                            "",
                            listOf("alle-50-lasta"),
                            listOf(
                                NekkuProductMealTime.BREAKFAST,
                                NekkuProductMealTime.LUNCH,
                                NekkuProductMealTime.SNACK,
                            ),
                            NekkuApiProductMealType.Kasvis,
                        ),
                        NekkuApiProduct(
                            "Päivällinen vegaani päiväkoti",
                            "31000008",
                            "",
                            listOf("100-lasta"),
                            listOf(NekkuProductMealTime.DINNER),
                            NekkuApiProductMealType.Kasvis,
                        ),
                    )
            )
        fetchAndUpdateNekkuProducts(client, db)
        db.read { tx ->
            val products = tx.getNekkuProducts()
            assertEquals(2, products.size)
        }
    }

    @Test
    fun `Nekku product updates values for old products`() {
        var client =
            TestNekkuClient(
                nekkuProducts =
                    listOf(
                        NekkuApiProduct(
                            "Ateriapalvelu 1 kasvis",
                            "31000010",
                            "",
                            listOf("alle-50-lasta"),
                            listOf(
                                NekkuProductMealTime.BREAKFAST,
                                NekkuProductMealTime.LUNCH,
                                NekkuProductMealTime.SNACK,
                            ),
                            NekkuApiProductMealType.Kasvis,
                        )
                    )
            )
        fetchAndUpdateNekkuProducts(client, db)
        db.read { tx ->
            val products = tx.getNekkuProducts()
            assertEquals(1, products.size)
        }

        client =
            TestNekkuClient(
                nekkuProducts =
                    listOf(
                        NekkuApiProduct(
                            "Ateriapalvelu 1 vegaani",
                            "31000010",
                            "2",
                            listOf("100-lasta"),
                            null,
                            NekkuApiProductMealType.Vegaani,
                        )
                    )
            )
        fetchAndUpdateNekkuProducts(client, db)
        db.transaction { tx ->
            val products = tx.getNekkuProducts()
            assertEquals("Ateriapalvelu 1 vegaani", products[0].name)
            assertEquals("2", products[0].optionsId)
            assertEquals("100-lasta", products[0].customerTypes.first())
            assertEquals(null, products[0].mealTime)
            assertEquals(NekkuProductMealType.VEGAN, products[0].mealType)
        }
    }

    @Test
    fun `meal order jobs for daycare groups without customer number are not planned`() {

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
                    )
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
        val group = DevDaycareGroup(daycareId = daycare.id, nekkuCustomerNumber = null)

        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(group)
        }

        // Tuesday
        val now = HelsinkiDateTime.of(LocalDate.of(2025, 4, 1), LocalTime.of(2, 25))

        planNekkuOrderJobs(db, asyncJobRunner, now)

        assertEquals(emptyList(), getNekkuWeeklyJobs())
    }

    @Test
    fun `meal order jobs for daycare groups with customer number are planned`() {

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
                    )
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
        val group = DevDaycareGroup(daycareId = daycare.id, nekkuCustomerNumber = "2501K6089")

        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(group)
        }

        // Tuesday
        val now = HelsinkiDateTime.of(LocalDate.of(2025, 3, 31), LocalTime.of(2, 25))

        planNekkuOrderJobs(db, asyncJobRunner, now)

        assertEquals(5, getNekkuWeeklyJobs().count())
    }

    @Test
    fun `meal order jobs for daycare groups are planned for two week time and it does not create job for daycare that is not open`() {

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
                    )
            )
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
        val now = HelsinkiDateTime.of(LocalDate.of(2025, 3, 31), LocalTime.of(2, 25))

        val nowPlusTwoWeeks = HelsinkiDateTime.of(LocalDate.of(2025, 4, 14), LocalTime.of(2, 25))

        planNekkuOrderJobs(db, asyncJobRunner, now)

        assertEquals(
            nowPlusTwoWeeks.toLocalDate().toString(),
            getNekkuWeeklyJobs().first().date.toString(),
        )

        assertEquals(6, getNekkuWeeklyJobs().count())
    }

    @Test
    fun `meal order jobs for daycare groups are re-planned for tomorrow`() {

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
                    )
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

        assertEquals(
            tomorrow.toLocalDate().toString(),
            getNekkuDailyJobs().single().date.toString(),
        )
    }

    @Test
    fun `meal order jobs for daycare groups are not re-planned for tomorrow if daycare is not open`() {

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
        // products
        fetchAndUpdateNekkuProducts(client, db)
        fetchAndUpdateNekkuSpecialDiets(client, db)

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

        assertEquals(
            nextMonday.toLocalDate().toString(),
            getNekkuDailyJobs().single().date.toString(),
        )
    }

    @Test
    fun `meal order jobs for daycare groups are not planned if daycare has no operation days`() {

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
        // products
        fetchAndUpdateNekkuProducts(client, db)
        fetchAndUpdateNekkuSpecialDiets(client, db)

        val area = DevCareArea()

        val daycare =
            DevDaycare(
                areaId = area.id,
                mealtimeBreakfast = TimeRange(LocalTime.of(8, 0), LocalTime.of(8, 20)),
                mealtimeLunch = TimeRange(LocalTime.of(11, 15), LocalTime.of(11, 45)),
                mealtimeSnack = TimeRange(LocalTime.of(13, 30), LocalTime.of(13, 50)),
                operationTimes = listOf(
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                ),
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

        assertEquals(0, getNekkuDailyJobs().count())
    }

    @Test
    fun `Send Nekku orders with known reservations`() {
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
        // products
        fetchAndUpdateNekkuProducts(client, db)
        fetchAndUpdateNekkuSpecialDiets(client, db)
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

        createAndSendNekkuOrder(client, db, group.id, monday, 0.9)
        createAndSendNekkuOrder(client, db, group.id, tuesday, 0.9)

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
    fun `Send Nekku orders with known reservations and remove 10prcent of normal orders`() {
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
        // products
        fetchAndUpdateNekkuProducts(client, db)
        fetchAndUpdateNekkuSpecialDiets(client, db)
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

        createAndSendNekkuOrder(client, db, group.id, monday, 0.9)
        createAndSendNekkuOrder(client, db, group.id, tuesday, 0.9)

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
    fun `10percnt deduction does not remove any vegan or vegetable diets`() {
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
        // products
        fetchAndUpdateNekkuProducts(client, db)
        fetchAndUpdateNekkuSpecialDiets(client, db)
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

        createAndSendNekkuOrder(client, db, group.id, monday, 0.9)
        createAndSendNekkuOrder(client, db, group.id, tuesday, 0.9)

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
        fetchAndUpdateNekkuSpecialDiets(client, db)
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
                DevChild(id = child.id, eatsBreakfast = false)
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

        createAndSendNekkuOrder(client, db, group.id, monday, 0.9)

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
                customers =
                    listOf(
                        NekkuApiCustomer(
                            "2501K6089",
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
        // products
        fetchAndUpdateNekkuProducts(client, db)
        fetchAndUpdateNekkuSpecialDiets(client, db)
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
                DevChild(id = child.id, eatsBreakfast = false)
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

        createAndSendNekkuOrder(client, db, group.id, monday, 0.9)

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
                customers =
                    listOf(
                        NekkuApiCustomer(
                            "2501K6089",
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
        // products
        fetchAndUpdateNekkuProducts(client, db)
        fetchAndUpdateNekkuSpecialDiets(client, db)
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

        createAndSendNekkuOrder(client, db, group.id, monday, 0.9)
        createAndSendNekkuOrder(client, db, group.id, tuesday, 0.9)

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
                customers =
                    listOf(
                        NekkuApiCustomer(
                            "2501K6089",
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
        // products
        fetchAndUpdateNekkuProducts(client, db)
        fetchAndUpdateNekkuSpecialDiets(client, db)
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

        createAndSendNekkuOrder(client, db, group.id, monday, 0.9)
        createAndSendNekkuOrder(client, db, group.id, tuesday, 0.9)

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
                customers =
                    listOf(
                        NekkuApiCustomer(
                            "2501K6089",
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
        // products
        fetchAndUpdateNekkuProducts(client, db)
        fetchAndUpdateNekkuSpecialDiets(client, db)
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

        createAndSendNekkuOrder(client, db, group.id, monday, 0.9)
        createAndSendNekkuOrder(client, db, group.id, tuesday, 0.9)

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
        fetchAndUpdateNekkuSpecialDiets(client, db)
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

        createAndSendNekkuOrder(client, db, group.id, monday, 0.9)
        createAndSendNekkuOrder(client, db, group.id, tuesday, 0.9)

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
        fetchAndUpdateNekkuSpecialDiets(client, db)
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

        createAndSendNekkuOrder(client, db, group.id, monday, 0.9)
        createAndSendNekkuOrder(client, db, group.id, tuesday, 0.9)

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
        // products
        fetchAndUpdateNekkuProducts(client, db)
        fetchAndUpdateNekkuSpecialDiets(client, db)

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
            mixedSpecial1Child1.id,
            "2",
            "AE1FE5FE-9619-4D7A-9043-A6B0C615156B",
            "Laktoositon ruokavalio",
        )
        insertNekkuSpecialDietChoice(
            mixedSpecial1Child1.id,
            "2",
            "AE1FE5FE-9619-4D7A-9043-A6B0C615156B",
            "Luontaisesti gluteeniton ruokavalio",
        )
        insertNekkuSpecialDietChoice(
            mixedSpecial1Child2.id,
            "2",
            "AE1FE5FE-9619-4D7A-9043-A6B0C615156B",
            "Luontaisesti gluteeniton ruokavalio",
        )
        insertNekkuSpecialDietChoice(
            mixedSpecial1Child2.id,
            "2",
            "AE1FE5FE-9619-4D7A-9043-A6B0C615156B",
            "Laktoositon ruokavalio",
        )
        insertNekkuSpecialDietChoice(
            mixedSpecial2Child1.id,
            "2",
            "AE1FE5FE-9619-4D7A-9043-A6B0C615156B",
            "Kananmunaton ruokavalio",
        )
        insertNekkuSpecialDietChoice(
            vegetableSpecialChild1.id,
            "2",
            "17A9ACF0-DE9E-4C07-882E-C8C47351D009",
            "Pähkinätön",
        )
        insertNekkuSpecialDietChoice(
            vegetableSpecialChild2.id,
            "2",
            "17A9ACF0-DE9E-4C07-882E-C8C47351D009",
            "Pähkinätön",
        )

        createAndSendNekkuOrder(client, db, group.id, monday, 0.9)

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
        // products
        fetchAndUpdateNekkuProducts(client, db)
        fetchAndUpdateNekkuSpecialDiets(client, db)

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
            mixedBabyWithoutLactose.id,
            "2",
            "AE1FE5FE-9619-4D7A-9043-A6B0C615156B",
            "Laktoositon ruokavalio",
        )
        insertNekkuSpecialDietChoice(
            mixedBabyWithOtherSpecialDiet.id,
            "2",
            "17A9ACF0-DE9E-4C07-882E-C8C47351D009",
            "Pähkinätön",
        )
        insertNekkuSpecialDietChoice(
            mixedBabyWithoutLactoseAndAnotherSpecialDiet.id,
            "2",
            "AE1FE5FE-9619-4D7A-9043-A6B0C615156B",
            "Laktoositon ruokavalio",
        )
        insertNekkuSpecialDietChoice(
            mixedBabyWithoutLactoseAndAnotherSpecialDiet.id,
            "2",
            "17A9ACF0-DE9E-4C07-882E-C8C47351D009",
            "Pähkinätön",
        )

        createAndSendNekkuOrder(client, db, group.id, monday, 0.9)

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
        fetchAndUpdateNekkuSpecialDiets(client, db)

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

        createAndSendNekkuOrder(client, db, group.id, monday, 0.9)

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
    fun `Meal types should contain the null value`() {
        val mealTypes =
            nekkuController.getNekkuMealTypes(dbInstance(), getAuthenticatedEmployee(), getClock())

        assert(mealTypes.map { it.type }.contains(null))
    }

    @Test
    fun `Meal types should contain all NekkuProductMealTime values`() {
        val mealTypes =
            nekkuController.getNekkuMealTypes(dbInstance(), getAuthenticatedEmployee(), getClock())

        NekkuProductMealType.entries.forEach { entry ->
            assert(mealTypes.map { it.type }.contains(entry))
        }
    }

    @Test
    fun `Fetching customer numbers should work`() {
        insertCustomerNumbers()

        val customerNumbers =
            nekkuController.getNekkuUnitNumbers(
                dbInstance(),
                getAuthenticatedEmployee(),
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
        insertSpecialDiets()

        val specialDiets =
            nekkuController.getNekkuSpecialDiets(
                dbInstance(),
                getAuthenticatedEmployee(),
                getClock(),
            )
        assertEquals(1, specialDiets.size)
        assertEquals(listOf(NekkuSpecialDietWithoutFields("2", "Päiväkodit ER")), specialDiets)
    }

    @Test
    fun `Fetching special diet fields should work`() {
        insertSpecialDiets()

        val specialDietFields =
            nekkuController.getNekkuSpecialDietFields(
                dbInstance(),
                getAuthenticatedEmployee(),
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
        insertSpecialDiets()

        val specialDietOptions =
            nekkuController.getNekkuSpecialDietOptions(
                dbInstance(),
                getAuthenticatedEmployee(),
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

    private fun assertOrdersListEquals(
        expected: List<NekkuClient.NekkuOrders>,
        actual: List<NekkuClient.NekkuOrders>,
    ) {
        assertEquals(expected.size, actual.size)
        expected.zip(actual).forEach { (expected, actual) ->
            assertNekkuOrdersEquals(expected, actual)
        }
    }

    private fun assertNekkuOrdersEquals(
        expected: NekkuClient.NekkuOrders,
        actual: NekkuClient.NekkuOrders,
    ) {
        assertEquals(expected.dryRun, actual.dryRun)
        expected.orders.zip(actual.orders).forEach { (expected, actual) ->
            assertOrderEquals(expected, actual)
        }
    }

    private fun assertOrderEquals(
        expected: NekkuClient.NekkuOrder,
        actual: NekkuClient.NekkuOrder,
    ) {
        assertEquals(expected.deliveryDate, actual.deliveryDate)
        assertEquals(expected.customerNumber, actual.customerNumber)
        assertEquals(expected.groupId, actual.groupId)
        assertEquals(expected.description, actual.description)
        assertEquals(expected.items.toSet(), actual.items.toSet())
    }

    private fun getAuthenticatedEmployee(): AuthenticatedUser.Employee {
        val employee =
            DevEmployee(
                id = EmployeeId(UUID.randomUUID()),
                firstName = "Test",
                lastName = "Employee",
            )
        db.transaction { tx -> tx.insert(employee) }
        return AuthenticatedUser.Employee(employee.id, setOf(UserRole.ADMIN))
    }

    private fun getClock(): MockEvakaClock =
        MockEvakaClock(HelsinkiDateTime.of(LocalDate.of(2025, 4, 15), LocalTime.of(12, 0)))

    private fun insertCustomerNumbers() =
        db.transaction { tx ->
            tx.createUpdate {
                    sql(
                        "INSERT INTO nekku_customer VALUES" +
                            "('1234', 'Lönnrotinkadun päiväkoti', 'Varhaiskasvatus')," +
                            "('5678', 'Rubeberginkadun päiväkoti', 'Varhaiskasvatus')"
                    )
                }
                .execute()
        }

    private fun insertSpecialDiets() {
        db.transaction { tx ->
            tx.createUpdate { sql("INSERT INTO nekku_special_diet VALUES ('2', 'Päiväkodit ER')") }
                .execute()

            tx.createUpdate {
                    sql(
                        "INSERT INTO nekku_special_diet_field VALUES" +
                            "('12345678-9ABC-DEF0-1234-56789ABCDEF0', 'Erityisruokavaliot', 'CHECKBOXLIST', '2')," +
                            "('56789ABC-DEF0-1234-5678-9ABCDEF01234', 'Muu erityisruokavalio', 'TEXT', '2')"
                    )
                }
                .execute()

            tx.createUpdate {
                    sql(
                        "INSERT INTO nekku_special_diet_option VALUES" +
                            "(1, 'Kananmunaton', 'Kananmunaton', '12345678-9ABC-DEF0-1234-56789ABCDEF0')," +
                            "(2, 'Sianlihaton', 'Sianlihaton', '12345678-9ABC-DEF0-1234-56789ABCDEF0')"
                    )
                }
                .execute()
        }
    }

    private fun insertNekkuSpecialDietChoice(
        childId: ChildId,
        dietId: String,
        dietField: String,
        dietValue: String,
    ) {
        db.transaction { tx ->
            tx.createUpdate {
                    sql(
                        "INSERT INTO nekku_special_diet_choices VALUES (" +
                            "${bind(childId)}," +
                            "${bind(dietId)}," +
                            "${bind(dietField)}," +
                            "${bind(dietValue)}" +
                            ")"
                    )
                }
                .execute()
        }
    }

    @Test
    fun `Make sure that Nekku order is stored in database`() {

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
        // products
        fetchAndUpdateNekkuProducts(client, db)
        fetchAndUpdateNekkuSpecialDiets(client, db)

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
            mixedBabyWithoutLactose.id,
            "2",
            "AE1FE5FE-9619-4D7A-9043-A6B0C615156B",
            "Laktoositon ruokavalio",
        )
        insertNekkuSpecialDietChoice(
            mixedBabyWithOtherSpecialDiet.id,
            "2",
            "17A9ACF0-DE9E-4C07-882E-C8C47351D009",
            "Pähkinätön",
        )
        insertNekkuSpecialDietChoice(
            mixedBabyWithoutLactoseAndAnotherSpecialDiet.id,
            "2",
            "AE1FE5FE-9619-4D7A-9043-A6B0C615156B",
            "Laktoositon ruokavalio",
        )
        insertNekkuSpecialDietChoice(
            mixedBabyWithoutLactoseAndAnotherSpecialDiet.id,
            "2",
            "17A9ACF0-DE9E-4C07-882E-C8C47351D009",
            "Pähkinätön",
        )

        createAndSendNekkuOrder(client, db, group.id, monday, 0.9)

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
        // products
        fetchAndUpdateNekkuProducts(client, db)
        fetchAndUpdateNekkuSpecialDiets(client, db)

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
            mixedBabyWithoutLactose.id,
            "2",
            "AE1FE5FE-9619-4D7A-9043-A6B0C615156B",
            "Laktoositon ruokavalio",
        )
        insertNekkuSpecialDietChoice(
            mixedBabyWithOtherSpecialDiet.id,
            "2",
            "17A9ACF0-DE9E-4C07-882E-C8C47351D009",
            "Pähkinätön",
        )
        insertNekkuSpecialDietChoice(
            mixedBabyWithoutLactoseAndAnotherSpecialDiet.id,
            "2",
            "AE1FE5FE-9619-4D7A-9043-A6B0C615156B",
            "Laktoositon ruokavalio",
        )
        insertNekkuSpecialDietChoice(
            mixedBabyWithoutLactoseAndAnotherSpecialDiet.id,
            "2",
            "17A9ACF0-DE9E-4C07-882E-C8C47351D009",
            "Pähkinätön",
        )

        createAndSendNekkuOrder(client, db, group.id, monday, 0.9)

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
            removedChildren.forEach {
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
                    .forEach { tx.insert(it) }
            }
        }

        insertNekkuSpecialDietChoice(
            mixedBabyWithoutLactose.id,
            "2",
            "AE1FE5FE-9619-4D7A-9043-A6B0C615156B",
            "Laktoositon ruokavalio",
        )

        createAndSendNekkuOrder(client, db, group.id, monday, 0.9)

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
                    ),
                ),
                nekkuOrderReportResult.toSet(),
            )
        }
    }

    private fun getNekkuWeeklyJobs() =
        db.read { tx ->
            tx.createQuery { sql("SELECT payload FROM async_job WHERE type = 'SendNekkuOrder'") }
                .map { jsonColumn<AsyncJob.SendNekkuOrder>("payload") }
                .toList()
        }

    private fun getNekkuDailyJobs() =
        db.read { tx ->
            tx.createQuery {
                    sql("SELECT payload FROM async_job WHERE type = 'SendNekkuDailyOrder'")
                }
                .map { jsonColumn<AsyncJob.SendNekkuOrder>("payload") }
                .toList()
        }
}

class TestNekkuClient(
    private val customers: List<NekkuApiCustomer> = emptyList(),
    private val specialDiets: List<NekkuApiSpecialDiet> = emptyList(),
    private val nekkuProducts: List<NekkuApiProduct> = emptyList(),
) : NekkuClient {
    val orders = mutableListOf<NekkuClient.NekkuOrders>()

    override fun getCustomers(): List<NekkuApiCustomer> {
        return customers
    }

    override fun getSpecialDiets(): List<NekkuApiSpecialDiet> {
        return specialDiets
    }

    override fun getProducts(): List<NekkuApiProduct> {
        return nekkuProducts
    }

    override fun createNekkuMealOrder(nekkuOrders: NekkuClient.NekkuOrders): NekkuOrderResult {
        orders.add(nekkuOrders)

        return NekkuOrderResult(
            message = "Input ok, 5 orders would be created.",
            created = listOf("12345", "65432"),
            cancelled = emptyList(),
        )
    }
}

class DeserializingTestNekkuClient(
    private val jsonMapper: JsonMapper,
    private val customers: String = "",
    private val specialDiets: String = "",
    private val nekkuProducts: String = "",
) : NekkuClient {
    val orders = mutableListOf<NekkuClient.NekkuOrders>()

    override fun getCustomers(): List<NekkuApiCustomer> {
        return jsonMapper.readValue<List<NekkuApiCustomer>>(customers)
    }

    override fun getSpecialDiets(): List<NekkuApiSpecialDiet> {
        return jsonMapper.readValue<List<NekkuApiSpecialDiet>>(specialDiets)
    }

    override fun getProducts(): List<NekkuApiProduct> {
        return jsonMapper.readValue<List<NekkuApiProduct>>(nekkuProducts)
    }

    override fun createNekkuMealOrder(nekkuOrders: NekkuClient.NekkuOrders): NekkuOrderResult {
        orders.add(nekkuOrders)

        return NekkuOrderResult(
            message = "Input ok, 5 orders would be created.",
            created = listOf("12345", "65432"),
            cancelled = emptyList(),
        )
    }
}
