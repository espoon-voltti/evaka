// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.db

import fi.espoo.evaka.shared.dev.ensureDevData
import fi.espoo.evaka.shared.dev.runDevScript
import org.jdbi.v3.core.Jdbi

class DevDataInitializer(jdbi: Jdbi) {
    init {
        Database(jdbi).connect { db ->
            db.transaction { tx ->
                tx.runDevScript("reset-database.sql")
                tx.ensureDevData()
            }
        }
    }
}
