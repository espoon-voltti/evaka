// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.pairing.listPersonalDevices
import fi.espoo.evaka.pis.controllers.PinCode
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.insertDaycareAclRow
import fi.espoo.evaka.shared.auth.insertDaycareGroupAcl
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevPersonalMobileDevice
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.jupiter.api.Test

class InactiveEmployeesRoleResetIntegrationTest : PureJdbiTest(resetDbBeforeEach = true) {
    private val firstOfAugust2021 =
        HelsinkiDateTime.of(LocalDate.of(2021, 8, 1), LocalTime.of(3, 15))

    @Test
    fun `global roles are not reset when last_login is now`() {
        val employeeId =
            db.transaction {
                it.insert(DevEmployee(lastLogin = firstOfAugust2021, roles = setOf(UserRole.ADMIN)))
            }

        val resetEmployeeIds = db.transaction { it.deactivateInactiveEmployees(firstOfAugust2021) }

        assertEquals(listOf(), resetEmployeeIds)
        val globalRoles = db.read { it.getEmployeeWithRoles(employeeId) }!!.globalRoles
        assertEquals(setOf(UserRole.ADMIN), globalRoles.toSet())
    }

    @Test
    fun `global roles are reset when last_login is over 45 days ago`() {
        val employeeId =
            db.transaction {
                it.insert(
                    DevEmployee(
                        lastLogin = firstOfAugust2021.minusDays(46),
                        roles = setOf(UserRole.ADMIN)
                    )
                )
            }

        db.transaction { it.deactivateInactiveEmployees(firstOfAugust2021) }

        val globalRoles = db.read { it.getEmployeeWithRoles(employeeId) }!!.globalRoles
        assertEquals(listOf(), globalRoles)
    }

    @Test
    fun `scoped roles are not reset when last_login is now`() {
        val employeeId =
            db.transaction {
                val employeeId = it.insert(DevEmployee(lastLogin = firstOfAugust2021))
                val areaId = it.insert(DevCareArea())
                val unitId = it.insert(DevDaycare(areaId = areaId))
                it.insertDaycareAclRow(
                    daycareId = unitId,
                    employeeId = employeeId,
                    role = UserRole.STAFF
                )
                employeeId
            }

        db.transaction { it.deactivateInactiveEmployees(firstOfAugust2021) }

        val scopedRoles =
            db.read { it.getEmployeeWithRoles(employeeId) }!!.daycareRoles.map { it.role }
        assertEquals(setOf(UserRole.STAFF), scopedRoles.toSet())
    }

    @Test
    fun `scoped roles are reset when last_login is over 45 days ago`() {
        val employeeId =
            db.transaction {
                val employeeId = it.insert(DevEmployee(lastLogin = firstOfAugust2021.minusDays(46)))
                val areaId = it.insert(DevCareArea())
                val unitId = it.insert(DevDaycare(areaId = areaId))
                it.insertDaycareAclRow(
                    daycareId = unitId,
                    employeeId = employeeId,
                    role = UserRole.STAFF
                )
                it.setDaycareAclUpdated(unitId, employeeId, firstOfAugust2021.minusDays(46))
                employeeId
            }

        db.transaction { it.deactivateInactiveEmployees(firstOfAugust2021) }

        val employeeWithRoles = db.read { it.getEmployeeWithRoles(employeeId) }!!
        assertEquals(listOf(), employeeWithRoles.daycareRoles)
        assertEquals(listOf(), employeeWithRoles.daycareGroupRoles)
    }

    @Test
    fun `scoped roles are not reset when last_login is distant past but daycare acl has been recently updated`() {
        val employeeId =
            db.transaction {
                val employeeId =
                    it.insert(DevEmployee(lastLogin = firstOfAugust2021.minusDays(1000)))
                val areaId = it.insert(DevCareArea())
                val unitId = it.insert(DevDaycare(areaId = areaId))
                it.insertDaycareAclRow(
                    daycareId = unitId,
                    employeeId = employeeId,
                    role = UserRole.STAFF
                )
                it.setDaycareAclUpdated(unitId, employeeId, firstOfAugust2021.minusDays(5))
                employeeId
            }

        db.transaction { it.deactivateInactiveEmployees(firstOfAugust2021) }

        val scopedRoles =
            db.read { it.getEmployeeWithRoles(employeeId) }!!.daycareRoles.map { it.role }
        assertEquals(listOf(UserRole.STAFF), scopedRoles)
    }

