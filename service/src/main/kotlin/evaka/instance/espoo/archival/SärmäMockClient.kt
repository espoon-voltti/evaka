// SPDX-FileCopyrightText: 2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.espoo.archival

import evaka.core.document.archival.ArchivalClient
import evaka.core.s3.Document

class SärmäMockClient : ArchivalClient {
    override fun putDocument(
        documentContent: Document,
        metadataXml: String,
        masterId: String,
        classId: String,
        virtualArchiveId: String,
    ): Pair<Int, String?> {
        return Pair(
            200,
            "status_message=Success.&transaction_id=2872934&protocol_version=1.0&status_code=200&instance_ids=354319&",
        )
    }
}
