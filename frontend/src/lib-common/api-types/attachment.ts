// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { AttachmentId } from 'lib-common/generated/api-types/shared'

export interface Attachment {
  id: AttachmentId
  name: string
  contentType: string
}