    @Test
    fun `scoped roles are not reset when last_login is in distant past but daycare group acl has been recently updated`() {
        val employeeId =
            db.transaction {
                val employeeId =
                    it.insert(DevEmployee(lastLogin = firstOfAugust2021.minusDays(1000)))
                val areaId = it.insert(DevCareArea())
                val unitId = it.insert(DevDaycare(areaId = areaId))
                val groupId = it.insert(DevDaycareGroup(daycareId = unitId))
                it.insertDaycareAclRow(
                    daycareId = unitId,
                    employeeId = employeeId,
                    role = UserRole.STAFF
                )
                it.setDaycareAclUpdated(unitId, employeeId, firstOfAugust2021.minusDays(1000))
                it.insertDaycareGroupAcl(
                    daycareId = unitId,
                    employeeId = employeeId,
                    groupIds = listOf(groupId)
                )
                it.setDaycareGroupAclUpdated(groupId, employeeId, firstOfAugust2021.minusDays(5))
                employeeId
            }

        db.transaction { it.deactivateInactiveEmployees(firstOfAugust2021) }

        val scopedRoles =
            db.read { it.getEmployeeWithRoles(employeeId) }!!.daycareRoles.map { it.role }
        assertEquals(listOf(UserRole.STAFF), scopedRoles)
    }

    @Test
    fun `pin code is reset when last_login is over 45 days ago`() {
        val employeeId =
            db.transaction {
                val employeeId = it.insert(DevEmployee(lastLogin = firstOfAugust2021.minusDays(46)))
                val areaId = it.insert(DevCareArea())
                val unitId = it.insert(DevDaycare(areaId = areaId))
                it.insertDaycareAclRow(
                    daycareId = unitId,
                    employeeId = employeeId,
                    role = UserRole.STAFF
                )
                it.setDaycareAclUpdated(unitId, employeeId, firstOfAugust2021.minusDays(46))
                it.upsertPinCode(userId = employeeId, pinCode = PinCode("6712"))
                employeeId
            }

        db.transaction { it.deactivateInactiveEmployees(firstOfAugust2021) }

        val pin = db.read { it.getPinCode(userId = employeeId) }
        assertNull(pin)
    }

    @Test
    fun `personal mobile pairing is removed when last_login is over 45 days ago`() {
        val employeeId =
            db.transaction {
                val employeeId = it.insert(DevEmployee(lastLogin = firstOfAugust2021.minusDays(46)))
                val areaId = it.insert(DevCareArea())
                val unitId = it.insert(DevDaycare(areaId = areaId))
                it.insertDaycareAclRow(
                    daycareId = unitId,
                    employeeId = employeeId,
                    role = UserRole.STAFF
                )
                it.setDaycareAclUpdated(unitId, employeeId, firstOfAugust2021.minusDays(46))
                it.insert(DevPersonalMobileDevice(employeeId = employeeId))
                employeeId
            }
        assertEquals(1, db.read { it.listPersonalDevices(employeeId).size })

        db.transaction { it.deactivateInactiveEmployees(firstOfAugust2021) }

        assertEquals(0, db.read { it.listPersonalDevices(employeeId).size })
    }

    @Test
    fun `personal mobile pairing is not removed when last_login is less than 45 days ago`() {
        val employeeId =
            db.transaction {
                val employeeId = it.insert(DevEmployee(lastLogin = firstOfAugust2021.minusDays(44)))
                val areaId = it.insert(DevCareArea())
                val unitId = it.insert(DevDaycare(areaId = areaId))
                it.insertDaycareAclRow(
                    daycareId = unitId,
                    employeeId = employeeId,
                    role = UserRole.STAFF
                )
                it.setDaycareAclUpdated(unitId, employeeId, firstOfAugust2021.minusDays(44))
                it.insert(DevPersonalMobileDevice(employeeId = employeeId))
                employeeId
            }
        assertEquals(1, db.read { it.listPersonalDevices(employeeId).size })

        db.transaction { it.deactivateInactiveEmployees(firstOfAugust2021) }

        assertEquals(1, db.read { it.listPersonalDevices(employeeId).size })
    }

    private fun Database.Transaction.setDaycareAclUpdated(
        unitId: DaycareId,
        employeeId: EmployeeId,
        timestamp: HelsinkiDateTime
    ) {
        execute {
            sql(
                """
ALTER TABLE daycare_acl DISABLE TRIGGER USER;
UPDATE daycare_acl SET updated = ${bind(timestamp)}
WHERE daycare_id = ${bind(unitId)} AND employee_id = ${bind(employeeId)};
ALTER TABLE daycare_acl ENABLE TRIGGER USER;
"""
            )
        }
    }

    private fun Database.Transaction.setDaycareGroupAclUpdated(
        groupId: GroupId,
        employeeId: EmployeeId,
        timestamp: HelsinkiDateTime
    ) {
        execute {
            sql(
                """
ALTER TABLE daycare_group_acl DISABLE TRIGGER USER;
UPDATE daycare_group_acl SET updated = ${bind(timestamp)}
WHERE daycare_group_id = ${bind(groupId)} AND employee_id = ${bind(employeeId)};
ALTER TABLE daycare_group_acl ENABLE TRIGGER USER;
"""
            )
        }
    }
}
