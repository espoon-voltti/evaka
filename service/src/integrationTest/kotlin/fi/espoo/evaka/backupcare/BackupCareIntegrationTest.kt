// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.backupcare

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.shared.BackupCareId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.PlacementId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.Conflict
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.RealEvakaClock
import fi.espoo.evaka.test.getBackupCareRowById
import fi.espoo.evaka.test.getBackupCareRowsByChild
import fi.espoo.evaka.testArea
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDaycare2
import fi.espoo.evaka.testDecisionMaker_1
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired

class BackupCareIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired lateinit var backupCareController: BackupCareController

    private val clock = RealEvakaClock()
    private val serviceWorker =
        AuthenticatedUser.Employee(testDecisionMaker_1.id, setOf(UserRole.SERVICE_WORKER))

    final val placementStart = LocalDate.of(2021, 1, 1)
    final val placementEnd = placementStart.plusDays(200)

    final val backupCareStart = placementStart.plusDays(1)
    final val backupCareEnd = placementStart.plusDays(10)

    lateinit var placementId: PlacementId

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insert(testDecisionMaker_1)
            tx.insert(testArea)
            tx.insert(testDaycare)
            tx.insert(testDaycare2)
            tx.insert(testChild_1, DevPersonType.CHILD)
            placementId =
                tx.insert(
                    DevPlacement(
                        childId = testChild_1.id,
                        unitId = testDaycare2.id,
                        startDate = placementStart,
                        endDate = placementEnd,
                    )
                )
        }
    }

    @Test
    fun testUpdate() {
        val groupId =
            db.transaction { tx -> tx.insert(DevDaycareGroup(daycareId = testDaycare.id)) }
        val period = FiniteDateRange(backupCareStart, backupCareEnd)
        val id = createBackupCareAndAssert(period = period)
        val changedPeriod = period.copy(end = backupCareEnd.plusDays(4))

        backupCareController.updateBackupCare(
            dbInstance(),
            serviceWorker,
            clock,
            id,
            BackupCareUpdateRequest(groupId = groupId, period = changedPeriod),
        )

        db.read { r ->
            r.getBackupCareRowsByChild(testChild_1.id).exactlyOne().also {
                assertEquals(id, it.id)
                assertEquals(testChild_1.id, it.childId)
                assertEquals(testDaycare.id, it.unitId)
                assertEquals(groupId, it.groupId)
                assertEquals(changedPeriod, it.period())
            }
        }
    }

    @Test
    fun testOverlapError() {
        createBackupCareAndAssert()
        assertThrows<Conflict> {
            backupCareController.createBackupCare(
                dbInstance(),
                serviceWorker,
                clock,
                testChild_1.id,
                NewBackupCare(
                    unitId = testDaycare.id,
                    groupId = null,
                    period =
                        FiniteDateRange(backupCareStart.plusDays(1), backupCareStart.plusDays(4)),
                ),
            )
        }
    }

    @Test
    fun testChildBackupCare() {
        val groupName = "Test Group"
        val groupId =
            db.transaction {
                it.insert(DevDaycareGroup(daycareId = testDaycare.id, name = groupName))
            }
        val id = createBackupCareAndAssert(groupId = groupId)
        val backupCares =
            backupCareController
                .getChildBackupCares(dbInstance(), serviceWorker, clock, testChild_1.id)
                .backupCares
                .map { it.backupCare }

        assertEquals(
            listOf(
                ChildBackupCare(
                    id = id,
                    unit = BackupCareUnit(id = testDaycare.id, name = testDaycare.name),
                    group = BackupCareGroup(id = groupId, name = groupName),
                    period = FiniteDateRange(backupCareStart, backupCareEnd),
                )
            ),
            backupCares,
        )
    }

    @Test
    fun `backup care must be created during a placement`() {
        assertThrows<BadRequest> {
            backupCareController.createBackupCare(
                dbInstance(),
                serviceWorker,
                clock,
                testChild_1.id,
                NewBackupCare(
                    unitId = testDaycare.id,
                    groupId = null,
                    period =
                        FiniteDateRange(placementStart.minusDays(10), placementStart.plusDays(2)),
                ),
            )
        }
        assertEquals(0, db.read { r -> r.getBackupCaresForChild(testChild_1.id).size })
    }

    @Test
    fun `backup care must be during a placement when modifying range`() {
        val groupId =
            db.transaction { tx -> tx.insert(DevDaycareGroup(daycareId = testDaycare.id)) }
        val id = createBackupCareAndAssert(groupId = groupId)

        val newPeriod = FiniteDateRange(placementStart.minusDays(10), placementStart.plusDays(2))

        assertThrows<BadRequest> {
            backupCareController.updateBackupCare(
                dbInstance(),
                serviceWorker,
                clock,
                id,
                BackupCareUpdateRequest(groupId = groupId, period = newPeriod),
            )
        }

        assertNotEquals(
            newPeriod,
            db.read { r -> r.getBackupCaresForChild(testChild_1.id)[0].period },
        )
    }

    @Test
    fun `backup care must be created for unit that is open the whole period`() {
        val area = db.transaction { tx -> tx.insert(DevCareArea(shortName = "test_area1")) }
        val daycareId =
            db.transaction { tx ->
                tx.insert(
                    DevDaycare(
                        openingDate = backupCareStart,
                        closingDate = backupCareEnd,
                        areaId = area,
                    )
                )
            }

        // Backup care starts before daycare opening
        assertThrows<BadRequest> {
            backupCareController.createBackupCare(
                dbInstance(),
                serviceWorker,
                clock,
                testChild_1.id,
                NewBackupCare(
                    unitId = daycareId,
                    groupId = null,
                    period = FiniteDateRange(backupCareStart.minusDays(1), backupCareEnd),
                ),
            )
        }

        // Backup care continues after daycare closing
        assertThrows<BadRequest> {
            backupCareController.createBackupCare(
                dbInstance(),
                serviceWorker,
                clock,
                testChild_1.id,
                NewBackupCare(
                    unitId = daycareId,
                    groupId = null,
                    period = FiniteDateRange(backupCareStart, backupCareEnd.plusDays(1)),
                ),
            )
        }

        // Backup care is completely outside daycare opening and closing dates
        assertThrows<BadRequest> {
            backupCareController.createBackupCare(
                dbInstance(),
                serviceWorker,
                clock,
                testChild_1.id,
                NewBackupCare(
                    unitId = daycareId,
                    groupId = null,
                    period =
                        FiniteDateRange(backupCareStart.minusDays(1), backupCareStart.minusDays(1)),
                ),
            )
        }
    }

    private fun createBackupCareAndAssert(
        childId: ChildId = testChild_1.id,
        unitId: DaycareId = testDaycare.id,
        groupId: GroupId? = null,
        period: FiniteDateRange = FiniteDateRange(backupCareStart, backupCareEnd),
    ): BackupCareId {
        val result =
            backupCareController.createBackupCare(
                dbInstance(),
                serviceWorker,
                clock,
                childId,
                NewBackupCare(unitId = unitId, groupId = groupId, period = period),
            )
        val id = result.id

        db.read { r ->
            r.getBackupCareRowById(id).also {
                assertEquals(id, it.id)
                assertEquals(childId, it.childId)
                assertEquals(unitId, it.unitId)
                assertEquals(groupId, it.groupId)
                assertEquals(period, it.period())
            }
        }
        return id
    }
}
