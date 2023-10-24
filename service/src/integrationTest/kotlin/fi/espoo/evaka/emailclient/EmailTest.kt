//  SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
//  SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.emailclient

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.pis.EmailMessageType
import fi.espoo.evaka.pis.updateEnabledEmailTypes
import fi.espoo.evaka.shared.DatabaseTable
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.testAdult_1
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class EmailTest : PureJdbiTest(resetDbBeforeEach = true) {
    @BeforeEach
    fun beforeEach() {
        db.transaction { tx -> tx.insert(testAdult_1, DevPersonType.RAW_ROW) }
    }

    @Test
    fun `messages are not sent to invalid email addresses`() {
        listOf("test", "test@example", "test@example.", "test@example.c")
            .mapNotNull { address -> createEmail(toAddress = address) }
            .also { emails -> assertEquals(emptyList(), emails) }
    }

    @Test
    fun `receiver's enabled email notification types are respected`() {
        // Not set -> all messages are sent
        EmailMessageType.values()
            .mapNotNull { type -> createEmail(emailType = type) }
            .also { emails -> assertEquals(EmailMessageType.values().size, emails.size) }

        // Only some notification types are enabled
        db.transaction { tx ->
            tx.updateEnabledEmailTypes(
                testAdult_1.id,
                listOf(
                    EmailMessageType.TRANSACTIONAL,
                    EmailMessageType.BULLETIN_NOTIFICATION,
                    EmailMessageType.DOCUMENT_NOTIFICATION
                )
            )
        }
        EmailMessageType.values()
            .mapNotNull { type -> createEmail(emailType = type, toAddress = "$type@example.com") }
            .also { emails ->
                assertEquals(
                    listOf(
                        "TRANSACTIONAL@example.com",
                        "BULLETIN_NOTIFICATION@example.com",
                        "DOCUMENT_NOTIFICATION@example.com"
                    ),
                    emails.map { it.toAddress }
                )
            }
    }

    private fun createEmail(
        emailType: EmailMessageType = EmailMessageType.TRANSACTIONAL,
        toAddress: String = "test@example.com"
    ): Email? {
        db.transaction { tx ->
            tx.createUpdate<DatabaseTable> {
                    sql(
                        "UPDATE person SET email = ${bind(toAddress)} WHERE id = ${bind(testAdult_1.id)}"
                    )
                }
                .execute()
        }
        return Email.create(db, testAdult_1.id, emailType, toAddress, testContent, "traceid")
    }
}

private val testContent = EmailContent(subject = "subject", html = "html", text = "text")
