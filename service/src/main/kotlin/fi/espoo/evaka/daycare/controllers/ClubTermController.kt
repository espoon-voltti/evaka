// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare.controllers

import fi.espoo.evaka.daycare.ClubTerm
import fi.espoo.evaka.daycare.getClubTerms
import fi.espoo.evaka.shared.db.Database
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class ClubTermController {

    @GetMapping("/public/club-terms")
    fun getClubTerms(db: Database.Connection): List<ClubTerm> {
        return db.read { it.getClubTerms() }
    }
}
