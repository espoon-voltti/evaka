// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import FiniteDateRange from 'lib-common/finite-date-range'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'
import { AssistanceMeasure } from 'lib-customizations/types'

export interface AssistanceNeed {
  id: UUID
  childId: UUID
  startDate: LocalDate
  endDate: LocalDate
  capacityFactor: number
  description: string
  bases: Set<string>
  otherBasis: string
}

export type { AssistanceMeasure }

export interface AssistanceAction {
  id: UUID
  childId: UUID
  startDate: LocalDate
  endDate: LocalDate
  actions: Set<string>
  otherAction: string
  measures: Set<AssistanceMeasure>
}

export interface AssistanceActionOption {
  value: string
  nameFi: string
}

export interface AssistanceBasisOption {
  value: string
  nameFi: string
  descriptionFi: string | null
}

export interface AdditionalInformation {
  allergies: string
  diet: string
  additionalInfo: string
  preferredName: string | null
  medication: string
}

export interface ChildBackupCare {
  id: UUID
  unit: {
    id: UUID
    name: string
  }
  group?: {
    id: UUID
    name: string
  }
  period: FiniteDateRange
}

export interface ChildBackupPickup {
  id: UUID
  childId: UUID
  name: string
  phone: string
}
