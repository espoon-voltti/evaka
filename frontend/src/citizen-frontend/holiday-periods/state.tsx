// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { head } from 'lodash'
import React, { createContext, useContext, useMemo } from 'react'

import { useUser } from 'citizen-frontend/auth/state'
import { combine, Loading, Result, Success } from 'lib-common/api'
import FiniteDateRange from 'lib-common/finite-date-range'
import {
  FixedPeriodQuestionnaireWithChildren,
  HolidayPeriod
} from 'lib-common/generated/api-types/holidayperiod'
import LocalDate from 'lib-common/local-date'
import { useApiState } from 'lib-common/utils/useRestApi'

import { getActiveQuestionnaires, getHolidayPeriods } from './api'

type QuestionnaireAvailability = boolean | 'with-strong-auth'
export type HolidayBanner =
  | { type: 'none' }
  | { type: 'holiday'; period: FiniteDateRange; deadline: LocalDate }
  | { type: 'questionnaire'; deadline: LocalDate }

export interface HolidayPeriodsState {
  holidayPeriods: Result<HolidayPeriod[]>
  activeFixedPeriodQuestionnaire: Result<
    FixedPeriodQuestionnaireWithChildren | undefined
  >
  questionnaireAvailable: QuestionnaireAvailability
  holidayBanner: Result<HolidayBanner>
}

const defaultState: HolidayPeriodsState = {
  holidayPeriods: Loading.of(),
  activeFixedPeriodQuestionnaire: Loading.of(),
  questionnaireAvailable: false,
  holidayBanner: Loading.of()
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

    const holidayBanner = combine(
      activeFixedPeriodQuestionnaire,
      holidayPeriods
    ).map<HolidayBanner>(([questionnaire, periods]) => {
      if (questionnaire) {
        return {
          type: 'questionnaire',
          deadline: questionnaire.questionnaire.active.end
        }
      }

      const today = LocalDate.today()
      const activeHolidayPeriod = periods.find((p) =>
        p.reservationDeadline?.isEqualOrAfter(today)
      )
      return activeHolidayPeriod?.reservationDeadline
        ? {
            type: 'holiday',
            deadline: activeHolidayPeriod.reservationDeadline,
            period: activeHolidayPeriod.period
          }
        : { type: 'none' }
    })

    const value = useMemo(
      () => ({
        activeFixedPeriodQuestionnaire,
        holidayPeriods,
        questionnaireAvailable,
        holidayBanner
      }),
      [
        activeFixedPeriodQuestionnaire,
        holidayPeriods,
        questionnaireAvailable,
        holidayBanner
      ]
    )

    return (
      <HolidayPeriodsContext.Provider value={value}>
        {children}
      </HolidayPeriodsContext.Provider>
    )
  }
)

export const useHolidayPeriods = () => useContext(HolidayPeriodsContext)
