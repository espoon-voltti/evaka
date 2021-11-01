// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTCreator
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
import fi.espoo.evaka.shared.EmployeeId
import java.time.Clock
import java.time.Duration
import java.time.ZonedDateTime
import java.util.Date
import java.util.UUID

private const val tokenIssuer = "evaka-service"
private val tokenExpiration = Duration.ofHours(1)

fun DecodedJWT.toAuthenticatedUser(): AuthenticatedUser? = this.subject?.let { subject ->
    val id = UUID.fromString(subject)
    val employeeId: EmployeeId? = this.claims["evaka_employee_id"]?.asString()?.let(UUID::fromString)?.let(::EmployeeId)
    val type = this.claims["evaka_type"]?.asString()?.let(AuthenticatedUserType::valueOf)
    val roles = (this.claims["scope"]?.asString() ?: "")
        .let {
            if (it.isEmpty()) emptyList()
            else it.split(' ')
        }
        .map { enumValueOf<UserRole>(it.removePrefix("ROLE_")) }
        .toSet()
    return when (type) {
        AuthenticatedUserType.citizen -> AuthenticatedUser.Citizen(id)
        AuthenticatedUserType.citizen_weak -> AuthenticatedUser.WeakCitizen(id)
        AuthenticatedUserType.employee -> AuthenticatedUser.Employee(id, roles)
        AuthenticatedUserType.mobile -> AuthenticatedUser.MobileDevice(id, employeeId)
        AuthenticatedUserType.system -> AuthenticatedUser.SystemInternalUser
        null -> null
    }
}

fun AuthenticatedUser.applyToJwt(jwt: JWTCreator.Builder): JWTCreator.Builder = jwt
    .withSubject(id.toString())
    .withClaim("evaka_type", this.type.toString())
    .also {
        if (this is AuthenticatedUser.Employee) {
            it.withClaim("scope", roles.joinToString(" "))
        }
    }
    .also {
        if (this is AuthenticatedUser.MobileDevice && employeeId != null) {
            it.withClaim("evaka_employee_id", employeeId.toString())
        }
    }

fun encodeSignedJwtToken(
    algorithm: Algorithm,
    user: AuthenticatedUser,
    clock: Clock = Clock.systemDefaultZone()
): String {
    val issuedAt = ZonedDateTime.now(clock)
    val expiresAt = issuedAt.plus(tokenExpiration)

    val jwt = JWT.create()
        .withIssuer(tokenIssuer)
        .withIssuedAt(Date.from(issuedAt.toInstant()))
        .withExpiresAt(Date.from(expiresAt.toInstant()))
    user.applyToJwt(jwt)
    return jwt.sign(algorithm)
}
