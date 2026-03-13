// SPDX-FileCopyrightText: 2021 City of Turku
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.turku.emailclient.config

import fi.espoo.evaka.EvakaEnv
import fi.espoo.evaka.daycare.domain.Language
import fi.espoo.evaka.emailclient.EmailContent
import fi.espoo.evaka.emailclient.IEmailMessageProvider
import fi.espoo.evaka.shared.domain.Rectangle
import java.time.LocalDate
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class EmailMessageProviderTest {
    private val emailMessageProvider: IEmailMessageProvider =
        EmailMessageProvider(
            EvakaEnv(
                koskiEnabled = false,
                sfiEnabled = false,
                vtjEnabled = false,
                webPushEnabled = false,
                jamixEnabled = false,
                aromiEnabled = false,
                archivalEnabled = false,
                nekkuEnabled = false,
                forceUnpublishDocumentTemplateEnabled = false,
                asyncJobRunnerDisabled = false,
                frontendBaseUrlFi = "https://turku-evaka.test",
                frontendBaseUrlSv = "https://turku-evaka.test",
                feeDecisionMinDate = LocalDate.of(2020, 1, 1),
                maxAttachmentsPerUser = 10,
                mockClock = false,
                nrOfDaysFeeDecisionCanBeSentInAdvance = 0,
                nrOfDaysVoucherValueDecisionCanBeSentInAdvance = 0,
                plannedAbsenceEnabledForHourBasedServiceNeeds = false,
                personAddressEnvelopeWindowPosition = Rectangle(0, 0, 0, 0),
                replacementInvoicesStart = null,
                passwordBlacklistDirectory = null,
                placementToolServiceNeedOptionId = null,
                newBrowserLoginEmailEnabled = false,
            )
        )

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
