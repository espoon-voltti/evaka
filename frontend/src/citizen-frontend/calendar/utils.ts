// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Result } from 'lib-common/api'
import FiniteDateRange from 'lib-common/finite-date-range'
import { ActiveQuestionnaire } from 'lib-common/generated/api-types/holidayperiod'
import { ReservationChild } from 'lib-common/generated/api-types/reservations'
import LocalDate from 'lib-common/local-date'

import { User } from '../auth/state'

export const getEarliestReservableDate = (
  childInfo: ReservationChild[],
  reservableDays: Record<string, FiniteDateRange[]>
) => {
  const earliestReservableDateByChild = childInfo.map((c) =>
    reservableDays[c.id].reduce<LocalDate | undefined>(
      (acc, cur) => (!acc || cur.start.isBefore(acc) ? cur.start : acc),
      undefined
    )
  )
  return earliestReservableDateByChild.reduce<LocalDate | undefined>(
    (acc, cur) => (!acc || cur?.isBefore(acc) ? cur : acc),
    undefined
  )
}

export const getLatestReservableDate = (
  childInfo: ReservationChild[],
  reservableDays: Record<string, FiniteDateRange[]>
) => {
  const earliestReservableDateByChild = childInfo.map((c) =>
    reservableDays[c.id].reduce(
      (acc, cur) => (cur.end.isAfter(acc) ? cur.end : acc),
      LocalDate.todayInSystemTz()
    )
  )
  return earliestReservableDateByChild.reduce(
    (acc, cur) => (cur.isAfter(acc) ? cur : acc),
    LocalDate.todayInSystemTz()
  )
}

export const isDayReservableForSomeone = (
  date: LocalDate,
  reservableDays: Record<string, FiniteDateRange[]>
) =>
  Object.entries(reservableDays).some(([_childId, ranges]) =>
    ranges.some((r) => r.includes(date))
  )

export type QuestionnaireAvailability = boolean | 'with-strong-auth'

export function isQuestionnaireAvailable(
  activeQuestionnaires: Result<ActiveQuestionnaire | null>,
  user: User | undefined
): QuestionnaireAvailability {
  return activeQuestionnaires
    .map<QuestionnaireAvailability>((val) =>
      !val || !user
        ? false
        : val.questionnaire.requiresStrongAuth && user.authLevel !== 'STRONG'
        ? 'with-strong-auth'
        : true
    )
    .getOrElse(false)
}
