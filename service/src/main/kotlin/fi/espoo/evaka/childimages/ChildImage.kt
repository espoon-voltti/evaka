// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.childimages

import fi.espoo.evaka.s3.DocumentService
import fi.espoo.evaka.s3.DocumentWrapper
import fi.espoo.evaka.s3.checkFileContentType
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

data class ChildImage(
    val id: UUID,
    val childId: UUID
)

const val childImagesBucketPrefix = "child-images/"

fun replaceImage(
    db: Database,
    documentClient: DocumentService,
    bucket: String,
    childId: UUID,
    file: MultipartFile
) {
    val contentType = file.contentType ?: throw BadRequest("Missing content type")
    if (contentType != "image/jpeg") {
        throw BadRequest("Unsupported content type")
    }
    checkFileContentType(file.inputStream, contentType)

    var deletedId: UUID? = null
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
    db: Database,
    documentClient: DocumentService,
    bucket: String,
    childId: UUID
) {
    db.transaction { tx ->
        tx.deleteChildImage(childId)
            ?.let { imageId -> documentClient.delete(bucket, "$childImagesBucketPrefix$imageId") }
    }
}
