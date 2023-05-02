// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.espoobi

import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.QuerySql
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.servlet.function.ServerResponse

object EspooBiPoc {
    val getAreas =
        streamingCsvRoute<BiArea> { sql("""
SELECT id, updated, name
FROM care_area
""") }

    val getUnits =
        streamingCsvRoute<BiUnit> {
            sql(
                """
SELECT
    id, updated, care_area_id AS area, name, provider_type, cost_center,
    'CLUB' = ANY(type) AS club, 'PRESCHOOL' = ANY(type) AS preschool, 'PREPARATORY_EDUCATION' = ANY(type) AS preparatory_education,
    (CASE WHEN 'GROUP_FAMILY' = ANY(type) THEN 'GROUP_FAMILY'
          WHEN 'FAMILY' = ANY(type) THEN 'FAMILY'
          WHEN 'CENTRE' = ANY(type) THEN 'DAYCARE'
     END) AS daycare
FROM daycare
"""
            )
        }

    val getGroups =
        streamingCsvRoute<BiGroup> {
            sql("""
SELECT id, name, start_date, end_date
FROM daycare_group
""")
        }

    val getChildren =
        streamingCsvRoute<BiChild> {
            sql(
                """
SELECT
    id, updated, date_of_birth AS birth_date, language, language_at_home,
    restricted_details_enabled AS vtj_non_disclosure, postal_code, post_office
FROM child
JOIN person USING (id)
"""
            )
        }

    val getPlacements =
        streamingCsvRoute<BiPlacement> {
            sql(
                """
SELECT id, updated, child_id AS child, unit_id AS unit, start_date, end_date, FALSE AS is_backup, type
FROM placement

UNION ALL

SELECT id, updated, child_id AS child, unit_id AS unit, start_date, end_date, TRUE AS is_backup, NULL AS type
FROM backup_care
"""
            )
        }

    val getGroupPlacements =
        streamingCsvRoute<BiGroupPlacement> {
            sql(
                """
SELECT id, updated, daycare_placement_id AS placement, daycare_group_id AS "group", start_date, end_date
FROM daycare_group_placement

UNION ALL

SELECT id, updated, id AS placement, group_id AS "group", start_date, end_date
FROM backup_care
WHERE group_id IS NOT NULL
"""
            )
        }
}

private fun printEspooBiCsvField(value: Any?): String =
    // Espoo BI tooling doesn't know how to handle RFC4180-style CSV double quote escapes, so our
    // only option is to remove quotes from the original data completely
    printCsvField(value).replace("\"", "")

typealias StreamingCsvRoute = (db: Database, user: AuthenticatedUser.Integration) -> ServerResponse

private inline fun <reified T : Any> streamingCsvRoute(
    crossinline f: QuerySql.Builder<T>.() -> QuerySql<T>
): StreamingCsvRoute = { db, _ ->
    ServerResponse.ok().build { _, response ->
        db.connect { dbc ->
            dbc.read { tx ->
                val records =
                    toCsvRecords(
                        ::printEspooBiCsvField,
                        T::class,
                        tx.createQuery { f() }.mapTo<T>().asSequence()
                    )
                val charset = CSV_CHARSET
                response.setHeader("Content-Type", "text/csv;charset=${charset.name()}")
                val writer = response.outputStream.bufferedWriter(charset)
                records.forEach {
                    writer.append(it)
                    writer.append(CSV_RECORD_SEPARATOR)
                }
                writer.flush()
            }
        }
        ModelAndView()
    }
}
