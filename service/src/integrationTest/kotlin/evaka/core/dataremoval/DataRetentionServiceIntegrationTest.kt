// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.dataremoval

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import evaka.core.FullApplicationTest
import evaka.core.application.ApplicationStatus
import evaka.core.application.ApplicationType
import evaka.core.application.persistence.club.Adult as ClubAdult
import evaka.core.application.persistence.club.Apply as ClubApply
import evaka.core.application.persistence.club.Child as ClubChild
import evaka.core.application.persistence.club.ClubFormV0
import evaka.core.childimages.insertChildImage
import evaka.core.shared.PersonId
import evaka.core.shared.async.AsyncJob
import evaka.core.shared.async.AsyncJobRunner
import evaka.core.shared.db.Database
import evaka.core.shared.dev.DevCareArea
import evaka.core.shared.dev.DevDaycare
import evaka.core.shared.dev.DevPerson
import evaka.core.shared.dev.DevPersonType
import evaka.core.shared.dev.DevPlacement
import evaka.core.shared.dev.insert
import evaka.core.shared.dev.insertTestApplication
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.shared.domain.MockEvakaClock
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

class DataRetentionServiceIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var dataRetentionService: DataRetentionService
    @Autowired private lateinit var asyncJobRunner: AsyncJobRunner<AsyncJob>

    private val today = LocalDate.of(2026, 9, 12)
    private val clock = MockEvakaClock(HelsinkiDateTime.of(today, LocalTime.of(2, 0)))

    private val area = DevCareArea()
    private val daycare = DevDaycare(areaId = area.id)
    private val longGone = DevPerson(dateOfBirth = today.minusYears(15))
    private val recent = DevPerson(dateOfBirth = today.minusYears(5))

    private val logAppender = ListAppender<ILoggingEvent>()
    private val rootLogger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger

    @BeforeEach
    fun startLogCapture() {
        logAppender.start()
        rootLogger.addAppender(logAppender)
    }

    @AfterEach
    fun stopLogCapture() {
        rootLogger.detachAppender(logAppender)
        logAppender.stop()
    }

    @BeforeEach
    fun setup() {
        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(longGone, DevPersonType.CHILD)
            tx.insert(recent, DevPersonType.CHILD)
            tx.execute {
                sql(
                    "UPDATE person SET created = ${bind(HelsinkiDateTime.of(today.minusYears(13), LocalTime.NOON))} WHERE id = ${bind(longGone.id)}"
                )
            }
            tx.insert(
                DevPlacement(
                    childId = longGone.id,
                    unitId = daycare.id,
                    startDate = today.minusYears(12),
                    endDate = today.minusYears(11),
                )
            )
            tx.insertChildImage(longGone.id)
            tx.insert(
                DevPlacement(
                    childId = recent.id,
                    unitId = daycare.id,
                    startDate = today.minusYears(2),
                    endDate = today.minusYears(1),
                )
            )
        }
    }

    private fun remaining(id: PersonId): Set<String> = db.read { tx ->
        tx.createQuery {
                sql(
                    """
SELECT 'person' AS t FROM person WHERE id = ${bind(id)}
UNION ALL SELECT 'child' FROM child WHERE id = ${bind(id)}
UNION ALL SELECT DISTINCT 'placement' FROM placement WHERE child_id = ${bind(id)}
UNION ALL SELECT DISTINCT 'child_images' FROM child_images WHERE child_id = ${bind(id)}
"""
                )
            }
            .toSet<String>()
    }

    private fun lastAttemptedAt(id: PersonId): HelsinkiDateTime? = db.read { tx ->
        tx.createQuery {
                sql("SELECT data_retention_last_attempted_at FROM person WHERE id = ${bind(id)}")
            }
            .exactlyOne<HelsinkiDateTime?>()
    }

    private fun realRun(id: PersonId) =
        runDataRetention(
            db,
            clock,
            asyncJobRunner,
            buildDataRetentionSchema(
                valueDecisionCapacityFactorEnabled = false,
                overrides = DataRetentionOverrides.NONE,
            ),
            id,
            dryRun = false,
            optimized = true,
        )

    @Test
    fun `a run deletes the expired data of the person, queues the file deletions, and leaves a recent child alone`() {
        realRun(longGone.id)
        realRun(recent.id)
        // The image deletion that the first run queued
        assertEquals(1, asyncJobRunner.runPendingJobsSync(clock))

        assertEquals(emptySet(), remaining(longGone.id))
        assertEquals(setOf("person", "child", "placement"), remaining(recent.id))
    }

    @Test
    fun `a person who no longer exists is skipped`() {
        realRun(PersonId(UUID.randomUUID()))
        assertEquals(0, asyncJobRunner.runPendingJobsSync(clock))
    }

    @Test
    fun `a dry run deletes nothing and queues nothing`() {
        dataRetentionService.deleteExpiredPersonData(
            db,
            clock,
            AsyncJob.DeleteExpiredPersonData(longGone.id, dryRun = true),
        )
        assertEquals(setOf("person", "child", "placement", "child_images"), remaining(longGone.id))
        assertEquals(0, asyncJobRunner.runPendingJobsSync(clock))
    }

    @Test
    fun `a dry run with a mock time evaluates the data as of that time`() {
        val mockNow = clock.now().plusYears(15)
        dataRetentionService.deleteExpiredPersonData(
            db,
            clock,
            AsyncJob.DeleteExpiredPersonData(recent.id, dryRun = true, mockNow = mockNow),
        )

        // The data of the recent child is not yet expired today, but would be 15 years from now
        val dryRunLog =
            logAppender.list
                .map { it.formattedMessage }
                .single { it.startsWith("Dry run of data retention for person ${recent.id}") }
        assertContains(dryRunLog, "as of ${mockNow.toLocalDate()}: would delete ")
        assertContains(dryRunLog, "person (1 rows)")
        assertEquals(setOf("person", "child", "placement"), remaining(recent.id))
        assertEquals(0, asyncJobRunner.runPendingJobsSync(clock))
        assertNull(lastAttemptedAt(recent.id))
    }

    @Test
    fun `a mock time is not allowed in a real run`() {
        assertFailsWith<IllegalArgumentException> {
            dataRetentionService.deleteExpiredPersonData(
                db,
                clock,
                AsyncJob.DeleteExpiredPersonData(
                    longGone.id,
                    dryRun = false,
                    mockNow = clock.now().plusYears(15),
                ),
            )
        }

        assertEquals(setOf("person", "child", "placement", "child_images"), remaining(longGone.id))
        assertNull(lastAttemptedAt(longGone.id))
    }

    @Test
    fun `the nightly runs are dry runs for the persons whose last attempt is the oldest`() {
        val attemptedLongAgo = DevPerson()
        db.transaction { tx ->
            tx.insert(attemptedLongAgo, DevPersonType.ADULT)
            tx.setDataRetentionLastAttemptedAt(attemptedLongAgo.id, clock.now().minusDays(20))
            tx.setDataRetentionLastAttemptedAt(recent.id, clock.now().minusDays(10))
        }

        planDataRetentionRuns(
            db,
            clock,
            asyncJobRunner,
            personCount = 2,
            maxDuration = Duration.ofHours(1),
        )
        assertEquals(2, asyncJobRunner.runPendingJobsSync(clock))

        assertEquals(clock.now(), lastAttemptedAt(longGone.id))
        assertEquals(clock.now(), lastAttemptedAt(attemptedLongAgo.id))
        assertEquals(clock.now().minusDays(10), lastAttemptedAt(recent.id))
        assertEquals(setOf("person", "child", "placement", "child_images"), remaining(longGone.id))
    }

    @Test
    fun `the runs that would start after the time allocation of the night are skipped`() {
        planDataRetentionRuns(
            db,
            clock,
            asyncJobRunner,
            personCount = 2,
            maxDuration = Duration.ofHours(1),
        )
        val afterTheAllocation = MockEvakaClock(clock.now().plusHours(1).plusSeconds(1))
        // The first skipped run removes the other
        assertEquals(1, asyncJobRunner.runPendingJobsSync(afterTheAllocation))

        assertNull(lastAttemptedAt(longGone.id))
        assertNull(lastAttemptedAt(recent.id))
    }

    @Test
    fun `a failed run still records the attempt`() {
        // Only one duplicate per person is supported
        db.transaction { tx ->
            repeat(2) { tx.insert(DevPerson(duplicateOf = recent.id), DevPersonType.CHILD) }
        }

        val error =
            assertFailsWith<IllegalStateException> {
                dataRetentionService.deleteExpiredPersonData(
                    db,
                    clock,
                    AsyncJob.DeleteExpiredPersonData(recent.id, dryRun = true),
                )
            }

        assertContains(error.message!!, "is linked to several duplicates or originals")
        assertEquals(clock.now(), lastAttemptedAt(recent.id))
    }

    private fun duplicateOf(id: PersonId): PersonId? = db.read { tx ->
        tx.createQuery { sql("SELECT duplicate_of FROM person WHERE id = ${bind(id)}") }
            .exactlyOne<PersonId?>()
    }

    private fun koskiFrozenAt(id: PersonId): HelsinkiDateTime? = db.read { tx ->
        tx.createQuery {
                sql("SELECT koski_data_first_removed_at FROM child WHERE id = ${bind(id)}")
            }
            .exactlyOne<HelsinkiDateTime?>()
    }

    private fun Database.Transaction.setCreated(id: PersonId, created: HelsinkiDateTime) {
        execute { sql("UPDATE person SET created = ${bind(created)} WHERE id = ${bind(id)}") }
    }

    @Test
    fun `a duplicate pair is skipped until neither has been placed for ten years, and then each is handled on its own`() {
        val duplicate = DevPerson(dateOfBirth = longGone.dateOfBirth, duplicateOf = longGone.id)
        val duplicatePlacementEnd = today.minus(DUPLICATE_LINK_PERIOD)
        db.transaction { tx ->
            tx.insert(duplicate, DevPersonType.CHILD)
            tx.setCreated(duplicate.id, HelsinkiDateTime.of(today.minusYears(13), LocalTime.NOON))
            tx.insert(
                DevPlacement(
                    childId = duplicate.id,
                    unitId = daycare.id,
                    startDate = duplicatePlacementEnd.minusYears(1),
                    endDate = duplicatePlacementEnd,
                )
            )
        }

        realRun(longGone.id)
        realRun(duplicate.id)
        assertEquals(0, asyncJobRunner.runPendingJobsSync(clock))
        assertEquals(longGone.id, duplicateOf(duplicate.id))
        assertEquals(setOf("person", "child", "placement", "child_images"), remaining(longGone.id))
        assertEquals(setOf("person", "child", "placement"), remaining(duplicate.id))

        db.transaction { tx ->
            tx.execute {
                sql(
                    "UPDATE placement SET end_date = end_date - 1 WHERE child_id = ${bind(duplicate.id)}"
                )
            }
        }
        realRun(longGone.id)
        assertEquals(emptySet(), remaining(longGone.id))
        // The run planned for the duplicate, and the image deletion
        assertEquals(2, asyncJobRunner.runPendingJobsSync(clock))
        assertEquals(emptySet(), remaining(duplicate.id))
    }

    @Test
    fun `only a real run clears the link of a duplicate pair, and it freezes the Koski sync of both`() {
        val duplicate = DevPerson(dateOfBirth = longGone.dateOfBirth, duplicateOf = longGone.id)
        db.transaction { tx ->
            tx.insert(duplicate, DevPersonType.CHILD)
            tx.execute {
                sql(
                    """
INSERT INTO koski_study_right (child_id, unit_id, type, payload, version, data_version)
VALUES
    (${bind(longGone.id)}, ${bind(daycare.id)}, 'PRESCHOOL', '{}', 0, 1),
    (${bind(duplicate.id)}, ${bind(daycare.id)}, 'PRESCHOOL', '{}', 0, 1)
"""
                )
            }
        }

        dataRetentionService.deleteExpiredPersonData(
            db,
            clock,
            AsyncJob.DeleteExpiredPersonData(duplicate.id, dryRun = true),
        )
        assertEquals(longGone.id, duplicateOf(duplicate.id))
        assertNull(koskiFrozenAt(longGone.id))
        assertNull(koskiFrozenAt(duplicate.id))

        realRun(duplicate.id)
        assertNull(duplicateOf(duplicate.id))
        assertEquals(clock.now(), koskiFrozenAt(longGone.id))
        assertEquals(clock.now(), koskiFrozenAt(duplicate.id))
    }

    @Test
    fun `a duplicate pair that was never placed is unlinked a month after the later of them was created, unless an application sent within a year is pending`() {
        val guardian = DevPerson()
        val original = DevPerson(dateOfBirth = today.minusYears(3))
        val duplicate = DevPerson(dateOfBirth = original.dateOfBirth, duplicateOf = original.id)
        val aMonthAgo = HelsinkiDateTime.of(today.minusMonths(1), LocalTime.NOON)
        val applicationId = db.transaction { tx ->
            tx.insert(guardian, DevPersonType.ADULT)
            tx.insert(original, DevPersonType.CHILD)
            tx.insert(duplicate, DevPersonType.CHILD)
            tx.setCreated(original.id, aMonthAgo.minusMonths(6))
            tx.setCreated(duplicate.id, aMonthAgo)
            tx.insertTestApplication(
                type = ApplicationType.CLUB,
                status = ApplicationStatus.CANCELLED,
                confidential = false,
                guardianId = guardian.id,
                childId = duplicate.id,
                document =
                    ClubFormV0(
                        child = ClubChild(dateOfBirth = null),
                        guardian = ClubAdult(),
                        apply = ClubApply(preferredUnits = listOf(daycare.id)),
                    ),
            )
        }

        fun Database.Transaction.setApplicationSent(date: LocalDate) {
            execute {
                sql(
                    "UPDATE application SET status = 'SENT', sentdate = ${bind(date)} WHERE id = ${bind(applicationId)}"
                )
            }
        }

        realRun(original.id)
        assertEquals(original.id, duplicateOf(duplicate.id))

        db.transaction { tx ->
            tx.setCreated(duplicate.id, aMonthAgo.minusDays(1))
            tx.setApplicationSent(today.minusYears(1))
        }
        realRun(original.id)
        assertEquals(original.id, duplicateOf(duplicate.id))

        db.transaction { tx -> tx.setApplicationSent(today.minusYears(1).minusDays(1)) }
        realRun(original.id)
        assertNull(duplicateOf(duplicate.id))
    }
}
