// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.nekku

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.TimeRange
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired

class NekkuIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var asyncJobRunner: AsyncJobRunner<AsyncJob>

    @Test
    fun `Nekku customer sync does not sync empty data`() {
        val client = TestNekkuClient()
        assertThrows<Exception> { fetchAndUpdateNekkuCustomers(client, db) }
    }

    @Test
    fun `Nekku customer sync does sync non-empty data`() {
        val client =
            TestNekkuClient(
                customers =
                    listOf(
                        NekkuCustomer(
                            "2501K6089",
                            "Ahvenojan päiväkoti",
                            "Varhaiskasvatus",
                            "large",
                        )
                    )
            )
        fetchAndUpdateNekkuCustomers(client, db)
        db.transaction { tx ->
            val customers = tx.getNekkuCustomers().toSet()
            assertEquals(1, customers.size)
        }
    }

    @Test
    fun `Nekku customer lists only 'Varhaiskasvatus'-data`() {
        val client =
            TestNekkuClient(
                customers =
                    listOf(
                        NekkuCustomer(
                            "2501K6089",
                            "Ahvenojan päiväkoti",
                            "Varhaiskasvatus",
                            "large",
                        ),
                        NekkuCustomer("4282K9253", "Haukiputaan lukio lipa", "Liikunta", ""),
                    )
            )
        fetchAndUpdateNekkuCustomers(client, db)
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
                        NekkuCustomer(
                            "2501K6089",
                            "Ahvenojan päiväkoti",
                            "Varhaiskasvatus",
                            "large",
                        )
                    )
            )
        fetchAndUpdateNekkuCustomers(client, db)

        db.transaction { tx ->
            val customers = tx.getNekkuCustomers().toSet()

            assertEquals(1, customers.size)
            assertEquals("Ahvenojan päiväkoti", customers.first().name)
            assertEquals("large", customers.first().unit_size)
        }

        client =
            TestNekkuClient(
                customers =
                    listOf(
                        NekkuCustomer(
                            "2501K6089",
                            "Ahvenojan päiväkoti MUUTETTU",
                            "Varhaiskasvatus",
                            "small",
                        )
                    )
            )
        fetchAndUpdateNekkuCustomers(client, db)
        db.transaction { tx ->
            val customers = tx.getNekkuCustomers().toSet()

            assertEquals(1, customers.size)
            assertEquals("Ahvenojan päiväkoti MUUTETTU", customers.first().name)
            assertEquals("small", customers.first().unit_size)
        }
    }

    fun getNekkuSpecialDiet(): NekkuApiSpecialDiet {
        val nekkuSpecialDiet =
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
        return nekkuSpecialDiet
    }

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

    val nekkuProducts =
        listOf(
            NekkuApiProduct(
                "Ateriapalvelu 1 kasvis",
                "31000010",
                "",
                "small",
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
                "small",
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
                "large",
                listOf(NekkuProductMealTime.DINNER),
                NekkuApiProductMealType.Vegaani,
            ),
            NekkuApiProduct("Lounas kasvis er", "31001011", "2", "medium", null, null),
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
                            "small",
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
                            "large",
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
                            "small",
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
                            "medium",
                            null,
                            NekkuApiProductMealType.Vegaani,
                        )
                    )
            )
        fetchAndUpdateNekkuProducts(client, db)
        db.transaction { tx ->
            val products = tx.getNekkuProducts()
            assertEquals("Ateriapalvelu 1 vegaani", products[0].name)
            assertEquals("2", products[0].options_id)
            assertEquals("medium", products[0].unit_size)
            assertEquals(null, products[0].meal_time)
            assertEquals(NekkuProductMealType.VEGAN, products[0].meal_type)
        }
    }

    @Test
    fun `meal order jobs for daycare groups without customer number are not planned`() {

        var client =
            TestNekkuClient(
                customers =
                    listOf(
                        NekkuCustomer(
                            "2501K6089",
                            "Ahvenojan päiväkoti",
                            "Varhaiskasvatus",
                            "large",
                        )
                    )
            )
        fetchAndUpdateNekkuCustomers(client, db)

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

        assertEquals(emptyList(), getJobs())
    }

    @Test
    fun `meal order jobs for daycare groups with customer number are planned`() {

        var client =
            TestNekkuClient(
                customers =
                    listOf(
                        NekkuCustomer(
                            "2501K6089",
                            "Ahvenojan päiväkoti",
                            "Varhaiskasvatus",
                            "large",
                        )
                    )
            )
        fetchAndUpdateNekkuCustomers(client, db)

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

        planNekkuOrderJobs(db, asyncJobRunner, now)

        assertEquals(7, getJobs().count())
    }

    @Test
    fun `meal order jobs for daycare groups are planned for two week time`() {

        var client =
            TestNekkuClient(
                customers =
                    listOf(
                        NekkuCustomer(
                            "2501K6089",
                            "Ahvenojan päiväkoti",
                            "Varhaiskasvatus",
                            "large",
                        )
                    )
            )
        fetchAndUpdateNekkuCustomers(client, db)

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

        val nowPlusTwoWeeks = HelsinkiDateTime.of(LocalDate.of(2025, 4, 14), LocalTime.of(2, 25))

        planNekkuOrderJobs(db, asyncJobRunner, now)

        assertEquals(nowPlusTwoWeeks.toLocalDate().toString(), getJobs().first().date.toString())
    }

    @Test
    fun `Send Nekku orders`() {

        // First create all of the basic backgrounds like
        // Products
        // Customer numbers
        // Daycare with groups
        // Children with placements in the group and they are not absent
        db.transaction { tx ->
            val products = tx.getNekkuProducts()

        }
    }

    private fun getJobs() =
        db.read { tx ->
            tx.createQuery { sql("SELECT payload FROM async_job WHERE type = 'SendNekkuOrder'") }
                .map { jsonColumn<AsyncJob.SendNekkuOrder>("payload") }
                .toList()
        }
}

class TestNekkuClient(
    private val customers: List<NekkuCustomer> = emptyList(),
    private val specialDiets: List<NekkuApiSpecialDiet> = emptyList(),
    private val nekkuProducts: List<NekkuApiProduct> = emptyList(),
) : NekkuClient {
    val orders = mutableListOf<NekkuClient.NekkuOrders>()

    override fun getCustomers(): List<NekkuCustomer> {
        return customers
    }

    override fun getSpecialDiets(): List<NekkuApiSpecialDiet> {
        return specialDiets
    }

    override fun getProducts(): List<NekkuApiProduct> {
        return nekkuProducts
    }

    override fun createNekkuMealOrder(nekkuOrders: NekkuClient.NekkuOrders) {
        orders.add(nekkuOrders)
    }
}
