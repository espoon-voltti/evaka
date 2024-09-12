// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.application

import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import java.util.UUID
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/employee/placement-tool")
class PlacementToolController(private val placementToolService: PlacementToolService) {
    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun createPlacementToolApplications(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestPart("file") file: MultipartFile,
    ): UUID {
        placementToolService.createPlacementToolApplications(db, user, clock, file)

        // this is needed for fileUpload component
        return UUID.randomUUID()
    }
}
