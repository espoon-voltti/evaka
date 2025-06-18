// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.process

import fi.espoo.evaka.Audit
import fi.espoo.evaka.AuditId
import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/citizen/process-metadata")
class ProcessMetadataControllerCitizen(
    private val accessControl: AccessControl,
    private val processMetadataService: ProcessMetadataService,
) {
    @GetMapping("/applications/{applicationId}")
    fun getApplicationMetadata(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @PathVariable applicationId: ApplicationId,
    ): ProcessMetadataResponse {
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Citizen.Application.READ,
                        applicationId,
                    )
                    val process =
                        tx.getArchiveProcessByApplicationId(applicationId)
                            ?: return@read ProcessMetadataResponse(null)
                    val processMetadata =
                        processMetadataService.getApplicationProcessMetadata(
                            tx,
                            user,
                            clock,
                            applicationId,
                            process,
                            isCitizen = true,
                        )
                    ProcessMetadataResponse(processMetadata.toCitizen())
                }
            }
            .also { response ->
                Audit.ApplicationReadMetadata.log(
                    targetId = AuditId(applicationId),
                    objectId = response.data?.process?.id?.let(AuditId::invoke),
                )
            }
    }
}
