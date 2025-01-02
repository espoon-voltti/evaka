// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

import FiniteDateRange from '../../finite-date-range'
import LocalDate from '../../local-date'
import { AbsenceType } from './absence'
import { HolidayPeriodId } from './shared'
import { HolidayQuestionnaireId } from './shared'
import { JsonOf } from '../../json'
import { PersonId } from './shared'
import { Translatable } from './shared'

/**
* Generated from fi.espoo.evaka.holidayperiod.ActiveQuestionnaire
*/
export interface ActiveQuestionnaire {
  eligibleChildren: PersonId[]
  previousAnswers: HolidayQuestionnaireAnswer[]
  questionnaire: HolidayQuestionnaire
}

/**
* Generated from fi.espoo.evaka.holidayperiod.FixedPeriodsBody
*/
export interface FixedPeriodsBody {
  fixedPeriods: Partial<Record<PersonId, FiniteDateRange | null>>
}

/**
* Generated from fi.espoo.evaka.holidayperiod.HolidayPeriod
*/
export interface HolidayPeriod {
  id: HolidayPeriodId
  period: FiniteDateRange
  reservationDeadline: LocalDate
  reservationsOpenOn: LocalDate
}

/**
* Generated from fi.espoo.evaka.holidayperiod.HolidayPeriodCreate
*/
export interface HolidayPeriodCreate {
  period: FiniteDateRange
  reservationDeadline: LocalDate
  reservationsOpenOn: LocalDate
}


export namespace HolidayPeriodEffect {
  /**
  * Generated from fi.espoo.evaka.holidayperiod.HolidayPeriodEffect.NotYetReservable
  */
  export interface NotYetReservable {
    type: 'NotYetReservable'
    period: FiniteDateRange
    reservationsOpenOn: LocalDate
  }

  /**
  * Generated from fi.espoo.evaka.holidayperiod.HolidayPeriodEffect.ReservationsClosed
  */
  export interface ReservationsClosed {
    type: 'ReservationsClosed'
  }

  /**
  * Generated from fi.espoo.evaka.holidayperiod.HolidayPeriodEffect.ReservationsOpen
  */
  export interface ReservationsOpen {
    type: 'ReservationsOpen'
  }
}

/**
* Generated from fi.espoo.evaka.holidayperiod.HolidayPeriodEffect
*/
export type HolidayPeriodEffect = HolidayPeriodEffect.NotYetReservable | HolidayPeriodEffect.ReservationsClosed | HolidayPeriodEffect.ReservationsOpen


/**
* Generated from fi.espoo.evaka.holidayperiod.HolidayPeriodUpdate
*/
export interface HolidayPeriodUpdate {
  reservationDeadline: LocalDate
  reservationsOpenOn: LocalDate
}


export namespace HolidayQuestionnaire {
  /**
  * Generated from fi.espoo.evaka.holidayperiod.HolidayQuestionnaire.FixedPeriodQuestionnaire
  */
  export interface FixedPeriodQuestionnaire {
    type: 'FIXED_PERIOD'
    absenceType: AbsenceType
    active: FiniteDateRange
    conditions: QuestionnaireConditions
    description: Translatable
    descriptionLink: Translatable
    id: HolidayQuestionnaireId
    periodOptionLabel: Translatable
    periodOptions: FiniteDateRange[]
    requiresStrongAuth: boolean
    title: Translatable
  }

  /**
  * Generated from fi.espoo.evaka.holidayperiod.HolidayQuestionnaire.OpenRangesQuestionnaire
  */
  export interface OpenRangesQuestionnaire {
    type: 'OPEN_RANGES'
    absenceType: AbsenceType
    absenceTypeThreshold: number
    active: FiniteDateRange
    conditions: QuestionnaireConditions
    description: Translatable
    descriptionLink: Translatable
    id: HolidayQuestionnaireId
    period: FiniteDateRange
    requiresStrongAuth: boolean
    title: Translatable
  }
}

/**
* Generated from fi.espoo.evaka.holidayperiod.HolidayQuestionnaire
*/
export type HolidayQuestionnaire = HolidayQuestionnaire.FixedPeriodQuestionnaire | HolidayQuestionnaire.OpenRangesQuestionnaire


