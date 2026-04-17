// SPDX-FileCopyrightText: 2021 City of Oulu
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.oulu.database

import evaka.core.shared.db.Database
import evaka.core.shared.dev.runSqlScript

fun Database.Transaction.ensureOuluDevData() {
    if (createQuery { sql("SELECT count(*) FROM daycare") }.exactlyOne<Int>() == 0) {
        listOf("oulu/dev-data.sql").forEach { runSqlScript(it) }
    }
}
