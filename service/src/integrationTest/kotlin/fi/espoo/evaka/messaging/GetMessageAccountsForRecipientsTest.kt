// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.messaging

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.daycare.CareType
import fi.espoo.evaka.pis.service.insertGuardian
import fi.espoo.evaka.placement.MessagingCategory
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.serviceneed.ShiftCareType
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.MessageAccountId
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.insertDaycareAclRow
import fi.espoo.evaka.shared.dev.DevBackupCare
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.DevDaycareGroupPlacement
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevFosterParent
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.DevServiceNeed
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.security.PilotFeature
import fi.espoo.evaka.snDefaultDaycare
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.FieldSource

class GetMessageAccountsForRecipientsTest : PureJdbiTest(resetDbBeforeEach = true) {
    private val employee1 = DevEmployee(firstName = "Firstname", lastName = "Employee")

    @BeforeEach
    fun setUp() {
        db.transaction { tx -> tx.insert(employee1) }
    }

    @Test
    fun `getMessageAccountsForRecipients returns correct accounts`() {
        val today = LocalDate.of(2021, 5, 1)

        val area = DevCareArea()
        val daycare =
            DevDaycare(areaId = area.id, enabledPilotFeatures = setOf(PilotFeature.MESSAGING))
        val group = DevDaycareGroup(daycareId = daycare.id)

        val currentChild = DevPerson()
        val starterChild = DevPerson()
        val changerChild = DevPerson()
        val currentParent = DevPerson()
        val starterParent = DevPerson()
        val changerParent = DevPerson()
        val currentPlacement =
            DevPlacement(
                childId = currentChild.id,
                unitId = daycare.id,
                startDate = today.minusDays(10),
                endDate = today.plusDays(30),
            )
        val starterPlacement =
            DevPlacement(
                childId = starterChild.id,
                unitId = daycare.id,
                startDate = today.plusDays(5),
                endDate = today.plusDays(30),
            )
        val changerPlacement1 =
            DevPlacement(
                childId = changerChild.id,
                unitId = daycare.id,
                startDate = today.minusDays(10),
                endDate = today.plusDays(30),
            )
        val changerPlacement2 =
            DevPlacement(
                childId = changerChild.id,
                unitId = daycare.id,
                startDate = today.plusDays(31),
                endDate = today.plusDays(60),
            )

        val (
            employeeAccountId,
            currentParentAccountId,
            starterParentAccountId,
            changerParentAccountId) =
            db.transaction { tx ->
                tx.insert(area)
                tx.insert(daycare)
                tx.insert(group)

                tx.insert(currentChild, DevPersonType.CHILD)
                tx.insert(starterChild, DevPersonType.CHILD)
                tx.insert(changerChild, DevPersonType.CHILD)
                tx.insert(currentParent, DevPersonType.ADULT)
                tx.insert(starterParent, DevPersonType.ADULT)
                tx.insert(changerParent, DevPersonType.ADULT)

                tx.insertGuardian(guardianId = currentParent.id, childId = currentChild.id)
                tx.insertGuardian(guardianId = starterParent.id, childId = starterChild.id)
                tx.insertGuardian(guardianId = changerParent.id, childId = changerChild.id)

                tx.insert(currentPlacement)
                tx.insert(starterPlacement)
                tx.insert(changerPlacement1)
                tx.insert(changerPlacement2)

                tx.insert(
                    DevDaycareGroupPlacement(
                        daycareGroupId = group.id,
                        daycarePlacementId = currentPlacement.id,
                        startDate = today.minusDays(10),
                        endDate = today.plusDays(30),
                    )
                )
                tx.insert(
                    DevDaycareGroupPlacement(
                        daycareGroupId = group.id,
                        daycarePlacementId = starterPlacement.id,
                        startDate = today.plusDays(5),
                        endDate = today.plusDays(30),
                    )
                )
                tx.insert(
                    DevDaycareGroupPlacement(
                        daycareGroupId = group.id,
                        daycarePlacementId = changerPlacement1.id,
                        startDate = today.minusDays(10),
                        endDate = today.plusDays(30),
                    )
                )
                tx.insert(
                    DevDaycareGroupPlacement(
                        daycareGroupId = group.id,
                        daycarePlacementId = changerPlacement2.id,
                        startDate = today.plusDays(31),
                        endDate = today.plusDays(60),
                    )
                )

                val currentParentAccountId = tx.getCitizenMessageAccount(currentParent.id)
                val starterParentAccountId = tx.getCitizenMessageAccount(starterParent.id)
                val changerParentAccountId = tx.getCitizenMessageAccount(changerParent.id)

                tx.insertDaycareAclRow(daycare.id, employee1.id, UserRole.UNIT_SUPERVISOR)
                val employeeAccountId = tx.upsertEmployeeMessageAccount(employee1.id)

                data class AccountIds(
                    val accountId1: MessageAccountId,
                    val accountId2: MessageAccountId,
                    val accountId3: MessageAccountId,
                    val accountId4: MessageAccountId,
                )

                AccountIds(
                    employeeAccountId,
                    currentParentAccountId,
                    starterParentAccountId,
                    changerParentAccountId,
                )
            }

        val areaRecipients = setOf(MessageRecipient.Area(area.id))
        val areaAccounts =
            db.read { tx ->
                tx.getMessageAccountsForRecipients(
                    senderAccount = tx.getSenderAccount(employeeAccountId),
                    recipients = areaRecipients,
                    filters = null,
                    date = today,
                )
            }

        assertEquals(2, areaAccounts.size)
        assertTrue(areaAccounts.any { it.first == currentParentAccountId })
        assertTrue(areaAccounts.any { it.first == changerParentAccountId })

        val currentUnitAccounts =
            db.read { tx ->
                tx.getMessageAccountsForRecipients(
                    senderAccount = tx.getSenderAccount(employeeAccountId),
                    recipients = setOf(MessageRecipient.Unit(daycare.id, false)),
                    filters = null,
                    date = today,
                )
            }

        assertEquals(2, currentUnitAccounts.size)
        assertTrue(currentUnitAccounts.any { it.first == currentParentAccountId })
        assertTrue(currentUnitAccounts.any { it.first == changerParentAccountId })

        val starterUnitAccounts =
            db.read { tx ->
                tx.getMessageAccountsForRecipients(
                    senderAccount = tx.getSenderAccount(employeeAccountId),
                    recipients = setOf(MessageRecipient.Unit(daycare.id, true)),
                    filters = null,
                    date = today,
                )
            }

        assertEquals(1, starterUnitAccounts.size)
        assertTrue(starterUnitAccounts.any { it.first == starterParentAccountId })

        val currentGroupAccounts =
            db.read { tx ->
                tx.getMessageAccountsForRecipients(
                    senderAccount = tx.getSenderAccount(employeeAccountId),
                    recipients = setOf(MessageRecipient.Group(group.id, false)),
                    filters = null,
                    date = today,
                )
            }

        assertEquals(2, currentGroupAccounts.size)
        assertTrue(currentGroupAccounts.any { it.first == currentParentAccountId })
        assertTrue(currentGroupAccounts.any { it.first == changerParentAccountId })

        val starterGroupAccounts =
            db.read { tx ->
                tx.getMessageAccountsForRecipients(
                    senderAccount = tx.getSenderAccount(employeeAccountId),
                    recipients = setOf(MessageRecipient.Group(group.id, true)),
                    filters = null,
                    date = today,
                )
            }

        assertEquals(1, starterGroupAccounts.size)
        assertTrue(starterGroupAccounts.any { it.first == starterParentAccountId })

        val childRecipients = setOf(MessageRecipient.Child(currentChild.id))
        val childAccounts =
            db.read { tx ->
                tx.getMessageAccountsForRecipients(
                    senderAccount = tx.getSenderAccount(employeeAccountId),
                    recipients = childRecipients,
                    filters = null,
                    date = today,
                )
            }

        assertEquals(1, childAccounts.size)
        assertTrue(childAccounts.any { it.first == currentParentAccountId })
    }

    @Suppress("unused")
    private val placementFilters: List<Pair<List<MessagingCategory>, Int>> =
        listOf(
            listOf(MessagingCategory.MESSAGING_DAYCARE) to 4,
            listOf(MessagingCategory.MESSAGING_PRESCHOOL) to 2,
            listOf(MessagingCategory.MESSAGING_CLUB) to 1,
            listOf(MessagingCategory.MESSAGING_CLUB, MessagingCategory.MESSAGING_DAYCARE) to 5,
            listOf(MessagingCategory.MESSAGING_PRESCHOOL, MessagingCategory.MESSAGING_DAYCARE) to 6,
            listOf<MessagingCategory>() to 7,
        )

    @ParameterizedTest(name = "messagingCategoryList, expectedRecipientCount={0}")
    @FieldSource("placementFilters")
    fun `getMessageAccountsForRecipients returns correct accounts for different placement type categories`(
        arg: Pair<List<MessagingCategory>, Int>
    ) {
        val (messagingCategoryList, expectedRecipientCount) = arg
        val today = LocalDate.now()
        val area = DevCareArea()
        val daycare =
            DevDaycare(areaId = area.id, enabledPilotFeatures = setOf(PilotFeature.MESSAGING))
        val daycareGroup = DevDaycareGroup(daycareId = daycare.id)
        val preschoolGroup = DevDaycareGroup(daycareId = daycare.id)
        val clubGroup = DevDaycareGroup(daycareId = daycare.id)

        val dayCareChild1a = DevPerson()
        val dayCareChild1b = DevPerson()
        val dayCareChild2 = DevPerson()
        val dayCareChild3 = DevPerson()
        val preSchoolChild1 = DevPerson()
        val preSchoolChild2 = DevPerson()
        val clubChild = DevPerson()

        val daycareParent1 = DevPerson()
        val daycareParent2 = DevPerson()
        val daycareParent3 = DevPerson()
        val preschoolParent1 = DevPerson()
        val preschoolParent2 = DevPerson()
        val clubParent = DevPerson()

        val (
            employeeAccountId,
        ) = db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(daycareGroup)
            tx.insert(preschoolGroup)
            tx.insert(clubGroup)

            tx.insert(dayCareChild1a, DevPersonType.CHILD)
            tx.insert(dayCareChild1b, DevPersonType.CHILD)
            tx.insert(dayCareChild2, DevPersonType.CHILD)
            tx.insert(dayCareChild3, DevPersonType.CHILD)
            tx.insert(preSchoolChild1, DevPersonType.CHILD)
            tx.insert(preSchoolChild2, DevPersonType.CHILD)
            tx.insert(clubChild, DevPersonType.CHILD)

            tx.insert(daycareParent1, DevPersonType.ADULT)
            tx.insert(daycareParent2, DevPersonType.ADULT)
            tx.insert(daycareParent3, DevPersonType.ADULT)
            tx.insert(preschoolParent1, DevPersonType.ADULT)
            tx.insert(preschoolParent2, DevPersonType.ADULT)
            tx.insert(clubParent, DevPersonType.ADULT)

            tx.insertGuardian(guardianId = daycareParent1.id, childId = dayCareChild1a.id)
            tx.insertGuardian(guardianId = daycareParent1.id, childId = dayCareChild1b.id)
            tx.insertGuardian(guardianId = daycareParent2.id, childId = dayCareChild2.id)
            tx.insertGuardian(guardianId = daycareParent3.id, childId = dayCareChild3.id)
            tx.insertGuardian(guardianId = preschoolParent1.id, childId = preSchoolChild1.id)
            tx.insertGuardian(guardianId = preschoolParent2.id, childId = preSchoolChild2.id)
            tx.insertGuardian(guardianId = clubParent.id, childId = clubChild.id)

            val daycarePlacement1 =
                tx.insert(
                    DevPlacement(
                        childId = dayCareChild1a.id,
                        unitId = daycare.id,
                        startDate = today.minusDays(10),
                        endDate = today.plusDays(30),
                        type = PlacementType.DAYCARE,
                    )
                )
            val daycarePlacement2 =
                tx.insert(
                    DevPlacement(
                        childId = dayCareChild1b.id,
                        unitId = daycare.id,
                        startDate = today.minusDays(10),
                        endDate = today.plusDays(30),
                        type = PlacementType.DAYCARE_PART_TIME,
                    )
                )
            val daycarePlacement3 =
                tx.insert(
                    DevPlacement(
                        childId = dayCareChild2.id,
                        unitId = daycare.id,
                        startDate = today.minusDays(10),
                        endDate = today.plusDays(30),
                        type = PlacementType.DAYCARE,
                    )
                )
            val daycarePlacement4 =
                tx.insert(
                    DevPlacement(
                        childId = dayCareChild3.id,
                        unitId = daycare.id,
                        startDate = today.minusDays(10),
                        endDate = today.plusDays(30),
                        type = PlacementType.DAYCARE_FIVE_YEAR_OLDS,
                    )
                )

            val preschoolPlacement1 =
                tx.insert(
                    DevPlacement(
                        childId = preSchoolChild1.id,
                        unitId = daycare.id,
                        startDate = today.minusDays(10),
                        endDate = today.plusDays(30),
                        type = PlacementType.PRESCHOOL,
                    )
                )
            val preschoolPlacement2 =
                tx.insert(
                    DevPlacement(
                        childId = preSchoolChild2.id,
                        unitId = daycare.id,
                        startDate = today.minusDays(10),
                        endDate = today.plusDays(30),
                        type = PlacementType.PRESCHOOL_DAYCARE,
                    )
                )
            val clubPlacement1 =
                tx.insert(
                    DevPlacement(
                        childId = clubChild.id,
                        unitId = daycare.id,
                        startDate = today.minusDays(10),
                        endDate = today.plusDays(30),
                        type = PlacementType.CLUB,
                    )
                )

            tx.insert(
                DevDaycareGroupPlacement(
                    daycareGroupId = daycareGroup.id,
                    daycarePlacementId = daycarePlacement1,
                    startDate = today.minusDays(10),
                    endDate = today.plusDays(30),
                )
            )
            tx.insert(
                DevDaycareGroupPlacement(
                    daycareGroupId = daycareGroup.id,
                    daycarePlacementId = daycarePlacement2,
                    startDate = today.minusDays(10),
                    endDate = today.plusDays(30),
                )
            )
            tx.insert(
                DevDaycareGroupPlacement(
                    daycareGroupId = daycareGroup.id,
                    daycarePlacementId = daycarePlacement3,
                    startDate = today.minusDays(10),
                    endDate = today.plusDays(30),
                )
            )
            tx.insert(
                DevDaycareGroupPlacement(
                    daycareGroupId = daycareGroup.id,
                    daycarePlacementId = daycarePlacement4,
                    startDate = today.minusDays(10),
                    endDate = today.plusDays(30),
                )
            )
            tx.insert(
                DevDaycareGroupPlacement(
                    daycareGroupId = preschoolGroup.id,
                    daycarePlacementId = preschoolPlacement1,
                    startDate = today.minusDays(10),
                    endDate = today.plusDays(30),
                )
            )
            tx.insert(
                DevDaycareGroupPlacement(
                    daycareGroupId = preschoolGroup.id,
                    daycarePlacementId = preschoolPlacement2,
                    startDate = today.minusDays(10),
                    endDate = today.plusDays(30),
                )
            )
            tx.insert(
                DevDaycareGroupPlacement(
                    daycareGroupId = clubGroup.id,
                    daycarePlacementId = clubPlacement1,
                    startDate = today.minusDays(10),
                    endDate = today.plusDays(30),
                )
            )

            tx.insertDaycareAclRow(daycare.id, employee1.id, UserRole.UNIT_SUPERVISOR)
            val employeeAccountId = tx.upsertEmployeeMessageAccount(employee1.id)

            data class AccountIds(val accountId1: MessageAccountId)

            AccountIds(employeeAccountId)
        }

