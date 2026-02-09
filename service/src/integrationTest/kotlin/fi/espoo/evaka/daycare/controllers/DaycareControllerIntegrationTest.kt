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
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.dev.DevBackupCare
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevDaycareCaretaker
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.DevDaycareGroupPlacement
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevPerson
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
import fi.espoo.evaka.shared.security.PilotFeature
import fi.espoo.evaka.test.getValidDaycareApplication
import fi.espoo.evaka.user.EvakaUser
import fi.espoo.evaka.user.EvakaUserType
import java.time.LocalDate
import java.time.LocalTime
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

    private val area = DevCareArea()
    private val daycare =
        DevDaycare(
            areaId = area.id,
            enabledPilotFeatures = setOf(PilotFeature.PLACEMENT_TERMINATION),
        )
    private val supervisor = DevEmployee(firstName = "Elina", lastName = "Esimies")
    private val staffMember = DevEmployee()
    private val adult = DevPerson()
    private val child1 = DevPerson(dateOfBirth = LocalDate.of(2017, 6, 1))
    private val child2 = DevPerson(dateOfBirth = LocalDate.of(2016, 3, 1))
    // Under 3 on test date for 1.75 capacity factor
    private val child3 = DevPerson(dateOfBirth = LocalDate.of(2018, 9, 1))
    private val child4 = DevPerson(dateOfBirth = LocalDate.of(2019, 3, 2))

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(adult, DevPersonType.ADULT)
            listOf(child1, child2, child3, child4).forEach { tx.insert(it, DevPersonType.CHILD) }
            tx.insertServiceNeedOptions()

            tx.insert(supervisor, mapOf(daycare.id to UserRole.UNIT_SUPERVISOR))
            tx.insert(staffMember, mapOf(daycare.id to UserRole.STAFF))
        }
    }

    @Test
    fun `get daycare`() {
        val response = getDaycare(daycare.id)

        db.read { assertEquals(it.getDaycare(daycare.id), response.daycare) }
    }

    @Test
    fun `create group`() {
        val name = "Ryh mä"
        val startDate = LocalDate.of(2019, 1, 1)
        val caretakers = 42.0
        val group = createDaycareGroup(daycare.id, name, startDate, caretakers)

        assertEquals(daycare.id, group.daycareId)
        assertEquals(name, group.name)
        assertEquals(startDate, group.startDate)
        assertNull(group.endDate)
        assertTrue(group.deletable)

        db.read { assertEquals(it.getDaycareGroup(group.id), group) }
        assertTrue(groupHasMessageAccount(group.id))
    }

    @Test
    fun `delete group`() {
        val group = createDaycareGroup(daycare.id, "Test", LocalDate.of(2019, 1, 1), 5.0)

        deleteDaycareGroup(daycare.id, group.id)

        db.read { assertNull(it.getDaycareGroup(group.id)) }
        assertFalse(groupHasMessageAccount(group.id))
    }

    @Test
    fun `cannot delete a group when it has a placement`() {
        val group = DevDaycareGroup(daycareId = daycare.id)
        val placement = DevPlacement(unitId = daycare.id, childId = child1.id)
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

        assertThrows<Conflict> { deleteDaycareGroup(daycare.id, group.id) }

        db.read { assertNotNull(it.getDaycareGroup(group.id)) }
    }

    @Test
    fun `cannot delete a group when it is a backup care`() {
        val group = DevDaycareGroup(daycareId = daycare.id)
        db.transaction {
            it.insert(group)
            it.createDaycareGroupMessageAccount(group.id)
            it.insert(DevPlacement(childId = child1.id, unitId = daycare.id))
            it.insert(
                DevBackupCare(
                    childId = child1.id,
                    unitId = daycare.id,
                    groupId = group.id,
                    period = FiniteDateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 2, 1)),
                )
            )
        }

        assertThrows<Conflict> { deleteDaycareGroup(daycare.id, group.id) }

        db.read { assertNotNull(it.getDaycareGroup(group.id)) }
    }

    @Test
    fun `cannot delete a group when it has messages`() {
        val group = DevDaycareGroup(daycareId = daycare.id)
        db.transaction {
            it.insert(group)
            val accountId = it.createDaycareGroupMessageAccount(group.id)
            it.insertMessageContent("Juhannus tulee pian", accountId)
        }

        assertThrows<Conflict> { deleteDaycareGroup(daycare.id, group.id) }

        db.read { assertNotNull(it.getDaycareGroup(group.id)) }
    }

    @Test
    fun `get details of all groups`() {
        val group1 = DevDaycareGroup(daycareId = daycare.id, name = "Group 1")
        val group2 = DevDaycareGroup(daycareId = daycare.id, name = "Group 2")
        val placement1 =
            DevPlacement(
                childId = child1.id,
                unitId = daycare.id,
                startDate = today.minusDays(10),
                endDate = today.plusYears(1),
            )
        val placement2 =
            DevPlacement(
                childId = child2.id,
                unitId = daycare.id,
                startDate = today.minusDays(10),
                endDate = today.plusYears(1),
            )
        val placement3 =
            DevPlacement(
                childId = child3.id,
                unitId = daycare.id,
                startDate = today.minusDays(10),
                endDate = today.plusDays(14),
                terminationRequestedDate = today.minusDays(5),
                terminatedBy = adult.evakaUserId(),
            )
        db.transaction {
            it.insert(group1)
            it.insert(DevDaycareCaretaker(groupId = group1.id, amount = 3.0.toBigDecimal()))

            it.insert(group2)
            it.insert(DevDaycareCaretaker(groupId = group2.id, amount = 1.0.toBigDecimal()))

            // Missing group
            it.insert(placement1)

            // Ok
            it.insert(placement2)
            it.insert(
                DevDaycareGroupPlacement(
                    daycarePlacementId = placement2.id,
                    daycareGroupId = group1.id,
                    startDate = today.minusDays(10),
                    endDate = today.plusYears(1),
                )
            )

            // Terminated
            it.insert(placement3)
            it.insert(
                DevDaycareGroupPlacement(
                    daycarePlacementId = placement3.id,
                    daycareGroupId = group1.id,
                    startDate = today.minusDays(10),
                    endDate = today.plusDays(14),
                )
            )
        }

        val details = getGroupDetails(daycare.id)
        assertEquals(setOf(group1.id, group2.id), details.groups.map { it.id }.toSet())
        assertEquals(
            setOf(placement1.id, placement2.id, placement3.id),
            details.placements.map { it.id }.toSet(),
        )
        assertEquals(listOf(placement1.id), details.missingGroupPlacements.map { it.placementId })
        assertEquals(listOf(placement3.id), details.recentlyTerminatedPlacements.map { it.id })
        assertEquals(
            listOf(
                    UnitChildrenCapacityFactors(child1.id, 1.0, 1.0),
                    UnitChildrenCapacityFactors(child2.id, 1.0, 1.0),
                    UnitChildrenCapacityFactors(child3.id, 1.0, 1.75),
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
        val daycare2 = DevDaycare(name = "Test Daycare 2", areaId = area.id)
        val child5 = DevPerson()
        val child6 = DevPerson()
        val child7 = DevPerson()
        val group = DevDaycareGroup(daycareId = daycare.id)
        val placement1 =
            DevPlacement(
                childId = child1.id,
                unitId = daycare.id,
                startDate = today.minusDays(10),
                endDate = today.plusDays(7),
                terminationRequestedDate = today.minusDays(15),
                terminatedBy = adult.evakaUserId(),
            )
        val placement2 =
            DevPlacement(
                childId = child2.id,
                unitId = daycare.id,
                startDate = today.minusDays(10),
                endDate = today.plusDays(7),
                terminationRequestedDate = today.minusDays(14),
                terminatedBy = adult.evakaUserId(),
            )
        val placement3 =
            DevPlacement(
                childId = child3.id,
                unitId = daycare.id,
                startDate = today.minusDays(10),
                endDate = today.plusDays(14),
                terminationRequestedDate = today.minusDays(5),
                terminatedBy = adult.evakaUserId(),
            )
        val placement4 =
            DevPlacement(
                type = PlacementType.PRESCHOOL_DAYCARE,
                childId = child4.id,
                unitId = daycare.id,
                startDate = today.minusDays(10),
                endDate = today.plusDays(15),
                terminationRequestedDate = today.minusDays(2),
                terminatedBy = adult.evakaUserId(),
            )
        val placement6 =
            DevPlacement(
                childId = child5.id,
                unitId = daycare.id,
                startDate = today.minusDays(10),
                endDate = today.plusDays(20),
            )
        val placement7 =
            DevPlacement(
                childId = child6.id,
                unitId = daycare.id,
                startDate = today.minusDays(10),
                endDate = today.plusDays(21),
            )
        db.transaction {
            it.insert(daycare2)
            it.insert(child5, DevPersonType.CHILD)
            it.insert(child6, DevPersonType.CHILD)
            it.insert(child7, DevPersonType.CHILD)
            it.insert(group)

            // Terminated over 2 weeks ago -> not listed
            it.insert(placement1)

            // No group
            it.insert(placement2)

            // Has group
            it.insert(placement3)
            it.insert(
                DevDaycareGroupPlacement(
                    daycarePlacementId = placement3.id,
                    daycareGroupId = group.id,
                    startDate = today.minusDays(10),
                    endDate = today.plusDays(14),
                )
            )

            // Connected daycare placement is terminated, preschool continues
            it.insert(placement4)
            it.insert(
                DevDaycareGroupPlacement(
                    daycarePlacementId = placement4.id,
                    daycareGroupId = group.id,
                    startDate = today.minusDays(10),
                    endDate = today.plusDays(15),
                )
            )
            it.insert(
                DevPlacement(
                    type = PlacementType.PRESCHOOL,
                    childId = child4.id,
                    unitId = daycare.id,
                    startDate = today.plusDays(16),
                    endDate = today.plusYears(1),
                )
            )

            // Active transfer application
            it.insert(placement6)
            it.insert(
                DevDaycareGroupPlacement(
                    daycarePlacementId = placement6.id,
                    daycareGroupId = group.id,
                    startDate = today.minusDays(10),
                    endDate = today.plusDays(20),
                )
            )
            it.insert(
                DevPlacement(
                    childId = child5.id,
                    unitId = daycare2.id,
                    startDate = today.plusDays(21),
                    endDate = today.plusYears(1),
                )
            )
            it.insertTestApplication(
                    type = ApplicationType.DAYCARE,
                    guardianId = adult.id,
                    childId = child5.id,
                    document =
                        DaycareFormV0.fromApplication2(
                            getValidDaycareApplication(preferredUnit = daycare2)
                        ),
                    status = ApplicationStatus.ACTIVE,
                    confidential = true,
                    transferApplication = true,
                )
                .also { applicationId ->
                    it.insertTestDecision(
                        TestDecision(
                            startDate = today.plusDays(21),
                            endDate = today.plusYears(1),
                            unitId = daycare2.id,
                            createdBy = supervisor.evakaUserId,
                            applicationId = applicationId,
                            type = DecisionType.DAYCARE,
                        )
                    )
                }

            // Active transfer application without group
            it.insert(placement7)
            it.insert(
                DevPlacement(
                    childId = child6.id,
                    unitId = daycare2.id,
                    startDate = today.plusDays(22),
                    endDate = today.plusYears(1),
                )
            )
            it.insertTestApplication(
                    type = ApplicationType.DAYCARE,
                    guardianId = adult.id,
                    childId = child6.id,
                    document =
                        DaycareFormV0.fromApplication2(
                            getValidDaycareApplication(preferredUnit = daycare2)
                        ),
                    status = ApplicationStatus.ACTIVE,
                    confidential = true,
                    transferApplication = true,
                )
                .also { applicationId ->
                    it.insertTestDecision(
                        TestDecision(
                            startDate = today.plusDays(22),
                            endDate = today.plusYears(1),
                            unitId = daycare2.id,
                            createdBy = supervisor.evakaUserId,
                            applicationId = applicationId,
                            type = DecisionType.DAYCARE,
                        )
                    )
                }

            // Old active transfer application, child has been transferred already -> not listed
            it.insert(
                DevPlacement(
                    childId = child7.id,
                    unitId = daycare2.id,
                    startDate = today.minusYears(1),
                    endDate = today.minusDays(11),
                )
            )
            it.insert(
                DevPlacement(
                    childId = child7.id,
                    unitId = daycare.id,
                    startDate = today.minusDays(10),
                    endDate = today.plusDays(21),
                )
            )
            it.insertTestApplication(
                    type = ApplicationType.DAYCARE,
                    guardianId = adult.id,
                    childId = child7.id,
                    document =
                        DaycareFormV0.fromApplication2(
                            getValidDaycareApplication(preferredUnit = daycare2)
                        ),
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
                            unitId = daycare.id,
                            createdBy = supervisor.evakaUserId,
                            applicationId = applicationId,
                            type = DecisionType.DAYCARE,
                        )
                    )
                }
        }

        val details = getGroupDetails(daycare.id)
        assertEquals(
            listOf(
                TerminatedPlacement(
                    id = placement2.id,
                    endDate = today.plusDays(7),
                    type = PlacementType.DAYCARE,
                    terminationRequestedDate = today.minusDays(14),
                    child =
                        ChildBasics(
                            id = child2.id,
                            socialSecurityNumber = child2.ssn,
                            firstName = child2.firstName,
                            lastName = child2.lastName,
                            dateOfBirth = child2.dateOfBirth,
                        ),
                    terminatedBy =
                        EvakaUser(
                            id = adult.evakaUserId(),
                            name = "${adult.lastName} ${adult.firstName}",
                            type = EvakaUserType.CITIZEN,
                        ),
                    currentDaycareGroupName = null,
                    connectedDaycareOnly = false,
                ),
                TerminatedPlacement(
                    id = placement3.id,
                    endDate = today.plusDays(14),
                    type = PlacementType.DAYCARE,
                    terminationRequestedDate = today.minusDays(5),
                    child =
                        ChildBasics(
                            id = child3.id,
                            socialSecurityNumber = child3.ssn,
                            firstName = child3.firstName,
                            lastName = child3.lastName,
                            dateOfBirth = child3.dateOfBirth,
                        ),
                    terminatedBy =
                        EvakaUser(
                            id = adult.evakaUserId(),
                            name = "${adult.lastName} ${adult.firstName}",
                            type = EvakaUserType.CITIZEN,
                        ),
                    currentDaycareGroupName = group.name,
                    connectedDaycareOnly = false,
                ),
                TerminatedPlacement(
                    id = placement4.id,
                    endDate = today.plusDays(15),
                    type = PlacementType.PRESCHOOL_DAYCARE,
                    terminationRequestedDate = today.minusDays(2),
                    child =
                        ChildBasics(
                            id = child4.id,
                            socialSecurityNumber = child4.ssn,
                            firstName = child4.firstName,
                            lastName = child4.lastName,
                            dateOfBirth = child4.dateOfBirth,
                        ),
                    terminatedBy =
                        EvakaUser(
                            id = adult.evakaUserId(),
                            name = "${adult.lastName} ${adult.firstName}",
                            type = EvakaUserType.CITIZEN,
                        ),
                    currentDaycareGroupName = group.name,
                    connectedDaycareOnly = true,
                ),
                TerminatedPlacement(
                    id = placement6.id,
                    endDate = today.plusDays(20),
                    type = PlacementType.DAYCARE,
                    terminationRequestedDate = null,
                    child =
                        ChildBasics(
                            id = child5.id,
                            socialSecurityNumber = child5.ssn,
                            firstName = child5.firstName,
                            lastName = child5.lastName,
                            dateOfBirth = child5.dateOfBirth,
                        ),
                    terminatedBy =
                        EvakaUser(
                            id = adult.evakaUserId(),
                            name = "${adult.lastName} ${adult.firstName}",
                            type = EvakaUserType.CITIZEN,
                        ),
                    currentDaycareGroupName = group.name,
                    connectedDaycareOnly = false,
                ),
                TerminatedPlacement(
                    id = placement7.id,
                    endDate = today.plusDays(21),
                    type = PlacementType.DAYCARE,
                    terminationRequestedDate = null,
                    child =
                        ChildBasics(
                            id = child6.id,
                            socialSecurityNumber = child6.ssn,
                            firstName = child6.firstName,
                            lastName = child6.lastName,
                            dateOfBirth = child6.dateOfBirth,
                        ),
                    terminatedBy =
                        EvakaUser(
                            id = adult.evakaUserId(),
                            name = "${adult.lastName} ${adult.firstName}",
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

        val daycare = getDaycare(daycare.id).daycare
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
                childId = child1.id,
                unitId = daycare.id,
                startDate = today,
                endDate = endDate,
            )
        db.transaction { tx ->
            tx.insert(admin)
            tx.insert(placement)
        }

        val daycare = getDaycare(daycare.id).daycare
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
                childId = child1.id,
                unitId = daycare.id,
                period = FiniteDateRange(today, endDate),
            )
        db.transaction { tx ->
            tx.insert(admin)
            tx.insert(backupCare)
        }

        val daycare = getDaycare(daycare.id).daycare
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

        val daycare = getDaycare(daycare.id).daycare
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
                daycareId = daycare.id,
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
                daycareId = daycare.id,
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
                daycareId = daycare.id,
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
        daycareController.updateDaycare(dbInstance(), user, MockEvakaClock(now), daycare.id, fields)
    }

    private fun updateUnitClosingDate(user: AuthenticatedUser.Employee, closingDate: LocalDate) {
        daycareController.updateUnitClosingDate(
            dbInstance(),
            user,
            MockEvakaClock(now),
            daycare.id,
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
        nekkuOrderReductionPercentage = 15,
        nekkuNoWeekendMealOrders = false,
    )
