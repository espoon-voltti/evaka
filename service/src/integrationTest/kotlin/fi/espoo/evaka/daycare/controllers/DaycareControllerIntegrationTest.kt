// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare.controllers

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.daycare.getDaycare
import fi.espoo.evaka.daycare.getDaycareGroup
import fi.espoo.evaka.daycare.service.Caretakers
import fi.espoo.evaka.daycare.service.DaycareGroup
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.messaging.createDaycareGroupMessageAccount
import fi.espoo.evaka.messaging.insertMessageContent
import fi.espoo.evaka.placement.ChildBasics
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.placement.TerminatedPlacement
import fi.espoo.evaka.placement.UnitChildrenCapacityFactors
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.PlacementId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.insertDaycareAclRow
import fi.espoo.evaka.shared.dev.DevBackupCare
import fi.espoo.evaka.shared.dev.DevDaycareCaretaker
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.DevDaycareGroupPlacement
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.Conflict
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.shared.domain.RealEvakaClock
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testChild_2
import fi.espoo.evaka.testChild_3
import fi.espoo.evaka.testChild_4
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.user.EvakaUser
import fi.espoo.evaka.user.EvakaUserType
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired

class DaycareControllerIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var daycareController: DaycareController

    private val today = LocalDate.of(2021, 1, 12)
    private val now = HelsinkiDateTime.of(today, LocalTime.of(12, 0))
    private val supervisorId = EmployeeId(UUID.randomUUID())
    private val supervisor = AuthenticatedUser.Employee(supervisorId, emptySet())
    private val staffId = EmployeeId(UUID.randomUUID())
    private val staffMember = AuthenticatedUser.Employee(staffId, emptySet())

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insertGeneralTestFixtures()

            tx.insert(DevEmployee(id = supervisorId, firstName = "Elina", lastName = "Esimies"))
            tx.insert(DevEmployee(id = staffId))
            tx.insertDaycareAclRow(testDaycare.id, supervisorId, UserRole.UNIT_SUPERVISOR)
            tx.insertDaycareAclRow(testDaycare.id, staffId, UserRole.STAFF)
        }
    }

    @Test
    fun `get daycare`() {
        val (daycare, _, _) = getDaycare(testDaycare.id)

        db.read { assertEquals(it.getDaycare(testDaycare.id), daycare) }
    }

    @Test
    fun `create group`() {
        val name = "Ryh mä"
        val startDate = LocalDate.of(2019, 1, 1)
        val caretakers = 42.0
        val group = createDaycareGroup(testDaycare.id, name, startDate, caretakers)

        assertEquals(testDaycare.id, group.daycareId)
        assertEquals(name, group.name)
        assertEquals(startDate, group.startDate)
        assertNull(group.endDate)
        assertTrue(group.deletable)

        db.read { assertEquals(it.getDaycareGroup(group.id), group) }
        assertTrue(groupHasMessageAccount(group.id))
    }

    @Test
    fun `delete group`() {
        val group = createDaycareGroup(testDaycare.id, "Test", LocalDate.of(2019, 1, 1), 5.0)

        deleteDaycareGroup(testDaycare.id, group.id)

        db.read { assertNull(it.getDaycareGroup(group.id)) }
        assertFalse(groupHasMessageAccount(group.id))
    }

    @Test
    fun `cannot delete a group when it has a placement`() {
        val group = DevDaycareGroup(daycareId = testDaycare.id)
        val placement = DevPlacement(unitId = testDaycare.id, childId = testChild_1.id)
        db.transaction {
            it.insert(placement)
            it.insert(group)
            it.createDaycareGroupMessageAccount(group.id)
            it.insert(
                DevDaycareGroupPlacement(
                    daycarePlacementId = placement.id,
                    daycareGroupId = group.id
                )
            )
        }

        assertThrows<Conflict> { deleteDaycareGroup(testDaycare.id, group.id) }

        db.read { assertNotNull(it.getDaycareGroup(group.id)) }
    }

    @Test
    fun `cannot delete a group when it is a backup care`() {
        val group = DevDaycareGroup(daycareId = testDaycare.id)
        db.transaction {
            it.insert(group)
            it.createDaycareGroupMessageAccount(group.id)
            it.insert(DevPlacement(childId = testChild_1.id, unitId = testDaycare.id))
            it.insert(
                DevBackupCare(
                    childId = testChild_1.id,
                    unitId = testDaycare.id,
                    groupId = group.id,
                    period = FiniteDateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 2, 1))
                )
            )
        }

        assertThrows<Conflict> { deleteDaycareGroup(testDaycare.id, group.id) }

        db.read { assertNotNull(it.getDaycareGroup(group.id)) }
    }

    @Test
    fun `cannot delete a group when it has messages`() {
        val group = DevDaycareGroup(daycareId = testDaycare.id)
        db.transaction {
            it.insert(group)
            val accountId = it.createDaycareGroupMessageAccount(group.id)
            it.insertMessageContent("Juhannus tulee pian", accountId)
        }

        assertThrows<Conflict> { deleteDaycareGroup(testDaycare.id, group.id) }

        db.read { assertNotNull(it.getDaycareGroup(group.id)) }
    }

    @Test
    fun `get details of all groups`() {
        val group1 = DevDaycareGroup(id = GroupId(UUID.randomUUID()), daycareId = testDaycare.id)
        val group2 = DevDaycareGroup(id = GroupId(UUID.randomUUID()), daycareId = testDaycare.id)
        val placementId1 = PlacementId(UUID.randomUUID())
        val placementId2 = PlacementId(UUID.randomUUID())
        val placementId3 = PlacementId(UUID.randomUUID())
        db.transaction {
            it.insert(group1)
            it.insert(DevDaycareCaretaker(groupId = group1.id, amount = 3.0.toBigDecimal()))

            it.insert(group2)
            it.insert(DevDaycareCaretaker(groupId = group2.id, amount = 1.0.toBigDecimal()))

            // Missing group
            it.insert(
                DevPlacement(
                    id = placementId1,
                    childId = testChild_1.id,
                    unitId = testDaycare.id,
                    startDate = today.minusDays(10),
                    endDate = today.plusYears(1)
                )
            )

            // Ok
            it.insert(
                DevPlacement(
                    id = placementId2,
                    childId = testChild_2.id,
                    unitId = testDaycare.id,
                    startDate = today.minusDays(10),
                    endDate = today.plusYears(1)
                )
            )
            it.insert(
                DevDaycareGroupPlacement(
                    daycarePlacementId = placementId2,
                    daycareGroupId = group1.id,
                    startDate = today.minusDays(10),
                    endDate = today.plusYears(1)
                )
            )

            // Terminated
            it.insert(
                DevPlacement(
                    id = placementId3,
                    childId = testChild_3.id,
                    unitId = testDaycare.id,
                    startDate = today.minusDays(10),
                    endDate = today.plusDays(14),
                    terminationRequestedDate = today.minusDays(5),
                    terminatedBy = EvakaUserId(testAdult_1.id.raw)
                )
            )
            it.insert(
                DevDaycareGroupPlacement(
                    daycarePlacementId = placementId3,
                    daycareGroupId = group1.id,
                    startDate = today.minusDays(10),
                    endDate = today.plusDays(14)
                )
            )
        }

        val details = getGroupDetails(testDaycare.id)
        assertEquals(setOf(group1.id, group2.id), details.groups.map { it.id }.toSet())
        assertEquals(
            setOf(placementId1, placementId2, placementId3),
            details.placements.map { it.id }.toSet()
        )
        assertEquals(listOf(placementId1), details.missingGroupPlacements.map { it.placementId })
        assertEquals(listOf(placementId3), details.recentlyTerminatedPlacements.map { it.id })
        assertEquals(
            listOf(
                    UnitChildrenCapacityFactors(testChild_1.id, 1.0, 1.0),
                    UnitChildrenCapacityFactors(testChild_2.id, 1.0, 1.0),
                    UnitChildrenCapacityFactors(testChild_3.id, 1.0, 1.75),
                )
                .sortedBy { it.childId },
            details.unitChildrenCapacityFactors.sortedBy { it.childId }
        )
        assertEquals(
            mapOf(
                group1.id to Caretakers(3.0, 3.0),
                group2.id to Caretakers(1.0, 1.0),
            ),
            details.caretakers
        )
        assertEquals(2, details.groupOccupancies?.confirmed?.size)
        assertEquals(2, details.groupOccupancies?.realized?.size)
    }

    @Test
    fun `group details - terminated placements`() {
        val group = DevDaycareGroup(id = GroupId(UUID.randomUUID()), daycareId = testDaycare.id)
        val placementId1 = PlacementId(UUID.randomUUID())
        val placementId2 = PlacementId(UUID.randomUUID())
        val placementId3 = PlacementId(UUID.randomUUID())
        val placementId4 = PlacementId(UUID.randomUUID())
        val placementId5 = PlacementId(UUID.randomUUID())
        db.transaction {
            it.insert(group)

            // Terminated over 2 weeks ago -> not listed
            it.insert(
                DevPlacement(
                    id = placementId1,
                    childId = testChild_1.id,
                    unitId = testDaycare.id,
                    startDate = today.minusDays(10),
                    endDate = today.plusDays(7),
                    terminationRequestedDate = today.minusDays(15),
                    terminatedBy = EvakaUserId(testAdult_1.id.raw)
                )
            )

            // No group
            it.insert(
                DevPlacement(
                    id = placementId2,
                    childId = testChild_2.id,
                    unitId = testDaycare.id,
                    startDate = today.minusDays(10),
                    endDate = today.plusDays(7),
                    terminationRequestedDate = today.minusDays(14),
                    terminatedBy = EvakaUserId(testAdult_1.id.raw)
                )
            )

            // Has group
            it.insert(
                DevPlacement(
                    id = placementId3,
                    childId = testChild_3.id,
                    unitId = testDaycare.id,
                    startDate = today.minusDays(10),
                    endDate = today.plusDays(14),
                    terminationRequestedDate = today.minusDays(5),
                    terminatedBy = EvakaUserId(testAdult_1.id.raw)
                )
            )
            it.insert(
                DevDaycareGroupPlacement(
                    daycarePlacementId = placementId3,
                    daycareGroupId = group.id,
                    startDate = today.minusDays(10),
                    endDate = today.plusDays(14)
                )
            )

            // Connected daycare placement is terminated, preschool continues
            it.insert(
                DevPlacement(
                    id = placementId4,
                    type = PlacementType.PRESCHOOL_DAYCARE,
                    childId = testChild_4.id,
                    unitId = testDaycare.id,
                    startDate = today.minusDays(10),
                    endDate = today.plusDays(15),
                    terminationRequestedDate = today.minusDays(2),
                    terminatedBy = EvakaUserId(testAdult_1.id.raw)
                )
            )
            it.insert(
                DevDaycareGroupPlacement(
                    daycarePlacementId = placementId4,
                    daycareGroupId = group.id,
                    startDate = today.minusDays(10),
                    endDate = today.plusDays(15)
                )
            )
            it.insert(
                DevPlacement(
                    id = placementId5,
                    type = PlacementType.PRESCHOOL,
                    childId = testChild_4.id,
                    unitId = testDaycare.id,
                    startDate = today.plusDays(16),
                    endDate = today.plusYears(1)
                )
            )
        }

        val details = getGroupDetails(testDaycare.id)
        assertEquals(
            listOf(
                TerminatedPlacement(
                    id = placementId2,
                    endDate = today.plusDays(7),
                    type = PlacementType.DAYCARE,
                    terminationRequestedDate = today.minusDays(14),
                    child =
                        ChildBasics(
                            id = testChild_2.id,
                            socialSecurityNumber = testChild_2.ssn,
                            firstName = testChild_2.firstName,
                            lastName = testChild_2.lastName,
                            dateOfBirth = testChild_2.dateOfBirth
                        ),
                    terminatedBy =
                        EvakaUser(
                            id = EvakaUserId(testAdult_1.id.raw),
                            name = "${testAdult_1.lastName} ${testAdult_1.firstName}",
                            type = EvakaUserType.CITIZEN
                        ),
                    currentDaycareGroupName = null,
                    connectedDaycareOnly = false
                ),
                TerminatedPlacement(
                    id = placementId3,
                    endDate = today.plusDays(14),
                    type = PlacementType.DAYCARE,
                    terminationRequestedDate = today.minusDays(5),
                    child =
                        ChildBasics(
                            id = testChild_3.id,
                            socialSecurityNumber = testChild_3.ssn,
                            firstName = testChild_3.firstName,
                            lastName = testChild_3.lastName,
                            dateOfBirth = testChild_3.dateOfBirth
                        ),
                    terminatedBy =
                        EvakaUser(
                            id = EvakaUserId(testAdult_1.id.raw),
                            name = "${testAdult_1.lastName} ${testAdult_1.firstName}",
                            type = EvakaUserType.CITIZEN
                        ),
                    currentDaycareGroupName = group.name,
                    connectedDaycareOnly = false
                ),
                TerminatedPlacement(
                    id = placementId4,
                    endDate = today.plusDays(15),
                    type = PlacementType.PRESCHOOL_DAYCARE,
                    terminationRequestedDate = today.minusDays(2),
                    child =
                        ChildBasics(
                            id = testChild_4.id,
                            socialSecurityNumber = testChild_4.ssn,
                            firstName = testChild_4.firstName,
                            lastName = testChild_4.lastName,
                            dateOfBirth = testChild_4.dateOfBirth
                        ),
                    terminatedBy =
                        EvakaUser(
                            id = EvakaUserId(testAdult_1.id.raw),
                            name = "${testAdult_1.lastName} ${testAdult_1.firstName}",
                            type = EvakaUserType.CITIZEN
                        ),
                    currentDaycareGroupName = group.name,
                    connectedDaycareOnly = true
                )
            ),
            details.recentlyTerminatedPlacements.sortedBy { it.endDate }
        )
    }

    private fun getDaycare(daycareId: DaycareId): DaycareController.DaycareResponse {
        return daycareController.getDaycare(dbInstance(), staffMember, RealEvakaClock(), daycareId)
    }

    private fun createDaycareGroup(
        daycareId: DaycareId,
        name: String,
        startDate: LocalDate,
        initialCaretakers: Double
    ): DaycareGroup {
        return daycareController.createGroup(
            dbInstance(),
            supervisor,
            RealEvakaClock(),
            daycareId,
            DaycareController.CreateGroupRequest(name, startDate, initialCaretakers)
        )
    }

    private fun deleteDaycareGroup(
        daycareId: DaycareId,
        groupId: GroupId,
    ) {
        daycareController.deleteGroup(
            dbInstance(),
            supervisor,
            RealEvakaClock(),
            daycareId,
            groupId
        )
    }

    private fun getGroupDetails(daycareId: DaycareId): UnitGroupDetails {
        return daycareController.getUnitGroupDetails(
            dbInstance(),
            supervisor,
            MockEvakaClock(now),
            daycareId,
            from = today,
            to = today,
        )
    }

    private fun groupHasMessageAccount(groupId: GroupId): Boolean =
        db.read {
                it.createQuery {
                    sql(
                        """
SELECT EXISTS(
    SELECT * FROM message_account WHERE daycare_group_id = ${bind(groupId)} AND active = true
)
"""
                    )
                }
            }
            .exactlyOne()
}
