// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.espoobi

import fi.espoo.evaka.shared.Id
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.QuerySql
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredMemberProperties
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.servlet.function.ServerResponse
import org.unbescape.csv.CsvEscape

val CSV_CHARSET = Charsets.UTF_8
const val CSV_FIELD_SEPARATOR = ","
const val CSV_RECORD_SEPARATOR = "\r\n"

private fun formatCsvField(value: Any?): String =
    when (value) {
        null -> ""
        is Number -> value.toString()
        is String -> value
        is Id<*> -> value.raw.toString()
        is LocalDate -> value.format(DateTimeFormatter.ISO_LOCAL_DATE)
        is Enum<*> -> value.name
        else -> error("Unsupported CSV field type ${value.javaClass}")
    }

inline fun <reified T : Any> streamingCsvQuery(
    crossinline f: QuerySql.Builder<T>.() -> QuerySql<T>
): (db: Database, user: AuthenticatedUser.Integration) -> ServerResponse = { db, _ ->
    ServerResponse.ok().build { _, response ->
        db.connect { dbc ->
            dbc.read { tx ->
                val records = toCsvRecords(tx.createQuery { f() }.mapTo<T>().asSequence())
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

inline fun <reified T : Any> toCsvRecords(sequence: Sequence<T>): Sequence<String> =
    toCsvRecords(T::class, sequence)

fun <T : Any> toCsvRecords(clazz: KClass<T>, values: Sequence<T>): Sequence<String> {
    check(clazz.isData)
    val props = clazz.declaredMemberProperties.toList()
    val header = props.joinToString(CSV_FIELD_SEPARATOR) { CsvEscape.escapeCsv(it.name) }
    return sequenceOf(header) +
        values.map { record ->
            props.joinToString(CSV_FIELD_SEPARATOR) {
                CsvEscape.escapeCsv(formatCsvField(it.get(record)))
            }
        }
}
