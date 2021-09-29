// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pedagogicaldocument

import fi.espoo.evaka.Audit
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/citizen/pedagogical-documents")
class PedagogicalDocumentControllerCitizen() {
    @GetMapping
    fun getPedagogicalDocumentsByGuardian(
        db: Database.Connection,
        user: AuthenticatedUser
    ): List<PedagogicalDocument> {
        Audit.PedagogicalDocumentReadByGuardian.log(user.id)
        return db.read { it.findPedagogicalDocumentsByGuardian(PersonId(user.id)) }
    }

    private fun Database.Read.findPedagogicalDocumentsByGuardian(guardianId: PersonId): List<PedagogicalDocument> {
        return this.createQuery(
            """
                SELECT 
                    pd.id,
                    pd.child_id,
                    pd.description,
                    pd.created,
                    pd.updated,
                    a.id attachment_id,
                    a.name attachment_name,
                    a.content_type attachment_content_type
                FROM pedagogical_document pd
                JOIN guardian g ON pd.child_id = g.child_id
                LEFT JOIN attachment a ON a.pedagogical_document_id = pd.id
                WHERE g.guardian_id = :guardian_id
            """.trimIndent()
        )
            .bind("guardian_id", guardianId)
            .let {
                try {
                    it.map { row -> mapPedagogicalDocument(row) }
                        .toList()
                } catch (e: Exception) {
                    listOf()
                }
            }
    }
}
