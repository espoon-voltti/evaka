// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.utils

import java.text.Normalizer

private val REGEX_UNACCENT = "\\p{InCombiningDiacriticalMarks}+".toRegex()
/**
 * Converts accented characters into non-accented equivalents.
 */
val stripAccent: (String) -> String = {
    val temp = Normalizer.normalize(it, Normalizer.Form.NFD)
    REGEX_UNACCENT.replace(temp, "")
}

private val REGEX_NON_ALPHANUMERIC = "[^\\w]".toRegex()
/**
 * Completely strips all non-alphanumeric characters and replaces them with white space so the words get split afterwards.
 */
val stripNonAlphanumeric: (String) -> String = { REGEX_NON_ALPHANUMERIC.replace(it, " ") }

// Match ALL Unicode whitespaces with the embeddable switch (?U).
// Otherwise some more obscure spaces are not matched (see: https://www.unicode.org/Public/UCD/latest/ucd/PropList.txt)
private val whitespaceRegex =
    """(?U)\s+""".toRegex()

val splitSearchText: (String) -> List<String> = { it.split(whitespaceRegex) }
