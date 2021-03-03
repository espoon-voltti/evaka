// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from '@evaka/lib-common/local-date'
import { UUID } from '../types/index'

export interface CaretakersResponse {
  unitName: string
  groupName: string
  caretakers: CaretakerAmount[]
}

export interface CaretakerAmount {
  id: UUID
  groupId: UUID
  startDate: LocalDate
  endDate: LocalDate | null
  amount: number
}
