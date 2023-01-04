// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import head from 'lodash/head'
import React, { createContext, useContext, useMemo } from 'react'

import { useUser } from 'citizen-frontend/auth/state'
import { Loading, Result } from 'lib-common/api'
import {
  ActiveQuestionnaire,
  HolidayPeriod
} from 'lib-common/generated/api-types/holidayperiod'
import { useQueryResult } from 'lib-common/query'

import { activeQuestionnairesQuery, holidayPeriodsQuery } from './queries'

type QuestionnaireAvailability = boolean | 'with-strong-auth'

export interface HolidayPeriodsState {
  holidayPeriods: Result<HolidayPeriod[]>
  activeFixedPeriodQuestionnaire: Result<ActiveQuestionnaire | undefined>
  questionnaireAvailable: QuestionnaireAvailability
}

const defaultState: HolidayPeriodsState = {
  holidayPeriods: Loading.of(),
  activeFixedPeriodQuestionnaire: Loading.of(),
  questionnaireAvailable: false
}

export const HolidayPeriodsContext =
  createContext<HolidayPeriodsState>(defaultState)

export const HolidayPeriodsContextProvider = React.memo(
  function HolidayPeriodsContextContextProvider({
    children
  }: {
    children: React.ReactNode
  }) {
    const user = useUser()
    const holidayPeriods = useQueryResult(holidayPeriodsQuery, {
      enabled: !!user
    })
    const activeQuestionnaires = useQueryResult(activeQuestionnairesQuery, {
      enabled: !!user
    })

    const activeFixedPeriodQuestionnaire = activeQuestionnaires.map(head)
    const questionnaireAvailable = activeFixedPeriodQuestionnaire
      .map<QuestionnaireAvailability>((val) =>
        !val || !user
          ? false
          : val.questionnaire.requiresStrongAuth && user.authLevel !== 'STRONG'
          ? 'with-strong-auth'
          : true
      )
      .getOrElse(false)

    const value = useMemo(
      () => ({
        activeFixedPeriodQuestionnaire,
        holidayPeriods,
        questionnaireAvailable
      }),
      [activeFixedPeriodQuestionnaire, holidayPeriods, questionnaireAvailable]
    )

    return (
      <HolidayPeriodsContext.Provider value={value}>
        {children}
      </HolidayPeriodsContext.Provider>
    )
  }
)

export const useHolidayPeriods = () => useContext(HolidayPeriodsContext)
