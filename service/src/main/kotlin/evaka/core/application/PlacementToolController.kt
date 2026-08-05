// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.application

import evaka.core.Audit
import evaka.core.AuditContext
import evaka.core.daycare.PreschoolTerm
import evaka.core.shared.AttachmentId
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.db.Database
import evaka.core.shared.domain.EvakaClock
import evaka.core.shared.security.AccessControl
import evaka.core.shared.security.Action
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
        val audit = AuditContext()
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
            .also { validation ->
                audit.addMeta("count", validation.count).addMeta("existing", validation.existing)
                audit.log(Audit.PlacementToolValidate, clock)
            }
    }

    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun createPlacementToolApplications(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestPart("file") file: MultipartFile,
    ): AttachmentId {
        val audit = AuditContext()
        return db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Global.PLACEMENT_TOOL,
                    )
                    placementToolService.createPlacementToolApplications(
                        tx,
                        user,
                        clock,
                        audit,
                        file,
                    )

                    // this is needed for fileUpload component
                    AttachmentId(UUID.randomUUID())
                }
            }
            .also { audit.log(Audit.PlacementTool, clock) }
    }

    @GetMapping("/next-term")
    fun getNextPreschoolTerm(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
    ): List<PreschoolTerm> {
        val audit = AuditContext()
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
            .also { terms ->
                audit.add(terms.map { it.id })
                audit.log(Audit.PreschoolTermRead, clock)
            }
    }
}

data class PlacementToolValidation(val count: Int, val existing: Int)
