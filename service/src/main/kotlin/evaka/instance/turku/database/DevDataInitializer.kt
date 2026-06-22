// SPDX-FileCopyrightText: 2021 City of Turku
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.turku.database

import evaka.core.shared.db.Database
import evaka.core.shared.noopTracer
import org.jdbi.v3.core.Jdbi

class DevDataInitializer(jdbi: Jdbi) {
    init {
        Database(jdbi, noopTracer()).connect { db ->
            db.transaction { tx -> tx.ensureTurkuDevData() }
        }
    }
}
