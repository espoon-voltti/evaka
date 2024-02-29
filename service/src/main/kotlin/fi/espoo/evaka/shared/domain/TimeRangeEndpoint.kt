// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.domain

import java.time.LocalTime

sealed interface TimeRangeEndpoint : Comparable<TimeRangeEndpoint> {
    val inner: LocalTime

    fun asStart(): Start

    fun asEnd(): End

    fun isMidnight(): Boolean = this.inner == LocalTime.MIDNIGHT

    fun toDbString(): String

    /** 00:00:00 means midnight in the same day */
    data class Start(override val inner: LocalTime) : TimeRangeEndpoint {
        override fun compareTo(other: TimeRangeEndpoint): Int =
            when (other) {
                is Start -> this.inner.compareTo(other.inner)
                is End ->
                    if (this.inner == LocalTime.MIDNIGHT || other.inner == LocalTime.MIDNIGHT) -1
                    else this.inner.compareTo(other.inner)
            }

        override fun asStart() = this

        override fun asEnd(): End =
            if (this.inner == LocalTime.MIDNIGHT)
                throw IllegalArgumentException("Cannot convert midnight start to end")
            else End(inner)

        override fun toDbString(): String = inner.toString()
    }

    /** 00:00:00 means midnight in the next day */
    data class End(override val inner: LocalTime) : TimeRangeEndpoint {
        override fun compareTo(other: TimeRangeEndpoint): Int =
            when (other) {
                is Start ->
                    if (other.inner == LocalTime.MIDNIGHT || this.inner == LocalTime.MIDNIGHT) 1
                    else inner.compareTo(other.inner)
                is End ->
                    if (this.inner == LocalTime.MIDNIGHT && other.inner == LocalTime.MIDNIGHT) 0
                    else if (this.inner == LocalTime.MIDNIGHT) 1
                    else if (other.inner == LocalTime.MIDNIGHT) -1 else inner.compareTo(other.inner)
            }

        override fun asStart(): Start =
            if (this.inner == LocalTime.MIDNIGHT)
                throw IllegalArgumentException("Cannot convert midnight end to start")
            else Start(inner)

        override fun asEnd() = this

        override fun toDbString(): String =
            if (this.isMidnight())
                throw IllegalArgumentException("Cannot convert midnight end to db string")
            else inner.toString()

        companion object {
            val MAX = End(LocalTime.MAX)
        }
    }
}
