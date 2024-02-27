// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { mutation, query } from 'lib-common/query'

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
import { createQueryKeys } from '../../query'

const queryKeys = createQueryKeys('holidayPeriods', {
  holidayPeriods: () => ['holidayPeriods'],
  holidayPeriod: (id: string) => ['holidayPeriod', id],
  questionnaires: () => ['questionnaires'],
  questionnaire: (id: string) => ['questionnaire', id],
  preschoolTerms: () => ['preschoolTerms'],
  preschoolTerm: (id: string) => ['preschoolTerm', id],
  clubTerms: () => ['clubTerms'],
  clubTerm: (id: string) => ['clubTerm', id]
})

export const holidayPeriodsQuery = query({
  api: getHolidayPeriods,
  queryKey: queryKeys.holidayPeriods
})

export const holidayPeriodQuery = query({
  api: getHolidayPeriod,
  queryKey: ({ id }) => queryKeys.holidayPeriod(id)
})

export const questionnairesQuery = query({
  api: getQuestionnaires,
  queryKey: () => queryKeys.questionnaires()
})

export const questionnaireQuery = query({
  api: getQuestionnaire,
  queryKey: ({ id }) => queryKeys.questionnaire(id)
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
  invalidateQueryKeys: ({ id }) => [
    queryKeys.holidayPeriods(),
    queryKeys.holidayPeriod(id)
  ]
})

export const createFixedPeriodQuestionnaireMutation = mutation({
  api: createHolidayQuestionnaire,
  invalidateQueryKeys: () => [queryKeys.questionnaires()]
})

export const updateFixedPeriodQuestionnaireMutation = mutation({
  api: updateHolidayQuestionnaire,
  invalidateQueryKeys: ({ id }) => [
    queryKeys.questionnaires(),
    queryKeys.questionnaire(id)
  ]
})

export const deleteQuestionnaireMutation = mutation({
  api: deleteHolidayQuestionnaire,
  invalidateQueryKeys: ({ id }) => [
    queryKeys.questionnaires(),
    queryKeys.questionnaire(id)
  ]
})

export const clubTermsQuery = query({
  api: getClubTerms,
  queryKey: () => queryKeys.clubTerms()
})

export const createClubTermMutation = mutation({
  api: createClubTerm,
  invalidateQueryKeys: () => [queryKeys.clubTerms()]
})

export const updateClubTermMutation = mutation({
  api: updateClubTerm,
  invalidateQueryKeys: ({ id }) => [
    queryKeys.clubTerms(),
    queryKeys.clubTerm(id)
  ]
})

export const deleteClubTermMutation = mutation({
  api: deleteClubTerm,
  invalidateQueryKeys: ({ id }) => [
    queryKeys.clubTerms(),
    queryKeys.clubTerm(id)
  ]
})
