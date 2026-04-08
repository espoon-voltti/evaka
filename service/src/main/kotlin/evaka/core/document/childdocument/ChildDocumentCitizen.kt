// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.document.childdocument

import evaka.core.document.ChildDocumentType
import evaka.core.document.DocumentTemplate
import evaka.core.shared.ChildDocumentId
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.user.EvakaUser
import org.jdbi.v3.core.mapper.Nested
import org.jdbi.v3.json.Json

data class ChildDocumentCitizenSummary(
    val id: ChildDocumentId,
    val status: DocumentStatus,
    val type: ChildDocumentType,
    val templateName: String,
    val publishedAt: HelsinkiDateTime,
    val unread: Boolean,
    @Nested("child") val child: ChildBasics,
    val answeredAt: HelsinkiDateTime?,
    @Nested("answered_by") val answeredBy: EvakaUser?,
    @Nested("decision") val decision: ChildDocumentDecision? = null,
)

data class ChildDocumentCitizenDetails(
    val id: ChildDocumentId,
    val status: DocumentStatus,
    val publishedAt: HelsinkiDateTime?,
    val downloadable: Boolean,
    @Json val content: DocumentContent,
    @Nested("child") val child: ChildBasics,
    @Nested("template") val template: DocumentTemplate,
    @Nested("decision") val decision: ChildDocumentDecision? = null,
)
