// SPDX-FileCopyrightText: 2024 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.vesilahti.database

import evaka.core.shared.db.Database
import evaka.core.shared.dev.runDevScript
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
                tx.runDevScript("reset-tampere-database-for-e2e-tests.sql")
                tx.ensureVesilahtiDevData()
            }
        }
        MockPersonDetailsService.add(legacyMockVtjDataset())
    }
}
