// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.espoo.bi

import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID
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
        is Boolean -> if (value) "true" else "false"
        is UUID -> value.toString()
        is LocalDate -> value.format(DateTimeFormatter.ISO_LOCAL_DATE)
        is HelsinkiDateTime ->
            value.toZonedDateTime().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        is List<*> -> value.joinToString(",")
        is Enum<*> -> value.name
        else -> error("Unsupported CSV field type ${value.javaClass}")
    }

fun <T : Any> toCsvRecords(
    printField: (value: Any?) -> String,
    clazz: KClass<T>,
    values: Sequence<T>,
): Sequence<String> {
    check(clazz.isData)
    val props = clazz.declaredMemberProperties.toList()
    val header =
        props.joinToString(CSV_FIELD_SEPARATOR, postfix = CSV_RECORD_SEPARATOR) {
            CsvEscape.escapeCsv(it.name)
        }
    return sequenceOf(header) +
        values.map { record ->
            props.joinToString(CSV_FIELD_SEPARATOR, postfix = CSV_RECORD_SEPARATOR) {
                CsvEscape.escapeCsv(printField(it.get(record)))
            }
        }
}
