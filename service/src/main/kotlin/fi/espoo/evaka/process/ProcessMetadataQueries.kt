// SPDX-FileCopyrightText: 2024 City of Espoo
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.process

import fi.espoo.evaka.shared.ChildDocumentId
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.user.EvakaUser

fun Database.Read.getChildDocumentMetadata(documentId: ChildDocumentId): DocumentMetadata =
    createQuery {
            sql(
                """
        SELECT 
            dt.id,
            dt.name,
            cd.created,
            e.id AS created_by_id,
            e.name AS created_by_name,
            e.type AS created_by_type,
            dt.confidential,
            dt.confidentiality_duration_years,
            dt.confidentiality_basis,
            cd.document_key
        FROM child_document cd
        JOIN document_template dt ON dt.id = cd.template_id
        LEFT JOIN evaka_user e ON e.employee_id = cd.created_by
        WHERE cd.id = ${bind(documentId)}
    """
            )
        }
        .map {
            DocumentMetadata(
                documentId = column("id"),
                name = column("name"),
                createdAt = column("created"),
                createdBy =
                    column<EvakaUserId?>("created_by_id")?.let {
                        EvakaUser(
                            id = it,
                            name = column("created_by_name"),
                            type = column("created_by_type"),
                        )
                    },
                confidential = column("confidential"),
                confidentiality =
                    if (column<Boolean>("confidential")) {
                        DocumentConfidentiality(
                            durationYears = column("confidentiality_duration_years"),
                            basis = column("confidentiality_basis"),
                        )
                    } else null,
                downloadPath =
                    column<String?>("document_key")?.let { "/employee/child-documents/$it/pdf" },
                receivedBy = null,
            )
        }
        .exactlyOne()
