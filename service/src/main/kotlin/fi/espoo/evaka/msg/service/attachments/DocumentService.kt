// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.msg.service.attachments

interface DocumentService {
    /**
     * Get attachment by attachment path. Subclasses can set add more restrictions to path value.
     */
    fun getDocument(bucket: String, key: String): ByteArray
}