        val postMessageFilter =
            messagingCategoryList
                .takeIf { it.isNotEmpty() }
                ?.let { MessageController.PostMessageFilters(placementTypes = it) }

        val areaRecipients = setOf(MessageRecipient.Area(area.id))

        val recipientMessageAccounts =
            db.read { tx ->
                tx.getMessageAccountsForRecipients(
                    senderAccount = tx.getSenderAccount(employeeAccountId),
                    recipients = areaRecipients,
                    filters = postMessageFilter,
                    date = today,
                )
            }

        assertEquals(expectedRecipientCount, recipientMessageAccounts.size)
    }

    @Test
    fun `getMessageAccountsForRecipients with year of birth filter returns only matching children`() {
        val today = LocalDate.of(2021, 5, 1)
        val area = DevCareArea()
        val daycare =
            DevDaycare(areaId = area.id, enabledPilotFeatures = setOf(PilotFeature.MESSAGING))
        val group = DevDaycareGroup(daycareId = daycare.id)

        val child2018 = DevPerson(dateOfBirth = LocalDate.of(2018, 3, 15))
        val child2019 = DevPerson(dateOfBirth = LocalDate.of(2019, 6, 20))
        val child2020 = DevPerson(dateOfBirth = LocalDate.of(2020, 1, 10))
        val parent2018 = DevPerson()
        val parent2019 = DevPerson()
        val parent2020 = DevPerson()

        val employeeAccountId =
            db.transaction { tx ->
                tx.insert(area)
                tx.insert(daycare)
                tx.insert(group)

                tx.insert(child2018, DevPersonType.CHILD)
                tx.insert(child2019, DevPersonType.CHILD)
                tx.insert(child2020, DevPersonType.CHILD)
                tx.insert(parent2018, DevPersonType.ADULT)
                tx.insert(parent2019, DevPersonType.ADULT)
                tx.insert(parent2020, DevPersonType.ADULT)

                tx.insertGuardian(guardianId = parent2018.id, childId = child2018.id)
                tx.insertGuardian(guardianId = parent2019.id, childId = child2019.id)
                tx.insertGuardian(guardianId = parent2020.id, childId = child2020.id)

                listOf(child2018, child2019, child2020).forEach { child ->
                    val placement =
                        tx.insert(
                            DevPlacement(
                                childId = child.id,
                                unitId = daycare.id,
                                startDate = today.minusDays(10),
                                endDate = today.plusDays(30),
                            )
                        )
                    tx.insert(
                        DevDaycareGroupPlacement(
                            daycareGroupId = group.id,
                            daycarePlacementId = placement,
                            startDate = today.minusDays(10),
                            endDate = today.plusDays(30),
                        )
                    )
                }

                tx.insertDaycareAclRow(daycare.id, employee1.id, UserRole.UNIT_SUPERVISOR)
                tx.upsertEmployeeMessageAccount(employee1.id)
            }

        val parent2018AccountId = db.read { tx -> tx.getCitizenMessageAccount(parent2018.id) }
        val parent2019AccountId = db.read { tx -> tx.getCitizenMessageAccount(parent2019.id) }
        val parent2020AccountId = db.read { tx -> tx.getCitizenMessageAccount(parent2020.id) }

        val filter2018 = MessageController.PostMessageFilters(yearsOfBirth = listOf(2018))
        val accounts2018 =
            db.read { tx ->
                tx.getMessageAccountsForRecipients(
                    senderAccount = tx.getSenderAccount(employeeAccountId),
                    recipients = setOf(MessageRecipient.Area(area.id)),
                    filters = filter2018,
                    date = today,
                )
            }

        assertEquals(1, accounts2018.size)
        assertTrue(accounts2018.any { it.first == parent2018AccountId })

        val filter2019And2020 =
            MessageController.PostMessageFilters(yearsOfBirth = listOf(2019, 2020))
        val accounts2019And2020 =
            db.read { tx ->
                tx.getMessageAccountsForRecipients(
                    senderAccount = tx.getSenderAccount(employeeAccountId),
                    recipients = setOf(MessageRecipient.Area(area.id)),
                    filters = filter2019And2020,
                    date = today,
                )
            }

        assertEquals(2, accounts2019And2020.size)
        assertTrue(accounts2019And2020.any { it.first == parent2019AccountId })
        assertTrue(accounts2019And2020.any { it.first == parent2020AccountId })
        assertFalse(accounts2019And2020.any { it.first == parent2018AccountId })
    }

    @Test
    fun `getMessageAccountsForRecipients with shift care filter returns only FULL shift care children`() {
        val today = LocalDate.of(2021, 5, 1)
        val area = DevCareArea()
        val daycare =
            DevDaycare(areaId = area.id, enabledPilotFeatures = setOf(PilotFeature.MESSAGING))
        val group = DevDaycareGroup(daycareId = daycare.id)

        val childWithFullShiftCare = DevPerson()
        val childWithIntermittentShiftCare = DevPerson()
        val childWithNoShiftCare = DevPerson()
        val parentFullShift = DevPerson()
        val parentIntermittentShift = DevPerson()
        val parentNoShift = DevPerson()

        val employeeAccountId =
            db.transaction { tx ->
                tx.insert(area)
                tx.insert(daycare)
                tx.insert(group)
                tx.insert(snDefaultDaycare)

                tx.insert(childWithFullShiftCare, DevPersonType.CHILD)
                tx.insert(childWithIntermittentShiftCare, DevPersonType.CHILD)
                tx.insert(childWithNoShiftCare, DevPersonType.CHILD)
                tx.insert(parentFullShift, DevPersonType.ADULT)
                tx.insert(parentIntermittentShift, DevPersonType.ADULT)
                tx.insert(parentNoShift, DevPersonType.ADULT)

                tx.insertGuardian(
                    guardianId = parentFullShift.id,
                    childId = childWithFullShiftCare.id,
                )
                tx.insertGuardian(
                    guardianId = parentIntermittentShift.id,
                    childId = childWithIntermittentShiftCare.id,
                )
                tx.insertGuardian(guardianId = parentNoShift.id, childId = childWithNoShiftCare.id)

                val placementFull =
                    tx.insert(
                        DevPlacement(
                            childId = childWithFullShiftCare.id,
                            unitId = daycare.id,
                            startDate = today.minusDays(10),
                            endDate = today.plusDays(30),
                        )
                    )
                val placementIntermittent =
                    tx.insert(
                        DevPlacement(
                            childId = childWithIntermittentShiftCare.id,
                            unitId = daycare.id,
                            startDate = today.minusDays(10),
                            endDate = today.plusDays(30),
                        )
                    )
                val placementNone =
                    tx.insert(
                        DevPlacement(
                            childId = childWithNoShiftCare.id,
                            unitId = daycare.id,
                            startDate = today.minusDays(10),
                            endDate = today.plusDays(30),
                        )
                    )

                tx.insert(
                    DevDaycareGroupPlacement(
                        daycareGroupId = group.id,
                        daycarePlacementId = placementFull,
                        startDate = today.minusDays(10),
                        endDate = today.plusDays(30),
                    )
                )
                tx.insert(
                    DevDaycareGroupPlacement(
                        daycareGroupId = group.id,
                        daycarePlacementId = placementIntermittent,
                        startDate = today.minusDays(10),
                        endDate = today.plusDays(30),
                    )
                )
                tx.insert(
                    DevDaycareGroupPlacement(
                        daycareGroupId = group.id,
                        daycarePlacementId = placementNone,
                        startDate = today.minusDays(10),
                        endDate = today.plusDays(30),
                    )
                )

                tx.insert(
                    DevServiceNeed(
                        placementId = placementFull,
                        startDate = today.minusDays(10),
                        endDate = today.plusDays(30),
                        optionId = snDefaultDaycare.id,
                        shiftCare = ShiftCareType.FULL,
                        confirmedBy = EvakaUserId(employee1.id.raw),
                    )
                )
                tx.insert(
                    DevServiceNeed(
                        placementId = placementIntermittent,
                        startDate = today.minusDays(10),
                        endDate = today.plusDays(30),
                        optionId = snDefaultDaycare.id,
                        shiftCare = ShiftCareType.INTERMITTENT,
                        confirmedBy = EvakaUserId(employee1.id.raw),
                    )
                )
                tx.insert(
                    DevServiceNeed(
                        placementId = placementNone,
                        startDate = today.minusDays(10),
                        endDate = today.plusDays(30),
                        optionId = snDefaultDaycare.id,
                        shiftCare = ShiftCareType.NONE,
                        confirmedBy = EvakaUserId(employee1.id.raw),
                    )
                )

                tx.insertDaycareAclRow(daycare.id, employee1.id, UserRole.UNIT_SUPERVISOR)
                tx.upsertEmployeeMessageAccount(employee1.id)
            }

        val parentFullShiftAccountId =
            db.read { tx -> tx.getCitizenMessageAccount(parentFullShift.id) }

        val filter = MessageController.PostMessageFilters(shiftCare = true)
        val accounts =
            db.read { tx ->
                tx.getMessageAccountsForRecipients(
                    senderAccount = tx.getSenderAccount(employeeAccountId),
                    recipients = setOf(MessageRecipient.Area(area.id)),
                    filters = filter,
                    date = today,
                )
            }

        assertEquals(1, accounts.size)
        assertTrue(accounts.any { it.first == parentFullShiftAccountId })
    }

    @Test
    fun `getMessageAccountsForRecipients with intermittent shift care filter returns only INTERMITTENT shift care children`() {
        val today = LocalDate.of(2021, 5, 1)
        val area = DevCareArea()
        val daycare =
            DevDaycare(areaId = area.id, enabledPilotFeatures = setOf(PilotFeature.MESSAGING))
        val group = DevDaycareGroup(daycareId = daycare.id)

        val childWithFullShiftCare = DevPerson()
        val childWithIntermittentShiftCare = DevPerson()
        val childWithNoShiftCare = DevPerson()
        val parentFullShift = DevPerson()
        val parentIntermittentShift = DevPerson()
        val parentNoShift = DevPerson()

        val employeeAccountId =
            db.transaction { tx ->
                tx.insert(area)
                tx.insert(daycare)
                tx.insert(group)
                tx.insert(snDefaultDaycare)

                tx.insert(childWithFullShiftCare, DevPersonType.CHILD)
                tx.insert(childWithIntermittentShiftCare, DevPersonType.CHILD)
                tx.insert(childWithNoShiftCare, DevPersonType.CHILD)
                tx.insert(parentFullShift, DevPersonType.ADULT)
                tx.insert(parentIntermittentShift, DevPersonType.ADULT)
                tx.insert(parentNoShift, DevPersonType.ADULT)

                tx.insertGuardian(
                    guardianId = parentFullShift.id,
                    childId = childWithFullShiftCare.id,
                )
                tx.insertGuardian(
                    guardianId = parentIntermittentShift.id,
                    childId = childWithIntermittentShiftCare.id,
                )
                tx.insertGuardian(guardianId = parentNoShift.id, childId = childWithNoShiftCare.id)

                val placementFull =
                    tx.insert(
                        DevPlacement(
                            childId = childWithFullShiftCare.id,
                            unitId = daycare.id,
                            startDate = today.minusDays(10),
                            endDate = today.plusDays(30),
                        )
                    )
                val placementIntermittent =
                    tx.insert(
                        DevPlacement(
                            childId = childWithIntermittentShiftCare.id,
                            unitId = daycare.id,
                            startDate = today.minusDays(10),
                            endDate = today.plusDays(30),
                        )
                    )
                val placementNone =
                    tx.insert(
                        DevPlacement(
                            childId = childWithNoShiftCare.id,
                            unitId = daycare.id,
                            startDate = today.minusDays(10),
                            endDate = today.plusDays(30),
                        )
                    )

                tx.insert(
                    DevDaycareGroupPlacement(
                        daycareGroupId = group.id,
                        daycarePlacementId = placementFull,
                        startDate = today.minusDays(10),
                        endDate = today.plusDays(30),
                    )
                )
                tx.insert(
                    DevDaycareGroupPlacement(
                        daycareGroupId = group.id,
                        daycarePlacementId = placementIntermittent,
                        startDate = today.minusDays(10),
                        endDate = today.plusDays(30),
                    )
                )
                tx.insert(
                    DevDaycareGroupPlacement(
                        daycareGroupId = group.id,
                        daycarePlacementId = placementNone,
                        startDate = today.minusDays(10),
                        endDate = today.plusDays(30),
                    )
                )

                tx.insert(
                    DevServiceNeed(
                        placementId = placementFull,
                        startDate = today.minusDays(10),
                        endDate = today.plusDays(30),
                        optionId = snDefaultDaycare.id,
                        shiftCare = ShiftCareType.FULL,
                        confirmedBy = EvakaUserId(employee1.id.raw),
                    )
                )
                tx.insert(
                    DevServiceNeed(
                        placementId = placementIntermittent,
                        startDate = today.minusDays(10),
                        endDate = today.plusDays(30),
                        optionId = snDefaultDaycare.id,
                        shiftCare = ShiftCareType.INTERMITTENT,
                        confirmedBy = EvakaUserId(employee1.id.raw),
                    )
                )
                tx.insert(
                    DevServiceNeed(
                        placementId = placementNone,
                        startDate = today.minusDays(10),
                        endDate = today.plusDays(30),
                        optionId = snDefaultDaycare.id,
                        shiftCare = ShiftCareType.NONE,
                        confirmedBy = EvakaUserId(employee1.id.raw),
                    )
                )

                tx.insertDaycareAclRow(daycare.id, employee1.id, UserRole.UNIT_SUPERVISOR)
                tx.upsertEmployeeMessageAccount(employee1.id)
            }

        val parentIntermittentShiftAccountId =
            db.read { tx -> tx.getCitizenMessageAccount(parentIntermittentShift.id) }

        val filter = MessageController.PostMessageFilters(intermittentShiftCare = true)
        val accounts =
            db.read { tx ->
                tx.getMessageAccountsForRecipients(
                    senderAccount = tx.getSenderAccount(employeeAccountId),
                    recipients = setOf(MessageRecipient.Area(area.id)),
                    filters = filter,
                    date = today,
                )
            }

        assertEquals(1, accounts.size)
        assertTrue(accounts.any { it.first == parentIntermittentShiftAccountId })
    }

    @Test
    fun `getMessageAccountsForRecipients with both shift care filters returns FULL or INTERMITTENT shift care children`() {
        val today = LocalDate.of(2021, 5, 1)
        val area = DevCareArea()
        val daycare =
            DevDaycare(areaId = area.id, enabledPilotFeatures = setOf(PilotFeature.MESSAGING))
        val group = DevDaycareGroup(daycareId = daycare.id)

        val childWithFullShiftCare = DevPerson()
        val childWithIntermittentShiftCare = DevPerson()
        val childWithNoShiftCare = DevPerson()
        val parentFullShift = DevPerson()
        val parentIntermittentShift = DevPerson()
        val parentNoShift = DevPerson()

        val employeeAccountId =
            db.transaction { tx ->
                tx.insert(area)
                tx.insert(daycare)
                tx.insert(group)
                tx.insert(snDefaultDaycare)

                tx.insert(childWithFullShiftCare, DevPersonType.CHILD)
                tx.insert(childWithIntermittentShiftCare, DevPersonType.CHILD)
                tx.insert(childWithNoShiftCare, DevPersonType.CHILD)
                tx.insert(parentFullShift, DevPersonType.ADULT)
                tx.insert(parentIntermittentShift, DevPersonType.ADULT)
                tx.insert(parentNoShift, DevPersonType.ADULT)

                tx.insertGuardian(
                    guardianId = parentFullShift.id,
                    childId = childWithFullShiftCare.id,
                )
                tx.insertGuardian(
                    guardianId = parentIntermittentShift.id,
                    childId = childWithIntermittentShiftCare.id,
                )
                tx.insertGuardian(guardianId = parentNoShift.id, childId = childWithNoShiftCare.id)

                val placementFull =
                    tx.insert(
                        DevPlacement(
                            childId = childWithFullShiftCare.id,
                            unitId = daycare.id,
                            startDate = today.minusDays(10),
                            endDate = today.plusDays(30),
                        )
                    )
                val placementIntermittent =
                    tx.insert(
                        DevPlacement(
                            childId = childWithIntermittentShiftCare.id,
                            unitId = daycare.id,
                            startDate = today.minusDays(10),
                            endDate = today.plusDays(30),
                        )
                    )
                val placementNone =
                    tx.insert(
                        DevPlacement(
                            childId = childWithNoShiftCare.id,
                            unitId = daycare.id,
                            startDate = today.minusDays(10),
                            endDate = today.plusDays(30),
                        )
                    )

                tx.insert(
                    DevDaycareGroupPlacement(
                        daycareGroupId = group.id,
                        daycarePlacementId = placementFull,
                        startDate = today.minusDays(10),
                        endDate = today.plusDays(30),
                    )
                )
                tx.insert(
                    DevDaycareGroupPlacement(
                        daycareGroupId = group.id,
                        daycarePlacementId = placementIntermittent,
                        startDate = today.minusDays(10),
                        endDate = today.plusDays(30),
                    )
                )
                tx.insert(
                    DevDaycareGroupPlacement(
                        daycareGroupId = group.id,
                        daycarePlacementId = placementNone,
                        startDate = today.minusDays(10),
                        endDate = today.plusDays(30),
                    )
                )

                tx.insert(
                    DevServiceNeed(
                        placementId = placementFull,
                        startDate = today.minusDays(10),
                        endDate = today.plusDays(30),
                        optionId = snDefaultDaycare.id,
                        shiftCare = ShiftCareType.FULL,
                        confirmedBy = EvakaUserId(employee1.id.raw),
                    )
                )
                tx.insert(
                    DevServiceNeed(
                        placementId = placementIntermittent,
                        startDate = today.minusDays(10),
                        endDate = today.plusDays(30),
                        optionId = snDefaultDaycare.id,
                        shiftCare = ShiftCareType.INTERMITTENT,
                        confirmedBy = EvakaUserId(employee1.id.raw),
                    )
                )
                tx.insert(
                    DevServiceNeed(
                        placementId = placementNone,
                        startDate = today.minusDays(10),
                        endDate = today.plusDays(30),
                        optionId = snDefaultDaycare.id,
                        shiftCare = ShiftCareType.NONE,
                        confirmedBy = EvakaUserId(employee1.id.raw),
                    )
                )

                tx.insertDaycareAclRow(daycare.id, employee1.id, UserRole.UNIT_SUPERVISOR)
                tx.upsertEmployeeMessageAccount(employee1.id)
            }

        val parentFullShiftAccountId =
            db.read { tx -> tx.getCitizenMessageAccount(parentFullShift.id) }
        val parentIntermittentShiftAccountId =
            db.read { tx -> tx.getCitizenMessageAccount(parentIntermittentShift.id) }

        val filter =
            MessageController.PostMessageFilters(shiftCare = true, intermittentShiftCare = true)
        val accounts =
            db.read { tx ->
                tx.getMessageAccountsForRecipients(
                    senderAccount = tx.getSenderAccount(employeeAccountId),
                    recipients = setOf(MessageRecipient.Area(area.id)),
                    filters = filter,
                    date = today,
                )
            }

        assertEquals(2, accounts.size)
        assertTrue(accounts.any { it.first == parentFullShiftAccountId })
        assertTrue(accounts.any { it.first == parentIntermittentShiftAccountId })
    }

    @Test
    fun `getMessageAccountsForRecipients with family daycare filter returns only children in FAMILY or GROUP_FAMILY units`() {
        val today = LocalDate.of(2021, 5, 1)
        val area = DevCareArea()
        val familyDaycare =
            DevDaycare(
                areaId = area.id,
                type = setOf(CareType.FAMILY),
                enabledPilotFeatures = setOf(PilotFeature.MESSAGING),
            )
        val groupFamilyDaycare =
            DevDaycare(
                areaId = area.id,
                type = setOf(CareType.GROUP_FAMILY),
                enabledPilotFeatures = setOf(PilotFeature.MESSAGING),
            )
        val centreDaycare =
            DevDaycare(
                areaId = area.id,
                type = setOf(CareType.CENTRE),
                enabledPilotFeatures = setOf(PilotFeature.MESSAGING),
            )

        val familyGroup = DevDaycareGroup(daycareId = familyDaycare.id)
        val groupFamilyGroup = DevDaycareGroup(daycareId = groupFamilyDaycare.id)
        val centreGroup = DevDaycareGroup(daycareId = centreDaycare.id)

        val childInFamily = DevPerson()
        val childInGroupFamily = DevPerson()
        val childInCentre = DevPerson()
        val parentFamily = DevPerson()
        val parentGroupFamily = DevPerson()
        val parentCentre = DevPerson()

        val employeeAccountId =
            db.transaction { tx ->
                tx.insert(area)
                tx.insert(familyDaycare)
                tx.insert(groupFamilyDaycare)
                tx.insert(centreDaycare)
                tx.insert(familyGroup)
                tx.insert(groupFamilyGroup)
                tx.insert(centreGroup)

                tx.insert(childInFamily, DevPersonType.CHILD)
                tx.insert(childInGroupFamily, DevPersonType.CHILD)
                tx.insert(childInCentre, DevPersonType.CHILD)
                tx.insert(parentFamily, DevPersonType.ADULT)
                tx.insert(parentGroupFamily, DevPersonType.ADULT)
                tx.insert(parentCentre, DevPersonType.ADULT)

                tx.insertGuardian(guardianId = parentFamily.id, childId = childInFamily.id)
                tx.insertGuardian(
                    guardianId = parentGroupFamily.id,
                    childId = childInGroupFamily.id,
                )
                tx.insertGuardian(guardianId = parentCentre.id, childId = childInCentre.id)

                val placementFamily =
                    tx.insert(
                        DevPlacement(
                            childId = childInFamily.id,
                            unitId = familyDaycare.id,
                            startDate = today.minusDays(10),
                            endDate = today.plusDays(30),
                        )
                    )
                val placementGroupFamily =
                    tx.insert(
                        DevPlacement(
                            childId = childInGroupFamily.id,
                            unitId = groupFamilyDaycare.id,
                            startDate = today.minusDays(10),
                            endDate = today.plusDays(30),
                        )
                    )
                val placementCentre =
                    tx.insert(
                        DevPlacement(
                            childId = childInCentre.id,
                            unitId = centreDaycare.id,
                            startDate = today.minusDays(10),
                            endDate = today.plusDays(30),
                        )
                    )

                tx.insert(
                    DevDaycareGroupPlacement(
                        daycareGroupId = familyGroup.id,
                        daycarePlacementId = placementFamily,
                        startDate = today.minusDays(10),
                        endDate = today.plusDays(30),
                    )
                )
                tx.insert(
                    DevDaycareGroupPlacement(
                        daycareGroupId = groupFamilyGroup.id,
                        daycarePlacementId = placementGroupFamily,
                        startDate = today.minusDays(10),
                        endDate = today.plusDays(30),
                    )
                )
                tx.insert(
                    DevDaycareGroupPlacement(
                        daycareGroupId = centreGroup.id,
                        daycarePlacementId = placementCentre,
                        startDate = today.minusDays(10),
                        endDate = today.plusDays(30),
                    )
                )

                tx.insertDaycareAclRow(familyDaycare.id, employee1.id, UserRole.UNIT_SUPERVISOR)
                tx.insertDaycareAclRow(
                    groupFamilyDaycare.id,
                    employee1.id,
                    UserRole.UNIT_SUPERVISOR,
                )
                tx.insertDaycareAclRow(centreDaycare.id, employee1.id, UserRole.UNIT_SUPERVISOR)
                tx.upsertEmployeeMessageAccount(employee1.id)
            }

        val parentFamilyAccountId = db.read { tx -> tx.getCitizenMessageAccount(parentFamily.id) }
        val parentGroupFamilyAccountId =
            db.read { tx -> tx.getCitizenMessageAccount(parentGroupFamily.id) }

        val filter = MessageController.PostMessageFilters(familyDaycare = true)
        val accounts =
            db.read { tx ->
                tx.getMessageAccountsForRecipients(
                    senderAccount = tx.getSenderAccount(employeeAccountId),
                    recipients = setOf(MessageRecipient.Area(area.id)),
                    filters = filter,
                    date = today,
                )
            }

        assertEquals(2, accounts.size)
        assertTrue(accounts.any { it.first == parentFamilyAccountId })
        assertTrue(accounts.any { it.first == parentGroupFamilyAccountId })
    }

    @Test
    fun `getMessageAccountsForRecipients with combination of filters applies all filters with AND logic`() {
        val today = LocalDate.of(2021, 5, 1)
        val area = DevCareArea()
        val daycare =
            DevDaycare(areaId = area.id, enabledPilotFeatures = setOf(PilotFeature.MESSAGING))
        val group = DevDaycareGroup(daycareId = daycare.id)

        val child2019WithShiftCare = DevPerson(dateOfBirth = LocalDate.of(2019, 3, 15))
        val child2019NoShiftCare = DevPerson(dateOfBirth = LocalDate.of(2019, 6, 20))
        val child2020WithShiftCare = DevPerson(dateOfBirth = LocalDate.of(2020, 1, 10))
        val parent2019WithShiftCare = DevPerson()
        val parent2019NoShiftCare = DevPerson()
        val parent2020WithShiftCare = DevPerson()

        val employeeAccountId =
            db.transaction { tx ->
                tx.insert(area)
                tx.insert(daycare)
                tx.insert(group)
                tx.insert(snDefaultDaycare)

                tx.insert(child2019WithShiftCare, DevPersonType.CHILD)
                tx.insert(child2019NoShiftCare, DevPersonType.CHILD)
                tx.insert(child2020WithShiftCare, DevPersonType.CHILD)
                tx.insert(parent2019WithShiftCare, DevPersonType.ADULT)
                tx.insert(parent2019NoShiftCare, DevPersonType.ADULT)
                tx.insert(parent2020WithShiftCare, DevPersonType.ADULT)

                tx.insertGuardian(
                    guardianId = parent2019WithShiftCare.id,
                    childId = child2019WithShiftCare.id,
                )
                tx.insertGuardian(
                    guardianId = parent2019NoShiftCare.id,
                    childId = child2019NoShiftCare.id,
                )
                tx.insertGuardian(
                    guardianId = parent2020WithShiftCare.id,
                    childId = child2020WithShiftCare.id,
                )

                val placement2019Shift =
                    tx.insert(
                        DevPlacement(
                            childId = child2019WithShiftCare.id,
                            unitId = daycare.id,
                            startDate = today.minusDays(10),
                            endDate = today.plusDays(30),
                        )
                    )
                val placement2019NoShift =
                    tx.insert(
                        DevPlacement(
                            childId = child2019NoShiftCare.id,
                            unitId = daycare.id,
                            startDate = today.minusDays(10),
                            endDate = today.plusDays(30),
                        )
                    )
                val placement2020Shift =
                    tx.insert(
                        DevPlacement(
                            childId = child2020WithShiftCare.id,
                            unitId = daycare.id,
                            startDate = today.minusDays(10),
                            endDate = today.plusDays(30),
                        )
                    )

                listOf(placement2019Shift, placement2019NoShift, placement2020Shift).forEach {
                    placement ->
                    tx.insert(
                        DevDaycareGroupPlacement(
                            daycareGroupId = group.id,
                            daycarePlacementId = placement,
                            startDate = today.minusDays(10),
                            endDate = today.plusDays(30),
                        )
                    )
                }

                tx.insert(
                    DevServiceNeed(
                        placementId = placement2019Shift,
                        startDate = today.minusDays(10),
                        endDate = today.plusDays(30),
                        optionId = snDefaultDaycare.id,
                        shiftCare = ShiftCareType.FULL,
                        confirmedBy = EvakaUserId(employee1.id.raw),
                    )
                )
                tx.insert(
                    DevServiceNeed(
                        placementId = placement2019NoShift,
                        startDate = today.minusDays(10),
                        endDate = today.plusDays(30),
                        optionId = snDefaultDaycare.id,
                        shiftCare = ShiftCareType.NONE,
                        confirmedBy = EvakaUserId(employee1.id.raw),
                    )
                )
                tx.insert(
                    DevServiceNeed(
                        placementId = placement2020Shift,
                        startDate = today.minusDays(10),
                        endDate = today.plusDays(30),
                        optionId = snDefaultDaycare.id,
                        shiftCare = ShiftCareType.FULL,
                        confirmedBy = EvakaUserId(employee1.id.raw),
                    )
                )

                tx.insertDaycareAclRow(daycare.id, employee1.id, UserRole.UNIT_SUPERVISOR)
                tx.upsertEmployeeMessageAccount(employee1.id)
            }

        val parent2019WithShiftCareAccountId =
            db.read { tx -> tx.getCitizenMessageAccount(parent2019WithShiftCare.id) }

        val filter =
            MessageController.PostMessageFilters(yearsOfBirth = listOf(2019), shiftCare = true)
        val accounts =
            db.read { tx ->
                tx.getMessageAccountsForRecipients(
                    senderAccount = tx.getSenderAccount(employeeAccountId),
                    recipients = setOf(MessageRecipient.Area(area.id)),
                    filters = filter,
                    date = today,
                )
            }

        assertEquals(1, accounts.size)
        assertTrue(accounts.any { it.first == parent2019WithShiftCareAccountId })
    }

    @Test
    fun `getMessageAccountsForRecipients with shift care filter excludes children without service need`() {
        val today = LocalDate.of(2021, 5, 1)
        val area = DevCareArea()
        val daycare =
            DevDaycare(areaId = area.id, enabledPilotFeatures = setOf(PilotFeature.MESSAGING))
        val group = DevDaycareGroup(daycareId = daycare.id)

        val childWithServiceNeed = DevPerson()
        val childWithoutServiceNeed = DevPerson()
        val parentWithServiceNeed = DevPerson()
        val parentWithoutServiceNeed = DevPerson()

        val employeeAccountId =
            db.transaction { tx ->
                tx.insert(area)
                tx.insert(daycare)
                tx.insert(group)
                tx.insert(snDefaultDaycare)

                tx.insert(childWithServiceNeed, DevPersonType.CHILD)
                tx.insert(childWithoutServiceNeed, DevPersonType.CHILD)
                tx.insert(parentWithServiceNeed, DevPersonType.ADULT)
                tx.insert(parentWithoutServiceNeed, DevPersonType.ADULT)

                tx.insertGuardian(
                    guardianId = parentWithServiceNeed.id,
                    childId = childWithServiceNeed.id,
                )
                tx.insertGuardian(
                    guardianId = parentWithoutServiceNeed.id,
                    childId = childWithoutServiceNeed.id,
                )

                val placementWith =
                    tx.insert(
                        DevPlacement(
                            childId = childWithServiceNeed.id,
                            unitId = daycare.id,
                            startDate = today.minusDays(10),
                            endDate = today.plusDays(30),
                        )
                    )
                val placementWithout =
                    tx.insert(
                        DevPlacement(
                            childId = childWithoutServiceNeed.id,
                            unitId = daycare.id,
                            startDate = today.minusDays(10),
                            endDate = today.plusDays(30),
                        )
                    )

                tx.insert(
                    DevDaycareGroupPlacement(
                        daycareGroupId = group.id,
                        daycarePlacementId = placementWith,
                        startDate = today.minusDays(10),
                        endDate = today.plusDays(30),
                    )
                )
                tx.insert(
                    DevDaycareGroupPlacement(
                        daycareGroupId = group.id,
                        daycarePlacementId = placementWithout,
                        startDate = today.minusDays(10),
                        endDate = today.plusDays(30),
                    )
                )

                tx.insert(
                    DevServiceNeed(
                        placementId = placementWith,
                        startDate = today.minusDays(10),
                        endDate = today.plusDays(30),
                        optionId = snDefaultDaycare.id,
                        shiftCare = ShiftCareType.FULL,
                        confirmedBy = EvakaUserId(employee1.id.raw),
                    )
                )

                tx.insertDaycareAclRow(daycare.id, employee1.id, UserRole.UNIT_SUPERVISOR)
                tx.upsertEmployeeMessageAccount(employee1.id)
            }

        val parentWithServiceNeedAccountId =
            db.read { tx -> tx.getCitizenMessageAccount(parentWithServiceNeed.id) }

        val filter = MessageController.PostMessageFilters(shiftCare = true)
        val accounts =
            db.read { tx ->
                tx.getMessageAccountsForRecipients(
                    senderAccount = tx.getSenderAccount(employeeAccountId),
                    recipients = setOf(MessageRecipient.Area(area.id)),
                    filters = filter,
                    date = today,
                )
            }

        assertEquals(1, accounts.size)
        assertTrue(accounts.any { it.first == parentWithServiceNeedAccountId })
    }

    @Test
    fun `getMessageAccountsForRecipients with shift care filter excludes children with service need not overlapping reference date`() {
        val today = LocalDate.of(2021, 5, 1)
        val area = DevCareArea()
        val daycare =
            DevDaycare(areaId = area.id, enabledPilotFeatures = setOf(PilotFeature.MESSAGING))
        val group = DevDaycareGroup(daycareId = daycare.id)

        val childWithOverlappingServiceNeed = DevPerson()
        val childWithPastServiceNeed = DevPerson()
        val childWithFutureServiceNeed = DevPerson()
        val parentOverlapping = DevPerson()
        val parentPast = DevPerson()
        val parentFuture = DevPerson()

        val employeeAccountId =
            db.transaction { tx ->
                tx.insert(area)
                tx.insert(daycare)
                tx.insert(group)
                tx.insert(snDefaultDaycare)

                tx.insert(childWithOverlappingServiceNeed, DevPersonType.CHILD)
                tx.insert(childWithPastServiceNeed, DevPersonType.CHILD)
                tx.insert(childWithFutureServiceNeed, DevPersonType.CHILD)
                tx.insert(parentOverlapping, DevPersonType.ADULT)
                tx.insert(parentPast, DevPersonType.ADULT)
                tx.insert(parentFuture, DevPersonType.ADULT)

                tx.insertGuardian(
                    guardianId = parentOverlapping.id,
                    childId = childWithOverlappingServiceNeed.id,
                )
                tx.insertGuardian(guardianId = parentPast.id, childId = childWithPastServiceNeed.id)
                tx.insertGuardian(
                    guardianId = parentFuture.id,
                    childId = childWithFutureServiceNeed.id,
                )

                val placementOverlapping =
                    tx.insert(
                        DevPlacement(
                            childId = childWithOverlappingServiceNeed.id,
                            unitId = daycare.id,
                            startDate = today.minusDays(30),
                            endDate = today.plusDays(30),
                        )
                    )
                val placementPast =
                    tx.insert(
                        DevPlacement(
                            childId = childWithPastServiceNeed.id,
                            unitId = daycare.id,
                            startDate = today.minusDays(30),
                            endDate = today.plusDays(30),
                        )
                    )
                val placementFuture =
                    tx.insert(
                        DevPlacement(
                            childId = childWithFutureServiceNeed.id,
                            unitId = daycare.id,
                            startDate = today.minusDays(30),
                            endDate = today.plusDays(30),
                        )
                    )

                listOf(placementOverlapping, placementPast, placementFuture).forEach { placement ->
                    tx.insert(
                        DevDaycareGroupPlacement(
                            daycareGroupId = group.id,
                            daycarePlacementId = placement,
                            startDate = today.minusDays(30),
                            endDate = today.plusDays(30),
                        )
                    )
                }

                tx.insert(
                    DevServiceNeed(
                        placementId = placementOverlapping,
                        startDate = today.minusDays(10),
                        endDate = today.plusDays(10),
                        optionId = snDefaultDaycare.id,
                        shiftCare = ShiftCareType.FULL,
                        confirmedBy = EvakaUserId(employee1.id.raw),
                    )
                )
                tx.insert(
                    DevServiceNeed(
                        placementId = placementPast,
                        startDate = today.minusDays(30),
                        endDate = today.minusDays(1),
                        optionId = snDefaultDaycare.id,
                        shiftCare = ShiftCareType.FULL,
                        confirmedBy = EvakaUserId(employee1.id.raw),
                    )
                )
                tx.insert(
                    DevServiceNeed(
                        placementId = placementFuture,
                        startDate = today.plusDays(1),
                        endDate = today.plusDays(30),
                        optionId = snDefaultDaycare.id,
                        shiftCare = ShiftCareType.FULL,
                        confirmedBy = EvakaUserId(employee1.id.raw),
                    )
                )

                tx.insertDaycareAclRow(daycare.id, employee1.id, UserRole.UNIT_SUPERVISOR)
                tx.upsertEmployeeMessageAccount(employee1.id)
            }

        val parentOverlappingAccountId =
            db.read { tx -> tx.getCitizenMessageAccount(parentOverlapping.id) }

        val filter = MessageController.PostMessageFilters(shiftCare = true)
        val accounts =
            db.read { tx ->
                tx.getMessageAccountsForRecipients(
                    senderAccount = tx.getSenderAccount(employeeAccountId),
                    recipients = setOf(MessageRecipient.Area(area.id)),
                    filters = filter,
                    date = today,
                )
            }

        assertEquals(1, accounts.size)
        assertTrue(accounts.any { it.first == parentOverlappingAccountId })
    }

    @Test
    fun `getMessageAccountsForRecipients with shift care filter uses only service need overlapping query date when multiple exist`() {
        val today = LocalDate.of(2021, 5, 1)
        val area = DevCareArea()
        val daycare =
            DevDaycare(areaId = area.id, enabledPilotFeatures = setOf(PilotFeature.MESSAGING))
        val group = DevDaycareGroup(daycareId = daycare.id)

        val child = DevPerson()
        val parent = DevPerson()

        val employeeAccountId =
            db.transaction { tx ->
                tx.insert(area)
                tx.insert(daycare)
                tx.insert(group)
                tx.insert(snDefaultDaycare)

                tx.insert(child, DevPersonType.CHILD)
                tx.insert(parent, DevPersonType.ADULT)

                tx.insertGuardian(guardianId = parent.id, childId = child.id)

                val placement =
                    tx.insert(
                        DevPlacement(
                            childId = child.id,
                            unitId = daycare.id,
                            startDate = today.minusDays(60),
                            endDate = today.plusDays(60),
                        )
                    )

                tx.insert(
                    DevDaycareGroupPlacement(
                        daycareGroupId = group.id,
                        daycarePlacementId = placement,
                        startDate = today.minusDays(60),
                        endDate = today.plusDays(60),
                    )
                )

                tx.insert(
                    DevServiceNeed(
                        placementId = placement,
                        startDate = today.minusDays(60),
                        endDate = today.minusDays(10),
                        optionId = snDefaultDaycare.id,
                        shiftCare = ShiftCareType.NONE,
                        confirmedBy = EvakaUserId(employee1.id.raw),
                    )
                )
                tx.insert(
                    DevServiceNeed(
                        placementId = placement,
                        startDate = today.minusDays(9),
                        endDate = today.plusDays(10),
                        optionId = snDefaultDaycare.id,
                        shiftCare = ShiftCareType.FULL,
                        confirmedBy = EvakaUserId(employee1.id.raw),
                    )
                )
                tx.insert(
                    DevServiceNeed(
                        placementId = placement,
                        startDate = today.plusDays(11),
                        endDate = today.plusDays(60),
                        optionId = snDefaultDaycare.id,
                        shiftCare = ShiftCareType.NONE,
                        confirmedBy = EvakaUserId(employee1.id.raw),
                    )
                )

                tx.insertDaycareAclRow(daycare.id, employee1.id, UserRole.UNIT_SUPERVISOR)
                tx.upsertEmployeeMessageAccount(employee1.id)
            }

        val parentAccountId = db.read { tx -> tx.getCitizenMessageAccount(parent.id) }

        val filter = MessageController.PostMessageFilters(shiftCare = true)
        val accounts =
            db.read { tx ->
                tx.getMessageAccountsForRecipients(
                    senderAccount = tx.getSenderAccount(employeeAccountId),
                    recipients = setOf(MessageRecipient.Area(area.id)),
                    filters = filter,
                    date = today,
                )
            }

        assertEquals(1, accounts.size)
        assertTrue(accounts.any { it.first == parentAccountId })
    }

    @Test
    fun `getMessageAccountsForRecipients with PERSONAL account sender can access all children in their unit via employee_child_daycare_acl`() {
        val today = LocalDate.of(2021, 5, 1)
        val area = DevCareArea()
        val daycare =
            DevDaycare(areaId = area.id, enabledPilotFeatures = setOf(PilotFeature.MESSAGING))
        val group1 = DevDaycareGroup(daycareId = daycare.id, name = "Group 1")
        val group2 = DevDaycareGroup(daycareId = daycare.id, name = "Group 2")

        val childInGroup1 = DevPerson()
        val childInGroup2 = DevPerson()
        val parentGroup1 = DevPerson()
        val parentGroup2 = DevPerson()

        val supervisor = DevEmployee(firstName = "Unit", lastName = "Supervisor")

        val employeeAccountId =
            db.transaction { tx ->
                tx.insert(supervisor)
                tx.insert(area)
                tx.insert(daycare)
                tx.insert(group1)
                tx.insert(group2)

                tx.insert(childInGroup1, DevPersonType.CHILD)
                tx.insert(childInGroup2, DevPersonType.CHILD)
                tx.insert(parentGroup1, DevPersonType.ADULT)
                tx.insert(parentGroup2, DevPersonType.ADULT)

                tx.insertGuardian(guardianId = parentGroup1.id, childId = childInGroup1.id)
                tx.insertGuardian(guardianId = parentGroup2.id, childId = childInGroup2.id)

                val placement1 =
                    tx.insert(
                        DevPlacement(
                            childId = childInGroup1.id,
                            unitId = daycare.id,
                            startDate = today.minusDays(10),
                            endDate = today.plusDays(30),
                        )
                    )
                val placement2 =
                    tx.insert(
                        DevPlacement(
                            childId = childInGroup2.id,
                            unitId = daycare.id,
                            startDate = today.minusDays(10),
                            endDate = today.plusDays(30),
                        )
                    )

                tx.insert(
                    DevDaycareGroupPlacement(
                        daycareGroupId = group1.id,
                        daycarePlacementId = placement1,
                        startDate = today.minusDays(10),
                        endDate = today.plusDays(30),
                    )
                )
                tx.insert(
                    DevDaycareGroupPlacement(
                        daycareGroupId = group2.id,
                        daycarePlacementId = placement2,
                        startDate = today.minusDays(10),
                        endDate = today.plusDays(30),
                    )
                )

                tx.insertDaycareAclRow(daycare.id, supervisor.id, UserRole.UNIT_SUPERVISOR)
                tx.upsertEmployeeMessageAccount(supervisor.id)
            }

        val parentGroup1AccountId = db.read { tx -> tx.getCitizenMessageAccount(parentGroup1.id) }
        val parentGroup2AccountId = db.read { tx -> tx.getCitizenMessageAccount(parentGroup2.id) }

        val accounts =
            db.read { tx ->
                tx.getMessageAccountsForRecipients(
                    senderAccount = tx.getSenderAccount(employeeAccountId),
                    recipients = setOf(MessageRecipient.Area(area.id)),
                    filters = null,
                    date = today,
                )
            }

        assertEquals(2, accounts.size)
        assertTrue(accounts.any { it.first == parentGroup1AccountId })
        assertTrue(accounts.any { it.first == parentGroup2AccountId })
    }

    @Test
    fun `getMessageAccountsForRecipients with employee using GROUP account can only access children in that group`() {
        val today = LocalDate.of(2021, 5, 1)
        val area = DevCareArea()
        val daycare =
            DevDaycare(areaId = area.id, enabledPilotFeatures = setOf(PilotFeature.MESSAGING))
        val group1 = DevDaycareGroup(daycareId = daycare.id, name = "Group 1")
        val group2 = DevDaycareGroup(daycareId = daycare.id, name = "Group 2")

        val childInGroup1 = DevPerson()
        val childInGroup2 = DevPerson()
        val parentGroup1 = DevPerson()
        val parentGroup2 = DevPerson()

        val groupAccountId =
            db.transaction { tx ->
                tx.insert(area)
                tx.insert(daycare)
                tx.insert(group1)
                tx.insert(group2)

                tx.insert(childInGroup1, DevPersonType.CHILD)
                tx.insert(childInGroup2, DevPersonType.CHILD)
                tx.insert(parentGroup1, DevPersonType.ADULT)
                tx.insert(parentGroup2, DevPersonType.ADULT)

                tx.insertGuardian(guardianId = parentGroup1.id, childId = childInGroup1.id)
                tx.insertGuardian(guardianId = parentGroup2.id, childId = childInGroup2.id)

                val placement1 =
                    tx.insert(
                        DevPlacement(
                            childId = childInGroup1.id,
                            unitId = daycare.id,
                            startDate = today.minusDays(10),
                            endDate = today.plusDays(30),
                        )
                    )
                val placement2 =
                    tx.insert(
                        DevPlacement(
                            childId = childInGroup2.id,
                            unitId = daycare.id,
                            startDate = today.minusDays(10),
                            endDate = today.plusDays(30),
                        )
                    )

                tx.insert(
                    DevDaycareGroupPlacement(
                        daycareGroupId = group1.id,
                        daycarePlacementId = placement1,
                        startDate = today.minusDays(10),
                        endDate = today.plusDays(30),
                    )
                )
                tx.insert(
                    DevDaycareGroupPlacement(
                        daycareGroupId = group2.id,
                        daycarePlacementId = placement2,
                        startDate = today.minusDays(10),
                        endDate = today.plusDays(30),
                    )
                )

                tx.createQuery {
                        sql(
                            """
                        INSERT INTO message_account (daycare_group_id, type)
                        VALUES (${bind(group1.id)}, 'GROUP')
                        RETURNING id
                        """
                        )
                    }
                    .exactlyOne<MessageAccountId>()
            }

        val parentGroup1AccountId = db.read { tx -> tx.getCitizenMessageAccount(parentGroup1.id) }

        val accounts =
            db.read { tx ->
                tx.getMessageAccountsForRecipients(
                    senderAccount = tx.getSenderAccount(groupAccountId),
                    recipients = setOf(MessageRecipient.Area(area.id)),
                    filters = null,
                    date = today,
                )
            }

        assertEquals(1, accounts.size)
        assertTrue(accounts.any { it.first == parentGroup1AccountId })
    }

    @Test
    fun `getMessageAccountsForRecipients with MUNICIPAL account bypasses ACL and can access all children in area`() {
        val today = LocalDate.of(2021, 5, 1)
        val area = DevCareArea()
        val daycare =
            DevDaycare(areaId = area.id, enabledPilotFeatures = setOf(PilotFeature.MESSAGING))
        val group = DevDaycareGroup(daycareId = daycare.id)

        val child1 = DevPerson()
        val child2 = DevPerson()
        val parent1 = DevPerson()
        val parent2 = DevPerson()

        val municipalEmployee = DevEmployee(firstName = "Municipal", lastName = "Employee")

        val municipalAccountId =
            db.transaction { tx ->
                tx.insert(municipalEmployee)
                tx.insert(area)
                tx.insert(daycare)
                tx.insert(group)

                tx.insert(child1, DevPersonType.CHILD)
                tx.insert(child2, DevPersonType.CHILD)
                tx.insert(parent1, DevPersonType.ADULT)
                tx.insert(parent2, DevPersonType.ADULT)

                tx.insertGuardian(guardianId = parent1.id, childId = child1.id)
                tx.insertGuardian(guardianId = parent2.id, childId = child2.id)

                val placement1 =
                    tx.insert(
                        DevPlacement(
                            childId = child1.id,
                            unitId = daycare.id,
                            startDate = today.minusDays(10),
                            endDate = today.plusDays(30),
                        )
                    )
                val placement2 =
                    tx.insert(
                        DevPlacement(
                            childId = child2.id,
                            unitId = daycare.id,
                            startDate = today.minusDays(10),
                            endDate = today.plusDays(30),
                        )
                    )

                tx.insert(
                    DevDaycareGroupPlacement(
                        daycareGroupId = group.id,
                        daycarePlacementId = placement1,
                        startDate = today.minusDays(10),
                        endDate = today.plusDays(30),
                    )
                )
                tx.insert(
                    DevDaycareGroupPlacement(
                        daycareGroupId = group.id,
                        daycarePlacementId = placement2,
                        startDate = today.minusDays(10),
                        endDate = today.plusDays(30),
                    )
                )

                tx.createMunicipalMessageAccount()
            }

        val parent1AccountId = db.read { tx -> tx.getCitizenMessageAccount(parent1.id) }
        val parent2AccountId = db.read { tx -> tx.getCitizenMessageAccount(parent2.id) }

        val accounts =
            db.read { tx ->
                tx.getMessageAccountsForRecipients(
                    senderAccount = tx.getSenderAccount(municipalAccountId),
                    recipients = setOf(MessageRecipient.Area(area.id)),
                    filters = null,
                    date = today,
                )
            }

        assertEquals(2, accounts.size)
        assertTrue(accounts.any { it.first == parent1AccountId })
        assertTrue(accounts.any { it.first == parent2AccountId })
    }

    @Test
    fun `getMessageAccountsForRecipients with child with multiple guardians returns all guardian accounts`() {
        val today = LocalDate.of(2021, 5, 1)
        val area = DevCareArea()
        val daycare =
            DevDaycare(areaId = area.id, enabledPilotFeatures = setOf(PilotFeature.MESSAGING))
        val group = DevDaycareGroup(daycareId = daycare.id)

        val child = DevPerson()
        val guardian1 = DevPerson()
        val guardian2 = DevPerson()
        val guardian3 = DevPerson()

        val employeeAccountId =
            db.transaction { tx ->
                tx.insert(area)
                tx.insert(daycare)
                tx.insert(group)

                tx.insert(child, DevPersonType.CHILD)
                tx.insert(guardian1, DevPersonType.ADULT)
                tx.insert(guardian2, DevPersonType.ADULT)
                tx.insert(guardian3, DevPersonType.ADULT)

                tx.insertGuardian(guardianId = guardian1.id, childId = child.id)
                tx.insertGuardian(guardianId = guardian2.id, childId = child.id)
                tx.insertGuardian(guardianId = guardian3.id, childId = child.id)

                val placement =
                    tx.insert(
                        DevPlacement(
                            childId = child.id,
                            unitId = daycare.id,
                            startDate = today.minusDays(10),
                            endDate = today.plusDays(30),
                        )
                    )

                tx.insert(
                    DevDaycareGroupPlacement(
                        daycareGroupId = group.id,
                        daycarePlacementId = placement,
                        startDate = today.minusDays(10),
                        endDate = today.plusDays(30),
                    )
                )

                tx.insertDaycareAclRow(daycare.id, employee1.id, UserRole.UNIT_SUPERVISOR)
                tx.upsertEmployeeMessageAccount(employee1.id)
            }

        val guardian1AccountId = db.read { tx -> tx.getCitizenMessageAccount(guardian1.id) }
        val guardian2AccountId = db.read { tx -> tx.getCitizenMessageAccount(guardian2.id) }
        val guardian3AccountId = db.read { tx -> tx.getCitizenMessageAccount(guardian3.id) }

        val accounts =
            db.read { tx ->
                tx.getMessageAccountsForRecipients(
                    senderAccount = tx.getSenderAccount(employeeAccountId),
                    recipients = setOf(MessageRecipient.Child(child.id)),
                    filters = null,
                    date = today,
                )
            }

        assertEquals(3, accounts.size)
        assertTrue(
            accounts.any { it.first == guardian1AccountId && it.second == child.id },
            "Guardian 1 account should be returned with correct child ID",
        )
        assertTrue(
            accounts.any { it.first == guardian2AccountId && it.second == child.id },
            "Guardian 2 account should be returned with correct child ID",
        )
        assertTrue(
            accounts.any { it.first == guardian3AccountId && it.second == child.id },
            "Guardian 3 account should be returned with correct child ID",
        )
    }

    @Test
    fun `getMessageAccountsForRecipients with child with both guardian and foster parent returns both account types`() {
        val today = LocalDate.of(2021, 5, 1)
        val area = DevCareArea()
        val daycare =
            DevDaycare(areaId = area.id, enabledPilotFeatures = setOf(PilotFeature.MESSAGING))
        val group = DevDaycareGroup(daycareId = daycare.id)

        val child = DevPerson()
        val guardian = DevPerson()
        val fosterParent = DevPerson()

        val employeeAccountId =
            db.transaction { tx ->
                tx.insert(area)
                tx.insert(daycare)
                tx.insert(group)

                tx.insert(child, DevPersonType.CHILD)
                tx.insert(guardian, DevPersonType.ADULT)
                tx.insert(fosterParent, DevPersonType.ADULT)

                tx.insertGuardian(guardianId = guardian.id, childId = child.id)
                tx.insert(
                    DevFosterParent(
                        childId = child.id,
                        parentId = fosterParent.id,
                        validDuring = DateRange(today.minusDays(30), today.plusDays(30)),
                        modifiedAt = HelsinkiDateTime.now(),
                        modifiedBy = EvakaUserId(employee1.id.raw),
                    )
                )

                val placement =
                    tx.insert(
                        DevPlacement(
                            childId = child.id,
                            unitId = daycare.id,
                            startDate = today.minusDays(10),
                            endDate = today.plusDays(30),
                        )
                    )

                tx.insert(
                    DevDaycareGroupPlacement(
                        daycareGroupId = group.id,
                        daycarePlacementId = placement,
                        startDate = today.minusDays(10),
                        endDate = today.plusDays(30),
                    )
                )

                tx.insertDaycareAclRow(daycare.id, employee1.id, UserRole.UNIT_SUPERVISOR)
                tx.upsertEmployeeMessageAccount(employee1.id)
            }

        val guardianAccountId = db.read { tx -> tx.getCitizenMessageAccount(guardian.id) }
        val fosterParentAccountId = db.read { tx -> tx.getCitizenMessageAccount(fosterParent.id) }

        val accounts =
            db.read { tx ->
                tx.getMessageAccountsForRecipients(
                    senderAccount = tx.getSenderAccount(employeeAccountId),
                    recipients = setOf(MessageRecipient.Child(child.id)),
                    filters = null,
                    date = today,
                )
            }

        assertEquals(2, accounts.size)
        assertTrue(
            accounts.any { it.first == guardianAccountId && it.second == child.id },
            "Guardian account should be returned with correct child ID",
        )
        assertTrue(
            accounts.any { it.first == fosterParentAccountId && it.second == child.id },
            "Foster parent account should be returned with correct child ID",
        )
    }

    @Test
    fun `getMessageAccountsForRecipients with guardian of multiple children returns account multiple times with different child IDs`() {
        val today = LocalDate.of(2021, 5, 1)
        val area = DevCareArea()
        val daycare =
            DevDaycare(areaId = area.id, enabledPilotFeatures = setOf(PilotFeature.MESSAGING))
        val group = DevDaycareGroup(daycareId = daycare.id)

        val child1 = DevPerson()
        val child2 = DevPerson()
        val sharedGuardian = DevPerson()

        val employeeAccountId =
            db.transaction { tx ->
                tx.insert(area)
                tx.insert(daycare)
                tx.insert(group)

                tx.insert(child1, DevPersonType.CHILD)
                tx.insert(child2, DevPersonType.CHILD)
                tx.insert(sharedGuardian, DevPersonType.ADULT)

                tx.insertGuardian(guardianId = sharedGuardian.id, childId = child1.id)
                tx.insertGuardian(guardianId = sharedGuardian.id, childId = child2.id)

                val placement1 =
                    tx.insert(
                        DevPlacement(
                            childId = child1.id,
                            unitId = daycare.id,
                            startDate = today.minusDays(10),
                            endDate = today.plusDays(30),
                        )
                    )
                val placement2 =
                    tx.insert(
                        DevPlacement(
                            childId = child2.id,
                            unitId = daycare.id,
                            startDate = today.minusDays(10),
                            endDate = today.plusDays(30),
                        )
                    )

                tx.insert(
                    DevDaycareGroupPlacement(
                        daycareGroupId = group.id,
                        daycarePlacementId = placement1,
                        startDate = today.minusDays(10),
                        endDate = today.plusDays(30),
                    )
                )
                tx.insert(
                    DevDaycareGroupPlacement(
                        daycareGroupId = group.id,
                        daycarePlacementId = placement2,
                        startDate = today.minusDays(10),
                        endDate = today.plusDays(30),
                    )
                )

                tx.insertDaycareAclRow(daycare.id, employee1.id, UserRole.UNIT_SUPERVISOR)
                tx.upsertEmployeeMessageAccount(employee1.id)
            }

        val sharedGuardianAccountId =
            db.read { tx -> tx.getCitizenMessageAccount(sharedGuardian.id) }

        val accounts =
            db.read { tx ->
                tx.getMessageAccountsForRecipients(
                    senderAccount = tx.getSenderAccount(employeeAccountId),
                    recipients = setOf(MessageRecipient.Area(area.id)),
                    filters = null,
                    date = today,
                )
            }

        assertEquals(2, accounts.size)
        assertTrue(
            accounts.any { it.first == sharedGuardianAccountId && it.second == child1.id },
            "Shared guardian account should be returned with child1 ID",
        )
        assertTrue(
            accounts.any { it.first == sharedGuardianAccountId && it.second == child2.id },
            "Shared guardian account should be returned with child2 ID",
        )
    }

    @Test
    fun `getMessageAccountsForRecipients includes children in backup care via realized_placement_all function`() {
        val today = LocalDate.of(2021, 5, 1)
        val area = DevCareArea()
        val primaryDaycare =
            DevDaycare(areaId = area.id, enabledPilotFeatures = setOf(PilotFeature.MESSAGING))
        val backupDaycare =
            DevDaycare(areaId = area.id, enabledPilotFeatures = setOf(PilotFeature.MESSAGING))

        val primaryGroup = DevDaycareGroup(daycareId = primaryDaycare.id)
        val backupGroup = DevDaycareGroup(daycareId = backupDaycare.id)

        val child = DevPerson()
        val parent = DevPerson()

        val employeeAccountId =
            db.transaction { tx ->
                tx.insert(area)
                tx.insert(primaryDaycare)
                tx.insert(backupDaycare)
                tx.insert(primaryGroup)
                tx.insert(backupGroup)

                tx.insert(child, DevPersonType.CHILD)
                tx.insert(parent, DevPersonType.ADULT)

                tx.insertGuardian(guardianId = parent.id, childId = child.id)

                val primaryPlacement =
                    tx.insert(
                        DevPlacement(
                            childId = child.id,
                            unitId = primaryDaycare.id,
                            startDate = today.minusDays(30),
                            endDate = today.plusDays(30),
                        )
                    )

                tx.insert(
                    DevDaycareGroupPlacement(
                        daycareGroupId = primaryGroup.id,
                        daycarePlacementId = primaryPlacement,
                        startDate = today.minusDays(30),
                        endDate = today.plusDays(30),
                    )
                )

                tx.insert(
                    DevBackupCare(
                        childId = child.id,
                        unitId = backupDaycare.id,
                        groupId = backupGroup.id,
                        period = FiniteDateRange(today.minusDays(5), today.plusDays(5)),
                    )
                )

                tx.insertDaycareAclRow(primaryDaycare.id, employee1.id, UserRole.UNIT_SUPERVISOR)
                tx.insertDaycareAclRow(backupDaycare.id, employee1.id, UserRole.UNIT_SUPERVISOR)
                tx.upsertEmployeeMessageAccount(employee1.id)
            }

        val parentAccountId = db.read { tx -> tx.getCitizenMessageAccount(parent.id) }

        val accountsViaBackupUnit =
            db.read { tx ->
                tx.getMessageAccountsForRecipients(
                    senderAccount = tx.getSenderAccount(employeeAccountId),
                    recipients = setOf(MessageRecipient.Unit(backupDaycare.id)),
                    filters = null,
                    date = today,
                )
            }

        assertEquals(
            1,
            accountsViaBackupUnit.size,
            "Child in backup care should be included when querying backup unit",
        )
        assertTrue(accountsViaBackupUnit.any { it.first == parentAccountId })

        val accountsViaBackupGroup =
            db.read { tx ->
                tx.getMessageAccountsForRecipients(
                    senderAccount = tx.getSenderAccount(employeeAccountId),
                    recipients = setOf(MessageRecipient.Group(backupGroup.id)),
                    filters = null,
                    date = today,
                )
            }

        assertEquals(
            1,
            accountsViaBackupGroup.size,
            "Child in backup care should be included when querying backup group",
        )
        assertTrue(accountsViaBackupGroup.any { it.first == parentAccountId })
    }

    @Test
    fun `getMessageAccountsForRecipients includes placement starting exactly on query date`() {
        val today = LocalDate.of(2021, 5, 1)
        val area = DevCareArea()
        val daycare =
            DevDaycare(areaId = area.id, enabledPilotFeatures = setOf(PilotFeature.MESSAGING))
        val group = DevDaycareGroup(daycareId = daycare.id)

        val child = DevPerson()
        val parent = DevPerson()

        val employeeAccountId =
            db.transaction { tx ->
                tx.insert(area)
                tx.insert(daycare)
                tx.insert(group)

                tx.insert(child, DevPersonType.CHILD)
                tx.insert(parent, DevPersonType.ADULT)

                tx.insertGuardian(guardianId = parent.id, childId = child.id)

                val placement =
                    tx.insert(
                        DevPlacement(
                            childId = child.id,
                            unitId = daycare.id,
                            startDate = today,
                            endDate = today.plusDays(30),
                        )
                    )

                tx.insert(
                    DevDaycareGroupPlacement(
                        daycareGroupId = group.id,
                        daycarePlacementId = placement,
                        startDate = today,
                        endDate = today.plusDays(30),
                    )
                )

                tx.insertDaycareAclRow(daycare.id, employee1.id, UserRole.UNIT_SUPERVISOR)
                tx.upsertEmployeeMessageAccount(employee1.id)
            }

        val parentAccountId = db.read { tx -> tx.getCitizenMessageAccount(parent.id) }

        val accounts =
            db.read { tx ->
                tx.getMessageAccountsForRecipients(
                    senderAccount = tx.getSenderAccount(employeeAccountId),
                    recipients = setOf(MessageRecipient.Unit(daycare.id)),
                    filters = null,
                    date = today,
                )
            }

        assertEquals(
            1,
            accounts.size,
            "Placement starting exactly on query date should be included",
        )
        assertTrue(accounts.any { it.first == parentAccountId })
    }

    @Test
    fun `getMessageAccountsForRecipients includes placement ending exactly on query date`() {
        val today = LocalDate.of(2021, 5, 1)
        val area = DevCareArea()
        val daycare =
            DevDaycare(areaId = area.id, enabledPilotFeatures = setOf(PilotFeature.MESSAGING))
        val group = DevDaycareGroup(daycareId = daycare.id)

        val child = DevPerson()
        val parent = DevPerson()

        val employeeAccountId =
            db.transaction { tx ->
                tx.insert(area)
                tx.insert(daycare)
                tx.insert(group)

                tx.insert(child, DevPersonType.CHILD)
                tx.insert(parent, DevPersonType.ADULT)

                tx.insertGuardian(guardianId = parent.id, childId = child.id)

                val placement =
                    tx.insert(
                        DevPlacement(
                            childId = child.id,
                            unitId = daycare.id,
                            startDate = today.minusDays(30),
                            endDate = today,
                        )
                    )

                tx.insert(
                    DevDaycareGroupPlacement(
                        daycareGroupId = group.id,
                        daycarePlacementId = placement,
                        startDate = today.minusDays(30),
                        endDate = today,
                    )
                )

                tx.insertDaycareAclRow(daycare.id, employee1.id, UserRole.UNIT_SUPERVISOR)
                tx.upsertEmployeeMessageAccount(employee1.id)
            }

        val parentAccountId = db.read { tx -> tx.getCitizenMessageAccount(parent.id) }

        val accounts =
            db.read { tx ->
                tx.getMessageAccountsForRecipients(
                    senderAccount = tx.getSenderAccount(employeeAccountId),
                    recipients = setOf(MessageRecipient.Unit(daycare.id)),
                    filters = null,
                    date = today,
                )
            }

        assertEquals(1, accounts.size, "Placement ending exactly on query date should be included")
        assertTrue(accounts.any { it.first == parentAccountId })
    }

    @Test
    fun `getMessageAccountsForRecipients returns child as starter when gap exists between placements`() {
        val today = LocalDate.of(2021, 5, 1)
        val area = DevCareArea()
        val daycare =
            DevDaycare(areaId = area.id, enabledPilotFeatures = setOf(PilotFeature.MESSAGING))
        val group = DevDaycareGroup(daycareId = daycare.id)

        val child = DevPerson()
        val parent = DevPerson()

        val employeeAccountId =
            db.transaction { tx ->
                tx.insert(area)
                tx.insert(daycare)
                tx.insert(group)

                tx.insert(child, DevPersonType.CHILD)
                tx.insert(parent, DevPersonType.ADULT)

                tx.insertGuardian(guardianId = parent.id, childId = child.id)

                val pastPlacement =
                    tx.insert(
                        DevPlacement(
                            childId = child.id,
                            unitId = daycare.id,
                            startDate = today.minusDays(60),
                            endDate = today.minusDays(10),
                        )
                    )

                tx.insert(
                    DevDaycareGroupPlacement(
                        daycareGroupId = group.id,
                        daycarePlacementId = pastPlacement,
                        startDate = today.minusDays(60),
                        endDate = today.minusDays(10),
                    )
                )

                val futurePlacement =
                    tx.insert(
                        DevPlacement(
                            childId = child.id,
                            unitId = daycare.id,
                            startDate = today.plusDays(10),
                            endDate = today.plusDays(60),
                        )
                    )

                tx.insert(
                    DevDaycareGroupPlacement(
                        daycareGroupId = group.id,
                        daycarePlacementId = futurePlacement,
                        startDate = today.plusDays(10),
                        endDate = today.plusDays(60),
                    )
                )

                tx.insertDaycareAclRow(daycare.id, employee1.id, UserRole.UNIT_SUPERVISOR)
                tx.upsertEmployeeMessageAccount(employee1.id)
            }

        val parentAccountId = db.read { tx -> tx.getCitizenMessageAccount(parent.id) }

        val starterAccounts =
            db.read { tx ->
                tx.getMessageAccountsForRecipients(
                    senderAccount = tx.getSenderAccount(employeeAccountId),
                    recipients = setOf(MessageRecipient.Unit(daycare.id, starter = true)),
                    filters = null,
                    date = today,
                )
            }

        assertEquals(
            1,
            starterAccounts.size,
            "Child with gap between placements should be returned as starter",
        )
        assertTrue(starterAccounts.any { it.first == parentAccountId })
    }

    @Test
    fun `getMessageAccountsForRecipients does not return child as starter when returning to same group if earlier placement ended on or after query date`() {
        val today = LocalDate.of(2021, 5, 1)
        val area = DevCareArea()
        val daycare =
            DevDaycare(areaId = area.id, enabledPilotFeatures = setOf(PilotFeature.MESSAGING))
        val group = DevDaycareGroup(daycareId = daycare.id)

        val returningChild = DevPerson()
        val newChild = DevPerson()
        val returningParent = DevPerson()
        val newParent = DevPerson()

        val employeeAccountId =
            db.transaction { tx ->
                tx.insert(area)
                tx.insert(daycare)
                tx.insert(group)

                tx.insert(returningChild, DevPersonType.CHILD)
                tx.insert(newChild, DevPersonType.CHILD)
                tx.insert(returningParent, DevPersonType.ADULT)
                tx.insert(newParent, DevPersonType.ADULT)

                tx.insertGuardian(guardianId = returningParent.id, childId = returningChild.id)
                tx.insertGuardian(guardianId = newParent.id, childId = newChild.id)

                val returningChildPastPlacement =
                    tx.insert(
                        DevPlacement(
                            childId = returningChild.id,
                            unitId = daycare.id,
                            startDate = today.minusDays(60),
                            endDate = today,
                        )
                    )

                tx.insert(
                    DevDaycareGroupPlacement(
                        daycareGroupId = group.id,
                        daycarePlacementId = returningChildPastPlacement,
                        startDate = today.minusDays(60),
                        endDate = today,
                    )
                )

                val returningChildFuturePlacement =
                    tx.insert(
                        DevPlacement(
                            childId = returningChild.id,
                            unitId = daycare.id,
                            startDate = today.plusDays(1),
                            endDate = today.plusDays(60),
                        )
                    )

                tx.insert(
                    DevDaycareGroupPlacement(
                        daycareGroupId = group.id,
                        daycarePlacementId = returningChildFuturePlacement,
                        startDate = today.plusDays(1),
                        endDate = today.plusDays(60),
                    )
                )

                val newChildFuturePlacement =
                    tx.insert(
                        DevPlacement(
                            childId = newChild.id,
                            unitId = daycare.id,
                            startDate = today.plusDays(1),
                            endDate = today.plusDays(60),
                        )
                    )

                tx.insert(
                    DevDaycareGroupPlacement(
                        daycareGroupId = group.id,
                        daycarePlacementId = newChildFuturePlacement,
                        startDate = today.plusDays(1),
                        endDate = today.plusDays(60),
                    )
                )

                tx.insertDaycareAclRow(daycare.id, employee1.id, UserRole.UNIT_SUPERVISOR)
                tx.upsertEmployeeMessageAccount(employee1.id)
            }

        val newParentAccountId = db.read { tx -> tx.getCitizenMessageAccount(newParent.id) }

        val starterAccounts =
            db.read { tx ->
                tx.getMessageAccountsForRecipients(
                    senderAccount = tx.getSenderAccount(employeeAccountId),
                    recipients = setOf(MessageRecipient.Unit(daycare.id, starter = true)),
                    filters = null,
                    date = today,
                )
            }

        assertEquals(
            1,
            starterAccounts.size,
            "Only new child should be returned as starter, not the returning child",
        )
        assertTrue(
            starterAccounts.any { it.first == newParentAccountId },
            "New child's parent should be in starter list",
        )
    }

    @Test
    fun `getMessageAccountsForRecipients with empty recipients set returns empty list`() {
        val today = LocalDate.of(2021, 5, 1)
        val area = DevCareArea()
        val daycare =
            DevDaycare(areaId = area.id, enabledPilotFeatures = setOf(PilotFeature.MESSAGING))
        val group = DevDaycareGroup(daycareId = daycare.id)

        val child = DevPerson()
        val parent = DevPerson()

        val employeeAccountId =
            db.transaction { tx ->
                tx.insert(area)
                tx.insert(daycare)
                tx.insert(group)

                tx.insert(child, DevPersonType.CHILD)
                tx.insert(parent, DevPersonType.ADULT)

                tx.insertGuardian(guardianId = parent.id, childId = child.id)

                val placement =
                    tx.insert(
                        DevPlacement(
                            childId = child.id,
                            unitId = daycare.id,
                            startDate = today.minusDays(10),
                            endDate = today.plusDays(30),
                        )
                    )

                tx.insert(
                    DevDaycareGroupPlacement(
                        daycareGroupId = group.id,
                        daycarePlacementId = placement,
                        startDate = today.minusDays(10),
                        endDate = today.plusDays(30),
                    )
                )

                tx.insertDaycareAclRow(daycare.id, employee1.id, UserRole.UNIT_SUPERVISOR)
                tx.upsertEmployeeMessageAccount(employee1.id)
            }

        val accounts =
            db.read { tx ->
                tx.getMessageAccountsForRecipients(
                    senderAccount = tx.getSenderAccount(employeeAccountId),
                    recipients = emptySet(),
                    filters = null,
                    date = today,
                )
            }

        assertEquals(0, accounts.size, "Empty recipients set should return empty list")
    }

    @Test
    fun `getMessageAccountsForRecipients with recipients with no matching children returns empty list`() {
        val today = LocalDate.of(2021, 5, 1)
        val emptyArea = DevCareArea(name = "Empty Area", shortName = "empty_area")
        val areaWithDaycare =
            DevCareArea(name = "Area With Daycare", shortName = "area_with_daycare")
        val daycare =
            DevDaycare(
                areaId = areaWithDaycare.id,
                enabledPilotFeatures = setOf(PilotFeature.MESSAGING),
            )

        val employeeAccountId =
            db.transaction { tx ->
                tx.insert(emptyArea)
                tx.insert(areaWithDaycare)
                tx.insert(daycare)

                tx.insertDaycareAclRow(daycare.id, employee1.id, UserRole.UNIT_SUPERVISOR)
                tx.upsertEmployeeMessageAccount(employee1.id)
            }

        val accounts =
            db.read { tx ->
                tx.getMessageAccountsForRecipients(
                    senderAccount = tx.getSenderAccount(employeeAccountId),
                    recipients = setOf(MessageRecipient.Area(emptyArea.id)),
                    filters = null,
                    date = today,
                )
            }

        assertEquals(0, accounts.size, "Area with no daycares/children should return empty list")
    }

    @Test
    fun `getMessageAccountsForRecipients with filters that exclude all children returns empty list`() {
        val today = LocalDate.of(2021, 5, 1)
        val area = DevCareArea()
        val daycare =
            DevDaycare(areaId = area.id, enabledPilotFeatures = setOf(PilotFeature.MESSAGING))
        val group = DevDaycareGroup(daycareId = daycare.id)

        val child2020 = DevPerson(dateOfBirth = LocalDate.of(2020, 1, 10))
        val parent2020 = DevPerson()

        val employeeAccountId =
            db.transaction { tx ->
                tx.insert(area)
                tx.insert(daycare)
                tx.insert(group)

                tx.insert(child2020, DevPersonType.CHILD)
                tx.insert(parent2020, DevPersonType.ADULT)

                tx.insertGuardian(guardianId = parent2020.id, childId = child2020.id)

                val placement =
                    tx.insert(
                        DevPlacement(
                            childId = child2020.id,
                            unitId = daycare.id,
                            startDate = today.minusDays(10),
                            endDate = today.plusDays(30),
                        )
                    )

                tx.insert(
                    DevDaycareGroupPlacement(
                        daycareGroupId = group.id,
                        daycarePlacementId = placement,
                        startDate = today.minusDays(10),
                        endDate = today.plusDays(30),
                    )
                )

                tx.insertDaycareAclRow(daycare.id, employee1.id, UserRole.UNIT_SUPERVISOR)
                tx.upsertEmployeeMessageAccount(employee1.id)
            }

        val filterExcludingAllChildren =
            MessageController.PostMessageFilters(yearsOfBirth = listOf(2018, 2019))
        val accounts =
            db.read { tx ->
                tx.getMessageAccountsForRecipients(
                    senderAccount = tx.getSenderAccount(employeeAccountId),
                    recipients = setOf(MessageRecipient.Area(area.id)),
                    filters = filterExcludingAllChildren,
                    date = today,
                )
            }

        assertEquals(0, accounts.size, "Filter excluding all children should return empty list")
    }

    @Test
    fun `getMessageAccountsForRecipients with all default filter values behaves same as null filters`() {
        val today = LocalDate.of(2021, 5, 1)
        val area = DevCareArea()
        val daycare =
            DevDaycare(areaId = area.id, enabledPilotFeatures = setOf(PilotFeature.MESSAGING))
        val group = DevDaycareGroup(daycareId = daycare.id)

        val child1 = DevPerson()
        val child2 = DevPerson()
        val parent1 = DevPerson()
        val parent2 = DevPerson()

        val employeeAccountId =
            db.transaction { tx ->
                tx.insert(area)
                tx.insert(daycare)
                tx.insert(group)

                tx.insert(child1, DevPersonType.CHILD)
                tx.insert(child2, DevPersonType.CHILD)
                tx.insert(parent1, DevPersonType.ADULT)
                tx.insert(parent2, DevPersonType.ADULT)

                tx.insertGuardian(guardianId = parent1.id, childId = child1.id)
                tx.insertGuardian(guardianId = parent2.id, childId = child2.id)

                listOf(child1, child2).forEach { child ->
                    val placement =
                        tx.insert(
                            DevPlacement(
                                childId = child.id,
                                unitId = daycare.id,
                                startDate = today.minusDays(10),
                                endDate = today.plusDays(30),
                            )
                        )
                    tx.insert(
                        DevDaycareGroupPlacement(
                            daycareGroupId = group.id,
                            daycarePlacementId = placement,
                            startDate = today.minusDays(10),
                            endDate = today.plusDays(30),
                        )
                    )
                }

                tx.insertDaycareAclRow(daycare.id, employee1.id, UserRole.UNIT_SUPERVISOR)
                tx.upsertEmployeeMessageAccount(employee1.id)
            }

        val accountsWithNullFilter =
            db.read { tx ->
                tx.getMessageAccountsForRecipients(
                    senderAccount = tx.getSenderAccount(employeeAccountId),
                    recipients = setOf(MessageRecipient.Area(area.id)),
                    filters = null,
                    date = today,
                )
            }

        val accountsWithDefaultFilter =
            db.read { tx ->
                tx.getMessageAccountsForRecipients(
                    senderAccount = tx.getSenderAccount(employeeAccountId),
                    recipients = setOf(MessageRecipient.Area(area.id)),
                    filters =
                        MessageController.PostMessageFilters(
                            yearsOfBirth = emptyList(),
                            shiftCare = false,
                            intermittentShiftCare = false,
                            familyDaycare = false,
                            placementTypes = emptyList(),
                        ),
                    date = today,
                )
            }

        assertEquals(
            accountsWithNullFilter.size,
            accountsWithDefaultFilter.size,
            "Default filter values should behave same as null filter",
        )
        assertEquals(2, accountsWithNullFilter.size)
        assertEquals(2, accountsWithDefaultFilter.size)
    }

    @Test
    fun `getMessageAccountsForRecipients with multiple recipient types in same query returns accounts from all types`() {
        val today = LocalDate.of(2021, 5, 1)
        val area1 = DevCareArea(name = "Area 1", shortName = "area_1")
        val area2 = DevCareArea(name = "Area 2", shortName = "area_2")
        val daycare1 =
            DevDaycare(areaId = area1.id, enabledPilotFeatures = setOf(PilotFeature.MESSAGING))
        val daycare2 =
            DevDaycare(areaId = area2.id, enabledPilotFeatures = setOf(PilotFeature.MESSAGING))
        val group1 = DevDaycareGroup(daycareId = daycare1.id)
        val group2 = DevDaycareGroup(daycareId = daycare2.id)

        val childInArea1 = DevPerson()
        val childInUnit2 = DevPerson()
        val childInGroup2 = DevPerson()
        val specificChild = DevPerson()
        val parentArea1 = DevPerson()
        val parentUnit2 = DevPerson()
        val parentGroup2 = DevPerson()
        val parentSpecific = DevPerson()

        val employeeAccountId =
            db.transaction { tx ->
                tx.insert(area1)
                tx.insert(area2)
                tx.insert(daycare1)
                tx.insert(daycare2)
                tx.insert(group1)
                tx.insert(group2)

                tx.insert(childInArea1, DevPersonType.CHILD)
                tx.insert(childInUnit2, DevPersonType.CHILD)
                tx.insert(childInGroup2, DevPersonType.CHILD)
                tx.insert(specificChild, DevPersonType.CHILD)
                tx.insert(parentArea1, DevPersonType.ADULT)
                tx.insert(parentUnit2, DevPersonType.ADULT)
                tx.insert(parentGroup2, DevPersonType.ADULT)
                tx.insert(parentSpecific, DevPersonType.ADULT)

                tx.insertGuardian(guardianId = parentArea1.id, childId = childInArea1.id)
                tx.insertGuardian(guardianId = parentUnit2.id, childId = childInUnit2.id)
                tx.insertGuardian(guardianId = parentGroup2.id, childId = childInGroup2.id)
                tx.insertGuardian(guardianId = parentSpecific.id, childId = specificChild.id)

                val placement1 =
                    tx.insert(
                        DevPlacement(
                            childId = childInArea1.id,
                            unitId = daycare1.id,
                            startDate = today.minusDays(10),
                            endDate = today.plusDays(30),
                        )
                    )
                val placement2 =
                    tx.insert(
                        DevPlacement(
                            childId = childInUnit2.id,
                            unitId = daycare2.id,
                            startDate = today.minusDays(10),
                            endDate = today.plusDays(30),
                        )
                    )
                val placement3 =
                    tx.insert(
                        DevPlacement(
                            childId = childInGroup2.id,
                            unitId = daycare2.id,
                            startDate = today.minusDays(10),
                            endDate = today.plusDays(30),
                        )
                    )
                val placement4 =
                    tx.insert(
                        DevPlacement(
                            childId = specificChild.id,
                            unitId = daycare1.id,
                            startDate = today.minusDays(10),
                            endDate = today.plusDays(30),
                        )
                    )

                tx.insert(
                    DevDaycareGroupPlacement(
                        daycareGroupId = group1.id,
                        daycarePlacementId = placement1,
                        startDate = today.minusDays(10),
                        endDate = today.plusDays(30),
                    )
                )
                tx.insert(
                    DevDaycareGroupPlacement(
                        daycareGroupId = group2.id,
                        daycarePlacementId = placement2,
                        startDate = today.minusDays(10),
                        endDate = today.plusDays(30),
                    )
                )
                tx.insert(
                    DevDaycareGroupPlacement(
                        daycareGroupId = group2.id,
                        daycarePlacementId = placement3,
                        startDate = today.minusDays(10),
                        endDate = today.plusDays(30),
                    )
                )
                tx.insert(
                    DevDaycareGroupPlacement(
                        daycareGroupId = group1.id,
                        daycarePlacementId = placement4,
                        startDate = today.minusDays(10),
                        endDate = today.plusDays(30),
                    )
                )

                tx.insertDaycareAclRow(daycare1.id, employee1.id, UserRole.UNIT_SUPERVISOR)
                tx.insertDaycareAclRow(daycare2.id, employee1.id, UserRole.UNIT_SUPERVISOR)
                tx.upsertEmployeeMessageAccount(employee1.id)
            }

        val parentArea1AccountId = db.read { tx -> tx.getCitizenMessageAccount(parentArea1.id) }
        val parentUnit2AccountId = db.read { tx -> tx.getCitizenMessageAccount(parentUnit2.id) }
        val parentGroup2AccountId = db.read { tx -> tx.getCitizenMessageAccount(parentGroup2.id) }
        val parentSpecificAccountId =
            db.read { tx -> tx.getCitizenMessageAccount(parentSpecific.id) }

        val accounts =
            db.read { tx ->
                tx.getMessageAccountsForRecipients(
                    senderAccount = tx.getSenderAccount(employeeAccountId),
                    recipients =
                        setOf(
                            MessageRecipient.Area(area1.id),
                            MessageRecipient.Unit(daycare2.id),
                            MessageRecipient.Group(group2.id),
                            MessageRecipient.Child(specificChild.id),
                        ),
                    filters = null,
                    date = today,
                )
            }

        assertEquals(
            4,
            accounts.map { it.first }.toSet().size,
            "Should return accounts from all recipient types",
        )
        assertTrue(accounts.any { it.first == parentArea1AccountId })
        assertTrue(accounts.any { it.first == parentUnit2AccountId })
        assertTrue(accounts.any { it.first == parentGroup2AccountId })
        assertTrue(accounts.any { it.first == parentSpecificAccountId })
    }

    @Test
    fun `getMessageAccountsForRecipients with overlapping recipients returns DISTINCT accounts`() {
        val today = LocalDate.of(2021, 5, 1)
        val area = DevCareArea()
        val daycare =
            DevDaycare(areaId = area.id, enabledPilotFeatures = setOf(PilotFeature.MESSAGING))
        val group = DevDaycareGroup(daycareId = daycare.id)

        val child = DevPerson()
        val parent = DevPerson()

        val employeeAccountId =
            db.transaction { tx ->
                tx.insert(area)
                tx.insert(daycare)
                tx.insert(group)

                tx.insert(child, DevPersonType.CHILD)
                tx.insert(parent, DevPersonType.ADULT)

                tx.insertGuardian(guardianId = parent.id, childId = child.id)

                val placement =
                    tx.insert(
                        DevPlacement(
                            childId = child.id,
                            unitId = daycare.id,
                            startDate = today.minusDays(10),
                            endDate = today.plusDays(30),
                        )
                    )

                tx.insert(
                    DevDaycareGroupPlacement(
                        daycareGroupId = group.id,
                        daycarePlacementId = placement,
                        startDate = today.minusDays(10),
                        endDate = today.plusDays(30),
                    )
                )

                tx.insertDaycareAclRow(daycare.id, employee1.id, UserRole.UNIT_SUPERVISOR)
                tx.upsertEmployeeMessageAccount(employee1.id)
            }

        val parentAccountId = db.read { tx -> tx.getCitizenMessageAccount(parent.id) }

        val accounts =
            db.read { tx ->
                tx.getMessageAccountsForRecipients(
                    senderAccount = tx.getSenderAccount(employeeAccountId),
                    recipients =
                        setOf(
                            MessageRecipient.Area(area.id),
                            MessageRecipient.Unit(daycare.id),
                            MessageRecipient.Group(group.id),
                            MessageRecipient.Child(child.id),
                        ),
                    filters = null,
                    date = today,
                )
            }

        assertEquals(
            1,
            accounts.size,
            "Same child matching multiple recipient specifications should be returned once (DISTINCT works)",
        )
        assertTrue(accounts.any { it.first == parentAccountId && it.second == child.id })
    }

    @Test
    fun `getMessageAccountsForRecipients with starters and current mixed in same query returns both properly partitioned`() {
        val today = LocalDate.of(2021, 5, 1)
        val area = DevCareArea()
        val daycare =
            DevDaycare(areaId = area.id, enabledPilotFeatures = setOf(PilotFeature.MESSAGING))
        val group = DevDaycareGroup(daycareId = daycare.id)

        val currentChild = DevPerson()
        val starterChild = DevPerson()
        val currentParent = DevPerson()
        val starterParent = DevPerson()

        val employeeAccountId =
            db.transaction { tx ->
                tx.insert(area)
                tx.insert(daycare)
                tx.insert(group)

                tx.insert(currentChild, DevPersonType.CHILD)
                tx.insert(starterChild, DevPersonType.CHILD)
                tx.insert(currentParent, DevPersonType.ADULT)
                tx.insert(starterParent, DevPersonType.ADULT)

                tx.insertGuardian(guardianId = currentParent.id, childId = currentChild.id)
                tx.insertGuardian(guardianId = starterParent.id, childId = starterChild.id)

                val currentPlacement =
                    tx.insert(
                        DevPlacement(
                            childId = currentChild.id,
                            unitId = daycare.id,
                            startDate = today.minusDays(10),
                            endDate = today.plusDays(30),
                        )
                    )
                val starterPlacement =
                    tx.insert(
                        DevPlacement(
                            childId = starterChild.id,
                            unitId = daycare.id,
                            startDate = today.plusDays(5),
                            endDate = today.plusDays(30),
                        )
                    )

                tx.insert(
                    DevDaycareGroupPlacement(
                        daycareGroupId = group.id,
                        daycarePlacementId = currentPlacement,
                        startDate = today.minusDays(10),
                        endDate = today.plusDays(30),
                    )
                )
                tx.insert(
                    DevDaycareGroupPlacement(
                        daycareGroupId = group.id,
                        daycarePlacementId = starterPlacement,
                        startDate = today.plusDays(5),
                        endDate = today.plusDays(30),
                    )
                )

                tx.insertDaycareAclRow(daycare.id, employee1.id, UserRole.UNIT_SUPERVISOR)
                tx.upsertEmployeeMessageAccount(employee1.id)
            }

        val currentParentAccountId = db.read { tx -> tx.getCitizenMessageAccount(currentParent.id) }
        val starterParentAccountId = db.read { tx -> tx.getCitizenMessageAccount(starterParent.id) }

        val accounts =
            db.read { tx ->
                tx.getMessageAccountsForRecipients(
                    senderAccount = tx.getSenderAccount(employeeAccountId),
                    recipients =
                        setOf(
                            MessageRecipient.Unit(daycare.id, starter = false),
                            MessageRecipient.Unit(daycare.id, starter = true),
                        ),
                    filters = null,
                    date = today,
                )
            }

        assertEquals(2, accounts.size, "Should return both current and starter children")
        assertTrue(
            accounts.any { it.first == currentParentAccountId },
            "Current parent should be included",
        )
        assertTrue(
            accounts.any { it.first == starterParentAccountId },
            "Starter parent should be included",
        )
    }
}
