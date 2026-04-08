// SPDX-FileCopyrightText: 2021 City of Turku
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.turku.emailclient.config

import evaka.core.daycare.domain.Language
import evaka.core.emailclient.EmailContent
import evaka.instance.turku.TurkuEmailMessageProvider
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
