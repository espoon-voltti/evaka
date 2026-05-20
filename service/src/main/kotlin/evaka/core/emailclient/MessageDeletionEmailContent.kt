// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.emailclient

import evaka.core.messaging.AccountType
import evaka.core.shared.HtmlSafe
import java.time.Duration
import java.time.format.DateTimeFormatter

/**
 * Default Finnish content for the message-deletion emails. Municipalities that need to vary the
 * wording — translations, a different support-contact format, restructured layout — should override
 * [IEmailMessageProvider.messageDeletionSenderEmail] and
 * [IEmailMessageProvider.messageDeletionNotificationEmail] with fully custom implementations, the
 * same way other emails are customized. [detailsBlock] and [deleterIdentification] are exposed for
 * those overrides so the structural data formatting can be reused.
 */
object MessageDeletionEmailContent {
    /** Sent to the employee who deleted a message they had sent. */
    fun senderEmail(supportEmail: String?, data: MessageDeletionEmailData): EmailContent =
        EmailContent.fromHtml(
            subject = "eVaka-viesti poistettu – ota yhteyttä tukeen",
            html =
                """
<p>Olet poistanut viestin eVakasta. Vastaanottajien viestin sisältö on korvattu ilmoituksella poistosta. Voit tarvittaessa avata poistetun viestin katseltavaksi eVakassa.</p>
<p><strong>Ota yhteyttä tukeen${if (supportEmail != null) " ($supportEmail)" else ""} ja esihenkilöösi tietosuojailmoituksen tekemiseksi.</strong></p>
${detailsBlock(data)}
""",
        )

    /** Sent to the unit supervisors of the recipient units and to the municipality eVaka admin. */
    fun notificationEmail(supportEmail: String?, data: MessageDeletionEmailData): EmailContent {
        val deleter =
            deleterIdentification(data.deleterName, data.senderAccountName, data.senderAccountType)
        return EmailContent.fromHtml(
            subject = "eVaka-viesti poistettu – tee tietosuojailmoitus",
            html =
                """
<p>$deleter on poistanut viestin eVakasta. Viesti oli lähetetty väärille vastaanottajille. Vastaanottajien viestin sisältö on korvattu ilmoituksella poistosta. Poistaja voi tarvittaessa avata poistetun viestin katseltavaksi eVakassa.</p>
<p><strong>Viestin poistajaa on ohjeistettu ottamaan yhteyttä tukeen${if (supportEmail != null) " ($supportEmail)" else ""} ja esihenkilöönsä tietosuojailmoituksen tekemiseksi.</strong></p>
${detailsBlock(data)}
""",
        )
    }

    /**
     * "VIESTIN TIEDOT" footer block: sent-at, deleted-at, how long it was viewable, and recipient
     * count. Exposed so municipality-specific email overrides can reuse the structural formatting
     * without duplicating the duration logic.
     */
    fun detailsBlock(data: MessageDeletionEmailData): String {
        val fmt = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
        val viewableDuration =
            formatViewableDuration(
                Duration.between(data.sentAt.toInstant(), data.deletedAt.toInstant())
            )
        return """
<p><strong>VIESTIN TIEDOT</strong><br>
Lähetetty: ${data.sentAt.toLocalDateTime().format(fmt)}<br>
Poistettu: ${data.deletedAt.toLocalDateTime().format(fmt)}<br>
Oli vastaanottajien avattavissa: $viewableDuration<br>
Vastaanottajien lukumäärä: ${data.recipientCount}</p>
"""
    }

    /**
     * Identifies the deleter in a notification email: just the name for a personal account, or
     * `"Name (Account name)"` for a shared (group/municipal/…) account.
     */
    fun deleterIdentification(
        deleterName: HtmlSafe<String>,
        accountName: HtmlSafe<String>,
        accountType: AccountType,
    ): String =
        if (accountType == AccountType.PERSONAL || accountName.toString() == deleterName.toString())
            deleterName.toString()
        else "$deleterName ($accountName)"

    /**
     * Formats how long a deleted message was openable by recipients, e.g. `Alle 1 min`, `59 min`,
     * `1 h 1 min`, `3 vrk 3 h`. Covers the full range up to the end of the deletion window (the
     * start of the 8th day after sending, i.e. through the end of day 7).
     */
    private fun formatViewableDuration(duration: Duration): String {
        val totalMinutes = duration.toMinutes()
        return when {
            totalMinutes < 1L -> "Alle 1 min"
            totalMinutes < 60L -> "$totalMinutes min"
            totalMinutes < 24 * 60L -> {
                val h = totalMinutes / 60
                val m = totalMinutes % 60
                if (m == 0L) "$h h" else "$h h $m min"
            }
            else -> {
                val d = totalMinutes / (24 * 60)
                val h = (totalMinutes % (24 * 60)) / 60
                if (h == 0L) "$d vrk" else "$d vrk $h h"
            }
        }
    }
}
