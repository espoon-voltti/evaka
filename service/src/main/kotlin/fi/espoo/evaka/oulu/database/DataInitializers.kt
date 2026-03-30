// SPDX-FileCopyrightText: 2021 City of Oulu
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.oulu.database

import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.dev.runDevScript

fun Database.Transaction.ensureOuluDevData() {
    if (createQuery { sql("SELECT count(*) FROM daycare") }.exactlyOne<Int>() == 0) {
        listOf("oulu-dev-data.sql").forEach { runDevScript(it) }
    }
}

fun Database.Transaction.resetOuluDatabaseForE2ETests() {
    execute { sql("SELECT reset_oulu_database_for_e2e_tests()") }
}
