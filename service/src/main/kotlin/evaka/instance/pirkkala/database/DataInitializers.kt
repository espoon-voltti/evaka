// SPDX-FileCopyrightText: 2023-2024 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.pirkkala.database

import evaka.core.shared.db.Database
import evaka.core.shared.dev.runSqlScript

fun Database.Transaction.ensurePirkkalaDevData() {
    if (createQuery { sql("SELECT count(*) FROM daycare") }.mapTo<Int>().exactlyOne() == 0) {
        listOf("pirkkala/dev-data.sql").forEach { runSqlScript(it) }
    }
}
