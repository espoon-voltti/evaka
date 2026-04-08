//  SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
//  SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.espoo

import evaka.core.Sensitive
import evaka.core.shared.auth.PasswordConstraints
import evaka.core.shared.auth.PasswordSpecification
import evaka.core.shared.auth.isPasswordBlacklisted
import evaka.core.shared.db.Database

class DefaultPasswordSpecification(private val constraints: PasswordConstraints) :
    PasswordSpecification {
    override fun constraints(): PasswordConstraints = constraints

    override fun isPasswordAcceptable(
        dbc: Database.Connection,
        password: Sensitive<String>,
    ): Boolean = dbc.read { !it.isPasswordBlacklisted(password) }
}
