// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.setting

import fi.espoo.evaka.PureJdbiTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SettingQueriesTest : PureJdbiTest(resetDbBeforeEach = true) {
    @Test
    fun getAndSet() {
        db.transaction { tx ->
            assertThat(tx.getSettings()).isEmpty()

            val firstMap = mapOf(SettingType.DECISION_MAKER_NAME to "Päivi Päätöksentekijä")
            tx.setSettings(firstMap)
            assertThat(tx.getSettings()).containsExactlyInAnyOrderEntriesOf(firstMap)

            val secondMap =
                mapOf(
                    SettingType.DECISION_MAKER_NAME to "Pekka Päätöksentekijä",
                    SettingType.DECISION_MAKER_TITLE to "Sijainen",
                )
            tx.setSettings(secondMap)
            assertThat(tx.getSettings()).containsExactlyInAnyOrderEntriesOf(secondMap)

            tx.setSettings(mapOf())
            assertThat(tx.getSettings()).isEmpty()
        }
    }
}
