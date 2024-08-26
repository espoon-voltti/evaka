// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.voltti.logging

import com.fasterxml.jackson.core.JsonStreamContext
import net.logstash.logback.mask.ValueMasker

class SsnMasker : ValueMasker {
    override fun mask(context: JsonStreamContext?, value: Any?): Any {
        return if (value is String) {
            value.replace(
                Regex(
                    "(?<!-|[\\dA-z])(\\d{2})(\\d{2})(\\d{2})[-+ABCDEFUVWXY](\\d{3})[\\dA-Z](?!-)",
                    RegexOption.IGNORE_CASE,
                ),
                "REDACTED-SSN",
            )
        } else {
            value ?: "null"
        }
    }
}
