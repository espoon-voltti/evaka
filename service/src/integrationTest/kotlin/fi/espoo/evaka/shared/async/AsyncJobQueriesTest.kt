// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.async

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.mapJsonColumn
import fi.espoo.evaka.shared.db.transaction
import org.jdbi.v3.core.kotlin.mapTo
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.ZonedDateTime
import java.util.UUID

class AsyncJobQueriesTest : PureJdbiTest() {
    private val user = AuthenticatedUser(UUID.randomUUID(), setOf())

    @BeforeEach
    @AfterAll
    fun clean() {
        jdbi.open().use { h -> h.execute("TRUNCATE async_job") }
    }

    @Test
    fun testCompleteHappyCase() {
        val id = UUID.randomUUID()
        jdbi.open().use { h ->
            h.transaction { tx ->
                insertJob(tx, JobParams(NotifyDecisionCreated(id, user), 1234, Duration.ofMinutes(42)))
            }
            val runAt = h.createQuery("SELECT run_at FROM async_job").mapTo<ZonedDateTime>().one()

            val ref = h.transaction { tx ->
                claimJob(tx, listOf(AsyncJobType.DECISION_CREATED))!!
            }
            assertEquals(AsyncJobType.DECISION_CREATED, ref.jobType)
            val (retryRunAt, retryCount) = h.createQuery("SELECT run_at, retry_count FROM async_job").mapTo<Retry>()
                .one()
            assertTrue(retryRunAt > runAt)
            assertEquals(1233, retryCount)

            h.transaction { tx ->
                val payload = startJob(tx, ref, NotifyDecisionCreated::class.java)!!
                assertEquals(NotifyDecisionCreated(id, user), payload)

                completeJob(tx, ref)
            }

            val completedAt = h.createQuery("SELECT completed_at FROM async_job").mapTo<ZonedDateTime>().one()
            assertTrue(completedAt > runAt)
        }
    }

    @Test
    fun testParallelClaimContention() {
        val payloads = (0..1).map { _ -> NotifyDecisionCreated(UUID.randomUUID(), user) }
        jdbi.open().use { h ->
            h.transaction { tx ->
                payloads.map { insertJob(tx, JobParams(it, 999, Duration.ZERO)) }
            }
        }
        val handles = (0..2).map { jdbi.open() }
        try {
            val h0 = handles[0].begin()
            val h1 = handles[1].begin()
            val h2 = handles[2].begin()

            // Two jobs in the db -> only two claims should succeed
            val job0 = claimJob(h0)!!
            val job1 = claimJob(h1)!!
            assertNotEquals(job0.jobId, job1.jobId)
            assertNull(claimJob(h2))

            h1.rollback()

            // Handle 1 rolled back -> job 1 should now be available
            val job2 = claimJob(h2)!!
            assertEquals(job1.jobId, job2.jobId)

            completeJob(h0, job0)
            h0.commit()

            completeJob(h2, job2)
            h2.commit()
        } finally {
            handles.forEach { it.close() }
        }
        val completedCount = jdbi.open()
            .use { h ->
                h.createQuery("SELECT count(*) FROM async_job WHERE completed_at IS NOT NULL").mapTo<Int>().one()
            }
        assertEquals(2, completedCount)
    }

    @Test
    fun testLegacyUserRoleDeserialization() {
        val user = jdbi.open().use {
            it.createQuery(
                "SELECT jsonb_build_object('id', '44e1ff31-fce4-4ca1-922a-a385fb21c69e', 'roles', jsonb_build_array('ROLE_EVAKA_ESPOO_FINANCEADMIN')) AS user"
            ).map { row -> row.mapJsonColumn<AuthenticatedUser>("user") }.asSequence().first()
        }
        assertEquals(
            AuthenticatedUser(
                UUID.fromString("44e1ff31-fce4-4ca1-922a-a385fb21c69e"),
                setOf(UserRole.FINANCE_ADMIN)
            ),
            user
        )
    }

    data class Retry(val runAt: ZonedDateTime, val retryCount: Long)
}