/**
* Generated from fi.espoo.evaka.holidayperiod.HolidayQuestionnaireAnswer
*/
export interface HolidayQuestionnaireAnswer {
  childId: PersonId
  fixedPeriod: FiniteDateRange | null
  openRanges: FiniteDateRange[]
  questionnaireId: HolidayQuestionnaireId
}

/**
* Generated from fi.espoo.evaka.holidayperiod.OpenRangesBody
*/
export interface OpenRangesBody {
  openRanges: Partial<Record<PersonId, FiniteDateRange[]>>
}


export namespace QuestionnaireBody {
  /**
  * Generated from fi.espoo.evaka.holidayperiod.QuestionnaireBody.FixedPeriodQuestionnaireBody
  */
  export interface FixedPeriodQuestionnaireBody {
    type: 'FIXED_PERIOD'
    absenceType: AbsenceType
    active: FiniteDateRange
    conditions: QuestionnaireConditions
    description: Translatable
    descriptionLink: Translatable
    periodOptionLabel: Translatable
    periodOptions: FiniteDateRange[]
    requiresStrongAuth: boolean
    title: Translatable
  }

  /**
  * Generated from fi.espoo.evaka.holidayperiod.QuestionnaireBody.OpenRangesQuestionnaireBody
  */
  export interface OpenRangesQuestionnaireBody {
    type: 'OPEN_RANGES'
    absenceType: AbsenceType
    absenceTypeThreshold: number
    active: FiniteDateRange
    conditions: QuestionnaireConditions
    description: Translatable
    descriptionLink: Translatable
    period: FiniteDateRange
    requiresStrongAuth: boolean
    title: Translatable
  }
}

/**
* Generated from fi.espoo.evaka.holidayperiod.QuestionnaireBody
*/
export type QuestionnaireBody = QuestionnaireBody.FixedPeriodQuestionnaireBody | QuestionnaireBody.OpenRangesQuestionnaireBody


/**
* Generated from fi.espoo.evaka.holidayperiod.QuestionnaireConditions
*/
export interface QuestionnaireConditions {
  continuousPlacement: FiniteDateRange | null
}

/**
* Generated from fi.espoo.evaka.holidayperiod.QuestionnaireType
*/
export type QuestionnaireType =
  | 'FIXED_PERIOD'
  | 'OPEN_RANGES'


export function deserializeJsonActiveQuestionnaire(json: JsonOf<ActiveQuestionnaire>): ActiveQuestionnaire {
  return {
    ...json,
    previousAnswers: json.previousAnswers.map(e => deserializeJsonHolidayQuestionnaireAnswer(e)),
    questionnaire: deserializeJsonHolidayQuestionnaire(json.questionnaire)
  }
}


export function deserializeJsonFixedPeriodsBody(json: JsonOf<FixedPeriodsBody>): FixedPeriodsBody {
  return {
    ...json,
    fixedPeriods: Object.fromEntries(Object.entries(json.fixedPeriods).map(
      ([k, v]) => [k, v !== undefined ? (v != null) ? FiniteDateRange.parseJson(v) : null : v]
    ))
  }
}


export function deserializeJsonHolidayPeriod(json: JsonOf<HolidayPeriod>): HolidayPeriod {
  return {
    ...json,
    period: FiniteDateRange.parseJson(json.period),
    reservationDeadline: LocalDate.parseIso(json.reservationDeadline),
    reservationsOpenOn: LocalDate.parseIso(json.reservationsOpenOn)
  }
}


export function deserializeJsonHolidayPeriodCreate(json: JsonOf<HolidayPeriodCreate>): HolidayPeriodCreate {
  return {
    ...json,
    period: FiniteDateRange.parseJson(json.period),
    reservationDeadline: LocalDate.parseIso(json.reservationDeadline),
    reservationsOpenOn: LocalDate.parseIso(json.reservationsOpenOn)
  }
}



export function deserializeJsonHolidayPeriodEffectNotYetReservable(json: JsonOf<HolidayPeriodEffect.NotYetReservable>): HolidayPeriodEffect.NotYetReservable {
  return {
    ...json,
    period: FiniteDateRange.parseJson(json.period),
    reservationsOpenOn: LocalDate.parseIso(json.reservationsOpenOn)
  }
}
export function deserializeJsonHolidayPeriodEffect(json: JsonOf<HolidayPeriodEffect>): HolidayPeriodEffect {
  switch (json.type) {
    case 'NotYetReservable': return deserializeJsonHolidayPeriodEffectNotYetReservable(json)
    default: return json
  }
}


