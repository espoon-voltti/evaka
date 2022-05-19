// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.vasu

import fi.espoo.evaka.Audit
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.VasuDocumentId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/citizen/vasu")
class VasuControllerCitizen(
    private val accessControl: AccessControl
) {
    @GetMapping("/children/{childId}/vasu-summaries")
    fun getGuardianVasuSummariesByChild(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable childId: ChildId
    ): List<VasuDocumentSummary> {
        Audit.ChildVasuDocumentsReadByGuardian.log(childId, user.rawId())
        accessControl.requirePermissionFor(user, Action.Citizen.Child.READ_VASU_DOCUMENT, childId)

        return db.connect { dbc -> dbc.read { tx -> tx.getVasuDocumentSummaries(childId) } }
    }

    data class CitizenGetVasuDocumentResponse(
        val vasu: VasuDocument,
        val guardianHasGivenPermissionToShare: Boolean
    )

    @GetMapping("/{id}")
    fun getDocument(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable id: VasuDocumentId
    ): CitizenGetVasuDocumentResponse {
        Audit.VasuDocumentReadByGuardian.log(id, user.rawId())
        accessControl.requirePermissionFor(user, Action.Citizen.VasuDocument.READ, id)

        return db.connect { dbc ->
            dbc.read { tx ->
                val doc = tx.getLatestPublishedVasuDocument(id) ?: throw NotFound("document $id not found")
                CitizenGetVasuDocumentResponse(
                    vasu = doc,
                    guardianHasGivenPermissionToShare = doc.basics.guardians.find {
                            g ->
                        g.id.raw == user.rawId()
                    }?.let { it.hasGivenPermissionToShare } ?: false
                )
            }
        }
    }

    @PostMapping("/{id}/give-permission-to-share")
    fun givePermissionToShare(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable id: VasuDocumentId
    ) {
        Audit.VasuDocumentGivePermissionToShareByGuardian.log(id, user.rawId())
        accessControl.requirePermissionFor(user, Action.Citizen.VasuDocument.GIVE_PERMISSION_TO_SHARE, id)

        return db.connect { dbc ->
            dbc.transaction { it.setVasuGuardianHasGivenPermissionToShare(id, PersonId(user.rawId())) }
        }
    }
}
