// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { mutation, query } from 'lib-common/query'

import {
  createPreschoolTerm,
  deletePreschoolTerm,
  getPreschoolTerms,
  updatePreschoolTerm
} from '../../generated/api-clients/daycare'
import { createQueryKeys } from '../../query'

import {
  createFixedPeriodQuestionnaire,
  createHolidayPeriod,
  deleteHolidayPeriod,
  deleteQuestionnaire,
  getClubTermsResult,
  getHolidayPeriod,
  getHolidayPeriods,
  getQuestionnaire,
  getQuestionnaires,
  updateFixedPeriodQuestionnaire,
  updateHolidayPeriod
} from './api'

const queryKeys = createQueryKeys('holidayPeriods', {
  holidayPeriods: () => ['holidayPeriods'],
  holidayPeriod: (id: string) => ['holidayPeriod', id],
  questionnaires: () => ['questionnaires'],
  questionnaire: (id: string) => ['questionnaire', id],
  preschoolTerms: () => ['preschoolTerms'],
  preschoolTerm: (id: string) => ['preschoolTerm', id],
  clubTerms: () => ['clubTerms']
})

export const holidayPeriodsQuery = query({
  api: getHolidayPeriods,
  queryKey: queryKeys.holidayPeriods
})

export const holidayPeriodQuery = query({
  api: getHolidayPeriod,
  queryKey: queryKeys.holidayPeriod
})

export const questionnairesQuery = query({
  api: getQuestionnaires,
  queryKey: () => queryKeys.questionnaires()
})

export const questionnaireQuery = query({
  api: getQuestionnaire,
  queryKey: queryKeys.questionnaire
})

export const preschoolTermsQuery = query({
  api: getPreschoolTerms,
  queryKey: () => queryKeys.preschoolTerms()
})

export const createPreschoolTermMutation = mutation({
  api: createPreschoolTerm,
  invalidateQueryKeys: () => [queryKeys.preschoolTerms()]
})

export const updatePreschoolTermMutation = mutation({
  api: updatePreschoolTerm,
  invalidateQueryKeys: ({ id }) => [
    queryKeys.preschoolTerms(),
    queryKeys.preschoolTerm(id)
  ]
})

export const deletePreschoolTermMutation = mutation({
  api: deletePreschoolTerm,
  invalidateQueryKeys: ({ id }) => [
    queryKeys.preschoolTerms(),
    queryKeys.preschoolTerm(id)
  ]
})

export const createHolidayPeriodMutation = mutation({
  api: createHolidayPeriod,
  invalidateQueryKeys: () => [queryKeys.holidayPeriods()]
})

export const updateHolidayPeriodMutation = mutation({
  api: updateHolidayPeriod,
  invalidateQueryKeys: ({ id }) => [
    queryKeys.holidayPeriods(),
    queryKeys.holidayPeriod(id)
  ]
})

export const deleteHolidayPeriodMutation = mutation({
  api: deleteHolidayPeriod,
  invalidateQueryKeys: (id) => [
    queryKeys.holidayPeriods(),
    queryKeys.holidayPeriod(id)
  ]
})

export const createFixedPeriodQuestionnaireMutation = mutation({
  api: createFixedPeriodQuestionnaire,
  invalidateQueryKeys: () => [queryKeys.questionnaires()]
})

export const updateFixedPeriodQuestionnaireMutation = mutation({
  api: updateFixedPeriodQuestionnaire,
  invalidateQueryKeys: ({ id }) => [
    queryKeys.questionnaires(),
    queryKeys.questionnaire(id)
  ]
})

export const deleteQuestionnaireMutation = mutation({
  api: deleteQuestionnaire,
  invalidateQueryKeys: (id) => [
    queryKeys.questionnaires(),
    queryKeys.questionnaire(id)
  ]
})

export const clubTermsQuery = query({
  api: getClubTermsResult,
  queryKey: () => queryKeys.clubTerms()
})
