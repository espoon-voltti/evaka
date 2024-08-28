// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared

import com.google.common.hash.HashCode
import fi.espoo.evaka.shared.security.Action
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanBuilder
import io.opentelemetry.api.trace.Tracer
import java.util.UUID

fun noopTracer(): Tracer = OpenTelemetry.noop().getTracer("evaka-service")

object Tracing {
    val action = ToStringAttributeKey<Action>("action")
    val actionClass = ToStringAttributeKey<Class<out Any>>("actionclass")
    val enduserIdHash = ToStringAttributeKey<HashCode>("enduser.idhash")
    val evakaTraceId = AttributeKey.stringKey("evaka.traceid")
    val asyncJobId = ToStringAttributeKey<UUID>("asyncjob.id")
    val asyncJobRemainingAttempts = AttributeKey.longKey("asyncjob.remainingattempts")
    val headOfFamilyId = ToStringAttributeKey<PersonId>("headoffamily.id")
    // OTEL standard attribute:
    // https://opentelemetry.io/docs/specs/semconv/attributes-registry/exception/
    val exceptionEscaped = AttributeKey.booleanKey("exception.escaped")
}

@Suppress("NOTHING_TO_INLINE")
inline infix fun <T> AttributeKey<T>.withValue(value: T) = AttributeValue(this, value)

@Suppress("NOTHING_TO_INLINE")
inline infix fun <T> ToStringAttributeKey<T>.withValue(value: T) =
    AttributeValue(this.key, value.toString())

class ToStringAttributeKey<T>(key: String) {
    val key: AttributeKey<String> = AttributeKey.stringKey(key)
}

data class AttributeValue<T>(val key: AttributeKey<T>, val value: T)

fun <T> Span.setAttribute(attribute: ToStringAttributeKey<T>, value: T): Span =
    value?.let { setAttribute(attribute.key, it.toString()) } ?: this

fun <T> SpanBuilder.withAttribute(attribute: AttributeValue<T>): SpanBuilder =
    attribute.value?.let { setAttribute(attribute.key, it) } ?: this

inline fun <T> withSpan(span: Span, crossinline f: () -> T): T =
    span.makeCurrent().use {
        try {
            f()
        } catch (e: Exception) {
            span.recordException(e, Attributes.of(Tracing.exceptionEscaped, true))
            throw e
        } finally {
            span.end()
        }
    }

inline fun <T> Tracer.withSpan(
    operationName: String,
    vararg attributes: AttributeValue<*>,
    crossinline f: () -> T,
): T =
    withSpan(
        spanBuilder(operationName)
            .let { attributes.fold(it) { span, attribute -> span.withAttribute(attribute) } }
            .startSpan(),
        f,
    )

inline fun <T> Tracer.withDetachedSpan(
    operationName: String,
    vararg attributes: AttributeValue<*>,
    crossinline f: () -> T,
): T =
    withSpan(
        spanBuilder(operationName)
            .setNoParent()
            .let { attributes.fold(it) { span, attribute -> span.withAttribute(attribute) } }
            .startSpan(),
        f,
    )

// Generates a random 64-bit tracing ID in hex format
fun randomTracingId(): String = "%016x".format(UUID.randomUUID().leastSignificantBits)
