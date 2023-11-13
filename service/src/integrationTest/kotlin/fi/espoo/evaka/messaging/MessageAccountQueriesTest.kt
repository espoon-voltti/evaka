// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.messaging

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.insertDaycareAclRow
import fi.espoo.evaka.shared.auth.insertDaycareGroupAcl
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import fi.espoo.evaka.shared.security.PilotFeature
import fi.espoo.evaka.shared.security.actionrule.DefaultActionRuleMapping
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class MessageAccountQueriesTest : PureJdbiTest(resetDbBeforeEach = true) {

    private val personId = PersonId(UUID.randomUUID())
    private val supervisorId = EmployeeId(UUID.randomUUID())
    private val employee1Id = EmployeeId(UUID.randomUUID())
    private val employee2Id = EmployeeId(UUID.randomUUID())
    private val accessControl = AccessControl(DefaultActionRuleMapping(), noopTracer)
    private lateinit var clock: EvakaClock

    @BeforeEach
    fun setUp() {
        clock = MockEvakaClock(HelsinkiDateTime.of(LocalDate.of(2022, 11, 8), LocalTime.of(13, 36)))
        db.transaction {
            it.insert(
                DevPerson(id = personId, firstName = "Firstname", lastName = "Person"),
                DevPersonType.ADULT
            )

            it.insert(
                DevEmployee(id = supervisorId, firstName = "Firstname", lastName = "Supervisor")
            )
            it.upsertEmployeeMessageAccount(supervisorId)

            it.insert(
                DevEmployee(id = employee1Id, firstName = "Firstname", lastName = "Employee 1")
            )
            it.insert(
                DevEmployee(id = employee2Id, firstName = "Firstname", lastName = "Employee 2")
            )

            val randomUuid = it.insert(DevEmployee(firstName = "Random", lastName = "Employee"))
            it.upsertEmployeeMessageAccount(randomUuid)

            val areaId = it.insert(DevCareArea())
            val daycareId =
                it.insert(
                    DevDaycare(
                        areaId = areaId,
                        enabledPilotFeatures = setOf(PilotFeature.MESSAGING)
                    )
                )

            val groupId = it.insert(DevDaycareGroup(daycareId = daycareId, name = "Testiläiset"))
            it.createDaycareGroupMessageAccount(groupId)

            val group2Id = it.insert(DevDaycareGroup(daycareId = daycareId, name = "Koekaniinit"))
            it.createDaycareGroupMessageAccount(group2Id)

            it.insertDaycareAclRow(daycareId, supervisorId, UserRole.UNIT_SUPERVISOR)

            it.insertDaycareAclRow(daycareId, employee1Id, UserRole.STAFF)
            it.insertDaycareGroupAcl(daycareId, employee1Id, listOf(groupId))

            // employee2 has no groups
            it.insertDaycareAclRow(daycareId, employee2Id, UserRole.STAFF)

            // There should be no permissions to anything about this daycare
            val daycare2Id =
                it.insert(
                    DevDaycare(
                        areaId = areaId,
                        name = "Väärä päiväkoti",
                        enabledPilotFeatures = setOf(PilotFeature.MESSAGING)
                    )
                )
            val group3Id = it.insert(DevDaycareGroup(daycareId = daycare2Id, name = "Väärät"))
            it.createDaycareGroupMessageAccount(group3Id)
        }
    }

    @Test
    fun `citizen gets access to his own account`() {
        db.read { it.getCitizenMessageAccount(personId) }
    }

    @Test
    fun `supervisor get access to personal and all group accounts of his daycares`() {
        val personalAccountName = "Supervisor Firstname"
        val group1AccountName = "Test Daycare - Testiläiset"
        val group2AccountName = "Test Daycare - Koekaniinit"

        val accounts =
            db.transaction {
                it.getEmployeeMessageAccountIds(
                    accessControl.requireAuthorizationFilter(
                        it,
                        AuthenticatedUser.Employee(supervisorId, emptySet()),
                        clock,
                        Action.MessageAccount.ACCESS
                    )
                )
            }
        assertEquals(3, accounts.size)

        val accounts2 =
            db.read {
                it.getAuthorizedMessageAccountsForEmployee(
                    accessControl.requireAuthorizationFilter(
                        it,
                        AuthenticatedUser.Employee(supervisorId, emptySet()),
                        clock,
                        Action.MessageAccount.ACCESS
                    ),
                    "Espoo",
                    "Espoo palveluohjaus"
                )
            }
        assertEquals(3, accounts2.size)
        val personalAccount =
            accounts2.find { it.account.type == AccountType.PERSONAL }
                ?: throw Error("Personal account not found")
        assertEquals(personalAccountName, personalAccount.account.name)
        assertEquals(AccountType.PERSONAL, personalAccount.account.type)
        assertNull(personalAccount.daycareGroup)

        val groupAccount =
            accounts2.find { it.account.name == group1AccountName }
                ?: throw Error("Group account $group1AccountName not found")
        assertEquals(AccountType.GROUP, groupAccount.account.type)
        assertEquals("Test Daycare", groupAccount.daycareGroup?.unitName)
        assertEquals("Testiläiset", groupAccount.daycareGroup?.name)

        val group2Account =
            accounts2.find { it.account.name == group2AccountName }
                ?: throw Error("Group account $group2AccountName not found")
        assertEquals(AccountType.GROUP, group2Account.account.type)
        assertEquals("Test Daycare", group2Account.daycareGroup?.unitName)
        assertEquals("Koekaniinit", group2Account.daycareGroup?.name)
    }

    @Test
    fun `employee gets access to the accounts of his groups`() {
        val groupAccountName = "Test Daycare - Testiläiset"

        val accounts =
            db.transaction {
                it.getEmployeeMessageAccountIds(
                    accessControl.requireAuthorizationFilter(
                        it,
                        AuthenticatedUser.Employee(employee1Id, emptySet()),
                        clock,
                        Action.MessageAccount.ACCESS
                    )
                )
            }
        assertEquals(1, accounts.size)

        val accounts2 =
            db.read {
                it.getAuthorizedMessageAccountsForEmployee(
                    accessControl.requireAuthorizationFilter(
                        it,
                        AuthenticatedUser.Employee(employee1Id, emptySet()),
                        clock,
                        Action.MessageAccount.ACCESS
                    ),
                    "Espoo",
                    "Espoo palveluohjaus"
                )
            }
        assertEquals(1, accounts2.size)
        assertNull(accounts2.find { it.account.type == AccountType.PERSONAL })

        val groupAccount =
            accounts2.find { it.daycareGroup != null } ?: throw Error("Group account not found")
        assertEquals(groupAccountName, groupAccount.account.name)
        assertEquals(AccountType.GROUP, groupAccount.account.type)
        assertEquals("Test Daycare", groupAccount.daycareGroup?.unitName)
        assertEquals("Testiläiset", groupAccount.daycareGroup?.name)
    }

    @Test
    fun `employee not in any groups sees no accounts`() {
        val accounts =
            db.transaction {
                it.getEmployeeMessageAccountIds(
                    accessControl.requireAuthorizationFilter(
                        it,
                        AuthenticatedUser.Employee(employee2Id, emptySet()),
                        clock,
                        Action.MessageAccount.ACCESS
                    )
                )
            }
        assertEquals(0, accounts.size)

        val accounts2 =
            db.read {
                it.getAuthorizedMessageAccountsForEmployee(
                    accessControl.requireAuthorizationFilter(
                        it,
                        AuthenticatedUser.Employee(employee2Id, emptySet()),
                        clock,
                        Action.MessageAccount.ACCESS
                    ),
                    "Espoo",
                    "Espoo palveluohjaus"
                )
            }
        assertEquals(0, accounts2.size)
    }

    @Test
    fun `employee has no access to inactive accounts`() {
        assertEquals(
            3,
            db.read {
                    it.getEmployeeMessageAccountIds(
                        accessControl.requireAuthorizationFilter(
                            it,
                            AuthenticatedUser.Employee(supervisorId, emptySet()),
                            clock,
                            Action.MessageAccount.ACCESS
                        )
                    )
                }
                .size
        )
        db.transaction { it.deactivateEmployeeMessageAccount(supervisorId) }

        val accounts =
            db.transaction {
                it.getEmployeeMessageAccountIds(
                    accessControl.requireAuthorizationFilter(
                        it,
                        AuthenticatedUser.Employee(supervisorId, emptySet()),
                        clock,
                        Action.MessageAccount.ACCESS
                    )
                )
            }
        assertEquals(2, accounts.size)
    }

    @Test
    fun `unread counts`() {
        val now = HelsinkiDateTime.of(LocalDate.of(2022, 5, 14), LocalTime.of(12, 11))
        val counts =
            db.read {
                it.getUnreadMessagesCounts(
                    accessControl.requireAuthorizationFilter(
                        it,
                        AuthenticatedUser.Employee(supervisorId, emptySet()),
                        clock,
                        Action.MessageAccount.ACCESS
                    )
                )
            }
        assertEquals(0, counts.first().unreadCount)

        val employeeAccount = counts.first().accountId
        db.transaction { tx ->
            val allAccounts =
                tx.createQuery("SELECT id, name, 'PERSONAL' as type from message_account_view")
                    .toList<MessageAccount>()

            val contentId = tx.insertMessageContent("content", employeeAccount)
            val threadId =
                tx.insertThread(
                    MessageType.MESSAGE,
                    "title",
                    urgent = false,
                    sensitive = false,
                    isCopy = false
                )
            val messageId =
                tx.insertMessage(
                    now = now.minusSeconds(30),
                    contentId = contentId,
                    threadId = threadId,
                    sender = employeeAccount,
                    sentAt = now.minusSeconds(30),
                    recipientNames = allAccounts.map { it.name },
                    municipalAccountName = "Espoo",
                    serviceWorkerAccountName = "Espoo palveluohjaus"
                )
            tx.insertRecipients(listOf(messageId to allAccounts.map { it.id }.toSet()))
        }

        assertEquals(
            3,
            db.read { tx ->
                tx.getUnreadMessagesCounts(
                        accessControl.requireAuthorizationFilter(
                            tx,
                            AuthenticatedUser.Employee(supervisorId, emptySet()),
                            clock,
                            Action.MessageAccount.ACCESS
                        )
                    )
                    .sumOf { it.unreadCount }
            }
        )
        assertEquals(
            1,
            db.read {
                    it.getUnreadMessagesCounts(
                        accessControl.requireAuthorizationFilter(
                            it,
                            AuthenticatedUser.Employee(supervisorId, emptySet()),
                            clock,
                            Action.MessageAccount.ACCESS
                        )
                    )
                }
                .first()
                .unreadCount
        )
    }
}
