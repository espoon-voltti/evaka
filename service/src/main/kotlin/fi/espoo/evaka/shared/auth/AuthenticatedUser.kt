// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.auth

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import fi.espoo.evaka.pis.EmployeeUser
import fi.espoo.evaka.shared.domain.Forbidden
import java.util.UUID

@JsonSerialize(using = AuthenticatedUserJsonSerializer::class)
@JsonDeserialize(using = AuthenticatedUserJsonDeserializer::class)
sealed class AuthenticatedUser : RoleContainer {
    open val isEndUser = false
    open val isAdmin = false
    open val isSystemInternalUser = false

    abstract val id: UUID

    fun assertSystemInternalUser() {
        if (!this.isSystemInternalUser) {
            throw Forbidden("Only accessible to the system internal user")
        }
    }

    data class Citizen(override val id: UUID) : AuthenticatedUser() {
        override val roles: Set<UserRole> = setOf(UserRole.END_USER)
        override val isEndUser = true
    }

    data class WeakCitizen(override val id: UUID) : AuthenticatedUser() {
        override val roles: Set<UserRole> = setOf(UserRole.CITIZEN_WEAK)
        override val isEndUser = true
    }

    data class Employee private constructor(override val id: UUID, val globalRoles: Set<UserRole>, val allScopedRoles: Set<UserRole>) : AuthenticatedUser() {
        constructor(id: UUID, roles: Set<UserRole>) : this(id, roles - UserRole.SCOPED_ROLES, roles.intersect(UserRole.SCOPED_ROLES))
        constructor(employeeUser: EmployeeUser) : this(employeeUser.id, employeeUser.globalRoles, employeeUser.allScopedRoles)
        override val roles: Set<UserRole> = globalRoles + allScopedRoles
        override val isAdmin = roles.contains(UserRole.ADMIN)
    }

    data class MobileDevice(override val id: UUID) : AuthenticatedUser() {
        override val roles: Set<UserRole> = emptySet()
    }

    object SystemInternalUser : AuthenticatedUser() {
        override val id: UUID = UUID.fromString("00000000-0000-0000-0000-000000000000")
        override val roles: Set<UserRole> = emptySet()
        override val isSystemInternalUser = true
        override fun toString(): String = "SystemInternalUser"
    }
}
