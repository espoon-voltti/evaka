// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.messaging

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.pis.Creator
import fi.espoo.evaka.pis.createParentship
import fi.espoo.evaka.pis.service.insertGuardian
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.insertDaycareAclRow
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.dev.*
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.shared.domain.RealEvakaClock
import fi.espoo.evaka.shared.security.PilotFeature
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class MessageRecipientsIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var messageController: MessageController

    private val placementStart = LocalDate.now().minusDays(30)
    private val placementEnd = LocalDate.now().plusDays(30)

    @Test
    fun `message recipient endpoint works for units`() {
        val area = DevCareArea()
        val daycare1 =
            DevDaycare(areaId = area.id, enabledPilotFeatures = setOf(PilotFeature.MESSAGING))
        val daycare2 =
            DevDaycare(areaId = area.id, enabledPilotFeatures = setOf(PilotFeature.MESSAGING))

        val group1 = DevDaycareGroup(daycareId = daycare1.id, name = "Testaajat")
        val group2 = DevDaycareGroup(daycareId = daycare2.id, name = "Koekaniinit")

        val supervisor1 = DevEmployee()
        val supervisor2 = DevEmployee()

        val adult1 = DevPerson()
        val adult2 = DevPerson()
        val adult3 = DevPerson()

        val child1 = DevPerson()
        val child2 = DevPerson()
        val child3 = DevPerson()
        val child4 = DevPerson()

        val (groupMessageAccount, supervisor1MessageAccount, supervisor2MessageAccount) =
            db.transaction { tx ->
                tx.insert(area)
                tx.insert(daycare1)
                tx.insert(daycare2)

                tx.insert(group1)
                tx.insert(group2)

                val groupMessageAccount = tx.createDaycareGroupMessageAccount(group1.id)

                tx.insert(child1, DevPersonType.CHILD)
                tx.insert(child2, DevPersonType.CHILD)
                tx.insert(child3, DevPersonType.CHILD)
                tx.insert(child4, DevPersonType.CHILD)

                tx.insert(adult1, DevPersonType.ADULT)
                tx.insert(adult2, DevPersonType.ADULT)
                tx.insert(adult3, DevPersonType.ADULT)

                insertChildToGroup(tx, child1.id, adult1.id, group1.id, daycare1.id)
                insertChildToGroup(tx, child2.id, adult2.id, group2.id, daycare2.id)

                // Child 3 has a placement but is not in any group => should not show up in
                // recipients list
                tx.insert(
                    DevPlacement(
                        childId = child3.id,
                        unitId = daycare1.id,
                        startDate = placementStart,
                        endDate = placementEnd,
                    )
                )
                tx.insertGuardian(adult3.id, child3.id)

                // Child 4 has no guardians => should not show up in recipients list
                insertChildToGroup(tx, child4.id, null, group2.id, daycare2.id)
                tx.createParentship(child4.id, adult3.id, placementStart, placementEnd, Creator.DVV)

                tx.insert(supervisor1)
                val supervisor1MessageAccount = tx.upsertEmployeeMessageAccount(supervisor1.id)
                tx.insert(supervisor2)
                val supervisor2MessageAccount = tx.upsertEmployeeMessageAccount(supervisor2.id)
                tx.insertDaycareAclRow(daycare1.id, supervisor1.id, UserRole.UNIT_SUPERVISOR)
                tx.insertDaycareAclRow(daycare2.id, supervisor2.id, UserRole.UNIT_SUPERVISOR)

                listOf(groupMessageAccount, supervisor1MessageAccount, supervisor2MessageAccount)
            }

        val recipients1 =
            messageController.getSelectableRecipients(
                dbInstance(),
                AuthenticatedUser.Employee(supervisor1.id, setOf()),
                RealEvakaClock(),
            )
        assertEquals(
            setOf(
                SelectableRecipientsResponse(
                    accountId = supervisor1MessageAccount,
                    receivers =
                        listOf(
                            SelectableRecipient.Unit(
                                id = daycare1.id,
                                name = daycare1.name,
                                hasStarters = false,
                                receivers =
                                    listOf(
                                        SelectableRecipient.Group(
                                            id = group1.id,
                                            name = group1.name,
                                            hasStarters = false,
                                            receivers =
                                                listOf(
                                                    SelectableRecipient.Child(
                                                        id = child1.id,
                                                        name =
                                                            "${child1.lastName} ${child2.firstName}",
                                                        startDate = null,
                                                    )
                                                ),
                                        )
                                    ),
                            )
                        ),
                ),
                SelectableRecipientsResponse(
                    accountId = groupMessageAccount,
                    receivers =
                        listOf(
                            SelectableRecipient.Group(
                                id = group1.id,
                                name = group1.name,
                                hasStarters = false,
                                receivers =
                                    listOf(
                                        SelectableRecipient.Child(
                                            id = child1.id,
                                            name = "${child1.lastName} ${child1.firstName}",
                                            startDate = null,
                                        )
                                    ),
                            )
                        ),
                ),
            ),
            recipients1.toSet(),
        )

        val recipients2 =
            messageController.getSelectableRecipients(
                dbInstance(),
                AuthenticatedUser.Employee(supervisor2.id, setOf()),
                RealEvakaClock(),
            )
        assertEquals(
            setOf(
                SelectableRecipientsResponse(
                    accountId = supervisor2MessageAccount,
                    receivers =
                        listOf(
                            SelectableRecipient.Unit(
                                id = daycare2.id,
                                name = daycare2.name,
                                hasStarters = false,
                                receivers =
                                    listOf(
                                        SelectableRecipient.Group(
                                            id = group2.id,
                                            name = group2.name,
                                            hasStarters = false,
                                            receivers =
                                                listOf(
                                                    SelectableRecipient.Child(
                                                        id = child2.id,
                                                        name =
                                                            "${child2.lastName} ${child2.firstName}",
                                                        startDate = null,
                                                    )
                                                ),
                                        )
                                    ),
                            )
                        ),
                )
            ),
            recipients2.toSet(),
        )
    }

    @Test
    fun `municipal recipients`() {
        val today = LocalDate.of(2023, 12, 1)
        val employee = DevEmployee(roles = setOf(UserRole.MESSAGING))

        val area1 = DevCareArea(name = "area1", shortName = "area1")
        val daycare1 =
            DevDaycare(
                areaId = area1.id,
                openingDate = today.minusYears(1),
                enabledPilotFeatures = setOf(PilotFeature.MESSAGING),
            )
        val daycare2 =
            DevDaycare(
                areaId = area1.id,
                openingDate = today.minusYears(1),
                enabledPilotFeatures = setOf(PilotFeature.MESSAGING),
            )

        val area2 = DevCareArea(name = "area2", shortName = "area2")
        val daycare3 =
            DevDaycare(
                areaId = area2.id,
                openingDate = today.minusYears(1),
                enabledPilotFeatures = setOf(PilotFeature.MESSAGING),
            )
        // Closed
        val daycare4 =
            DevDaycare(
                areaId = area2.id,
                openingDate = today.minusYears(1),
                closingDate = today.minusDays(1),
                enabledPilotFeatures = setOf(PilotFeature.MESSAGING),
            )
        // Messaging disabled
        val daycare5 =
            DevDaycare(
                areaId = area2.id,
                openingDate = today.minusYears(1),
                enabledPilotFeatures = setOf(),
            )

        val municipalAccountId =
            db.transaction { tx ->
                tx.insert(area1)
                tx.insert(area2)
                tx.insert(daycare1)
                tx.insert(daycare2)
                tx.insert(daycare3)
                tx.insert(daycare4)
                tx.insert(daycare5)

                tx.insert(employee)
                tx.createMunicipalMessageAccount()
            }
        val expectations = mapOf(area1 to listOf(daycare1, daycare2), area2 to listOf(daycare3))

        val response =
            messageController
                .getSelectableRecipients(
                    dbInstance(),
                    AuthenticatedUser.Employee(employee.id, setOf(UserRole.MESSAGING)),
                    MockEvakaClock(HelsinkiDateTime.of(today, LocalTime.of(12, 0, 0))),
                )
                .single()
        assertEquals(municipalAccountId, response.accountId)

        assertEquals(expectations.size, response.receivers.size)
        for ((area, units) in expectations) {
            val areaRecipient =
                response.receivers.filterIsInstance<SelectableRecipient.Area>().find {
                    it.id == area.id
                }!!
            assertEquals(area.name, areaRecipient.name)

            assertEquals(
                units
                    .sortedBy { it.id }
                    .map { SelectableRecipient.UnitInArea(id = it.id, name = it.name) },
                areaRecipient.receivers.sortedBy { it.id },
            )
        }
    }

    @Test
    fun `child staying in the same group but with different placement does not show in starters`() {
        val area = DevCareArea()
        val daycare1 =
            DevDaycare(areaId = area.id, enabledPilotFeatures = setOf(PilotFeature.MESSAGING))
        val group = DevDaycareGroup(daycareId = daycare1.id, name = "Testaajat")
        val supervisor = DevEmployee()
        val adult1 = DevPerson()
        val child1 = DevPerson()

        db.transaction { tx ->
            tx.insert(area)
            val unitId = tx.insert(daycare1)
            val groupId = tx.insert(group)
            val childId = tx.insert(child1, DevPersonType.CHILD)
            val adultId = tx.insert(adult1, DevPersonType.ADULT)
            val daycarePlacementId1 =
                tx.insert(
                    DevPlacement(
                        childId = childId,
                        unitId = unitId,
                        startDate = placementStart,
                        endDate = placementEnd.minusDays(10),
                    )
                )
            tx.insert(
                DevDaycareGroupPlacement(
                    daycarePlacementId = daycarePlacementId1,
                    daycareGroupId = groupId,
                    startDate = placementStart,
                    endDate = placementEnd.minusDays(10),
                )
            )
            val daycarePlacementId2 =
                tx.insert(
                    DevPlacement(
                        childId = childId,
                        unitId = unitId,
                        startDate = placementEnd.minusDays(9),
                        endDate = placementEnd,
                    )
                )
            tx.insert(
                DevDaycareGroupPlacement(
                    daycarePlacementId = daycarePlacementId2,
                    daycareGroupId = groupId,
                    startDate = placementEnd.minusDays(9),
                    endDate = placementEnd,
                )
            )
            tx.insert(supervisor)
            tx.upsertEmployeeMessageAccount(supervisor.id)
            tx.insertDaycareAclRow(daycare1.id, supervisor.id, UserRole.UNIT_SUPERVISOR)
            tx.insertEmployeeToDaycareGroupAcl(groupId, supervisor.id)
            tx.createDaycareGroupMessageAccount(group.id)
            tx.insertGuardian(adultId, childId)
        }

        val recipients =
            messageController.getSelectableRecipients(
                dbInstance(),
                AuthenticatedUser.Employee(supervisor.id, setOf()),
                RealEvakaClock(),
            )
        assertTrue {
            recipients.all { a ->
                a.receivers.none { r ->
                    (r is SelectableRecipient.Child && r.startDate != null) ||
                        (r is SelectableRecipient.Group && r.hasStarters) ||
                        (r is SelectableRecipient.Unit && r.hasStarters)
                }
            }
        }
    }

    private fun insertChildToGroup(
        tx: Database.Transaction,
        childId: ChildId,
        guardianId: PersonId?,
        groupId: GroupId,
        unitId: DaycareId,
    ) {
        if (guardianId != null) tx.insertGuardian(guardianId, childId)
        val daycarePlacementId =
            tx.insert(
                DevPlacement(
                    childId = childId,
                    unitId = unitId,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )
        tx.insert(
            DevDaycareGroupPlacement(
                daycarePlacementId = daycarePlacementId,
                daycareGroupId = groupId,
                startDate = placementStart,
                endDate = placementEnd,
            )
        )
    }
}
