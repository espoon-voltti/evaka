// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.data

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import fi.espoo.evaka.shared.domain.LocalHm
import fi.espoo.evaka.shared.domain.LocalHmRange
import java.util.Objects

/**
 * An immutable data structure that is conceptually similar to a `Set<LocalHm>` but provides batch
 * operations that use `LocalHmRange` parameters.
 */
@JsonSerialize(using = LocalHmSetJsonSerializer::class)
class LocalHmSet private constructor(ranges: List<LocalHmRange>) :
    RangeBasedSet<LocalHm, LocalHmRange, LocalHmSet>(ranges) {
    override fun List<LocalHmRange>.toThis(): LocalHmSet =
        if (isEmpty()) EMPTY else LocalHmSet(this)

    override fun range(start: LocalHm, end: LocalHm): LocalHmRange = LocalHmRange(start, end)

    override fun equals(other: Any?): Boolean = other is LocalHmSet && this.ranges == other.ranges

    override fun hashCode(): Int = Objects.hash(ranges)

    override fun toString(): String =
        ranges.joinToString(separator = ",", prefix = "{", postfix = "}")

    companion object {
        private val EMPTY = LocalHmSet(emptyList())
        /** Returns an empty date set */
        fun empty(): LocalHmSet = EMPTY
        /** Returns a new date set containing all the given ranges */
        fun of(vararg ranges: LocalHmRange): LocalHmSet = empty().addAll(ranges.asSequence())
        /** Returns a new date set containing all the given ranges */
        fun of(ranges: Iterable<LocalHmRange>): LocalHmSet = empty().addAll(ranges)
        /** Returns a new date set containing all the given ranges */
        fun of(ranges: Sequence<LocalHmRange>): LocalHmSet = empty().addAll(ranges)
    }
}

class LocalHmSetJsonSerializer : JsonSerializer<LocalHmSet>() {
    override fun serialize(value: LocalHmSet, gen: JsonGenerator, serializers: SerializerProvider) {
        return serializers.defaultSerializeValue(value.ranges().toList(), gen)
    }
}