export function deserializeJsonHolidayPeriodUpdate(json: JsonOf<HolidayPeriodUpdate>): HolidayPeriodUpdate {
  return {
    ...json,
    reservationDeadline: LocalDate.parseIso(json.reservationDeadline),
    reservationsOpenOn: LocalDate.parseIso(json.reservationsOpenOn)
  }
}



export function deserializeJsonHolidayQuestionnaireFixedPeriodQuestionnaire(json: JsonOf<HolidayQuestionnaire.FixedPeriodQuestionnaire>): HolidayQuestionnaire.FixedPeriodQuestionnaire {
  return {
    ...json,
    active: FiniteDateRange.parseJson(json.active),
    conditions: deserializeJsonQuestionnaireConditions(json.conditions),
    periodOptions: json.periodOptions.map(e => FiniteDateRange.parseJson(e))
  }
}

export function deserializeJsonHolidayQuestionnaireOpenRangesQuestionnaire(json: JsonOf<HolidayQuestionnaire.OpenRangesQuestionnaire>): HolidayQuestionnaire.OpenRangesQuestionnaire {
  return {
    ...json,
    active: FiniteDateRange.parseJson(json.active),
    conditions: deserializeJsonQuestionnaireConditions(json.conditions),
    period: FiniteDateRange.parseJson(json.period)
  }
}
export function deserializeJsonHolidayQuestionnaire(json: JsonOf<HolidayQuestionnaire>): HolidayQuestionnaire {
  switch (json.type) {
    case 'FIXED_PERIOD': return deserializeJsonHolidayQuestionnaireFixedPeriodQuestionnaire(json)
    case 'OPEN_RANGES': return deserializeJsonHolidayQuestionnaireOpenRangesQuestionnaire(json)
    default: return json
  }
}


export function deserializeJsonHolidayQuestionnaireAnswer(json: JsonOf<HolidayQuestionnaireAnswer>): HolidayQuestionnaireAnswer {
  return {
    ...json,
    fixedPeriod: (json.fixedPeriod != null) ? FiniteDateRange.parseJson(json.fixedPeriod) : null,
    openRanges: json.openRanges.map(e => FiniteDateRange.parseJson(e))
  }
}


export function deserializeJsonOpenRangesBody(json: JsonOf<OpenRangesBody>): OpenRangesBody {
  return {
    ...json,
    openRanges: Object.fromEntries(Object.entries(json.openRanges).map(
      ([k, v]) => [k, v !== undefined ? v.map(e => FiniteDateRange.parseJson(e)) : v]
    ))
  }
}



export function deserializeJsonQuestionnaireBodyFixedPeriodQuestionnaireBody(json: JsonOf<QuestionnaireBody.FixedPeriodQuestionnaireBody>): QuestionnaireBody.FixedPeriodQuestionnaireBody {
  return {
    ...json,
    active: FiniteDateRange.parseJson(json.active),
    conditions: deserializeJsonQuestionnaireConditions(json.conditions),
    periodOptions: json.periodOptions.map(e => FiniteDateRange.parseJson(e))
  }
}

export function deserializeJsonQuestionnaireBodyOpenRangesQuestionnaireBody(json: JsonOf<QuestionnaireBody.OpenRangesQuestionnaireBody>): QuestionnaireBody.OpenRangesQuestionnaireBody {
  return {
    ...json,
    active: FiniteDateRange.parseJson(json.active),
    conditions: deserializeJsonQuestionnaireConditions(json.conditions),
    period: FiniteDateRange.parseJson(json.period)
  }
}
export function deserializeJsonQuestionnaireBody(json: JsonOf<QuestionnaireBody>): QuestionnaireBody {
  switch (json.type) {
    case 'FIXED_PERIOD': return deserializeJsonQuestionnaireBodyFixedPeriodQuestionnaireBody(json)
    case 'OPEN_RANGES': return deserializeJsonQuestionnaireBodyOpenRangesQuestionnaireBody(json)
    default: return json
  }
}


export function deserializeJsonQuestionnaireConditions(json: JsonOf<QuestionnaireConditions>): QuestionnaireConditions {
  return {
    ...json,
    continuousPlacement: (json.continuousPlacement != null) ? FiniteDateRange.parseJson(json.continuousPlacement) : null
  }
}
