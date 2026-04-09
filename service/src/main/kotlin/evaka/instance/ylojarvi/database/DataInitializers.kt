// SPDX-FileCopyrightText: 2023-2024 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.ylojarvi.database

import evaka.core.shared.db.Database
import evaka.core.shared.dev.runDevScript

fun Database.Transaction.ensureYlojarviDevData() {
    if (createQuery { sql("SELECT count(*) FROM daycare") }.mapTo<Int>().exactlyOne() == 0) {
        listOf("ylojarvi-dev-data.sql").forEach { runDevScript(it) }
    }
}
