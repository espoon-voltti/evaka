// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.db

import fi.espoo.evaka.shared.dev.ensureDevData
import io.opentracing.Tracer
import org.jdbi.v3.core.Jdbi

class DevDataInitializer(jdbi: Jdbi, tracer: Tracer) {
    init {
        Database(jdbi, tracer).connect { db -> db.transaction { tx -> tx.ensureDevData() } }
    }
}
