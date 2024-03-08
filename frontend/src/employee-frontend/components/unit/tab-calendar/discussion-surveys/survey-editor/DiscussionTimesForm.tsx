// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import orderBy from 'lodash/orderBy'
import React, { useCallback, useMemo, useRef, useState } from 'react'
import styled from 'styled-components'

import { renderResult } from 'employee-frontend/components/async-rendering'
import { useTranslation } from 'employee-frontend/state/i18n'
import FiniteDateRange from 'lib-common/finite-date-range'
import { mapped } from 'lib-common/form/form'
import { useForm, useFormFields } from 'lib-common/form/hooks'
import { CalendarEvent } from 'lib-common/generated/api-types/calendarevent'
import LocalDate from 'lib-common/local-date'
import { useQueryResult } from 'lib-common/query'
import { UUID } from 'lib-common/types'
import { scrollRefIntoView } from 'lib-common/utils/scrolling'
import Button from 'lib-components/atoms/buttons/Button'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { H3 } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'

import { unitGroupDetailsQuery } from '../../../queries'
import { DiscussionTimesCalendar } from '../times-calendar/TimesCalendar'

import { DiscussionSurveyEditMode } from './DiscussionSurveyEditor'
import DiscussionSurveyForm from './DiscussionSurveyForm'
import { surveyForm } from './form'

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
  eventData,
  groupId,
  unitId,
  editMode
}: {
  eventData: CalendarEvent | null
  groupId: UUID
  unitId: UUID
  editMode: DiscussionSurveyEditMode
}) {
  const { i18n } = useTranslation()
  const t = i18n.unit.calendar.events

  const calendarRef = useRef(null)
  const getCalendarHorizon = useCallback(() => {
    const today = LocalDate.todayInSystemTz()
    const previousMonday = today.subDays(today.getIsoDayOfWeek() - 1)

    const defaultHorizonDate = previousMonday.addMonths(3).lastDayOfMonth()

    const eventDataHorizonDate = eventData
      ? eventData.period.end.lastDayOfMonth()
      : today

    return defaultHorizonDate.isAfter(eventDataHorizonDate)
      ? defaultHorizonDate
      : eventDataHorizonDate
  }, [eventData])

  const [calendarHorizonDate, setCalendarHorizonDate] =
    useState<LocalDate>(getCalendarHorizon())

  const mappedForm = mapped(surveyForm, (output) => ({
    ...output
  }))

  const initializedForm = useForm(
    mappedForm,
    () => ({
      title: eventData?.title ?? '',
      description: eventData?.description ?? '',
      times: []
    }),
    i18n.validationErrors
  )

  const { times } = useFormFields(initializedForm)

  const period = useMemo(() => {
    const today = LocalDate.todayInSystemTz()

    if (eventData) {
      return eventData.period
    } else {
      if (times.state.length > 0) {
        const sortedTimes = orderBy(times.state, [(t) => t.date])
        return new FiniteDateRange(
          sortedTimes[0].date,
          sortedTimes[sortedTimes.length - 1].date
        )
      } else {
        return new FiniteDateRange(today, today)
      }
    }
  }, [times.state, eventData])

  const calendarRange = useMemo(() => {
    const today = LocalDate.todayInSystemTz()

    const previousMonday = today.subDays(today.getIsoDayOfWeek() - 1)

    return new FiniteDateRange(previousMonday, calendarHorizonDate)
  }, [calendarHorizonDate])

  const groupData = useQueryResult(
    unitGroupDetailsQuery(unitId, calendarRange.start, calendarRange.end)
  )

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
      {renderResult(groupData, (groupResult) => (
        <>
          <DiscussionSurveyForm
            eventData={eventData}
            period={period}
            form={initializedForm}
            groupResult={groupResult}
            groupId={groupId}
            unitId={unitId}
          />

          {editMode === 'create' && (
            <>
              <BorderedBox>
                <H3 noMargin>
                  {t.discussionReservation.surveyDiscussionTimesTitle}
                </H3>
              </BorderedBox>
              <TimesCalendarContainer>
                <DiscussionTimesCalendar
                  unitId={unitId}
                  groupId={groupId}
                  times={times}
                  calendarRange={calendarRange}
                  addAction={addTime}
                  removeAction={removeTimeById}
                />

                <Gap size="L" ref={calendarRef} />
                <FixedSpaceRow
                  fullWidth
                  alignItems="center"
                  justifyContent="center"
                >
                  <Button
                    onClick={() => {
                      setCalendarHorizonDate(
                        calendarHorizonDate.addMonths(1).lastDayOfMonth()
                      )
                      scrollRefIntoView(calendarRef, 500)
                    }}
                    text={
                      i18n.unit.calendar.events.discussionReservation.calendar
                        .addTimeButton
                    }
                  />
                </FixedSpaceRow>
              </TimesCalendarContainer>
            </>
          )}
        </>
      ))}
    </div>
  )
})
