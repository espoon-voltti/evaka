// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.vasu

import fi.espoo.evaka.Audit
import fi.espoo.evaka.AuditId
import fi.espoo.evaka.children.getCitizenChildIds
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.FeatureConfig
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
    @GetMapping("/children/{childId}/vasu-summaries")
    fun getChildVasuSummaries(
        db: Database,
        clock: EvakaClock,
        user: AuthenticatedUser.Citizen,
        @PathVariable childId: ChildId
    ): CitizenGetVasuDocumentSummariesResponse {
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Citizen.Child.READ_VASU_DOCUMENT_SUMMARIES,
                        childId
                    )
                    CitizenGetVasuDocumentSummariesResponse(
                        data =
                            tx.getVasuDocumentSummaries(childId).filter { it.publishedAt != null },
                        permissionToShareRequired =
                            featureConfig.curriculumDocumentPermissionToShareRequired
                    )
                }
            }
            .also {
                Audit.ChildVasuDocumentsReadByGuardian.log(
                    targetId = AuditId(childId),
                    meta = mapOf("count" to it.data.size)
                )
            }
    }

    data class CitizenGetVasuDocumentSummariesResponse(
        val data: List<VasuDocumentSummary>,
        val permissionToShareRequired: Boolean
    )

    @GetMapping("/children/unread-count")
    fun getGuardianUnreadVasuCount(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock
    ): Map<ChildId, Int> {
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Citizen.Person.READ_VASU_UNREAD_COUNT,
                        user.id
                    )
                    val children = tx.getCitizenChildIds(clock.today(), user.id)
                    if (!featureConfig.curriculumDocumentPermissionToShareRequired) {
                        return@read children.associateWith { 0 }
                    }
                    children.associateWith { childId ->
                        tx.getVasuDocumentSummaries(childId)
                            .filter { it.publishedAt != null }
                            .filterNot { doc ->
                                doc.guardiansThatHaveGivenPermissionToShare.contains(user.id)
                            }
                            .size
                    }
                }
            }
            .also { Audit.ChildVasuDocumentsReadByGuardian.log(targetId = AuditId(user.id)) }
    }

    data class CitizenGetVasuDocumentResponse(
        val vasu: VasuDocument,
        val permissionToShareRequired: Boolean,
        val guardianHasGivenPermissionToShare: Boolean
    )

    @GetMapping("/{id}")
    fun getDocument(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @PathVariable id: VasuDocumentId
    ): CitizenGetVasuDocumentResponse {
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Citizen.VasuDocument.READ,
                        id
                    )
                    val doc =
                        tx.getLatestPublishedVasuDocument(clock.today(), id)
                            ?: throw NotFound("document $id not found")
                    CitizenGetVasuDocumentResponse(
                        vasu = doc.redact(),
                        permissionToShareRequired =
                            featureConfig.curriculumDocumentPermissionToShareRequired,
                        guardianHasGivenPermissionToShare =
                            doc.basics.guardians
                                .find { it.id.raw == user.rawId() }
                                ?.hasGivenPermissionToShare ?: false
                    )
                }
            }
            .also { Audit.VasuDocumentReadByGuardian.log(targetId = AuditId(id)) }
    }

    @PostMapping("/{id}/give-permission-to-share")
    fun givePermissionToShare(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @PathVariable id: VasuDocumentId
    ) {
        return db.connect { dbc ->
                dbc.transaction {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Citizen.VasuDocument.GIVE_PERMISSION_TO_SHARE,
                        id
                    )
                    it.setVasuGuardianHasGivenPermissionToShare(id, user.id)
                }
            }
            .also { Audit.VasuDocumentGivePermissionToShareByGuardian.log(targetId = AuditId(id)) }
    }
}
