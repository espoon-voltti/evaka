// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.childimages

import fi.espoo.evaka.Audit
import fi.espoo.evaka.AuditId
import fi.espoo.evaka.s3.DocumentKey
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
