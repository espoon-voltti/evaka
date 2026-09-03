//  SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
//  SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.emailclient

import evaka.core.PureJdbiTest
import evaka.core.pis.NotificationCategory
import evaka.core.pis.updateDisabledEmailTypes
import evaka.core.shared.dev.DevPerson
import evaka.core.shared.dev.DevPersonType
import evaka.core.shared.dev.insert
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class EmailTest : PureJdbiTest(resetDbBeforeEach = true) {
    private val adult = DevPerson()

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx -> tx.insert(adult, DevPersonType.RAW_ROW) }
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
        NotificationCategory.entries
            .mapNotNull { type -> createEmail(category = type) }
            .also { emails -> assertEquals(NotificationCategory.entries.size, emails.size) }

        // Only some notification types are enabled
        db.transaction { tx ->
            tx.updateDisabledEmailTypes(
                adult.id,
                // Disable all but three
                NotificationCategory.entries.toSet() -
                    setOf(
                        NotificationCategory.TRANSACTIONAL,
                        NotificationCategory.BULLETIN_NOTIFICATION,
                        NotificationCategory.DOCUMENT_NOTIFICATION,
                    ),
            )
        }
        NotificationCategory.entries
            .mapNotNull { type -> createEmail(category = type, toAddress = "$type@example.com") }
            .also { emails ->
                assertEquals(
                    listOf(
                        "TRANSACTIONAL@example.com",
                        "BULLETIN_NOTIFICATION@example.com",
                        "DOCUMENT_NOTIFICATION@example.com",
                    ),
                    emails.map { it.toAddress },
                )
            }
    }

    private fun createEmail(
        category: NotificationCategory = NotificationCategory.TRANSACTIONAL,
        toAddress: String = "test@example.com",
    ): Email? {
        val fromAddress = FromAddress("Foo <foo@example.com>", null)
        db.transaction { tx ->
            tx.createUpdate {
                    sql("UPDATE person SET email = ${bind(toAddress)} WHERE id = ${bind(adult.id)}")
                }
                .execute()
        }
        return Email.create(db, adult.id, category, fromAddress, testContent, "traceid")
    }
}

private val testContent = EmailContent(subject = "subject", html = "html", text = "text")
