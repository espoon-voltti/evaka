// SPDX-FileCopyrightText: 2021 City of Turku
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.turku.emailclient.config

import fi.espoo.evaka.daycare.domain.Language
import fi.espoo.evaka.emailclient.EmailContent
import fi.espoo.evaka.turku.TurkuEmailMessageProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class EmailMessageProviderTest {
    private val emailMessageProvider = TurkuEmailMessageProvider()

    @Test
    fun testNonPreschoolMessagesDoNotContainEspooText() {
        assertNotContainEspooText(emailMessageProvider.daycareApplicationReceived(Language.fi))
        assertNotContainEspooText(emailMessageProvider.clubApplicationReceived(Language.fi))
        assertNotContainEspooText(emailMessageProvider.pendingDecisionNotification(Language.fi))
    }

    private fun assertNotContainEspooText(content: EmailContent) {
        assertThat(content.subject.also(::println))
            .isNotBlank
            .doesNotContainIgnoringCase("espoo")
            .doesNotContainIgnoringCase("esbo")

        assertThat(content.text.also(::println))
            .isNotBlank
            .doesNotContainIgnoringCase("espoo")
            .doesNotContainIgnoringCase("esbo")

        assertThat(content.html.also(::println))
            .isNotBlank
            .doesNotContainIgnoringCase("espoo")
            .doesNotContainIgnoringCase("esbo")
    }
}
