// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.db

import fi.espoo.evaka.identity.SSN_PATTERN
import fi.espoo.evaka.shared.utils.splitSearchText
import fi.espoo.evaka.shared.utils.stripAccent
import fi.espoo.evaka.shared.utils.stripNonAlphanumeric

/**
 * Returns a predicate that does a free text search using the given text on the given table names.
 *
 * The tables *must have* the following columns: social_security_number, date_of_birth, freetext_vec
 */
fun personFreeTextSearchPredicate(tables: Collection<String>, searchText: String): PredicateSql {
    val ssnPredicates =
        findSsnParams(searchText).map { ssn ->
            Predicate { where("lower($it.social_security_number) = lower(${bind(ssn)})") }
        }
    val datePredicates =
        findDateParams(searchText).map { date ->
            Predicate { where("to_char($it.date_of_birth, 'DDMMYY') = ${bind(date)}") }
        }
    val freeTextPredicate =
        searchText
            .let(removeSsnParams)
            .let(removeDateParams)
            .takeIf { it.isNotBlank() }
            ?.let(::freeTextParamsToTsQuery)
            ?.let { tsQuery ->
                Predicate { where("$it.freetext_vec @@ to_tsquery('simple', ${bind(tsQuery)})") }
            }

    val freeTextPredicateSql =
        if (freeTextPredicate != null)
            PredicateSql.any(tables.map { table -> freeTextPredicate.forTable(table) })
        else PredicateSql.alwaysTrue()

    val ssnPredicateSql =
        PredicateSql.all(
            ssnPredicates.map { predicate ->
                PredicateSql.any(tables.map { table -> predicate.forTable(table) })
            }
        )

    val datePredicateSql =
        PredicateSql.all(
            datePredicates.map { predicate ->
                PredicateSql.any(tables.map { table -> predicate.forTable(table) })
            }
        )

    return PredicateSql.all(freeTextPredicateSql, ssnPredicateSql, datePredicateSql)
}

fun freeTextSearchPredicate(tables: List<String>, searchText: String): PredicateSql {
    val ssnParams = findSsnParams(searchText)
    val dateParams = findDateParams(searchText)
    val freeTextString = searchText.let(removeSsnParams).let(removeDateParams)

    val freeText =
        if (freeTextString.isNotBlank()) {
            freeTextComputedColumnPredicate(tables, freeTextParamsToTsQuery(freeTextString))
        } else {
            null
        }

    val ssn =
        if (ssnParams.isNotEmpty()) {
            ssnPredicate(tables, ssnParams)
        } else {
            null
        }

    val date =
        if (dateParams.isNotEmpty()) {
            datePredicate(tables, dateParams)
        } else {
            null
        }

    return PredicateSql.allNotNull(freeText, ssn, date)
}

fun employeeFreeTextSearchPredicate(searchText: String): Predicate {
    val nameColumns = listOf("first_name", "last_name")
    val ssnPredicate =
        Predicate.all(
            findSsnParams(searchText).map { ssn ->
                Predicate { where("lower($it.social_security_number) = lower(${bind(ssn)})") }
            }
        )
    val freeTextPredicate =
        searchText
            .let(removeSsnParams)
            .takeIf { it.isNotBlank() }
            ?.let(::freeTextParamsToTsQuery)
            ?.let { tsQuery ->
                Predicate {
                    val tsVector =
                        nameColumns.joinToString(" || ") { column ->
                            "to_tsvector('simple', coalesce(unaccent($it.$column), ''))"
                        }
                    where("($tsVector) @@ to_tsquery('simple', ${bind(tsQuery)})")
                }
            }
    return Predicate.allNotNull(ssnPredicate, freeTextPredicate)
}

fun disjointNumberPredicate(table: String, column: String, params: Collection<String>) =
    PredicateSql.any(
        params.map { param -> PredicateSql { where("$table.$column::text = ${bind(param)}") } }
    )

private fun freeTextComputedColumnPredicate(tables: List<String>, query: String) =
    PredicateSql.any(
        tables.map { table ->
            PredicateSql { where("$table.freetext_vec @@ to_tsquery('simple', ${bind(query)})") }
        }
    )

private fun findSsnParams(str: String) =
    splitSearchText(str).filter(SSN_PATTERN.toRegex(RegexOption.IGNORE_CASE)::matches)

private val removeSsnParams: (String) -> String = {
    it.replace(SSN_PATTERN.toRegex(RegexOption.IGNORE_CASE), "")
}

private fun ssnPredicate(tablePrefixes: List<String>, params: Collection<String>) =
    PredicateSql.all(
        params.map { ssnParam ->
            PredicateSql.any(
                tablePrefixes.map { table ->
                    PredicateSql {
                        where("lower($table.social_security_number) = lower(${bind(ssnParam)})")
                    }
                }
            )
        }
    )

private val dateRegex = "^\\d{6}$".toRegex()

private fun findDateParams(str: String) = splitSearchText(str).filter(dateRegex::matches)

private val removeDateParams: (String) -> String = { it.replace(dateRegex, "") }

private fun datePredicate(tables: List<String>, params: Collection<String>) =
    PredicateSql.all(
        params.map { param ->
            PredicateSql.any(
                tables.map { table ->
                    PredicateSql {
                        where("to_char($table.date_of_birth, 'DDMMYY') = ${bind(param)}")
                    }
                }
            )
        }
    )

fun freeTextParamsToTsQuery(searchText: String): String =
    searchText
        .let(stripAccent)
        .let(stripNonAlphanumeric)
        .let(splitSearchText)
        .filter { it.isNotBlank() }
        .map { param -> "$param:*" }
        .joinToString(" & ")
