// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.shared.db

import evaka.core.shared.dev.ensureDevData
import evaka.core.shared.dev.runDevScript
import evaka.core.vtjclient.service.persondetails.MockPersonDetailsService
import evaka.core.vtjclient.service.persondetails.legacyMockVtjDataset
import io.opentelemetry.api.trace.Tracer
import org.jdbi.v3.core.Jdbi

class DevDataInitializer(jdbi: Jdbi, tracer: Tracer) {
    init {
        Database(jdbi, tracer).connect { db ->
            db.transaction { tx ->
                tx.runDevScript("lock-database-nowait.sql")
                tx.runDevScript("reset-database.sql")
                tx.ensureDevData()
            }
        }
        MockPersonDetailsService.add(legacyMockVtjDataset())
    }
}
