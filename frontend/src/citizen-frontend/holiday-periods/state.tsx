// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import head from 'lodash/head'
import React, { createContext, useContext, useMemo } from 'react'

import { useUser } from 'citizen-frontend/auth/state'
import { combine, Loading, Result } from 'lib-common/api'
import FiniteDateRange from 'lib-common/finite-date-range'
import {
  ActiveQuestionnaire,
  HolidayPeriod
} from 'lib-common/generated/api-types/holidayperiod'
import LocalDate from 'lib-common/local-date'
import { useQueryResult } from 'lib-common/query'

import { activeQuestionnairesQuery, holidayPeriodsQuery } from './queries'

type QuestionnaireAvailability = boolean | 'with-strong-auth'
export type NoCta = { type: 'none' }
export type HolidayCta =
  | NoCta
  | { type: 'holiday'; period: FiniteDateRange; deadline: LocalDate }
  | { type: 'questionnaire'; deadline: LocalDate }

export interface HolidayPeriodsState {
  holidayPeriods: Result<HolidayPeriod[]>
  activeFixedPeriodQuestionnaire: Result<ActiveQuestionnaire | undefined>
  questionnaireAvailable: QuestionnaireAvailability
  holidayCta: Result<HolidayCta>
}

const defaultState: HolidayPeriodsState = {
  holidayPeriods: Loading.of(),
  activeFixedPeriodQuestionnaire: Loading.of(),
  questionnaireAvailable: false,
  holidayCta: Loading.of()
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

    const holidayCta = combine(
      activeFixedPeriodQuestionnaire,
      holidayPeriods
    ).map<HolidayCta>(([questionnaire, periods]) => {
      if (questionnaire) {
        return {
          type: 'questionnaire',
          deadline: questionnaire.questionnaire.active.end
        }
      }

      const today = LocalDate.todayInSystemTz()
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
        holidayCta
      }),
      [
        activeFixedPeriodQuestionnaire,
        holidayPeriods,
        questionnaireAvailable,
        holidayCta
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
