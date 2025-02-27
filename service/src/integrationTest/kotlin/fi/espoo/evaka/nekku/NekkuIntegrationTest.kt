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
        val client = TestNekkuClient(customers = mapOf(Pair("2501K6089", "Ahvenojan päiväkoti")))
        fetchAndUpdateNekkuCustomers(client, db, loggerWarner)
        db.transaction { tx ->
            val customers = tx.getNekkuCustomers().toSet()
            assertEquals(1, customers.size)
        }
    }
}

class TestNekkuClient(val customers: Map<String, String> = mapOf()) : NekkuClient {

    override fun getCustomers(): List<NekkuClient.NekkuCustomer> {
        return customers.map { (number, name) -> NekkuClient.NekkuCustomer(number, name) }
    }
}
