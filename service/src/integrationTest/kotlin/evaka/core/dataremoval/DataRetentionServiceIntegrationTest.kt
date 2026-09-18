// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.dataremoval

import evaka.core.FullApplicationTest
import evaka.core.childimages.insertChildImage
import evaka.core.shared.PersonId
import evaka.core.shared.async.AsyncJob
import evaka.core.shared.async.AsyncJobRunner
import evaka.core.shared.dev.DevCareArea
import evaka.core.shared.dev.DevDaycare
import evaka.core.shared.dev.DevPerson
import evaka.core.shared.dev.DevPersonType
import evaka.core.shared.dev.DevPlacement
import evaka.core.shared.dev.insert
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.shared.domain.MockEvakaClock
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
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

    @Test
    fun `the job deletes the expired data of the person, queues the file deletions, and leaves a recent child alone`() {
        db.transaction { tx ->
            asyncJobRunner.plan(
                tx,
                listOf(
                    AsyncJob.DeleteExpiredPersonData(longGone.id, dryRun = false),
                    AsyncJob.DeleteExpiredPersonData(recent.id, dryRun = false),
                ),
                runAt = clock.now(),
            )
        }
        // The two retention jobs, and the image deletion that the first of them queued
        assertEquals(3, asyncJobRunner.runPendingJobsSync(clock))

        assertEquals(emptySet(), remaining(longGone.id))
        assertEquals(setOf("person", "child", "placement"), remaining(recent.id))
    }

    @Test
    fun `a person who no longer exists is skipped`() {
        dataRetentionService.deleteExpiredPersonData(
            db,
            clock,
            AsyncJob.DeleteExpiredPersonData(PersonId(UUID.randomUUID()), dryRun = false),
        )
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
}
