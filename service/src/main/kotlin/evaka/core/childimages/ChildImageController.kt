// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.childimages

import evaka.core.Audit
import evaka.core.AuditId
import evaka.core.ExcludeCodeGen
import evaka.core.s3.DocumentKey
import evaka.core.s3.DocumentService
import evaka.core.s3.checkFileContentType
import evaka.core.shared.ChildId
import evaka.core.shared.ChildImageId
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.db.Database
import evaka.core.shared.domain.BadRequest
import evaka.core.shared.domain.EvakaClock
import evaka.core.shared.security.AccessControl
import evaka.core.shared.security.Action
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
) {
    @PutMapping(
        "/employee-mobile/children/{childId}/image",
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE],
    )
    fun putImage(
        db: Database,
        user: AuthenticatedUser.MobileDevice,
        clock: EvakaClock,
        @PathVariable childId: ChildId,
        @RequestPart("file") file: MultipartFile,
    ) {
        val contentType = checkFileContentType(file.inputStream, allowedContentTypes)

        val imageSize = Imaging.getImageSize(file.inputStream, null)

        if (imageSize.getWidth() > maxImageSize || imageSize.getHeight() > maxImageSize) {
            throw BadRequest("Image size must not exceed $maxImageSize pixels")
        }

        val imageId =
            db.connect { dbc ->
                dbc.read {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Child.UPLOAD_IMAGE,
                        childId,
                    )
                }

                replaceImage(dbc, documentClient, childId, file, contentType)
            }
        Audit.ChildImageUpload.log(
            targetId = AuditId(childId),
            objectId = AuditId(imageId),
            meta = mapOf("size" to file.size, "contentType" to contentType),
        )
    }

    @DeleteMapping("/employee-mobile/children/{childId}/image")
    fun deleteImage(
        db: Database,
        user: AuthenticatedUser.MobileDevice,
        clock: EvakaClock,
        @PathVariable childId: ChildId,
    ) {
        val imageId =
            db.connect { dbc ->
                dbc.read {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Child.DELETE_IMAGE,
                        childId,
                    )
                }
                dbc.transaction { tx -> removeImage(tx, documentClient, childId) }
            }
        Audit.ChildImageDelete.log(
            targetId = AuditId(childId),
            objectId = imageId?.let(AuditId::invoke),
        )
    }

    @GetMapping("/citizen/child-images/{imageId}")
    fun getImageCitizen(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @PathVariable imageId: ChildImageId,
    ): ResponseEntity<Any> = getImageInternal(db, user, clock, imageId)

    @ExcludeCodeGen
    @GetMapping("/employee-mobile/child-images/{imageId}")
    fun getImage(
        db: Database,
        user: AuthenticatedUser.MobileDevice,
        clock: EvakaClock,
        @PathVariable imageId: ChildImageId,
    ): ResponseEntity<Any> = getImageInternal(db, user, clock, imageId)

    private fun getImageInternal(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        imageId: ChildImageId,
    ): ResponseEntity<Any> {
        db.connect { dbc ->
            dbc.read {
                accessControl.requirePermissionFor(
                    it,
                    user,
                    clock,
                    Action.ChildImage.DOWNLOAD,
                    imageId,
                )
            }
        }

        val documentLocation = documentClient.locate(DocumentKey.ChildImage(imageId))
        return documentClient.responseInline(documentLocation, null).also {
            Audit.ChildImageDownload.log(targetId = AuditId(imageId))
        }
    }
}
