// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import orderBy from 'lodash/orderBy'
import React, { MutableRefObject, useCallback, useMemo } from 'react'
import styled from 'styled-components'

import { useTranslation } from 'employee-frontend/state/i18n'
import FiniteDateRange from 'lib-common/finite-date-range'
import { mapped } from 'lib-common/form/form'
import { useForm, useFormFields } from 'lib-common/form/hooks'
import {
  CalendarEvent,
  DiscussionReservationDay
} from 'lib-common/generated/api-types/calendarevent'
import { UnitGroupDetails } from 'lib-common/generated/api-types/daycare'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'
import Button from 'lib-components/atoms/buttons/Button'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { H3 } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'

import { DiscussionTimesCalendar } from '../times-calendar/TimesCalendar'

import DiscussionSurveyForm from './DiscussionSurveyForm'
import { filterAttendees, mergeAttendeeChanges, surveyForm } from './form'

export const TimesCalendarContainer = styled.div`
  max-height: 680px;
  overflow-y: scroll;
  overflow-x: hidden;
`

export const BorderedBox = styled.div`
  position: relative;
  z-index: 2;
  box-shadow: 0 2px 0px rgba(0, 0, 0, 0.15);
  margin-bottom: 0;
  padding-top: 24px;
  padding-bottom: 24px;
`
export type NewEventTimeForm = {
  id: UUID
  date: LocalDate
  childId: null
  timeRange: { startTime: string; endTime: string }
}

export const getPeriodFromDatesOrToday = (
  dates: LocalDate[]
): FiniteDateRange => {
  const today = LocalDate.todayInSystemTz()
  const sortedDates = orderBy(dates)
  return sortedDates.length > 0
    ? new FiniteDateRange(sortedDates[0], sortedDates[sortedDates.length - 1])
    : new FiniteDateRange(today, today)
}

export default React.memo(function DiscussionTimesForm({
  eventData,
  groupId,
  unitId,
  groupData,
  horizonRef,
  calendarRange,
  maxCalendarRange,
  calendarDays,
  extendHorizonAction
}: {
  eventData: CalendarEvent | null
  groupId: UUID
  unitId: UUID
  groupData: UnitGroupDetails
  horizonRef: MutableRefObject<HTMLDivElement | null>
  calendarRange: FiniteDateRange
  calendarDays: DiscussionReservationDay[]
  maxCalendarRange: FiniteDateRange
  extendHorizonAction: () => void
}) {
  const { i18n } = useTranslation()
  const t = i18n.unit.calendar.events
  const today = LocalDate.todayInSystemTz()

  const initialPeriod = useMemo(
    () => eventData?.period ?? new FiniteDateRange(today, today),
    [eventData, today]
  )

  const mappedForm = mapped(surveyForm, (output) => ({
    ...output
  }))

  const initializedForm = useForm(
    mappedForm,
    () => ({
      title: eventData?.title ?? '',
      description: eventData?.description ?? '',
      times: [],
      attendees: filterAttendees(groupData, groupId, eventData, initialPeriod)
    }),
    i18n.validationErrors,
    {
      onUpdate(prevState, nextState) {
        // if times have changed, check whether their min-max period has changed
        const oldTimes = prevState.times.map((t) => t.date)
        const newTimes = nextState.times.map((t) => t.date)
        if (oldTimes.length === 0 && newTimes.length === 0) {
          return nextState
        } else {
          const oldPeriod = getPeriodFromDatesOrToday(oldTimes)
          const newPeriod = getPeriodFromDatesOrToday(newTimes)
          if (oldPeriod.isEqual(newPeriod)) {
            return nextState
          } else {
            // in case of a period change, recalculate attendees from group placement data
            return {
              ...nextState,
              attendees: mergeAttendeeChanges(
                groupData,
                prevState,
                newPeriod,
                groupId,
                eventData
              )
            }
          }
        }
      }
    }
  )

  const { times } = useFormFields(initializedForm)

  const addTime = useCallback(
    (et: NewEventTimeForm) => {
      times.set([...times.state, et])
    },
    [times]
  )
  const removeTimeById = useCallback(
    (id: UUID) => {
      times.set(times.state.filter((t) => t.id !== id))
    },
    [times]
  )

  return (
    <div>
      <DiscussionSurveyForm
        eventData={eventData}
        form={initializedForm}
        groupId={groupId}
        unitId={unitId}
      />

      {!eventData && (
        <>
          <BorderedBox>
            <H3 noMargin>
              {t.discussionReservation.surveyDiscussionTimesTitle}
            </H3>
          </BorderedBox>
          <TimesCalendarContainer>
            <DiscussionTimesCalendar
              times={times}
              calendarRange={calendarRange}
              calendarDays={calendarDays}
              addAction={addTime}
              removeAction={removeTimeById}
              horizonRef={horizonRef}
            />

            {calendarRange.end.isBefore(maxCalendarRange.end) && (
              <>
                <Gap size="L" />
                <FixedSpaceRow
                  fullWidth
                  alignItems="center"
                  justifyContent="center"
                >
                  <Button
                    onClick={extendHorizonAction}
                    text={
                      i18n.unit.calendar.events.discussionReservation.calendar
                        .addTimeButton
                    }
                  />
                </FixedSpaceRow>
                <Gap size="m" />
              </>
            )}
          </TimesCalendarContainer>
        </>
      )}
    </div>
  )
})
