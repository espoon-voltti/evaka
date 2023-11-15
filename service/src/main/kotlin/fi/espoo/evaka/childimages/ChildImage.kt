// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.childimages

import fi.espoo.evaka.s3.ContentTypePattern
import fi.espoo.evaka.s3.Document
import fi.espoo.evaka.s3.DocumentService
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.ChildImageId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import org.springframework.web.multipart.MultipartFile

data class ChildImage(val id: ChildImageId, val childId: ChildId, val updated: HelsinkiDateTime)

const val childImagesBucketPrefix = "child-images/"

val allowedContentTypes = setOf(ContentTypePattern.JPEG, ContentTypePattern.PNG)

fun replaceImage(
    db: Database.Connection,
    documentClient: DocumentService,
    bucket: String,
    childId: ChildId,
    file: MultipartFile,
    contentType: String
): ChildImageId {
    var deletedId: ChildImageId? = null
    val imageId =
        db.transaction { tx ->
            deletedId = tx.deleteChildImage(childId)
            val imageId = tx.insertChildImage(childId)

            documentClient.upload(
                bucket,
                Document(
                    name = "$childImagesBucketPrefix$imageId",
                    bytes = file.bytes,
                    contentType = contentType
                )
            )
            imageId
        }
    if (deletedId != null) {
        documentClient.delete(bucket, "$childImagesBucketPrefix$deletedId")
    }
    return imageId
}

fun removeImage(
    tx: Database.Transaction,
    documentClient: DocumentService,
    bucket: String,
    childId: ChildId
): ChildImageId? =
    tx.deleteChildImage(childId)?.also { imageId ->
        documentClient.delete(bucket, "$childImagesBucketPrefix$imageId")
    }
