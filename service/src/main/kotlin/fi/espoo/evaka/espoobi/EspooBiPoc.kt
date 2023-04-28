// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.espoobi

import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.QuerySql
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.servlet.function.ServerResponse

class EspooBiPoc {
    val getAreas = streamingCsvRoute<BiArea> { sql("""
SELECT id, name
FROM care_area
""") }

    val getUnits =
        streamingCsvRoute<BiUnit> {
            sql("""
    SELECT id, care_area_id AS area, name
    FROM daycare
    """)
        }
}

private fun printEspooBiCsvField(value: Any?): String =
    // Espoo BI tooling doesn't know how to handle RFC4180-style CSV double quote escapes, so our
    // only option is to remove quotes from the original data completely
    printCsvField(value).replace("\"", "")

private inline fun <reified T : Any> streamingCsvRoute(
    crossinline f: QuerySql.Builder<T>.() -> QuerySql<T>
): (db: Database, user: AuthenticatedUser.Integration) -> ServerResponse = { db, _ ->
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
