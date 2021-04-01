// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.auth

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
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

    data class Employee(override val id: UUID, override val roles: Set<UserRole>) : AuthenticatedUser() {
        override val isAdmin = roles.contains(UserRole.ADMIN)
    }

    data class MobileDevice(override val id: UUID) : AuthenticatedUser() {
        override val roles: Set<UserRole> = emptySet()
    }

    object SystemInternalUser : AuthenticatedUser() {
        override val id: UUID = UUID.fromString("00000000-0000-0000-0000-000000000000")
        override val roles: Set<UserRole> = emptySet()
        override val isSystemInternalUser = true
    }
}
