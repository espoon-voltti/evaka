// SPDX-FileCopyrightText: 2021 City of Turku
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.turku.database

import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.noopTracer
import fi.espoo.evaka.vtjclient.service.persondetails.MockPersonDetailsService
import fi.espoo.evaka.vtjclient.service.persondetails.legacyMockVtjDataset
import org.jdbi.v3.core.Jdbi

class DevDataInitializer(jdbi: Jdbi) {
    init {
        Database(jdbi, noopTracer()).connect { db ->
            db.transaction { tx -> tx.ensureTurkuDevData() }
        }
        MockPersonDetailsService.add(legacyMockVtjDataset())
    }
}
