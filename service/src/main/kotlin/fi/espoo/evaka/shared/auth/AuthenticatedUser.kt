// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.auth

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.google.common.hash.HashCode
import com.google.common.hash.Hashing
import fi.espoo.evaka.pis.EmployeeUser
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.MobileDeviceId
import fi.espoo.evaka.shared.PersonId
import java.util.UUID

@JsonSerialize(using = AuthenticatedUserJsonSerializer::class)
@JsonDeserialize(using = AuthenticatedUserJsonDeserializer::class)
sealed class AuthenticatedUser {
    open val isAdmin = false

    abstract val type: AuthenticatedUserType
    val evakaUserId: EvakaUserId
        get() = EvakaUserId(rawId())

    abstract fun rawId(): UUID

    val rawIdHash: HashCode
        get() = Hashing.sha256().hashString(rawId().toString(), Charsets.UTF_8)

    data class Citizen(val id: PersonId, val authLevel: CitizenAuthLevel) : AuthenticatedUser() {
        override fun rawId(): UUID = id.raw

        override val type =
            when (authLevel) {
                CitizenAuthLevel.STRONG -> AuthenticatedUserType.citizen
                CitizenAuthLevel.WEAK -> AuthenticatedUserType.citizen_weak
            }
    }

    data class Employee
    private constructor(
        val id: EmployeeId,
        val globalRoles: Set<UserRole>,
        val allScopedRoles: Set<UserRole>,
    ) : AuthenticatedUser() {
        constructor(
            id: EmployeeId,
            roles: Set<UserRole>,
        ) : this(id, roles - UserRole.SCOPED_ROLES, roles.intersect(UserRole.SCOPED_ROLES))

        constructor(
            employeeUser: EmployeeUser
        ) : this(employeeUser.id, employeeUser.globalRoles, employeeUser.allScopedRoles)

        override fun rawId(): UUID = id.raw

        override val isAdmin = globalRoles.contains(UserRole.ADMIN)
        override val type = AuthenticatedUserType.employee
    }

    data class MobileDevice(val id: MobileDeviceId, val employeeId: EmployeeId? = null) :
        AuthenticatedUser() {
        val authLevel: MobileAuthLevel
            get() = if (employeeId != null) MobileAuthLevel.PIN_LOGIN else MobileAuthLevel.DEFAULT

        override fun rawId(): UUID = id.raw

        override val type = AuthenticatedUserType.mobile

        val employeeIdHash: HashCode?
            get() = employeeId?.let { Hashing.sha256().hashString(it.toString(), Charsets.UTF_8) }
    }

    object Integration : AuthenticatedUser() {
        override fun rawId(): UUID = UUID.fromString("00000000-0000-0000-0000-000000000000")

        override val type = AuthenticatedUserType.integration

        override fun toString(): String = "Integration"
    }

    object SystemInternalUser : AuthenticatedUser() {
        override fun rawId(): UUID = UUID.fromString("00000000-0000-0000-0000-000000000000")

        override val type = AuthenticatedUserType.system

        override fun toString(): String = "SystemInternalUser"
    }
}

enum class CitizenAuthLevel {
    WEAK,
    STRONG,
}

enum class MobileAuthLevel {
    DEFAULT,
    PIN_LOGIN,
}

/** Low-level AuthenticatedUser type "tag" used in serialized representations (JWT, JSON). */
@Suppress("EnumEntryName", "ktlint:standard:enum-entry-name-case")
enum class AuthenticatedUserType {
    citizen,
    citizen_weak,
    employee,
    mobile,
    system,
    integration,
}
