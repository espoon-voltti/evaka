// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { Action } from 'lib-common/generated/action'
import type { AssistanceMeasure } from 'lib-common/generated/api-types/assistanceaction'
import type { AssistanceNeed as ServiceAssistanceNeed } from 'lib-common/generated/api-types/assistanceneed'
import type LocalDate from 'lib-common/local-date'
import type { UUID } from 'lib-common/types'

export interface AssistanceNeed extends Omit<ServiceAssistanceNeed, 'bases'> {
  bases: Set<string>
}

export interface AssistanceNeedResponse {
  need: AssistanceNeed
  permittedActions: Action.AssistanceNeed[]
}

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
