// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.auth

import com.auth0.jwt.interfaces.DecodedJWT
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.MobileDeviceId
import fi.espoo.evaka.shared.PersonId
import java.util.UUID

fun DecodedJWT.toAuthenticatedUser(): AuthenticatedUser? =
    this.subject?.let { subject ->
        val id = UUID.fromString(subject)
        val employeeId: EmployeeId? =
            this.claims["evaka_employee_id"]?.asString()?.let(UUID::fromString)?.let(::EmployeeId)
        val type = this.claims["evaka_type"]?.asString()?.let(AuthenticatedUserType::valueOf)
        val roles =
            (this.claims["scope"]?.asString() ?: "")
                .let {
                    if (it.isEmpty()) {
                        emptyList()
                    } else {
                        it.split(' ')
                    }
                }
                .map { enumValueOf<UserRole>(it.removePrefix("ROLE_")) }
                .toSet()
        return when (type) {
            AuthenticatedUserType.citizen ->
                AuthenticatedUser.Citizen(PersonId(id), CitizenAuthLevel.STRONG)
            AuthenticatedUserType.citizen_weak ->
                AuthenticatedUser.Citizen(PersonId(id), CitizenAuthLevel.WEAK)
            AuthenticatedUserType.employee -> AuthenticatedUser.Employee(EmployeeId(id), roles)
            AuthenticatedUserType.mobile ->
                AuthenticatedUser.MobileDevice(MobileDeviceId(id), employeeId)
            AuthenticatedUserType.system -> AuthenticatedUser.SystemInternalUser
            AuthenticatedUserType.integration -> AuthenticatedUser.Integration
            null -> null
        }
    }
