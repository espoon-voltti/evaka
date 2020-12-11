// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import axios from 'axios'
import { UUID } from '@/types'
import { JsonOf } from '@evaka/lib-common/src/json'

const client = axios.create({
  baseURL: '/api/application'
})

interface DecisionsResponse {
  decisions: Decision[]
}

export type DecisionStatus = 'PENDING' | 'ACCEPTED' | 'REJECTED'

export interface Decision {
  id: UUID
  type: DecisionType
  startDate: Date
  endDate: Date
  unit: string
  applicationId: UUID
  childId: UUID
  childName: string
  decisionNumber: number
  sentDate: Date
  status: DecisionStatus
  requestedStartDate: Date | undefined
  resolved: Date | undefined
}

export type DecisionType =
  | 'CLUB'
  | 'DAYCARE'
  | 'DAYCARE_PART_TIME'
  | 'PRESCHOOL'
  | 'PRESCHOOL_DAYCARE'
  | 'PREPARATORY_EDUCATION'

export async function getDecisions(): Promise<Decision[]> {
  const { data } = await client.get<JsonOf<DecisionsResponse>>('/decisions')
  return data.decisions.map((d) => ({
    ...d,
    startDate: new Date(d.startDate),
    endDate: new Date(d.endDate),
    sentDate: new Date(d.sentDate),
    requestedStartDate: d.requestedStartDate
      ? new Date(d.requestedStartDate)
      : undefined,
    resolved: d.resolved ? new Date(d.resolved) : undefined
  }))
}

export async function acceptDecision(
  applicationId: UUID,
  decisionId: UUID,
  requestedStartDate: Date
) {
  await client.post(`/enduser/v2/applications/${encodeURIComponent(applicationId)}/actions/accept-decision`, {
    decisionId,
    requestedStartDate
  })
}

export async function rejectDecision(applicationId: UUID, decisionId: UUID) {
  await client.post(`/enduser/v2/applications/${encodeURIComponent(applicationId)}/actions/reject-decision`, {
    decisionId
  })
}
