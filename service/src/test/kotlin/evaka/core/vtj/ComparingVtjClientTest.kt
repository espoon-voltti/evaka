// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.vtj

import evaka.core.identity.Ssn
import evaka.core.shared.EvakaUserId
import evaka.core.shared.domain.MockEvakaClock
import java.time.Duration
import java.util.UUID
import kotlin.test.assertEquals
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

class ComparingVtjClientTest {
    private val user = EvakaUserId(UUID.randomUUID())
    private val ssn = Ssn.from("070644-937X")
    private val person =
        VtjPerson(
            firstNames = "Johannes",
            lastName = "Karhula",
            ssn = ssn,
            restrictedDetails = null,
        )
    private val soapClient = FixedVtjClient(person)
    private val dvvClient = FixedVtjClient(person)
    private val clock = MockEvakaClock(2026, 1, 1, 12, 0)
    private val client =
        ComparingVtjClient(soapClient, dvvClient, interval = Duration.ofHours(1), clock = clock)

    @AfterEach
    fun afterEach() {
        client.close()
    }

    @Test
    fun `the calls are compared with one DVV query for each query type`() {
        assertEquals(mapOf(ssn to person), client.fetchPersons(user, listOf(ssn)))
        val _ = client.fetchPersons(user, listOf(ssn))
        val _ = client.fetchWithGuardians(user, listOf(ssn))
        client.compareQueued()

        assertEquals(listOf("fetchPersons", "fetchWithGuardians"), dvvClient.calls)
    }

    @Test
    fun `a failed DVV query pauses the comparison, and the calls of the round are tried again after the pause`() {
        dvvClient.fails = true
        val _ = client.fetchPersons(user, listOf(ssn))
        val _ = client.fetchWithGuardians(user, listOf(ssn))
        client.compareQueued()
        dvvClient.fails = false
        client.compareQueued()
        assertEquals(listOf("fetchPersons"), dvvClient.calls)

        clock.tick(Duration.ofMinutes(1))
        client.compareQueued()

        assertEquals(listOf("fetchPersons", "fetchPersons", "fetchWithGuardians"), dvvClient.calls)
    }

    @Test
    fun `a call is tried at most three times`() {
        dvvClient.fails = true
        val _ = client.fetchPersons(user, listOf(ssn))
        repeat(4) {
            client.compareQueued()
            clock.tick(Duration.ofMinutes(1))
        }

        assertEquals(listOf("fetchPersons", "fetchPersons", "fetchPersons"), dvvClient.calls)
    }

    @Test
    fun `a call without comparison is not queued`() {
        val _ = ComparingVtjClient.withoutComparison { client.fetchPersons(user, listOf(ssn)) }
        client.compareQueued()

        assertEquals(emptyList(), dvvClient.calls)
    }

    @Test
    fun `a full queue drops the new comparisons`() {
        ComparingVtjClient(soapClient, dvvClient, interval = Duration.ofHours(1), queueSize = 2)
            .use { client ->
                val _ =
                    client.fetchPersons(
                        user,
                        listOf(ssn, Ssn.from("150385-9013"), Ssn.from("200687-903A")),
                    )
                client.compareQueued()
            }

        assertEquals(listOf(listOf(ssn, Ssn.from("150385-9013"))), dvvClient.requestedSsns)
    }
}

private class FixedVtjClient(private val person: VtjPerson) : VtjClient {
    var fails = false
    val calls = mutableListOf<String>()
    val requestedSsns = mutableListOf<List<Ssn>>()

    override fun fetchPersons(requestingUser: EvakaUserId, ssns: Collection<Ssn>) =
        fetch("fetchPersons", ssns) { person }

    override fun fetchWithGuardians(requestingUser: EvakaUserId, ssns: Collection<Ssn>) =
        fetch("fetchWithGuardians", ssns) { VtjPersonAndGuardians(person, emptyList()) }

    override fun fetchWithDependants(requestingUser: EvakaUserId, ssns: Collection<Ssn>) =
        fetch("fetchWithDependants", ssns) { VtjPersonAndDependants(person, emptyList()) }

    private fun <T> fetch(name: String, ssns: Collection<Ssn>, result: () -> T): Map<Ssn, T> {
        calls += name
        requestedSsns += ssns.toList()
        if (fails) error("DVV failed")
        return mapOf(person.ssn to result()).filterKeys { it in ssns }
    }
}
