// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.varda

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.OphEnv
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.DevServiceNeed
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.dev.insertServiceNeedOption
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.snDaycareFullDay35
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class VardaUpdateServiceIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired lateinit var vardaUpdateService: VardaUpdateService
    @Autowired lateinit var ophEnv: OphEnv

    private val now = HelsinkiDateTime.of(LocalDate.of(2024, 1, 1), LocalTime.of(12, 0))
    private val clock = MockEvakaClock(now)

    @Test
    fun `new children are added to varda_state and update is planned`() {
        val area = DevCareArea()
        val unit = DevDaycare(areaId = area.id)
        val child1 = DevPerson(ssn = "030320A904N")
        val child2 = DevPerson(ssn = "030320A905P")

        db.transaction { tx ->
            tx.insert(area)
            tx.insert(unit)
            tx.insert(child1, DevPersonType.CHILD)
            tx.insert(child2, DevPersonType.CHILD)

            tx.insert(
                DevPlacement(
                    childId = child1.id,
                    unitId = unit.id,
                    startDate = LocalDate.of(2021, 1, 1),
                    endDate = LocalDate.of(2021, 2, 28),
                )
            )
            tx.insert(
                DevPlacement(
                    childId = child2.id,
                    unitId = unit.id,
                    startDate = LocalDate.of(2021, 1, 1),
                    endDate = LocalDate.of(2021, 2, 28),
                )
            )
        }

        vardaUpdateService.planChildrenUpdate(db, clock)

        assertEquals(setOf(child1.id, child2.id), getVardaStateChildIds())
        assertEquals(setOf(child1.id, child2.id), getPlannedChildIds())
    }

    @Test
    fun `update is not planned if state is up-to-date`() {
        val area = DevCareArea()
        val unit = DevDaycare(areaId = area.id, ophOrganizerOid = ophEnv.organizerOid)
        val employee = DevEmployee()
        val child = DevPerson(ssn = "030320A904N")

        db.transaction { tx ->
            tx.insertServiceNeedOption(snDaycareFullDay35)
            tx.insert(area)
            tx.insert(unit)
            tx.insert(employee)

            tx.insert(child, DevPersonType.CHILD)
            tx.insert(
                    DevPlacement(
                        childId = child.id,
                        unitId = unit.id,
                        startDate = LocalDate.of(2021, 1, 1),
                        endDate = LocalDate.of(2021, 2, 28),
                    )
                )
                .let { placementId ->
                    val period =
                        FiniteDateRange(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 2, 28))
                    tx.insert(
                        DevServiceNeed(
                            placementId = placementId,
                            startDate = period.start,
                            endDate = period.end,
                            optionId = snDaycareFullDay35.id,
                            confirmedBy = employee.evakaUserId,
                            confirmedAt = HelsinkiDateTime.now(),
                        )
                    )
                }

            // Compute the state that would be created by the updater
            val state =
                VardaUpdater(
                        DateRange(LocalDate.of(2019, 1, 1), null),
                        ophEnv.organizerOid,
                        "sourceSystem",
                    )
                    .getEvakaState(tx, LocalDate.of(2024, 1, 1), child.id)
            tx.execute {
                sql(
                    "INSERT INTO varda_state (child_id, state) VALUES (${bind(child.id)}, ${bindJson(state)})"
                )
            }
        }

        vardaUpdateService.planChildrenUpdate(db, clock)

        assertEquals(emptySet(), getPlannedChildIds())
    }

    private fun getVardaStateChildIds(): Set<ChildId> =
        db.read { tx -> tx.getVardaUpdateChildIds() }.toSet()

    private fun getPlannedChildIds(): Set<ChildId> =
        db.read { tx ->
            tx.createQuery { sql("SELECT (payload->>'childId')::uuid FROM async_job") }
                .mapTo<ChildId>()
                .toSet()
        }
}
