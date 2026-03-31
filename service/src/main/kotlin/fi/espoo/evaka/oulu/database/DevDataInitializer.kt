// SPDX-FileCopyrightText: 2021 City of Oulu
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.oulu.database

import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.noopTracer
import fi.espoo.evaka.vtjclient.service.persondetails.MockPersonDetailsService
import fi.espoo.evaka.vtjclient.service.persondetails.legacyMockVtjDataset
import org.jdbi.v3.core.Jdbi

class DevDataInitializer(jdbi: Jdbi) {
    init {
        Database(jdbi, noopTracer()).connect { db ->
            db.transaction { tx -> tx.ensureOuluDevData() }
        }
        MockPersonDetailsService.add(legacyMockVtjDataset())
    }
}
