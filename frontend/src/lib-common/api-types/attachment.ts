// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { UUID } from '../types'

export interface Attachment {
  id: UUID
  name: string
  contentType: string
}
