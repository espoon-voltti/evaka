// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.application

import fi.espoo.evaka.Audit
import fi.espoo.evaka.AuditId
import fi.espoo.evaka.daycare.PreschoolTerm
import fi.espoo.evaka.shared.AttachmentId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import java.util.UUID
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/employee/placement-tool")
class PlacementToolController(
    private val placementToolService: PlacementToolService,
    private val accessControl: AccessControl,
) {
    @PostMapping("/validation", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun validatePlacementToolApplications(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestPart("file") file: MultipartFile,
    ): PlacementToolValidation {
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Global.PLACEMENT_TOOL,
                    )
                    placementToolService.validatePlacementToolApplications(tx, clock, file)
                }
            }
            .also { Audit.PlacementToolValidate.log() }
    }

    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun createPlacementToolApplications(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestPart("file") file: MultipartFile,
    ): AttachmentId {
        return db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Global.PLACEMENT_TOOL,
                    )
                    placementToolService.createPlacementToolApplications(tx, user, clock, file)

                    // this is needed for fileUpload component
                    AttachmentId(UUID.randomUUID())
                }
            }
            .also { Audit.PlacementTool.log() }
    }

    @GetMapping("/next-term")
    fun getNextPreschoolTerm(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
    ): List<PreschoolTerm> {
        return db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Global.PLACEMENT_TOOL,
                    )
                    listOfNotNull(
                        placementToolService.findNextPreschoolTerm(date = clock.today(), tx = tx)
                    )
                }
            }
            .also {
                Audit.PlacementTool.log(
                    objectId = it.firstOrNull()?.let { term -> AuditId(term.id) }
                )
            }
    }
}

data class PlacementToolValidation(val count: Int, val existing: Int)
