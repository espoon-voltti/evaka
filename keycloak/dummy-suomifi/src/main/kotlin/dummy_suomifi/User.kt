// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package dummy_suomifi

import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

data class User(
    val ssn: String,
    val commonName: String,
    val givenName: String,
    val surname: String
) {
    val displayName: String
        get() = "$givenName $surname"

    // Tunnistetusta käyttäjästä välitettävät attribuutit, Suppea, väline loa2/loa3
    // (verkkopankkitunnukset, mobiilivarmenne tai varmennekortti)
    // https://palveluhallinta.suomi.fi/fi/tuki/artikkelit/590ad07b14bbb10001966f50
    fun samlAttributes(): List<UserAttribute> =
        listOf(
            UserAttribute("nationalIdentificationNumber", "urn:oid:1.2.246.21", ssn),
            UserAttribute("cn", "urn:oid:2.5.4.3", commonName),
            UserAttribute("displayName", "urn:oid:2.16.840.1.113730.3.1.241", displayName),
            UserAttribute("givenName", "urn:oid:2.5.4.42", givenName),
            UserAttribute("sn", "urn:oid:2.5.4.4", surname),
        )
}

data class UserAttribute(val friendlyName: String, val urn: String, val value: String)

private val defaultUser =
    User(
        ssn = "120482-955X",
        commonName = "Tammi Tauno Matias",
        givenName = "Tauno",
        surname = "Tammi"
    )

class UserStore {
    private val lock = ReentrantLock()

    private val users = mutableListOf(defaultUser)

    fun reset() = lock.withLock { users.clear() }

    fun getAll() = lock.withLock { users.toList() }

    fun add(user: User) = lock.withLock { users.add(user) }
}
