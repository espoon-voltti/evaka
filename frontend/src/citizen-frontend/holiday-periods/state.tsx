// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { head } from 'lodash'
import React, { createContext, useContext, useMemo } from 'react'

import { useUser } from 'citizen-frontend/auth/state'
import { Loading, Result, Success } from 'lib-common/api'
import {
  FixedPeriodQuestionnaireWithChildren,
  HolidayPeriod
} from 'lib-common/generated/api-types/holidayperiod'
import { useApiState } from 'lib-common/utils/useRestApi'

import { getActiveQuestionnaires, getHolidayPeriods } from './api'

type QuestionnaireAvailability = boolean | 'with-strong-auth'

export interface HolidayPeriodsState {
  holidayPeriods: Result<HolidayPeriod[]>
  activeFixedPeriodQuestionnaire: Result<
    FixedPeriodQuestionnaireWithChildren | undefined
  >
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
    const [holidayPeriods] = useApiState(
      () => (user ? getHolidayPeriods() : Promise.resolve(Success.of([]))),
      [user]
    )
    const [activeQuestionnaires] = useApiState(
      () =>
        user ? getActiveQuestionnaires() : Promise.resolve(Success.of([])),
      [user]
    )

    const activeFixedPeriodQuestionnaire = activeQuestionnaires.map(head)
    const questionnaireAvailable = activeFixedPeriodQuestionnaire
      .map<QuestionnaireAvailability>((val) =>
        !val || !user
          ? false
          : val.questionnaire.requiresStrongAuth && user.userType !== 'ENDUSER'
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
