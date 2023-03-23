// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.espoobi

class EspooBiPoc {
    val getAreas = streamingCsvQuery<BiArea> { sql("""
SELECT id, name
FROM care_area
""") }

    val getUnits =
        streamingCsvQuery<BiUnit> {
            sql("""
SELECT id, care_area_id AS area, name
FROM daycare
""")
        }
}
