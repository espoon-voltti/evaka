// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.messaging

import evaka.core.FullApplicationTest
import evaka.core.emailclient.IEmailMessageProvider
import evaka.core.emailclient.MessageDeletionEmailData
import evaka.core.shared.HtmlSafe
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.instance.oulu.OuluEmailMessageProvider
import java.time.Duration
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class MessageDeletionEmailServiceIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired lateinit var emailMessageProvider: IEmailMessageProvider

    private fun helsinki(text: String) = HelsinkiDateTime.of(LocalDateTime.parse(text))

    private fun emailData(
        sentAt: HelsinkiDateTime,
        deletedAt: HelsinkiDateTime,
        recipientCount: Int = 1,
        deleterName: HtmlSafe<String> = HtmlSafe("Mari Virtanen"),
        senderAccountName: HtmlSafe<String> = HtmlSafe("Mari Virtanen"),
        senderAccountType: AccountType = AccountType.PERSONAL,
    ) =
        MessageDeletionEmailData(
            deleterName = deleterName,
            senderAccountName = senderAccountName,
            senderAccountType = senderAccountType,
            sentAt = sentAt,
            deletedAt = deletedAt,
            recipientCount = recipientCount,
        )

    @Test
    fun `sender email contains the support address and the viewable duration`() {
        val email =
            emailMessageProvider.messageDeletionSenderEmail(
                "evaka@espoo.fi",
                emailData(
                    sentAt = helsinki("2026-03-02T09:00"),
                    deletedAt = helsinki("2026-03-05T12:00"),
                    recipientCount = 32,
                ),
            )

        assertEquals("eVaka-viesti poistettu – ota yhteyttä tukeen", email.subject)
        assertTrue(email.html.contains("evaka@espoo.fi"))
        assertTrue(email.html.contains("3 vrk 3 h"))
        assertTrue(email.html.contains("Vastaanottajien lukumäärä"))
        assertTrue(email.html.contains("32"))
    }

    @Test
    fun `sender email omits the support address when none is configured`() {
        val email =
            emailMessageProvider.messageDeletionSenderEmail(
                null,
                emailData(
                    sentAt = helsinki("2026-03-02T09:00"),
                    deletedAt = helsinki("2026-03-05T12:00"),
                ),
            )

        assertTrue(email.html.contains("Ota yhteyttä tukeen ja esihenkilöösi"))
        assertFalse(email.html.contains("tukeen ("))
    }

    @Test
    fun `duration formatter - under one minute`() =
        assertDurationRenders(Duration.ofSeconds(30), "Alle 1 min")

    @Test
    fun `duration formatter - 59 seconds is still under one minute`() =
        assertDurationRenders(Duration.ofSeconds(59), "Alle 1 min")

    @Test
    fun `duration formatter - exactly one minute`() =
        assertDurationRenders(Duration.ofMinutes(1), "1 min")

    @Test
    fun `duration formatter - 59 min`() = assertDurationRenders(Duration.ofMinutes(59), "59 min")

    @Test fun `duration formatter - 1 h`() = assertDurationRenders(Duration.ofHours(1), "1 h")

    @Test
    fun `duration formatter - 1 h 1 min`() =
        assertDurationRenders(Duration.ofMinutes(61), "1 h 1 min")

    @Test
    fun `duration formatter - 23 h 59 min`() =
        assertDurationRenders(Duration.ofMinutes(23 * 60 + 59), "23 h 59 min")

    @Test fun `duration formatter - 1 vrk`() = assertDurationRenders(Duration.ofHours(24), "1 vrk")

    @Test
    fun `duration formatter - 1 vrk 1 h`() =
        assertDurationRenders(Duration.ofHours(25), "1 vrk 1 h")

    @Test
    fun `duration formatter - 7 vrk 23 h`() =
        assertDurationRenders(Duration.ofHours(7 * 24 + 23), "7 vrk 23 h")

    @Test
    fun `notification email renders a personal-account deleter without parentheses`() {
        val email =
            emailMessageProvider.messageDeletionNotificationEmail(
                "evaka@espoo.fi",
                emailData(
                    deleterName = HtmlSafe("Mari Virtanen"),
                    senderAccountName = HtmlSafe("Mari Virtanen"),
                    senderAccountType = AccountType.PERSONAL,
                    sentAt = helsinki("2026-03-02T09:00"),
                    deletedAt = helsinki("2026-03-05T12:00"),
                    recipientCount = 3,
                ),
            )

        assertTrue(email.html.contains("Mari Virtanen on poistanut"))
        assertFalse(email.html.contains("Mari Virtanen ("))
    }

    @Test
    fun `notification email renders a group-account deleter with the account name in parentheses`() {
        val email =
            emailMessageProvider.messageDeletionNotificationEmail(
                "evaka@espoo.fi",
                emailData(
                    deleterName = HtmlSafe("Mari Virtanen"),
                    senderAccountName = HtmlSafe("Testialueen päiväkoti"),
                    senderAccountType = AccountType.GROUP,
                    sentAt = helsinki("2026-03-02T09:00"),
                    deletedAt = helsinki("2026-03-05T12:00"),
                    recipientCount = 3,
                ),
            )

        assertTrue(email.html.contains("Mari Virtanen (Testialueen päiväkoti) on poistanut"))
    }

    @Test
    fun `Oulu sender email points the deleter to the service portal instead of an email`() {
        val email =
            OuluEmailMessageProvider()
                .messageDeletionSenderEmail(
                    null,
                    emailData(
                        sentAt = helsinki("2026-03-02T09:00"),
                        deletedAt = helsinki("2026-03-05T12:00"),
                    ),
                )

        assertEquals("eVaka-viesti poistettu – ota yhteyttä tukeen", email.subject)
        assertTrue(email.html.contains("https://palvelupyynto.siku.ouka.fi/customerui/"))
        assertFalse(email.html.contains("Ota yhteyttä tukeen ("))
        assertTrue(email.html.contains("Vastaanottajien lukumäärä"))
    }

    @Test
    fun `Oulu notification email points the deleter to the service portal instead of an email`() {
        val email =
            OuluEmailMessageProvider()
                .messageDeletionNotificationEmail(
                    null,
                    emailData(
                        deleterName = HtmlSafe("Mari Virtanen"),
                        senderAccountName = HtmlSafe("Mari Virtanen"),
                        senderAccountType = AccountType.PERSONAL,
                        sentAt = helsinki("2026-03-02T09:00"),
                        deletedAt = helsinki("2026-03-05T12:00"),
                    ),
                )

        assertEquals("eVaka-viesti poistettu – tee tietosuojailmoitus", email.subject)
        assertTrue(email.html.contains("https://palvelupyynto.siku.ouka.fi/customerui/"))
        assertTrue(email.html.contains("Mari Virtanen on poistanut"))
        assertFalse(email.html.contains("ottamaan yhteyttä tukeen ("))
    }

    private fun assertDurationRenders(duration: Duration, expected: String) {
        val start = LocalDateTime.parse("2026-01-01T00:00")
        val email =
            emailMessageProvider.messageDeletionSenderEmail(
                "evaka@espoo.fi",
                emailData(
                    sentAt = HelsinkiDateTime.of(start),
                    deletedAt = HelsinkiDateTime.of(start.plus(duration)),
                ),
            )
        // assert the exact rendered line, so e.g. "1 h" can't match a "1 h 1 min" rendering
        assertTrue(
            email.text.lineSequence().any {
                it.trim() == "Oli vastaanottajien avattavissa: $expected"
            },
            "Expected viewable-duration line '$expected', got:\n${email.text}",
        )
    }
}
