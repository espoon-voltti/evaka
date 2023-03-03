// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.espoobi

import fi.espoo.evaka.shared.db.Database
import jakarta.servlet.http.HttpServletResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/public/espoo-bi")
class EspooBiPoc {
    @GetMapping("/areas", produces = ["text/csv;charset=UTF-8"])
    fun getAreas(db: Database, response: HttpServletResponse) =
        csvQueryEndpoint<BiArea>(db, response) { sql("""
SELECT id, name
FROM care_area
""") }

    @GetMapping("/units", produces = ["text/csv;charset=UTF-8"])
    fun getUnits(db: Database, response: HttpServletResponse) =
        csvQueryEndpoint<BiUnit>(db, response) {
            sql("""
SELECT id, care_area_id AS area, name
FROM daycare
""")
        }

    @GetMapping("/children", produces = ["text/csv;charset=UTF-8"])
    fun getChildren(db: Database, response: HttpServletResponse) =
        csvQueryEndpoint<BiChild>(db, response) {
            sql(
                """
SELECT id, first_name, last_name
FROM person
WHERE EXISTS (
    SELECT 1
    FROM placement
    WHERE child_id = person.id
) OR EXISTS (
    SELECT 1
    FROM application
    WHERE child_id = person.id
)
"""
            )
        }

    @GetMapping("/placement", produces = ["text/csv;charset=UTF-8"])
    fun getPlacements(db: Database, response: HttpServletResponse) =
        csvQueryEndpoint<BiPlacement>(db, response) {
            sql(
                """
SELECT id, child_id AS child, unit_id AS unit, type, start_date, end_date
FROM placement
"""
            )
        }
}
