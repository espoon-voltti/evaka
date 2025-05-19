// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import type { AttachmentId } from 'lib-common/generated/api-types/shared'
import type { Uri } from 'lib-common/uri'
import { client } from '../../client'
import { uri } from 'lib-common/uri'


/**
* Generated from fi.espoo.evaka.attachment.AttachmentsController.getAttachment
*/
export function getAttachment(
  request: {
    attachmentId: AttachmentId,
    requestedFilename: string
  }
): { url: Uri } {
  return {
    url: uri`/employee-mobile/attachments/${request.attachmentId}/download/${request.requestedFilename}`.withBaseUrl(client.defaults.baseURL ?? '')
  }
}
