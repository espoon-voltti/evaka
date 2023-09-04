// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.document.childdocument

import fi.espoo.evaka.Audit
import fi.espoo.evaka.shared.ChildDocumentId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/citizen/child-documents")
class ChildDocumentControllerCitizen(private val accessControl: AccessControl) {

    @GetMapping
    fun getDocuments(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @RequestParam childId: ChildId
    ): List<ChildDocumentSummary> {
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Citizen.Child.READ_CHILD_DOCUMENTS,
                        childId
                    )
                    tx.getChildDocuments(childId).filter { doc -> doc.publishedAt != null }
                }
            }
            .also { Audit.ChildDocumentRead.log(targetId = childId) }
    }

    @GetMapping("/{documentId}")
    fun getDocument(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @PathVariable documentId: ChildDocumentId
    ): ChildDocumentDetails {
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Citizen.ChildDocument.READ,
                        documentId
                    )

                    tx.getChildDocument(documentId)?.takeIf { it.publishedAt != null }
                        ?: throw NotFound("Document $documentId not found")
                }
            }
            .also { Audit.ChildDocumentRead.log(targetId = documentId) }
    }
}
