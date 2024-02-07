// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { mutation, query } from 'lib-common/query'

import { getPreschoolTermsResult } from '../../api/applications'
import { createQueryKeys } from '../../query'

import {
  createFixedPeriodQuestionnaire,
  createHolidayPeriod,
  deleteHolidayPeriod,
  deleteQuestionnaire,
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
  preschoolTerms: () => ['preschoolTerms']
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

export const preschoolTermsQuery = query({
  api: getPreschoolTermsResult,
  queryKey: () => queryKeys.preschoolTerms()
})

export const questionnaireQuery = query({
  api: getQuestionnaire,
  queryKey: queryKeys.questionnaire
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
