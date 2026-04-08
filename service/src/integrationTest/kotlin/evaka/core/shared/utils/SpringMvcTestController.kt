// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.shared.utils

import evaka.core.shared.auth.AuthenticatedUser
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/integration-test")
class SpringMvcTestController {
    @GetMapping("/require-auth") fun requireAuth(user: AuthenticatedUser) {}

    @GetMapping("/require-auth-employee") fun requireAuth(user: AuthenticatedUser.Employee) {}
}
