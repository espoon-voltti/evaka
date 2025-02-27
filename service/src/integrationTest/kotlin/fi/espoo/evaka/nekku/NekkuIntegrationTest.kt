package fi.espoo.evaka.nekku

import fi.espoo.evaka.FullApplicationTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class NekkuIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {


    @Test
    fun `diet sync does not sync empty data`() {
        val client = TestNekkuClient()
        assertThrows<Exception> { fetchAndUpdateNekkuCustomers(client, db, asyncJobRunner, now) }
    }
}


class TestNekkuClient(
    val customerNumbers: CustomerNumbers,
) : NekkuClient {

    data class NekkuCustomer(val number: String, val name: String)
    val customers = mutableListOf<NekkuCustomer>()

    override fun getCustomers(): List<NekkuCustomer> {
        return customers.map { (customerNumber, customerId) ->
            NekkuClient.NekkuCustomer(customerId, customerNumber)
        }
    }
}