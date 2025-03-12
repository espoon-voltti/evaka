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

    @Test
    fun `Nekku special diets sync does not sync empty data`() {
        val client = TestNekkuClient()
        assertThrows<Exception> { fetchAndUpdateNekkuSpecialDiets(client, db) }
    }

    @Test
    fun `Nekku special diets sync does sync non-empty data`() {
        val client =
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
            val specialDiets = tx.getNekkuSpecialDiets()
            assertEquals(1, specialDiets)
        }
    }
}

class TestNekkuClient(
    private val customers: List<NekkuCustomer> = emptyList(),
    private val specialDiets: List<NekkuSpecialDiet> = emptyList(),
) : NekkuClient {

    override fun getCustomers(): List<NekkuCustomer> {
        return customers
    }

    override fun getSpecialDiets(): List<NekkuSpecialDiet> {
        return specialDiets
    }
}
