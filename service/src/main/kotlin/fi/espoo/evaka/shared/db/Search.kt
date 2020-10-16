// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.db

import fi.espoo.evaka.identity.SSN_PATTERN
import fi.espoo.evaka.shared.utils.splitSearchText
import fi.espoo.evaka.shared.utils.stripAccent
import fi.espoo.evaka.shared.utils.stripNonAlphanumeric

data class DBQuery(val query: String, val params: Map<String, String>)

private val freeTextParamName = "free_text"
private val ssnParamName = { index: Int -> "ssn_$index" }
private val dateParamName = { index: Int -> "date_$index" }

fun freeTextSearchQuery(tables: List<String>, searchText: String): DBQuery {
    val ssnParams = findSsnParams(searchText)
    val dateParams = findDateParams(searchText)
    val freeTextString = searchText.let(removeSsnParams).let(removeDateParams)

    val freeTextQuery =
        if (freeTextString.isNotBlank()) freeTextQuery(tables, freeTextParamName)
        else null

    val ssnQuery =
        if (ssnParams.isNotEmpty()) ssnQuery(tables, ssnParams)
        else null

    val dateQuery =
        if (dateParams.isNotEmpty()) dateQuery(tables, dateParams)
        else null

    val wholeQuery = listOfNotNull("true", freeTextQuery, ssnQuery, dateQuery)
        .joinToString(" AND ")

    val allParams = (
        ssnParams.mapIndexed { index, param -> ssnParamName(index) to param } +
            dateParams.mapIndexed { index, param -> dateParamName(index) to param } +
            (freeTextParamName to freeTextParamsToTsQuery(freeTextString)).takeIf { freeTextString.isNotBlank() }
        ).filterNotNull().toMap()

    return DBQuery(wholeQuery, allParams)
}

fun disjointNumberQuery(table: String, column: String, params: Collection<String>): Pair<String, Map<String, String>> {
    val numberParamName = { index: Int -> "${table}_${column}_$index" }
    val numberParams = params.mapIndexed { index, param -> numberParamName(index) to param }.toMap()
    val numberParamQuery = params.mapIndexed { index, _ ->
        "$table.$column::text = :${numberParamName(index)}"
    }.joinToString(" OR ", "(", ")")

    return numberParamQuery to numberParams
}

private val freeTextSearchColumns =
    listOf("first_name", "last_name", "street_address", "postal_code")

private fun freeTextQuery(tables: List<String>, param: String): String {
    val tsVector = tables
        .flatMap { table ->
            freeTextSearchColumns.map { column -> "$table.$column" }
        }
        .map { column -> "to_tsvector('simple', coalesce(unaccent($column), ''))" }
        .joinToString("\n|| ", "(", ")")

    val tsQuery = "to_tsquery('simple', :$param)"

    return "($tsVector @@ $tsQuery)"
}

private fun findSsnParams(str: String) = splitSearchText(str).filter(SSN_PATTERN.toRegex()::matches)
private val removeSsnParams: (String) -> String = { it.replace(SSN_PATTERN.toRegex(), "") }

private fun ssnQuery(tablePrefixes: List<String>, params: Collection<String>): String {
    return params
        .mapIndexed { index, _ ->
            tablePrefixes
                .map { table -> "$table.social_security_number = :${ssnParamName(index)}" }
                .joinToString(" OR ", "(", ")")
        }
        .joinToString(" AND ", "(", ")")
}

private val dateRegex = "^\\d{6}$".toRegex()
private fun findDateParams(str: String) = splitSearchText(str).filter(dateRegex::matches)
private val removeDateParams: (String) -> String = { it.replace(dateRegex, "") }

private fun dateQuery(tables: List<String>, params: Collection<String>): String {
    return params
        .mapIndexed { index, _ ->
            tables
                .map { table -> "to_char($table.date_of_birth, 'DDMMYY') = :${dateParamName(index)}" }
                .joinToString(" OR ", "(", ")")
        }
        .joinToString(" AND ", "(", ")")
}

fun freeTextParamsToTsQuery(searchText: String): String = searchText
    .let(stripAccent)
    .let(stripNonAlphanumeric)
    .let(splitSearchText)
    .filter { it.isNotBlank() }
    .map { param -> "$param:*" }
    .joinToString(" & ")
