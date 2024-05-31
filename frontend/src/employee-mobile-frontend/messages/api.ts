// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { UUID } from 'lib-common/types'

import { API_URL } from '../client'

export function getAttachmentUrl(
  attachmentId: UUID,
  requestedFilename: string
): string {
  const encodedFilename = encodeURIComponent(requestedFilename)
  return `${API_URL}/attachments/${attachmentId}/download/${encodedFilename}`
}
