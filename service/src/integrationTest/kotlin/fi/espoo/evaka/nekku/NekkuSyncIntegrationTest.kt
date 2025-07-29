package fi.espoo.evaka.nekku

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.daycare.getDaycareGroup
import fi.espoo.evaka.emailclient.MockEmailClient
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.DevDaycareGroupPlacement
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired

class NekkuSyncIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var asyncJobRunner: AsyncJobRunner<AsyncJob>

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

    @Test
    fun `Nekku special diets sync does not sync empty data`() {
        val client = TestNekkuClient()
        assertThrows<Exception> { fetchAndUpdateNekkuSpecialDiets(client, db, asyncJobRunner, now) }
    }

    @Test
    fun `Nekku special diets sync does sync non-empty data`() {
        val client = TestNekkuClient(specialDiets = listOf(getNekkuSpecialDiet()))

        fetchAndUpdateNekkuSpecialDiets(client, db, asyncJobRunner, now)
        db.transaction { tx ->
            val specialDiets = tx.getNekkuSpecialOptions().toSet()
            assertEquals(5, specialDiets.size)
        }
    }

    @Test
    fun `Nekku special diets sync does update data`() {
        var client = TestNekkuClient(specialDiets = listOf(getNekkuSpecialDiet()))
        fetchAndUpdateNekkuSpecialDiets(client, db, asyncJobRunner, now)
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
        fetchAndUpdateNekkuSpecialDiets(client, db, asyncJobRunner, now)
        db.transaction { tx ->
            val specialDiets = tx.getNekkuSpecialOptions()
            assertEquals(5, specialDiets.size)
        }
    }

    @Test
    fun `Nekku special diets sync removes old data and creates new data`() {
        var client = TestNekkuClient(specialDiets = listOf(getNekkuSpecialDiet()))
        fetchAndUpdateNekkuSpecialDiets(client, db, asyncJobRunner, now)

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

        fetchAndUpdateNekkuSpecialDiets(client, db, asyncJobRunner, now)

        db.transaction { tx ->
            val specialDiets = tx.getNekkuSpecialOptions()
            assertEquals(2, specialDiets.size)
        }
    }

    @Test
    fun `Nekku special diets sync adds new special diet objects`() {
        var client = TestNekkuClient(specialDiets = listOf(getNekkuSpecialDiet()))
        fetchAndUpdateNekkuSpecialDiets(client, db, asyncJobRunner, now)

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

        fetchAndUpdateNekkuSpecialDiets(client, db, asyncJobRunner, now)

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

        fetchAndUpdateNekkuSpecialDiets(client, db, asyncJobRunner, now)

        db.read { tx -> assertEquals(6, tx.getNekkuSpecialDietFields().size) }
    }

    @Test
    fun `Nekku special diet sync removes removed special diets from children`() {
        var client = TestNekkuClient(specialDiets = listOf(getNekkuSpecialDiet()))
        fetchAndUpdateNekkuSpecialDiets(client, db, asyncJobRunner, now)

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
            db,
            childWithFreeTextField.id,
            "2",
            "17A9ACF0-DE9E-4C07-882E-C8C47351D009",
            "Random value",
        )
        insertNekkuSpecialDietChoice(
            db,
            childWithRemainingCheckbox.id,
            "2",
            "AE1FE5FE-9619-4D7A-9043-A6B0C615156B",
            "Kananmunaton ruokavalio",
        )
        insertNekkuSpecialDietChoice(
            db,
            childWithRemovedCheckbox.id,
            "2",
            "AE1FE5FE-9619-4D7A-9043-A6B0C615156B",
            "Laktoositon ruokavalio",
        )
        insertNekkuSpecialDietChoice(
            db,
            childWithFreeTextFieldAndRemainingCheckbox.id,
            "2",
            "17A9ACF0-DE9E-4C07-882E-C8C47351D009",
            "Random value",
        )
        insertNekkuSpecialDietChoice(
            db,
            childWithFreeTextFieldAndRemainingCheckbox.id,
            "2",
            "AE1FE5FE-9619-4D7A-9043-A6B0C615156B",
            "Kananmunaton ruokavalio",
        )
        insertNekkuSpecialDietChoice(
            db,
            childWithFreeTextFieldAndRemovedCheckbox.id,
            "2",
            "17A9ACF0-DE9E-4C07-882E-C8C47351D009",
            "Random value",
        )
        insertNekkuSpecialDietChoice(
            db,
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

        fetchAndUpdateNekkuSpecialDiets(client, db, asyncJobRunner, now)

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

    @Test
    fun `removing special diets from children generates warning emails`() {

        val today = LocalDate.of(2025, 5, 19)
        val noonToday = HelsinkiDateTime.of(today, LocalTime.of(12, 0))

        var client = TestNekkuClient(specialDiets = listOf(getNekkuSpecialDiet()))
        fetchAndUpdateNekkuSpecialDiets(client, db, asyncJobRunner, noonToday)

        val firstChildWithNoSpecialDiet =
            DevPerson(firstName = "Anselmi", lastName = "Allergiation")
        val childWithSeveralAllergies = DevPerson(firstName = "Anneli", lastName = "Allerginen")
        val firstChildWithRemovedAllergy = DevPerson(firstName = "Lasse", lastName = "Laktoositon")
        val firstChildWithRemainingAllergy =
            DevPerson(firstName = "Kalle", lastName = "Kananmunaton")
        val secondChildWithRemovedAllergy = DevPerson(firstName = "Pirjo", lastName = "Pähkinäton")
        val secondChildWithNoAllergies =
            DevPerson(firstName = "Kirsi", lastName = "Kaikkiruokainen")
        val thirdChildWithNoAllergies = DevPerson(firstName = "Outi", lastName = "Ongelmaton")
        val secondChildWithRemainingAllergy =
            DevPerson(firstName = "Seppo", lastName = "Sianlihaton")

        val allChildren =
            listOf(
                firstChildWithNoSpecialDiet,
                childWithSeveralAllergies,
                firstChildWithRemovedAllergy,
                firstChildWithRemainingAllergy,
                secondChildWithRemovedAllergy,
                secondChildWithNoAllergies,
                secondChildWithRemainingAllergy,
                thirdChildWithNoAllergies,
            )

        db.transaction { tx -> allChildren.forEach { tx.insert(it, DevPersonType.CHILD) } }

        val area = DevCareArea()
        val daycare1 = DevDaycare(areaId = area.id)
        val daycare2 = DevDaycare(areaId = area.id)
        val daycare3 = DevDaycare(areaId = area.id)
        val group1 = DevDaycareGroup(daycareId = daycare1.id, name = "Karhukoplalaiset")
        val group2 = DevDaycareGroup(daycareId = daycare2.id, name = "Milla Magiat")
        val group3 = DevDaycareGroup(daycareId = daycare2.id, name = "Kulta-Into Piit")
        val group4 = DevDaycareGroup(daycareId = daycare3.id, name = "Arpin Lusènet")
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

        db.transaction { tx ->
            tx.insert(
                    DevPlacement(
                        childId = firstChildWithNoSpecialDiet.id,
                        unitId = daycare1.id,
                        startDate = today,
                        endDate = today,
                    )
                )
                .also { placementId ->
                    tx.insert(
                        DevDaycareGroupPlacement(
                            daycarePlacementId = placementId,
                            daycareGroupId = group1.id,
                            startDate = today,
                            endDate = today,
                        )
                    )
                }
            tx.insert(
                    DevPlacement(
                        childId = childWithSeveralAllergies.id,
                        unitId = daycare1.id,
                        startDate = today,
                        endDate = today,
                    )
                )
                .also { placementId ->
                    tx.insert(
                        DevDaycareGroupPlacement(
                            daycarePlacementId = placementId,
                            daycareGroupId = group1.id,
                            startDate = today,
                            endDate = today,
                        )
                    )
                }

            tx.insert(
                    DevPlacement(
                        childId = firstChildWithRemovedAllergy.id,
                        unitId = daycare2.id,
                        startDate = today,
                        endDate = today,
                    )
                )
                .also { placementId ->
                    tx.insert(
                        DevDaycareGroupPlacement(
                            daycarePlacementId = placementId,
                            daycareGroupId = group2.id,
                            startDate = today,
                            endDate = today,
                        )
                    )
                }
            tx.insert(
                    DevPlacement(
                        childId = firstChildWithRemainingAllergy.id,
                        unitId = daycare2.id,
                        startDate = today,
                        endDate = today,
                    )
                )
                .also { placementId ->
                    tx.insert(
                        DevDaycareGroupPlacement(
                            daycarePlacementId = placementId,
                            daycareGroupId = group2.id,
                            startDate = today,
                            endDate = today,
                        )
                    )
                }

            tx.insert(
                    DevPlacement(
                        childId = secondChildWithNoAllergies.id,
                        unitId = daycare2.id,
                        startDate = today,
                        endDate = today,
                    )
                )
                .also { placementId ->
                    tx.insert(
                        DevDaycareGroupPlacement(
                            daycarePlacementId = placementId,
                            daycareGroupId = group3.id,
                            startDate = today,
                            endDate = today,
                        )
                    )
                }
            tx.insert(
                    DevPlacement(
                        childId = secondChildWithRemovedAllergy.id,
                        unitId = daycare2.id,
                        startDate = today,
                        endDate = today,
                    )
                )
                .also { placementId ->
                    tx.insert(
                        DevDaycareGroupPlacement(
                            daycarePlacementId = placementId,
                            daycareGroupId = group3.id,
                            startDate = today,
                            endDate = today,
                        )
                    )
                }

            tx.insert(
                    DevPlacement(
                        childId = thirdChildWithNoAllergies.id,
                        unitId = daycare3.id,
                        startDate = today,
                        endDate = today,
                    )
                )
                .also { placementId ->
                    tx.insert(
                        DevDaycareGroupPlacement(
                            daycarePlacementId = placementId,
                            daycareGroupId = group4.id,
                            startDate = today,
                            endDate = today,
                        )
                    )
                }
            tx.insert(
                    DevPlacement(
                        childId = secondChildWithRemainingAllergy.id,
                        unitId = daycare3.id,
                        startDate = today,
                        endDate = today,
                    )
                )
                .also { placementId ->
                    tx.insert(
                        DevDaycareGroupPlacement(
                            daycarePlacementId = placementId,
                            daycareGroupId = group4.id,
                            startDate = today,
                            endDate = today,
                        )
                    )
                }
        }

        insertNekkuSpecialDietChoice(
            db,
            childWithSeveralAllergies.id,
            "2",
            "17A9ACF0-DE9E-4C07-882E-C8C47351D009",
            "Pähkinätön",
        )
        insertNekkuSpecialDietChoice(
            db,
            childWithSeveralAllergies.id,
            "2",
            "AE1FE5FE-9619-4D7A-9043-A6B0C615156B",
            "Laktoositon ruokavalio",
        )
        insertNekkuSpecialDietChoice(
            db,
            firstChildWithRemovedAllergy.id,
            "2",
            "AE1FE5FE-9619-4D7A-9043-A6B0C615156B",
            "Laktoositon ruokavalio",
        )
        insertNekkuSpecialDietChoice(
            db,
            firstChildWithRemainingAllergy.id,
            "2",
            "AE1FE5FE-9619-4D7A-9043-A6B0C615156B",
            "Kananmunaton ruokavalio",
        )
        insertNekkuSpecialDietChoice(
            db,
            secondChildWithRemovedAllergy.id,
            "2",
            "17A9ACF0-DE9E-4C07-882E-C8C47351D009",
            "Pähkinätön",
        )
        insertNekkuSpecialDietChoice(
            db,
            secondChildWithRemainingAllergy.id,
            "2",
            "AE1FE5FE-9619-4D7A-9043-A6B0C615156B",
            "Sianlihaton ruokavalio",
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

        fetchAndUpdateNekkuSpecialDiets(client, db, asyncJobRunner, noonToday) // triggers emails

        asyncJobRunner.runPendingJobsSync(MockEvakaClock(noonToday))

        val expectedTestContent1 =
            """
Seuraavilta lapsilta on poistunut allergiatietoja koska kyseinen kenttä on poistunut Nekusta. Tässä viestissä on lasten alkuperäiset allergiatiedot. Varmista että lasten tiedot päivitetään uusien Nekku-kenttien mukaisiksi.

Karhukoplalaiset
Lapsen tunniste: ${childWithSeveralAllergies.id}, lapsen ruokavaliot: Laktoositon ruokavalio, Pähkinätön
        """
                .trim()

        val expectedTestContent2 =
            """
Seuraavilta lapsilta on poistunut allergiatietoja koska kyseinen kenttä on poistunut Nekusta. Tässä viestissä on lasten alkuperäiset allergiatiedot. Varmista että lasten tiedot päivitetään uusien Nekku-kenttien mukaisiksi.

Kulta-Into Piit
Lapsen tunniste: ${secondChildWithRemovedAllergy.id}, lapsen ruokavaliot: Pähkinätön

Milla Magiat
Lapsen tunniste: ${firstChildWithRemovedAllergy.id}, lapsen ruokavaliot: Laktoositon ruokavalio
        """
                .trim()

        assertEquals(
            listOf(
                "supervisor1@city.fi" to expectedTestContent1,
                "supervisor2@city.fi" to expectedTestContent2,
            ),
            MockEmailClient.emails.sortedBy { it.toAddress }.map { it.toAddress to it.content.text },
        )
    }

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
}
