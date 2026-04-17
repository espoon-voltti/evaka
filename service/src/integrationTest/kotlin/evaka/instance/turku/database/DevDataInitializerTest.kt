// SPDX-FileCopyrightText: 2021 City of Turku
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.turku.database

import evaka.core.PureJdbiTest
import evaka.core.shared.dev.DevCareArea
import evaka.core.shared.dev.insert
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
