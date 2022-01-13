// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.childimages

import fi.espoo.evaka.Audit
import fi.espoo.evaka.BucketEnv
import fi.espoo.evaka.s3.DocumentService
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.ChildImageId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import mu.KotlinLogging
import org.springframework.core.io.InputStreamResource
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
class ChildImageController(
    private val accessControl: AccessControl,
    private val documentClient: DocumentService,
    env: BucketEnv
) {
    private val logger = KotlinLogging.logger { }
    private val bucket = env.data

    @PutMapping("/children/{childId}/image", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun putImage(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable childId: ChildId,
        @RequestPart("file") file: MultipartFile
    ) {
        Audit.ChildImageUpload.log(targetId = childId)
        accessControl.requirePermissionFor(user, Action.Child.UPLOAD_IMAGE, childId)

        db.connect { dbc -> replaceImage(dbc, documentClient, bucket, childId, file) }
    }

    @DeleteMapping("/children/{childId}/image")
    fun deleteImage(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable childId: ChildId
    ) {
        Audit.ChildImageDelete.log(targetId = childId)
        accessControl.requirePermissionFor(user, Action.Child.DELETE_IMAGE, childId)

        db.connect { dbc -> removeImage(dbc, documentClient, bucket, childId) }
    }

    @GetMapping(value = ["/child-images/{imageId}", "/citizen/child-images/{imageId}"])
    fun getImage(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable imageId: ChildImageId
    ): ResponseEntity<Any> {
        Audit.ChildImageDownload.log(targetId = imageId)
        accessControl.requirePermissionFor(user, Action.ChildImage.DOWNLOAD, imageId)

        val key = "$childImagesBucketPrefix$imageId"
        val presignedUrl = documentClient.presignedGetUrl(bucket, key)

        return if (presignedUrl != null) {
            val url = "/internal_redirect/$presignedUrl"
            logger.info("Child image $imageId: Using X-Accel-Redirect $url")
            ResponseEntity.ok()
                .header("X-Accel-Redirect", url)
                .body("")
        } else {
            // In dev there's no nginx, so stream the data directly
            logger.info("Child image $imageId: Streaming to client")
            val stream = documentClient.stream(bucket, key)
            ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(InputStreamResource(stream))
        }
    }
}
