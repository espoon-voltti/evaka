// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.espoobi

import fi.espoo.evaka.shared.Id
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.QuerySql
import jakarta.servlet.http.HttpServletResponse
import java.time.LocalDate
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredMemberProperties
import org.unbescape.csv.CsvEscape

inline fun <reified T : Any> csvQueryEndpoint(
    db: Database,
    response: HttpServletResponse,
    crossinline f: QuerySql.Builder<T>.() -> QuerySql<T>
) =
    db.connect { dbc ->
        dbc.read { tx ->
            val rows = toCsv(tx.createQuery { f() }.mapTo<T>().asSequence())
            response.setHeader("Content-Type", "text/csv;charset=UTF-8")
            val writer = response.outputStream.bufferedWriter(Charsets.UTF_8)
            rows.forEach {
                writer.append(it)
                writer.append('\n')
            }
            writer.flush()
        }
    }

inline fun <reified T : Any> toCsv(sequence: Sequence<T>): Sequence<String> =
    toCsv(T::class, sequence)

fun <T : Any> toCsv(clazz: KClass<T>, sequence: Sequence<T>): Sequence<String> {
    check(clazz.isData)
    val props = clazz.declaredMemberProperties.toList()
    val header = props.joinToString(",") { CsvEscape.escapeCsv(it.name) }
    return sequenceOf(header) +
        sequence.map { row ->
            props.joinToString(",") { CsvEscape.escapeCsv(formatCsvField(it.get(row))) }
        }
}

private fun formatCsvField(value: Any?): String =
    when (value) {
        null -> ""
        is Number -> value.toString()
        is String -> value
        is Id<*> -> value.raw.toString()
        is LocalDate -> value.toString()
        is Enum<*> -> value.name
        else -> error("Unsupported CSV field type ${value.javaClass}")
    }
