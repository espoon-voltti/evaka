// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.db

import fi.espoo.evaka.shared.DatabaseTable
import fi.espoo.evaka.shared.Id

class AuditContext {
    @PublishedApi internal val ids = mutableMapOf<String, MutableSet<Id<*>>>()

    inline fun <reified T : DatabaseTable> add(id: Id<T>): AuditContext {
        val key = T::class.simpleName!!.replaceFirstChar { it.lowercase() } + "Id"
        ids.getOrPut(key) { mutableSetOf() }.add(id)
        return this
    }

    inline fun <reified T : DatabaseTable> add(ids: Collection<Id<T>>): AuditContext {
        if (ids.isEmpty()) return this
        val key = T::class.simpleName!!.replaceFirstChar { it.lowercase() } + "Id"
        this.ids.getOrPut(key) { mutableSetOf() }.addAll(ids)
        return this
    }

    val context: Map<String, Set<Id<*>>>
        get() = ids.mapValues { it.value.toSet() }
}
