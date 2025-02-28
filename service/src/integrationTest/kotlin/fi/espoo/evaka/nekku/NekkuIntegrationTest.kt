// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.nekku

import fi.espoo.evaka.FullApplicationTest
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

private val logger = KotlinLogging.logger {}

val loggerWarner: (String) -> Unit = { s -> logger.warn { s } }

class NekkuIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {

    @Test
    fun `Nekku customer sync does not sync empty data`() {
        val client = TestNekkuClient()
        assertThrows<Exception> { fetchAndUpdateNekkuCustomers(client, db, loggerWarner) }
    }

    @Test
    fun `Nekku customer sync does not sync non-empty data`() {
        val client =
            TestNekkuClient(
                customers =
                    listOf(
                        NekkuCustomer("2501K6089", "Ahvenojan päiväkoti", group = "Varhaiskasvatus")
                    )
            )
        fetchAndUpdateNekkuCustomers(client, db, loggerWarner)
        db.transaction { tx ->
            val customers = tx.getNekkuCustomers().toSet()
            assertEquals(1, customers.size)
        }
    }
}

class TestNekkuClient(
    private val customers: List<NekkuCustomer> = emptyList()
) : NekkuClient {

    override fun getCustomers(): List<NekkuCustomer> {
        return customers
    }
}
