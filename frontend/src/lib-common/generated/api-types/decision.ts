// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import type { Action } from '../action'
import type { ApplicationId } from './shared'
import type { DaycareId } from './shared'
import type { DecisionGenericReasoningId } from './shared'
import type { DecisionId } from './shared'
import type { DecisionIndividualReasoningId } from './shared'
import HelsinkiDateTime from '../../helsinki-date-time'
import type { JsonOf } from '../../json'
import LocalDate from '../../local-date'
import type { PersonId } from './shared'
import type { ProviderType } from './daycare'

/**
* Generated from evaka.core.decision.Decision
*/
export interface Decision {
  applicationId: ApplicationId
  archivedAt: HelsinkiDateTime | null
  childId: PersonId
  childName: string
  createdBy: string
  decisionNumber: number
  documentContainsContactInfo: boolean
  documentKey: string | null
  endDate: LocalDate
  id: DecisionId
  requestedStartDate: LocalDate | null
  resolved: LocalDate | null
  resolvedByName: string | null
  sentDate: LocalDate | null
  startDate: LocalDate
  status: DecisionStatus
  type: DecisionType
  unit: DecisionUnit
}

/**
* Generated from evaka.core.decision.DecisionDraft
*/
export interface DecisionDraft {
  endDate: LocalDate
  id: DecisionId
  planned: boolean
  startDate: LocalDate
  type: DecisionType
  unitId: DaycareId
}

/**
* Generated from evaka.core.decision.DecisionDraftUpdate
*/
export interface DecisionDraftUpdate {
  endDate: LocalDate
  id: DecisionId
  planned: boolean
  startDate: LocalDate
  unitId: DaycareId
}

/**
* Generated from evaka.core.decision.reasoning.DecisionGenericReasoning
*/
export interface DecisionGenericReasoning {
  body: DecisionGenericReasoningBody
  createdAt: HelsinkiDateTime
  id: DecisionGenericReasoningId
  modifiedAt: HelsinkiDateTime
}

/**
* Generated from evaka.core.decision.reasoning.DecisionGenericReasoningBody
*/
export interface DecisionGenericReasoningBody {
  collectionType: DecisionReasoningCollectionType
  ready: boolean
  textFi: string
  textSv: string
  validFrom: LocalDate
}

/**
* Generated from evaka.core.decision.reasoning.DecisionIndividualReasoning
*/
export interface DecisionIndividualReasoning {
  body: DecisionIndividualReasoningBody
  createdAt: HelsinkiDateTime
  id: DecisionIndividualReasoningId
  modifiedAt: HelsinkiDateTime
  removedAt: HelsinkiDateTime | null
}

/**
* Generated from evaka.core.decision.reasoning.DecisionIndividualReasoningBody
*/
export interface DecisionIndividualReasoningBody {
  collectionType: DecisionReasoningCollectionType
  textFi: string
  textSv: string
  titleFi: string
  titleSv: string
}

/**
* Generated from evaka.core.decision.reasoning.DecisionReasoningCollectionType
*/
export type DecisionReasoningCollectionType =
  | 'DAYCARE'
  | 'PRESCHOOL'

/**
* Generated from evaka.core.decision.DecisionStatus
*/
export type DecisionStatus =
  | 'PENDING'
  | 'ACCEPTED'
  | 'REJECTED'

/**
* Generated from evaka.core.decision.DecisionType
*/
export type DecisionType =
  | 'CLUB'
  | 'DAYCARE'
  | 'DAYCARE_PART_TIME'
  | 'PRESCHOOL'
  | 'PRESCHOOL_DAYCARE'
  | 'PRESCHOOL_CLUB'
  | 'PREPARATORY_EDUCATION'

/**
* Generated from evaka.core.decision.DecisionUnit
*/
export interface DecisionUnit {
  daycareDecisionName: string
  decisionHandler: string
  decisionHandlerAddress: string
  id: DaycareId
  manager: string | null
  name: string
  phone: string | null
  postOffice: string
  postalCode: string
  preschoolDecisionName: string
  providerType: ProviderType
  streetAddress: string
}

/**
* Generated from evaka.core.decision.DecisionController.DecisionWithPermittedActions
*/
export interface DecisionWithPermittedActions {
  data: Decision
  permittedActions: Action.Decision[]
}


export function deserializeJsonDecision(json: JsonOf<Decision>): Decision {
  return {
    ...json,
    archivedAt: (json.archivedAt != null) ? HelsinkiDateTime.parseIso(json.archivedAt) : null,
    endDate: LocalDate.parseIso(json.endDate),
    requestedStartDate: (json.requestedStartDate != null) ? LocalDate.parseIso(json.requestedStartDate) : null,
    resolved: (json.resolved != null) ? LocalDate.parseIso(json.resolved) : null,
    sentDate: (json.sentDate != null) ? LocalDate.parseIso(json.sentDate) : null,
    startDate: LocalDate.parseIso(json.startDate)
  }
}


export function deserializeJsonDecisionDraft(json: JsonOf<DecisionDraft>): DecisionDraft {
  return {
    ...json,
    endDate: LocalDate.parseIso(json.endDate),
    startDate: LocalDate.parseIso(json.startDate)
  }
}


export function deserializeJsonDecisionDraftUpdate(json: JsonOf<DecisionDraftUpdate>): DecisionDraftUpdate {
  return {
    ...json,
    endDate: LocalDate.parseIso(json.endDate),
    startDate: LocalDate.parseIso(json.startDate)
  }
}


export function deserializeJsonDecisionGenericReasoning(json: JsonOf<DecisionGenericReasoning>): DecisionGenericReasoning {
  return {
    ...json,
    body: deserializeJsonDecisionGenericReasoningBody(json.body),
    createdAt: HelsinkiDateTime.parseIso(json.createdAt),
    modifiedAt: HelsinkiDateTime.parseIso(json.modifiedAt)
  }
}


export function deserializeJsonDecisionGenericReasoningBody(json: JsonOf<DecisionGenericReasoningBody>): DecisionGenericReasoningBody {
  return {
    ...json,
    validFrom: LocalDate.parseIso(json.validFrom)
  }
}


export function deserializeJsonDecisionIndividualReasoning(json: JsonOf<DecisionIndividualReasoning>): DecisionIndividualReasoning {
  return {
    ...json,
    createdAt: HelsinkiDateTime.parseIso(json.createdAt),
    modifiedAt: HelsinkiDateTime.parseIso(json.modifiedAt),
    removedAt: (json.removedAt != null) ? HelsinkiDateTime.parseIso(json.removedAt) : null
  }
}


export function deserializeJsonDecisionWithPermittedActions(json: JsonOf<DecisionWithPermittedActions>): DecisionWithPermittedActions {
  return {
    ...json,
    data: deserializeJsonDecision(json.data)
  }
}
