// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.webpush

import evaka.core.document.childdocument.ChildDocumentNotificationType
import evaka.core.shared.ChildDocumentId
import java.util.UUID
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test

class ChildDocumentPathTest {
    private val documentId =
        ChildDocumentId(UUID.fromString("7e2a9d6c-3b1f-4c8e-9a5d-1f0b2c3d4e5f"))

    private fun document(type: ChildDocumentNotificationType) =
        CitizenPushNotification.Document(documentId, type, childName = "Test")

    @Test
    fun `a fillable document opens in edit mode`() {
        assertEquals(
            "/child-documents/$documentId?readOnly=false",
            childDocumentPath(document(ChildDocumentNotificationType.EDITABLE_DOCUMENT)),
        )
    }

    @Test
    fun `other documents open in read mode`() {
        listOf(
                ChildDocumentNotificationType.BASIC_DOCUMENT,
                ChildDocumentNotificationType.DECISION_DOCUMENT,
            )
            .forEach {
                assertEquals("/child-documents/$documentId", childDocumentPath(document(it)))
            }
    }
}
