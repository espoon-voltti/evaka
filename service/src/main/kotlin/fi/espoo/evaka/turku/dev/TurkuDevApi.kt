// SPDX-FileCopyrightText: 2021 City of Turku
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.turku.dev

import fi.espoo.evaka.ExcludeCodeGen
import org.springframework.context.annotation.Profile
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Profile("enable_dev_api")
@RestController
@RequestMapping("/dev-api/turku")
@ExcludeCodeGen
class TurkuDevApi {
    @GetMapping fun healthCheck(): ResponseEntity<Unit> = ResponseEntity.noContent().build()
}
