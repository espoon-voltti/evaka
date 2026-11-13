// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.voltti.logging

import net.logstash.logback.mask.ValueMasker
import tools.jackson.core.TokenStreamContext

class SsnMasker : ValueMasker {
    override fun mask(context: TokenStreamContext?, value: Any?): Any {
        if (value !is String) return value ?: "null"
        val isGeneratedHexId =
            context?.currentName() in generatedHexFields && generatedHexId.matches(value)
        return if (isGeneratedHexId) value else value.replace(ssnRegex, "REDACTED-SSN")
    }

    companion object {
        private val ssnRegex =
            Regex(
                "(?<!-|[\\dA-z])(\\d{2})(\\d{2})(\\d{2})[-+ABCDEFUVWXY](\\d{3})[\\dA-Z](?!-)",
                RegexOption.IGNORE_CASE,
            )

        // Random IDs, hashes and commit SHAs never contain SSNs, but ~0.5% of them match the
        // pattern.
        private val generatedHexFields =
            setOf(
                MdcKey.TRACE_ID.key,
                MdcKey.SPAN_ID.key,
                MdcKey.USER_ID_HASH.key,
                MdcKey.SECONDARY_USER_ID_HASH.key,
                "appCommit",
            )
        private val generatedHexId = Regex("[0-9a-f]{16,64}")
    }
}
