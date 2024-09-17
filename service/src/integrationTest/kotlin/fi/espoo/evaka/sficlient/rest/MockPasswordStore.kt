// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.sficlient.rest

import fi.espoo.evaka.Sensitive
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class MockPasswordStore(initialPassword: String) : PasswordStore {
    private val lock = ReentrantLock()
    private val passwords: MutableList<String> = mutableListOf(initialPassword)
    private var indexByLabel: MutableMap<PasswordStore.Label, Int> =
        mutableMapOf(PasswordStore.Label.CURRENT to 0)

    override fun getPassword(label: PasswordStore.Label): PasswordStore.VersionedPassword? =
        lock.withLock {
            val index = indexByLabel[label] ?: return null
            PasswordStore.VersionedPassword(
                Sensitive(passwords[index]),
                PasswordStore.Version(index.toLong()),
            )
        }

    override fun putPassword(password: Sensitive<String>): PasswordStore.Version =
        lock.withLock {
            val index = passwords.size
            passwords += password.value
            PasswordStore.Version(index.toLong())
        }

    override fun moveLabel(version: PasswordStore.Version, label: PasswordStore.Label) =
        lock.withLock { indexByLabel[label] = version.value.toInt() }
}
