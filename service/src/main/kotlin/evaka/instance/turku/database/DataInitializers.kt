// SPDX-FileCopyrightText: 2021 City of Turku
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.turku.database

import evaka.core.shared.db.Database
import evaka.core.shared.dev.runSqlScript

fun Database.Transaction.ensureTurkuDevData() {
    if (createQuery { sql("SELECT count(*) FROM daycare") }.mapTo<Int>().exactlyOne() == 0) {
        listOf("turku/dev-data.sql").forEach { runSqlScript(it) }
    }
}
