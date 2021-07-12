// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from 'lib-common/local-date'
import { UUID } from './index'
import { PersonDetails } from './person'

export interface Partnership {
  id: UUID
  partners: PersonDetails[]
  startDate: LocalDate
  endDate: LocalDate | null
  conflict: boolean
}

export interface Parentship {
  id: UUID
  headOfChildId: string
  headOfChild: PersonDetails
  childId: string
  child: PersonDetails
  startDate: LocalDate
  endDate: LocalDate
  conflict: boolean
}
