// SPDX-FileCopyrightText: 2024 City of Espoo
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.process

import fi.espoo.evaka.shared.ChildDocumentId
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.user.EvakaUser
import org.intellij.lang.annotations.Language

@Language("sql")
val sfiDeliverySelect =
    """
    SELECT coalesce(jsonb_agg(
        jsonb_build_object(
            'recipientName', recipient.last_name || ' ' || recipient.first_name,
            'time', coalesce(delivery.time, sm.created_at),
            'method', coalesce(delivery.method, 'PENDING')
        )
    ), '[]'::jsonb)
    FROM sfi_message sm
    JOIN person recipient ON sm.guardian_id = recipient.id
    LEFT JOIN LATERAL (
        SELECT
            sme.created_at AS time,
            CASE
                WHEN sme.event_type = 'ELECTRONIC_MESSAGE_CREATED' THEN 'ELECTRONIC'
                WHEN sme.event_type = 'SENT_FOR_PRINTING_AND_ENVELOPING' THEN 'PAPER_MAIL'
            END AS method
        FROM sfi_message_event sme
        WHERE sme.message_id = sm.id AND sme.event_type IN (
            'ELECTRONIC_MESSAGE_CREATED',
            'SENT_FOR_PRINTING_AND_ENVELOPING'
        )
        ORDER BY sme.created_at
        LIMIT 1
    ) delivery ON true        
"""

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
            cd.document_key,
            (
                $sfiDeliverySelect
                WHERE sm.document_id = cd.id
            ) AS sfi_deliveries
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
                sfiDeliveries = jsonColumn("sfi_deliveries"),
            )
        }
        .exactlyOne()
