// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.nekku

import fi.espoo.evaka.FullApplicationTest
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class NekkuIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {

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

    fun getNekkuSpecialDiet(): NekkuSpecialDiet {
        var nekkuSpecialDiet =
            NekkuSpecialDiet(
                "2",
                "Päiväkodit er.",
                listOf(
                    NekkuSpecialDietsField(
                        "17A9ACF0-DE9E-4C07-882E-C8C47351D009",
                        "Muu erityisruokavalio, mikä?",
                        NekkuSpecialDietType.TEXT,
                    ),
                    NekkuSpecialDietsField(
                        "AE1FE5FE-9619-4D7A-9043-A6B0C615156B",
                        "Erityisruokavaliot",
                        NekkuSpecialDietType.CHECKBOXLIST,
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
                        NekkuSpecialDiet(
                            "2",
                            "Päiväkodit erikois",
                            listOf(
                                NekkuSpecialDietsField(
                                    "17A9ACF0-DE9E-4C07-882E-C8C47351D009",
                                    "Muu erityisruokavalio, mikä?",
                                    NekkuSpecialDietType.TEXT,
                                ),
                                NekkuSpecialDietsField(
                                    "AE1FE5FE-9619-4D7A-9043-A6B0C615156B",
                                    "Erityisruokavaliot",
                                    NekkuSpecialDietType.CHECKBOXLIST,
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
                        NekkuSpecialDiet(
                            "2",
                            "Päiväkodit er.",
                            listOf(
                                NekkuSpecialDietsField(
                                    "17A9ACF0-DE9E-4C07-882E-C8C47351D009",
                                    "Muu erityisruokavalio, mikä?",
                                    NekkuSpecialDietType.TEXT,
                                ),
                                NekkuSpecialDietsField(
                                    "AE1FE5FE-9619-4D7A-9043-A6B0C615156B",
                                    "Erityisruokavaliot",
                                    NekkuSpecialDietType.CHECKBOXLIST,
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
                        NekkuSpecialDiet(
                            "3",
                            "Päiväkodit erikoiset",
                            listOf(
                                NekkuSpecialDietsField(
                                    "17A9ACF0-DE9E-4C07-882E-C8C47351D008",
                                    "Muu erityisruokavalio, mikä?",
                                    NekkuSpecialDietType.TEXT,
                                ),
                                NekkuSpecialDietsField(
                                    "AE1FE5FE-9619-4D7A-9043-A6B0C6151566",
                                    "Erityisruokavaliot",
                                    NekkuSpecialDietType.CHECKBOXLIST,
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
            NekkuProduct(
                "Ateriapalvelu 1 kasvis",
                "31000010",
                "",
                "small",
                listOf(
                    NekkuProductMealTime.BREAKFAST,
                    NekkuProductMealTime.LUNCH,
                    NekkuProductMealTime.SNACK,
                ),
                NekkuProductMealType.VEGETABLE,
            ),
            NekkuProduct(
                "Ateriapalvelu 1 kasvis er",
                "31000011",
                "2",
                "small",
                listOf(
                    NekkuProductMealTime.BREAKFAST,
                    NekkuProductMealTime.LUNCH,
                    NekkuProductMealTime.SNACK,
                ),
                NekkuProductMealType.VEGETABLE,
            ),
            NekkuProduct(
                "Päivällinen vegaani päiväkoti",
                "31000008",
                "",
                "large",
                listOf(NekkuProductMealTime.DINNER),
                NekkuProductMealType.VEGAN,
            ),
            NekkuProduct("Lounas kasvis er", "31001011", "2", "medium", null, null),
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
        db.transaction { tx ->
            val products = tx.getNekkuProducts()
            assertEquals(4, products.size)
        }
    }

    @Test
    fun `Nekku product deletes old products`() {
        var client = TestNekkuClient(nekkuProducts = nekkuProducts)
        fetchAndUpdateNekkuProducts(client, db)
        db.transaction { tx ->
            val products = tx.getNekkuProducts()
            assertEquals(4, products.size)
        }

        client =
            TestNekkuClient(
                nekkuProducts =
                    listOf(
                        NekkuProduct(
                            "Ateriapalvelu 1 kasvis",
                            "31000010",
                            "",
                            "small",
                            listOf(
                                NekkuProductMealTime.BREAKFAST,
                                NekkuProductMealTime.LUNCH,
                                NekkuProductMealTime.SNACK,
                            ),
                            NekkuProductMealType.VEGETABLE,
                        ),
                        NekkuProduct(
                            "Päivällinen vegaani päiväkoti",
                            "31000008",
                            "",
                            "large",
                            listOf(NekkuProductMealTime.DINNER),
                            NekkuProductMealType.VEGAN,
                        ),
                    )
            )
        fetchAndUpdateNekkuProducts(client, db)
        db.transaction { tx ->
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
                        NekkuProduct(
                            "Ateriapalvelu 1 kasvis",
                            "31000010",
                            "",
                            "small",
                            listOf(
                                NekkuProductMealTime.BREAKFAST,
                                NekkuProductMealTime.LUNCH,
                                NekkuProductMealTime.SNACK,
                            ),
                            NekkuProductMealType.VEGETABLE,
                        )
                    )
            )
        fetchAndUpdateNekkuProducts(client, db)
        db.transaction { tx ->
            val products = tx.getNekkuProducts()
            assertEquals(1, products.size)
        }

        client =
            TestNekkuClient(
                nekkuProducts =
                    listOf(
                        NekkuProduct(
                            "Ateriapalvelu 1 vegaani",
                            "31000010",
                            "2",
                            "medium",
                            null,
                            NekkuProductMealType.VEGAN,
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
}

class TestNekkuClient(
    private val customers: List<NekkuCustomer> = emptyList(),
    private val specialDiets: List<NekkuSpecialDiet> = emptyList(),
    private val nekkuProducts: List<NekkuProduct> = emptyList(),
) : NekkuClient {

    override fun getCustomers(): List<NekkuCustomer> {
        return customers
    }

    override fun getSpecialDiets(): List<NekkuSpecialDiet> {
        return specialDiets
    }

    override fun getProducts(): List<NekkuProduct> {
        return nekkuProducts
    }
}
