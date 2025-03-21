// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare.controllers

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.application.ApplicationStatus
import fi.espoo.evaka.application.ApplicationType
import fi.espoo.evaka.application.persistence.daycare.DaycareFormV0
import fi.espoo.evaka.daycare.Daycare
import fi.espoo.evaka.daycare.DaycareFields
import fi.espoo.evaka.daycare.getDaycare
import fi.espoo.evaka.daycare.getDaycareGroup
import fi.espoo.evaka.daycare.service.Caretakers
import fi.espoo.evaka.daycare.service.DaycareGroup
import fi.espoo.evaka.decision.DecisionType
import fi.espoo.evaka.insertServiceNeedOptions
import fi.espoo.evaka.messaging.createDaycareGroupMessageAccount
import fi.espoo.evaka.messaging.insertMessageContent
import fi.espoo.evaka.placement.ChildBasics
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.placement.TerminatedPlacement
import fi.espoo.evaka.placement.UnitChildrenCapacityFactors
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.PlacementId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.dev.DevBackupCare
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevDaycareCaretaker
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.DevDaycareGroupPlacement
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.TestDecision
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.dev.insertTestApplication
import fi.espoo.evaka.shared.dev.insertTestDecision
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.Conflict
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.shared.domain.RealEvakaClock
import fi.espoo.evaka.test.validDaycareApplication
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testArea
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testChild_2
import fi.espoo.evaka.testChild_3
import fi.espoo.evaka.testChild_4
import fi.espoo.evaka.testChild_5
import fi.espoo.evaka.testChild_6
import fi.espoo.evaka.testChild_7
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDaycare2
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

    private val supervisor = DevEmployee(firstName = "Elina", lastName = "Esimies")
    private val staffMember = DevEmployee()

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insert(testArea)
            tx.insert(testDaycare)
            tx.insert(testAdult_1, DevPersonType.ADULT)
            listOf(testChild_1, testChild_2, testChild_3, testChild_4).forEach {
                tx.insert(it, DevPersonType.CHILD)
            }
            tx.insertServiceNeedOptions()

            tx.insert(supervisor, mapOf(testDaycare.id to UserRole.UNIT_SUPERVISOR))
            tx.insert(staffMember, mapOf(testDaycare.id to UserRole.STAFF))
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
                    daycareGroupId = group.id,
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
                    period = FiniteDateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 2, 1)),
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
                    endDate = today.plusYears(1),
                )
            )

            // Ok
            it.insert(
                DevPlacement(
                    id = placementId2,
                    childId = testChild_2.id,
                    unitId = testDaycare.id,
                    startDate = today.minusDays(10),
                    endDate = today.plusYears(1),
                )
            )
            it.insert(
                DevDaycareGroupPlacement(
                    daycarePlacementId = placementId2,
                    daycareGroupId = group1.id,
                    startDate = today.minusDays(10),
                    endDate = today.plusYears(1),
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
                    terminatedBy = EvakaUserId(testAdult_1.id.raw),
                )
            )
            it.insert(
                DevDaycareGroupPlacement(
                    daycarePlacementId = placementId3,
                    daycareGroupId = group1.id,
                    startDate = today.minusDays(10),
                    endDate = today.plusDays(14),
                )
            )
        }

        val details = getGroupDetails(testDaycare.id)
        assertEquals(setOf(group1.id, group2.id), details.groups.map { it.id }.toSet())
        assertEquals(
            setOf(placementId1, placementId2, placementId3),
            details.placements.map { it.id }.toSet(),
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
            details.unitChildrenCapacityFactors.sortedBy { it.childId },
        )
        assertEquals(
            mapOf(group1.id to Caretakers(3.0, 3.0), group2.id to Caretakers(1.0, 1.0)),
            details.caretakers,
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
        val placementId6 = PlacementId(UUID.randomUUID())
        val placementId7 = PlacementId(UUID.randomUUID())
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
                    terminatedBy = EvakaUserId(testAdult_1.id.raw),
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
                    terminatedBy = EvakaUserId(testAdult_1.id.raw),
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
                    terminatedBy = EvakaUserId(testAdult_1.id.raw),
                )
            )
            it.insert(
                DevDaycareGroupPlacement(
                    daycarePlacementId = placementId3,
                    daycareGroupId = group.id,
                    startDate = today.minusDays(10),
                    endDate = today.plusDays(14),
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
                    terminatedBy = EvakaUserId(testAdult_1.id.raw),
                )
            )
            it.insert(
                DevDaycareGroupPlacement(
                    daycarePlacementId = placementId4,
                    daycareGroupId = group.id,
                    startDate = today.minusDays(10),
                    endDate = today.plusDays(15),
                )
            )
            it.insert(
                DevPlacement(
                    id = placementId5,
                    type = PlacementType.PRESCHOOL,
                    childId = testChild_4.id,
                    unitId = testDaycare.id,
                    startDate = today.plusDays(16),
                    endDate = today.plusYears(1),
                )
            )

            // Active transfer application
            it.insert(testDaycare2)
            it.insert(testChild_5, DevPersonType.CHILD)
            it.insert(
                DevPlacement(
                    id = placementId6,
                    childId = testChild_5.id,
                    unitId = testDaycare.id,
                    startDate = today.minusDays(10),
                    endDate = today.plusDays(20),
                )
            )
            it.insert(
                DevDaycareGroupPlacement(
                    daycarePlacementId = placementId6,
                    daycareGroupId = group.id,
                    startDate = today.minusDays(10),
                    endDate = today.plusDays(20),
                )
            )
            it.insert(
                DevPlacement(
                    childId = testChild_5.id,
                    unitId = testDaycare2.id,
                    startDate = today.plusDays(21),
                    endDate = today.plusYears(1),
                )
            )
            it.insertTestApplication(
                    type = ApplicationType.DAYCARE,
                    guardianId = testAdult_1.id,
                    childId = testChild_5.id,
                    document = DaycareFormV0.fromApplication2(validDaycareApplication),
                    status = ApplicationStatus.ACTIVE,
                    confidential = true,
                    transferApplication = true,
                )
                .also { applicationId ->
                    it.insertTestDecision(
                        TestDecision(
                            startDate = today.plusDays(21),
                            endDate = today.plusYears(1),
                            unitId = testDaycare2.id,
                            createdBy = supervisor.evakaUserId,
                            applicationId = applicationId,
                            type = DecisionType.DAYCARE,
                        )
                    )
                }

            // Active transfer application without group
            it.insert(testChild_6, DevPersonType.CHILD)
            it.insert(
                DevPlacement(
                    id = placementId7,
                    childId = testChild_6.id,
                    unitId = testDaycare.id,
                    startDate = today.minusDays(10),
                    endDate = today.plusDays(21),
                )
            )
            it.insert(
                DevPlacement(
                    childId = testChild_6.id,
                    unitId = testDaycare2.id,
                    startDate = today.plusDays(22),
                    endDate = today.plusYears(1),
                )
            )
            it.insertTestApplication(
                    type = ApplicationType.DAYCARE,
                    guardianId = testAdult_1.id,
                    childId = testChild_6.id,
                    document = DaycareFormV0.fromApplication2(validDaycareApplication),
                    status = ApplicationStatus.ACTIVE,
                    confidential = true,
                    transferApplication = true,
                )
                .also { applicationId ->
                    it.insertTestDecision(
                        TestDecision(
                            startDate = today.plusDays(22),
                            endDate = today.plusYears(1),
                            unitId = testDaycare2.id,
                            createdBy = supervisor.evakaUserId,
                            applicationId = applicationId,
                            type = DecisionType.DAYCARE,
                        )
                    )
                }

            // Old active transfer application, child has been transferred already -> not listed
            it.insert(testChild_7, DevPersonType.CHILD)
            it.insert(
                DevPlacement(
                    childId = testChild_7.id,
                    unitId = testDaycare2.id,
                    startDate = today.minusYears(1),
                    endDate = today.minusDays(11),
                )
            )
            it.insert(
                DevPlacement(
                    childId = testChild_7.id,
                    unitId = testDaycare.id,
                    startDate = today.minusDays(10),
                    endDate = today.plusDays(21),
                )
            )
            it.insertTestApplication(
                    type = ApplicationType.DAYCARE,
                    guardianId = testAdult_1.id,
                    childId = testChild_7.id,
                    document = DaycareFormV0.fromApplication2(validDaycareApplication),
                    status = ApplicationStatus.ACTIVE,
                    confidential = true,
                    transferApplication = true,
                    sentDate = today.minusMonths(2),
                )
                .also { applicationId ->
                    it.insertTestDecision(
                        TestDecision(
                            startDate = today.minusDays(10),
                            endDate = today.plusDays(21),
                            unitId = testDaycare.id,
                            createdBy = supervisor.evakaUserId,
                            applicationId = applicationId,
                            type = DecisionType.DAYCARE,
                        )
                    )
                }
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
                            dateOfBirth = testChild_2.dateOfBirth,
                        ),
                    terminatedBy =
                        EvakaUser(
                            id = EvakaUserId(testAdult_1.id.raw),
                            name = "${testAdult_1.lastName} ${testAdult_1.firstName}",
                            type = EvakaUserType.CITIZEN,
                        ),
                    currentDaycareGroupName = null,
                    connectedDaycareOnly = false,
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
                            dateOfBirth = testChild_3.dateOfBirth,
                        ),
                    terminatedBy =
                        EvakaUser(
                            id = EvakaUserId(testAdult_1.id.raw),
                            name = "${testAdult_1.lastName} ${testAdult_1.firstName}",
                            type = EvakaUserType.CITIZEN,
                        ),
                    currentDaycareGroupName = group.name,
                    connectedDaycareOnly = false,
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
                            dateOfBirth = testChild_4.dateOfBirth,
                        ),
                    terminatedBy =
                        EvakaUser(
                            id = EvakaUserId(testAdult_1.id.raw),
                            name = "${testAdult_1.lastName} ${testAdult_1.firstName}",
                            type = EvakaUserType.CITIZEN,
                        ),
                    currentDaycareGroupName = group.name,
                    connectedDaycareOnly = true,
                ),
                TerminatedPlacement(
                    id = placementId6,
                    endDate = today.plusDays(20),
                    type = PlacementType.DAYCARE,
                    terminationRequestedDate = null,
                    child =
                        ChildBasics(
                            id = testChild_5.id,
                            socialSecurityNumber = testChild_5.ssn,
                            firstName = testChild_5.firstName,
                            lastName = testChild_5.lastName,
                            dateOfBirth = testChild_5.dateOfBirth,
                        ),
                    terminatedBy =
                        EvakaUser(
                            id = EvakaUserId(testAdult_1.id.raw),
                            name = "${testAdult_1.lastName} ${testAdult_1.firstName}",
                            type = EvakaUserType.CITIZEN,
                        ),
                    currentDaycareGroupName = group.name,
                    connectedDaycareOnly = false,
                ),
                TerminatedPlacement(
                    id = placementId7,
                    endDate = today.plusDays(21),
                    type = PlacementType.DAYCARE,
                    terminationRequestedDate = null,
                    child =
                        ChildBasics(
                            id = testChild_6.id,
                            socialSecurityNumber = testChild_6.ssn,
                            firstName = testChild_6.firstName,
                            lastName = testChild_6.lastName,
                            dateOfBirth = testChild_6.dateOfBirth,
                        ),
                    terminatedBy =
                        EvakaUser(
                            id = EvakaUserId(testAdult_1.id.raw),
                            name = "${testAdult_1.lastName} ${testAdult_1.firstName}",
                            type = EvakaUserType.CITIZEN,
                        ),
                    currentDaycareGroupName = null,
                    connectedDaycareOnly = false,
                ),
            ),
            details.recentlyTerminatedPlacements.sortedBy { it.endDate },
        )
    }

    @Test
    fun `cannot set daycare close date to earlier than open date`() {
        val admin = DevEmployee(roles = setOf(UserRole.ADMIN))
        val openingDate = today
        db.transaction { tx -> tx.insert(admin) }

        val daycare = getDaycare(testDaycare.id).daycare
        val fields = DaycareFields.fromDaycare(daycare).copy(openingDate = openingDate)

        assertThrows<BadRequest> {
            updateDaycare(admin.user, fields.copy(closingDate = openingDate.minusDays(1)))
        }

        updateDaycare(admin.user, fields.copy(closingDate = openingDate))

        assertThrows<BadRequest> { updateUnitClosingDate(admin.user, openingDate.minusDays(1)) }

        updateUnitClosingDate(admin.user, openingDate)
    }

    @Test
    fun `cannot set daycare close date to earlier than the end of last placement`() {
        val admin = DevEmployee(roles = setOf(UserRole.ADMIN))
        val endDate = today.plusYears(1)
        val placement =
            DevPlacement(
                childId = testChild_1.id,
                unitId = testDaycare.id,
                startDate = today,
                endDate = endDate,
            )
        db.transaction { tx ->
            tx.insert(admin)
            tx.insert(placement)
        }

        val daycare = getDaycare(testDaycare.id).daycare
        val fields = DaycareFields.fromDaycare(daycare)

        assertThrows<BadRequest> {
            updateDaycare(admin.user, fields.copy(closingDate = endDate.minusDays(1)))
        }

        updateDaycare(admin.user, fields.copy(closingDate = endDate))

        assertThrows<BadRequest> { updateUnitClosingDate(admin.user, endDate.minusDays(1)) }

        updateUnitClosingDate(admin.user, endDate)
    }

    @Test
    fun `cannot set daycare close date to earlier than the end of last backup care`() {
        val admin = DevEmployee(roles = setOf(UserRole.ADMIN))
        val endDate = today.plusYears(1)
        val backupCare =
            DevBackupCare(
                childId = testChild_1.id,
                unitId = testDaycare.id,
                period = FiniteDateRange(today, endDate),
            )
        db.transaction { tx ->
            tx.insert(admin)
            tx.insert(backupCare)
        }

        val daycare = getDaycare(testDaycare.id).daycare
        val fields = DaycareFields.fromDaycare(daycare)

        assertThrows<BadRequest> {
            updateDaycare(admin.user, fields.copy(closingDate = endDate.minusDays(1)))
        }

        updateDaycare(admin.user, fields.copy(closingDate = endDate))
    }

    @Test
    fun `daycare can be closed if there are no placements or backup cares`() {
        val admin = DevEmployee(roles = setOf(UserRole.ADMIN))
        db.transaction { tx -> tx.insert(admin) }

        val daycare = getDaycare(testDaycare.id).daycare
        val fields = DaycareFields.fromDaycare(daycare)

        updateDaycare(admin.user, fields.copy(closingDate = today))
    }

    @Test
    fun `can add and modify group aromi customer id`() {
        val admin = DevEmployee(roles = setOf(UserRole.ADMIN))
        val testArea = DevCareArea(name = "Testarea 111", shortName = "ta111")
        val testDaycare = DevDaycare(name = "Testunit 1110", areaId = testArea.id)
        db.transaction { tx ->
            tx.insert(admin)
            tx.insert(testArea)
            tx.insert(testDaycare)
        }
        val aromiDaycareCustomerId = "TU1110_PK"
        val aromiPreschoolCustomerId = "TU1110_EO"
        val group =
            createDaycareGroup(
                daycareId = testDaycare.id,
                name = "Aromi test group",
                aromiCustomerId = "TU1110_PK",
                initialCaretakers = 7.0,
                startDate = today,
                user = admin.user,
            )

        assertEquals(aromiDaycareCustomerId, group.aromiCustomerId)

        val editedGroup =
            updateAndGetDaycareGroup(
                groupId = group.id,
                daycareId = testDaycare.id,
                name = group.name,
                startDate = group.startDate,
                endDate = group.endDate,
                jamixCustomerNumber = group.jamixCustomerNumber,
                aromiCustomerId = null,
                user = admin.user,
                nekkuCustomerNumber = null,
            )

        assertNull(editedGroup.aromiCustomerId)

        val editedGroup2 =
            updateAndGetDaycareGroup(
                groupId = group.id,
                daycareId = testDaycare.id,
                name = group.name,
                startDate = group.startDate,
                endDate = group.endDate,
                jamixCustomerNumber = group.jamixCustomerNumber,
                aromiCustomerId = aromiPreschoolCustomerId,
                user = admin.user,
                nekkuCustomerNumber = null,
            )

        assertEquals(aromiPreschoolCustomerId, editedGroup2.aromiCustomerId)
    }

    private fun getDaycare(daycareId: DaycareId): DaycareController.DaycareResponse {
        return daycareController.getDaycare(
            dbInstance(),
            staffMember.user,
            RealEvakaClock(),
            daycareId,
        )
    }

    private fun updateDaycare(user: AuthenticatedUser.Employee, fields: DaycareFields) {
        daycareController.updateDaycare(
            dbInstance(),
            user,
            MockEvakaClock(now),
            testDaycare.id,
            fields,
        )
    }

    private fun updateUnitClosingDate(user: AuthenticatedUser.Employee, closingDate: LocalDate) {
        daycareController.updateUnitClosingDate(
            dbInstance(),
            user,
            MockEvakaClock(now),
            testDaycare.id,
            closingDate,
        )
    }

    private fun createDaycareGroup(
        daycareId: DaycareId,
        name: String,
        startDate: LocalDate,
        initialCaretakers: Double,
        user: AuthenticatedUser.Employee = supervisor.user,
        aromiCustomerId: String? = null,
    ): DaycareGroup {
        return daycareController.createGroup(
            dbInstance(),
            user,
            RealEvakaClock(),
            daycareId,
            DaycareController.CreateGroupRequest(
                name,
                startDate,
                initialCaretakers,
                aromiCustomerId,
            ),
        )
    }

    private fun updateAndGetDaycareGroup(
        daycareId: DaycareId,
        groupId: GroupId,
        name: String,
        startDate: LocalDate,
        user: AuthenticatedUser.Employee,
        endDate: LocalDate? = null,
        aromiCustomerId: String? = null,
        jamixCustomerNumber: Int? = null,
        nekkuCustomerNumber: String? = null,
    ): DaycareGroup {
        daycareController.updateGroup(
            dbInstance(),
            user,
            RealEvakaClock(),
            daycareId,
            groupId,
            DaycareController.GroupUpdateRequest(
                name = name,
                startDate = startDate,
                aromiCustomerId = aromiCustomerId,
                jamixCustomerNumber = jamixCustomerNumber,
                endDate = endDate,
                nekkuCustomerNumber = nekkuCustomerNumber,
            ),
        )
        return daycareController.getGroups(dbInstance(), user, RealEvakaClock(), daycareId).first {
            g ->
            g.id == groupId
        }
    }

    private fun deleteDaycareGroup(daycareId: DaycareId, groupId: GroupId) {
        daycareController.deleteGroup(
            dbInstance(),
            supervisor.user,
            RealEvakaClock(),
            daycareId,
            groupId,
        )
    }

    private fun getGroupDetails(daycareId: DaycareId): UnitGroupDetails {
        return daycareController.getUnitGroupDetails(
            dbInstance(),
            supervisor.user,
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

private fun DaycareFields.Companion.fromDaycare(daycare: Daycare): DaycareFields =
    DaycareFields(
        name = daycare.name,
        openingDate = daycare.openingDate,
        closingDate = null,
        areaId = daycare.area.id,
        type = daycare.type,
        dailyPreschoolTime = daycare.dailyPreschoolTime,
        dailyPreparatoryTime = daycare.dailyPreparatoryTime,
        daycareApplyPeriod = daycare.daycareApplyPeriod,
        preschoolApplyPeriod = daycare.preschoolApplyPeriod,
        clubApplyPeriod = daycare.clubApplyPeriod,
        providerType = daycare.providerType,
        capacity = daycare.capacity,
        language = daycare.language,
        withSchool = daycare.withSchool,
        ghostUnit = daycare.ghostUnit,
        uploadToVarda = daycare.uploadToVarda,
        uploadChildrenToVarda = daycare.uploadChildrenToVarda,
        uploadToKoski = daycare.uploadToKoski,
        invoicedByMunicipality = daycare.invoicedByMunicipality,
        costCenter = daycare.costCenter,
        dwCostCenter = daycare.dwCostCenter,
        financeDecisionHandlerId = daycare.financeDecisionHandler?.id,
        additionalInfo = daycare.additionalInfo,
        phone = daycare.phone,
        email = daycare.email,
        url = daycare.url,
        visitingAddress = daycare.visitingAddress,
        location = daycare.location,
        mailingAddress = daycare.mailingAddress,
        unitManager = daycare.unitManager,
        preschoolManager = daycare.preschoolManager,
        decisionCustomization = daycare.decisionCustomization,
        ophUnitOid = daycare.ophUnitOid,
        ophOrganizerOid = daycare.ophOrganizerOid,
        operationTimes = daycare.operationTimes,
        shiftCareOperationTimes = daycare.shiftCareOperationTimes,
        shiftCareOpenOnHolidays = daycare.shiftCareOpenOnHolidays,
        businessId = daycare.businessId,
        iban = daycare.iban,
        providerId = daycare.providerId,
        partnerCode = daycare.partnerCode,
        mealtimes = daycare.mealTimes,
    )
