// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.vasu

import fi.espoo.evaka.Audit
import fi.espoo.evaka.pis.getGuardianDependants
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.FeatureConfig
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.VasuDocumentId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
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
    private val featureConfig: FeatureConfig,
    private val accessControl: AccessControl
) {
    data class ChildBasicInfo(
        val id: ChildId,
        val firstName: String,
        val lastName: String
    )

    @GetMapping("/children/{childId}/vasu-summaries")
    fun getChildVasuSummaries(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable childId: ChildId
    ): CitizenGetVasuDocumentSummariesResponse {
        Audit.ChildVasuDocumentsReadByGuardian.log(targetId = childId)

        return db.connect { dbc ->
            dbc.read { tx ->
                CitizenGetVasuDocumentSummariesResponse(
                    data = tx.getVasuDocumentSummaries(childId).filter { it.publishedAt != null },
                    permissionToShareRequired = featureConfig.curriculumDocumentPermissionToShareRequired
                )
            }
        }
    }

    data class CitizenGetVasuDocumentSummariesResponse(
        val data: List<VasuDocumentSummary>,
        val permissionToShareRequired: Boolean,
    )

    @GetMapping("/children/unread-count")
    fun getGuardianUnreadVasuCount(
        db: Database,
        user: AuthenticatedUser
    ): Map<ChildId, Int> {
        Audit.ChildVasuDocumentsReadByGuardian.log(user.rawId())
        return db.connect { dbc ->
            dbc.read { tx ->
                val children = tx.getGuardianDependants(PersonId(user.rawId()))
                if (!featureConfig.curriculumDocumentPermissionToShareRequired) {
                    return@read children.associate { child -> child.id to 0 }
                }
                children.associate { child ->
                    child.id to tx.getVasuDocumentSummaries(child.id)
                        .filter { it.publishedAt != null }
                        .filterNot { doc -> doc.guardiansThatHaveGivenPermissionToShare.contains(PersonId(user.evakaUserId.raw)) }
                        .size
                }
            }
        }
    }

    data class CitizenGetVasuDocumentResponse(
        val vasu: VasuDocument,
        val permissionToShareRequired: Boolean,
        val guardianHasGivenPermissionToShare: Boolean
    )

    @GetMapping("/{id}")
    fun getDocument(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable id: VasuDocumentId
    ): CitizenGetVasuDocumentResponse {
        Audit.VasuDocumentReadByGuardian.log(id, user.rawId())
        accessControl.requirePermissionFor(user, clock, Action.Citizen.VasuDocument.READ, id)

        return db.connect { dbc ->
            dbc.read { tx ->
                val doc = tx.getLatestPublishedVasuDocument(id) ?: throw NotFound("document $id not found")
                CitizenGetVasuDocumentResponse(
                    vasu = doc.redact(),
                    permissionToShareRequired = featureConfig.curriculumDocumentPermissionToShareRequired,
                    guardianHasGivenPermissionToShare = doc.basics.guardians.find { it.id.raw == user.rawId() }?.hasGivenPermissionToShare ?: false
                )
            }
        }
    }

    @PostMapping("/{id}/give-permission-to-share")
    fun givePermissionToShare(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable id: VasuDocumentId
    ) {
        Audit.VasuDocumentGivePermissionToShareByGuardian.log(id, user.rawId())
        accessControl.requirePermissionFor(user, clock, Action.Citizen.VasuDocument.GIVE_PERMISSION_TO_SHARE, id)

        return db.connect { dbc ->
            dbc.transaction { it.setVasuGuardianHasGivenPermissionToShare(id, PersonId(user.rawId())) }
        }
    }
}
