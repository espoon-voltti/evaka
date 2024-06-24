// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.async

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test

class AsyncJobQueriesTest : PureJdbiTest(resetDbBeforeEach = true) {
    private data class TestJob(
        val data: UUID
    ) : AsyncJobPayload {
        override val user: AuthenticatedUser? = null
    }

    private val jobType = AsyncJobType(TestJob::class)

    @Test
    fun testCompleteHappyCase() {
        val id = UUID.randomUUID()
        db.transaction {
            it.insertJob(
                JobParams(TestJob(id), 1234, Duration.ofMinutes(42), HelsinkiDateTime.now())
            )
        }
        val runAt =
            db.read {
                @Suppress("DEPRECATION")
                it.createQuery("SELECT run_at FROM async_job").exactlyOne<HelsinkiDateTime>()
            }

        val ref = db.transaction { it.claimJob(HelsinkiDateTime.now(), listOf(jobType))!! }
        assertEquals(jobType, ref.jobType)
        val (retryRunAt, retryCount) =
            db.read {
                @Suppress("DEPRECATION")
                it.createQuery("SELECT run_at, retry_count FROM async_job").exactlyOne<Retry>()
            }
        assertTrue(retryRunAt > runAt)
        assertEquals(1233, retryCount)

        db.transaction { tx ->
            val payload = tx.startJob(ref, HelsinkiDateTime.now())!!
            assertEquals(TestJob(id), payload)

            tx.completeJob(ref, HelsinkiDateTime.now())
        }

        val completedAt =
            db.read {
                @Suppress("DEPRECATION")
                it.createQuery("SELECT completed_at FROM async_job").exactlyOne<HelsinkiDateTime>()
            }
        assertTrue(completedAt > runAt)
    }

    @Test
    fun testParallelClaimContention() {
        val payloads = (0..1).map { TestJob(UUID.randomUUID()) }
        db.transaction { tx ->
            payloads.map { tx.insertJob(JobParams(it, 999, Duration.ZERO, HelsinkiDateTime.now())) }
        }
        val handles = (0..2).map { jdbi.open() }
        try {
            val h0 = handles[0].begin()
            val h1 = handles[1].begin()
            val h2 = handles[2].begin()

            // Two jobs in the db -> only two claims should succeed
            val job0 =
                Database.Transaction.wrap(h0).claimJob(HelsinkiDateTime.now(), listOf(jobType))!!
            val job1 =
                Database.Transaction.wrap(h1).claimJob(HelsinkiDateTime.now(), listOf(jobType))!!
            assertNotEquals(job0.jobId, job1.jobId)
            assertNull(
                Database.Transaction.wrap(h2).claimJob(HelsinkiDateTime.now(), listOf(jobType))
            )

            h1.rollback()

            // Handle 1 rolled back -> job 1 should now be available
            val job2 =
                Database.Transaction.wrap(h2).claimJob(HelsinkiDateTime.now(), listOf(jobType))!!
            assertEquals(job1.jobId, job2.jobId)

            Database.Transaction.wrap(h0).completeJob(job0, HelsinkiDateTime.now())
            h0.commit()

            Database.Transaction.wrap(h2).completeJob(job2, HelsinkiDateTime.now())
            h2.commit()
        } finally {
            handles.forEach { it.close() }
        }
        val completedCount =
            jdbi.open().use { h ->
                h
                    .createQuery("SELECT count(*) FROM async_job WHERE completed_at IS NOT NULL")
                    .mapTo(Int::class.java)
                    .one()
            }
        assertEquals(2, completedCount)
    }

    @Test
    fun testRemoveOldAsyncJobs() {
        val now = HelsinkiDateTime.of(LocalDate.of(2020, 9, 1), LocalTime.of(12, 0))
        val ancient = LocalDate.of(2019, 1, 1)
        val recent = LocalDate.of(2020, 3, 1)
        val future = LocalDate.of(2020, 9, 2)
        db.transaction { tx ->
            listOf(ancient, recent)
                .flatMap {
                    listOf(
                        TestJobParams(it, completed = false),
                        TestJobParams(it, completed = true)
                    )
                }.forEach { params -> tx.insertTestJob(params) }
            tx.insertTestJob(TestJobParams(future, completed = false))
        }

        db.removeOldAsyncJobs(now)

        val remainingJobs =
            db.read {
                @Suppress("DEPRECATION")
                it
                    .createQuery(
                        "SELECT run_at, completed_at IS NOT NULL AS completed FROM async_job ORDER BY 1,2"
                    ).toList {
                        TestJobParams(
                            runAt = column<HelsinkiDateTime>("run_at").toLocalDate(),
                            completed = column("completed")
                        )
                    }
            }
        assertEquals(
            listOf(
                TestJobParams(recent, completed = false),
                TestJobParams(recent, completed = true),
                TestJobParams(future, completed = false)
            ),
            remainingJobs
        )
    }

    private data class Retry(
        val runAt: HelsinkiDateTime,
        val retryCount: Long
    )
}

private data class TestJobParams(
    val runAt: LocalDate,
    val completed: Boolean
)

private fun Database.Transaction.insertTestJob(params: TestJobParams) =
    @Suppress("DEPRECATION")
    createUpdate(
        """
INSERT INTO async_job (type, run_at, retry_count, retry_interval, payload, claimed_at, claimed_by, completed_at)
VALUES ('TestJob', :runAt, 0, interval '1 hours', '{}', :completedAt, :claimedBy, :completedAt)
    """
    ).bind("runAt", HelsinkiDateTime.of(params.runAt, LocalTime.of(12, 0)))
        .bind(
            "completedAt",
            HelsinkiDateTime.of(params.runAt, LocalTime.of(14, 0)).takeIf { params.completed }
        ).bind("claimedBy", 42.takeIf { params.completed })
        .execute()
