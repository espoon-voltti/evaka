// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications
/* eslint-disable import/order, prettier/prettier, @typescript-eslint/no-namespace, @typescript-eslint/no-redundant-type-constituents */

import LocalDate from '../../local-date'
import { ProviderType } from './daycare'
import { UUID } from '../../types'

/**
* Generated from fi.espoo.evaka.decision.Decision
*/
export interface Decision {
  applicationId: UUID
  childId: UUID
  childName: string
  createdBy: string
  decisionNumber: number
  documentKey: string | null
  endDate: LocalDate
  id: UUID
  otherGuardianDocumentKey: string | null
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
* Generated from fi.espoo.evaka.decision.DecisionDraft
*/
export interface DecisionDraft {
  endDate: LocalDate
  id: UUID
  planned: boolean
  startDate: LocalDate
  type: DecisionType
  unitId: UUID
}

/**
* Generated from fi.espoo.evaka.decision.DecisionDraftUpdate
*/
export interface DecisionDraftUpdate {
  endDate: LocalDate
  id: UUID
  planned: boolean
  startDate: LocalDate
  unitId: UUID
}

/**
* Generated from fi.espoo.evaka.decision.DecisionListResponse
*/
export interface DecisionListResponse {
  decisions: Decision[]
}

/**
* Generated from fi.espoo.evaka.decision.DecisionStatus
*/
export type DecisionStatus =
  | 'PENDING'
  | 'ACCEPTED'
  | 'REJECTED'

/**
* Generated from fi.espoo.evaka.decision.DecisionType
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
* Generated from fi.espoo.evaka.decision.DecisionUnit
*/
export interface DecisionUnit {
  daycareDecisionName: string
  decisionHandler: string
  decisionHandlerAddress: string
  id: UUID
  manager: string | null
  name: string
  phone: string | null
  postOffice: string
  postalCode: string
  preschoolDecisionName: string
  providerType: ProviderType
  streetAddress: string
}
