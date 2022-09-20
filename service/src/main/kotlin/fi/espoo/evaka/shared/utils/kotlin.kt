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
