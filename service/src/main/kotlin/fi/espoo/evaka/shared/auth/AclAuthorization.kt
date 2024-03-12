// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.auth

sealed class AclAuthorization<in T> {
    abstract fun isAuthorized(id: T): Boolean

    data object All : AclAuthorization<Any>() {
        override fun isAuthorized(id: Any): Boolean = true
    }

    data class Subset<T>(val ids: Set<T>) : AclAuthorization<T>() {
        override fun isAuthorized(id: T): Boolean = ids.contains(id)
    }
}
