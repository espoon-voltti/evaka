// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.childimages

import fi.espoo.evaka.Audit
import fi.espoo.evaka.BucketEnv
import fi.espoo.evaka.s3.DocumentService
import fi.espoo.evaka.s3.checkFileContentType
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.ChildImageId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import mu.KotlinLogging
import org.apache.commons.imaging.Imaging
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

const val maxImageSize = 512

@RestController
class ChildImageController(
    private val accessControl: AccessControl,
    private val documentClient: DocumentService,
    env: BucketEnv
) {
    private val logger = KotlinLogging.logger {}
    private val bucket = env.data

    @PutMapping("/children/{childId}/image", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun putImage(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable childId: ChildId,
        @RequestPart("file") file: MultipartFile
    ) {
        Audit.ChildImageUpload.log(targetId = childId)
        accessControl.requirePermissionFor(user, clock, Action.Child.UPLOAD_IMAGE, childId)

        val contentType = checkFileContentType(file.inputStream, allowedContentTypes)

        val imageSize = Imaging.getImageSize(file.inputStream, null)

        if (imageSize.getWidth() > maxImageSize || imageSize.getHeight() > maxImageSize) {
            throw BadRequest("Image size must not exceed $maxImageSize pixels")
        }

        db.connect { dbc -> replaceImage(dbc, documentClient, bucket, childId, file, contentType) }
    }

    @DeleteMapping("/children/{childId}/image")
    fun deleteImage(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable childId: ChildId
    ) {
        Audit.ChildImageDelete.log(targetId = childId)
        accessControl.requirePermissionFor(user, clock, Action.Child.DELETE_IMAGE, childId)

        db.connect { dbc -> removeImage(dbc, documentClient, bucket, childId) }
    }

    @GetMapping(value = ["/child-images/{imageId}", "/citizen/child-images/{imageId}"])
    fun getImage(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable imageId: ChildImageId
    ): ResponseEntity<Any> {
        Audit.ChildImageDownload.log(targetId = imageId)
        accessControl.requirePermissionFor(user, clock, Action.ChildImage.DOWNLOAD, imageId)

        val key = "$childImagesBucketPrefix$imageId"
        return documentClient.responseInline(bucket, key, null)
    }
}
