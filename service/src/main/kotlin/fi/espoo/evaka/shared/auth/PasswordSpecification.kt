//  SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
//  SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.auth

import fi.espoo.evaka.Sensitive
import fi.espoo.evaka.shared.db.Database

interface PasswordSpecification {
    /** Returns the structural constraints for new passwords. */
    fun constraints(): PasswordConstraints

    /**
     * Checks if an otherwise structurally valid password is acceptable.
     *
     * A password could be rejected for example if it's included in some list of too easily
     * guessable passwords
     */
    fun isPasswordAcceptable(dbc: Database.Connection, password: Sensitive<String>): Boolean = true
}
