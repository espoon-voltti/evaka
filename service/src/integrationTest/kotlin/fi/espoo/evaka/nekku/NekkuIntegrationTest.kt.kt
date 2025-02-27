package fi.espoo.evaka.nekku

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.jamix.JamixClient
import fi.espoo.evaka.jamix.JamixSpecialDiet
import fi.espoo.evaka.jamix.JamixTexture
import fi.espoo.evaka.jamix.TestJamixClient
import fi.espoo.evaka.jamix.fetchAndUpdateJamixDiets
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