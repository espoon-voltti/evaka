// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.espoo.bi

import fi.espoo.evaka.EspooBiEnv
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.Sensitive
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired

class EspooBiJobTest : FullApplicationTest(resetDbBeforeEach = true) {
    private val clock = MockEvakaClock(HelsinkiDateTime.of(LocalDateTime.of(2023, 1, 1, 1, 0)))

    @Autowired private lateinit var mockEndpoint: MockBiEndpoint
    private lateinit var job: EspooBiJob

    @BeforeAll
    override fun beforeAll() {
        super.beforeAll()
        val client =
            EspooBiHttpClient(
                EspooBiEnv(
                    url = "http://localhost:$httpPort/public/mock-espoo-bi",
                    username = "user",
                    password = Sensitive("password")
                )
            )
        job = EspooBiJob(client)
    }

    @BeforeEach
    fun beforeEach() {
        mockEndpoint.clearData()
    }

    @Test
    fun `BI client sends a stream successfully`() {
        val record = "line\n"
        val recordCount = 100_000
        job.sendBiTable(
            db,
            clock,
            "test",
            object : CsvQuery {
                override fun <R> invoke(
                    tx: Database.Read,
                    useResults: (records: Sequence<String>) -> R
                ) = useResults(generateSequence { record }.take(recordCount))
            }
        )

        val request = mockEndpoint.getCapturedRequests().values.single()
        val expected = record.toByteArray(CSV_CHARSET)
        val chunks =
            request.body
                .asSequence()
                .chunked(expected.size, List<Byte>::toByteArray)
                .toList()
        assertEquals(recordCount, chunks.size)
        for (chunk in chunks) {
            assertContentEquals(expected, chunk)
        }
    }
}
