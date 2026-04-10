// SPDX-FileCopyrightText: 2024 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.orivesi.database

import evaka.core.shared.db.Database
import evaka.core.shared.dev.runSqlScript

fun Database.Transaction.ensureOrivesiDevData() {
    if (createQuery { sql("SELECT count(*) FROM daycare") }.mapTo<Int>().exactlyOne() == 0) {
        listOf("orivesi/dev-data.sql").forEach { runSqlScript(it) }
    }
}
