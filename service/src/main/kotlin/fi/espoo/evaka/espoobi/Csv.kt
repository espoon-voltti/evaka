// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.espoobi

import fi.espoo.evaka.shared.Id
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredMemberProperties
import org.unbescape.csv.CsvEscape

val CSV_CHARSET = Charsets.UTF_8
const val CSV_FIELD_SEPARATOR = ","
const val CSV_RECORD_SEPARATOR = "\r\n"

fun printCsvField(value: Any?): String =
    when (value) {
        null -> ""
        is Number -> value.toString()
        is String -> value
        is Id<*> -> value.raw.toString()
        is LocalDate -> value.format(DateTimeFormatter.ISO_LOCAL_DATE)
        is Enum<*> -> value.name
        else -> error("Unsupported CSV field type ${value.javaClass}")
    }

fun <T : Any> toCsvRecords(
    printField: (value: Any?) -> String,
    clazz: KClass<T>,
    values: Sequence<T>
): Sequence<String> {
    check(clazz.isData)
    val props = clazz.declaredMemberProperties.toList()
    val header = props.joinToString(CSV_FIELD_SEPARATOR) { CsvEscape.escapeCsv(it.name) }
    return sequenceOf(header) +
        values.map { record ->
            props.joinToString(CSV_FIELD_SEPARATOR) {
                CsvEscape.escapeCsv(printField(it.get(record)))
            }
        }
}
