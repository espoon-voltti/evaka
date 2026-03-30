// SPDX-FileCopyrightText: 2021 City of Oulu
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.oulu.database

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.insert
import org.junit.jupiter.api.Test

class DevDataInitializerTest : PureJdbiTest(resetDbBeforeEach = true) {
    @Test
    fun init() {
        db.transaction { tx ->
            tx.insert(DevCareArea(shortName = "keskusta", name = "Keskusta"))
            tx.insert(DevCareArea(shortName = "kastelli", name = "Kastelli"))
            tx.insert(DevCareArea(shortName = "kiiminki", name = "Kiiminki"))
        }
        DevDataInitializer(jdbi)
    }
}
