// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Failure, Result, Success } from 'lib-common/api'
import FiniteDateRange from 'lib-common/finite-date-range'
import {
  AssistanceNeedDecision,
  AssistanceNeedDecisionCitizenListItem
} from 'lib-common/generated/api-types/assistanceneed'
import {
  Child,
  ChildrenResponse
} from 'lib-common/generated/api-types/children'
import {
  ChildPlacement,
  ChildPlacementResponse,
  PlacementTerminationRequestBody,
  TerminatablePlacementGroup
} from 'lib-common/generated/api-types/placement'
import { JsonOf } from 'lib-common/json'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'

import { client } from '../api-client'

export function getChildren(): Promise<Result<ChildrenResponse>> {
  return client
    .get<JsonOf<ChildrenResponse>>('/citizen/children')
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}

export function getChild(childId: UUID): Promise<Result<Child>> {
  return client
    .get<JsonOf<Child>>(`/citizen/children/${childId}`)
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}

const deserializeChildPlacement = ({
  startDate,
  endDate,
  terminationRequestedDate,
  ...rest
}: JsonOf<ChildPlacement>): ChildPlacement => ({
  ...rest,
  startDate: LocalDate.parseIso(startDate),
  endDate: LocalDate.parseIso(endDate),
  terminationRequestedDate: terminationRequestedDate
    ? LocalDate.parseIso(terminationRequestedDate)
    : null
})

const deserializeTerminatablePlacementGroup = ({
  startDate,
  endDate,
  placements,
  additionalPlacements,
  ...rest
}: JsonOf<TerminatablePlacementGroup>): TerminatablePlacementGroup => ({
  ...rest,
  startDate: LocalDate.parseIso(startDate),
  endDate: LocalDate.parseIso(endDate),
  placements: placements.map(deserializeChildPlacement),
  additionalPlacements: additionalPlacements.map(deserializeChildPlacement)
})

export function getPlacements(
  childId: UUID
): Promise<Result<ChildPlacementResponse>> {
  return client
    .get<JsonOf<ChildPlacementResponse>>(
      `/citizen/children/${childId}/placements`
    )
    .then(({ data: { placements, ...rest } }) =>
      Success.of({
        ...rest,
        placements: placements.map(deserializeTerminatablePlacementGroup)
      })
    )
    .catch((e) => Failure.fromError(e))
}

export function terminatePlacement(
  childId: UUID,
  body: PlacementTerminationRequestBody
): Promise<Result<void>> {
  return client
    .post(`/citizen/children/${childId}/placements/terminate`, body)
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))
}

export function getAssistanceNeedDecisions(
  childId: UUID
): Promise<Result<AssistanceNeedDecisionCitizenListItem[]>> {
  return client
    .get<JsonOf<AssistanceNeedDecisionCitizenListItem[]>>(
      `/citizen/children/${childId}/assistance-need-decisions`
    )
    .then(({ data }) =>
      Success.of(
        data.map((decision) => ({
          ...decision,
          decisionMade:
            decision.decisionMade === null
              ? null
              : LocalDate.parseIso(decision.decisionMade),
          endDate:
            decision.endDate === null
              ? null
              : LocalDate.parseIso(decision.endDate),
          startDate:
            decision.startDate === null
              ? null
              : LocalDate.parseIso(decision.startDate)
        }))
      )
    )
    .catch((e) => Failure.fromError(e))
}

const parseDate = (str: string | null): LocalDate | null =>
  str ? LocalDate.parseIso(str) : null

const mapToAssistanceNeedDecision = (
  data: JsonOf<AssistanceNeedDecision>
): AssistanceNeedDecision => ({
  ...data,
  startDate: parseDate(data.startDate),
  endDate: parseDate(data.endDate),
  assistanceServicesTime:
    data.assistanceServicesTime !== null
      ? FiniteDateRange.parseJson(data.assistanceServicesTime)
      : null,
  decisionMade: parseDate(data.decisionMade),
  guardiansHeardOn: parseDate(data.guardiansHeardOn),
  sentForDecision: parseDate(data.sentForDecision)
})

export function getAssistanceNeedDecision(
  id: UUID
): Promise<Result<AssistanceNeedDecision>> {
  return client
    .get<JsonOf<AssistanceNeedDecision>>(
      `/citizen/children/assistance-need-decision/${id}`
    )
    .then((res) => Success.of(mapToAssistanceNeedDecision(res.data)))
    .catch((e) => Failure.fromError(e))
}
