// SPDX-FileCopyrightText: 2021 City of Turku
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.turku.database

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.insert
import org.junit.jupiter.api.Test

class DevDataInitializerTest : PureJdbiTest(resetDbBeforeEach = true) {
    @Test
    fun init() {
        db.transaction { tx ->
            tx.insert(DevCareArea(shortName = "lantinen", name = "Läntinen"))
            tx.insert(DevCareArea(shortName = "etelainen", name = "Eteläinen"))
            tx.insert(DevCareArea(shortName = "itainen", name = "Itäinen"))
        }
        DevDataInitializer(jdbi)
    }
}
