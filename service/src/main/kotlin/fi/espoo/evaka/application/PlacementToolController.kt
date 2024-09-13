// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.application

import fi.espoo.evaka.Audit
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import java.util.UUID
import org.springframework.http.MediaType
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
    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun createPlacementToolApplications(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestPart("file") file: MultipartFile,
    ): UUID {
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
                    UUID.randomUUID()
                }
            }
            .also { Audit.PlacementTool }
    }
}
