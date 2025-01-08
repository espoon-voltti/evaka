// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Queries } from 'lib-common/query'

import {
  createPreschoolTerm,
  deletePreschoolTerm,
  getPreschoolTerms,
  updatePreschoolTerm,
  createClubTerm,
  deleteClubTerm,
  getClubTerms,
  updateClubTerm
} from '../../generated/api-clients/daycare'
import {
  createHolidayPeriod,
  createHolidayQuestionnaire,
  deleteHolidayPeriod,
  deleteHolidayQuestionnaire,
  getHolidayPeriod,
  getHolidayPeriods,
  getQuestionnaire,
  getQuestionnaires,
  updateHolidayPeriod,
  updateHolidayQuestionnaire
} from '../../generated/api-clients/holidayperiod'

const q = new Queries()

export const holidayPeriodsQuery = q.query(getHolidayPeriods)

export const holidayPeriodQuery = q.query(getHolidayPeriod)

export const questionnairesQuery = q.query(getQuestionnaires)

export const questionnaireQuery = q.query(getQuestionnaire)

export const preschoolTermsQuery = q.query(getPreschoolTerms)

export const createPreschoolTermMutation = q.mutation(createPreschoolTerm, [
  () => preschoolTermsQuery()
])

export const updatePreschoolTermMutation = q.mutation(updatePreschoolTerm, [
  () => preschoolTermsQuery()
])

export const deletePreschoolTermMutation = q.mutation(deletePreschoolTerm, [
  () => preschoolTermsQuery()
])

export const createHolidayPeriodMutation = q.mutation(createHolidayPeriod, [
  () => holidayPeriodsQuery()
])

export const updateHolidayPeriodMutation = q.mutation(updateHolidayPeriod, [
  ({ id }) => holidayPeriodQuery({ id }),
  () => holidayPeriodsQuery()
])

export const deleteHolidayPeriodMutation = q.mutation(deleteHolidayPeriod, [
  ({ id }) => holidayPeriodQuery({ id }),
  () => holidayPeriodsQuery()
])

export const createQuestionnaireMutation = q.mutation(
  createHolidayQuestionnaire,
  [() => questionnairesQuery()]
)

export const updateQuestionnaireMutation = q.mutation(
  updateHolidayQuestionnaire,
  [() => questionnairesQuery(), ({ id }) => questionnaireQuery({ id })]
)

export const deleteQuestionnaireMutation = q.mutation(
  deleteHolidayQuestionnaire,
  [({ id }) => questionnaireQuery({ id }), () => questionnairesQuery()]
)

export const clubTermsQuery = q.query(getClubTerms)

export const createClubTermMutation = q.mutation(createClubTerm, [
  () => clubTermsQuery()
])

export const updateClubTermMutation = q.mutation(updateClubTerm, [
  () => clubTermsQuery()
])

export const deleteClubTermMutation = q.mutation(deleteClubTerm, [
  () => clubTermsQuery()
])
