// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { mutation, query } from 'lib-common/query'

import { queryKeys as calendarQueryKeys } from '../calendar/queries'
import { createQueryKeys } from '../query'

import {
  getActiveQuestionnaires,
  getHolidayPeriods,
  postFixedPeriodQuestionnaireAnswer
} from './api'

const queryKeys = createQueryKeys('holidayPeriods', {
  holidayPeriodsQuery: () => ['holidayPeriods'],
  activeQuestionnairesQuery: () => ['activeQuestionnaires']
})

export const holidayPeriodsQuery = query({
  api: getHolidayPeriods,
  queryKey: queryKeys.holidayPeriodsQuery
})

export const activeQuestionnairesQuery = query({
  api: getActiveQuestionnaires,
  queryKey: queryKeys.activeQuestionnairesQuery
})

export const answerFixedPeriodQuestionnaireMutation = mutation({
  api: postFixedPeriodQuestionnaireAnswer,
  invalidateQueryKeys: () => [
    activeQuestionnairesQuery.queryKey,
    calendarQueryKeys.allReservations()
  ]
})
