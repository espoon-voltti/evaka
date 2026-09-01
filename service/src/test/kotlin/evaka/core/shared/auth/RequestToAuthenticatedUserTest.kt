// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.shared.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import evaka.core.shared.EmployeeId
import evaka.core.shared.MobileDeviceId
import evaka.core.shared.PersonId
import evaka.core.shared.noopTracer
import fi.espoo.voltti.auth.JwtTokenDecoder
import fi.espoo.voltti.logging.MdcKey
import jakarta.servlet.GenericServlet
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.jupiter.api.AfterEach
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import tools.jackson.module.kotlin.jsonMapper

class RequestToAuthenticatedUserTest {
    private val algorithm = Algorithm.none()
    private val jwtDecoder = JwtTokenDecoder(JWT.require(algorithm).build())
    private val filter = RequestToAuthenticatedUser(noopTracer())

    /** Captures MDC from inside the chain, because the filter clears it on the way out. */
    private class MdcCapturingServlet : GenericServlet() {
        var userRoles: String? = null

        override fun service(req: ServletRequest, res: ServletResponse) {
            userRoles = MdcKey.USER_ROLES.get()
        }
    }

    @AfterEach fun afterEach() = MdcKey.USER_ROLES.unset()

    private fun rolesLoggedFor(user: AuthenticatedUser): String? {
        val request =
            MockHttpServletRequest().apply {
                addHeader("Authorization", "Bearer ${JWT.create().sign(algorithm)}")
                addHeader("X-User", jsonMapper().writeValueAsString(user))
            }
        val response = MockHttpServletResponse()
        val servlet = MdcCapturingServlet()
        jwtDecoder.doFilter(request, response, MockFilterChain())
        filter.doFilter(request, response, MockFilterChain(servlet))
        return servlet.userRoles
    }

    @Test
    fun `global and scoped roles are logged together, sorted and delimiter-wrapped`() {
        val roles =
            rolesLoggedFor(
                AuthenticatedUser.Employee(
                    EmployeeId(UUID.randomUUID()),
                    setOf(
                        UserRole.SERVICE_WORKER,
                        UserRole.EARLY_CHILDHOOD_EDUCATION_SECRETARY,
                        UserRole.ADMIN,
                    ),
                )
            )
        assertEquals("|ADMIN|EARLY_CHILDHOOD_EDUCATION_SECRETARY|SERVICE_WORKER|", roles)
    }

    @Test
    fun `a single role is still wrapped in delimiters`() {
        val roles =
            rolesLoggedFor(
                AuthenticatedUser.Employee(EmployeeId(UUID.randomUUID()), setOf(UserRole.ADMIN))
            )
        assertEquals("|ADMIN|", roles)
    }

    @Test
    fun `an employee without any role logs nothing`() {
        assertNull(
            rolesLoggedFor(AuthenticatedUser.Employee(EmployeeId(UUID.randomUUID()), emptySet()))
        )
    }

    @Test
    fun `a citizen logs no roles`() {
        assertNull(
            rolesLoggedFor(
                AuthenticatedUser.Citizen(PersonId(UUID.randomUUID()), CitizenAuthLevel.STRONG)
            )
        )
    }

    @Test
    fun `a mobile device logs no roles`() {
        assertNull(
            rolesLoggedFor(
                AuthenticatedUser.MobileDevice(
                    MobileDeviceId(UUID.randomUUID()),
                    EmployeeId(UUID.randomUUID()),
                )
            )
        )
    }

    @Test
    fun `roles are cleared once the request completes`() {
        val _ =
            rolesLoggedFor(
                AuthenticatedUser.Employee(EmployeeId(UUID.randomUUID()), setOf(UserRole.ADMIN))
            )
        assertNull(MdcKey.USER_ROLES.get())
    }
}
