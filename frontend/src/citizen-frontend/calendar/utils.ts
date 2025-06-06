// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import sum from 'lodash/sum'

import type { Result } from 'lib-common/api'
import type { CitizenCalendarEvent } from 'lib-common/generated/api-types/calendarevent'
import type { ActiveQuestionnaire } from 'lib-common/generated/api-types/holidayperiod'
import type {
  ReservableTimeRange,
  Reservation,
  ReservationChild
} from 'lib-common/generated/api-types/reservations'
import type LocalDate from 'lib-common/local-date'
import { formatPersonName } from 'lib-common/names'
import { featureFlags, type Translations } from 'lib-customizations/citizen'

import type { User } from '../auth/state'

import type { MonthlyTimeSummary } from './MonthlyHoursSummary'
import type { ChildImageData } from './RoundChildImages'

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

export function countEventsForDay(
  events: CitizenCalendarEvent[],
  day: LocalDate
) {
  const currentEvents = events.filter((e) => e.period.includes(day))

  if (currentEvents.length > 0) {
    const daycareEvents = currentEvents.filter(
      (e) => e.eventType === 'DAYCARE_EVENT'
    )
    const discussionSurveys = currentEvents.filter(
      (e) => e.eventType === 'DISCUSSION_SURVEY'
    )
    //the number of children that are attending the event at this calendar day
    const eventCount = sum(
      daycareEvents.map(
        ({ attendingChildren }) =>
          Object.values(attendingChildren).filter((ac) =>
            ac!.some(({ periods }) => periods.some((p) => p.includes(day)))
          ).length
      )
    )

    const discussionReservationCount = featureFlags.discussionReservations
      ? discussionSurveys.reduce(
          (acc, curr) =>
            //the number of children that have a reserved discussion time for this survey at this calendar date
            //(if a reserved time is returned, it belongs to the child)
            acc +
            Object.values(curr.timesByChild).filter((times) =>
              times!.some((t) => t.date.isEqual(day) && t.childId)
            ).length,
          0
        )
      : 0

    return eventCount + discussionReservationCount
  } else {
    return 0
  }
}

export function getSummaryForMonth(
  childData: ReservationChild[],
  year: number,
  month: number
): MonthlyTimeSummary[] {
  return childData.flatMap(({ monthSummaries, ...rest }) => {
    const summaryForMonth = monthSummaries?.find(
      (monthSummary) =>
        monthSummary.year === year && monthSummary.month === month
    )
    if (!summaryForMonth) {
      return []
    }
    return {
      name: formatPersonName(rest, 'Preferred'),
      ...summaryForMonth
    }
  })
}

export const getChildImages = (
  childData: ReservationChild[]
): ChildImageData[] =>
  childData.map((child, index) => ({
    childId: child.id,
    imageId: child.imageId,
    initialLetter: (formatPersonName(child, 'FirstFirst') || '?')[0],
    colorIndex: index,
    childName: child.firstName
  }))

export const formatReservation = (
  reservation: Reservation.Times,
  reservableTimeRange: ReservableTimeRange,
  i18n: Translations
) => {
  const timeOutput = reservation.range.format()

  if (!featureFlags.intermittentShiftCare) {
    return timeOutput
  } else {
    const showIntermittentShiftCareNotice =
      reservableTimeRange.type === 'INTERMITTENT_SHIFT_CARE' &&
      (reservableTimeRange.placementUnitOperationTime === null ||
        !reservableTimeRange.placementUnitOperationTime.contains(
          reservation.range
        ))

    return showIntermittentShiftCareNotice
      ? `${timeOutput} ${i18n.calendar.intermittentShiftCareNotification}`
      : timeOutput
  }
}
