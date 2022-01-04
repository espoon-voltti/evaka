// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.childimages

import fi.espoo.evaka.s3.ContentType
import fi.espoo.evaka.s3.DocumentService
import fi.espoo.evaka.s3.DocumentWrapper
import fi.espoo.evaka.s3.getAndCheckFileContentType
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.ChildImageId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

data class ChildImage(
    val id: ChildImageId,
    val childId: ChildId,
    val updated: HelsinkiDateTime
)

const val childImagesBucketPrefix = "child-images/"

val allowedContentTypes = setOf(ContentType.JPEG, ContentType.PNG)

fun replaceImage(
    db: Database.Connection,
    documentClient: DocumentService,
    bucket: String,
    childId: UUID,
    file: MultipartFile
) {
    val contentType = getAndCheckFileContentType(file.inputStream, allowedContentTypes)

    var deletedId: ChildImageId? = null
    db.transaction { tx ->
        deletedId = tx.deleteChildImage(childId)
        val imageId = tx.insertChildImage(childId)

        documentClient.upload(
            bucket,
            DocumentWrapper(
                name = "$childImagesBucketPrefix$imageId",
                bytes = file.bytes
            ),
            contentType
        )
    }
    if (deletedId != null) {
        documentClient.delete(bucket, "$childImagesBucketPrefix$deletedId")
    }
}

fun removeImage(
    db: Database.Connection,
    documentClient: DocumentService,
    bucket: String,
    childId: UUID
) {
    db.transaction { tx ->
        tx.deleteChildImage(childId)
            ?.let { imageId -> documentClient.delete(bucket, "$childImagesBucketPrefix$imageId") }
    }
}
