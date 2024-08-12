// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.utils

import java.util.EnumSet

inline fun <T> T.applyIf(condition: Boolean, block: T.() -> Unit): T =
    if (condition) this.apply(block) else this

inline fun <reified T : Enum<T>> Array<out T>.toEnumSet(): EnumSet<T> =
    emptyEnumSet<T>().also { it += this }

inline fun <reified T : Enum<T>> Iterable<T>.toEnumSet(): EnumSet<T> =
    emptyEnumSet<T>().also { it += this }

inline fun <reified T : Enum<T>> Sequence<T>.toEnumSet(): EnumSet<T> =
    emptyEnumSet<T>().also { it += this }

inline fun <reified T : Enum<T>> emptyEnumSet(): EnumSet<T> = EnumSet.noneOf(T::class.java)

inline fun <reified T : Enum<T>> enumSetOf(vararg elements: T): EnumSet<T> =
    emptyEnumSet<T>().also { it += elements }

fun <K, V> mapOfNotNullValues(vararg pairs: Pair<K, V?>): Map<K, V> =
    pairs.mapNotNull { if (it.second != null) Pair(it.first, it.second!!) else null }.toMap()

/**
 * Memoizes a 1-argument function using a *non-thread-safe* cache.
 *
 * Nullable result types are not supported to prevent confusion between "not found" and "found but
 * null".
 */
fun <T, R : Any> memoize(f: (T) -> R): (T) -> R = run {
    val originalThread = Thread.currentThread().threadId()
    val cache: MutableMap<T, R> = mutableMapOf()
    return@run { input ->
        assert(Thread.currentThread().threadId() == originalThread) {
            "Memoized function accessed from the wrong thread"
        }
        cache.getOrPut(input) { f(input) }
    }
}
