// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare.controllers

import com.github.kittinunf.fuel.core.isSuccessful
import com.github.kittinunf.fuel.jackson.responseObject
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.DaycareAclRow
import fi.espoo.evaka.shared.auth.DaycareAclRowEmployee
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.asUser
import fi.espoo.evaka.shared.db.handle
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.insertTestCareArea
import fi.espoo.evaka.shared.dev.insertTestDaycare
import fi.espoo.evaka.shared.dev.insertTestEmployee
import fi.espoo.evaka.testAreaCode
import fi.espoo.evaka.testAreaId
import fi.espoo.evaka.testDaycare
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class UnitAclControllerIntegrationTest : FullApplicationTest() {
    private val employee = DaycareAclRowEmployee(
        id = UUID.randomUUID(),
        firstName = "First",
        lastName = "Last",
        email = "test@example.com"
    )
    private lateinit var admin: AuthenticatedUser

    @BeforeEach
    protected fun beforeEach() {
        jdbi.handle { h ->
            resetDatabase(h)
            admin = AuthenticatedUser.Employee(h.insertTestEmployee(DevEmployee(roles = setOf(UserRole.ADMIN))), roles = setOf(UserRole.ADMIN))
            h.insertTestCareArea(DevCareArea(id = testAreaId, name = testDaycare.areaName, areaCode = testAreaCode))
            h.insertTestDaycare(DevDaycare(areaId = testAreaId, id = testDaycare.id, name = testDaycare.name))
            employee.also {
                h.insertTestEmployee(DevEmployee(id = it.id, firstName = it.firstName, lastName = it.lastName, email = it.email))
            }
        }
    }

    @Test
    fun testBasicAclModificationFlow() {
        assertTrue(getAclRows().isEmpty())

        insertSupervisor()
        assertEquals(
            listOf(
                DaycareAclRow(employee = employee, role = UserRole.UNIT_SUPERVISOR)
            ),
            getAclRows()
        )

        deleteSupervisor()
        assertTrue(getAclRows().isEmpty())

        insertStaff()
        assertEquals(
            listOf(
                DaycareAclRow(employee = employee, role = UserRole.STAFF)
            ),
            getAclRows()
        )

        deleteStaff()
        assertTrue(getAclRows().isEmpty())
    }

    private fun getAclRows(): List<DaycareAclRow> {
        val (_, res, body) = http.get("/daycares/${testDaycare.id}/acl")
            .asUser(admin)
            .responseObject<DaycareAclResponse>(objectMapper)
        assertTrue(res.isSuccessful)
        return body.get().rows
    }

    private fun insertSupervisor() {
        val (_, res, _) = http.put("/daycares/${testDaycare.id}/supervisors/${employee.id}")
            .asUser(admin)
            .response()
        assertTrue(res.isSuccessful)
    }

    private fun deleteSupervisor() {
        val (_, res, _) = http.delete("/daycares/${testDaycare.id}/supervisors/${employee.id}")
            .asUser(admin)
            .response()
        assertTrue(res.isSuccessful)
    }

    private fun insertStaff() {
        val (_, res, _) = http.put("/daycares/${testDaycare.id}/staff/${employee.id}")
            .asUser(admin)
            .response()
        assertTrue(res.isSuccessful)
    }

    private fun deleteStaff() {
        val (_, res, _) = http.delete("/daycares/${testDaycare.id}/staff/${employee.id}")
            .asUser(admin)
            .response()
        assertTrue(res.isSuccessful)
    }
}
