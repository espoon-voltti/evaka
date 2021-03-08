package fi.espoo.voltti.logging

import com.fasterxml.jackson.core.JsonStreamContext
import net.logstash.logback.mask.ValueMasker

class SsnMasker : ValueMasker {
    override fun mask(context: JsonStreamContext?, value: Any?): Any {
        return if (value is String) {
            value.replace(Regex("(?<!-|[\\dA-z])(\\d{2})(\\d{2})(\\d{2})[Aa+-](\\d{3})[\\dA-z](?!-)"), "REDACTED-SSN")
        } else value ?: "null"
    }
}
