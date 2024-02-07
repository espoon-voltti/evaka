// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import orderBy from 'lodash/orderBy'
import React, { useCallback, useMemo, useState } from 'react'
import styled from 'styled-components'

import { renderResult } from 'employee-frontend/components/async-rendering'
import { useTranslation } from 'employee-frontend/state/i18n'
import FiniteDateRange from 'lib-common/finite-date-range'
import { BoundForm, useForm, useFormFields } from 'lib-common/form/hooks'
import { CalendarEvent } from 'lib-common/generated/api-types/calendarevent'
import LocalDate from 'lib-common/local-date'
import { useQueryResult } from 'lib-common/query'
import { UUID } from 'lib-common/types'
import Button from 'lib-components/atoms/buttons/Button'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { H3 } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'

import { unitGroupDetailsQuery } from '../../../queries'
import {
  DiscussionReservationCalendar,
  DiscussionTimesCalendar
} from '../times-calendar/TimesCalendar'

import {
  DiscussionSurveyEditMode,
  DiscussionSurveyForm,
  basicInfoForm,
  timesForm
} from './DiscussionSurveyEditor'

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

export default React.memo(function DiscussionTimesForm({
  basicInfo,
  eventData,
  groupId,
  unitId,
  editMode
}: {
  basicInfo: BoundForm<typeof basicInfoForm>
  eventData: CalendarEvent | null
  groupId: UUID
  unitId: UUID
  editMode: DiscussionSurveyEditMode
}) {
  const { i18n } = useTranslation()
  const t = i18n.unit.calendar.events
  const today = LocalDate.todayInSystemTz()

  const [calendarHorizonInMonths, setCalendarHorizonInMonths] =
    useState<number>(3)

  const discussionTimesForm = useForm(
    timesForm,
    () => ({
      times:
        eventData && eventData.times.length > 0
          ? eventData.times.map((t) => ({
              id: t.id,
              childId: t.childId,
              date: t.date,
              timeRange: {
                startTime: t.startTime.format(),
                endTime: t.endTime.format()
              }
            }))
          : []
    }),
    i18n.validationErrors
  )

  const { times } = useFormFields(discussionTimesForm)

  const period = useMemo(() => {
    const today = LocalDate.todayInSystemTz()

    if (times.state.length > 0) {
      const sortedTimes = orderBy(times.state, [(t) => t.date])
      return new FiniteDateRange(
        sortedTimes[0].date,
        sortedTimes[sortedTimes.length - 1].date
      )
    } else {
      return new FiniteDateRange(today, today)
    }
  }, [times.state])

  const groupData = useQueryResult(
    unitGroupDetailsQuery(unitId, period.start, period.end)
  )

  const calendarPeriod = useMemo(() => {
    const sortedTimes = orderBy(times.state, [(t) => t.date])
    let start, end: LocalDate
    if (sortedTimes.length === 0) {
      start = today
      end = today.addMonths(calendarHorizonInMonths)
    } else {
      const [firstTime, lastTime] = [
        sortedTimes[0].date,
        sortedTimes[sortedTimes.length - 1].date
      ]

      start = firstTime.isEqualOrBefore(today) ? firstTime : today
      end = lastTime.isEqualOrAfter(today)
        ? lastTime.addMonths(calendarHorizonInMonths)
        : today.addMonths(calendarHorizonInMonths)
    }

    // force period start to be a full week to maintain grid
    const previousMondayFromStart = start.subDays(start.getIsoDayOfWeek() - 1)
    const lastDayOfEndMonth = LocalDate.of(end.year, end.month, 1)
      .addMonths(1)
      .subDays(1)

    return new FiniteDateRange(previousMondayFromStart, lastDayOfEndMonth)
  }, [times.state, today, calendarHorizonInMonths])

  const addTimeForDay = useCallback(
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
    <>
      <div>
        {renderResult(groupData, (groupResult) => {
          const { individualChildren, groups, isNewSurvey } = eventData
            ? { ...eventData, isNewSurvey: false }
            : { individualChildren: [], groups: [], isNewSurvey: true }

          return (
            <DiscussionSurveyForm
              times={discussionTimesForm}
              basicInfo={basicInfo}
              eventData={eventData}
              period={period}
              unitId={unitId}
              invitedAttendees={{ individualChildren, groups, isNewSurvey }}
              possibleAttendees={groupResult}
              groupId={groupId}
            />
          )
        })}
        <BorderedBox>
          <H3 noMargin>{t.discussionReservation.surveyDiscussionTimesTitle}</H3>
        </BorderedBox>
        <TimesCalendarContainer>
          {editMode === 'create' ? (
            <DiscussionTimesCalendar
              unitId={unitId}
              groupId={groupId}
              times={discussionTimesForm}
              addAction={addTimeForDay}
              removeAction={removeTimeById}
              calendarRange={calendarPeriod}
            />
          ) : (
            <DiscussionReservationCalendar
              unitId={unitId}
              groupId={groupId}
              eventData={eventData}
              invitees={[]}
              calendarRange={new FiniteDateRange(today, today)}
            />
          )}

          <Gap size="L" />
          <FixedSpaceRow fullWidth alignItems="center" justifyContent="center">
            <Button
              onClick={() =>
                setCalendarHorizonInMonths(calendarHorizonInMonths + 1)
              }
              text={
                i18n.unit.calendar.events.discussionReservation.calendar
                  .addTimeButton
              }
            />
          </FixedSpaceRow>
        </TimesCalendarContainer>
      </div>
    </>
  )
})
