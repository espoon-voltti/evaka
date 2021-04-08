// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.async

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.mapJsonColumn
import org.jdbi.v3.core.kotlin.mapTo
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
    private val user = AuthenticatedUser.SystemInternalUser

    @BeforeEach
    fun beforeEach() {
        db.transaction { it.execute("TRUNCATE async_job") }
    }

    @Test
    fun testCompleteHappyCase() {
        val id = UUID.randomUUID()
        db.transaction {
            it.insertJob(
                JobParams(
                    NotifyDecisionCreated(id, user, sendAsMessage = false),
                    1234,
                    Duration.ofMinutes(42)
                )
            )
        }
        val runAt = db.read { it.createQuery("SELECT run_at FROM async_job").mapTo<ZonedDateTime>().one() }

        val ref = db.transaction { it.claimJob(listOf(AsyncJobType.DECISION_CREATED))!! }
        assertEquals(AsyncJobType.DECISION_CREATED, ref.jobType)
        val (retryRunAt, retryCount) = db.read {
            it.createQuery("SELECT run_at, retry_count FROM async_job").mapTo<Retry>().one()
        }
        assertTrue(retryRunAt > runAt)
        assertEquals(1233, retryCount)

        db.transaction { tx ->
            val payload = tx.startJob(ref, NotifyDecisionCreated::class.java)!!
            assertEquals(NotifyDecisionCreated(id, user, sendAsMessage = false), payload)

            tx.completeJob(ref)
        }

        val completedAt =
            db.read { it.createQuery("SELECT completed_at FROM async_job").mapTo<ZonedDateTime>().one() }
        assertTrue(completedAt > runAt)
    }

    @Test
    fun testParallelClaimContention() {
        val payloads = (0..1).map { NotifyDecisionCreated(UUID.randomUUID(), user, sendAsMessage = false) }
        db.transaction { tx ->
            payloads.map { tx.insertJob(JobParams(it, 999, Duration.ZERO)) }
        }
        val handles = (0..2).map { jdbi.open() }
        try {
            val h0 = handles[0].begin()
            val h1 = handles[1].begin()
            val h2 = handles[2].begin()

            // Two jobs in the db -> only two claims should succeed
            val job0 = Database.Transaction.wrap(h0).claimJob()!!
            val job1 = Database.Transaction.wrap(h1).claimJob()!!
            assertNotEquals(job0.jobId, job1.jobId)
            assertNull(Database.Transaction.wrap(h2).claimJob())

            h1.rollback()

            // Handle 1 rolled back -> job 1 should now be available
            val job2 = Database.Transaction.wrap(h2).claimJob()!!
            assertEquals(job1.jobId, job2.jobId)

            Database.Transaction.wrap(h0).completeJob(job0)
            h0.commit()

            Database.Transaction.wrap(h2).completeJob(job2)
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
        val user = db.read {
            it.createQuery(
                "SELECT jsonb_build_object('id', '44e1ff31-fce4-4ca1-922a-a385fb21c69e', 'roles', jsonb_build_array('ROLE_EVAKA_ESPOO_FINANCEADMIN')) AS user"
            ).map { row -> row.mapJsonColumn<AuthenticatedUser>("user") }.asSequence().first()
        }
        assertEquals(
            AuthenticatedUser.Employee(
                UUID.fromString("44e1ff31-fce4-4ca1-922a-a385fb21c69e"),
                setOf(UserRole.FINANCE_ADMIN)
            ),
            user
        )
    }

    data class Retry(val runAt: ZonedDateTime, val retryCount: Long)
}
