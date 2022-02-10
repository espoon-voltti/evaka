// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import FiniteDateRange from 'lib-common/finite-date-range'
import { Action } from 'lib-common/generated/action'
import {
  AssistanceBasisOption,
  AssistanceNeed as ServiceAssistanceNeed
} from 'lib-common/generated/api-types/assistanceneed'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'
import { AssistanceMeasure } from 'lib-customizations/types'

export type { AssistanceBasisOption }

export interface AssistanceNeed extends Omit<ServiceAssistanceNeed, 'bases'> {
  bases: Set<string>
}

export interface AssistanceNeedResponse {
  need: AssistanceNeed
  permittedActions: Action.AssistanceNeed[]
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

export interface AssistanceActionResponse {
  action: AssistanceAction
  permittedActions: Action.AssistanceAction[]
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
