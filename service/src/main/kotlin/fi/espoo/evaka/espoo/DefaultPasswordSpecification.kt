//  SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
//  SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.espoo

import fi.espoo.evaka.Sensitive
import fi.espoo.evaka.shared.auth.PasswordConstraints
import fi.espoo.evaka.shared.auth.PasswordSpecification
import fi.espoo.evaka.shared.auth.isPasswordBlacklisted
import fi.espoo.evaka.shared.db.Database

class DefaultPasswordSpecification(private val constraints: PasswordConstraints) :
    PasswordSpecification {
    override fun constraints(): PasswordConstraints = constraints

    override fun isPasswordAcceptable(
        dbc: Database.Connection,
        password: Sensitive<String>,
    ): Boolean = dbc.read { !it.isPasswordBlacklisted(password) }
}
