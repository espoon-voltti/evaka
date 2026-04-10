// SPDX-FileCopyrightText: 2021 City of Tampere
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.tampere.database

import evaka.core.shared.db.Database
import evaka.core.shared.dev.runSqlScript

fun Database.Transaction.ensureTampereDevData() {
    if (createQuery { sql("SELECT count(*) FROM daycare") }.mapTo<Int>().exactlyOne() == 0) {
        listOf("tampere/dev-data.sql").forEach { runSqlScript(it) }
    }
}

fun Database.Transaction.resetTampereDatabaseForE2ETests() {
    execute { sql("SELECT reset_tampere_database_for_e2e_tests()") }
    execute {
        sql(
            "INSERT INTO evaka_user (id, type, name) VALUES ('00000000-0000-0000-0000-000000000000', 'SYSTEM', 'eVaka')"
        )
    }
}
