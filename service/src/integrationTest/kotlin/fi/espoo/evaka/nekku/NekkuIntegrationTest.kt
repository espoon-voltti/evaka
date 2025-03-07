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
                            //TODO
                        )
                    )
            )
        fetchAndUpdateNekkuCustomers(client, db)
        db.transaction { tx ->
            val customers = tx.getNekkuCustomers().toSet()
            assertEquals(1, customers.size)
        }
    }


}




class TestNekkuClient(
    private val customers: List<NekkuCustomer> = emptyList(),
    private val specialDiets: List<NekkuSpecialDiet> = emptyList(),
    private val products: List<NekkuProduct> = emptyList()
) : NekkuClient {

    override fun getCustomers(): List<NekkuCustomer> {
        return customers
    }

    override fun getSpecialDiets(): List<NekkuSpecialDiet> {
        return specialDiets
    }

    override fun getProducts(): List<NekkuProduct> {
        return products
    }
}
