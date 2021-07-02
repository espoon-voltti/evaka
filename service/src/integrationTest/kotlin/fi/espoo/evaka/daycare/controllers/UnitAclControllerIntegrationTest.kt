// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare.controllers

import com.github.kittinunf.fuel.core.isSuccessful
import com.github.kittinunf.fuel.jackson.responseObject
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.DaycareAclRow
import fi.espoo.evaka.shared.auth.DaycareAclRowEmployee
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.asUser
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.insertTestCareArea
import fi.espoo.evaka.shared.dev.insertTestDaycare
import fi.espoo.evaka.shared.dev.insertTestEmployee
import fi.espoo.evaka.testAreaCode
import fi.espoo.evaka.testAreaId
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDaycare2
import org.jdbi.v3.core.kotlin.mapTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UnitAclControllerIntegrationTest : FullApplicationTest() {
    private val employee = DaycareAclRowEmployee(
        id = EmployeeId(UUID.randomUUID()),
        firstName = "First",
        lastName = "Last",
        email = "test@example.com"
    )
    private lateinit var admin: AuthenticatedUser

    @BeforeEach
    protected fun beforeEach() {
        db.transaction { tx ->
            tx.resetDatabase()
            admin = AuthenticatedUser.Employee(
                tx.insertTestEmployee(DevEmployee(roles = setOf(UserRole.ADMIN))),
                roles = setOf(UserRole.ADMIN)
            )
            tx.insertTestCareArea(DevCareArea(id = testAreaId, name = testDaycare.areaName, areaCode = testAreaCode))
            tx.insertTestDaycare(DevDaycare(areaId = testAreaId, id = testDaycare.id, name = testDaycare.name))
            tx.insertTestDaycare(DevDaycare(areaId = testAreaId, id = testDaycare2.id, name = testDaycare2.name))
            employee.also {
                tx.insertTestEmployee(
                    DevEmployee(
                        id = it.id.raw,
                        firstName = it.firstName,
                        lastName = it.lastName,
                        email = it.email
                    )
                )
            }
        }
    }

    @Test
    fun `basic acl modification flow`() {
        assertTrue(getAclRows().isEmpty())

        insertSupervisor(testDaycare.id)
        assertEquals(
            listOf(
                DaycareAclRow(employee = employee, role = UserRole.UNIT_SUPERVISOR, groupIds = emptyList())
            ),
            getAclRows()
        )

        deleteSupervisor(testDaycare.id)
        assertTrue(getAclRows().isEmpty())

        insertStaff()
        assertEquals(
            listOf(
                DaycareAclRow(employee = employee, role = UserRole.STAFF, groupIds = emptyList())
            ),
            getAclRows()
        )

        deleteStaff()
        assertTrue(getAclRows().isEmpty())
    }

    @Test
    fun `supervisor message account`() {
        assertEquals(MessageAccountState.NO_ACCOUNT, employeeMessageAccountState())
        insertSupervisor(testDaycare.id)
        assertEquals(MessageAccountState.ACTIVE_ACCOUNT, employeeMessageAccountState())
        insertSupervisor(testDaycare2.id)
        assertEquals(MessageAccountState.ACTIVE_ACCOUNT, employeeMessageAccountState())
        deleteSupervisor(testDaycare.id)
        assertEquals(MessageAccountState.ACTIVE_ACCOUNT, employeeMessageAccountState())
        deleteSupervisor(testDaycare2.id)
        assertEquals(MessageAccountState.INACTIVE_ACCOUNT, employeeMessageAccountState())
    }

    private fun getAclRows(): List<DaycareAclRow> {
        val (_, res, body) = http.get("/daycares/${testDaycare.id}/acl")
            .asUser(admin)
            .responseObject<DaycareAclResponse>(objectMapper)
        assertTrue(res.isSuccessful)
        return body.get().rows
    }

    private fun insertSupervisor(daycareId: UUID) {
        val (_, res, _) = http.put("/daycares/$daycareId/supervisors/${employee.id}")
            .asUser(admin)
            .response()
        assertTrue(res.isSuccessful)
    }

    private fun deleteSupervisor(daycareId: UUID) {
        val (_, res, _) = http.delete("/daycares/$daycareId/supervisors/${employee.id}")
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

    private enum class MessageAccountState {
        NO_ACCOUNT,
        ACTIVE_ACCOUNT,
        INACTIVE_ACCOUNT
    }

    private fun employeeMessageAccountState(): MessageAccountState =
        db.read {
            it.createQuery(
                """
                    SELECT active FROM message_account
                    WHERE employee_id = :employeeId
                """.trimIndent()
            )
                .bind("employeeId", employee.id)
                .mapTo<Boolean>().toList()
        }.let { accounts ->
            if (accounts.size == 1) {
                if (accounts[0]) {
                    MessageAccountState.ACTIVE_ACCOUNT
                } else {
                    MessageAccountState.INACTIVE_ACCOUNT
                }
            } else if (accounts.isEmpty()) MessageAccountState.NO_ACCOUNT
            else throw RuntimeException("Employee has more than one account")
        }
}
