// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.espoobi

import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import jakarta.servlet.http.HttpServletResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping

@RequestMapping("/integration/espoo-bi-poc")
class EspooBiPoc {
    @GetMapping("/areas")
    fun getAreas(db: Database, user: AuthenticatedUser.Integration, response: HttpServletResponse) =
        csvQueryEndpoint<BiArea>(db, user, response) { sql("""
SELECT id, name
FROM care_area
""") }

    @GetMapping("/units")
    fun getUnits(db: Database, user: AuthenticatedUser.Integration, response: HttpServletResponse) =
        csvQueryEndpoint<BiUnit>(db, user, response) {
            sql("""
SELECT id, care_area_id AS area, name
FROM daycare
""")
        }
}
