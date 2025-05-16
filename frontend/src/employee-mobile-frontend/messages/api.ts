// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { AttachmentId } from 'lib-common/generated/api-types/shared'

import { getAttachment } from '../generated/api-clients/attachment'

export function getAttachmentUrl(
  attachmentId: AttachmentId,
  requestedFilename: string
): string {
  return getAttachment({ attachmentId, requestedFilename }).url.toString()
}
