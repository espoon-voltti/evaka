// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.childimages

import evaka.core.s3.ContentTypePattern
import evaka.core.s3.DocumentKey
import evaka.core.s3.DocumentService
import evaka.core.shared.ChildId
import evaka.core.shared.ChildImageId
import evaka.core.shared.db.Database
import evaka.core.shared.domain.HelsinkiDateTime
import org.springframework.web.multipart.MultipartFile

data class ChildImage(val id: ChildImageId, val childId: ChildId, val updated: HelsinkiDateTime)

val allowedContentTypes = setOf(ContentTypePattern.JPEG, ContentTypePattern.PNG)

fun replaceImage(
    db: Database.Connection,
    documentClient: DocumentService,
    childId: ChildId,
    file: MultipartFile,
    contentType: String,
): ChildImageId {
    var deletedId: ChildImageId? = null
    val imageId =
        db.transaction { tx ->
            deletedId = tx.deleteChildImage(childId)
            val imageId = tx.insertChildImage(childId)

            documentClient.upload(DocumentKey.ChildImage(imageId), file.bytes, contentType)
            imageId
        }
    deletedId?.let { documentClient.delete(DocumentKey.ChildImage(it)) }
    return imageId
}

fun removeImage(
    tx: Database.Transaction,
    documentClient: DocumentService,
    childId: ChildId,
): ChildImageId? =
    tx.deleteChildImage(childId)?.also { imageId ->
        documentClient.delete(DocumentKey.ChildImage(imageId))
    }
