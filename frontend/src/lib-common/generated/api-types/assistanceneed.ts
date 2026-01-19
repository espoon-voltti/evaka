// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import type { Action } from '../action'
import type { AssistanceNeedVoucherCoefficientId } from './shared'
import type { EvakaUser } from './user'
import FiniteDateRange from '../../finite-date-range'
import HelsinkiDateTime from '../../helsinki-date-time'
import type { JsonOf } from '../../json'
import type { PersonId } from './shared'

/**
* Generated from fi.espoo.evaka.assistanceneed.vouchercoefficient.AssistanceNeedVoucherCoefficient
*/
export interface AssistanceNeedVoucherCoefficient {
  childId: PersonId
  coefficient: number
  id: AssistanceNeedVoucherCoefficientId
  modifiedAt: HelsinkiDateTime
  modifiedBy: EvakaUser
  validityPeriod: FiniteDateRange
}

/**
* Generated from fi.espoo.evaka.assistanceneed.vouchercoefficient.AssistanceNeedVoucherCoefficientRequest
*/
export interface AssistanceNeedVoucherCoefficientRequest {
  coefficient: number
  validityPeriod: FiniteDateRange
}

/**
* Generated from fi.espoo.evaka.assistanceneed.vouchercoefficient.AssistanceNeedVoucherCoefficientResponse
*/
export interface AssistanceNeedVoucherCoefficientResponse {
  permittedActions: Action.AssistanceNeedVoucherCoefficient[]
  voucherCoefficient: AssistanceNeedVoucherCoefficient
}


export function deserializeJsonAssistanceNeedVoucherCoefficient(json: JsonOf<AssistanceNeedVoucherCoefficient>): AssistanceNeedVoucherCoefficient {
  return {
    ...json,
    modifiedAt: HelsinkiDateTime.parseIso(json.modifiedAt),
    validityPeriod: FiniteDateRange.parseJson(json.validityPeriod)
  }
}


export function deserializeJsonAssistanceNeedVoucherCoefficientRequest(json: JsonOf<AssistanceNeedVoucherCoefficientRequest>): AssistanceNeedVoucherCoefficientRequest {
  return {
    ...json,
    validityPeriod: FiniteDateRange.parseJson(json.validityPeriod)
  }
}


export function deserializeJsonAssistanceNeedVoucherCoefficientResponse(json: JsonOf<AssistanceNeedVoucherCoefficientResponse>): AssistanceNeedVoucherCoefficientResponse {
  return {
    ...json,
    voucherCoefficient: deserializeJsonAssistanceNeedVoucherCoefficient(json.voucherCoefficient)
  }
}
