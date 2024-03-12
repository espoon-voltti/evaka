// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.auth

import fi.espoo.evaka.shared.DaycareId

sealed class AclAuthorization {
    abstract fun isAuthorized(id: DaycareId): Boolean

    abstract val ids: Set<DaycareId>?

    data object All : AclAuthorization() {
        override fun isAuthorized(id: DaycareId): Boolean = true

        override val ids: Set<DaycareId>? = null
    }

    data class Subset(override val ids: Set<DaycareId>) : AclAuthorization() {
        override fun isAuthorized(id: DaycareId): Boolean = ids.contains(id)
    }

    fun isEmpty(): Boolean {
        return this is Subset && this.ids.isEmpty()
    }
}
