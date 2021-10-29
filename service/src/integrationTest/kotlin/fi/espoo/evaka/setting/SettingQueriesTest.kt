package fi.espoo.evaka.setting

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.resetDatabase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

class SettingQueriesTest : PureJdbiTest() {

    @AfterEach
    fun afterEach() {
        db.transaction { tx -> tx.resetDatabase() }
    }

    @Test
    fun getAndSet() {
        db.transaction { tx ->
            assertThat(tx.getSettings()).isEmpty()

            val firstMap = mapOf(SettingType.DECISION_MAKER_NAME to "Päivi Päätöksentekijä")
            tx.setSettings(firstMap)
            assertThat(tx.getSettings()).containsExactlyInAnyOrderEntriesOf(firstMap)

            val secondMap = mapOf(
                SettingType.DECISION_MAKER_NAME to "Pekka Päätöksentekijä",
                SettingType.DECISION_MAKER_TITLE to "Sijainen"
            )
            tx.setSettings(secondMap)
            assertThat(tx.getSettings()).containsExactlyInAnyOrderEntriesOf(secondMap)

            tx.setSettings(mapOf())
            assertThat(tx.getSettings()).isEmpty()
        }
    }
}
