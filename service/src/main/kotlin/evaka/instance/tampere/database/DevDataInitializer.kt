// SPDX-FileCopyrightText: 2021 City of Tampere
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.tampere.database

import evaka.core.shared.db.Database
import evaka.core.shared.dev.runSqlScript
import evaka.core.shared.noopTracer
import evaka.core.vtjclient.service.persondetails.MockPersonDetailsService
import evaka.core.vtjclient.service.persondetails.legacyMockVtjDataset
import org.jdbi.v3.core.Jdbi
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("local")
class DevDataInitializer(jdbi: Jdbi) {
    init {
        Database(jdbi, noopTracer()).connect { db ->
            db.transaction { tx ->
                tx.runSqlScript("tampere/reset-tampere-database-for-e2e-tests.sql")
                tx.ensureTampereDevData()
            }
        }
        MockPersonDetailsService.add(legacyMockVtjDataset())
    }
}
